---
id: "USR-MET-01"
nombre: "Métricas de Hardware"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-03", "REG-LOG-01"]
tags: ["metricas", "zepp", "health-connect", "hardware"]
---

# Métricas de Hardware

## 1. Alcance
Mapeo de datos capturados por dispositivos wearables y APIs de salud.

## 2. Amazfit GTS 4 (Zepp)

### Métricas de Sueño
| Métrica Zepp | Variable Sistema | Descripción |
|--------------|------------------|-------------|
| Sleep Score | `ZEPP_SLEEP_SCORE` | Puntuación 0-100 |
| Deep Sleep | `ZEPP_DEEP_SLEEP` | Minutos sueño profundo |
| REM Sleep | `ZEPP_REM_SLEEP` | Minutos sueño REM |
| Light Sleep | `ZEPP_LIGHT_SLEEP` | Minutos sueño ligero |
| Awake Time | `ZEPP_AWAKE` | Minutos despierto |
| Bedtime | `ZEPP_BEDTIME` | Hora de dormir |
| Wake Time | `ZEPP_WAKETIME` | Hora de despertar |

### Métricas Cardíacas
| Métrica Zepp | Variable Sistema | Descripción |
|--------------|------------------|-------------|
| Resting HR | `ZEPP_HR_REST` | FC en reposo (bpm) |
| SpO2 | `ZEPP_SPO2` | Saturación oxígeno (%) |

### Métricas de Actividad
| Métrica Zepp | Variable Sistema | Descripción |
|--------------|------------------|-------------|
| Steps | `ZEPP_STEPS` | Pasos diarios |
| Calories | `ZEPP_CALORIES` | Calorías quemadas |
| Distance | `ZEPP_DISTANCE` | Distancia (km) |
| PAI | `ZEPP_PAI` | Personal Activity Intelligence |
| VO2max | `ZEPP_VO2MAX` | Capacidad aeróbica estimada |

### Métricas de Estrés
| Métrica Zepp | Variable Sistema | Descripción |
|--------------|------------------|-------------|
| Stress Level | `ZEPP_STRESS` | Nivel de estrés (0-100) |

### Métricas de Readiness
| Métrica Zepp | Variable Sistema | Descripción |
|--------------|------------------|-------------|
| Readiness Score | `ZEPP_READINESS` | Preparación para entrenar |

### Otras Métricas GTS 4
| Métrica Zepp | Variable Sistema | Descripción |
|--------------|------------------|-------------|
| Body Battery | `ZEPP_BODY_BATTERY` | Energía corporal |
| Recovery Time | `ZEPP_RECOVERY_TIME` | Tiempo recuperación sugerido |
| Training Load | `ZEPP_TRAINING_LOAD` | Carga de entrenamiento |
| Training Effect | `ZEPP_TRAINING_EFFECT` | Efecto aeróbico/anaeróbico |
| Max HR (sesión) | `ZEPP_HR_MAX` | FC máxima en entreno |
| Avg HR (sesión) | `ZEPP_HR_AVG` | FC media en entreno |
| HR Zones | `ZEPP_HR_ZONES` | Tiempo en cada zona |
| Breathing Rate | `ZEPP_BREATHING` | Respiraciones/min |

## 3. Báscula Xiaomi (Zepp)

### Métricas de Composición Corporal
| Métrica | Variable Sistema | Descripción |
|---------|------------------|-------------|
| Peso | `SCALE_WEIGHT` | Peso total (kg) |
| Grasa corporal | `SCALE_BODY_FAT` | Porcentaje grasa (%) |
| Masa muscular | `SCALE_MUSCLE` | Masa muscular (kg) |
| Agua corporal | `SCALE_WATER` | Porcentaje agua (%) |
| Masa ósea | `SCALE_BONE` | Masa ósea (kg) |
| Grasa visceral | `SCALE_VISCERAL` | Nivel grasa visceral |
| BMI | `SCALE_BMI` | Índice masa corporal |
| BMR | `SCALE_BMR` | Metabolismo basal (kcal) |
| Edad corporal | `SCALE_BODY_AGE` | Edad metabólica estimada |
| Proteína | `SCALE_PROTEIN` | Porcentaje proteína (%) |
| Tipo de cuerpo | `SCALE_BODY_TYPE` | Clasificación corporal |

## 4. Health Connect (Android)

### Métricas Sincronizadas
| Métrica | Variable Sistema | Fuente |
|---------|------------------|--------|
| Pasos totales | `HC_STEPS` | Agregado multi-app |
| Distancia | `HC_DISTANCE` | GPS + sensores |
| Calorías activas | `HC_ACTIVE_KCAL` | Estimación |

## 5. Umbrales del Sistema

> ⏳ **Pendiente**: Los umbrales específicos deben definirse en `evidencia/` con papers.
> Variables disponibles: `ZEPP_SLEEP_SCORE`, `ZEPP_HRV`, `ZEPP_HR_REST`, `ZEPP_STRESS`

## 6. Uso en el Sistema
1. `motor_pesos.md` consume estas variables para autorregulación.
2. Los umbrales se definen en `reglas/logica/motor_pesos.md`.
