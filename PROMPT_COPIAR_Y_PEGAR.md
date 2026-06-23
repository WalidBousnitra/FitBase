# FITBASE - PROMPT PARA GENERACIÓN DE CÓDIGO

> **INSTRUCCIÓN**: Copia este prompt y pégalo junto con la estructura del proyecto en una nueva sesión de IA.

---

# REGLAS ABSOLUTAS

```
1. PROHIBIDO inventar datos - Tu ÚNICA fuente son los archivos .md del proyecto
2. PROHIBIDO usar Kotlin - SOLO Java 17
3. PROHIBIDO usar Jetpack Compose - SOLO Views/XML
4. Comentarios en ESPAÑOL
5. Material Design 3 con modo OSCURO
```

---

# 1. TU TAREA

Generar una **app Android completa** llamada FitBase para entrenamientos y nutrición.

**Stack técnico**:
- Android (Java 17, Views/XML, minSdk 26, targetSdk 34)
- Arquitectura MVVM + Repository
- Backend: Google Apps Script + Google Sheets (gratis)
- Métricas: Health Connect (Zepp + Mi Fitness)

---

# 2. CÓMO USAR EL PROYECTO

## Estructura de la Knowledge Base

```
/knowledge_base
├── manifest.md              ← LEER PRIMERO (mapa del proyecto)
│
├── /usuario                 ← DATOS DEL USUARIO (leer estos archivos)
│   ├── biometria.md         ← Peso, altura, composición, objetivos, fases
│   ├── prioridades.md       ← Ranking de objetivos (guía decisiones)
│   └── /metricas            ← Dispositivos (Zepp, báscula)
│
├── /evidencia               ← CIENCIA (reglas basadas en papers)
│   ├── hipertrofia.md       ← Volumen, frecuencia, intensidad
│   ├── nutricion.md         ← Macros, timing
│   └── ...                  ← Otros papers procesados
│
└── /reglas                  ← LÓGICA DE NEGOCIO
    ├── /entrenamiento       ← Programación, ejercicios
    ├── /nutricion           ← Motor de dieta
    ├── /logica              ← Motor de pesos, base de datos
    └── /desarrollo          ← Especificaciones UI, código
```

## Orden de Lectura

1. **manifest.md** - Entiende la estructura completa
2. **usuario/biometria.md** - Datos del usuario y objetivos
3. **usuario/prioridades.md** - Qué es más importante
4. **reglas/logica/base_datos.md** - Esquema de las 14 hojas
5. **reglas/logica/motor_pesos.md** - Lógica de progresión
6. **reglas/logica/motor_dieta.md** - Cálculo de macros
7. **reglas/entrenamiento/programacion.md** - Split, volumen, frecuencia
8. **reglas/desarrollo/especificacion_ui.md** - Pantallas y flujos

---

# 3. QUÉ GENERAR

## Fase 1: Backend (Google Apps Script)
```
Codigo.gs - API REST completa
├── doGet(e)  - Leer datos
├── doPost(e) - Escribir datos
└── Funciones para cada hoja
```

Leer esquema de: `reglas/logica/base_datos.md`

## Fase 2: Android - Modelo
```
Clases Java que representan las entidades:
- Usuario.java
- Sesion.java
- Ejercicio.java
- EjercicioLog.java
- PesoLog.java
- MetricasZepp.java
```

Basarse en: `reglas/logica/base_datos.md`

## Fase 3: Android - Data Layer
```
- AppDatabase.java (Room)
- DAOs para cada entidad
- ApiService.java (Retrofit)
- FitBaseRepository.java
```

## Fase 4: Android - Managers
```
- DemoDataProvider.java    ← Datos mock para MODO DEMO
- HealthConnectManager.java ← Leer de Zepp/Mi Fitness
- MotorDietaManager.java   ← Lógica de reglas/logica/motor_dieta.md
- MotorPesosManager.java   ← Lógica de reglas/logica/motor_pesos.md
```

