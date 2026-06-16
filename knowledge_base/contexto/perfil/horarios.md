---
id: "PER-02"
nombre: "Horarios y Ventanas de Actividad"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["NUT-02", "LOG-04"]
tags: ["perfil", "horarios", "cronotipo", "calendario"]
---

# Horarios y Ventanas de Actividad

## 1. Alcance
Definir los bloques de tiempo libre y el cronotipo para posicionar los entrenamientos y las comidas.

## 2. Variables del Sistema
* [CRONOTIPO]: [Ej: Matutino / Vespertino]
* [HORA_DESPERTAR]: [Rellenar]
* [HORA_DORMIR]: [Rellenar]
* [VENTANA_ENTRENO]: [Ej: 18:00 - 20:00]

## 3. Lógica y Reglas
1. La app no debe lanzar notificaciones de entreno fuera de la [VENTANA_ENTRENO].