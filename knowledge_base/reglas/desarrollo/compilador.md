---
id: "REG-DEV-02"
nombre: "Reglas de Código"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["SYS-00", "REG-DEV-01", "REG-LOG-02"]
tags: ["reglas", "desarrollo", "codigo", "java", "android"]
---

# Reglas de Código

## 1. Alcance
Reglas para la generación de código por parte de la IA.

---

## 2. Arquitectura General

```
┌─────────────────────────────────────────────────────────┐
│                    DISPOSITIVO MÓVIL                      │
│                (Xiaomi Redmi Note 14 Pro 5G)              │
│                                                           │
│   ┌───────────────────────────────────────────────┐   │
│   │              APP FITBASE (Java)                  │   │
│   │   - Toda la lógica se ejecuta AQUÍ              │   │
│   │   - UI, timers, cálculos, cache                 │   │
│   │   - Room DB para cache offline                   │   │
│   └───────────────────────────────────────────────┘   │
│                         │                                 │
└─────────────────────────┼───────────────────────────────┘
                          │ HTTP/REST (cuando hay red)
                          ▼
┌─────────────────────────────────────────────────────────┐
│                     GOOGLE CLOUD                          │
│                                                           │
│   ┌─────────────────┐   ┌───────────────────────┐   │
│   │  Apps Script    │   │    Google Sheets      │   │
│   │   (API REST)    │──▶│    (Base de Datos)    │   │
│   │                 │   │                       │   │
│   │  - doGet()      │   │  - usuarios           │   │
│   │  - doPost()     │   │  - sesiones_plan      │   │
│   │  - Sin hosting  │   │  - ejercicios_log     │   │
│   │  - Gratis       │   │  - etc (12 hojas)     │   │
│   └─────────────────┘   └───────────────────────┘   │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

### Ventajas de esta arquitectura
- **Sin hosting**: Todo gratis (Apps Script + Sheets)
- **Sin servidor**: No hay que mantener nada
- **Offline**: La app funciona sin red, sync cuando vuelve
- **Simple**: El usuario ya conoce Apps Script

---

## 3. Stack Tecnológico

| Componente | Tecnología | Versión | Notas |
|------------|------------|---------|-------|
| **Frontend** | **Java** + Views/XML | Java 17+ | Usuario domina Java |
| UI Framework | Android Views | — | NO Jetpack Compose |
| "Backend" | Google Apps Script | V8 Runtime | Solo API, sin lógica pesada |
| Base de datos | Google Sheets | — | 12 hojas (REG-LOG-02) |
| API | REST (Apps Script Web App) | — | JSON |
| Min SDK | 35 (Android 15) | — | Requerido por Hyper Island (HyperOS 3, ver REG-DEV-04) — antes 26/~95% dispositivos, aceptable por ser app de un único usuario |
| Target SDK | 36 (Android 16) | — | Última estable |
| Compile SDK | 36 | — | AGP 8.9.1+ requerido |
| Cache local | Room | 2.6.1 | Para offline |

> **IMPORTANTE**: Java, NO Kotlin. El usuario debe poder leer y modificar el código.
> Excepción puntual: puentes finos hacia SDKs que son Kotlin-first y no
> exponen una API cómoda desde Java (`HealthConnectBridge.kt` para Health
> Connect, `HyperIslandTimerBridge.kt` para Hyper Island/HyperOS — ver §16).
> Toda la lógica de negocio sigue en Java; estos ficheros solo traducen llamadas.

---

## 3. Estructura del Proyecto Android

```
FitBase/
├── app/
│   ├── src/main/
│   │   ├── java/com/fitbase/
│   │   │   ├── data/                    # Capa de datos
│   │   │   │   ├── api/                 # Retrofit interfaces
│   │   │   │   │   └── FitBaseApi.java
│   │   │   │   ├── model/               # POJOs / DTOs
│   │   │   │   │   ├── Sesion.java
│   │   │   │   │   ├── Ejercicio.java
│   │   │   │   │   ├── EjercicioLog.java
│   │   │   │   │   └── MetricasDia.java
│   │   │   │   ├── repository/          # Repositorios
│   │   │   │   │   ├── SesionRepository.java
│   │   │   │   │   └── MetricasRepository.java
│   │   │   │   └── local/               # Room / SharedPrefs
│   │   │   │       └── AppDatabase.java
│   │   │   │
│   │   │   ├── ui/                      # Capa de presentación
│   │   │   │   ├── home/                # Pantalla mañana
│   │   │   │   │   ├── HomeActivity.java
│   │   │   │   │   └── HomeViewModel.java
│   │   │   │   ├── workout/             # Pantalla gym
│   │   │   │   │   ├── WorkoutActivity.java
│   │   │   │   │   ├── WorkoutViewModel.java
│   │   │   │   │   ├── ExerciseFragment.java
│   │   │   │   │   └── TimerFragment.java
│   │   │   │   ├── summary/             # Pantalla fin
│   │   │   │   │   └── SummaryActivity.java
│   │   │   │   └── common/              # Componentes reutilizables
│   │   │   │       ├── RirSelectorView.java
│   │   │   │       └── CountdownTimerView.java
│   │   │   │
│   │   │   ├── service/                 # Servicios Android
│   │   │   │   └── TimerService.java    # Foreground service para timer
│   │   │   │
│   │   │   ├── util/                    # Utilidades
│   │   │   │   ├── Constants.java
│   │   │   │   └── NetworkUtils.java
│   │   │   │
│   │   │   └── FitBaseApp.java          # Application class
│   │   │
│   │   ├── res/
│   │   │   ├── layout/                  # XMLs de UI
│   │   │   │   ├── activity_home.xml
│   │   │   │   ├── activity_workout.xml
│   │   │   │   ├── fragment_exercise.xml
│   │   │   │   ├── fragment_timer.xml
│   │   │   │   └── view_rir_selector.xml
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   ├── dimens.xml
│   │   │   │   └── themes.xml
│   │   │   └── drawable/
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   └── build.gradle
│
├── build.gradle                         # Project-level
├── settings.gradle
└── gradle.properties
```

---

## 4. Dependencias Android (build.gradle app)

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
        versionName "1.0"
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    // AndroidX
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'
    
    // Networking
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    
    // Local DB (cache offline)
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    
    // Swipe gestures
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
    
    // Health Connect (lectura de métricas)
    implementation 'androidx.health.connect:connect-client:1.1.0-alpha07'
}
```

