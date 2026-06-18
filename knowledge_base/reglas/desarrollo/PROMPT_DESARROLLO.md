---
id: "REG-DEV-00"
nombre: "Prompt Maestro de Desarrollo"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["SYS-00", "REG-DEV-01", "REG-DEV-02", "REG-LOG-02"]
tags: ["desarrollo", "prompt", "master", "codigo"]
---

# 🚀 PROMPT MAESTRO DE DESARROLLO

> **PROPÓSITO**: Este documento contiene TODO el contexto necesario para que la IA genere el código de FitBase. Leer ANTES de generar cualquier código.

---

## 1. REGLAS ABSOLUTAS

```yaml
PROHIBIDO:
  - Inventar lógica de negocio (solo usar archivos /evidencia/ y /reglas/)
  - Usar Kotlin (SOLO Java)
  - Usar Jetpack Compose (SOLO Views/XML)
  - Crear dependencias de servidores externos
  - Añadir features no especificadas
  
OBLIGATORIO:
  - Java 17 + Views/XML
  - MVVM con Repository pattern
  - Room para cache offline
  - Retrofit para API
  - Material Design 3 modo oscuro
  - Nombres en español (clases, variables, comentarios)
  - Código simple y legible (el usuario debe entenderlo)
```

---

## 2. ARQUITECTURA

```
┌────────────────────────────────────────────────────────────┐
│                    APP ANDROID (Java)                       │
│                                                             │
│   ┌─────────┐    ┌─────────────┐    ┌─────────────┐        │
│   │   UI    │◄──►│  ViewModel  │◄──►│ Repository  │        │
│   │ (Views) │    │             │    │             │        │
│   └─────────┘    └─────────────┘    └──────┬──────┘        │
│                                            │                │
│                            ┌───────────────┼───────────────┐│
│                            │               │               ││
│                            ▼               ▼               ││
│                      ┌──────────┐    ┌──────────┐          ││
│                      │   Room   │    │ Retrofit │          ││
│                      │  (cache) │    │  (API)   │          ││
│                      └──────────┘    └────┬─────┘          ││
│                                           │                ││
└───────────────────────────────────────────┼────────────────┘│
                                            │ HTTPS
┌───────────────────────────────────────────┼────────────────┐
│                     GOOGLE CLOUD                            │
│                            │                                │
│   ┌────────────────────────▼────────────────────────┐      │
│   │              APPS SCRIPT (API REST)              │      │
│   │                                                  │      │
│   │  function doGet(e) { ... }   // Leer datos      │      │
│   │  function doPost(e) { ... }  // Escribir datos  │      │
│   └──────────────────────┬───────────────────────────┘      │
│                          │                                  │
│   ┌──────────────────────▼───────────────────────────┐     │
│   │           GOOGLE SHEETS (14 hojas)               │     │
│   │                                                  │     │
│   │  usuarios | metricas_zepp | peso_log | ...       │     │
│   └──────────────────────────────────────────────────┘     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. USUARIO OBJETIVO

| Campo | Valor |
|-------|-------|
| Único usuario | El desarrollador |
| Edad | 24 años (20/07/2001) |
| Altura | 188 cm |
| Peso actual | 78.2 kg |
| Objetivo | 83 kg, 15% grasa |
| Nivel | Intermedio-avanzado |
| Restricciones | Halal, Ramadán |

### Dispositivos
| Dispositivo | Modelo | Uso |
|-------------|--------|-----|
| Móvil | Xiaomi Redmi Note 14 Pro 5G | App principal |
| Wearable | Amazfit GTS 4 | Sueño, HRV, FC |
| Báscula | Xiaomi Mi Scale | Peso, composición |

---

## 4. FLUJOS PRINCIPALES

### 4.1 Flujo Mañana (10 segundos)
```
Usuario despierta
    │
    ▼
Abre app ──▶ Splash (0.5s)
    │
    ▼
HOME: Ver macros del día
    │
    ├── 3280 kcal (si entrena)
    ├── P 156g | C 488g | G 78g
    ├── 8000 pasos objetivo
    └── 3.0L agua
    │
    ▼
"Hoy: Push Day" ──▶ (tap para ver preview)
```

### 4.2 Flujo Gym (60-90 min)
```
Tap "Empezar Entreno"
    │
    ▼
