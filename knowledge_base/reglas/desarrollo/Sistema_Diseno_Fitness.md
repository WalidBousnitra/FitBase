# SISTEMA DE DISEÑO: FITNESS VIVO & PREMIUM
Especificación técnica completa para UI/UX en Android (Views/XML) inspirado en Apple iOS Clean Aesthetic.

> **IMPORTANTE**: Este proyecto usa **Views/XML** exclusivamente. NO usar Jetpack Compose.

Este documento contiene todos los tokens de diseño, tipografías, dimensiones, reglas de espaciado, componentes gráficos clave y especificaciones detalladas de animaciones y transiciones.

---

## 1. Arquitectura de Color (Tema DUAL: Claro + Oscuro)

El sistema sigue la filosofía Apple de **colores semánticos** que se adaptan automáticamente al tema.

### 1.1 Tema CLARO (Light Mode)

| Token | Hex | Descripción |
| :--- | :--- | :--- |
| `colorBackground` | `#F2F2F7` | Fondo base (Apple systemGray6) |
| `colorSurface` | `#FFFFFF` | Tarjetas y contenedores |
| `colorSurfaceElevated` | `#FFFFFF` | Modales, bottom sheets |
| `colorTextPrimary` | `#000000` | Títulos, métricas principales |
| `colorTextSecondary` | `#3C3C43` α60% | Subtítulos, labels |
| `colorTextTertiary` | `#3C3C43` α30% | Placeholders, hints |
| `colorSeparator` | `#3C3C43` α30% | Líneas divisorias |

### 1.2 Tema OSCURO (Dark Mode) — **PREFERIDO**

| Token | Hex | Descripción |
| :--- | :--- | :--- |
| `colorBackground` | `#000000` | Fondo base (Apple true black OLED) |
| `colorSurface` | `#1C1C1E` | Tarjetas (Apple systemGray6 dark) |
| `colorSurfaceElevated` | `#2C2C2E` | Modales, bottom sheets |
| `colorTextPrimary` | `#FFFFFF` | Títulos, métricas principales |
| `colorTextSecondary` | `#EBEBF5` α60% | Subtítulos, labels |
| `colorTextTertiary` | `#EBEBF5` α30% | Placeholders, hints |
| `colorSeparator` | `#545458` α65% | Líneas divisorias |

### 1.3 Colores de Acento (Iguales en ambos temas)

| Token | Hex | Uso |
| :--- | :--- | :--- |
| `colorAccentPrimary` | `#FF2D55` | Botones principales, enlaces |
| `colorAccentSecondary` | `#FF9500` | Fin del gradiente |
| `colorGradientStart` | `#FF2D55` | Gradiente botón CTA |
| `colorGradientEnd` | `#FF9500` | Gradiente botón CTA |
| `colorSuccess` | `#30D158` | Completado, PR, anillos cerrados |
| `colorWarning` | `#FFD60A` | Advertencias, fatiga alta |
| `colorError` | `#FF453A` | Errores, sobreentrenamiento |
| `colorChart` | `#5E5CE6` | Gráficas de rendimiento |
| `colorChartSecondary` | `#BF5AF2` | Gráficas secundarias |

## 2. Sistema Tipográfico (Escala Android sp)
Se recomienda la fuente **Inter** o **SF Pro**. En Android, mapear los tamaños usando `sp` y pesos definidos:

| Estilo de Texto | Tamaño (sp) | Peso (Font Weight) | Line Height | Uso sugerido |
| :--- | :--- | :--- | :--- | :--- |
| **DisplayMetric** | 36sp | ExtraBold (800) | 1.1 | Grandes números en tiempo real. |
| **LargeTitle** | 32sp | Bold (700) | 1.2 | Títulos de pestañas raíz. |
| **Header2** | 22sp | SemiBold (600) | 1.3 | Títulos dentro de vistas o rutinas. |
| **BodyPrimary** | 16sp | Normal (400) | 1.4 | Descripciones de entrenamientos. |
| **CaptionMetrics**| 13sp | Medium (500) | 1.2 | Sub-datos dentro de tarjetas. |

## 3. Proporciones, Cuadrícula y Layout (dp)
Implementar el sistema de diseño basado en **8dp**.

* **Margen de Pantalla Global:** `20dp` en los bordes izquierdo y derecho.
* **Separación entre Tarjetas:** `16dp` verticalmente.
* **Radio de Tarjeta (Border Radius):** `24dp` para contenedores (Look redondeado Apple).
* **Radio de Botones de Acción:** Completamente redondeados (forma de píldora, `CornerRadius = 100dp`).
* **Altura del Botón Principal:** Mínimo `56dp` para pulsación táctil con fatiga.
* **Área de Toque Mínima:** `44dp x 44dp` para cualquier icono.

## 4. Catálogo de Animaciones y Transiciones

### A. Transiciones de Pantalla
* **ios_push_enter / ios_push_exit**
  * *Efecto:* Entra desde la derecha con escalado 0.95 a 1.0, la anterior se desplaza 30% a la izquierda bajando opacidad a 0.5.
  * *Interpolator:* `CubicBezier(0.25, 1.0, 0.5, 1.0)`
  * *Duración:* 400ms.
* **shared_element_exercise_card**
  * *Efecto:* La tarjeta de rutina se expande suavemente transformándose en la cabecera.
  * *Interpolator:* `FastOutSlowInEasing`. Duración: 450ms.

### B. Micro-Interacciones
* **button_press_scale**
  * *Efecto:* Al presionar, reduce escala a `0.94f` y regresa elásticamente a `1.0f`.
  * *Mecánica:* Spring Force (`dampingRatio = LowBouncy`, `stiffness = Medium`).