---

## 5. Convenciones de Código Java

### Nomenclatura
```java
// Clases: PascalCase
public class WorkoutActivity extends AppCompatActivity { }

// Variables: camelCase, nombres en español para negocio
private int serieActual;
private float pesoSugerido;
private List<Ejercicio> ejerciciosDelDia;

// Constantes: UPPER_SNAKE_CASE
public static final int TIMER_DESCANSO_DEFAULT_MS = 120000;
public static final String EXTRA_SESION_ID = "sesion_id";

// Métodos: camelCase, verbos
public void iniciarSerie() { }
public void guardarLog() { }
private void calcularVolumenTotal() { }
```

### Comentarios
```java
/**
 * Calcula el peso sugerido para la próxima serie.
 * 
 * Referencia: REG-LOG-01 (motor_pesos.md)
 * Regla ACSM: Si completó +1-2 reps sobre objetivo → subir 2.5-5%
 * 
 * @param ultimoLog Log de la serie anterior
 * @return Peso sugerido en kg
 */
public float calcularPesoSugerido(EjercicioLog ultimoLog) {
    // Si completó más reps de las objetivo con RIR >= objetivo
    if (ultimoLog.getRepsCompletadas() >= ultimoLog.getRepsObjetivo() + 1 
        && ultimoLog.getRirPercibido() >= ultimoLog.getRirObjetivo()) {
        return ultimoLog.getPesoUsado() * 1.025f; // +2.5%
    }
    return ultimoLog.getPesoUsado();
}
```

### Patrón de Arquitectura
```
┌─────────────────────────────────────────────────────┐
│                    Activity/Fragment                │
│                    (solo UI, delegación)            │
└─────────────────────────┬───────────────────────────┘
                          │ observa LiveData
                          ▼
┌─────────────────────────────────────────────────────┐
│                     ViewModel                       │
│                  (estado UI, lógica simple)         │
└─────────────────────────┬───────────────────────────┘
                          │ llama métodos
                          ▼
┌─────────────────────────────────────────────────────┐
│                    Repository                       │
│               (decide: API o cache local)           │
└───────────┬─────────────────────────────┬───────────┘
            │                             │
            ▼                             ▼
┌───────────────────┐         ┌───────────────────────┐
│    FitBaseApi     │         │    Room Database      │
│  (Retrofit/REST)  │         │   (cache offline)     │
└───────────────────┘         └───────────────────────┘
```

