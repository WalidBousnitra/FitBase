---
id: "DEV-01"
nombre: "Especificación de UI y Frontend"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["GIM-03", "LOG-03"]
tags: ["desarrollo", "frontend", "ui", "android", "java"]
---

# Especificación de UI y Frontend

## 1. Alcance
Reglas de diseño de interfaces, navegabilidad y experiencia de usuario en la app de Android.

## 2. Variables del Sistema
* [TEMA_APP]: Dark Mode obligatorio
* [INPUT_MÉTODO]: Incrementos por botones rápidos (+2.5, -2.5) en lugar de teclado numérico para facilitar uso bajo fatiga.

## 3. Lógica y Reglas
1. La UI principal debe mostrar primero el formulario de entrada de fatiga (`MET-02`) antes de revelar la rutina del día.