# FitBase AI Agent Instructions

## Propósito
Esta guía ayuda a los agentes a trabajar con FitBase de forma inmediata y segura. El repositorio es una base de conocimiento centrada en Markdown y no contiene un proyecto ejecutable tradicional.

## Principios clave
- La única fuente de verdad son los archivos `knowledge_base/**/*.md`.
- No inferir ni inventar reglas, lógica de negocio, pautas nutricionales o protocolos de entrenamiento fuera de lo que dicen los MD.
- No crear archivos ni carpetas fuera de la estructura existente sin autorización explícita.
- Cuando haya duda, usar `knowledge_base/manifest.md` como autoridad principal.

## Estructura principal
- `knowledge_base/manifest.md` — autoridad del dominio y mapa maestro de la estructura.
- `knowledge_base/contexto/` — datos del usuario, horarios, cultura, métricas y contexto que no modifican reglas directas.
- `knowledge_base/evidencia/` — hallazgos científicos, papers y evidencia de apoyo.
  - No todas las entradas están en carpetas; muchas son MD directos dentro de `evidencia/`.
  - Carpetas preservadas: `movilidad/`, `nutricion/`, `postura/`, `hipertrofia/`.
- `knowledge_base/reglas/` — reglas de negocio, lógica y especificaciones que afectan decisiones.
- `knowledge_base/plantilla.md` / `knowledge_base/plantilla reglas.txt` — plantillas de documentación.

## Arquitectura
- Frontend: Android nativo (Kotlin / Jetpack Compose).
- Backend: Google Apps Script (JavaScript V8).
- Persistencia: Google Sheets como base de datos relacional tabular.
- Ingesta de métricas: Health Connect (Android) y mapeo de Zepp (Amazfit GTS 4).

## Documentos esenciales
- `knowledge_base/manifest.md` — arquitectura, roles y mapa de dominios.
- `knowledge_base/desarrollo/prompt_compilador.md` — normas de generación de código y estilo.
- `knowledge_base/reglas/logica/*.md` — motores de dieta, pesos, base de datos y excepciones.
- `knowledge_base/reglas/gimnasio/*.md` — reglas de programación de entrenamiento.
- `knowledge_base/evidencia/*.md` — evidencia científica aplicable a decisiones de entrenamiento y nutrición.

## Comportamiento esperado
- Priorizar siempre la estructura y el contenido de `knowledge_base/`.
- Generar respuestas y código en español usando la terminología del repositorio.
- Informar al usuario si no existen archivos de compilación/bibliotecas ejecutables ni configuraciones de build.
- Para código: usar Kotlin para frontend y Apps Script para backend, según `prompt_compilador.md`.
- Para lógica: separar claramente el contexto de usuario (`contexto/`) de la evidencia (`evidencia/`) y las reglas (`reglas/`).

## Notas prácticas
- `README.md` no define un proyecto instalable.
- No hay `package.json`, `build.gradle`, `pyproject.toml`, ni otro fichero de build estándar en el árbol actual.
- `knowledge_base/manifest.md` debe mantenerse sincronizado con el árbol real.
- Si propones nuevas categorías de `evidencia/`, confirma primero con el usuario antes de crear carpetas.

## Mejora continua
- Esta guía se actualiza junto con `knowledge_base/manifest.md`.
- Si la estructura cambia, primero ajusta el manifiesto y luego las instrucciones de agente.
