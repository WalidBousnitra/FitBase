---
id: "SYS-00"
nombre: "Manifest Maestro"
fecha_modificacion: "18/06/2026"
estado: "PROD_ACTUAL"
relacionados: ["all"]
tags: ["system-prompt", "core", "arquitectura", "bible"]
---

# Manifest Maestro

## 1. Propósito y REGLA CERO

Este documento es la **Única Fuente de Verdad (SSOT)** del proyecto FitBase.

> **REGLA CERO:** Tu única fuente de verdad son los archivos `.md` de este repositorio. 
> Queda **PROHIBIDO** inferir, inventar o utilizar conocimiento pre-entrenado para lógica de negocio, ejercicios o pautas nutricionales.

## 2. Stack Tecnológico

| Componente | Tecnología | Notas |
|------------|------------|-------|
| **Frontend** | Android (Java + Views/XML) | NO Kotlin, NO Compose |
| UI | Material Design 3, modo oscuro | Minimalismo absoluto |
| Backend | Google Apps Script | Solo API REST, sin lógica pesada |
| Base de Datos | Google Sheets | 14 hojas (REG-LOG-02) |
| Métricas | Amazfit GTS 4 (Zepp) | Sueño, HRV, FC, pasos |
| Báscula | Xiaomi Mi Scale | Peso, composición corporal |
| Dispositivo | Xiaomi Redmi Note 14 Pro 5G | Referencia para UI |

### Arquitectura
```
┌────────────────────────────────────────┐
│       MÓVIL (Java, todo local)         │
│   App FitBase - UI + Lógica + Cache    │
└───────────────┬────────────────────────┘
                │ HTTP/REST (cuando hay red)
                ▼
┌────────────────────────────────────────┐
│         GOOGLE CLOUD (gratis)          │
│   Apps Script (API) → Sheets (BD)      │
└────────────────────────────────────────┘
```

---

## 3. Mapa Estructural (39 archivos)

```
/knowledge_base
├── manifest.md              # [SYS-00] Este archivo
├── plantilla.md             # Plantilla para crear nuevos MDs
│
├── /usuario                 # Datos personales y tracking
│   ├── prioridades.md       # [USR-01] ★ Ranking de objetivos
│   ├── biometria.md         # [USR-02] Medidas y objetivos físicos
│   ├── equipamiento.md      # [USR-03] Gym, cocina, dispositivos
│   ├── /perfil
│   │   ├── cultura.md       # [USR-PER-01] Restricciones culturales
│   │   └── horarios.md      # [USR-PER-02] Disponibilidad y cronotipo
│   └── /metricas
│       ├── hardware.md      # [USR-MET-01] Zepp + Health Connect
│       └── subjetivas.md    # [USR-MET-02] RPE, energía, estrés
│
├── /evidencia               # Papers científicos (1:1 con prioridades)
│   ├── _indice_papers.md    # [EVI-00] Índice de papers procesados
│   ├── _guia_extraccion.md  # Guía para extraer papers
│   ├── estetica.md          # [EVI-01] P1: Estética muscular
│   ├── postura.md           # [EVI-02] P2: Corrección postural
│   ├── hipertrofia.md       # [EVI-03] P3: Crecimiento muscular
│   ├── flexibilidad.md      # [EVI-04] P4: Movilidad articular
│   ├── estres.md            # [EVI-05] P5: Cortisol y estrés
│   ├── hormonal.md          # [EVI-06] P6: Salud hormonal
│   ├── vitalidad.md         # [EVI-07] P7: Energía y vitalidad
│   ├── digestivo.md         # [EVI-08] P8: Salud digestiva
│   ├── agilidad.md          # [EVI-09] P9: Agilidad
│   ├── cardio.md            # [EVI-10] P10: Capacidad cardiovascular
│   ├── nutricion.md         # [EVI-11] Soporte: Ciencia nutricional
│   ├── sueno.md             # [EVI-12] Soporte: Sueño y recuperación
│   ├── suplementacion.md    # [EVI-13] Soporte: Suplementos
│   ├── periodizacion.md     # [EVI-14] Soporte: Periodización y ciclos
│   ├── lesiones.md          # [EVI-15] Soporte: Dolor y rehabilitación
│   ├── calentamiento.md     # [EVI-16] Soporte: Warm-up y activación
│   └── fatiga_mental.md     # [EVI-17] Soporte: Fatiga mental y rendimiento
│
└── /reglas                  # Lógica de negocio de la app
    ├── /entrenamiento
    │   ├── programacion.md  # [REG-ENT-01] Split, frecuencia, periodización
    │   ├── ejercicios.md    # [REG-ENT-02] Inventario y selección
    │   ├── calentamiento.md # [REG-ENT-03] Activación y movilidad
    │   └── preferencias.md  # [REG-ENT-04] Preferencias de entrenamiento
    ├── /nutricion
    │   ├── motor_dieta.md   # [REG-NUT-01] Cálculo de macros
    │   └── preferencias.md  # [REG-NUT-02] Filtros alimentarios
    ├── /logica
    │   ├── motor_pesos.md   # [REG-LOG-01] Autorregulación de cargas
    │   ├── base_datos.md    # [REG-LOG-02] Esquema Google Sheets (14 hojas)
    │   └── excepciones.md   # [REG-LOG-03] Viajes, enfermedad, Ramadán
    └── /desarrollo
        ├── PROMPT_DESARROLLO.md  # [REG-DEV-00] ★ Prompt maestro para IA
        ├── MANUAL_DESPLIEGUE.md  # [REG-DEV-03] Guía paso a paso
        ├── ui.md                 # [REG-DEV-01] Especificación Android
        └── compilador.md         # [REG-DEV-02] Reglas de código para IA
        └── compilador.md    # [REG-DEV-02] Reglas de código para IA
```

