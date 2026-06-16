---
id: "LOG-01"
nombre: "Motor Algorítmico de Cargas"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["MET-01", "MET-02", "GIM-02"]
tags: ["logica", "algoritmo", "backend", "autorregulacion"]
---

# Motor Algorítmico de Cargas

## 1. Alcance
Cerebro que ajusta los pesos de la rutina basándose en fatiga y sueño.

## 2. Variables del Sistema
* [FACTOR_FATIGA_EXTREMA]: 0.90
* [FACTOR_FATIGA_MODERADA]: 0.95
* [UMBRAL_SUEÑO_CRITICO]: 60 (Sleep Score)

## 3. Lógica y Reglas
1. Si `MET-01[ZEPP_SLEEP_SCORE] < UMBRAL_SUEÑO_CRITICO`, aplicar `FACTOR_FATIGA_MODERADA` a los levantamientos del día.