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

### Filosofía: NADA HARDCODEADO

```
┌──────────────────────────────────────────────────────────────┐
│  Este repositorio NO contiene:                               │
│    ✗ Plan de entrenamiento predefinido                       │
│    ✗ Ejercicios específicos hardcodeados                     │
│    ✗ Fases (CUT/BULK) con fechas                             │
│    ✗ Rutinas fijas                                           │
│                                                              │
│  Este repositorio SÍ contiene:                               │
│    ✓ Datos del usuario (biometría, preferencias)             │
│    ✓ Evidencia científica (papers procesados)                │
│    ✓ Reglas de lógica (cómo usar la evidencia)               │
│    ✓ Prompt para que la IA genere TODO dinámicamente         │
│                                                              │
│  DURACIÓN DEL PLAN: 11 meses (definido por usuario)          │
└──────────────────────────────────────────────────────────────┘
```

Ver: `/PROMPT_FITBASE.md` para instrucciones de generación.

## 2. Stack Tecnológico

| Componente | Tecnología | Notas |
|------------|------------|-------|
| **Frontend** | Android (Java + Views/XML) | NO Kotlin, NO Compose |
| UI | Material Design 3, modo oscuro | Minimalismo absoluto |
| Backend | Google Apps Script | Solo API REST, sin lógica pesada |
| Base de Datos | Google Sheets | Esquema simplificado (REG-LOG-02) |
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

## 3. Mapa Estructural

```
/FitBase-main
├── AGENTS.md                # Instrucciones para GitHub Copilot
├── PROMPT_FITBASE.md        # ★ PROMPT PRINCIPAL (usar este)
│
└── /knowledge_base
    ├── manifest.md          # [SYS-00] Este archivo
    ├── plantilla.md         # Plantilla para crear nuevos MDs
    │
    ├── /usuario             # MIS DATOS (rellenar)
    │   ├── prioridades.md   # [USR-01] ★ Ranking de objetivos
    │   ├── biometria.md     # [USR-02] Datos físicos actuales
    │   ├── preferencias_ejercicios.md  # [USR-04] Favoritos/exclusiones
    │   ├── equipamiento.md  # [USR-03] Gym, cocina, dispositivos
    │   ├── /perfil
    │   │   ├── cultura.md   # [USR-PER-01] Halal, Ramadán
    │   │   └── horarios.md  # [USR-PER-02] Disponibilidad
    │   └── /metricas
    │       ├── hardware.md  # [USR-MET-01] Zepp + Health Connect
    │       └── subjetivas.md # [USR-MET-02] RPE, energía
    │
    ├── /evidencia           # CIENCIA (papers procesados)
    │   ├── _indice_papers.md
    │   ├── hipertrofia.md   # [EVI-03] Schoenfeld: volumen, frecuencia
    │   ├── nutricion.md     # [EVI-11] Mifflin, Helms: calorías, macros
    │   ├── periodizacion.md # [EVI-14] Bompa: fases, ciclos
    │   ├── postura.md       # [EVI-02] Ejercicios correctivos
    │   └── ... (17 archivos total)
    │
    └── /reglas              # LÓGICA (basada en evidencia)
        ├── /entrenamiento
        │   ├── programacion.md  # [REG-ENT-01] Split, frecuencia
        │   ├── seleccion_ejercicios.md # [REG-ENT-02] Reglas de selección
        │   ├── calentamiento.md # [REG-ENT-03]
        │   └── preferencias.md  # [REG-ENT-04]
        ├── /nutricion
        │   ├── motor_dieta.md   # [REG-NUT-01] Cálculo macros
        │   └── preferencias.md  # [REG-NUT-02] Filtros alimentarios
        ├── /natacion
        │   └── piscina.md       # [REG-NAT-01] 2x/semana
        ├── /logica
        │   ├── motor_pesos.md   # [REG-LOG-01] Progresión de cargas
        │   ├── base_datos.md    # [REG-LOG-02] Esquema Sheets simplificado
        │   └── excepciones.md   # [REG-LOG-03] Viajes, enfermedad
        └── /desarrollo
            ├── ui.md            # [REG-DEV-01] Especificación pantallas
            ├── compilador.md    # [REG-DEV-02] Reglas de código
            └── Sistema_Diseno_Fitness.md # [REG-DEV-03] Colores, diseño
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
| [base_datos.md](reglas/logica/base_datos.md) | Esquema Sheets simplificado | ⭐⭐⭐ |
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
  - Crear las hojas activas definidas en base_datos.md con headers exactos
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