PREVIEW: "Push Day - 6 ejercicios - ~75 min"
    │
    ▼
[ ▶ EMPEZAR ]
    │
    ▼
EJERCICIO 1/6:
    ┌────────────────────┐
    │  PRESS INCLINADO   │
    │       18 kg        │  ◄── Peso sugerido
    │      10 reps       │  ◄── Reps objetivo
    │    Serie 1/4       │
    └────────────────────┘
    │
    ▼
Usuario hace la serie
    │
    ▼
◀ SWIPE IZQUIERDA ▶  (o tap "Hecho")
    │
    ▼
Modal: "¿Cuántas hiciste?"
    │
    ├── 8-9-10-11-12 (opciones rápidas)
    └── Input numérico
    │
    ▼
Modal: "¿Peso usado?"
    │
    ├── 16-18-20-22 (alrededor del sugerido)
    └── Input numérico
    │
    ▼
Modal: "¿RIR?" (opcional, tap para skip)
    │
    ├── 0-1-2-3-4+
    └── Skip
    │
    ▼
TIMER DESCANSO (pantalla completa)
    │
    ├── 120s (conteo regresivo)
    ├── Vibra cuando termina
    └── Timer también en notificación
    │
    ▼
Vuelve a EJERCICIO (Serie 2/4)
    │
    ... repite hasta última serie
    │
    ▼
Avanza a EJERCICIO 2/6
    │
    ... repite hasta 6/6
    │
    ▼
RESUMEN:
    ├── Tiempo total: 72 min
    ├── Series: 24
    ├── Volumen: 4,320 kg
    └── Sync a Sheets
```

### 4.3 Flujo Nutrición (durante el día)
```
Usuario come algo
    │
    ▼
Abre app ──▶ Pantalla NUTRICIÓN
    │
    ▼
LOG COMIDA:
    ├── Hora (auto o manual)
    ├── Descripción (texto libre)
    ├── Proteína (g)
    ├── Carbos (g)
    └── Grasa (g)
    │
    ▼
Guarda ──▶ Actualiza totales del día
```

---

## 5. ESTRUCTURA DE CÓDIGO ANDROID

```
FitBase/
├── app/src/main/java/com/fitbase/
│   │
│   ├── FitBaseApp.java              # Application class
│   │
│   ├── data/
│   │   ├── api/
│   │   │   ├── FitBaseApi.java      # Interface Retrofit
│   │   │   └── ApiClient.java       # Singleton Retrofit
│   │   │
│   │   ├── model/                   # POJOs
│   │   │   ├── Usuario.java
│   │   │   ├── Sesion.java
│   │   │   ├── Ejercicio.java
│   │   │   ├── EjercicioLog.java
│   │   │   ├── MetricasZepp.java
│   │   │   ├── PesoLog.java
│   │   │   ├── ComidaLog.java
│   │   │   ├── PlanAnual.java
│   │   │   └── PlanSemanal.java
│   │   │
│   │   ├── local/                   # Room
│   │   │   ├── AppDatabase.java
│   │   │   ├── SesionDao.java
│   │   │   └── CacheDao.java
│   │   │
│   │   └── repository/
│   │       ├── SesionRepository.java
│   │       ├── MetricasRepository.java
│   │       └── NutricionRepository.java
│   │
│   ├── ui/
│   │   ├── splash/
│   │   │   └── SplashActivity.java
│   │   │
│   │   ├── home/
│   │   │   ├── HomeActivity.java
│   │   │   └── HomeViewModel.java
│   │   │
│   │   ├── workout/
│   │   │   ├── WorkoutActivity.java
│   │   │   ├── WorkoutViewModel.java
│   │   │   ├── EjercicioFragment.java
│   │   │   ├── TimerFragment.java
│   │   │   └── ResumenActivity.java
│   │   │
│   │   ├── nutricion/
│   │   │   ├── NutricionActivity.java
│   │   │   └── NutricionViewModel.java
│   │   │
│   │   ├── plan/
│   │   │   ├── PlanAnualActivity.java
│   │   │   └── PlanSemanalActivity.java
│   │   │
│   │   └── common/
│   │       ├── RirSelectorView.java
│   │       └── MacrosCardView.java
│   │
│   ├── service/
│   │   └── TimerService.java        # Foreground service
│   │
│   └── util/
│       ├── Constantes.java
│       ├── Formateador.java
│       └── RedUtils.java
│
├── app/src/main/res/
│   ├── layout/
│   │   ├── activity_splash.xml
│   │   ├── activity_home.xml
│   │   ├── activity_workout.xml
│   │   ├── fragment_ejercicio.xml
│   │   ├── fragment_timer.xml
│   │   ├── activity_resumen.xml
│   │   ├── activity_nutricion.xml
│   │   ├── activity_plan_anual.xml
│   │   ├── activity_plan_semanal.xml
│   │   ├── dialog_reps.xml
│   │   ├── dialog_peso.xml
│   │   └── dialog_rir.xml
│   │
│   ├── values/
│   │   ├── colors.xml
│   │   ├── strings.xml
│   │   ├── dimens.xml
│   │   └── themes.xml
│   │
│   └── drawable/
│       └── (iconos)
│
└── build.gradle (app)
```

---

## 6. CÓDIGO APPS SCRIPT

### Estructura
```javascript
// Archivo: Codigo.gs

