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

> ⚠️ **IMPORTANTE**: 
> - Variables `ZEPP_*` y `SCALE_*` = Lo que miden los dispositivos (referencia)
> - Variables `HC_*` = Lo que la app lee de Health Connect (**variables activas del sistema**)
> - La app NO lee de Zepp/Xiaomi directamente. Lee de Health Connect.

## 2. Amazfit GTS 4 (Zepp) - Referencia

> Estas variables son referencia de lo que el reloj mide. No todas están disponibles en Health Connect.

### Métricas de Sueño
| Métrica Zepp | Variable Referencia | Descripción |
|--------------|---------------------|-------------|
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

## 4. Health Connect (Android) - FUENTE PRINCIPAL

> ⚠️ **IMPORTANTE**: Zepp y Xiaomi NO tienen API pública. La app FitBase lee datos vía **Health Connect**, que es la API oficial de Android para datos de salud.

### Flujo de Datos
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Amazfit GTS 4  │────▶│    Zepp App     │────▶│ Health Connect  │
│   (wearable)    │     │   (sincroniza)  │     │  (API Android)  │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
┌─────────────────┐     ┌─────────────────┐              │
│ Báscula Xiaomi  │────▶│  Mi Fitness App │──────────────┤
│   (bluetooth)   │     │   (sincroniza)  │              │
└─────────────────┘     └─────────────────┘              │
                                                         ▼
                                                ┌─────────────────┐
                                                │   APP FITBASE   │
                                                │  (lee HC API)   │
                                                └─────────────────┘
```

### Configuración del Usuario (Una sola vez)
1. Zepp app → Ajustes → Health Connect → Activar sincronización
2. Mi Fitness → Ajustes → Health Connect → Activar sincronización
3. FitBase → Solicita permisos de Health Connect al primer uso

### Métricas Disponibles vía Health Connect
| Categoría | Métrica | Variable Sistema | Fuente Original |
|-----------|---------|------------------|-----------------|
| **Sueño** | Sleep Score | `HC_SLEEP_SCORE` | Zepp |
| **Sueño** | Duración total | `HC_SLEEP_DURATION` | Zepp |
| **Sueño** | Sueño profundo | `HC_SLEEP_DEEP` | Zepp |
| **Sueño** | Sueño REM | `HC_SLEEP_REM` | Zepp |
| **Cardio** | FC reposo | `HC_HR_REST` | Zepp |
| **Cardio** | FC durante ejercicio | `HC_HR_EXERCISE` | Zepp |
| **Actividad** | Pasos | `HC_STEPS` | Zepp / Mi Fitness |
| **Actividad** | Calorías activas | `HC_ACTIVE_KCAL` | Zepp |
| **Actividad** | Distancia | `HC_DISTANCE` | Zepp |
| **Cuerpo** | Peso | `HC_WEIGHT` | Mi Fitness (báscula) |
| **Cuerpo** | Grasa corporal | `HC_BODY_FAT` | Mi Fitness (báscula) |
| **Cuerpo** | Masa muscular | `HC_MUSCLE_MASS` | Mi Fitness (báscula) |

### Métricas NO Disponibles en Health Connect
| Métrica | Razón | Alternativa |
|---------|-------|-------------|
| HRV (RMSSD) | Zepp no lo exporta a HC | Entrada manual o ignorar |
| Readiness Score | Propietario de Zepp | Calcular con sueño + FC |
| PAI | Propietario de Zepp | Ignorar |
| Estrés | Zepp no lo exporta | Entrada manual |
| Grasa visceral | Xiaomi no lo exporta | Entrada manual |

### Permisos Requeridos
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.health.READ_STEPS"/>
<uses-permission android:name="android.permission.health.READ_SLEEP"/>
<uses-permission android:name="android.permission.health.READ_HEART_RATE"/>
<uses-permission android:name="android.permission.health.READ_WEIGHT"/>
<uses-permission android:name="android.permission.health.READ_BODY_FAT"/>
<uses-permission android:name="android.permission.health.READ_DISTANCE"/>
<uses-permission android:name="android.permission.health.READ_ACTIVE_CALORIES_BURNED"/>
```

### Frecuencia de Lectura
```yaml
HEALTH_CONNECT_SYNC:
  al_abrir_app: true
  background_sync: false  # Para no gastar batería
  datos_a_leer:
    - sueño: "última noche"
    - peso: "última medición"
    - pasos: "hoy"
    - fc_reposo: "último valor"
```

## 5. Umbrales del Sistema

> Los umbrales usan las variables `HC_*` de Health Connect.
> Definidos en `reglas/logica/motor_pesos.md`

## 6. Uso en el Sistema
1. La app lee Health Connect al abrirse (mañana)
2. `motor_pesos.md` usa `HC_SLEEP_SCORE` y `HC_HR_REST` para autorregulación
3. `motor_dieta.md` usa `HC_STEPS` para ajustar calorías
4. Los datos se guardan en `metricas_zepp` de Google Sheets (mismo esquema)