---

## 6. Estructura Apps Script (Backend)

```
FitBase-Backend/
├── Main.gs                  # Endpoints REST (doGet, doPost)
├── Config.gs                # Constantes, IDs de sheets
├── Services/
│   ├── SesionService.gs     # Lógica de sesiones
│   ├── EjercicioService.gs  # Lógica de ejercicios
│   ├── MetricasService.gs   # Lógica de métricas Zepp
│   ├── MotorPesos.gs        # Motor de cargas (REG-LOG-01)
│   └── MotorDieta.gs        # Motor de dieta (REG-NUT-01)
├── Utils/
│   ├── SheetUtils.gs        # Helpers para Sheets
│   ├── DateUtils.gs         # Helpers de fechas
│   └── Validators.gs        # Validaciones
└── Models/
    └── Types.gs             # JSDoc types
```

### Convenciones Apps Script
```javascript
/**
 * Obtiene la sesión del día para un usuario.
 * 
 * @param {string} userId - ID del usuario
 * @param {string} fecha - Fecha en formato YYYY-MM-DD
 * @return {Object} Sesión del día con ejercicios
 */
function GetSesionDelDia(userId, fecha) {
  // Funciones públicas: PascalCase
}

function _calcularAjusteHrv(hrvActual, hrvMedia, hrvSd) {
  // Funciones privadas: _camelCase
}

const SHEET_SESIONES = 'sesiones_plan';
const SHEET_EJERCICIOS = 'ejercicios_plan';
// Constantes: UPPER_SNAKE_CASE
```

---

## 7. API REST (Apps Script Web App)

### Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `?action=getSesionHoy&userId=X` | Sesión del día |
| GET | `?action=getMetricasDia&userId=X` | Macros, pasos, agua |
| POST | `?action=guardarLog` | Guardar ejercicio_log |
| POST | `?action=finalizarSesion` | Cerrar sesión |

### Formato Respuesta
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

---

## 8. Reglas de la IA

### PROHIBICIONES
1. **NUNCA** usar Kotlin → Solo Java
2. **NUNCA** usar Jetpack Compose → Solo Views/XML
3. **NUNCA** inventar reglas de negocio no documentadas
4. **NUNCA** usar conocimiento pre-entrenado para lógica de dominio
5. **NUNCA** generar código sin referencia a archivos de reglas

### OBLIGACIONES
1. **SIEMPRE** referenciar el archivo de reglas que justifica la lógica
2. **SIEMPRE** comentar la referencia en el código
3. **SIEMPRE** usar nombres en español para variables de negocio
4. **SIEMPRE** validar contra el manifest antes de crear archivos nuevos
5. **SIEMPRE** incluir manejo de errores (try-catch)

---

## 9. Formato de Respuesta al Generar Código

```markdown
## Archivo: [ruta/archivo.java]
### Referencia: [REG-XX-YY] (archivo_reglas.md)
### Justificación: [Por qué este código sigue las reglas]

\`\`\`java
// código aquí
\`\`\`
```

---

## 10. Checklist Pre-Código

Antes de generar código, verificar:

- [ ] ¿La lógica está documentada en `/knowledge_base/`?
- [ ] ¿Hay evidencia científica si es regla de entrenamiento/nutrición?
- [ ] ¿El archivo está en la estructura correcta?
- [ ] ¿Los nombres siguen las convenciones?
- [ ] ¿Hay comentarios con referencias?
- [ ] ¿Hay manejo de errores?
- [ ] ¿Funciona offline (cache local)?

---

## 11. Modelos de Datos (POJOs)

> Estos modelos corresponden a las hojas de Google Sheets definidas en [base_datos.md](../logica/base_datos.md)

### Usuario.java
```java
package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

public class Usuario {
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("nombre")
    private String nombre;
    
    @SerializedName("nacimiento")
    private String nacimiento;
    
    @SerializedName("alturaCm")
    private int alturaCm;
    
    @SerializedName("sexo")
    private String sexo;
    
    @SerializedName("objetivo")
    private String objetivo; // "bulk", "cut", "mantener"
    
    @SerializedName("diasEntreno")
    private int diasEntreno;
    
    @SerializedName("split")
    private String split;
    
    @SerializedName("ramadan")
    private boolean ramadan;
    
    @SerializedName("halal")
    private boolean halal;
    
    // Constructor vacío para Gson
    public Usuario() {}
    
    // Getters y setters...
}
```