// ================== CONFIGURACIÓN ==================
const SPREADSHEET_ID = 'TU_ID_AQUI';
const HOJAS = {
  usuarios: 'usuarios',
  metricasZepp: 'metricas_zepp',
  pesoLog: 'peso_log',
  sesionesPlan: 'sesiones_plan',
  ejerciciosPlan: 'ejercicios_plan',
  ejerciciosLog: 'ejercicios_log',
  progresionLog: 'progresion_log',
  comidasLog: 'comidas_log',
  hidratacionLog: 'hidratacion_log',
  suplementosLog: 'suplementos_log',
  excepcionesLog: 'excepciones_log',
  planAnual: 'plan_anual',
  planSemanal: 'plan_semanal',
  ejerciciosCatalogo: 'ejercicios_catalogo'
};

// ================== ENDPOINTS ==================
function doGet(e) {
  const accion = e.parameter.accion;
  const userId = e.parameter.userId;
  
  let respuesta;
  
  switch(accion) {
    case 'getSesionHoy':
      respuesta = getSesionHoy(userId);
      break;
    case 'getMetricasHoy':
      respuesta = getMetricasHoy(userId);
      break;
    case 'getPlanAnual':
      respuesta = getPlanAnual(userId);
      break;
    case 'getPlanSemanal':
      respuesta = getPlanSemanal(userId, e.parameter.semana);
      break;
    // ... más casos
    default:
      respuesta = { error: 'Acción no válida' };
  }
  
  return ContentService.createTextOutput(JSON.stringify(respuesta))
    .setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  const datos = JSON.parse(e.postData.contents);
  const accion = datos.accion;
  
  let respuesta;
  
  switch(accion) {
    case 'logEjercicio':
      respuesta = logEjercicio(datos);
      break;
    case 'logComida':
      respuesta = logComida(datos);
      break;
    case 'logPeso':
      respuesta = logPeso(datos);
      break;
    // ... más casos
    default:
      respuesta = { error: 'Acción no válida' };
  }
  
  return ContentService.createTextOutput(JSON.stringify(respuesta))
    .setMimeType(ContentService.MimeType.JSON);
}

// ================== FUNCIONES GET ==================
function getSesionHoy(userId) {
  const hoja = SpreadsheetApp.openById(SPREADSHEET_ID)
    .getSheetByName(HOJAS.sesionesPlan);
  const datos = hoja.getDataRange().getValues();
  const hoy = Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd');
  
  // Buscar sesión de hoy
  for (let i = 1; i < datos.length; i++) {
    if (datos[i][1] === userId && datos[i][2] === hoy) {
      return {
        sesionId: datos[i][0],
        tipo: datos[i][3],
        fase: datos[i][5],
        ajusteVolumen: datos[i][6],
        duracionEst: datos[i][8]
      };
    }
  }
  
  return { error: 'No hay sesión hoy' };
}

// ... más funciones
```

---

## 7. CONSTANTES DEL SISTEMA

```java
// Constantes.java
public class Constantes {
    
    // API
    public static final String API_URL = "https://script.google.com/macros/s/TU_DEPLOY_ID/exec";
    public static final String USER_ID = "USR_001";
    
