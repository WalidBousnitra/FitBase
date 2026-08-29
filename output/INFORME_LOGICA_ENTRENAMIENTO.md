# Informe: Lógica de Entrenamiento en FitBase

> Análisis técnico de toda la lógica de entrenamiento — desde la evidencia científica hasta su implementación real en `Codigo.gs`, Google Sheets y la app Android. Cada decisión se justifica con su fuente. Incluye una sección final dedicada exclusivamente al uso del Sleep Score, a petición explícita.

---

## 1. Arquitectura de 3 capas

```
knowledge_base/*.md        "qué debería pasar, y por qué" (evidencia + reglas)
        ▼
Codigo.gs (Apps Script)     "qué pasa realmente, cada vez que se sirve una sesión"
        ▼
Android app + Sheets        "qué ve y registra el usuario"
```

Igual que en el informe de nutrición: hay contenido en el knowledge_base que **no tiene código correspondiente** (protocolos de lesión/enfermedad/viaje, redistribución de volumen tras ausencia). Se marca explícitamente dónde en la §8.

---

## 2. Los dos motores distintos — no confundir

FitBase tiene **dos sistemas de autorregulación separados** que a veces se mezclan al hablar de "el motor":

| Motor | Qué ajusta | Dónde vive |
|---|---|---|
| **Motor de Cargas** (`calcularPesoSugerido_`) | El **peso en kg** (o reps, en ejercicios sin carga externa) de cada ejercicio | 6 capas multiplicativas — §4 |
| **MAV — Volumen Máximo Adaptativo** (`ajustarSeriesAdaptativo_`) | El **número de series** de ejercicios de aislamiento en grupos prioritarios | Independiente del anterior — §5 |

Ambos leen el mismo `factorDia` (calculado una sola vez por `calcularAjusteDia_`), pero lo usan con lógica distinta — esto es clave para entender por qué un sueño malo no siempre "reduce el entrenamiento" de la misma forma en peso y en volumen (ver §7, la pregunta original).

---

## 3. Capa de datos — qué se persiste y por qué

### `ejercicios_log`: retención de 7 días, no historial permanente

`base_datos.md §3.6`: el motor de cargas **no necesita** un historial largo — solo el rendimiento más reciente. Justificación explícita en el código: en fase de adaptación hay más diferencia entre lo planeado y lo real, y el motor se vuelve más preciso con el tiempo, pero eso ya lo captura el **peso recalculado**, no hace falta guardar meses de logs.

**Consecuencia de diseño directa**: si pasan más de 7 días sin registrar un ejercicio, `obtenerMejorSetReciente_` no encuentra nada → el motor devuelve *"Sin historial — elige tu peso"*. Esto no es un bug: es la razón por la que la Capa 5 (gap >14 días) se eliminó como código muerto (nunca puede dispararse con un log de 7 días de retención).

### `ejercicios_plan`: NO guarda pesos

`base_datos.md §3.5` y `motor_pesos.md §8` lo remarcan: los pesos son **efímeros**, calculados al vuelo en cada `getSesionHoy_()`. Esto hace que `guardarLog_` (POST) sea O(1) — solo un `appendRow`, sin recalcular nada al guardar. Todo el cálculo pesado ocurre al **leer** la sesión de la mañana, no al escribir series por la noche.

### `plan_anual` / `sesiones_plan`: qué decide cada una

- `plan_anual` fija la **fase** (VOL/FZA/DEF/MNT/DELOAD) por rango de fechas — de aquí sale el objetivo nutricional y el "modulador de fase" del motor de cargas.
- `sesiones_plan` es la tabla ancla del día a día: `getSesionHoy_()` busca aquí la sesión de hoy por fecha exacta.

---

## 4. Motor de Cargas — `calcularPesoSugerido_()` (6 capas)

Fórmula final exacta (Codigo.gs línea 1555):
```js
pesoFinal = redondear025( pesoProg × factorDescanso × factorDia )
```
Desglose capa por capa:

### Capa 1 — BASE: el *mejor* set, no el último

```js
var ultimo = obtenerMejorSetReciente_(ejercicioId); // máx(reps + RIR) de la última sesión
```
**Por qué el mejor set y no el último**: con series rectas al mismo RIR objetivo, el último set siempre tiene menos reps por fatiga acumulada *dentro* de la sesión. Usar el último set hacía que el motor interpretase una sesión perfecta como "te quedaste corto" y bajara el peso — erosión silenciosa del progreso. Es un fix documentado explícitamente en el código (2026), y es la decisión individual más importante de todo el motor: cambia qué dato de entrada alimenta las otras 5 capas.

