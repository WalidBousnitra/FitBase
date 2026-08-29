---
id: "REG-LOG-01"
nombre: "Motor de Cargas"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-MET-01", "USR-MET-02", "REG-ENT-01"]
tags: ["reglas", "logica", "motor", "pesos", "autorregulacion"]
---

# Motor de Cargas

## 1. Alcance
Algoritmo que ajusta las cargas de entrenamiento basándose en fatiga, sueño y readiness.

## 2. Baseline del Usuario (18/06/2026)

| Métrica | Valor Actual | Interpretación |
|---------|--------------|----------------|
| FC reposo | 53 bpm | ✅ Excelente (atlético) |
| VO2max | 50 ml/kg/min | ✅ Excelente |
| Sleep Score | 83 | ✅ Bueno |
| Horas sueño | 7h | ⚠️ Leve déficit vs 8h ideal |
| Pasos | 7390/día | ⚠️ Bajo vs 8000 objetivo |

> **Estado actual**: El usuario tiene buena capacidad de recuperación. FC reposo y VO2max indican buena condición cardiovascular. Sueño adecuado pero con margen de mejora.

---

## 3. Variables de Entrada

### Métricas de Hardware (vía Health Connect)

> **Fuente datos**: La app lee de Health Connect, que recibe de Zepp/Mi Fitness.
> Ver [USR-MET-01](../../usuario/metricas/hardware.md) para flujo completo.

> ✅ **Fuente HRV**: Kiviniemi et al. (2007) - `evidencia/sueno.md` § 7

| Variable | Criterio | Acción | Evidencia |
|----------|----------|--------|-----------|
| `HC_SLEEP_SCORE` (calculado) | **< 30** | Reducir carga ligeramente (<5%) | ⚠️ HEURÍSTICO |
| `HC_HR_REST` | **+10bpm vs baseline** | Considerar descanso | ⚠️ HEURÍSTICO |
| `HC_HR_REST` | **↑ 2+ días consecutivos** | Día de recuperación activa | ✅ Adaptado de Kiviniemi 2007 |

> ⚠️ **HRV no disponible**: Zepp no exporta RMSSD a Health Connect. Como alternativa, usamos:
> - Sleep Score (calculado desde duración 70% + eficiencia 30% — ver nota 2026-g más abajo, ya NO usa % de fases profundo/REM)
> - FC reposo elevada (indicador indirecto de estrés/fatiga)

### Métricas Subjetivas (USR-MET-02) — SOLO TRACKING (2026)
| Variable | Uso |
|----------|-----|
| `SUB_ENERGIA` | Se guarda y se muestra en progresión. **NO afecta el cálculo de carga** — decisión explícita del usuario. |
| `SUB_ESTRES` | Se guarda y se muestra en progresión. **NO afecta el cálculo de carga** — decisión explícita del usuario. |
| `SUB_DOMS` | Sentido común, no implementado como regla automática. |

> **Cambio (2026)**: Antes `SUB_ENERGIA`/`SUB_ESTRES` recortaban el factor del día
> (Capa 6, `calcularAjusteDia_`). El usuario pidió retirarlos del motor: los
> quiere registrar para poder mirarlos en retrospectiva ("apuntármelo"), no
> que el sistema los use para decidir por él. El motor ahora solo usa FC
> reposo (Kiviniemi 2007) y Sleep Score (Fullagar 2015) — ver §5 y §6.

## 4. Protocolo FC Reposo (Adaptado de Evidencia)

> ✅ **Fuente original**: Kiviniemi et al. (2007) usaba HRV. Adaptamos a FC reposo.
> La FC reposo elevada es indicador indirecto de estrés/recuperación incompleta.

```yaml
FC_AUTOREGULACION:
  fuente_datos: "Health Connect (HC_HR_REST)"
  
  medicion_original:  # Usuario ve en Zepp app
    momento: "Al despertar, antes de levantarse"
    duracion: "Automático (Amazfit mide durante la noche)"
  
  calculo_referencia:
    media_movil: "10 días anteriores"
    umbral_alto: "media + 10 bpm"
    
  decision:
    FC_≤_media: "Sesión planificada normal"
    FC_>_umbral_alto: "Reducir intensidad"
    tendencia_ascendente_2d: "Considerar recuperación activa"
```

> **Nota**: FC reposo elevada indica que el sistema nervioso simpático está activado (estrés, recuperación incompleta, enfermedad incipiente).

---

## 5. Lógica de Cálculo

> Combinación de FC reposo (adaptado de paper) + Sleep Score + heurísticas marcadas

