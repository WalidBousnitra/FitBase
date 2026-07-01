# FitBase AI Agent Instructions

## Propósito
Base de conocimiento para una app de programación de entrenamiento y nutrición. El repositorio contiene exclusivamente archivos Markdown que sirven como fuente de verdad para la IA.

## REGLA CERO
> **PROHIBIDO** inferir, inventar o usar conocimiento pre-entrenado para lógica de negocio, ejercicios o pautas nutricionales.
> La **ÚNICA** fuente de verdad son los archivos `.md` de `/knowledge_base/`.

## Estructura del Proyecto

```
/knowledge_base
├── manifest.md           # ★ Mapa maestro - LEER PRIMERO
├── plantilla.md          # Plantilla para crear nuevos MDs
│
├── /usuario              # Contexto personal (rellenar con datos del usuario)
│   ├── prioridades.md    # ★ Ranking de objetivos (guía toda la lógica)
│   ├── biometria.md      # Medidas corporales y objetivos
│   ├── equipamiento.md   # Gym, cocina, dispositivos
│   ├── /perfil           # Cultura, horarios, cronotipo
│   └── /metricas         # Hardware (Zepp) + subjetivas (RPE)
│
├── /evidencia            # ✅ Papers científicos COMPLETOS (17 archivos)
│   ├── _indice_papers.md # Índice de 31 papers procesados
│   ├── estetica.md       # P1: Estética muscular
│   ├── postura.md        # P2: Corrección postural
│   ├── hipertrofia.md    # P3: Crecimiento muscular (Schoenfeld x4)
│   ├── flexibilidad.md   # P4: Movilidad (Page, Bandy, Afonso)
│   ├── estres.md         # P5: Cortisol (Salmon, Kraemer)
│   ├── hormonal.md       # P6: Salud hormonal (Kraemer)
│   ├── vitalidad.md      # P7: Energía (Fullagar, Salmon)
│   ├── digestivo.md      # P8: Salud digestiva (Mailing)
│   ├── agilidad.md       # P9: Agilidad (Asadi)
│   ├── cardio.md         # P10: Cardiovascular (Wilson, Viana)
│   ├── nutricion.md      # Soporte (Helms, Iraki, Chaouachi)
│   ├── sueno.md          # Soporte (Fullagar)
│   ├── suplementacion.md # Soporte (Helms, Iraki)
│   ├── periodizacion.md  # Soporte (Bompa)
│   ├── lesiones.md       # Soporte (Smith)
│   ├── calentamiento.md  # Soporte (Rodrigues)
│   └── fatiga_mental.md  # Soporte (Van Cutsem)
│
└── /reglas               # Lógica de negocio (actualizada con evidencia)
    ├── /entrenamiento    # Programación, ejercicios, calentamiento
    ├── /nutricion        # Motor de dieta, preferencias
    ├── /natacion         # Clases de natación 2x/semana
    ├── /logica           # Motor de cargas, BD, excepciones
    └── /desarrollo       # UI y reglas de código
```

## Arquitectura Técnica

| Componente | Tecnología |
|------------|------------|
| Frontend | Android (Java + Views/XML) |
| Backend | Google Apps Script (V8) |
| Base de Datos | Google Sheets (esquema simplificado) |
| Métricas | Health Connect ← Zepp (Amazfit GTS 4) |

## Archivos Clave

| Archivo | Propósito |
|---------|-----------|
| `manifest.md` | Mapa completo y reglas de estructura |
| `usuario/prioridades.md` | Orden de importancia para decisiones |
| `evidencia/_indice_papers.md` | Índice de 31 papers procesados |
| `reglas/entrenamiento/programacion.md` | Volumen, frecuencia, periodización |
| `reglas/logica/motor_pesos.md` | Autorregulación de cargas |
| `reglas/nutricion/motor_dieta.md` | Cálculo de macros |

## Comportamiento del Agente

### HACER
- Leer `manifest.md` antes de cualquier acción
- Respetar el orden de `prioridades.md` en decisiones
- Usar `plantilla.md` para crear nuevos documentos
- Usar datos de `/evidencia/` para toda lógica de entrenamiento/nutrición
- Generar código en español (Java frontend, Apps Script backend)
- Justificar toda lógica con referencia a archivos de `/knowledge_base/`

### NO HACER
- Crear archivos fuera de la estructura del manifest
- Inventar reglas de entrenamiento o nutrición
- Usar conocimiento externo para lógica de negocio
- Modificar prioridades sin autorización explícita

## Flujo de Trabajo

```
USUARIO define contexto → EVIDENCIA respalda con papers → REGLAS traducen a lógica → APP implementa en código
```

## Estado del Proyecto (18/06/2026)

| Componente | Estado | Archivos |
|------------|--------|----------|
| Evidencia | ✅ **COMPLETO** | 17 archivos, 31 papers |
| Reglas | ✅ **ACTUALIZADO** | Programación con datos reales |
| Usuario | ⏳ Parcial | Biometría en borrador |
| App | ⏳ Pendiente | No iniciado |

## Notas
- No hay archivos de build (`package.json`, `build.gradle`, etc.)
- Todos los archivos de evidencia están **ACTIVOS** con datos de papers reales
- La carpeta `contexto/` antigua fue renombrada a `usuario/`