Si no hay ningún log del ejercicio en los últimos 7 días, o el último peso registrado fue 0 (primera vez) → *"elige tu peso"*, sin capas adicionales.

### Capa 2 — APRE (Mann 2010 + ACSM 2009)

```
delta_capacidad = (reps_mejor_set + RIR_percibido) − (reps_objetivo_tope + RIR_objetivo)
```
| delta | Ajuste | Justificación |
|---|---|---|
| ≤ −4 | −10% | Mann 2010: 0-2 reps en el set AMRAP → muy pesado |
| ≤ −2 | −5% | Mann 2010: 3-4 reps → pesado |
| ≤ 0 | 0% | En el objetivo o 1 por debajo → mantener |
| 1..3 | +5% | ACSM 2009: "completas 1-2 reps MÁS → sube 2-10%" |
| > 3 | +10% | Mann 2010: 13+ reps → muy fácil |

**Fix relevante ya aplicado**: el umbral de subida bajó de "delta≥2" a "delta≥1" — con el umbral antiguo, superar el techo del rango por solo 1 repetición no movía la carga (estancamiento). Ahora sí, cumpliendo la doble progresión real de ACSM 2009.

`repsObjetivo` llega como string de rango ("8-10") — `parseRepsObjetivo_` toma el **tope** del rango (10), porque ACSM 2009 dice que se progresa al superar el techo, no la media.

### Capa 3 — FASE (Bompa 2019): caps de agresividad

```js
{ VOL: cap_subida 5%, cap_bajada 10% }
{ FZA: cap_subida 10%, cap_bajada 10% }
{ DEF: cap_subida 3%, cap_bajada 10% }
{ MNT: cap_subida 2.5%, cap_bajada 5% }
{ DELOAD: override total a 87.5% de intensidad (Bompa: reducir 10-15%) }
```
**Nota de precisión** (el propio `motor_pesos.md` lo simplifica como "±5%, ±10%..."): en el código real, el cap de **bajada siempre es igual o mayor que el de subida** en todas las fases salvo FZA — es decir, el motor está diseñado para bajar peso más fácilmente que para subirlo. Es una asimetría de seguridad intencional, no un error de transcripción del markdown.

DELOAD hace un **override completo**: ignora APRE, nutrición y descanso — el peso es directamente `pesoBase × 0.875`, coherente con "la descarga ya viene reducida por diseño, no por rendimiento del día".

### Capa 4 — NUTRICIÓN (Helms 2014)

```js
if (objNutri === 'cut' && ajusteKg > 0) ajusteKg *= 0.5;
```
En déficit calórico es fisiológicamente más difícil ganar fuerza real — se permite progresar, pero a mitad de velocidad. Solo afecta subidas, nunca bajadas (una bajada de peso en cut no se "frena" — sería contraproducente).

### Capa 5 — DESCANSO INTER-SESIÓN

```js
if (diasDesde > 7) factorDescanso = 0.95;
```
Con la retención de 7 días de `ejercicios_log`, el único hueco que puede sobrevivir hasta esta capa es un ejercicio de frecuencia 1x/semana que se retrasa a 8-9 días — el tramo antiguo ">14 días → ×0.90" era código muerto (nunca hay un log de 14 días vivo) y se eliminó.

### Capa 6 — DÍA (Kiviniemi 2007 + Fullagar 2015)

```js
var factorDia = Number(ctx.ajusteDia) || 1.0;
```
Este es el factor calculado por `calcularAjusteDia_()` — **desarrollado en detalle en la §7**, porque es el centro de la pregunta sobre el sueño.

---

## 5. MAV — Volumen Máximo Adaptativo (`ajustarSeriesAdaptativo_`)

Sistema **independiente** del peso — ajusta el número de *series*, no los kg.

```js
if (fase === 'FZA' || fase === 'DELOAD') return sin cambios; // no se auto-regulan

// BAJADA
if (factorDia <= 0.80 && series > 1) → series - 1   // "vol-recuperacion"

// SUBIDA (solo aislamiento de grupos prioritarios, semana ≥2 del mesociclo)
if (factorDia >= 1.0 && semFase >= 2 && !esCompuesto) → +1 en sem2, +2 desde sem3
  tope: +2 series por GRUPO muscular completo (no por ejercicio), compartido
  entre todos los ejercicios de aislamiento de ese grupo en la sesión
```

**Punto crítico para la pregunta del usuario**: el umbral de bajada es `factorDia <= 0.80`. Como se ve en la §7, el sueño **por sí solo nunca baja de 0.90** — con lo cual **un sleep score bajo, aislado, nunca activa el recorte de series**. Solo lo activa una FC de reposo elevada (que por sí sola ya da 0.80) o la combinación FC+sueño (0.72).