* **progress_ring_draw**
  * *Efecto:* Los anillos circulares se dibujan desde 0 al valor actual.
  * *Interpolator:* `LinearOutSlowInEasing`. Duración: 1200ms.
* **staggered_list_fade_in**
  * *Efecto:* Ítems aparecen de arriba hacia abajo con retardo en cascada de 40ms, elevándose desde 10dp abajo.

## 5. Bloque JSON para Generación de Código

```json
{
  "design_system_name": "Apple-Inspired-Fitness-Vibrant",
  "platform": "Android Views/XML (NO Compose)",
  "default_theme": "dark",
  
  "colors_light": {
    "colorBackground": "#F2F2F7",
    "colorSurface": "#FFFFFF",
    "colorSurfaceElevated": "#FFFFFF",
    "colorTextPrimary": "#000000",
    "colorTextSecondary": "#3C3C4399",
    "colorTextTertiary": "#3C3C434D",
    "colorSeparator": "#3C3C434D"
  },
  
  "colors_dark": {
    "colorBackground": "#000000",
    "colorSurface": "#1C1C1E",
    "colorSurfaceElevated": "#2C2C2E",
    "colorTextPrimary": "#FFFFFF",
    "colorTextSecondary": "#EBEBF599",
    "colorTextTertiary": "#EBEBF54D",
    "colorSeparator": "#545458A6"
  },
  
  "colors_accent": {
    "colorAccentPrimary": "#FF2D55",
    "colorAccentSecondary": "#FF9500",
    "colorSuccess": "#30D158",
    "colorWarning": "#FFD60A",
    "colorError": "#FF453A",
    "colorChart": "#5E5CE6",
    "colorChartSecondary": "#BF5AF2"
  },

  "typography": {
    "font_family": "Inter",
    "display_metric": { "size_sp": 36, "weight": 800, "line_height": 1.1 },
    "large_title": { "size_sp": 32, "weight": 700, "line_height": 1.2 },
    "header_card": { "size_sp": 22, "weight": 600, "line_height": 1.3 },
    "body": { "size_sp": 16, "weight": 400, "line_height": 1.4 },
    "caption": { "size_sp": 13, "weight": 500, "line_height": 1.2 }
  },

  "layout_specs": {
    "screen_padding_horizontal_dp": 20,
    "card_spacing_dp": 16,
    "card_corner_radius_dp": 24,
    "button_corner_radius_dp": 100,
    "button_height_dp": 56,
    "touch_target_min_dp": 44,
    "grid_base_dp": 8
  },

  "animations": {
    "screen_transition": {
      "duration_ms": 400,
      "easing": "cubic-bezier(0.25, 1.0, 0.5, 1.0)"
    },
    "button_press": {
      "scale_pressed": 0.94,
      "spring_damping": "low_bouncy",
      "spring_stiffness": "medium"
    },
    "progress_ring": {
      "duration_ms": 1200,
      "easing": "LinearOutSlowIn"
    },
    "list_stagger_delay_ms": 40
  }
}
```

---

## 6. Implementación Android (colors.xml)

### values/colors.xml (Tema Claro por defecto)
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Colores base (Light) -->
    <color name="colorBackground">#F2F2F7</color>
    <color name="colorSurface">#FFFFFF</color>
    <color name="colorSurfaceElevated">#FFFFFF</color>
    <color name="colorTextPrimary">#000000</color>
    <color name="colorTextSecondary">#993C3C43</color>
    <color name="colorTextTertiary">#4D3C3C43</color>
    <color name="colorSeparator">#4D3C3C43</color>
    
    <!-- Acentos (iguales en ambos temas) -->
    <color name="colorAccentPrimary">#FF2D55</color>
    <color name="colorAccentSecondary">#FF9500</color>
    <color name="colorSuccess">#30D158</color>
    <color name="colorWarning">#FFD60A</color>
    <color name="colorError">#FF453A</color>
    <color name="colorChart">#5E5CE6</color>
    <color name="colorChartSecondary">#BF5AF2</color>
</resources>
```

### values-night/colors.xml (Tema Oscuro)
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Colores base (Dark) - OLED True Black -->
    <color name="colorBackground">#000000</color>
    <color name="colorSurface">#1C1C1E</color>
    <color name="colorSurfaceElevated">#2C2C2E</color>
    <color name="colorTextPrimary">#FFFFFF</color>
    <color name="colorTextSecondary">#99EBEBF5</color>
    <color name="colorTextTertiary">#4DEBEBF5</color>
    <color name="colorSeparator">#A6545458</color>
</resources>
```

---

## 7. Notas de Implementación

### Cambio automático de tema
- Android cambia automáticamente entre `values/` y `values-night/` según la configuración del sistema
- Para forzar tema oscuro por defecto: `AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)`

### Fuente Inter
1. Descargar de [Google Fonts](https://fonts.google.com/specimen/Inter)
2. Colocar en `res/font/inter_*.ttf`
3. Crear `res/font/inter.xml` para la familia de fuentes

### Gradiente en botones
```xml
<!-- res/drawable/gradient_primary_button.xml -->
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient
        android:startColor="@color/colorAccentPrimary"
        android:endColor="@color/colorAccentSecondary"
        android:angle="90"/>
    <corners android:radius="100dp"/>
</shape>
```

### Animaciones Spring (botones)
Usar `SpringAnimation` de AndroidX:
```java
SpringAnimation scaleX = new SpringAnimation(view, DynamicAnimation.SCALE_X, 1f);
scaleX.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY);
scaleX.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
```