### Sesion.java
```java
package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Sesion {
    @SerializedName("sesionId")
    private String sesionId;
    
    @SerializedName("fecha")
    private String fecha;
    
    @SerializedName("tipo")
    private String tipo; // "Push", "Pull", "Upper", "Lower"
    
    @SerializedName("semanaMeso")
    private int semanaMeso;
    
    @SerializedName("fase")
    private String fase; // "acumulacion", "intensificacion", "deload"
    
    @SerializedName("ajusteVolumen")
    private float ajusteVolumen; // 0.8 a 1.0
    
    @SerializedName("razonAjuste")
    private String razonAjuste;
    
    @SerializedName("duracionEst")
    private int duracionEst; // minutos
    
    @SerializedName("completada")
    private boolean completada;
    
    // Lista de ejercicios de la sesión
    private List<Ejercicio> ejercicios;
    
    public Usuario() {}
    
    // Getters y setters...
}
```

### Ejercicio.java
```java
package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

public class Ejercicio {
    @SerializedName("planId")
    private String planId;
    
    @SerializedName("ejercicioId")
    private String ejercicioId;
    
    @SerializedName("nombre")
    private String nombre;
    
    @SerializedName("grupo")
    private String grupo;
    
    @SerializedName("orden")
    private int orden;
    
    @SerializedName("seriesPlan")
    private int seriesPlan;
    
    @SerializedName("repsPlan")
    private String repsPlan; // "8-10"
    
    @SerializedName("pesoSugerido")
    private float pesoSugerido;
    
    @SerializedName("descansoSeg")
    private int descansoSeg;
    
    // Para tracking en tiempo real
    private int serieActual = 0;
    private boolean completado = false;
    
    public Ejercicio() {}
    
    // Getters y setters...
    
    // Helpers
    public int getRepMin() {
        String[] parts = repsPlan.split("-");
        return Integer.parseInt(parts[0]);
    }
    
    public int getRepMax() {
        String[] parts = repsPlan.split("-");
        return parts.length > 1 ? Integer.parseInt(parts[1]) : getRepMin();
    }
}
```

### EjercicioLog.java
```java
package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

public class EjercicioLog {
    @SerializedName("logId")
    private String logId;
    
    @SerializedName("planId")
    private String planId;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("serie")
    private int serie;
    
    @SerializedName("repsReal")
    private int repsReal;
    
    @SerializedName("pesoReal")
    private float pesoReal;
    
    @SerializedName("rir")
    private Integer rir; // Nullable
    
    @SerializedName("tempo")
    private String tempo; // Nullable
    
    @SerializedName("notas")
    private String notas; // Nullable
    
    public EjercicioLog() {}
    
    // Constructor para crear desde UI
    public EjercicioLog(String planId, int serie, int reps, float peso) {
        this.planId = planId;
        this.serie = serie;
        this.repsReal = reps;
        this.pesoReal = peso;
    }
    
    // Getters y setters...
}
```

### MetricasZepp.java
```java
package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

public class MetricasZepp {
    @SerializedName("sleepScore")
    private int sleepScore;
    
    @SerializedName("sleepHoras")
    private float sleepHoras;
    
    @SerializedName("sleepDeep")
    private int sleepDeep; // minutos
    
    @SerializedName("sleepRem")
    private int sleepRem; // minutos
    
    @SerializedName("hrvRmssd")
    private int hrvRmssd;
    
    @SerializedName("hrReposo")
    private int hrReposo;
    
    @SerializedName("readiness")
    private int readiness;
    
    @SerializedName("stressAvg")
    private int stressAvg;
    
    @SerializedName("pasosAyer")
    private int pasosAyer;
    
    @SerializedName("caloriasActivas")
    private int caloriasActivas;
    
    public MetricasZepp() {}
    
    // Getters y setters...
    
    /**
     * Determina si el HRV indica recuperación baja
     * Referencia: REG-LOG-01 (motor_pesos.md) - Protocolo Kiviniemi
     */
    public boolean esHrvBajo(float mediaHrv10d, float sdHrv10d) {
        return hrvRmssd < (mediaHrv10d - sdHrv10d);
    }
}
```

