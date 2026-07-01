---
id: "REG-DEV-01"
nombre: "Especificación de UI"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["SYS-00", "REG-DEV-02", "REG-LOG-02"]
tags: ["reglas", "desarrollo", "ui", "android", "flujos"]
---

# Especificación de UI

## 1. Principios de Diseño

### Filosofía: MINIMALISMO ABSOLUTO
- **Cero relleno** — Solo información esencial
- **Pantalla completa** — Sin elementos decorativos
- **Visual** — Números grandes, iconos, colores
- **Directo** — Una acción por pantalla
- **Gestos** — Swipe para avanzar, tap para confirmar

### Dispositivo Referencia
- Xiaomi Redmi Note 14 Pro 5G
- Pantalla: 6.67" AMOLED, 2712 x 1220
- Modo oscuro obligatorio

---

## 2. Momentos de Uso

| Momento | Cuándo | Duración | Acción |
|---------|--------|----------|--------|
| **Mañana** | Al despertar | 10 seg | Ver objetivos del día |
| **Gym** | Al llegar | 60-90 min | Guía ejercicio a ejercicio |
| **Post-entreno** | Al acabar | 10 seg | Ver resumen + tips |
| **Planificación** | Cuando quiera | Variable | Ver plan anual/semanal |

> **NO se usa** el resto del día (excepto para consultar planificación)

---

## 3. FLUJO MAÑANA

### 3.1 Animación de Entrada
```
┌─────────────────────────────────────────┐
│                                         │
│                                         │
│                                         │
│                                         │
│              [LOGO]                     │
│             FitBase                     │
│                                         │
│                                         │
│           ────────────                  │  ← Barra de carga
│                                         │
│                                         │
└─────────────────────────────────────────┘
        (0.5s) → fade a pantalla principal
```

### 3.2 Pantalla Principal Mañana (FULLSCREEN)

```
┌─────────────────────────────────────────┐
│                                         │
│           miércoles 18 jun             │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │                                 │   │
│  │         3280                    │   │  ← Número gigante
│  │          kcal                   │   │
│  │                                 │   │
│  │    P 156g  C 488g  G 78g       │   │
│  │                                 │   │
│  └─────────────────────────────────┘   │
│                                         │
│  ┌───────────┐  ┌───────────┐          │
│  │  👟 8000  │  │  💧 3.0L  │          │  ← Cards secundarias
│  │   pasos   │  │   agua    │          │
│  └───────────┘  └───────────┘          │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │  🍽️ 3 comidas + 2 snacks       │   │  ← Info timing
│  │  Pre-entreno: 2h antes          │   │
│  │  Post-entreno: <30min           │   │
│  └─────────────────────────────────┘   │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │  🚶 Pasos: distribuir todo el   │   │
│  │  día, evitar >2h sentado        │   │
│  └─────────────────────────────────┘   │
│                                         │
│                                         │
│  Hoy: Push Day                    ▶    │  ← Si hay entreno
│                                         │
└─────────────────────────────────────────┘
```

### 3.3 Si NO hay entreno hoy

```
┌─────────────────────────────────────────┐
│                                         │
│           miércoles 18 jun             │
│            día de descanso              │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │         2855                    │   │  ← Mantenimiento
│  │          kcal                   │   │
│  │    (mantenimiento)              │   │
│  └─────────────────────────────────┘   │
│                                         │
│  ... (resto igual)                      │
│                                         │
│  Próximo entreno: mañana (Pull)        │
│                                         │
└─────────────────────────────────────────┘
```

---

## 4. FLUJO GYM

### 4.1 Tap en "Empezar Entreno"

```
┌─────────────────────────────────────────┐
│                                         │
│                                         │
│                                         │
│                                         │
│              PUSH DAY                   │
│                                         │
│              6 ejercicios               │
│              ~75 min                    │
│                                         │
│                                         │
│                                         │
│         [ ▶ EMPEZAR ]                   │
│                                         │
│                                         │
│                                         │
└─────────────────────────────────────────┘
```

