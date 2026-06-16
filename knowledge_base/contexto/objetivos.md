---
id: "NUT-02"
nombre: "Contexto Nutricional Personal"
fecha_modificacion: "16/06/2026"
estado: "BORRADOR"
relacionados: ["PER-01", "PER-02", "PER-03", "NUT-01"]
tags: ["contexto", "nutricion", "personal", "preferencias"]
---

# Contexto Nutricional Personal

## 1. Alcance
Información sobre el usuario y su contexto personal: estado actual, métricas, recursos disponibles, hábitos, gustos y limitaciones.

## 2. Estado actual y métricas a medir
* [PESO_ACTUAL]: [Rellenar] kg
* [ALTURA]: [Rellenar] cm
* [GRASA_CORPORAL_ACTUAL]: [Rellenar] %
* [FUERZA_ACTUAL]: [Rellenar] (Ej: press 100 kg, sentadilla 140 kg)
* [RESISTENCIA_ACTUAL]: [Rellenar] (Ej: 5 km, 20 min de remo)
* [ENERGÍA_MEDIA_ACTUAL]: [Rellenar] (Escala 1-5)
* [SUEÑO_PROMEDIO_ACTUAL]: [Rellenar] horas
* [PASOS_PROMEDIO_ACTUAL]: [Rellenar] pasos diarios
* [NIVEL_ESTRES_LABORAL_ACTUAL]: [Rellenar] (Ej: 1-5)

## 3. Objetivos deseados
Los objetivos deben usar la misma estructura de métricas actuales, pero con los valores futuros deseados.
* [PESO_OBJETIVO]: [Rellenar] kg
* [GRASA_CORPORAL_OBJETIVO]: [Rellenar] %
* [FUERZA_OBJETIVO]: [Rellenar] (Ej: press 110 kg, sentadilla 160 kg)
* [RESISTENCIA_OBJETIVO]: [Rellenar] (Ej: 8 km, 30 min de remo)
* [ENERGÍA_MEDIA_OBJETIVO]: [Rellenar] (Escala 1-5)
* [SUEÑO_PROMEDIO_OBJETIVO]: [Rellenar] horas
* [PASOS_PROMEDIO_OBJETIVO]: [Rellenar] pasos diarios
* [NIVEL_ESTRES_LABORAL_OBJETIVO]: [Rellenar] (Ej: 1-5)

## 4. Aspectos que quiero mejorar
* [MEJORAR_OVERNIGHT_RECUPERACIÓN]
* [MEJORAR_CALIDAD_SUEÑO]
* [MEJORAR_ENERGÍA_DIARIA]
* [MEJORAR_COMPOSICIÓN_CORPORAL]
* [MEJORAR_FUERZA]
* [MEJORAR_RESISTENCIA]
* [MEJORAR_MOVILIDAD]

## 4. Recursos y equipamiento disponibles
* [BÁSCULA]: [Ej: Báscula digital, bioimpedancia, ninguna]
* [RELOJ]: [Ej: Amazfit GTS 4]
* [MÓVIL]: [Ej: Android, iOS]
* [GIMNASIO_DISPONIBLE]: [Ej: Gimnasio comercial, gimnasio casero, ninguno]
* [MAQUINARIA_DISPONIBLE]: [Ej: press banca, sentadilla, polea, mancuernas, barras]
* [UTENSILIOS_COCINA]: [Ej: Airfryer, microondas, horno, batidora]

## 5. Gustos y logística personal
* [EJERCICIOS_FAVORITOS]: [Ej: sentadilla, peso muerto, dominadas]
* [DIAS_ENTRENO_PREFERIDOS]: [Ej: L, M, X, V]
* [COMIDAS_FAVORITAS]: [Ej: pollo, cuscús, lentejas]
* [COMIDAS_ODIADAS]: [Ej: brócoli, coliflor]
* [RESTRICCIONES_ALIMENTARIAS]: [Ej: halal, sin gluten, intolerancias]
* [PRESUPUESTO_COMPRA]: [Ej: bajo, medio, alto]
* [TIEMPO_PREPARACIÓN_PERMITIDO]: [Ej: 10-20 min, 20-40 min]

## 6. Horarios y contexto laboral
* [TIPO_TRABAJO]: [Ej: oficina, remoto, físico, conductor]
* [HORARIO_TRABAJO]: [Ej: 09:00-18:00, con desplazamientos]
* [TIEMPO_DESPLAZAMIENTO]: [Ej: 30 min cada trayecto]
* [GASTO_ENERGÍA_LABORAL]: [Ej: alto, medio, bajo]
* [VENTANA_ALIMENTACIÓN_EXTRA]: [Ej: cenas nocturnas, comidas fuera de casa]

## 7. Nota cultural y de alimentación
* [PAIS_RESIDENCIA]: [Ej: España]
* [INFLUENCIA_GASTRONÓMICA]: [Ej: Marruecos]
* [RESTRICCION_CULTURAL]: [Ej: halal]
* [RAMADÁN_ACTIVO]: [Booleano]
* [NOTA_RAMADÁN]: [Rellenar] (Condiciones para Iftar y Suhoor)

## 8. Uso en el sistema
1. Este archivo describe únicamente el contexto personal y no debe contener reglas de cálculo.
2. Las reglas de ajuste calórico, selección de ejercicios y programación se definen en `knowledge_base/reglas/`.
