---
id: "NUT-01"
nombre: "Preferencias y Logística de Cocina"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["PER-03", "LOG-02"]
tags: ["nutricion", "alimentos", "cocina", "intolerancias"]
---

# Preferencias y Logística de Cocina

## 1. Alcance
Listado de alimentos permitidos, vetados y herramientas de cocción disponibles.

## 2. Variables del Sistema
* [ALIMENTOS_TOP]: [Ej: Pollo, Cuscús, Avena...]
* [ALIMENTOS_ODIADOS]: [Ej: Brócoli...]
* [EQUIPAMIENTO]: [Ej: Airfryer, Microondas, Horno]

## 3. Lógica y Reglas
1. El generador de dietas excluirá estrictamente el array de [ALIMENTOS_ODIADOS].