### 4.2 Ejercicio Activo (PANTALLA PRINCIPAL)

**Caso A: Ejercicio con REPS**
```
┌─────────────────────────────────────────┐
│  1/6                         ⏱ 12:34   │
├─────────────────────────────────────────┤
│                                         │
│                                         │
│         PRESS INCLINADO                 │
│                                         │
│                                         │
│              18 kg                      │  ← GRANDE
│                                         │
│         ┌─────────────┐                 │
│         │     10      │                 │  ← Reps objetivo
│         │    reps     │                 │
│         └─────────────┘                 │
│                                         │
│          Serie 2 / 4                    │
│                                         │
│                                         │
│                                         │
│                                         │
│         ◀ swipe para completar ▶       │
│                                         │
└─────────────────────────────────────────┘
```

**Caso B: Ejercicio con TIEMPO**
```
┌─────────────────────────────────────────┐
│  5/6                         ⏱ 58:21   │
├─────────────────────────────────────────┤
│                                         │
│                                         │
│            PLANCHA                      │
│                                         │
│                                         │
│                                         │
│         ┌─────────────┐                 │
│         │    0:45     │                 │  ← Tiempo objetivo
│         │             │                 │
│         └─────────────┘                 │
│                                         │
│          Serie 2 / 3                    │
│                                         │
│                                         │
│       [ ▶ INICIAR TIEMPO ]              │  ← Tap para empezar cuenta
│                                         │
│                                         │
│         ◀ swipe para completar ▶       │
│                                         │
└─────────────────────────────────────────┘
```

**Caso C: Movilidad/Calentamiento**
```
┌─────────────────────────────────────────┐
│  0/6 (warm-up)               ⏱ 00:00   │
├─────────────────────────────────────────┤
│                                         │
│                                         │
│        FOAM ROLL TORÁCICO               │
│                                         │
│                                         │
│                                         │
│         ┌─────────────┐                 │
│         │    2:00     │                 │
│         │             │                 │
│         └─────────────┘                 │
│                                         │
│                                         │
│                                         │
│       [ ▶ INICIAR ]                     │
│                                         │
│                                         │
│         ◀ swipe para saltar ▶          │
│                                         │
└─────────────────────────────────────────┘
```

### 4.3 Después de Swipe → Registro RPE

```
┌─────────────────────────────────────────┐
│                                         │
│                                         │
│         ¿Cuántas hiciste?               │
│                                         │
│         ┌─────────────┐                 │
│         │     10      │                 │  ← Editable, empieza con objetivo
│         └─────────────┘                 │
│              reps                       │
│                                         │
│                                         │
│         ¿Cuántas te quedaban?           │
│                                         │
│    ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐  │
│    │ 0  │ │ 1  │ │ 2  │ │ 3  │ │ 4+ │  │  ← Botones GRANDES
│    └────┘ └────┘ └────┘ └────┘ └────┘  │
│                                         │
│                                         │
│              [ ✓ ]                      │  ← Confirmar
│                                         │
│                                         │
└─────────────────────────────────────────┘
```

### 4.4 Timer de Descanso

```
┌─────────────────────────────────────────┐
│                                         │
│                                         │
│                                         │
│                                         │
│                                         │
│              2:00                       │  ← GIGANTE, toda la pantalla
│                                         │
│                                         │
│                                         │
│                                         │
│          Próximo: Serie 3/4             │
│             18 kg x 10                  │
│                                         │
│                                         │
│         ◀ swipe para saltar ▶          │
│                                         │
└─────────────────────────────────────────┘
```

**IMPORTANTE: Notificación persistente**
```
┌─────────────────────────────────────────┐
│ 🏋️ FitBase        Descanso: 1:45       │  ← En barra de notificación
└─────────────────────────────────────────┘
```
- Usuario puede ir a otras apps (WhatsApp, Spotify...)
- Timer SIEMPRE visible en notificación
- Al llegar a 0:00 → Vibración + Sonido + Notificación expandida

