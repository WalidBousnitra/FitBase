---
id: "NUT-02"
nombre: "Objetivos y Macros Base"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["LOG-02", "MET-03"]
tags: ["nutricion", "macros", "fase", "calorias"]
---

# Objetivos y Macros Base

## 1. Alcance
Definir la fase actual (volumen/definición) y los requerimientos calóricos estáticos.

## 2. Variables del Sistema
* [FASE_ACTUAL]: [Ej: Definición / Mantenimiento / Volumen]
* [CALORIAS_BASE]: [Rellenar] kcal
* [PROTEINA_G_KG]: [Rellenar] (Ej: 2.2g)
* [META_PASOS_DIARIOS]: [Rellenar]

## 3. Lógica y Reglas
1. Las [CALORIAS_BASE] son el punto de partida antes de que `motor_dieta.md` aplique los ajustes por NEAT.