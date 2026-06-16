---
id: "MET-02"
nombre: "Métricas Subjetivas del Usuario"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["LOG-01"]
tags: ["metricas", "subjetivas", "estres", "fatiga"]
---

# Métricas Subjetivas del Usuario

## 1. Alcance
Formulario de entrada rápida de sensaciones físicas y mentales pre-entreno.

## 2. Variables del Sistema
* [NIVEL_ESTRES_LABORAL]: [Rango 1-5]
* [ENERGIA_PRE_ENTRENO]: [Rango 1-5]
* [DOLOR_ARTICULAR]: [Booleano/Zonas]

## 3. Lógica y Reglas
1. Si [DOLOR_ARTICULAR] == true en zona planificada, el sistema sugerirá un ejercicio alternativo del mismo patrón de movimiento.