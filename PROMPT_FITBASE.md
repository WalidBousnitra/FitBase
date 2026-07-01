# FITBASE - PROMPT DE GENERACIÓN

> **USO**: Copia este archivo + toda la carpeta `/knowledge_base/` a una IA.

---

## 1. QUÉ DEBE HACER LA IA

### PASO 1: LEER TODO
```
OBLIGATORIO leer en este orden:

1. manifest.md                    → Entender estructura
2. usuario/prioridades.md         → Qué es importante para mí
3. usuario/biometria.md           → Mis datos físicos actuales
4. usuario/preferencias_ejercicios.md → Ejercicios que me gustan/evito
5. usuario/perfil/cultura.md      → Restricciones (halal, Ramadán)
6. usuario/perfil/horarios.md     → Mi disponibilidad

7. evidencia/periodizacion.md     → Cómo estructurar fases (Bompa)
8. evidencia/hipertrofia.md       → Volumen, frecuencia, RIR (Schoenfeld)
9. evidencia/nutricion.md         → Calorías y macros (Mifflin, Helms)
10. evidencia/postura.md          → Ejercicios correctivos (es mi P2)

11. reglas/logica/base_datos.md   → Esquema de Google Sheets
12. reglas/logica/motor_pesos.md  → Lógica de progresión
13. reglas/nutricion/motor_dieta.md → Lógica de calorías
14. reglas/desarrollo/ui.md       → Especificación de pantallas
15. reglas/desarrollo/Sistema_Diseno_Fitness.md → Colores y diseño
```

---

### PASO 2: GENERAR (basándose SOLO en lo leído)

#### A) PLAN DE ENTRENAMIENTO (11 meses)
Usando datos de `/evidencia/`:
- Estructura de fases según Bompa (`periodizacion.md`)
- Volumen semanal según Schoenfeld (`hipertrofia.md`)
- Sistema RIR según Helms (`hipertrofia.md`)
- Ejercicios según mis preferencias (`usuario/preferencias_ejercicios.md`)
- Prioridad muscular: Hombros > Bíceps > Espalda (`prioridades.md`)

#### B) PLAN DE NUTRICIÓN
Usando datos de `/evidencia/nutricion.md`:
- TMB con fórmula Mifflin-St Jeor
- TDEE con factor de actividad
- Macros según Helms/Iraki (proteína 1.6-2.2g/kg en bulk)
- Ajustes por fase (déficit/superávit)

#### C) VISTA DEL AÑO COMPLETO
Una pantalla en la app que muestre:
- Calendario de 11 meses
- Fases generadas (coloreadas)
- Checkpoints de progreso
- Resumen de cada fase

#### D) APP ANDROID COMPLETA
Especificaciones en `/reglas/desarrollo/`:
- **Stack**: Java 17 + Views/XML (NO Kotlin, NO Compose)
- **Arquitectura**: MVVM + Repository + Room + Retrofit
- **Diseño**: `Sistema_Diseno_Fitness.md` (colores, fuentes)
- **Pantallas**: `ui.md` (todos los flujos)
- **Base de datos**: `base_datos.md` (esquema simplificado en Sheets)

---

### PASO 3: ENTREGAR

La IA debe darme **4 entregables**:

