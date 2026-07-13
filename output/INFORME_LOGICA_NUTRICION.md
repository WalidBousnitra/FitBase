# Informe: Lógica de Nutrición en FitBase

> Análisis técnico de toda la lógica nutricional existente en el proyecto — desde la base de conocimiento (evidencia científica) hasta su implementación real en `Codigo.gs` (backend), Google Sheets (BBDD) y la app Android. Cada decisión de código se justifica con su fuente.

---

## 1. Arquitectura de 3 capas

```
knowledge_base/*.md  (evidencia + reglas)
        │  "qué debería pasar, y por qué"
        ▼
Codigo.gs (Apps Script)
        │  "qué pasa realmente, en cada request"
        ▼
Android app (Java) + Google Sheets (persistencia)
        │  "qué ve y guarda el usuario"
```

Esto importa porque **no todo lo que dice el knowledge_base está implementado**. Hay una brecha real y documentada entre la "receta ideal" (markdown) y el "motor real" (código) — se detalla en la §9.

---

## 2. Capa 1 — Base de conocimiento (evidencia + reglas)

| Archivo | Rol | Usado en código? |
|---|---|---|
| `evidencia/nutricion.md` (EVI-11) | Papers: Mifflin 1990, Helms 2014, Iraki 2019, Barakat 2020, Chaouachi 2009 | ✅ Sí — fuente directa de `getMacrosHoy_` |
| `reglas/nutricion/motor_dieta.md` (REG-NUT-01) | Traduce los papers a fórmulas y rangos concretos | ✅ Sí — es el "spec" que `getMacrosHoy_` implementa |
| `reglas/nutricion/preferencias.md` (REG-NUT-02) | Filtros halal, favoritos, logística de cocina | ❌ **No** — ver §9 |
| `usuario/perfil/cultura.md` (USR-PER-01) | Halal, Ramadán, tradiciones (cuscús, Eid) | ⚠️ **Parcial** — solo la parte de Ramadán/hidratación está en código; el filtrado de alimentos no |
| `usuario/biometria.md` (USR-02) | Peso, altura, edad, % grasa de referencia | ✅ Sí — son los valores de *fallback* hardcodeados |
| `reglas/logica/base_datos.md` (REG-LOG-02) | Qué se persiste y qué no | ✅ Sí — coincide 1:1 con las hojas reales |

---

## 3. Capa 2 — Base de datos (qué se guarda y por qué)

### Decisión clave: **no existe una hoja de "comidas" ni de "consumo"**

`base_datos.md §1`: *"Comida: no se guarda en BBDD; solo visual diaria en app."*

**Justificación real (encontrada en el código, `getMacrosHoy_` líneas 491-501):**
> *"La app no controla horarios de comida (nutrición es de solo lectura, FatSecret/Health Connect)"*

Es decir: el registro de qué comes ya lo hace **FatSecret** (app de terceros) y llega a la app vía **Health Connect** (`NutritionRecord`). FitBase no reimplementa un diario de comidas — solo lee lo que ya existe. Por eso Sheets nunca necesita una tabla de alimentos: sería una duplicación sin valor.

### La única hoja relevante para nutrición: `metricas_zepp`

| Columna | Para qué sirve en el motor de dieta |
|---|---|
| `num_peso_kg` | Recalcula BMR cada día (el peso cambia semana a semana) |
| `num_grasa_pct` | Recalcula la masa magra real para la proteína de fase **cut** (Helms 2014 la pide en g/kg LBM, no peso total) |

**Por qué una fila por día y no una tabla de "pesajes"**: `guardarMetricas_` hace *upsert* por fecha con **merge de campos** — si el peso llega solo (báscula) y el sueño llega por separado (reloj), no se pisan entre sí. Se leyó la implementación exacta: antes de escribir, lee la fila existente de ese día y solo sobreescribe los campos que llegan con dato nuevo.

### Por qué NO se persiste `metricas_subjetivas` en el cálculo de macros

`base_datos.md §3.2` es explícito: energía/estrés se guardan (trackeo puro) pero **no entran en `calcularAjusteDia_`** ni en `getMacrosHoy_` — decisión explícita del usuario ("solo quiero trackearlo"), documentada como tal, no un descuido.

---

## 4. Capa 3 — El motor real: `getMacrosHoy_()` (Codigo.gs, líneas 419-524)

