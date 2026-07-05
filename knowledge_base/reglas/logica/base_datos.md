---
id: "REG-LOG-02"
nombre: "Arquitectura de Base de Datos"
fecha_modificacion: "04/07/2026"
estado: "ACTIVO"
relacionados: ["SYS-00", "REG-LOG-01", "REG-ENT-01", "USR-01"]
tags: ["reglas", "logica", "base-datos", "sheets", "simplificado"]
---

# Arquitectura de Base de Datos

## 1. Alcance
Modelo de persistencia simplificado para FitBase con estas reglas:
- Usuario unico: no se persiste user_id en tablas operativas.
- Solo se guarda lo necesario para ajustar entrenamiento y fases.
- Comida: no se guarda en BBDD; solo visual diaria en app.
- metricas_zepp centraliza TODO lo recogido de Health Connect (sueno, pasos,
  FC reposo, peso, % grasa) — antes peso/grasa vivian en peso_log, separado.
- Ejercicios log: retencion maxima de 7 dias — solo sirve para el reajuste
  dinamico del motor de cargas, no es historial permanente.

## 2. Flujos Activos

```text
FLUJO MANANA
Health Connect -> metricas_zepp (sueno, pasos, FC reposo, peso, % grasa — una fila/dia)

FLUJO GYM
sesiones_plan + ejercicios_plan -> app
app -> ejercicios_log (ultimos 7 dias, autolimpieza tras cada serie)

FLUJO NOCTURNO (tras las 22:00, 1 vez/dia)
app -> metricas_subjetivas (energia, estres, notas)

FLUJO PLAN
plan_anual + sesiones_plan + ejercicios_plan pre-generados por prioridades
```

## 3. Hojas Activas de Google Sheets

### 3.1 METRICAS_ZEPP
Todo lo que se recoge de Health Connect, una fila por dia.

| Columna | Tipo | Descripcion |
|---|---|---|
| metrica_id | STRING | PK |
| date_fecha | DATE | Fecha |
| num_sleep_score | NUMBER | Puntuacion de sueno ESTIMADA (0-100), calculada por la app a partir de duracion/fases de HC |
| num_pasos | NUMBER | Pasos del dia |
| num_hr_reposo | NUMBER | Pulsaciones en reposo |
| num_peso_kg | NUMBER | Peso corporal (bascula Zepp/Xiaomi via HC) |
| num_grasa_pct | NUMBER | Porcentaje de grasa |
| date_sync | DATETIME | Timestamp de sincronizacion, o `'FICTICIO'` si la fila viene de rellenarDatosFicticios() |

`guardarMetricas_` hace upsert por fecha con MERGE de campos: si solo llega
el peso ese dia, no borra el sueno/pasos/FC ya guardados (y viceversa) —
lee la fila existente antes de sobreescribir.

### 3.2 METRICAS_SUBJETIVAS
Entrada manual, una vez al dia tras las 22:00 (selector de 5 niveles en la app).

| Columna | Tipo | Descripcion |
|---|---|---|
| subjetiva_id | STRING | PK |
| date_fecha | DATE | Fecha |
| num_energia | NUMBER | Nivel de energia / desgaste fisico (1-5) |
| num_estres | NUMBER | Nivel de estres (1-5) |
| str_notas | STRING | Notas libres opcionales |

El motor de ajuste diario (`calcularAjusteDia_`) mira primero el estres de
AYER (la pregunta se hace de noche, sobre el dia que termina — es el dato
relevante para la sesion de HOY), y si no hay dato de ayer prueba con el de
hoy. Umbral: `num_estres >= 4` (escala 1-5) reduce el factor del dia.

### 3.3 PLAN_ANUAL
Fases del macrociclo definidas por prioridades del usuario. Imprescindible:
de aqui sale la fase actual, que decide macros (bulk/cut/mantener), objetivo
de pasos, y el modulador de fase del motor de cargas.

| Columna | Tipo | Descripcion |
|---|---|---|
| fase_id | STRING | PK |
| num_año | NUMBER | Año |
| num_orden | NUMBER | Orden de fase |
| str_nombre_fase | STRING | Nombre |
| str_tipo | STRING | VOL/FZA/DEF/MNT/DELOAD |
| date_inicio | DATE | Inicio |
| date_fin | DATE | Fin |
| num_semanas | NUMBER | Duracion |
| num_volumen_objetivo | NUMBER | Series objetivo |
| str_rir_rango | STRING | RIR |
| str_foco_muscular | STRING | Foco muscular |
| str_objetivo_nutri | STRING | bulk/cut/mantener |
| str_notas | STRING | Notas |

