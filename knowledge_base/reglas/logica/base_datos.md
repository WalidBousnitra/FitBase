---
id: "REG-LOG-02"
nombre: "Arquitectura de Base de Datos"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["SYS-00", "REG-LOG-01", "REG-NUT-01"]
tags: ["reglas", "logica", "base-datos", "sheets"]
---

# Arquitectura de Base de Datos

## 1. Alcance
Estructura y reglas para la persistencia de datos en Google Sheets, diseñada para soportar los tres flujos principales de la app.

## 2. Flujos de Datos

```
┌─────────────────────────────────────────────────────────────────┐
│ FLUJO 1: MAÑANA (Auto/Semi-auto)                                │
│ Health Connect → [sueño, FC reposo, pasos] → metricas_zepp      │
│ Usuario → [peso (si no synced)] → peso_log                      │
│ Motor → Calcula ajuste del día → ready para gym                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ FLUJO 2: GYM (Interactivo)                                      │
│ Motor → Genera sesión ajustada → sesiones_plan                  │
│ App → Muestra ejercicio + carga sugerida                        │
│ Usuario → Feedback (peso, reps, RIR) → ejercicios_log           │
│ Motor → Usa feedback para ajustar próximas sesiones             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ FLUJO 3: NUTRICIÓN (Manual)                                     │
│ Usuario → Log macros → comidas_log                              │
│ Usuario → Adherencia supps → suplementos_log                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Hojas de Google Sheets

### 3.1 USUARIOS (Configuración)

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `user_id` | STRING | PK, único | `USR_001` |
| `str_nombre` | STRING | Nombre display | `Usuario` |
| `date_nacimiento` | DATE | Fecha nacimiento | `2001-07-20` |
| `num_altura_cm` | NUMBER | Altura en cm | `188` |
| `str_sexo` | STRING | `M` / `F` | `M` |
| `str_objetivo` | STRING | bulk/cut/mantener | `bulk` |
| `num_dias_entreno` | NUMBER | Días/semana | `4` |
| `str_split` | STRING | Tipo de split | `Upper/Lower/Push/Pull` |
| `bool_ramadan` | BOOLEAN | Practica Ramadán | `true` |
| `bool_halal` | BOOLEAN | Dieta halal | `true` |
| `date_creado` | DATETIME | Timestamp creación | `2026-06-18T10:00:00` |
| `date_modificado` | DATETIME | Última modificación | `2026-06-18T10:00:00` |

---

### 3.2 METRICAS_ZEPP (Auto - Mañana)

> Datos sincronizados automáticamente del Amazfit GTS 4 vía Health Connect

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `metrica_id` | STRING | PK | `ZEP_20260618_001` |
| `user_id` | STRING | FK → usuarios | `USR_001` |
| `date_fecha` | DATE | Fecha de los datos | `2026-06-18` |
| `num_sleep_score` | NUMBER | Sleep Score (0-100) | `83` |
| `num_sleep_horas` | NUMBER | Horas dormidas | `7.2` |
| `num_sleep_deep_min` | NUMBER | Minutos sueño profundo | `85` |
| `num_sleep_rem_min` | NUMBER | Minutos REM | `95` |
| `num_hrv_rmssd` | NUMBER | HRV en ms | `42` |
| `num_hr_reposo` | NUMBER | FC reposo | `53` |
| `num_readiness` | NUMBER | Readiness Score (0-100) | `78` |
| `num_stress_avg` | NUMBER | Estrés promedio día anterior | `35` |
| `num_pasos_ayer` | NUMBER | Pasos día anterior | `7390` |
| `num_calorias_activas` | NUMBER | Calorías activas | `320` |
| `date_sync` | DATETIME | Timestamp de sync | `2026-06-18T07:30:00` |

**Campos calculados (Apps Script)**:
```javascript
// Media móvil 10 días para HRV (Kiviniemi)
num_hrv_media_10d = AVERAGE(últimos 10 días)
num_hrv_sd_10d = STDEV(últimos 10 días)
bool_hrv_bajo = (num_hrv_rmssd < num_hrv_media_10d - num_hrv_sd_10d)
```

---

### 3.3 PESO_LOG (Semi-auto - Mañana)

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `peso_id` | STRING | PK | `PES_20260618_001` |
| `user_id` | STRING | FK → usuarios | `USR_001` |
| `date_fecha` | DATE | Fecha medición | `2026-06-18` |
| `num_peso_kg` | NUMBER | Peso en kg | `78.2` |
| `num_grasa_pct` | NUMBER | % grasa (si disponible) | `18.9` |
| `num_musculo_kg` | NUMBER | Masa muscular (si disponible) | `63.4` |
| `str_fuente` | STRING | `manual` / `smart_scale` | `manual` |
| `str_condiciones` | STRING | Notas (ayunas, post-cardio...) | `ayunas` |
| `date_creado` | DATETIME | Timestamp | `2026-06-18T07:35:00` |

**Campos calculados**:
```javascript
// Media móvil 7 días para suavizar fluctuaciones
num_peso_media_7d = AVERAGE(últimos 7 días)
num_tendencia = (peso_hoy - peso_hace_7d) / 7  // kg/día
```

---

### 3.4 SESIONES_PLAN (Motor → App)

> Sesiones generadas por el motor para cada día

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `sesion_id` | STRING | PK | `SES_20260618_001` |
| `user_id` | STRING | FK → usuarios | `USR_001` |
| `date_fecha` | DATE | Fecha planificada | `2026-06-18` |
| `str_tipo` | STRING | Tipo de día (Push/Pull/Upper/Lower) | `Push` |
| `num_semana_meso` | NUMBER | Semana del mesociclo (1-4) | `2` |
| `str_fase` | STRING | Fase (acumulación/intensificación/deload) | `acumulacion` |
| `num_ajuste_volumen` | NUMBER | Factor aplicado por HRV/sueño | `0.90` |
| `str_razon_ajuste` | STRING | Por qué se ajustó | `HRV bajo` |
| `num_duracion_est_min` | NUMBER | Duración estimada | `75` |
| `bool_completada` | BOOLEAN | ¿Se completó? | `false` |
| `date_inicio` | DATETIME | Hora de inicio real | `null` |
| `date_fin` | DATETIME | Hora de fin real | `null` |
| `date_creado` | DATETIME | Cuándo se generó | `2026-06-18T07:30:00` |

---

### 3.5 EJERCICIOS_PLAN (Motor → App)

> Ejercicios planificados para cada sesión

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `plan_id` | STRING | PK | `PLA_20260618_001` |
| `sesion_id` | STRING | FK → sesiones_plan | `SES_20260618_001` |
| `ejercicio_id` | STRING | FK → ejercicios_catalogo | `EJE_PRESS_BANCA` |
| `num_orden` | NUMBER | Orden en la sesión | `1` |
| `num_series_plan` | NUMBER | Series planificadas | `4` |
| `num_reps_plan` | STRING | Reps objetivo (rango) | `8-10` |
| `num_peso_sugerido_kg` | NUMBER | Peso sugerido por motor | `70` |
| `num_rir_objetivo` | NUMBER | RIR objetivo | `2` |
| `num_descanso_seg` | NUMBER | Descanso entre series | `120` |
| `str_notas` | STRING | Instrucciones especiales | `Control excéntrico 3s` |
| `bool_es_warmup` | BOOLEAN | ¿Es serie de calentamiento? | `false` |

---

### 3.6 EJERCICIOS_LOG (Usuario → Motor)

> **FEEDBACK DEL USUARIO** - Alimenta el motor de progresión

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `log_id` | STRING | PK | `LOG_20260618_001` |
| `plan_id` | STRING | FK → ejercicios_plan | `PLA_20260618_001` |
| `sesion_id` | STRING | FK → sesiones_plan | `SES_20260618_001` |
| `ejercicio_id` | STRING | FK → ejercicios_catalogo | `EJE_PRESS_BANCA` |
| `num_serie` | NUMBER | Número de serie | `1` |
| `num_peso_usado_kg` | NUMBER | Peso real usado | `72.5` |
| `num_reps_completadas` | NUMBER | Reps reales | `9` |
| `num_rir_percibido` | NUMBER | RIR percibido por usuario | `1` |
| `num_rpe` | NUMBER | RPE (alternativa a RIR) | `9` |
| `str_sensacion` | STRING | `facil` / `bien` / `duro` / `fallo` | `bien` |
| `str_notas` | STRING | Notas del usuario | `Codo molestó última rep` |
| `bool_dolor` | BOOLEAN | ¿Hubo dolor? | `false` |
| `str_zona_dolor` | STRING | Si dolor, dónde | `null` |
| `date_timestamp` | DATETIME | Cuándo se registró | `2026-06-18T18:45:00` |

**Uso por el motor**:
```javascript
// Regla ACSM: Si completó +1-2 reps sobre objetivo con RIR>objetivo → subir peso
if (reps_completadas >= reps_plan_max + 1 && rir_percibido >= rir_objetivo) {
  peso_proximo = peso_usado * 1.025  // +2.5%
}