Aquí está el 90% de la lógica nutricional del proyecto. Desglose completo, decisión por decisión:

### 4.1 Peso de entrada

```js
const peso = getPesoActual_();
```
Lee la **última fila con `num_peso_kg > 0`** de `metricas_zepp`, recorriendo de abajo hacia arriba. Si la hoja no existe o no hay ningún peso registrado todavía → **fallback a 78.2 kg** (el valor exacto de `biometria.md`).

**Justificación de este patrón (fallback = dato documentado, no un número arbitrario)**: garantiza que si el pipeline Zepp→Health Connect falla un día, el usuario sigue viendo macros razonables en vez de un error o un `NaN`. Es la misma razón por la que `getGrasaActual_()` cae a `18.9` — el % de grasa exacto medido el 18/06/2026.

### 4.2 BMR — Mifflin-St Jeor (1990)

```js
const bmr = (10 * peso) + (6.25 * altura) - (5 * edad) + 5;
```
**Por qué esta fórmula y no Harris-Benedict**: `evidencia/nutricion.md §2` cita el propio paper — Mifflin-St Jeor tiene R²=0.71 sobre 498 sujetos, mientras que Harris-Benedict **sobreestima un 5%**. Con un objetivo de bulk "limpio" (minimizar grasa ganada), sobreestimar el gasto es el peor error posible: te haría comer de más pensando que es mantenimiento.

`altura` y `edad` están hardcodeados (188 cm, 24 años) porque son datos fijos de una sola persona (app mono-usuario) — no tiene sentido una tabla para 2 valores que no cambian en 11 meses.

### 4.3 TDEE — factor de actividad 1.55

```js
const tdee = Math.round(bmr * 1.55);
```
**Por qué 1.55 ("Moderado") y no 1.725 ("Activo"), entrenando 6 días/semana**: `motor_dieta.md §3` da 3 razones explícitas, replicadas en el comentario del código:
1. El trabajo es 100% sedentario (oficina).
2. La natación (2x/semana) es de baja intensidad — no compensa hacia "Activo".
3. **Es mejor infraestimar y corregir al alza** que sobreestimar y acumular grasa de forma silenciosa — asimetría de riesgo intencional.

Y una regla de auto-corrección: *si el peso se estanca 2+ semanas en bulk → subir a 1.65 manualmente.* Esto NO está automatizado en el código (es una instrucción para el humano, no una condición en `getMacrosHoy_`) — otra brecha documentada en §9.

### 4.4 Selección de objetivo nutricional (bulk / cut / mantener)

```js
const plan = getPlanAnual_();
let obj = 'bulk', mult = 1.15, protRatio = 2.0;
if (plan.fase_actual) {
  const n = plan.fase_actual.str_objetivo_nutri || 'bulk';
  if (n === 'cut') { mult = 0.80; obj = 'cut'; }
  else if (n === 'mantener') { mult = 1.0; protRatio = 2.0; obj = 'mantener'; }
}
```
**Decisión de diseño importante**: el objetivo nutricional NO vive en `getMacrosHoy_` — vive en la hoja `plan_anual`, columna `str_objetivo_nutri`, y este es solo el consumidor. Esto significa que **cambiar de fase (bulk→cut) es un cambio de datos, no de código** — coherente con la filosofía "NADA HARDCODEADO" del manifest.

**Por qué `DELOAD` no tiene rama propia aquí**: confirmado también en `base_datos.md` — el deload solo afecta al *entrenamiento* (menos series), nunca a las calorías. Nutricionalmente, un deload se trata como el `str_objetivo_nutri` que tenga asignada esa fase (normalmente `mantener`).

### 4.5 Ajuste calórico por objetivo

| Fase | Multiplicador | Justificación |
|---|---|---|
| Bulk | `× 1.15` | Iraki 2019 recomienda 10-20% de superávit para intermedios/avanzados; 15% es el punto medio, elegido explícitamente por ser conservador (avanzados "deben ser más conservadores — menor potencial de ganancia") |
| Cut | `× 0.80` | Dentro del rango de `motor_dieta.md §4` (0.80-0.90); se elige el extremo más agresivo del rango |
| Mantener | `× 1.0` | Trivial — TDEE sin ajuste |