### 4.5 Fin de Timer → Auto-avanza

```
┌─────────────────────────────────────────┐
│ 🏋️ FitBase        ¡VAMOS! Serie 3      │  ← Notificación al acabar
└─────────────────────────────────────────┘
```

Al tocar notificación o abrir app → Vuelve a pantalla de ejercicio

### 4.6 Último Ejercicio (SIN TIMER)

Después del último ejercicio → Directo a resumen (no hay descanso)

---

## 5. PANTALLA FIN DE SESIÓN

### 5.1 Puntuación + Resumen

```
┌─────────────────────────────────────────┐
│                                         │
│              PUSH DAY                   │
│            completado ✓                 │
│                                         │
│         ┌─────────────┐                 │
│         │             │                 │
│         │     87      │                 │  ← Puntuación (0-100)
│         │    /100     │                 │
│         │             │                 │
│         └─────────────┘                 │
│                                         │
│    ⏱ 1h 12min   📊 8,450 kg            │
│                                         │
│    PRs: 🏆 Press Inclinado             │
│                                         │
├─────────────────────────────────────────┤
│                                         │
│  💡 PARA HOY:                           │
│                                         │
│  • Sistema nervioso cargado             │
│  • Duerme mínimo 8h esta noche          │
│  • Toma batido proteína + carbos        │
│    en los próximos 30 min               │
│  • Evita cardio intenso hoy             │
│                                         │
│                                         │
│         [ ✓ CERRAR ]                    │
│                                         │
└─────────────────────────────────────────┘
```

### 5.2 Tips Personalizados según Sesión

| Tipo de Sesión | Tips |
|----------------|------|
| **Día de PR / Alta intensidad** | "Sistema nervioso fundido, 9h sueño, batido post, evita estrés" |
| **Volumen alto** | "Músculos fatigados, prioriza proteína, estiramientos suaves" |
| **Deload** | "Buen trabajo de recuperación, mantén actividad ligera" |
| **Pierna** | "DOMS esperados mañana, caminar ayuda a recuperar" |
| **Con dolor reportado** | "Vigila [zona], si persiste consulta fisio" |

---

## 6. Interacciones

| Gesto | Acción |
|-------|--------|
| **Swipe izquierda** | Completar serie / Siguiente |
| **Swipe derecha** | Volver / Cancelar |
| **Tap centro** | Confirmar / Acción principal |
| **Tap número** | Editar valor |
| **Long press** | Menú contextual (ajustar peso, saltar ejercicio) |

---

## 7. Notificaciones

### 7.1 Durante Entreno (Foreground Service)
```
Notificación persistente:
┌───────────────────────────────────────┐
│ 🏋️ FitBase                           │
│ Press Inclinado - Serie 2/4          │
│ Descanso: 1:45                        │
│ [Abrir]                    [Pausar]   │
└───────────────────────────────────────┘
```

### 7.2 Timer Terminado
```
Notificación heads-up (expandida):
┌───────────────────────────────────────┐
│ 🏋️ ¡VAMOS!                           │
│ Serie 3 de Press Inclinado           │
│ 18 kg x 10 reps                       │
└───────────────────────────────────────┘
+ Vibración larga (500ms)
+ Sonido corto
```

---

## 8. Componentes UI

| Componente | Descripción |
|------------|-------------|
| `FullScreenNumber` | Número gigante centrado (peso, tiempo, reps) |
| `RirButtons` | 5 botones en fila (0,1,2,3,4+) |
| `SwipeContainer` | Detecta swipe izq/der |
| `CountdownTimer` | Timer con notificación foreground |
| `MacroCard` | Card con P/C/G |
| `TipCard` | Recomendación post-entreno |
| `ProgressRing` | Círculo de progreso para puntuación |

---

