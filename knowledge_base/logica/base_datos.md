---
id: "LOG-03"
nombre: "Base de Datos y Esquema"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["all"]
tags: ["logica", "base-datos", "sheets", "schema"]
---

# Base de Datos y Esquema

## 1. Alcance
Nomenclatura y estructura estricta de las columnas en Google Sheets.

## 2. Variables del Sistema
* [HOJA_ENTRENAMIENTOS]: Columnas [Fecha, Ejercicio, Peso, Reps, RIR]
* [HOJA_METRICAS]: Columnas [Fecha, Peso_Corporal, Sleep_Score, Pasos]

## 3. Lógica y Reglas
1. Toda lectura/escritura del Backend (Apps Script) debe apuntar exactamente a estos nombres de columna.