### 4.6 Ajuste por actividad diaria (pasos)

```js
const pasos = getPasosHoy_();
if (pasos > 12000) calorias += 175;
```
`motor_dieta.md §6` dice "+150-200 kcal si pasos > 12000" — el código toma **175, el punto medio exacto**. Este es el único ajuste "dinámico intra-día" real que existe: el resto de reglas de §6 (repartir carbos pre/post-entreno) **no están implementadas** — ver el comentario de auditoría NUT-03 en el propio código: no hay tracking de comidas, así que no hay forma de hacer cumplir un reparto horario. Decisión honesta: se documenta como alcance real, no como bug pendiente.

### 4.7 Proteína — la decisión más compleja del motor

```js
if (obj === 'cut') {
  var grasaPctActual = getGrasaActual_();
  var lbm = peso * (1 - grasaPctActual / 100);
  protG = Math.round(lbm * 2.7);
} else {
  protG = Math.round(peso * protRatio); // protRatio = 2.0
}
```

- **Bulk/Mantener**: `2.0 g/kg de peso total`. Está dentro del rango de Iraki 2019 (1.6-2.2 g/kg) — se eligió el extremo alto del rango, coherente con la prioridad P1 (estética/V-taper) que premia retención/ganancia de tejido magro.
- **Cut**: aquí está el fix más importante documentado en todo el archivo (**auditoría NUT-02**). Helms 2014 especifica la proteína de déficit en **g/kg de masa magra (LBM)**, no peso total (2.3-3.1 g/kg LBM). La versión anterior aplicaba un ratio fijo sobre el peso total asumiendo un %BF constante que nunca se releía. Ahora:
  1. Se lee el `%grasa` **más reciente real** de `metricas_zepp` (no un valor fijo).
  2. Se calcula `LBM = peso × (1 - %grasa/100)`.
  3. Se aplica `2.7 g/kg LBM` — el punto medio exacto del rango Helms (2.3-3.1).

**Por qué esto importa nutricionalmente**: si el usuario baja de 18.9% a 15% de grasa a mitad de un cut, su LBM sube en términos relativos y su proteína objetivo debe subir con ella — de lo contrario, un %BF fijo antiguo iría infra-dosificando proteína justo cuando más protección muscular se necesita (déficit calórico = mayor riesgo catabólico).

### 4.8 Grasas

```js
if (obj === 'cut') {
  grasaG = Math.round(calorias * 0.25 / 9); // 25% kcal
} else {
  grasaG = Math.round(peso * 1.0); // 1.0 g/kg
}
```
- **Bulk/Mantener**: 1.0 g/kg — punto medio del rango Iraki (0.5-1.5 g/kg).
- **Cut**: 25% de las calorías — punto medio del rango Helms (15-30%), calculado en % de kcal (no g/kg) porque en déficit las calorías totales ya son más bajas, y fijar un g/kg fijo podría dejar muy poco espacio para carbohidratos peri-entreno.

### 4.9 Carbohidratos — "el resto"

```js
const carbosG = Math.round((calorias - protG * 4 - grasaG * 9) / 4);
```
Coherente con **todos** los papers citados (Helms, Iraki): los carbohidratos nunca se calculan con un rango propio — son *lo que queda* tras fijar proteína (prioridad máxima) y grasa (mínimo hormonal). Es la jerarquía de decisión estándar en nutrición deportiva basada en evidencia, no una elección arbitraria del proyecto.

### 4.10 Agua

```js
var agua = Math.round(peso * 35) + (esEntreno ? 500 : 0);
if (ramadanActivo) agua = Math.round(agua * 1.15);
```
- Base: 35 ml/kg/día — rango EFSA citado en `evidencia/vitalidad.md`.
- +500 ml en días de entreno — compensación de sudoración, valor heurístico redondo (no de un paper específico).
- **+15% en Ramadán**: es el único ajuste nutricional real que Ramadán aplica a un número (ver §4.12). Justificación explícita en el comentario: *"misma agua total, concentrada en menos horas de ventana abierta"* — no es más agua en términos absolutos ideales, es compensar que solo se puede beber entre Iftar y Suhur.

### 4.11 Pasos objetivo por fase