### MacrosDia.java
```java
package com.fitbase.data.model;

/**
 * Macros calculados para un día específico
 * Referencia: REG-NUT-01 (motor_dieta.md)
 */
public class MacrosDia {
    private int kcalObjetivo;
    private int proteinaG;
    private int carbosG;
    private int grasaG;
    private boolean esEntrenamiento;
    
    // Valores consumidos (tracking)
    private int kcalConsumidas = 0;
    private int proteinaConsumida = 0;
    private int carbosConsumidos = 0;
    private int grasaConsumida = 0;
    
    public MacrosDia(boolean entrena) {
        this.esEntrenamiento = entrena;
        // Valores de motor_dieta.md
        if (entrena) {
            this.kcalObjetivo = 3280;
            this.proteinaG = 156;
            this.carbosG = 488;
            this.grasaG = 78;
        } else {
            this.kcalObjetivo = 2855;
            this.proteinaG = 156;
            this.carbosG = 380;
            this.grasaG = 78;
        }
    }
    
    // Getters, setters, y métodos para añadir comida...
    
    public int getKcalRestantes() {
        return kcalObjetivo - kcalConsumidas;
    }
}
```

---

## 12. API Interface (Retrofit)

```java
package com.fitbase.data.api;

import com.fitbase.data.model.*;
import retrofit2.Call;
import retrofit2.http.*;

public interface FitBaseApi {
    
    // ==================== GET ====================
    
    @GET("exec")
    Call<ApiResponse<Usuario>> getUsuario(
        @Query("accion") String accion,
        @Query("userId") String userId
    );
    
    @GET("exec")
    Call<ApiResponse<Sesion>> getSesionHoy(
        @Query("accion") String accion,
        @Query("userId") String userId
    );
    
    @GET("exec")
    Call<ApiResponse<EjerciciosResponse>> getEjerciciosSesion(
        @Query("accion") String accion,
        @Query("sesionId") String sesionId
    );
    
    @GET("exec")
    Call<ApiResponse<MetricasZepp>> getMetricasHoy(
        @Query("accion") String accion,
        @Query("userId") String userId
    );
    
    @GET("exec")
    Call<ApiResponse<PlanAnualResponse>> getPlanAnual(
        @Query("accion") String accion,
        @Query("userId") String userId,
        @Query("anio") int anio
    );
    
    // ==================== POST ====================
    
    @POST("exec")
    Call<ApiResponse<LogResponse>> logEjercicio(@Body LogEjercicioRequest request);
    
    @POST("exec")
    Call<ApiResponse<LogResponse>> logComida(@Body LogComidaRequest request);
    
    @POST("exec")
    Call<ApiResponse<LogResponse>> logPeso(@Body LogPesoRequest request);
    
    @POST("exec")
    Call<ApiResponse<Void>> completarSesion(@Body CompletarSesionRequest request);
}
```

### ApiResponse wrapper
```java
package com.fitbase.data.model;

public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String error;
    private String mensaje;
    
    public boolean isSuccess() {
        return success || error == null;
    }
    
    // Getters...
}
```

---

## 13. Colores (Modo Oscuro)

```xml
<!-- res/values/colors.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Fondo principal -->
    <color name="background_primary">#121212</color>
    <color name="background_surface">#1E1E1E</color>
    <color name="background_card">#2D2D2D</color>
    
    <!-- Texto -->
    <color name="text_primary">#FFFFFF</color>
    <color name="text_secondary">#B3B3B3</color>
    <color name="text_disabled">#666666</color>
    
    <!-- Acentos -->
    <color name="accent_primary">#BB86FC</color>    <!-- Morado Material -->
    <color name="accent_secondary">#03DAC6</color>  <!-- Cyan Material -->
    
    <!-- Estados -->
    <color name="success">#4CAF50</color>
    <color name="warning">#FF9800</color>
    <color name="error">#F44336</color>
    
    <!-- Macros -->
    <color name="macro_proteina">#E91E63</color>    <!-- Rosa -->
    <color name="macro_carbos">#FFC107</color>      <!-- Amarillo -->
    <color name="macro_grasa">#2196F3</color>       <!-- Azul -->
</resources>
```