// Regla APRE: Ajuste dinámico según rendimiento
if (reps_completadas >= 13) peso_proximo += 5  // kg
else if (reps_completadas >= 8) peso_proximo += 2.5
else if (reps_completadas <= 4) peso_proximo -= 2.5
```

---

### 3.7 PROGRESION_LOG (Histórico)

> Historial de progresión por ejercicio para visualización y análisis

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `prog_id` | STRING | PK | `PRO_20260618_001` |
| `user_id` | STRING | FK → usuarios | `USR_001` |
| `ejercicio_id` | STRING | FK → ejercicios_catalogo | `EJE_PRESS_BANCA` |
| `date_fecha` | DATE | Fecha del registro | `2026-06-18` |
| `num_1rm_estimado` | NUMBER | 1RM estimado ese día | `90` |
| `num_peso_trabajo` | NUMBER | Peso de trabajo usado | `72.5` |
| `num_volumen_total` | NUMBER | Peso × reps × series | `2610` |
| `num_reps_max` | NUMBER | Máximas reps en una serie | `10` |
| `str_pr_tipo` | STRING | Si fue PR: `1rm` / `reps` / `volumen` | `null` |

---

### 3.8 COMIDAS_LOG (Nutrición)

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `comida_id` | STRING | PK | `COM_20260618_001` |
| `user_id` | STRING | FK → usuarios | `USR_001` |
| `date_fecha` | DATE | Fecha | `2026-06-18` |
| `str_tipo_comida` | STRING | desayuno/comida/cena/snack | `comida` |
| `num_calorias` | NUMBER | Kcal totales | `980` |
| `num_proteina_g` | NUMBER | Gramos proteína | `50` |
| `num_carbos_g` | NUMBER | Gramos carbohidratos | `150` |
| `num_grasas_g` | NUMBER | Gramos grasas | `25` |
| `bool_pre_entreno` | BOOLEAN | ¿Fue pre-entreno? | `false` |
| `bool_post_entreno` | BOOLEAN | ¿Fue post-entreno? | `true` |
| `str_notas` | STRING | Descripción/notas | `Arroz con pollo` |
| `date_hora` | DATETIME | Hora de la comida | `2026-06-18T14:30:00` |

**Resumen diario (calculado)**:
```javascript
// Comparar con objetivo de motor_dieta.md
total_kcal = SUM(calorias del día)
total_prot = SUM(proteina del día)
adherencia_kcal = total_kcal / objetivo_kcal  // % cumplimiento
adherencia_prot = total_prot / objetivo_prot
```

---

### 3.9 HIDRATACION_LOG (Agua)

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `hidra_id` | STRING | PK | `HID_20260618_001` |
| `user_id` | STRING | FK → usuarios | `USR_001` |
| `date_fecha` | DATE | Fecha | `2026-06-18` |
| `num_agua_ml` | NUMBER | ML bebidos acumulados | `2500` |
| `num_objetivo_ml` | NUMBER | Objetivo del día | `3000` |
| `date_modificado` | DATETIME | Última actualización | `2026-06-18T15:30:00` |

**Cálculo objetivo agua** (heurístico):
```javascript
// ~35-40 ml por kg de peso corporal
objetivo_base = peso_kg * 35  // 78.2 * 35 = 2737 ml
// Ajustar por entrenamiento
if (dia_entreno) objetivo += 500  // 3237 → ~3L
// Ajustar por calor/actividad (futuro: integrar con clima)
```

---

### 3.10 SUPLEMENTOS_LOG (Adherencia)

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `supp_id` | STRING | PK | `SUP_20260618_001` |
| `user_id` | STRING | FK → usuarios | `USR_001` |
| `date_fecha` | DATE | Fecha | `2026-06-18` |
| `bool_whey` | BOOLEAN | ¿Tomó whey? | `true` |
| `bool_caseina` | BOOLEAN | ¿Tomó caseína? | `true` |
| `bool_vitd_k` | BOOLEAN | ¿Tomó Vit D+K? | `true` |
| `bool_omega3` | BOOLEAN | ¿Tomó Omega-3? | `true` |
| `bool_magnesio` | BOOLEAN | ¿Tomó Magnesio? | `true` |
| `bool_ashwagandha` | BOOLEAN | ¿Tomó Ashwagandha? | `false` |
| `bool_cromo` | BOOLEAN | ¿Tomó Cromo? | `true` |
| `str_notas` | STRING | Notas | `Se acabó ashwa` |

---

### 3.11 EXCEPCIONES_LOG (Contingencias)

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `exc_id` | STRING | PK | `EXC_20260618_001` |
| `user_id` | STRING | FK → usuarios | `USR_001` |
| `str_tipo` | STRING | viaje/enfermedad/lesion/ramadan/estres | `viaje` |
| `date_inicio` | DATE | Inicio de excepción | `2026-06-20` |
| `date_fin` | DATE | Fin (null si indefinido) | `2026-06-25` |
| `str_detalles` | STRING | Detalles adicionales | `Viaje trabajo Madrid` |
| `str_zona_afectada` | STRING | Si lesión, zona | `null` |
| `num_severidad` | NUMBER | 1-10 si aplica | `null` |
| `bool_activa` | BOOLEAN | ¿Sigue activa? | `true` |

---

### 3.12 PLAN_ANUAL (Macrociclo)

> Planificación del año completo por fases

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `fase_id` | STRING | PK | `FAS_2026_01` |
| `user_id` | STRING | FK → usuarios | `USR_001` |
| `num_año` | NUMBER | Año | `2026` |
| `num_orden` | NUMBER | Orden de la fase (1-12) | `1` |
| `str_nombre_fase` | STRING | Nombre de la fase | `Volumen 1` |
| `str_tipo` | STRING | VOL/FZA/DEF/MNT/DELOAD | `VOL` |
| `date_inicio` | DATE | Fecha inicio | `2026-01-06` |
| `date_fin` | DATE | Fecha fin | `2026-02-16` |
| `num_semanas` | NUMBER | Duración en semanas | `6` |
| `num_volumen_objetivo` | NUMBER | Series/músculo/semana | `16` |
| `str_rir_rango` | STRING | RIR objetivo | `2-4` |
| `str_foco_muscular` | STRING | Grupos prioritarios | `Hombros, Espalda` |
| `str_objetivo_nutri` | STRING | bulk/cut/mantener | `bulk` |
| `str_notas` | STRING | Notas adicionales | `Fase de base` |

**Tipos de fase:**
```javascript
TIPOS_FASE = {
  VOL: "Volumen (hipertrofia, superávit)",
  FZA: "Fuerza (intensificación, mantener)",
  DEF: "Definición (déficit calórico)",
  MNT: "Mantenimiento (descanso activo)",
  DELOAD: "Descarga (reducción 50%)"
}
```

---

### 3.13 PLAN_SEMANAL (Microciclo template)

> Template de la semana tipo para cada fase

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `semana_id` | STRING | PK | `SEM_2026_W25` |
| `fase_id` | STRING | FK → plan_anual | `FAS_2026_01` |
| `user_id` | STRING | FK → usuarios | `USR_001` |
| `num_semana_año` | NUMBER | Semana del año (1-52) | `25` |
| `num_semana_fase` | NUMBER | Semana dentro de la fase | `2` |
| `str_lunes` | STRING | Tipo de día | `Pierna` |
| `str_martes` | STRING | Tipo de día | `Descanso` |
| `str_miercoles` | STRING | Tipo de día | `Push` |
| `str_jueves` | STRING | Tipo de día | `Descanso` |
| `str_viernes` | STRING | Tipo de día | `Pull` |
| `str_sabado` | STRING | Tipo de día | `Upper` |
| `str_domingo` | STRING | Tipo de día | `Descanso` |
| `str_rir_semana` | STRING | RIR objetivo de la semana | `2-3` |
| `bool_deload` | BOOLEAN | ¿Es semana de descarga? | `false` |

---

### 3.14 EJERCICIOS_CATALOGO (Referencia)

| Columna | Tipo | Descripción | Ejemplo |
|---------|------|-------------|---------|
| `ejercicio_id` | STRING | PK | `EJE_PRESS_BANCA` |
| `str_nombre` | STRING | Nombre del ejercicio | `Press Banca` |
| `str_nombre_en` | STRING | Nombre en inglés | `Bench Press` |
| `str_grupo_principal` | STRING | Grupo muscular principal | `Pecho` |
| `arr_grupos_secundarios` | STRING | Grupos secundarios (JSON) | `["Triceps","Hombro"]` |
| `str_patron` | STRING | Patrón de movimiento | `Empuje Horizontal` |
| `str_equipamiento` | STRING | Equipo necesario | `Barra, Banco` |
| `bool_compuesto` | BOOLEAN | ¿Es multiarticular? | `true` |
| `bool_favorito` | BOOLEAN | ¿Es favorito del usuario? | `false` |
| `bool_excluido` | BOOLEAN | ¿Está excluido? | `true` |
| `str_razon_exclusion` | STRING | Por qué excluido | `No me gusta` |
| `str_alternativa` | STRING | Ejercicio alternativo | `EJE_PRESS_MANCUERNAS` |

---

## 4. Relaciones

```
usuarios (1) ──────┬──── (N) metricas_zepp
                   ├──── (N) peso_log
                   ├──── (N) plan_anual ────── (N) plan_semanal
                   ├──── (N) sesiones_plan ────── (N) ejercicios_plan
                   │                                      │
                   │                              ejercicios_log
                   │                                      │
                   ├──── (N) progresion_log ◄─────────────┘
                   ├──── (N) comidas_log
                   ├──── (N) hidratacion_log
                   ├──── (N) suplementos_log
                   └──── (N) excepciones_log

