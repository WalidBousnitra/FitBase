---
id: "SYS-00"
nombre: "Manifest Maestro, Protocolo RAG y Arquitectura del Sistema"
fecha_modificacion: "16/06/2026"
estado: "PROD_ACTUAL"
relacionados: ["all"]
tags: ["system-prompt", "core", "arquitectura", "bible", "ci-cd"]
---

# CONTENIDO

## 1. Alcance, Rol de la IA y Stack Tecnológico
Este documento es la **Única Fuente de Verdad (Single Source of Truth - SSOT)** del proyecto. 
Eres un Ingeniero de Software Senior operando bajo una arquitectura *Knowledge-Driven*. Tu objetivo es compilar, mantener y refactorizar el código de este ecosistema.

**REGLA CERO:** Tu única fuente de verdad son los archivos `.md` de este repositorio. Queda estrictamente prohibido inferir, inventar o utilizar conocimiento externo pre-entrenado para definir la lógica de negocio, los ejercicios o las pautas nutricionales.

**Stack Tecnológico Autorizado:**
* **Frontend:** Android App Nativa (Kotlin). Actúa como un visualizador ágil (Dumb UI) centrado en el registro rápido de datos bajo fatiga.
* **Backend:** Google Apps Script (REST API). Actúa como el cerebro computacional y motor de lógica.
* **Base de Datos:** Google Sheets. Persistencia relacional tablar (SSOT de los datos del usuario).
* **Ingesta de Hardware:** Health Connect API (Android) y mapeo de datos de Zepp (Amazfit GTS 4).

---

## 2. Mapa Estructural y Matriz de Dominios
Queda prohibida la creación de archivos fuera de esta estructura plana. Toda modificación de la arquitectura debe ser autorizada y registrada exclusivamente en este bloque.

```text
/knowledge_base
├── manifest.md                 # [SYS-00] ESTE ARCHIVO: Núcleo de control y flujo.
│
├── /plantilla.md               # Plantilla de documentos de conocimiento.
├── /plantilla reglas.txt       # Plantilla de reglas de negocio o datos.
│
├── /contexto                   # Datos del usuario y del entorno que NO modifican reglas directas.
│   ├── /perfil
│   │   ├── biometria.md        # Datos físicos basales, historial clínico, medidas y lesiones.
│   │   ├── horarios.md         # Ventanas horarias disponibles, cronotipo y bloques de descanso.
│   │   └── cultura.md          # Fusión gastronómica España/Marruecos y adaptaciones de calendario.
│   │
│   ├── /nutricion
│   │   ├── preferencias.md     # Filtros de alimentos, intolerancias y logística de cocina.
│   │   └── objetivos.md        # Objetivos actuales, macros base y metas de actividad.
│   │
│   ├── prioridades.md         # Prioridades globales del proyecto en orden de importancia.
│   │
│   └── /metricas
│       ├── hardware_zepp.md    # Mapeo de datos del Amazfit GTS 4.
│       ├── subjetivas.md       # Logs de estrés mental, fatiga subjetiva y energía pre-entreno.
│       └── salud_connect.md    # Ingesta de pasos diarios y NEAT.
│
├── /evidencia                  # Hallazgos, papers y evidencia científica de apoyo.
│   ├── cardio.md               # Evidencia sobre cardio y capacidad aeróbica.
│   ├── fuerza.md               # Evidencia sobre fuerza y adaptaciones neuromusculares.
│   ├── hipertrofia
│   │   └── hipertrofia.md       # Evidencia sobre hipertrofia muscular y crecimiento.
│   ├── intensidad.md           # Evidencia sobre intensidad, RPE y cargas relativas.
│   ├── lesiones.md             # Evidencia sobre prevención y recuperación de lesiones.
│   ├── metabolismo.md          # Evidencia sobre metabolismo y adaptación energética.
│   ├── periodizacion.md        # Evidencia sobre ciclos y planificación.
│   ├── postura
│   │   └── postura.md          # Evidencia sobre postura y control corporal.
│   ├── recuperacion.md         # Evidencia sobre recuperación, sueño y regeneración.
│   ├── rendimiento.md          # Evidencia sobre potencia y eficiencia.
│   ├── salud.md                # Evidencia sobre salud general y estado fisiológico.
│   ├── suplementacion.md       # Evidencia sobre suplementos y ayudas ergogénicas.
│   ├── volumen.md              # Evidencia sobre volumen óptimo y estrés mecánico.
│   ├── frecuencia.md           # Evidencia sobre frecuencia de entrenamiento.
│   ├── flexibilidad.md         # Evidencia sobre flexibilidad y estiramientos.
│   ├── psicologia.md           # Evidencia sobre motivación, hábitos y adherencia.
│   ├── progreso.md             # Evidencia sobre adaptación y progresión de cargas.
│   ├── nutricion
│   │   └── ciencia_nutricion.md    # Evidencia científica sobre digestión y timing.
│   └── movilidad
│       └── movilidad.md            # Evidencia sobre movilidad y rango articular.
│
└── /reglas                    # Reglas de negocio y lógica que afectan decisiones directas.
    ├── /gimnasio
    │   ├── inventario.md       # Reglas del equipo disponible y selección de cargas.
    │   ├── rutina.md           # Reglas de programación de sesiones y bloques de trabajo.
    │   └── calentamiento.md    # Reglas de activación neuromuscular y preparación.
    │
    ├── /movilidad
    │   ├── diagnostico.md      # Reglas para diagnóstico de desequilibrios y prioridades.
    │   └── protocolos.md       # Reglas de movilidad y protocolos correctivos.
    │
    ├── /natacion
    │   └── piscina.md          # Reglas de sesiones de nado y volumen técnico.
    │
    ├── /logica
    │   ├── motor_pesos.md      # Algoritmos matemáticos y factores de penalización.
    │   ├── motor_dieta.md      # Reglas de ajuste calórico dinámico.
    │   ├── base_datos.md       # Reglas de arquitectura, indexación y nomenclatura en Sheets.
    │   └── excepciones.md      # Reglas de contingencia: viajes, enfermedad, atípicos.
    │
    └── /desarrollo
        ├── especificacion_ui.md  # Reglas de interfaz, flujo y servicios.
        └── prompt_compilador.md  # Reglas de generación de código.
```