---

## 14. Dimensiones

```xml
<!-- res/values/dimens.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Márgenes -->
    <dimen name="margin_xs">4dp</dimen>
    <dimen name="margin_s">8dp</dimen>
    <dimen name="margin_m">16dp</dimen>
    <dimen name="margin_l">24dp</dimen>
    <dimen name="margin_xl">32dp</dimen>
    
    <!-- Texto -->
    <dimen name="text_title_giant">72sp</dimen>  <!-- Número grande kcal -->
    <dimen name="text_title_large">32sp</dimen>  <!-- Nombre ejercicio -->
    <dimen name="text_title">24sp</dimen>
    <dimen name="text_subtitle">18sp</dimen>
    <dimen name="text_body">16sp</dimen>
    <dimen name="text_caption">12sp</dimen>
    
    <!-- Cards -->
    <dimen name="card_corner_radius">12dp</dimen>
    <dimen name="card_elevation">4dp</dimen>
    
    <!-- Botones -->
    <dimen name="button_height">56dp</dimen>
    <dimen name="button_corner_radius">28dp</dimen>
</resources>
```

---

## 15. Health Connect - Integración

> Referencia: [USR-MET-01](../../usuario/metricas/hardware.md)

### Permisos en AndroidManifest.xml
```xml
<!-- Health Connect permissions -->
<uses-permission android:name="android.permission.health.READ_STEPS"/>
<uses-permission android:name="android.permission.health.READ_SLEEP"/>
<uses-permission android:name="android.permission.health.READ_HEART_RATE"/>
<uses-permission android:name="android.permission.health.READ_WEIGHT"/>
<uses-permission android:name="android.permission.health.READ_BODY_FAT"/>
<uses-permission android:name="android.permission.health.READ_DISTANCE"/>
<uses-permission android:name="android.permission.health.READ_ACTIVE_CALORIES_BURNED"/>

<!-- Health Connect intent filter (requerido para Privacy Policy) -->
<intent-filter>
    <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"/>
</intent-filter>

<!-- Queries para verificar si Health Connect está instalado -->
<queries>
    <package android:name="com.google.android.apps.healthdata"/>
</queries>
```

### HealthConnectManager.java
```java
package com.fitbase.data.health;

import android.content.Context;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.records.*;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.time.TimeRangeFilter;
import java.time.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manager para leer datos de Health Connect
 * Fuentes: Zepp (Amazfit GTS 4) y Mi Fitness (báscula Xiaomi)
 * Referencia: USR-MET-01 (hardware.md)
 */
public class HealthConnectManager {
    
    private final HealthConnectClient client;
    
    public HealthConnectManager(Context context) {
        this.client = HealthConnectClient.getOrCreate(context);
    }
    
    /**
     * Verifica si Health Connect está disponible e instalado
     */
    public static boolean isAvailable(Context context) {
        int status = HealthConnectClient.getSdkStatus(context);
        return status == HealthConnectClient.SDK_AVAILABLE;
    }
    
    /**
     * Lee el sueño de la última noche
     * Variable: HC_SLEEP_SCORE, HC_SLEEP_DURATION, HC_SLEEP_DEEP, HC_SLEEP_REM
     */
    public CompletableFuture<SleepData> getLastNightSleep() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(Duration.ofHours(24));
        
        TimeRangeFilter timeRange = TimeRangeFilter.between(startTime, endTime);
        ReadRecordsRequest<SleepSessionRecord> request = 
            new ReadRecordsRequest.Builder<>(SleepSessionRecord.class)
                .setTimeRangeFilter(timeRange)
                .build();
        
        // Implementar lectura asíncrona...
        return null; // TODO: Implementar
    }
    
    /**
     * Lee el peso más reciente
     * Variable: HC_WEIGHT, HC_BODY_FAT
     */
    public CompletableFuture<WeightData> getLatestWeight() {
        // TODO: Implementar
        return null;
    }
    
    /**
     * Lee pasos de hoy
     * Variable: HC_STEPS
     */
    public CompletableFuture<Integer> getTodaySteps() {
        // TODO: Implementar
        return null;
    }
    
    /**
     * Lee FC en reposo más reciente
     * Variable: HC_HR_REST
     */
    public CompletableFuture<Integer> getRestingHeartRate() {
        // TODO: Implementar
        return null;
    }
    
    // Clases de datos internas
    public static class SleepData {
        public int durationMinutes;
        public int deepMinutes;
        public int remMinutes;
        public int lightMinutes;
        public int score; // Calculado: (deep*2 + rem*1.5 + light) / duration * 100
    }
    
    public static class WeightData {
        public float weightKg;
        public float bodyFatPercent;
        public String date;
    }
}
```