ejercicios_catalogo (1) ──── (N) ejercicios_plan
                        └──── (N) ejercicios_log
```

---

## 5. Reglas de Integridad

1. **Soft delete**: Nunca eliminar, usar `bool_activo = false`
2. **Timestamps**: ISO 8601 (`YYYY-MM-DDTHH:mm:ss`)
3. **Decimales**: Punto, no coma
4. **IDs**: Formato `TIPO_YYYYMMDD_NNNN`
5. **Validaciones**:
   - `num_peso_kg`: 30-200
   - `num_reps`: 1-100
   - `num_rir`: 0-5
   - `num_sleep_score`: 0-100

---

## 6. Índices y Consultas Frecuentes

```javascript
// Última sesión de un ejercicio (para sugerir peso)
function getUltimoLog(userId, ejercicioId) {
  return QUERY("SELECT * FROM ejercicios_log 
                WHERE user_id = ? AND ejercicio_id = ? 
                ORDER BY date_timestamp DESC LIMIT 1")
}

// Media HRV 10 días (para autorregulación Kiviniemi)
function getHrvMedia10d(userId) {
  return QUERY("SELECT AVG(num_hrv_rmssd), STDEV(num_hrv_rmssd)
                FROM metricas_zepp 
                WHERE user_id = ? 
                ORDER BY date_fecha DESC LIMIT 10")
}

// Volumen semanal por grupo muscular
function getVolumenSemanal(userId, grupoMuscular) {
  return QUERY("SELECT SUM(peso * reps * series) 
                FROM ejercicios_log 
                WHERE user_id = ? 
                  AND grupo_principal = ?
                  AND date >= DATE_SUB(NOW(), 7)")
}
```

---

## 7. Pre-Generación del Plan Completo

> ⚠️ **DECISIÓN ARQUITECTÓNICA**: Las sesiones NO se generan día a día.
> Se pre-generan TODAS las sesiones del año durante el despliegue.
> El usuario abre la app cualquier día y ya tiene su sesión lista.

### 7.1 Función `generarPlanCompleto()`

Se ejecuta UNA VEZ durante el despliegue (post-`inicializarHojas()`).
Genera las ~192 sesiones (48 semanas × 4 gym/semana) + ~1300 filas de ejercicios.

```javascript
function generarPlanCompleto() {
  // 1. Leer plan_anual → obtener fases con fechas
  // 2. Para cada fase:
  //    a. Determinar semanas (fecha_inicio → fecha_fin)
  //    b. Para cada semana:
  //       - Determinar RIR según posición en mesociclo
  //       - Para cada día de gym (LUN, MIÉ, VIE, SÁB):
  //         * Crear fila en sesiones_plan
  //         * Crear filas en ejercicios_plan según fase+día
  //         * num_peso_sugerido_kg = 0 (desconocido hasta primera sesión)
  // 3. Marcar plan_semanal con template por semana
}
```

### 7.2 Qué queda PRE-CARGADO (no depende de nada en runtime)

| Hoja | Contenido pre-cargado | Dependencia runtime |
|------|----------------------|---------------------|
| `plan_anual` | 10 fases con fechas exactas | NINGUNA |
| `plan_semanal` | 48 semanas con split + RIR | NINGUNA |
| `sesiones_plan` | ~192 sesiones con fecha+tipo+fase | NINGUNA |
| `ejercicios_plan` | ~1300 ejercicios con series+reps+descanso | Solo `num_peso_sugerido_kg` se actualiza post-sesión |
| `ejercicios_catalogo` | ~25 ejercicios únicos | NINGUNA |

### 7.3 Qué se actualiza en RUNTIME (motor_pesos)

| Campo | Cuándo se actualiza | Lógica |
|-------|--------------------|---------| 
| `ejercicios_plan.num_peso_sugerido_kg` | Post-registro de serie anterior | ACSM 2009: +2.5% si excedes reps con RIR≥2 |
| `sesiones_plan.num_ajuste_volumen` | Al abrir la app (si hay métricas) | Kiviniemi: FC reposo > media+10 → ×0.80 |
| `sesiones_plan.bool_completada` | Al terminar sesión | Marca como hecha |

### 7.4 Si el motor NO puede ejecutarse (sin red)

La app usa el ÚLTIMO peso registrado para ese ejercicio (cache local Room).
Si es la PRIMERA VEZ (peso = 0), la app muestra "Elige tu peso" y el usuario ajusta manualmente.

### 7.5 Triggers que SÍ se mantienen (opcionales, no críticos)

| Trigger | Frecuencia | Acción | ¿Crítico? |
|---------|------------|--------|-----------|
| `actualizarPesos` | Post-sesión | Recalcula pesos sugeridos para próximas sesiones | NO — si falla, el usuario ajusta manual |
| `syncMetricas` | Al abrir app | Lee HC y aplica ajuste de volumen | NO — sin ajuste = sesión normal al 100% |
| `backupSemanal` | Domingo 3:00 | Copia de seguridad | NO |

---

## 8. Uso en el Sistema (Flujo Corregido)

### Despliegue (UNA VEZ):
```
inicializarHojas() → generarPlanCompleto() → 192 sesiones + 1300 ejercicios listos
```

### Día a día (RUNTIME):
```
1. App abre → consulta fecha hoy → busca sesión en sesiones_plan
2. Si hay sesión → carga ejercicios_plan de esa sesión
3. Muestra ejercicio + peso sugerido (o "Elige peso" si es primera vez)
4. Usuario entrena y registra → ejercicios_log
5. Post-registro → motor_pesos actualiza peso sugerido de la PRÓXIMA sesión del mismo ejercicio
6. Si no hay red → todo se encola en Room y se sincroniza después
```

### Garantía:
> **Para CUALQUIER día entre 31/08/2026 y 31/07/2027, al abrir la app,
> la sesión con sus ejercicios, series y reps ya está pre-cargada.**
> Solo el peso sugerido depende del historial (y si no hay historial, se deja manual).