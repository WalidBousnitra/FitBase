---
id: "LOG-04"
nombre: "Excepciones y Contingencias"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["PER-03", "LOG-01", "LOG-02"]
tags: ["logica", "contingencia", "viajes", "ramadan"]
---

# Excepciones y Contingencias

## 1. Alcance
Protocolos de emergencia cuando el usuario sale de su rutina o entorno habitual.

## 2. Variables del Sistema
* [MODO_VIAJE]: [Booleano]
* [MODO_ENFERMEDAD]: [Booleano]

## 3. Lógica y Reglas
1. Si [MODO_VIAJE] == true, congelar progresiones en `motor_pesos.md` y cambiar a rutina de mantenimiento bodyweight o gimnasio de hotel.