```
┌─────────────────────────────────────────────────────────────────┐
│ ENTREGABLE 1: PLAN GENERADO                                     │
├─────────────────────────────────────────────────────────────────┤
│ - Plan de 11 meses con fases (basado en evidencia)              │
│ - Ejercicios seleccionados (basado en preferencias)             │
│ - Calorías y macros calculados (basado en mis datos)            │
│ - Vista tipo calendario para ver el año entero                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ ENTREGABLE 2: CÓDIGO COMPLETO                                   │
├─────────────────────────────────────────────────────────────────┤
│ Backend (Google Apps Script):                                   │
│   - Codigo.gs con API REST completa                             │
│   - Instrucciones para crear las hojas activas del esquema      │
│                                                                 │
│ Android (Java + Views/XML):                                     │
│   - Todos los archivos .java                                    │
│   - Todos los layouts .xml                                      │
│   - Resources (colors.xml, strings.xml, themes.xml)             │
│   - AndroidManifest.xml                                         │
│   - build.gradle con dependencias                               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ ENTREGABLE 3: GUÍA DE DESPLIEGUE                                │
├─────────────────────────────────────────────────────────────────┤
│ Paso a paso:                                                    │
│   1. Crear Google Sheet y copiar estructura                     │
│   2. Crear Apps Script y pegar código                           │
│   3. Desplegar como Web App y copiar URL                        │
│   4. Abrir proyecto en Android Studio                           │
│   5. Configurar URL del backend                                 │
│   6. Compilar APK e instalar en móvil                          │
│   7. Dar permisos de Health Connect                             │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ ENTREGABLE 4: MANUAL DE USO                                     │
├─────────────────────────────────────────────────────────────────┤
│ - Qué hacer el primer día                                       │
│ - Cómo ver mi plan anual                                        │
│ - Cómo usar la app en el gym                                    │
│ - Cómo registrar series (peso, reps, RIR)                       │
│ - Cómo ver mi progreso                                          │
│ - Qué hacer si fallo un día                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. REGLAS ABSOLUTAS

```yaml
PROHIBIDO:
  - Inventar datos que no estén en /knowledge_base/
  - Usar Kotlin o Jetpack Compose
  - Hardcodear ejercicios específicos (se generan según preferencias)
  - Hardcodear fases con fechas (se calculan según fecha inicio)
  - Ignorar mis prioridades (P1 Estética, P2 Postura, P3 Hipertrofia)
  - Ignorar mis lesiones (codo - evitar extensión bajo carga)

OBLIGATORIO:
  - Toda lógica basada en /evidencia/ (citar paper)
  - Respetar preferencias de /usuario/
  - Código y comentarios en español
  - Duración del plan: 11 meses exactos
  - Diseño según Sistema_Diseno_Fitness.md (tema oscuro)
```

---

## 3. DATOS CLAVE (resumen para la IA)

```yaml
USUARIO:
  edad: 24 años
  altura: 188 cm
  peso: 78.2 kg
  grasa: 18.9%
  experiencia: "3 años casual"
  lesion: "Codo - evitar extensión bajo carga"
  
PRIORIDADES:
  P1: Estética (V-taper)
  P2: Postura (wall angels - no puede hacerlos)
  P3: Hipertrofia (Hombros > Bíceps > Espalda)
  
RESTRICCIONES:
  - Dieta halal
  - Ramadán (ajustar cuando corresponda)
  
EQUIPAMIENTO:
  - Gym completo (ver usuario/equipamiento.md)
  - Natación 2x/semana
  
DURACIÓN: 11 meses desde FECHA_INICIO (definida en biometria.md)
```

---

## 4. MODO DEMO

```yaml
CONDICIÓN: fecha_actual < FECHA_INICIO (de usuario/biometria.md)

COMPORTAMIENTO:
  - Banner visible: "🎮 MODO DEMO - Comienza el [FECHA_INICIO]"
  - Todas las pantallas navegables
  - Datos de ejemplo (mock)
  - NO requiere backend real
  - Health Connect opcional (usar datos ficticios si no hay)
```

---

## 5. CHECKLIST FINAL

Antes de entregar, la IA debe verificar:

- [ ] ¿El plan está basado en evidencia de /evidencia/?
- [ ] ¿Los ejercicios respetan mis preferencias y evitan los excluidos?
- [ ] ¿Se respeta mi lesión de codo?
- [ ] ¿Las fórmulas nutricionales citan papers?
- [ ] ¿El código es Java 17 + Views/XML (no Kotlin/Compose)?
- [ ] ¿El diseño sigue Sistema_Diseno_Fitness.md?
- [ ] ¿Hay una pantalla para ver el plan anual completo?
- [ ] ¿Las instrucciones de despliegue son paso a paso?
- [ ] ¿El manual explica cómo usar la app?
