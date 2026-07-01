---
id: "REG-LOG-02"
nombre: "Arquitectura de Base de Datos"
fecha_modificacion: "01/07/2026"
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
- Ejercicios log: retencion maxima de 7 dias.

## 2. Flujos Activos

```text
FLUJO MANANA
Zepp/Bascula -> metricas_zepp + peso_log

FLUJO GYM
sesiones_plan + ejercicios_plan -> app
app -> ejercicios_log (ultimos 7 dias)

FLUJO PLAN
plan_anual + plan_semanal pre-generados por prioridades
```

## 3. Hojas Activas de Google Sheets

### 3.1 METRICAS_ZEPP
Metricas diarias del reloj Zepp.

| Columna | Tipo | Descripcion |
|---|---|---|
| metrica_id | STRING | PK |
| date_fecha | DATE | Fecha |
| num_sleep_score | NUMBER | Puntuacion de sueno (0-100) |
| num_pasos | NUMBER | Pasos del dia |
| num_hr_reposo | NUMBER | Pulsaciones en reposo |
| num_vo2max | NUMBER | VO2 max |
| date_sync | DATETIME | Timestamp de sincronizacion |

### 3.2 PESO_LOG
Composicion corporal de bascula Zepp/Xiaomi.

| Columna | Tipo | Descripcion |
|---|---|---|
| peso_id | STRING | PK |
| date_fecha | DATE | Fecha |
| num_peso_kg | NUMBER | Peso |
| num_grasa_pct | NUMBER | Porcentaje de grasa |
| num_hidratacion_pct | NUMBER | Porcentaje de hidratacion |
| num_grasa_visceral | NUMBER | Grasa visceral |
| date_sync | DATETIME | Timestamp |

### 3.3 PLAN_ANUAL
Fases del macrociclo definidas por prioridades del usuario.

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

### 3.4 PLAN_SEMANAL
Microciclo semanal por fase.

| Columna | Tipo | Descripcion |
|---|---|---|
| semana_id | STRING | PK |
| fase_id | STRING | FK plan_anual |
| num_semana_año | NUMBER | Semana del ano |
| num_semana_fase | NUMBER | Semana dentro de fase |
| str_lunes | STRING | Sesion |
| str_martes | STRING | Sesion |
| str_miercoles | STRING | Sesion |
| str_jueves | STRING | Sesion |
| str_viernes | STRING | Sesion |
| str_sabado | STRING | Sesion |
| str_domingo | STRING | Sesion |
| str_rir_semana | STRING | RIR objetivo |
| bool_deload | BOOLEAN | Indicador deload |

### 3.5 SESIONES_PLAN
Sesiones ya calendarizadas para cada fecha.

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

### 3.6 EJERCICIOS_PLAN
Ejercicios de cada sesion.

| Columna | Tipo | Descripcion |
|---|---|---|
| plan_id | STRING | PK |
| sesion_id | STRING | FK sesiones_plan |
| ejercicio_id | STRING | FK catalogo |
| num_orden | NUMBER | Orden |
| num_series_plan | NUMBER | Series |
| num_reps_plan | STRING | Reps objetivo |
| num_peso_sugerido_kg | NUMBER | Peso sugerido |
| num_rir_objetivo | NUMBER | RIR |
| num_descanso_seg | NUMBER | Descanso |
| str_notas | STRING | Notas |
| bool_es_warmup | BOOLEAN | Serie calentamiento |

### 3.7 EJERCICIOS_LOG (RETENCION 7 DIAS)
Registro de series realizadas; solo se conserva la ultima semana.

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

Regla de retencion:
- Al insertar un log nuevo, se eliminan registros con fecha anterior a hoy-7 dias.

### 3.8 EJERCICIOS_CATALOGO
Catalogo de referencia de ejercicios.

## 4. Reglas Funcionales

1. Plan anual y semanal deben respetar prioridades en orden estricto:
- P1 Estetica muscular
- P2 Postura
- P3 Hipertrofia
- P4 Flexibilidad
- P5-P10 como moduladores secundarios

2. Motor de cargas usa:
- Ejercicios de la ultima semana (ejercicios_log)
- Metricas Zepp del dia (sleep score, pasos, FC reposo, VO2max)
- Composicion corporal reciente (peso_log)

3. Nutricion:
- Se calculan objetivos del dia para interfaz.
- No se persiste ningun consumo de comida en Sheets.

## 5. Endpoints Activos (Apps Script)

GET:
- accion=sesion_hoy
- accion=plan_anual
- accion=plan_semanal
- accion=macros_hoy
- accion=check_ausencia
- accion=progresion_metricas

POST:
- accion=guardar_log
- accion=guardar_peso
- accion=guardar_metricas
- accion=completar_sesion

Endpoints de comida o logs no esenciales quedan fuera de alcance.
