---
id: "REG-DEV-03"
nombre: "Manual de Despliegue"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["SYS-00", "REG-DEV-00", "REG-LOG-02"]
tags: ["desarrollo", "despliegue", "manual", "setup"]
---

# 📱 Manual de Despliegue FitBase

> **Objetivo**: App funcionando en tu móvil para el **1 de Septiembre de 2026**

---

## 📋 CHECKLIST PRE-DESPLIEGUE

```
[ ] 1. Google Sheets creado con 14 hojas
[ ] 2. Apps Script desplegado
[ ] 3. Android Studio instalado
[ ] 4. Proyecto Android creado
[ ] 5. App instalada en móvil
[ ] 6. Primer sync exitoso
```

---

## PARTE 1: GOOGLE SHEETS (Base de Datos)

### Paso 1.1: Crear Spreadsheet

1. Ve a [Google Drive](https://drive.google.com)
2. **Nuevo** → **Google Sheets** → **Hoja de cálculo en blanco**
3. Nombrar: `FitBase_DB`
4. Copiar el ID de la URL:
   ```
   https://docs.google.com/spreadsheets/d/ESTE_ES_TU_ID/edit
   ```
   > Guardar este ID, lo necesitarás en Apps Script

### Paso 1.2: Crear las 14 Hojas

Renombrar/crear hojas con estos nombres EXACTOS (respeta minúsculas y guiones bajos):

| # | Nombre de Hoja |
|---|----------------|
| 1 | `usuarios` |
| 2 | `metricas_zepp` |
| 3 | `peso_log` |
| 4 | `sesiones_plan` |
| 5 | `ejercicios_plan` |
| 6 | `ejercicios_log` |
| 7 | `progresion_log` |
| 8 | `comidas_log` |
| 9 | `hidratacion_log` |
| 10 | `suplementos_log` |
| 11 | `excepciones_log` |
| 12 | `plan_anual` |
| 13 | `plan_semanal` |
| 14 | `ejercicios_catalogo` |

### Paso 1.3: Añadir Headers a Cada Hoja

#### Hoja: `usuarios`
```
user_id | str_nombre | date_nacimiento | num_altura_cm | str_sexo | str_objetivo | num_dias_entreno | str_split | bool_ramadan | bool_halal | date_creado | date_modificado
```

#### Hoja: `metricas_zepp`
```
metrica_id | user_id | date_fecha | num_sleep_score | num_sleep_horas | num_sleep_deep_min | num_sleep_rem_min | num_hrv_rmssd | num_hr_reposo | num_readiness | num_stress_avg | num_pasos_ayer | num_calorias_activas | date_sync
```

#### Hoja: `peso_log`
```
peso_id | user_id | date_fecha | num_peso_kg | num_grasa_pct | num_musculo_kg | str_fuente | str_condiciones | date_creado
```

#### Hoja: `sesiones_plan`
```
sesion_id | user_id | date_fecha | str_tipo | num_semana_meso | str_fase | num_ajuste_volumen | str_razon_ajuste | num_duracion_est_min | bool_completada | date_inicio | date_fin | date_creado
```

#### Hoja: `ejercicios_plan`
```
plan_id | sesion_id | ejercicio_id | num_orden | num_series_plan | num_reps_plan | num_peso_sugerido | num_descanso_seg | str_notas | date_creado
```

#### Hoja: `ejercicios_log`
```
log_id | plan_id | user_id | num_serie | num_reps_real | num_peso_real | num_rir | num_tempo | str_notas | date_creado
```

#### Hoja: `progresion_log`
```
prog_id | user_id | ejercicio_id | date_fecha | num_peso_anterior | num_peso_nuevo | num_reps_anterior | num_reps_nuevo | str_razon | date_creado
```

#### Hoja: `comidas_log`
```
comida_id | user_id | date_fecha | str_hora | str_tipo | str_descripcion | num_kcal | num_proteina_g | num_carbos_g | num_grasa_g | date_creado
```

#### Hoja: `hidratacion_log`
```
hidra_id | user_id | date_fecha | num_litros | date_creado
```

#### Hoja: `suplementos_log`
```
supp_id | user_id | date_fecha | str_suplemento | num_cantidad | str_unidad | bool_tomado | date_creado
```

#### Hoja: `excepciones_log`
```
excep_id | user_id | date_inicio | date_fin | str_tipo | str_descripcion | num_ajuste_volumen | num_ajuste_kcal | date_creado
```

#### Hoja: `plan_anual`
```
plan_id | user_id | num_anio | num_semana | str_fase | str_objetivo | num_volumen_pct | str_notas | date_creado
```

#### Hoja: `plan_semanal`
```
semana_id | user_id | num_anio | num_semana | str_dia | str_tipo_sesion | str_ejercicios | num_volumen_total | str_notas | date_creado
```

#### Hoja: `ejercicios_catalogo`
```
ejercicio_id | str_nombre | str_grupo | str_patron | str_equipo | bool_compuesto | num_descanso_default | str_video_url | str_notas
```

### Paso 1.4: Insertar Datos Iniciales

#### En `usuarios` (fila 2):
```
USR_001 | Usuario | 2001-07-20 | 188 | M | bulk | 4 | Push/Pull/Upper/Lower | true | true | 2026-06-18T10:00:00 | 2026-06-18T10:00:00
```

#### En `ejercicios_catalogo` (datos mínimos):
```
EJE_PRESS_BANCA | Press banca | Pecho | Empuje | Barra | true | 180 | | 
EJE_PRESS_INCL | Press inclinado | Pecho | Empuje | Mancuernas | true | 180 | |
EJE_JALON | Jalón al pecho | Espalda | Tirón | Polea | true | 120 | |
EJE_REMO_SENT | Remo sentado | Espalda | Tirón | Polea | true | 120 | |
EJE_PRESS_HOMBRO | Press hombro | Hombros | Empuje | Mancuernas | true | 120 | |
EJE_ELEV_LAT | Elevaciones laterales | Hombros | Aislamiento | Mancuernas | false | 60 | |
EJE_CURL_BICEP | Curl bíceps | Bíceps | Tirón | Mancuernas | false | 60 | |
EJE_TRICEP_POLEA | Extensión tríceps | Tríceps | Empuje | Polea | false | 60 | |
EJE_SENTADILLA | Sentadilla | Cuádriceps | Compuesto | Barra | true | 180 | |
EJE_PESO_MUERTO | Peso muerto rumano | Isquios | Compuesto | Barra | true | 180 | |
```

---

## PARTE 2: APPS SCRIPT (API)

### Paso 2.1: Crear Proyecto

1. En tu Spreadsheet `FitBase_DB`, ve a **Extensiones** → **Apps Script**
2. Se abre el editor de Apps Script
3. Renombrar proyecto: `FitBase_API`

### Paso 2.2: Pegar Código

Borrar el contenido de `Código.gs` y pegar:

```javascript
// ==================== CONFIGURACIÓN ====================
const SPREADSHEET_ID = 'PEGA_TU_ID_AQUI'; // ← CAMBIAR ESTO

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

// ==================== ENDPOINTS ====================
function doGet(e) {
  try {
    const accion = e.parameter.accion;
    const userId = e.parameter.userId || 'USR_001';
    
    let respuesta;
    
    switch(accion) {
      case 'ping':
        respuesta = { status: 'ok', timestamp: new Date().toISOString() };
        break;
      case 'getUsuario':
        respuesta = getUsuario(userId);
        break;
      case 'getSesionHoy':
        respuesta = getSesionHoy(userId);
        break;
      case 'getEjerciciosSesion':
        respuesta = getEjerciciosSesion(e.parameter.sesionId);
        break;
      case 'getMetricasHoy':
        respuesta = getMetricasHoy(userId);
        break;
      case 'getPlanAnual':
        respuesta = getPlanAnual(userId, e.parameter.anio);
        break;
      case 'getPlanSemanal':
        respuesta = getPlanSemanal(userId, e.parameter.anio, e.parameter.semana);
        break;
      case 'getEjerciciosCatalogo':
        respuesta = getEjerciciosCatalogo();
        break;
      default:
        respuesta = { error: 'Acción GET no válida: ' + accion };
    }
    
    return jsonResponse(respuesta);
    
  } catch (error) {
    return jsonResponse({ error: error.toString() });
  }
}

function doPost(e) {
  try {
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
      case 'logHidratacion':
        respuesta = logHidratacion(datos);
        break;
      case 'completarSesion':
        respuesta = completarSesion(datos);
        break;
      case 'crearSesion':
        respuesta = crearSesion(datos);
        break;
      default:
        respuesta = { error: 'Acción POST no válida: ' + accion };
    }
    
    return jsonResponse(respuesta);
    
  } catch (error) {
    return jsonResponse({ error: error.toString() });
  }
}

function jsonResponse(data) {
  return ContentService.createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}

// ==================== FUNCIONES GET ====================
function getUsuario(userId) {
  const hoja = getHoja(HOJAS.usuarios);
  const datos = hoja.getDataRange().getValues();
  
  for (let i = 1; i < datos.length; i++) {
    if (datos[i][0] === userId) {
      return {
        userId: datos[i][0],
        nombre: datos[i][1],
        nacimiento: datos[i][2],
        alturaCm: datos[i][3],
        sexo: datos[i][4],
        objetivo: datos[i][5],
        diasEntreno: datos[i][6],
        split: datos[i][7],
        ramadan: datos[i][8],
        halal: datos[i][9]
      };
    }
  }
  return { error: 'Usuario no encontrado' };
}

function getSesionHoy(userId) {
  const hoja = getHoja(HOJAS.sesionesPlan);
  const datos = hoja.getDataRange().getValues();
  const hoy = Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd');
  
  for (let i = 1; i < datos.length; i++) {
    const fechaFila = Utilities.formatDate(new Date(datos[i][2]), 'Europe/Madrid', 'yyyy-MM-dd');
    if (datos[i][1] === userId && fechaFila === hoy) {
      return {
        sesionId: datos[i][0],
        fecha: fechaFila,
        tipo: datos[i][3],
        semanaMeso: datos[i][4],
        fase: datos[i][5],
        ajusteVolumen: datos[i][6],
        razonAjuste: datos[i][7],
        duracionEst: datos[i][8],
        completada: datos[i][9]
      };
    }
  }
  return { mensaje: 'No hay sesión programada para hoy', tipo: 'descanso' };
}

function getEjerciciosSesion(sesionId) {
  const hoja = getHoja(HOJAS.ejerciciosPlan);
  const catalogo = getHoja(HOJAS.ejerciciosCatalogo);
  const datosPlan = hoja.getDataRange().getValues();
  const datosCatalogo = catalogo.getDataRange().getValues();
  
  // Crear mapa de catálogo
  const mapaCatalogo = {};
  for (let i = 1; i < datosCatalogo.length; i++) {
    mapaCatalogo[datosCatalogo[i][0]] = {
      nombre: datosCatalogo[i][1],
      grupo: datosCatalogo[i][2],
      patron: datosCatalogo[i][3],
      equipo: datosCatalogo[i][4],
      compuesto: datosCatalogo[i][5]
    };
  }
  
  const ejercicios = [];
  for (let i = 1; i < datosPlan.length; i++) {
    if (datosPlan[i][1] === sesionId) {
      const ejercicioId = datosPlan[i][2];
      const info = mapaCatalogo[ejercicioId] || {};
      ejercicios.push({
        planId: datosPlan[i][0],
        ejercicioId: ejercicioId,
        nombre: info.nombre || ejercicioId,
        grupo: info.grupo,
        orden: datosPlan[i][3],
        seriesPlan: datosPlan[i][4],
        repsPlan: datosPlan[i][5],
        pesoSugerido: datosPlan[i][6],
        descansoSeg: datosPlan[i][7]
      });
    }
  }
  
  ejercicios.sort((a, b) => a.orden - b.orden);
  return { ejercicios: ejercicios };
}

function getMetricasHoy(userId) {
  const hoja = getHoja(HOJAS.metricasZepp);
  const datos = hoja.getDataRange().getValues();
  const hoy = Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd');
  
  for (let i = datos.length - 1; i >= 1; i--) {
    const fechaFila = Utilities.formatDate(new Date(datos[i][2]), 'Europe/Madrid', 'yyyy-MM-dd');
    if (datos[i][1] === userId && fechaFila === hoy) {
      return {
        sleepScore: datos[i][3],
        sleepHoras: datos[i][4],
        sleepDeep: datos[i][5],
        sleepRem: datos[i][6],
        hrvRmssd: datos[i][7],
        hrReposo: datos[i][8],
        readiness: datos[i][9],
        stressAvg: datos[i][10],
        pasosAyer: datos[i][11],
        caloriasActivas: datos[i][12]
      };
    }
  }
  return { mensaje: 'No hay métricas de hoy' };
}

function getPlanAnual(userId, anio) {
  const hoja = getHoja(HOJAS.planAnual);
  const datos = hoja.getDataRange().getValues();
  anio = parseInt(anio) || new Date().getFullYear();
  
  const semanas = [];
  for (let i = 1; i < datos.length; i++) {
    if (datos[i][1] === userId && datos[i][2] === anio) {
      semanas.push({
        semana: datos[i][3],
        fase: datos[i][4],
        objetivo: datos[i][5],
        volumenPct: datos[i][6],
        notas: datos[i][7]
      });
    }
  }
  
  return { anio: anio, semanas: semanas };
}

function getPlanSemanal(userId, anio, semana) {
  const hoja = getHoja(HOJAS.planSemanal);
  const datos = hoja.getDataRange().getValues();
  anio = parseInt(anio) || new Date().getFullYear();
  semana = parseInt(semana);
  
  const dias = [];
  for (let i = 1; i < datos.length; i++) {
    if (datos[i][1] === userId && datos[i][2] === anio && datos[i][3] === semana) {
      dias.push({
        dia: datos[i][4],
        tipoSesion: datos[i][5],
        ejercicios: datos[i][6],
        volumenTotal: datos[i][7],
        notas: datos[i][8]
      });
    }
  }
  
  return { anio: anio, semana: semana, dias: dias };
}

function getEjerciciosCatalogo() {
  const hoja = getHoja(HOJAS.ejerciciosCatalogo);
  const datos = hoja.getDataRange().getValues();
  
  const ejercicios = [];
  for (let i = 1; i < datos.length; i++) {
    ejercicios.push({
      ejercicioId: datos[i][0],
      nombre: datos[i][1],
      grupo: datos[i][2],
      patron: datos[i][3],
      equipo: datos[i][4],
      compuesto: datos[i][5],
      descansoDefault: datos[i][6]
    });
  }
  
  return { ejercicios: ejercicios };
}

// ==================== FUNCIONES POST ====================
function logEjercicio(datos) {
  const hoja = getHoja(HOJAS.ejerciciosLog);
  const logId = 'LOG_' + Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyyMMddHHmmss');
  
  hoja.appendRow([
    logId,
    datos.planId,
    datos.userId || 'USR_001',
    datos.serie,
    datos.repsReal,
    datos.pesoReal,
    datos.rir || null,
    datos.tempo || null,
    datos.notas || null,
    new Date()
  ]);
  
  return { success: true, logId: logId };
}

function logComida(datos) {
  const hoja = getHoja(HOJAS.comidasLog);
  const comidaId = 'COM_' + Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyyMMddHHmmss');
  
  hoja.appendRow([
    comidaId,
    datos.userId || 'USR_001',
    datos.fecha || Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd'),
    datos.hora || Utilities.formatDate(new Date(), 'Europe/Madrid', 'HH:mm'),
    datos.tipo || 'comida',
    datos.descripcion || '',
    datos.kcal || 0,
    datos.proteinaG || 0,
    datos.carbosG || 0,
    datos.grasaG || 0,
    new Date()
  ]);
  
  return { success: true, comidaId: comidaId };
}

function logPeso(datos) {
  const hoja = getHoja(HOJAS.pesoLog);
  const pesoId = 'PES_' + Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyyMMddHHmmss');
  
  hoja.appendRow([
    pesoId,
    datos.userId || 'USR_001',
    datos.fecha || Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd'),
    datos.pesoKg,
    datos.grasaPct || null,
    datos.musculoKg || null,
    datos.fuente || 'manual',
    datos.condiciones || 'ayunas',
    new Date()
  ]);
  
  return { success: true, pesoId: pesoId };
}

function logHidratacion(datos) {
  const hoja = getHoja(HOJAS.hidratacionLog);
  const hidraId = 'HID_' + Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyyMMddHHmmss');
  
  hoja.appendRow([
    hidraId,
    datos.userId || 'USR_001',
    datos.fecha || Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd'),
    datos.litros,
    new Date()
  ]);
  
  return { success: true, hidraId: hidraId };
}

function completarSesion(datos) {
  const hoja = getHoja(HOJAS.sesionesPlan);
  const datosHoja = hoja.getDataRange().getValues();
  
  for (let i = 1; i < datosHoja.length; i++) {
    if (datosHoja[i][0] === datos.sesionId) {
      hoja.getRange(i + 1, 10).setValue(true);  // bool_completada
      hoja.getRange(i + 1, 12).setValue(new Date());  // date_fin
      return { success: true, mensaje: 'Sesión completada' };
    }
  }
  
  return { error: 'Sesión no encontrada' };
}

function crearSesion(datos) {
  const hoja = getHoja(HOJAS.sesionesPlan);
  const sesionId = 'SES_' + Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyyMMddHHmmss');
  
  hoja.appendRow([
    sesionId,
    datos.userId || 'USR_001',
    datos.fecha || Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd'),
    datos.tipo,
    datos.semanaMeso || 1,
    datos.fase || 'acumulacion',
    datos.ajusteVolumen || 1.0,
    datos.razonAjuste || '',
    datos.duracionEst || 60,
    false,  // bool_completada
    null,   // date_inicio
    null,   // date_fin
    new Date()
  ]);
  
  return { success: true, sesionId: sesionId };
}

// ==================== HELPERS ====================
function getHoja(nombre) {
  return SpreadsheetApp.openById(SPREADSHEET_ID).getSheetByName(nombre);
}
```

### Paso 2.3: Desplegar como Web App

1. Clic en **Implementar** → **Nueva implementación**
2. Tipo: **Aplicación web**
3. Configurar:
   - Descripción: `FitBase API v1.0`
   - Ejecutar como: **Yo mismo**
   - Quién tiene acceso: **Cualquiera** (importante para que la app acceda)
4. Clic **Implementar**
5. **Autorizar** la app cuando pregunte
6. **COPIAR LA URL** que te da. Tiene este formato:
   ```
   https://script.google.com/macros/s/AKfycb.../exec
   ```
   > Esta URL va en tu app Android (Constantes.java)

### Paso 2.4: Probar API

Abre en el navegador:
```
TU_URL?accion=ping
```

Deberías ver:
```json
{"status":"ok","timestamp":"2026-06-18T10:00:00.000Z"}
```

Prueba obtener usuario:
```
TU_URL?accion=getUsuario&userId=USR_001
```

---

## PARTE 3: ANDROID STUDIO

### Paso 3.1: Instalar Android Studio

1. Descargar de [developer.android.com/studio](https://developer.android.com/studio)
2. Instalar con opciones por defecto
3. En el primer inicio, instalar SDK 34 cuando pregunte

### Paso 3.2: Crear Proyecto

1. **New Project** → **Empty Views Activity**
2. Configurar:
   - Name: `FitBase`
   - Package name: `com.fitbase`
   - Language: **Java** (NO Kotlin)
   - Minimum SDK: **API 26**
3. Finish

### Paso 3.3: Configurar build.gradle

Reemplazar `app/build.gradle` con el contenido de [PROMPT_DESARROLLO.md → §8](PROMPT_DESARROLLO.md)

### Paso 3.4: Sync y Build

1. Clic **Sync Now** cuando Android Studio lo pida
2. Esperar a que baje todas las dependencias
3. **Build** → **Make Project** (Ctrl+F9)
4. Verificar que compila sin errores

### Paso 3.5: Pegar Código Generado

El código se generará por la IA siguiendo [PROMPT_DESARROLLO.md](PROMPT_DESARROLLO.md)

Estructura a crear:
```
app/src/main/java/com/fitbase/
├── FitBaseApp.java
├── data/
│   ├── api/
│   ├── model/
│   ├── local/
│   └── repository/
├── ui/
│   ├── splash/
│   ├── home/
│   ├── workout/
│   ├── nutricion/
│   ├── plan/
│   └── common/
├── service/
└── util/
```

---

## PARTE 4: INSTALAR EN MÓVIL

### Paso 4.1: Habilitar Depuración USB

En tu Xiaomi Redmi Note 14 Pro 5G:

1. **Ajustes** → **Sobre el teléfono**
2. Toca **Versión de MIUI** 7 veces (activa Developer Options)
3. Vuelve a **Ajustes** → **Ajustes adicionales** → **Opciones de desarrollador**
4. Activar: **Depuración USB**
5. Activar: **Instalar vía USB**

### Paso 4.2: Conectar y Probar

1. Conecta el móvil por USB
2. En Android Studio, selecciona tu dispositivo en el dropdown
3. Clic **Run** (▶) o Shift+F10
4. Acepta permisos en el móvil cuando pregunte
5. La app se instala y abre automáticamente

### Paso 4.3: Verificar Conexión

1. Abre la app
2. Debería cargar datos de la API
3. Si falla, revisar:
   - ¿El móvil tiene internet?
   - ¿La URL de Apps Script es correcta?
   - ¿El Spreadsheet tiene datos?

---

## PARTE 5: USO DIARIO

### Mañana (10 seg)
1. Abrir app
2. Ver macros del día
3. Ver si hay entreno programado

### Gym (60-90 min)
1. Tap "Empezar entreno"
2. Seguir ejercicio a ejercicio
3. Registrar series (swipe izquierda)
4. Timer de descanso automático
5. Al final: resumen + sync

### Comidas (durante el día)
1. Abrir app → Nutrición
2. Añadir comida
3. Introducir macros aproximados

### Planificación (cuando quieras)
1. Ver Plan Anual → fases del año
2. Ver Plan Semanal → exportar para el corcho

---

## 🆘 SOLUCIÓN DE PROBLEMAS

| Problema | Solución |
|----------|----------|
| API no responde | Verificar que Apps Script está desplegado como Web App |
| Error "No autorizado" | Re-desplegar Apps Script y autorizar permisos |
| App no conecta | Verificar URL en Constantes.java |
| No hay sesión hoy | Crear sesión manualmente en la hoja sesiones_plan |
| Build falla | Sync Gradle, invalidar caches |

---

## 📅 TIMELINE RECOMENDADO

| Semana | Tarea |
|--------|-------|
| **Semana -2** (18-24 ago) | Crear Sheets + Apps Script |
| **Semana -1** (25-31 ago) | Crear proyecto Android + pegar código |
| **1 Sep** | ¡Lanzamiento! Primer uso real |
| **Semana 1** | Ajustes y bugs |
| **Semana 2+** | Uso normal |

---

**¡Listo! Para el 1 de Septiembre de 2026 tendrás FitBase funcionando.** 💪