## 9. Especificaciones Visuales

### Paleta (Modo Oscuro)
| Elemento | Color |
|----------|-------|
| Background | #121212 |
| Surface | #1E1E1E |
| Primary | #4FC3F7 (azul claro) |
| Success | #81C784 (verde) |
| Warning | #FFB74D (naranja) |
| Error | #E57373 (rojo) |
| Text Primary | #FFFFFF |
| Text Secondary | #B0B0B0 |

### Tipografía
| Uso | Tamaño |
|-----|--------|
| Números hero (peso, timer) | **72sp** |
| Números secundarios | 48sp |
| Headers | 24sp |
| Body | 16sp |
| Caption | 12sp |

### Espaciado
- Padding pantalla: 24dp
- Entre cards: 16dp
- Botones: mínimo 56dp altura

---

## 10. Estados de la App

```
         ┌──────────┐
         │  MAÑANA  │ ← Abre app
         └────┬─────┘
              │ (tap "Empezar" si hay entreno)
              ▼
    ┌─────────────────┐
    │  PREVIEW SESIÓN │
    └────────┬────────┘
             │ (tap "Empezar")
             ▼
    ┌─────────────────┐
    │   EJERCICIO     │ ◄───────────────┐
    └────────┬────────┘                 │
             │ (swipe izq)              │
             ▼                          │
    ┌─────────────────┐                 │
    │   REGISTRO RPE  │                 │
    └────────┬────────┘                 │
             │ (tap confirmar)          │
             ▼                          │
    ┌─────────────────┐                 │
    │     TIMER       │─────────────────┘ (auto cuando timer=0)
    └────────┬────────┘
             │ (si último ejercicio)
             ▼
    ┌─────────────────┐
    │  FIN + TIPS     │
    └────────┬────────┘
             │ (tap cerrar)
             ▼
         [APP CERRADA]
```

---

## 11. Offline

1. Al abrir app mañana → Cachea sesión del día
2. Durante entreno → Todo local
3. Al cerrar sesión → Sync a Sheets (si hay red)
4. Si no hay red → Guarda local, sync cuando vuelva

---

## 12. PANTALLA: PLAN ANUAL (Macrociclo)

> Acceso: Desde Home, botón/icono de calendario

### 12.1 Vista Anual

```
┌───────────────────────────────────────┐
│  ←                PLAN 2026                │
├───────────────────────────────────────┤
│                                           │
│  FASE ACTUAL: Volumen (sem 2/6)           │
│  ███████▓░░░░░░░░░░░░░░░░░  33%            │
│                                           │
├───────────────────────────────────────┤
│                                           │
│  ENE  FEB  MAR  ABR  MAY  JUN              │
│  ████ ████ ████ ████ ████ ▓▓▓▓ ◀ HOY      │
│  VOL  VOL  FZA  FZA  DEF  DEF              │
│                                           │
│  JUL  AGO  SEP  OCT  NOV  DIC              │
│  ░░░░ ░░░░ ░░░░ ░░░░ ░░░░ ░░░░              │
│  MNT  MNT  VOL  VOL  FZA  FZA              │
│                                           │
├───────────────────────────────────────┤
│  LEYENDA:                                 │
│  VOL = Volumen (hipertrofia)              │
│  FZA = Fuerza (intensificación)           │
│  DEF = Definición (déficit)               │
│  MNT = Mantenimiento                      │
│  ░░░ = Deload (cada 4-6 semanas)          │
│                                           │
└───────────────────────────────────────┘
```

### 12.2 Tap en un mes → Detalle del Mesociclo