```js
var pasosPorFase = { VOL: 8000, FZA: 8000, DEF: 10000, MNT: 9000, DELOAD: 7000 };
```
No es parte del cálculo de macros en sí, pero viaja en la misma respuesta. Fuente citada: `programacion.md §13, Wilson 2012`. Nótese la coherencia nutricional-conductual: **DEF (déficit) pide más pasos** (10000) que VOL (8000) — el NEAT extra ayuda al déficit sin tocar el entrenamiento de fuerza; DELOAD pide menos (7000, recuperación).

### 4.12 Ramadán — advisory, no reparto real

```js
var hoy = fechaHoy_();
var ramadanActivo = esRamadan_(hoy);
```
`esRamadan_` compara la fecha de hoy contra `RAMADAN_FECHAS` — **hardcodeado a mano** (`{ inicio: '2027-02-08', fin: '2027-03-10' }`), con un comentario explícito: *"el calendario islámico es lunar — hay que actualizarlas cada año, no se calculan."* Es una decisión de ingeniería correcta: calcular el calendario lunar islámico por software es un problema no trivial (depende de avistamiento lunar real en algunos casos) — mejor confirmarlo una vez al año que fabricar una aproximación astronómica poco fiable.

**Qué SÍ cambia en Ramadán**: solo el agua (+15%, ya visto). **Qué NO cambia**: calorías y macros totales — el comentario del código es tajante: *"cultura.md §8 NO pide cambiar el total diario de kcal/macros durante Ramadán — solo colapsar la ventana de comidas."* Como la app no tiene forma de forzar que comas en una ventana horaria concreta (no hay registro de comidas propio), el fix es **advisory**: un texto (`ramadan_nota`) que la UI muestra, no una función que reparta nada. Se aplica el mismo criterio ya usado en `getSesionHoy_()` con `ramadan_nota` para entreno — consistencia de patrón en toda la base de código.

---

## 5. Otros endpoints relacionados con nutrición

| Endpoint | Función | Qué añade sobre `macros_hoy` |
|---|---|---|
| `GET accion=vista_manana` | `getVistaMañana_()` | Empaqueta `macros` (solo objetivo, sin consumo) junto con sueño, fase, cardio, movilidad y Ramadán — es lo que pinta la pantalla Home de un solo golpe |
| `GET accion=preview_ramadan` | `getRamadanPreview_()` | Reutiliza `getRamadanInfo_()` con una fecha real dentro de Ramadán en vez de "hoy" — solo para poder ver el banner en desarrollo, fuera de temporada de Ramadán. No inventa contenido nuevo |
| `GET accion=cambio_fase` | `getCambioFase_()` | No es nutrición per se, pero expone `nutri` (bulk/cut/mantener) de la fase anterior y la nueva al hacer la transición |

**Caché**: `macros_hoy` y `vista_manana` se cachean 30s (`CACHE_TTL`) y se invalidan explícitamente en cada POST (`guardar_log`, `guardar_metricas`, etc.) — así que si te pesas por la mañana, el peso nuevo se refleja en el próximo cálculo de macros sin esperar el TTL completo.

---

## 6. Capa Android — cómo se refleja todo esto en la app

### 6.1 El objetivo llega del backend; el consumo llega de Health Connect — **nunca se mezclan en el servidor**

```
Codigo.gs → MacrosResponse{calorias_objetivo, proteina_g, ...}   (objetivo)
FatSecret → Health Connect → HealthConnectReader → HomeViewModel  (consumido)
                                     │
                                     ▼
                    HomeActivity.recalcularMacrosRestantes()
                    restante = objetivo - consumido   (se calcula EN EL CLIENTE)
```

Esta separación es deliberada y está documentada en el propio DTO (`MacrosResponse.java`):
```java
// Ramadán (cultura.md §8) — advisory únicamente: las kcal/macros totales
// NO cambian, solo se reparten distinto...
```
y en `HomeActivity.java`: *"Macros consumidas (Health Connect / FatSecret) → calcular restantes"*.

**Por qué el backend siempre manda `calorias_consumidas: 0`** en su respuesta cruda (línea 515 de `Codigo.gs`): el backend **no sabe ni le importa** cuánto has comido — eso vive enteramente en el teléfono (Health Connect), consistente con la memoria de este proyecto de "solo valores literales de Zepp/HC, nunca sintéticos". El backend solo calcula el objetivo; la resta la hace la Activity.

