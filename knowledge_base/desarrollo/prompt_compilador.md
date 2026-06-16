---
id: "DEV-02"
nombre: "Meta-Prompt y Normas de Compilación"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["SYS-00"]
tags: ["desarrollo", "prompt", "codigo", "estilo"]
---

# Meta-Prompt y Normas de Compilación

## 1. Alcance
Guía de estilo de código estricta para la generación de Kotlin y Google Apps Script.

## 2. Variables del Sistema
* [LENGUAJE_FRONT]: Kotlin (Jetpack Compose)
* [LENGUAJE_BACK]: JavaScript (Google Apps Script V8)

## 3. Lógica y Reglas
1. Todo código de Apps Script debe modularizarse en funciones independientes y usar `try-catch`.
2. Las variables en Kotlin deben escribirse en `camelCase`, coincidiendo conceptualmente con los nombres en `base_datos.md`.