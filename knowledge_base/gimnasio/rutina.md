---
id: "GIM-02"
nombre: "Rutina Actual y Distribución"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["GIM-01", "GIM-04", "LOG-01"]
tags: ["gimnasio", "rutina", "ejercicios", "fuerza"]
---

# Rutina Actual y Distribución

## 1. Alcance
Los bloques de entrenamiento, divisiones musculares (split) y ejercicios asignados.

## 2. Variables del Sistema
* [SPLIT_ACTUAL]: [Ej: Push/Pull/Legs o Torso/Pierna]
* [FRECUENCIA_SEMANAL]: [Ej: 4 días]
* [EJERCICIOS_ACTIVOS]: [Lista de ejercicios con series/reps]

## 3. Lógica y Reglas
1. Las cargas base de esta rutina serán modificadas diariamente por `motor_pesos.md`.