### Flujo de Sincronización
```
┌─────────────────────────────────────────────────────────────┐
│                    APP FITBASE - INICIO                      │
├─────────────────────────────────────────────────────────────┤
│ 1. Verificar: HealthConnectClient.getSdkStatus()            │
│    └─ SDK_UNAVAILABLE → Mostrar "Instala Health Connect"    │
│    └─ SDK_AVAILABLE → Continuar                             │
│                                                             │
│ 2. Solicitar permisos (si no concedidos)                    │
│    └─ Mostrar rationale: "FitBase necesita acceso a..."     │
│    └─ Usuario acepta → Guardar estado                       │
│                                                             │
│ 3. Leer datos de Health Connect                             │
│    └─ Sueño última noche                                    │
│    └─ Peso última medición                                  │
│    └─ Pasos de hoy                                          │
│    └─ FC reposo                                             │
│                                                             │
│ 4. Enviar a Google Sheets (metricas_zepp)                   │
│    └─ POST /exec?accion=guardarMetricas                     │
│                                                             │
│ 5. Usar datos para motor_pesos y motor_dieta                │
└─────────────────────────────────────────────────────────────┘
```

### Datos NO Disponibles (Entrada Manual)
Los siguientes datos requieren entrada manual del usuario porque Zepp/Xiaomi no los exportan a Health Connect:

| Métrica | Cómo obtener | Frecuencia |
|---------|--------------|------------|
| HRV (RMSSD) | Usuario mira Zepp app | Diario (opcional) |
| Nivel estrés | Usuario mira Zepp app | Diario (opcional) |

> **Recomendación**: Hacer estos campos opcionales en la UI para no frustrar al usuario.

---

## 16. Hyper Island (HyperOS 3) — Timer de Descanso

> Referencia: [HyperIsland ToolKit](https://github.com/D4vidDf/HyperIsland-ToolKit) (Apache 2.0)

### Por qué
El Redmi Note 14 Pro 5G (dispositivo de referencia) corre HyperOS 3, que tiene
"Hyper Island" — el equivalente Xiaomi a la Dynamic Island de iOS. La
notificación del timer de descanso (`TimerService`) se muestra ahí además de
como notificación normal, sin coste de batería extra: el sistema hace el
"tick" de la cuenta atrás nativamente (igual que ya hacíamos con el
`Chronometer` nativo de Android), la app no refresca nada cada segundo.

### Espectro de plantillas evaluado
La librería expone ~30 métodos de configuración (chat/texto, progreso lineal,
progreso circular, countdown/count-up, highlight, cover, animación, dígitos de
ancho fijo, pasos...). Para un timer de descanso que cuenta HACIA ATRÁS
segundos, la plantilla nativa **Countdown** (`setBigIslandCountdown` +
`setSmallIslandIcon` + `TimerInfo`) es la que mejor encaja — es exactamente el
propósito para el que existe, y evita tener que animar nosotros un progreso
lineal/circular actualizándolo constantemente.

### Implementación
- `HyperIslandTimerBridge.kt` (Kotlin, puente hacia la librería — ver
  excepción en la cabecera de este documento) construye el payload.
- `TimerService.crearNotificacion()` lo añade a la MISMA notificación normal
  vía `notification.extras.putString("miui.focus.param", json)` — en
  dispositivos no compatibles (`HyperIslandNotification.isSupported()` ==
  false) la notificación se comporta exactamente igual que antes, sin cambios.

### Coste: subida de plataforma
La librería exige `minSdk 35` (Android 15+) y `compileSdk 36` / AGP 8.9.1+ —
no hay versión publicada con requisitos menores (Hyper Island es una función
exclusiva de una base Android reciente). Se aceptó subir todo el proyecto
(antes minSdk 26/~95% dispositivos) porque FitBase es una app de un único
usuario (ver `manifest.md`) cuyo dispositivo real ya cumple el requisito.
