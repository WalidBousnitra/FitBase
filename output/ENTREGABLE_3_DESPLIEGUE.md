# ENTREGABLE 3: GUÍA DE DESPLIEGUE

> Paso a paso para poner FitBase en funcionamiento.

---

## PASO 1: Crear Google Sheet

1. Ve a [sheets.google.com](https://sheets.google.com) y crea una hoja nueva
2. Nómbrala: **FitBase-BD**
3. Copia el **ID del spreadsheet** de la URL:
   ```
   https://docs.google.com/spreadsheets/d/ESTE_ES_EL_ID/edit
   ```
4. Guárdalo — lo necesitarás en el Paso 2

---

## PASO 2: Crear Apps Script y pegar código

1. Desde la hoja de cálculo: **Extensiones → Apps Script**
2. Se abrirá el editor de Apps Script
3. Borra el contenido por defecto de `Código.gs`
4. Copia y pega TODO el contenido de `ENTREGABLE_2_CODIGO/backend/Codigo.gs`
5. **IMPORTANTE**: Reemplaza `TU_SPREADSHEET_ID_AQUI` por el ID del paso 1:
   ```javascript
   const SPREADSHEET_ID = 'tu_id_real_aqui';
   ```
6. Guarda el proyecto (Ctrl+S)
7. **Ejecuta la función `inicializarHojas()`**:
   - En el desplegable de funciones, selecciona `inicializarHojas`
   - Haz clic en ▶ (Ejecutar)
   - Autoriza los permisos cuando se soliciten
   - Verifica que se crearon las 14 hojas en tu spreadsheet

---

## PASO 3: Desplegar como Web App

1. En Apps Script: **Implementar → Nueva implementación**
2. Configurar:
   - **Tipo**: Aplicación web
   - **Ejecutar como**: Yo mismo
   - **Quién tiene acceso**: Cualquier persona
3. Haz clic en **Implementar**
4. **Copia la URL** que aparece (formato: `https://script.google.com/macros/s/XXXXXX/exec`)
5. Guárdala — es la URL de tu API

### Verificar que funciona:
Abre en el navegador:
```
https://script.google.com/macros/s/TU_DEPLOY_ID/exec?accion=macros_hoy
```
Deberías ver un JSON con las calorías y macros.

---

## PASO 4: Configurar proyecto Android

1. Abre Android Studio
2. **File → New → Import Project**
3. Selecciona la carpeta `ENTREGABLE_2_CODIGO/android/`
4. Espera a que Gradle sincronice las dependencias
5. Si hay errores de SDK:
   - **File → Project Structure → SDK Location**
   - Configura el Android SDK (necesitas SDK 34)

---

## PASO 5: Configurar URL del backend

1. Abre `app/build.gradle`
2. Busca la línea:
   ```groovy
   buildConfigField "String", "BASE_URL", "\"https://script.google.com/macros/s/TU_DEPLOY_ID/exec\""
   ```
3. Reemplaza `TU_DEPLOY_ID` con el ID de tu implementación del Paso 3
4. Sincroniza Gradle (File → Sync Project with Gradle Files)

---

## PASO 6: Compilar APK e instalar

### Opción A: Desde Android Studio (desarrollo)
1. Conecta tu Xiaomi Redmi Note 14 Pro 5G por USB
2. Activa **Depuración USB** en el móvil:
   - Ajustes → Sobre el teléfono → Toca "Versión MIUI" 7 veces
   - Ajustes → Ajustes adicionales → Opciones de desarrollador → Depuración USB
3. En Android Studio: **Run → Run 'app'** (Shift+F10)
4. Selecciona tu dispositivo y espera a que instale

### Opción B: Generar APK (producción)
1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`
3. Pásalo al móvil (USB, Drive, etc.) e instálalo
4. Si Android bloquea: **Ajustes → Aplicaciones → Instalar apps desconocidas**

---

## PASO 7: Permisos Health Connect

1. Instala **Health Connect** desde Play Store (si no está ya)
2. Instala **Zepp** (app de Amazfit GTS 4) y sincroniza el reloj
3. En Health Connect:
   - Abre Health Connect → Permisos de apps
   - Busca **FitBase**
   - Activa permisos: Sueño, Frecuencia cardíaca, Pasos
4. En Zepp:
   - Perfil → Health Connect → Conectar
   - Habilita sincronización de datos

---

## VERIFICACIÓN FINAL

| Paso | Verificar | Resultado esperado |
|------|-----------|-------------------|
| 1 | Abrir spreadsheet | 14 hojas con cabeceras |
| 2 | Ejecutar `inicializarHojas` | "Hojas inicializadas" |
| 3 | URL en navegador | JSON con macros |
| 4 | Proyecto en Android Studio | Sin errores de build |
| 5 | Build Config | URL correcta en BuildConfig |
| 6 | App en móvil | Se abre pantalla mañana |
| 7 | Health Connect | Permisos concedidos |

---

## TROUBLESHOOTING

### "Error al cargar datos"
- Verifica la URL del backend en `build.gradle`
- Prueba la URL directamente en el navegador
- Revisa que el Apps Script esté desplegado como "Cualquier persona"

### "No hay sesión planificada"
- Si la fecha actual es anterior al 31/08/2026, la app mostrará MODO DEMO
- Verifica que haya datos en la hoja `sesiones_plan`

### "Health Connect no sincroniza"
- Zepp debe estar ejecutándose en segundo plano
- Health Connect necesita permisos explícitos para cada tipo de dato
- Sincronización no es instantánea (puede tardar 15-30 min)

### "Gradle sync failed"
- Verifica que tienes Java 17 instalado
- Verifica que tienes Android SDK 34
- **File → Invalidate Caches → Restart**

---

## DIAGRAMA DE ARQUITECTURA DESPLEGADA

```
┌─────────────────────────────┐
│     MÓVIL (Xiaomi)          │
│                             │
│  ┌───────────────────┐     │
│  │    FitBase APK    │     │
│  │  (Java + Views)   │     │
│  └────────┬──────────┘     │
│           │ HTTP            │
│  ┌────────┴──────────┐     │
│  │  Health Connect   │     │
│  │  (Zepp ← GTS 4)  │     │
│  └───────────────────┘     │
└───────────┬─────────────────┘
            │ REST API
            ▼
┌─────────────────────────────┐
│     GOOGLE CLOUD            │
│                             │
│  Apps Script (exec URL)     │
│       ↓                     │
│  Google Sheets (14 hojas)   │
│                             │
└─────────────────────────────┘
```