```python
def calcular_ajuste(datos_usuario):
    ajuste = 1.0  # 100% = sesión normal
    
    # --- ADAPTADO DE EVIDENCIA (Kiviniemi 2007) ---
    # Usando FC reposo como proxy de HRV (Health Connect: HC_HR_REST)
    # PRIORIDAD 1: Tendencia ascendente → early return (no acumula)
    if datos_usuario.fc_tendencia == "ascendente_2d":
        return { factor: 0.70, tipo: "RECUPERACION_ACTIVA" }
    
    # PRIORIDAD 2: FC puntualmente alta
    if datos_usuario.fc_reposo > datos_usuario.fc_media_10d + 10:
        ajuste *= 0.80  # Reducción significativa
    
    # --- HEURÍSTICAS (marcar claramente) ---
    # Sleep Score: calculado desde HC_SLEEP_DURATION + fases (ESTIMADO, no es
    # el score real de Zepp — ver hardware.md §4). Umbral y magnitud
    # recalibrados (2026-d): el score puede hundirse en una noche buena con
    # fases atípicas, así que solo reacciona ante un score claramente malo.
    if datos_usuario.sleep_score < 30:
        ajuste *= 0.96  # ⚠️ HEURÍSTICO — antes <60 → ×0.90

    # Estrés/energía subjetivos: NO entran aquí (2026) — solo tracking, ver §3.

    return { factor: ajuste, tipo: "normal" si ajuste >= 1 sino "reducida" }
```

---

## 6. Progresión de Cargas

> ✅ **Fuentes**: ACSM (2009), Mann (2010), Rhea (2002) - `evidencia/periodizacion.md` §§ 11-13

### Criterio de Progresión (ACSM 2009)
```yaml
REGLA_SUBIR_PESO:
  condicion: "Completas 1-2 reps MÁS de las objetivo"
  aumento: "2-10%"
  ejemplo: "Si objetivo era 8 reps y haces 10, subir 2.5-5kg"
```

### Tiempos de Descanso (ACSM 2009)
| Objetivo | Descanso |
|----------|----------|
| Fuerza (1-6 RM) | **3-5 min** |
| Hipertrofia (8-12 RM) | 2-3 min | ⚠️ Superseded: Schoenfeld 2016 demostró 3 min > 1 min |
| Resistencia (15+ RM) | <1 min |

### Protocolo APRE Simplificado (Mann 2010)
```yaml
APRE_DIARIO:
  set_1: "10 reps @ 50% 6RM"
  set_2: "6 reps @ 75% 6RM"
  set_3: "máximo @ 100% 6RM"
  set_4: "máximo @ peso ajustado según set_3"
  
  ajuste_set_4:
    reps_0-2: "bajar 2.5-5 kg"
    reps_3-4: "bajar 0-2.5 kg"
    reps_5-7: "mantener"
    reps_8-12: "subir 2.5-5 kg"
    reps_13+: "subir 5-7.5 kg"
```

> **Resultado del paper**: APRE produjo **3-5× más mejoras** que periodización lineal.

### Progresión RIR Semanal (Helms)
```yaml
RIR_PROGRESION:
  semana_1: RIR 3-4 (RPE 6-7)
  semana_2: RIR 2-3 (RPE 7-8)
  semana_3: RIR 1-2 (RPE 8-9)
  semana_4: DELOAD
```

---

## 7. Reglas de Seguridad

> ⚠️ **Mayormente HEURÍSTICAS** - Usar criterio clínico

```yaml
BLOQUEOS_ABSOLUTOS:
  - hrv_muy_bajo: "< media - 2 SD"  # Extrapolación de Kiviniemi
  - dolor_agudo: "Usuario reporta dolor ≥7/10"
  - enfermedad: "Usuario reporta fiebre/malestar"
  
ALERTAS:
  - "3+ días sin recuperar HRV → Sugerir semana deload"
  - "Sleep score <50 → Priorizar descanso"
```

---

## 8. Uso en el Sistema

> **IMPORTANTE**: El motor NO almacena pesos en `ejercicios_plan`.
> Los pesos se calculan DINÁMICAMENTE al servir la sesión (`getSesionHoy_`).
> Esto hace el POST `guardarLog_` instantáneo (O(1), solo append).

### Función principal: `calcularPesoSugerido_(ejercicioId, ctx)`