    // Macros (calculados en motor_dieta.md)
    public static final int KCAL_ENTRENO = 3280;
    public static final int KCAL_DESCANSO = 2855;
    public static final int PROTEINA_G = 156;
    public static final int CARBOS_G = 488;
    public static final int GRASA_G = 78;
    
    // Objetivos diarios
    public static final int PASOS_OBJETIVO = 8000;
    public static final float AGUA_LITROS = 3.0f;
    
    // Timers (segundos)
    public static final int DESCANSO_COMPUESTO = 180;  // 3 min
    public static final int DESCANSO_AISLADO = 90;     // 1.5 min
    public static final int DESCANSO_CORE = 60;        // 1 min
    
    // UI
    public static final int SPLASH_DURACION_MS = 500;
    
    // Usuario
    public static final String FECHA_NACIMIENTO = "2001-07-20";
    public static final int ALTURA_CM = 188;
    public static final float PESO_ACTUAL = 78.2f;
    public static final float PESO_OBJETIVO = 83.0f;
}
```

---

## 8. DEPENDENCIAS (build.gradle)

```groovy
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.fitbase'
    compileSdk 34
    
    defaultConfig {
        applicationId "com.fitbase"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }
    
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    // Core Android
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
    
    // Lifecycle (ViewModel + LiveData)
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-runtime:2.7.0'
    
    // Networking
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    
    // Local Cache
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    
    // Utilidades
    implementation 'com.google.code.gson:gson:2.10.1'
}
```

---

## 9. ORDEN DE GENERACIÓN

### Fase 1: Backend (Apps Script)
```
1. Codigo.gs (principal)
   - Configuración
   - doGet() / doPost()
   - Funciones CRUD por hoja
   
2. Motor.gs (lógica)
   - Cálculo de cargas (motor_pesos.md)
   - Ajuste por HRV (Kiviniemi)
   - Generación de sesiones
```

### Fase 2: Android Base
```
1. Estructura del proyecto
2. build.gradle (app + project)
3. AndroidManifest.xml
4. Constantes.java
5. Modelos (POJOs)
6. API interface (FitBaseApi.java)
7. ApiClient.java (Retrofit singleton)
8. Room (AppDatabase + DAOs)
9. Repositories
```

### Fase 3: Android UI
```
1. themes.xml + colors.xml
2. SplashActivity
3. HomeActivity + ViewModel
4. WorkoutActivity + ViewModel
5. EjercicioFragment
6. TimerFragment + TimerService
7. ResumenActivity
8. NutricionActivity
9. PlanAnualActivity
10. PlanSemanalActivity
```

### Fase 4: Testing
```
1. Probar endpoints Apps Script manualmente
2. Conectar app a endpoints reales
3. Probar flujo completo gym
4. Verificar sync offline/online
```

---

## 10. ARCHIVOS DE REFERENCIA

| Necesidad | Archivo |
|-----------|---------|
| Esquema BD completo | [base_datos.md](../logica/base_datos.md) |
| Wireframes todas las pantallas | [ui.md](ui.md) |
| Convenciones código | [compilador.md](compilador.md) |
| Algoritmo cargas | [motor_pesos.md](../logica/motor_pesos.md) |
| Cálculo macros | [motor_dieta.md](../nutricion/motor_dieta.md) |
| Datos usuario | [biometria.md](../../usuario/biometria.md) |
| Horarios | [horarios.md](../../usuario/perfil/horarios.md) |
| Equipamiento gym | [equipamiento.md](../../usuario/equipamiento.md) |

---

## 11. VALIDACIONES ANTES DE GENERAR

- [ ] ¿El código es Java (NO Kotlin)?
- [ ] ¿Usa Views/XML (NO Compose)?
- [ ] ¿Sigue estructura MVVM?
- [ ] ¿Tiene nombres en español?
- [ ] ¿Es simple y legible?
- [ ] ¿No tiene dependencias externas raras?
- [ ] ¿Usa las constantes de Constantes.java?
- [ ] ¿El timer funciona como foreground service?
- [ ] ¿Hay cache offline con Room?
- [ ] ¿Los colores son modo oscuro?

---

**FECHA OBJETIVO: 1 de Septiembre de 2026**