**Por qué el tope de +2 es por grupo y no por ejercicio** (fix MAV-01 documentado): antes cada ejercicio de aislamiento podía subir +2 de forma independiente — Hombros con 3 ejercicios de aislamiento podía terminar con +6 series reales en una semana de buena recuperación, muy por encima del techo de evidencia (14-18 ser/semana, Schoenfeld 2017). Ahora hay un acumulador compartido por sesión (`bonoGrupoUsado`) que topa el bono en +2 **del grupo completo**.

---

## 6. Programación semanal — qué es fijo y qué es dinámico

### Fijo todo el año (`programacion.md §11`)
```yaml
lunes: PUSH (Pecho+Hombros+Tríceps)
martes: NATACIÓN
miercoles: PIERNA + Core
jueves: NATACIÓN
viernes: PULL (Espalda+Bíceps+Postura)
sabado: HOMBROS+BRAZOS+Postura
domingo: DESCANSO
```
**Por qué PPL+Hombros y no Upper/Lower**: la prioridad P1 (V-taper) exige 14-18 series/semana de hombros+espalda — matemáticamente imposible de encajar en solo 2 días Upper. El split es una consecuencia directa del ranking de prioridades, no una preferencia estética de programación.

**Rebalanceo de volumen ya auditado** (`programacion.md §11`, "2026-b"): se detectó que Hombros llegaba a 28 series/semana real (55-80% por encima de su propio techo, ya elevado por prioridad) porque dos ejercicios se repetían sin variar ángulo en ambos días de gym que tocan hombro, y porque el hombro es sinergista en Press inclinado (pecho) y Dominadas/Remo (espalda) — la fatiga sistémica real es mayor que el recuento de series directas. Se quitó la redundancia y se realojó el tiempo liberado a Pecho (4→7 series/semana), que estaba deliberadamente bajo el mínimo efectivo (5) por prioridad, pero no por debajo de él.

### Dinámico (decidido en cada request, no en la plantilla)
- **Pesos y reps de progresión** (Capas 1-6, arriba)
- **Volumen adaptativo** (MAV, arriba)
- **Ramadán**: -30% de series adicional sobre el volumen ya ajustado por MAV — pero NO en DELOAD (evita apilar -40% de deload + -30% de Ramadán = -58% sin que nadie lo decidiera así, fix RAM-01 documentado)
- **Cardio y pasos por fase** (`getCardioObjetivo_`): VOL/FZA=8000 pasos/0 min bici, DEF=10000/15-20min, MNT=9000/10min opcional, DELOAD=7000/0min. Justificación Wilson 2012: correr interfiere -31% con hipertrofia, bici no interfiere — por eso el cardio siempre es bici/elíptica, nunca correr, y solo aparece explícitamente en fases de déficit/mantenimiento
- **Movilidad matutina** (`getMovilidadMatutina_`): se muestra **todos los días sin excepción** (gym, natación o descanso) — Ruivo 2017 exige frecuencia diaria para que el protocolo correctivo de 16 semanas funcione; es la única sección de la vista mañana que no depende de ninguna condición
- **Core en día de descanso** (`getCoreDia_`): sube la frecuencia de abdominales a 2x/semana usando el día de descanso, para cumplir el rango 6-10 series/semana sin duplicar ejercicios en el mismo día que Pierna

---

## 7. El Sleep Score — cómo se usa, con precisión total

Esta sección responde directamente a la pregunta: *"¿por qué un 49 hoy reduciría mi entrenamiento si dormí genial?"*

### 7.1 De dónde sale el número — y por qué NO es el de Zepp

Health Connect **no expone** el Sleep Score propietario de Zepp — solo expone las fases de sueño en bruto (duración, profundo, REM, ligero). El número que ves en FitBase **no es el score de la app Zepp** — es un score **estimado**, calculado por la propia app FitBase (`HealthConnectBridge.kt`, función de lectura de `SleepSessionRecord`).

> **Actualizado (2026-g)**: la fórmula original tenía un 3er factor ("cercanía de %profundo/%REM a un target fisiológico de 18%/22.5%, penalización ×3/punto") que **no estaba respaldado por `evidencia/sueno.md`** (Fullagar 2015 no da esos porcentajes, y la cita "Ohayon et al. 2004" del código anterior ni siquiera era una fuente del proyecto) — un dato inventado que REGLA CERO prohíbe, y la causa más probable de que el score saliera "siempre bajo" respecto al de Zepp: los wearables de muñeca (Amazfit GTS 4 incluido) infieren fases por movimiento+FC, no EEG, así que su reparto profundo/REM rara vez encaja con un "ideal" de laboratorio, aunque la noche haya sido objetivamente buena. Ese factor se **eliminó** — la fórmula actual es:

```
score = duración(70%) + eficiencia(30%)

duración   = min(100, minutos_dormidos / 450 × 100)   # objetivo 7.5h (Fullagar 2015 / Nat. Sleep Foundation)
eficiencia = minutos_dormidos / minutos_en_cama × 100  # % del tiempo en cama realmente dormido (ratio real, sin "objetivo" inventado)
```

Los dos factores que quedan tienen respaldo directo en `evidencia/sueno.md` §2 (horas recomendadas) y son ratios de datos reales, no comparaciones contra un target inventado — el score ya no puede hundirse solo porque el reloj clasificó las fases de forma distinta a una composición "típica".

**Por qué aun así puede salir bajo con una noche que sientes genial** (ya sin el 3er factor):
- Dormiste menos de 7.5h (el 70% de peso del score cae directo, aunque sean 6h de sueño excelente) — esto es real y esperado, no un bug: menos horas es objetivamente menos sueño.
- Baja "eficiencia" si el reloj detectó tiempo despierto en cama que tú no recuerdas (p. ej. microdespertares que Zepp cuenta como AWAKE).

### 7.2 Dónde entra este número en la lógica de entrenamiento

Un único punto de entrada: `calcularAjusteDia_()` (Codigo.gs):

```js
// Umbral y magnitud recalibrados (2026-d, a petición del usuario)
if (metrica.num_sleep_score && metrica.num_sleep_score < 30) {
  factor *= 0.96;   // -4%
  razones.push('Sleep score muy bajo <30 (heurístico, Fullagar 2015)');
}
```

> **Actualizado (2026-d)**: el umbral original era `<60 → ×0.90` (-10%) — casi cualquier noche imperfecta lo disparaba. Combinado con el fix 2026-g de arriba (fórmula del score sin el factor de fases inventado), ahora hacen falta DOS cosas para que el sueño reduzca algo: (1) que el score real caiga por debajo de 30 — algo que solo pasa con duración/eficiencia genuinamente bajas, ya no por un reparto de fases "atípico" — y (2) aun así el efecto es mínimo (-4%, antes -10%).

Eso es **todo** lo que hace el sueño por sí solo. No hay ningún otro umbral en el código (el "<50 → priorizar descanso" que aparece en `motor_pesos.md §7` como "ALERTA" **nunca se implementó** — es un umbral que existe en el markdown pero no tiene ninguna línea de código correspondiente; confirmado por búsqueda exhaustiva).

### 7.3 Qué efecto real tiene esto hoy

| Consumidor de `factorDia` | Umbral para activarse | ¿Se activa con 0.96 (solo sueño)? |
|---|---|---|
| **Peso sugerido** (`calcularPesoSugerido_`, Capa 6) | Cualquier valor <1.0 ya multiplica | ✅ Sí, pero solo si score <30 — el peso sugerido sale **4% más bajo** que si el factor fuera 1.0 |
| **Volumen/series** (`ajustarSeriesAdaptativo_`) | Necesita `factorDia ≤ 0.80` | ❌ No — 0.96 > 0.80, así que **las series NO se reducen** por sueño solo |
| **Banner "Sueño pobre — carga ligeramente reducida"** (`HomeActivity`) | `score < 30` | ✅ Sí, y ahora coincide exactamente con cuándo el backend aplica el ajuste (antes el umbral de la UI y el del backend estaban desalineados) |

**Conclusión**: con la fórmula y el umbral actuales, un score bajo por sí solo como mucho resta un 4% al peso sugerido de cada ejercicio, y solo si cae por debajo de 30 — nunca toca el número de series/ejercicios. Y sigue siendo solo una *sugerencia*: puedes registrar el peso que tú decidas, el motor no bloquea nada.

### 7.4 Por qué el sistema ya NO usa energía/estrés subjetivos (contexto importante)

Aquí hay una tensión real que vale la pena señalar: en 2026 tú mismo pediste **explícitamente sacar** la energía y el estrés subjetivos del cálculo de ajuste diario — antes sí entraban (con un suelo de -30% adicional), y pediste que el motor dejara de decidir por ti con esos datos, quedándose solo con FC reposo y Sleep Score (documentado en `motor_pesos.md §3`: *"el usuario pidió retirarlos del motor: los quiere registrar para poder mirarlos en retrospectiva, no que el sistema los use para decidir por él"*).

