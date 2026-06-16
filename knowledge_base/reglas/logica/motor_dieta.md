---
id: "LOG-02"
nombre: "Motor Algorítmico de Dieta"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["MET-03", "NUT-02"]
tags: ["logica", "algoritmo", "backend", "dieta", "ajustes"]
---

# Motor Algorítmico de Dieta

## 1. Alcance
Cálculo reactivo de calorías y macros en función de la actividad diaria.

## 2. Variables del Sistema
* [KCAL_POR_1000_PASOS_EXTRA]: [Ej: 40 kcal]
* [DEFICIT_FIJO]: [Ej: 300 kcal]

## 3. Lógica y Reglas
1. Si `MET-03[TOTAL_PASOS_DIA] > NUT-02[META_PASOS_DIARIOS]`, añadir [KCAL_POR_1000_PASOS_EXTRA] al presupuesto del día.