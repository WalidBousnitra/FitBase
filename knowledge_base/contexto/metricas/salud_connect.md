---
id: "MET-03"
nombre: "Ingesta de Salud Connect (Pasos)"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["LOG-02"]
tags: ["metricas", "api", "health-connect", "pasos", "NEAT"]
---

# Ingesta de Salud Connect (Pasos)

## 1. Alcance
Lectura del Non-Exercise Activity Thermogenesis (NEAT).

## 2. Variables del Sistema
* [TOTAL_PASOS_DIA]: [Entero extraído vía API]
* [MOMENTO_LECTURA]: 22:00h (Cierre de día)

## 3. Lógica y Reglas
1. El backend consumirá [TOTAL_PASOS_DIA] al final del día para cruzarlo con `objetivos.md`.