```
┌───────────────────────────────────────┐
│  ←               JUNIO 2026                │
│                 VOLUMEN                    │
├───────────────────────────────────────┤
│                                           │
│  Semana 1 (3-9 jun)      RIR 3-4          │
│  ████████████████████ ✔ completada        │
│                                           │
│  Semana 2 (10-16 jun)    RIR 2-3          │
│  █████████▓▓▓▓▓▓░░░░░ ◀ ACTUAL (mié)     │
│                                           │
│  Semana 3 (17-23 jun)    RIR 1-2          │
│  ░░░░░░░░░░░░░░░░░░░░ pendiente          │
│                                           │
│  Semana 4 (24-30 jun)    DELOAD           │
│  ░░░░░░░░░░░░░░░░░░░░ -50% volumen       │
│                                           │
├───────────────────────────────────────┤
│  Volumen objetivo: 14-18 series/músculo   │
│  Foco: Hombros, Espalda (V-taper)         │
│                                           │
└───────────────────────────────────────┘
```

---

## 13. PANTALLA: PLAN SEMANAL (Para el corcho)

> Acceso: Desde Home o Plan Anual
> Opción: Exportar/Compartir como imagen

### 13.1 Vista Semanal

```
┌───────────────────────────────────────┐
│  ←          SEMANA 18 JUN          📷    │  ← Exportar imagen
│              Volumen - Sem 2/4            │
├───────────────────────────────────────┤
│                                           │
│  LUNES - PIERNA                           │
│  ─────────────                              │
│  Sentadilla         3x8   80kg            │
│  Hip Thrust         3x10  25kg            │
│  RDL                3x12  16kg            │
│  Hack Squat         3x12  25kg            │
│  Core (Bird dog)    3x12                  │
│                                           │
│  MARTES - DESCANSO                        │
│  ─────────────────                          │
│  Movilidad opcional                       │
│                                           │
│  MIÉRCOLES - PUSH          ◀ HOY          │
│  ───────────────────                        │
│  Press Inclinado    4x10  18kg            │
│  Cruces Polea       3x12                  │
│  Elev Lat Sentado   4x15  5kg             │
│  Elev Lat Polea     3x20                  │
│  Press Francés      3x10  6kg             │
│  Core (Dead bug)    3x12                  │
│                                           │
│  JUEVES - DESCANSO                        │
│  ─────────────────                          │
│  Movilidad opcional                       │
│                                           │
│  VIERNES - PULL                           │
│  ───────────────                            │
│  Dominadas          3x4   BW              │
│  Remo Neutro        3x10  42kg            │
│  Kelso Shrug        3x15  12kg            │
│  Curl Z             3x10                  │
│  Zottman            3x12  7kg             │
│  Granjero           3x45s 26kg            │
│                                           │
│  SÁBADO - UPPER                            │
│  ──────────────                             │
│  (ejercicios...)                          │
│                                           │
│  DOMINGO - DESCANSO                       │
│  ─────────────────                          │
│  Recuperación activa                       │
│                                           │
└───────────────────────────────────────┘
```

### 13.2 Botón Exportar (📷)

Al tocar genera imagen PNG del plan semanal:
- Fondo oscuro, texto claro
- Tamaño A4 para imprimir
- Compartir vía WhatsApp/Email/Guardar

```
┌───────────────────────────────────────┐
│           EXPORTAR COMO                   │
├───────────────────────────────────────┤
│                                           │
│  [ 🖼 Guardar imagen ]                    │
│                                           │
│  [ 📤 Compartir ]                         │
│                                           │
│  [ 🖨 Imprimir PDF ]                      │
│                                           │
└───────────────────────────────────────┘
```

---

## 14. Navegación Actualizada

```
                    ┌────────────┐
                    │    HOME    │
                    └─────┬──────┘
                          │
          ┌──────────────┼──────────────┐
          │              │              │
          ▼              ▼              ▼
   ┌───────────┐  ┌──────────┐  ┌───────────┐
   │ PLAN ANUAL│  │PLAN SEMAN│  │  ENTRENO  │
   └─────┬─────┘  └──────────┘  └─────┬─────┘
          │                            │
          ▼                            ▼
   ┌───────────┐               (flujo gym...)
   │DETALLE MES│
   └───────────┘
```