Recibe un objeto de contexto con:
```yaml
ctx:
  ajusteDia: 0.70-1.0       # Factor Kiviniemi (CAPA 6)
  fase: VOL/FZA/DEF/MNT/DELOAD  # Fase actual del plan anual
  objetivoNutri: bulk/cut/mantener  # Helms 2014
  repsObjetivo: 10           # Reps planificadas (del plan)
  rirObjetivo: 2             # RIR planificado (cambia por semana, Helms 2016)
```

### Capas de ajuste (multiplicativas):

| Capa | Fuente | Qué hace |
|------|--------|----------|
| 1. BASE | ejercicios_log | **Mejor set** (máx reps+RIR) de la sesión más reciente del ejercicio |
| 2. APRE | Mann 2010 + ACSM 2009 | delta_capacidad → -10% a +10% |

> **Capa 1 — por qué el MEJOR set y no el último**: con series rectas a un RIR
> objetivo, el último set siempre tiene menos reps por fatiga acumulada. Usar el
> último set hacía que el motor lo leyera como "te quedaste corto" y bajara el
> peso aunque la sesión fuera perfecta → el peso se erosionaba solo
> (infraentrenamiento). El mejor set refleja la capacidad real del día, que es
> lo que debe guiar la doble progresión.
| 3. FASE | Bompa 2019 (§7) | Cap de progresión: VOL ±5%, FZA ±10%, DEF ±3%, MNT ±2.5%, DELOAD -12.5% |
| 4. NUTRICIÓN | Helms 2014 | En cutting, cap subida al 50% |
| 5. DESCANSO | ACSM 2009 (frecuencia) | >7d gap → ×0.95 (el tramo >14d→×0.90 se eliminó: con retención de 7 días nunca hay un log tan viejo → era código muerto) |
| 6. DÍA | Kiviniemi 2007 + Fullagar 2015 | FC/sueño → factor 0.72–1.0 |

> **Capa 6 — historial (2026)**: llegó a incluir estrés y energía subjetivos
> (factor mínimo apilado 0.55, −45%, más agresivo que un deload), con un suelo
> en 0.70 para limitarlo. El usuario pidió sacar estrés/energía del cálculo por
> completo (quiere solo trackearlos, ver §3) — con solo FC(×0.80) y sueño el
> mínimo posible ya quedaba muy por encima de 0.70, así que el suelo se quitó
> también (código muerto, mismo criterio que el fix de la Capa 5).
>
> **Recalibración sueño (2026-d)**: umbral <60→×0.90 bajado a <30→×0.96 (a
> petición del usuario) — el score es una estimación (ver §3) que penaliza con
> demasiada fuerza noches buenas con fases de sueño atípicas. Mínimo apilado
> actual: FC(×0.80) × sueño(×0.96) = 0.768.
>
> **Corrección fórmula del score (2026-g)**: el propio cálculo del score
> (`HealthConnectBridge.kt`, no vive en `Codigo.gs`) tenía un 3er factor —
> "cercanía de %profundo/%REM a un target de 18%/22.5%, penalización ×3/punto"
> — sin respaldo en `evidencia/sueno.md` (dato inventado, viola REGLA CERO) y
> causa muy probable de que el score saliera sistemáticamente bajo frente al
> de Zepp (los wearables de muñeca reparten fases por movimiento+FC, no EEG,
> raramente calzan con un "ideal" de laboratorio). Eliminado — ahora
> `score = duración(70%) + eficiencia(30%)`, los dos únicos factores con
> respaldo real (Fullagar 2015 / National Sleep Foundation para duración;
> eficiencia es un ratio medido, no un target inventado).

### Fórmula APRE (Capa 2):
```
delta_capacidad = (reps_mejor_set + RIR_percibido) − (reps_objetivo_tope + RIR_objetivo)
  reps_objetivo_tope = TOPE del rango ("8-10" → 10) — ACSM: progresas al superar el techo

delta ≤ -4  → -10%  (muy pesado, Mann: reps_0-2)
delta ≤ -2  → -5%   (pesado, Mann: reps_3-4)
delta ≤  0  → 0%    (en el objetivo o 1 por debajo → mantener)
delta 1..3  → +5%   (superas el objetivo por 1-3 → subir. ACSM: "1-2 reps MÁS → subir")
delta > 3   → +10%  (muy fácil, Mann: reps_13+)
```

### Doble progresión real: peso O reps, según el ejercicio (2026)

> ✅ **Fuente**: ACSM (2009) — la sobrecarga progresiva sube por DOS ejes
> (carga o repeticiones), no solo por kg. Estándar NSCA/ACSM para prescripción
> de fuerza.