## Fase 5: Android - UI
```
Activities y ViewModels:
- SplashActivity
- HomeActivity + HomeViewModel
- WorkoutActivity + WorkoutViewModel  
- SummaryActivity
- PlanAnualActivity
- Dialogs (Reps, Peso, RIR)
```

Diseño en: `reglas/desarrollo/especificacion_ui.md`

## Fase 6: Layouts XML
```
Material Design 3, modo oscuro
Todos los layouts para las Activities/Fragments
```

## Fase 7: Resources
```
colors.xml, dimens.xml, strings.xml, themes.xml
AndroidManifest.xml con permisos Health Connect
build.gradle con dependencias
```

---

# 4. MODO DEMO (CRÍTICO)

```yaml
ACTIVAR_SI: fecha_actual < "1 Sep 2026"

COMPORTAMIENTO:
  - Banner: "🎮 MODO DEMO - El programa real comienza el 1 de Septiembre"
  - Todas las pantallas navegables
  - Datos ficticios de ejemplo
  - NO requiere Google Sheets real
  - Health Connect opcional (datos mock si no hay)
```

La fecha de inicio está en: `usuario/biometria.md` (buscar FECHA_INICIO)

---

# 5. DATOS CLAVE A EXTRAER

## Del archivo `usuario/biometria.md`:
- Datos básicos (edad, altura, sexo)
- Composición corporal actual
- Objetivos por fase (CUT, BULK, MINICUT)
- Checkpoints de progreso
- Métricas del wearable

## Del archivo `usuario/prioridades.md`:
- Orden de prioridades (#1 Estética, #2 Postura, etc.)
- Esto guía qué mostrar primero en la UI

## Del archivo `reglas/logica/motor_dieta.md`:
- Fórmulas: TMB, NEAT, TDEE
- Ajustes por fase (déficit/superávit)
- Cálculo de macros

## Del archivo `reglas/logica/motor_pesos.md`:
- Lógica de progresión (cuándo subir peso)
- Ajustes por recuperación (sleep score, HRV)
- Reglas de deload

## Del archivo `reglas/logica/base_datos.md`:
- Esquema de las 14 hojas de Google Sheets
- Nombres de columnas exactos
- Tipos de datos

## Del archivo `reglas/entrenamiento/programacion.md`:
- Split semanal (Upper/Lower + Natación)
- Volumen por grupo muscular
- Frecuencia y periodización

---

# 6. HEALTH CONNECT

Leer configuración de: `usuario/metricas/hardware_zepp.md`

Métricas a obtener:
- SleepSessionRecord (Zepp)
- RestingHeartRateRecord (Zepp)
- StepsRecord (Zepp)
- WeightRecord (Mi Fitness)
- BodyFatRecord (Mi Fitness)

---

# 7. FUNCIONALIDADES ADICIONALES

## Google Calendar Sync
- Crear eventos de entreno por fase
- Checkpoints en fechas clave
- Recordatorio diario de pesarse (7:00 AM)

La IA debe generar el código de Apps Script para esto.

## Vista Plan Anual
- Mostrar las 52 semanas
- Colorear por fase
- Indicar semana actual

---

# 8. DESPLIEGUE

El manual completo está en: `reglas/desarrollo/MANUAL_DESPLIEGUE.md`

Resumen:
1. Crear Google Sheets con 14 hojas
2. Desplegar Apps Script como Web App
3. Crear proyecto Android Studio
4. Generar APK e instalar

---

# 9. INSTRUCCIÓN FINAL

```
1. Lee los archivos .md indicados para extraer todos los datos
2. Genera el código en el orden especificado (Fases 1-7)
3. Asegúrate de que MODO DEMO funcione antes del 1 Sep 2026
4. Usa SOLO Java, SOLO Views/XML
5. Comenta el código en español
```

**Empieza preguntando**: "¿Puedo ver el contenido de manifest.md y biometria.md para empezar?"

O si ya tienes acceso al proyecto, empieza directamente con la Fase 1.

---

# FIN DEL PROMPT