La consecuencia de esa decisión es la que estás notando ahora: el **único** proxy subjetivo de "cómo te sientes" que queda en el sistema es una fórmula de sueño estimada e imprecisa — no tienes forma de decirle al motor "hoy me siento bien" para que lo compense, porque ese canal se cerró a propósito. No es un bug: es el resultado directo de una petición anterior tuya, que ahora choca con esta nueva.

---

## 8. Lo que NO está implementado (documentado pero sin código)

| Excepción (`excepciones.md`) | ¿Implementada? | Estado real |
|---|---|---|
| Día perdido | ✅ Sí | Capa 5 del motor de pesos (gap >7d → ×0.95) |
| Vacaciones/ausencia | ⚠️ Parcial | `registrarAusencia_` suspende sesiones y devuelve un **mensaje descriptivo** ("RIR+1", "reiniciar mesociclo") — pero no fija ningún `rirObjetivo` real ni reinicia `num_semana_meso` en la hoja. Es texto informativo, no una acción automática |
| Viajes | ❌ No | Sin trigger, sin endpoint, sin lógica — solo existe en el markdown |
| Enfermedad | ❌ No | Igual — solo markdown |
| Lesión | ❌ No | Igual — el único mecanismo real de exclusión de ejercicios por lesión es estático: el codo se excluye a nivel de catálogo/selección de ejercicios, no como una "excepción activable" |
| Estrés extremo (`SUB_ESTRES > 8`) | ❌ No | Contradice además la decisión de §7.4 — el estrés subjetivo ya no entra en ningún cálculo |
| Ramadán | ✅ Sí | `-30%` series (excepto DELOAD), fechas hardcodeadas y confirmadas a mano cada año |

---

## 9. Observaciones menores (no bugs críticos, notas de código)

1. **Mensaje inconsistente en `checkAusencia_`**: el comentario justo encima dice explícitamente *"NO se redistribuye volumen — la evidencia no soporta series extra compensatorias"*, pero el mensaje que se devuelve al usuario cuando hay días perdidos dice literalmente *"Se redistribuye volumen"*. El campo `redistribucion` siempre es `null` — el texto es un resto de una versión anterior de la función que quedó desactualizado.
2. **`registrarAusencia_` promete más de lo que hace**: el `impacto`/`nota` que devuelve describe RIR+1 y reinicio de mesociclo como si fueran automáticos, pero ninguno de los dos se escribe en ninguna hoja — es información para que el usuario lo aplique manualmente, no una acción del motor.
3. **Asimetría subida/bajada en la Capa 3** ya comentada en §4 — no es un error, pero el markdown (`motor_pesos.md`) la simplifica de forma que no se nota hasta leer el código real.

---

## 10. Tabla de trazabilidad (decisión de código → evidencia)

| Decisión | Fuente |
|---|---|
| FC reposo +10 vs media 10d → ×0.80 | Kiviniemi et al. 2007 (adaptado de HRV a FC reposo) |
| Tendencia FC ascendente 2d → ×0.70 (early return) | Kiviniemi 2007, extrapolación de tendencia |
| Sleep score <60 → ×0.90 | Fullagar 2015 — **sin umbral específico del paper, heurístico marcado como tal** |
| Mejor set (no último) como base de progresión | Mann 2010, aplicado a series rectas (no AMRAP original) |
| Delta APRE (tabla de 5 niveles) | Mann 2010 + ACSM 2009 |
| Caps de subida/bajada por fase | Bompa 2019 (periodización) |
| Cap de progresión al 50% en cut | Helms 2014 |
| Gap >7d → ×0.95 | ACSM 2009 (frecuencia mínima 2-3x/semana) |
| MAV: -1 serie si factorDia≤0.80, +1/+2 desde semana 2 | Schoenfeld, Ogborn & Krieger 2017 (dosis-respuesta) + Bompa 2019 (gestión de fatiga) |
| Split PPL+Hombros fijo | Derivado matemáticamente de prioridades.md P1 (14-18 ser/sem hombros+espalda) |
| Cardio = bici, nunca correr | Wilson 2012 (-31% interferencia con carrera) |
| Movilidad matutina diaria sin excepción | Ruivo 2017 (protocolo 16 semanas requiere consistencia diaria) |
| Ramadán -30% series (no en DELOAD) | cultura.md §5 + fix RAM-01 (evitar apilar con el -40% del deload) |

---

*Fuentes revisadas: `motor_pesos.md`, `programacion.md`, `excepciones.md`, `prioridades.md`, `usuario/metricas/hardware.md`, `Codigo.gs` (líneas 585-627, 785-842, 1301-1863), `HealthConnectBridge.kt` (líneas 260-360), `HomeActivity.java` (bloques de sueño/aviso de fatiga).*