### 3.4 SESIONES_PLAN
Sesiones ya calendarizadas para cada fecha — tabla ancla: `getSesionHoy_()`
busca aqui la sesion de hoy por fecha, se marca como completada aqui, y de
aqui sale el aviso de "dia perdido".

| Columna | Tipo | Descripcion |
|---|---|---|
| sesion_id | STRING | PK |
| date_fecha | DATE | Fecha |
| str_tipo | STRING | Push/Pierna/Pull/Hombros+Brazos |
| num_semana_meso | NUMBER | Semana de mesociclo |
| str_fase | STRING | Fase |
| num_ajuste_volumen | NUMBER | Ajuste del dia |
| num_duracion_est_min | NUMBER | Duracion estimada |
| bool_completada | BOOLEAN | Estado |
| date_inicio | DATETIME | Inicio real |
| date_fin | DATETIME | Fin real |
| date_creado | DATETIME | Creacion |

### 3.5 EJERCICIOS_PLAN
Ejercicios de cada sesion. NO almacena pesos (son dinamicos via motor).

| Columna | Tipo | Descripcion |
|---|---|---|
| plan_id | STRING | PK |
| sesion_id | STRING | FK sesiones_plan |
| ejercicio_id | STRING | FK catalogo |
| num_orden | NUMBER | Orden |
| num_series_plan | NUMBER | Series |
| str_reps_plan | STRING | Reps objetivo (ej: "8-10") |
| num_rir_objetivo | NUMBER | RIR |
| num_descanso_seg | NUMBER | Descanso |
| str_notas | STRING | Notas |
| bool_es_warmup | BOOLEAN | Serie calentamiento |

### 3.6 EJERCICIOS_LOG (RETENCION 7 DIAS)
Registro de series realizadas — SOLO para el reajuste dinamico del motor de
cargas (`calcularPesoSugerido_` lee el ultimo rendimiento real). No es un
historial permanente: en la fase de adaptacion habra bastante diferencia
entre lo planeado y lo real, y el motor se vuelve mas preciso con el tiempo,
pero eso ya lo captura el peso recalculado — no hace falta guardar meses de logs.

| Columna | Tipo | Descripcion |
|---|---|---|
| log_id | STRING | PK |
| plan_id | STRING | FK ejercicios_plan |
| sesion_id | STRING | FK sesiones_plan |
| ejercicio_id | STRING | Ejercicio |
| num_serie | NUMBER | Serie |
| num_peso_usado_kg | NUMBER | Peso real |
| num_reps_completadas | NUMBER | Reps reales |
| num_rir_percibido | NUMBER | RIR percibido |
| str_sensacion | STRING | facil/bien/duro/fallo |
| date_timestamp | DATETIME | Timestamp |

Regla de retencion (`limpiarLogsAntiguos_`, constante `EJERCICIOS_LOG_RETENCION_DIAS`):
- Al insertar un log nuevo (`guardarLog_`), se eliminan registros con fecha
  anterior a hoy-7 dias. Se mantiene solo con cada escritura, sin cron externo.

### 3.7 EJERCICIOS_CATALOGO
Catalogo de referencia de ejercicios — imprescindible: `ejercicios_plan` solo
guarda `ejercicio_id` (FK), este catalogo es lo que convierte el ID en nombre
legible ("Press inclinado mancuernas") y grupo muscular.

## 4. Reglas Funcionales

1. Plan anual y semanal deben respetar prioridades en orden estricto:
- P1 Estetica muscular
- P2 Postura
- P3 Hipertrofia
- P4 Flexibilidad
- P5-P10 como moduladores secundarios

2. Motor de cargas usa:
- Último registro por ejercicio dentro de ventana 7 dias (ejercicios_log)
- Metricas Zepp del dia (sleep score, pasos, FC reposo)
- Estres subjetivo de ayer (metricas_subjetivas)
- Composicion corporal reciente (metricas_zepp: peso, % grasa)

3. Nutricion:
- Se calculan objetivos del dia para interfaz.
- No se persiste ningun consumo de comida en Sheets.

## 5. Endpoints Activos (Apps Script)

GET:
- accion=sesion_hoy
- accion=vista_manana
- accion=plan_anual
- accion=macros_hoy
- accion=check_ausencia
- accion=progresion_metricas

POST:
- accion=guardar_log
- accion=guardar_metricas
- accion=guardar_metricas_subjetivas
- accion=completar_sesion
- accion=registrar_ausencia

Endpoints de comida o logs no esenciales quedan fuera de alcance.
