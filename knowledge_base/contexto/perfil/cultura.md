---
id: "PER-03"
nombre: "Cultura y Adaptación Gastronómica"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["NUT-01", "LOG-04"]
tags: ["perfil", "cultura", "gastronomia", "entorno"]
---

# Cultura y Adaptación Gastronómica

## 1. Alcance
Particularidades culturales que alteran el entorno dietético y logístico.

## 2. Variables del Sistema
* [PAIS_RESIDENCIA]: España
* [INFLUENCIA_GASTRONOMICA]: Marruecos
* [RESTRICCION_LOGISTICA]: Carne Halal

## 3. Lógica y Reglas
1. Adaptar recetas a perfiles de especias marroquíes controlando el exceso de grasas no cuantificadas.
2. Si `[RAMADAN_ACTIVO] == true`, la ventana de alimentación colapsa al horario nocturno (Iftar-Suhur).