### 6.2 `MacrosResponse.java` — helpers ya preparados para "restante"

```java
public int getCaloriasRestantes() { return Math.max(0, caloriasObjetivo - caloriasConsumidas); }
```
Estos métodos existen en el modelo pero, en la práctica, `HomeActivity` recalcula manualmente en `recalcularMacrosRestantes()` combinando el LiveData del ViewModel (consumido, de Health Connect) con `vista.getMacros()` (objetivo, del backend) — dos fuentes distintas que solo confluyen en la Activity.

### 6.3 `HealthConnectBridge.kt` / `HealthConnectReader.java` — el puente real con FatSecret

- Lee `NutritionRecord` (calorías + macros) escritos por FatSecret en Health Connect.
- Lee `WeightRecord` y `BodyFatRecord` — estos alimentan el **otro lado** del sistema: `DailySyncManager` los sube vía `POST guardar_metricas` a `metricas_zepp`, que es exactamente lo que `getPesoActual_()`/`getGrasaActual_()` leen al día siguiente para recalcular BMR y la proteína de cut. **Aquí se cierra el círculo**: báscula → Health Connect → Sheets → motor de dieta → nueva proteína objetivo.

### 6.4 Refresco automático

`HomeViewModel` refresca Health Connect cada 15 segundos (`INTERVALO_REFRESCO_MS`) mientras la app está abierta — así "macros restantes" se actualiza solo si registras algo en FatSecret sin tener que cerrar y reabrir FitBase.

---

## 7. Diagrama de flujo completo (objetivo vs. consumo)

```
                    ┌─────────────────────┐
                    │  plan_anual (Sheets)│  ← fase actual: bulk/cut/mantener
                    └──────────┬──────────┘
                               │
┌──────────────┐   peso/grasa │
│ metricas_zepp│──────────────┤
│   (Sheets)   │              ▼
└──────▲───────┘      getMacrosHoy_()  ← Mifflin-St Jeor + Iraki/Helms + Ramadán(+15% agua)
       │                      │
       │              calorias/prot/carbos/grasas/agua OBJETIVO
       │                      │
       │                      ▼
       │              MacrosResponse (API)
       │                      │
       │                      ▼
       │              HomeActivity (UI objetivo)
       │                      │
       │              restante = objetivo − consumido
       │                      ▲
       │                      │
       │           HealthConnectReader (consumido: FatSecret→HC)
       │                      │
┌──────┴───────┐   Weight/BodyFat   NutritionRecord
│ Báscula Xiaomi│──────────────┘         │
└───────────────┘                  FatSecret (app 3rd party)
```

---

## 8. Lo que NO está implementado (y por qué es correcto que no lo esté)

Esta es la parte más honesta del análisis: hay contenido extenso en `knowledge_base` que **no tiene ninguna línea de código correspondiente**. Confirmado con una búsqueda exhaustiva en `Codigo.gs` (0 coincidencias de "halal", "cerdo", "preferencias_alim", "receta", "comida_id").

| Contenido del knowledge_base | ¿Implementado? | Por qué (según la propia documentación) |
|---|---|---|
| Filtros halal (`preferencias.md §3`, `cultura.md §8`) | ❌ No | `cultura.md` línea 358 lo dice explícitamente: *"la app no genera ni sugiere alimentos — la nutrición es de solo lectura desde FatSecret/Health Connect"*. Es referencia para el USUARIO (o para mí, generando manualmente `ENTREGABLE_6_COMIDAS_FIJAS.html`), no una función del motor |
| Alimentos favoritos/excluidos (`preferencias.md §4-5`) | ❌ No | Mismo motivo — no hay generador de menús en código, solo objetivos numéricos |
| Reparto de carbos pre/post-entreno (`motor_dieta.md §6`) | ❌ No | Auditoría NUT-03 en el propio código: sin tracking de comidas no hay forma de hacer cumplir un timing intra-día |
| Auto-subir factor de actividad a 1.65 si el peso se estanca (`motor_dieta.md §3`) | ❌ No | Es una instrucción para ajuste manual del humano, no una condición programada |
| Ramadán: reparto proteína en Iftar/Cena/Suhur (`cultura.md §5`) | ⚠️ Solo como texto (`ramadan_nota`) | Advisory — no hay registro de comidas para aplicarlo de verdad |
| Cuscús de domingo, Eid, dulces marroquíes (`cultura.md §6`) | ❌ No | Documentación de contexto cultural para uso humano; no hay lógica de "día especial" en `getMacrosHoy_` más allá de Ramadán/Eid (que sí tiene fechas) |