---

## 4. Sistema de IDs

| Prefijo | Dominio | Ejemplo |
|---------|---------|---------|
| `SYS` | Sistema | SYS-00 (manifest) |
| `USR` | Usuario | USR-01 (prioridades) |
| `EVI` | Evidencia | EVI-03 (hipertrofia) |
| `REG-ENT` | Reglas Entrenamiento | REG-ENT-01 |
| `REG-NUT` | Reglas Nutrición | REG-NUT-01 |
| `REG-LOG` | Reglas Lógica | REG-LOG-01 |
| `REG-DEV` | Reglas Desarrollo | REG-DEV-01 |

---

## 5. Flujo de Trabajo

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  USUARIO    │────▶│  EVIDENCIA   │────▶│   REGLAS    │
│  (contexto) │     │  (papers)    │     │  (lógica)   │
└─────────────┘     └──────────────┘     └─────────────┘
       │                   │                    │
       └───────────────────┴────────────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │    APP      │
                    │  (código)   │
                    └─────────────┘
```

1. **Usuario** define prioridades y contexto personal
2. **Evidencia** respalda decisiones con papers científicos
3. **Reglas** traducen evidencia en lógica de negocio
4. **App** implementa las reglas en código

---

## 6. Reglas de Modificación

1. **Prohibido** crear archivos fuera de esta estructura
2. **Obligatorio** actualizar este manifest al añadir/eliminar archivos
3. Usar `plantilla.md` para crear nuevos documentos
4. Mantener relación 1:1 entre prioridades y evidencia

---

## 7. Contexto de Desarrollo (Leer antes de generar código)

### 7.1 Fechas Clave
| Fecha | Evento |
|-------|--------|
| 18/06/2026 | Knowledge base completado |
| 01/09/2026 | **Fecha objetivo de lanzamiento** |
| 01/09/2026 - 01/01/2027 | Primera fase de uso (4 meses) |

### 7.2 Usuario Objetivo
- **Único usuario**: El desarrollador es el usuario
- **Edad**: 24 años (20/07/2001)
- **Nivel fitness**: Intermedio-avanzado
- **Dispositivo**: Xiaomi Redmi Note 14 Pro 5G (6.67" AMOLED)
- **Wearable**: Amazfit GTS 4
- **Báscula**: Xiaomi Mi Scale

### 7.3 Archivos Críticos para el Código
| Archivo | Contiene | Prioridad |
|---------|----------|-----------|
| [compilador.md](reglas/desarrollo/compilador.md) | Estructura código, convenciones | ⭐⭐⭐ |
| [ui.md](reglas/desarrollo/ui.md) | Wireframes, flujos, gestos | ⭐⭐⭐ |
| [base_datos.md](reglas/logica/base_datos.md) | Esquema 14 hojas Sheets | ⭐⭐⭐ |
| [motor_pesos.md](reglas/logica/motor_pesos.md) | Algoritmo autorregulación | ⭐⭐ |
| [motor_dieta.md](reglas/nutricion/motor_dieta.md) | Cálculo macros | ⭐⭐ |
| [prioridades.md](usuario/prioridades.md) | Orden de decisiones | ⭐⭐ |
| [biometria.md](usuario/biometria.md) | Datos físicos actuales | ⭐ |

### 7.4 Orden de Generación de Código
```
1. Apps Script (backend)
   └── Código para doGet/doPost
   └── Funciones CRUD por hoja
   
2. Android (proyecto base)
   └── Estructura MVVM
   └── Retrofit + Room
   └── Modelos de datos
   
3. Android (pantallas)
   └── Home (macros del día)
   └── Workout flow (swipe)
   └── Timer (fullscreen)
   └── Plan anual/semanal
   └── Nutrición
   └── Settings
```

### 7.5 Preparación del Usuario (Antes de 01/09)
```yaml
GOOGLE:
  - Crear Spreadsheet "FitBase_DB" en Drive
  - Crear las 14 hojas con headers exactos
  - Crear proyecto Apps Script vinculado
  - Deploy como Web App
  - Copiar URL del endpoint

ANDROID_STUDIO:
  - Instalar Android Studio
  - Configurar SDK 24-34
  - Crear proyecto "FitBase"
  - Copiar código generado

DISPOSITIVO:
  - Habilitar "Developer options"
  - Habilitar "USB debugging"
  - Conectar y probar
```

---

## 8. Estado del Proyecto

| Componente | Estado | Archivos |
|------------|--------|----------|
| Evidencia | ✅ COMPLETO | 19 archivos, ~40 papers |
| Usuario | ✅ COMPLETO | 8 archivos |
| Reglas | ✅ COMPLETO | 12 archivos |
| Código | ⏳ PENDIENTE | Por generar |

**Próximo paso**: Ejecutar prompt de desarrollo
