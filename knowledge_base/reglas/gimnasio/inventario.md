---
id: "GIM-01"
nombre: "Inventario y Recursos Físicos"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["GIM-02"]
tags: ["gimnasio", "inventario", "maquinas", "equipamiento"]
---

# Inventario y Recursos Físicos

## 1. Alcance
Mapeo de la maquinaria exacta disponible en el gimnasio habitual y los incrementos de peso posibles.

## 2. Variables del Sistema
* [SALTOS_MANCUERNAS]: [Ej: 2kg, 2.5kg]
* [POLEAS_DISPONIBLES]: [Ej: Simple, Doble, Regulable]
* [MAQUINAS_ESPECIFICAS]: [Ej: Hack Squat, Prensa Inclinada]

## 3. Lógica y Reglas
1. La progresión matemática redondeará siempre al múltiplo más cercano de [SALTOS_MANCUERNAS].