**Conclusión de esta sección**: el motor de dieta real de FitBase resuelve exactamente un problema — **"¿cuántas kcal/macros/agua necesito hoy, dado mi peso, mi fase y si es Ramadán?"** — y lo resuelve con rigor (fórmulas validadas, LBM real, imputación de datos). Todo lo demás del knowledge_base de nutrición (qué comer, con qué ingredientes, filtrado cultural) es **material de consulta para generar contenido bajo demanda** (como los entregables de comidas), no lógica que corra en producción.

---

## 9. Tabla de trazabilidad (decisión de código → evidencia)

| Línea de código | Decisión | Fuente exacta |
|---|---|---|
| `bmr = (10×peso)+(6.25×altura)-(5×edad)+5` | Fórmula BMR | Mifflin et al. 1990, R²=0.71 |
| `tdee = bmr × 1.55` | Factor de actividad | `motor_dieta.md §3`, heurístico justificado (sedentario + natación baja intensidad) |
| `mult = 1.15` (bulk) | Superávit 15% | Iraki 2019, rango 10-20%, punto medio conservador |
| `mult = 0.80` (cut) | Déficit 20% | `motor_dieta.md §4`, rango 0.80-0.90, extremo agresivo |
| `protG = peso × 2.0` (bulk/mantener) | Proteína 2.0 g/kg | Iraki 2019, rango 1.6-2.2, extremo alto |
| `protG = LBM × 2.7` (cut) | Proteína sobre masa magra | Helms 2014, rango 2.3-3.1 g/kg LBM, punto medio |
| `grasaG = peso × 1.0` (bulk/mantener) | Grasa 1.0 g/kg | Iraki 2019, rango 0.5-1.5 |
| `grasaG = calorias × 0.25/9` (cut) | Grasa 25% kcal | Helms 2014, rango 15-30% |
| `carbosG = resto` | Carbohidratos = remanente | Helms 2014 + Iraki 2019 (ambos coinciden: carbos = resto) |
| `agua = peso × 35 (+500 entreno)` | Hidratación | EFSA / `evidencia/vitalidad.md` |
| `agua × 1.15` en Ramadán | +15% hidratación | Chaouachi et al. 2009 (deshidratación diurna, recuperación nocturna) |
| `pasos > 12000 → +175 kcal` | Ajuste NEAT | `motor_dieta.md §6`, rango 150-200, punto medio |
| `pasosPorFase = {DEF:10000, VOL:8000...}` | Pasos objetivo por fase | `programacion.md §13`, Wilson 2012 |

---

## 10. Observaciones menores (no bugs, solo notas de código)

1. **`protRatio` es redundante en la rama `mantener`**: se declara con valor por defecto `2.0` y luego se reasigna explícitamente a `2.0` otra vez dentro del `if (n === 'mantener')`. No cambia el resultado — es una asignación que no hace falta, pero tampoco es incorrecta.
2. **Todos los valores de fallback (78.2 kg, 18.9% grasa, 188 cm, 24 años) coinciden exactamente con `biometria.md`** — buena señal de que REGLA CERO se respeta también en los casos límite (Sheets vacía / sin conexión), no solo en el camino feliz.
3. El propio código está **auto-documentado con IDs de auditoría** (NUT-01, NUT-02, NUT-03, NUT-04, RAM-01, DATA-01, DATA-02) que explican qué se corrigió y por qué — esto facilitó mucho este análisis, ya que casi cada decisión no trivial tiene su justificación por escrito junto al código, no solo en el knowledge_base.

---

*Fuentes revisadas para este informe: `manifest.md`, `motor_dieta.md`, `evidencia/nutricion.md`, `preferencias.md`, `cultura.md`, `biometria.md`, `base_datos.md`, `Codigo.gs` (líneas 1-175, 400-530, 840-1000, 1990-2075), `Constants.java`, `MacrosResponse.java`, `HomeActivity.java`, `HomeViewModel.java`, `HealthConnectBridge.kt`, `HealthConnectReader.java`, `DailySyncManager.java`.*
