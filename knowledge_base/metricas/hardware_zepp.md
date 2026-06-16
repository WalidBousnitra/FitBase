---
id: "MET-01"
nombre: "Datos de Hardware: Amazfit Zepp"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["LOG-01"]
tags: ["metricas", "hardware", "zepp", "sueño"]
---

# Datos de Hardware: Amazfit Zepp

## 1. Alcance
Estructura de las métricas obtenidas del reloj inteligente (GTS 4).

## 2. Variables del Sistema
* [ZEPP_SLEEP_SCORE]: [0-100]
* [ZEPP_DEEP_SLEEP_MINS]: [Minutos]
* [ZEPP_RESTING_HR]: [Pulsaciones en reposo]

## 3. Lógica y Reglas
1. Estas variables alimentarán directamente el algoritmo de fatiga en `motor_pesos.md`.