**Antes**: el motor SOLO progresaba en kg, para todos los ejercicios por
igual — incluidos los de peso corporal/banda (Hollow hold, Wall angels, Band
pull-aparts, Rotación externa banda). Como esos siempre se registran con
`num_peso_usado_kg = 0` (no hay disco que añadir), la Capa 1 (`pesoBase <= 0`)
devolvía **siempre** "elige tu peso" — nunca progresaban, sesión tras sesión,
por muy bien que salieran las series. Código muerto para toda una categoría
de ejercicios.

**Ahora** (`esProgresionSinPeso_` + `calcularProgresionReps_` en Codigo.gs):
para ejercicios con equipamiento `Banda`/`Pared` (sin excepción en el catálogo
actual) y el caso explícito `EJE_HOLLOW` (`Suelo` es ambiguo: incluye también
Plancha lastrada, que sí carga peso real — se trata como excepción por ID en
vez de forzar una columna nueva para un único caso), la doble progresión
sube el **objetivo de reps/segundos** en vez del peso, usando la MISMA
fórmula `delta_capacidad` de la Capa 2:

```
delta_capacidad = (reps_mejor_set + RIR_percibido) − (reps_objetivo_tope + RIR_objetivo)

delta ≤ -4  → -2 unidades  (reps) / -10s (tiempo)
delta ≤ -2  → -1 unidad    (reps) / -5s  (tiempo)
delta ≤  0  → mantener
delta 1..3  → +1 unidad    (reps) / +5s  (tiempo)
delta > 3   → +2 unidades  (reps) / +10s (tiempo)
```

El paso de 5s para objetivos temporales ("30s") reutiliza el mismo incremento
que `getMovilidadMatutina_` usa para escalar hold times por tramo — consistencia
entre ambos sistemas de progresión sin peso. El factor del día (Capa 6) también
reduce el objetivo en un día de mala recuperación, igual que con el peso.

**Por qué NO se implementó de forma más amplia** (p. ej. "prefiere reps sobre
peso en fase VOL para todos los ejercicios de aislamiento"): el resto de
ejercicios con carga real (mancuernas/barra/máquina/polea) YA hacen doble
progresión de facto — el rango de reps del plan ("8-10") es fijo, así que el
usuario sube reps libremente DENTRO del rango a igual peso entre sesiones
(eso ya lo decide el usuario, no hace falta que el motor lo fuerce), y el
motor solo sube KG cuando se supera el TECHO del rango (Capa 2). Forzar
además una preferencia explícita "reps vs peso" en ejercicios con carga real
añadiría una capa de decisión sin un problema real que resolver — el único
hueco genuino era la categoría sin carga externa, ya cerrado arriba.

> **Auto-ajuste semanal**: Como `rirObjetivo` cambia cada semana (Helms 2016:
> sem1 RIR 3-4, sem2 RIR 2-3, sem3 RIR 1-2, sem4 deload), la fórmula
> se adapta automáticamente al microciclo sin lógica adicional.

### Registro de esfuerzo (RIR):
```yaml
# str_sensacion se retiró (limpieza 2026): los 4 botones de la app
# (Fácil/Bien/Duro/Fallo) YA fijan el RIR (3/2/1/0) 1:1, así que la sensación
# era el mismo dato duplicado. El motor usa directamente el RIR registrado.
```

### Cuándo se ejecuta:
1. **Al servir `getSesionHoy_`**: Para cada ejercicio de la sesión del día
2. Recibe contexto completo: fase actual, nutrición, RIR objetivo, ajuste del día

### Qué NO hace:
- NO escribe en `ejercicios_plan` (pesos son efímeros)
- NO genera sesiones ni selecciona ejercicios
- NO cambia series ni reps (definidas por fase en el template)
- NO es bloqueante: si falla, el usuario ve "Elige tu peso"

### Fallback sin red:
- La app usa el ÚLTIMO peso cacheado en Room
- Si es la primera sesión (peso = 0): "Elige tu peso" y el usuario introduce manualmente
- El peso manual se registra en ejercicios_log y sirve de base para la siguiente sugerencia

### Retorno enriquecido:
```json
{
  "peso": 82.5,
  "detalle": "80kg | ↑ fácil | Hipertrofia | → 82.5kg",
  "capas": {
    "base": 80, "ultimoReps": 11, "ultimoRIR": 3,
    "deltaCap": 2, "pctAPRE": 0.05, "nivelAPRE": "facil",
    "fase": "VOL", "faseNombre": "Hipertrofia",
    "factorDescanso": 1.0, "factorDia": 1.0
  }
}
```

> **Estado**: Motor COMPLETO. 6 capas multiplicativas. Evidencia en cada capa.