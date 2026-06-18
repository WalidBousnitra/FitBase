---
id: "USR-01"
nombre: "Prioridades Globales"
fecha_modificacion: "18/06/2026"
estado: "PROD_ACTUAL"
relacionados: ["SYS-00", "USR-02"]
tags: ["prioridades", "estrategia", "core", "decisiones"]
---

# Prioridades Globales

## 1. Alcance
Este documento define el **orden de importancia** para TODAS las decisiones del sistema. Los detalles de implementación (volúmenes, ejercicios, protocolos) se derivan de los archivos de `evidencia/` una vez completados con papers científicos.

> ⚠️ **REGLA FUNDAMENTAL**: Cuando dos objetivos entren en conflicto, gana el de número más bajo.

---

## 2. Ranking de Prioridades

| # | Prioridad | Subáreas | Evidencia |
|---|-----------|----------|-----------|
| 1 | **Estética muscular** | Proporciones, simetría, V-taper | [estetica.md](../evidencia/estetica.md) |
| 2 | **Postura** | Hipercifosis, hiperlordosis, hombros rotados, caja torácica, cuello, cadera | [postura.md](../evidencia/postura.md) |
| 3 | **Hipertrofia** | Hombros > Bíceps > Espalda > Tríceps > Pecho > Abdominales | [hipertrofia.md](../evidencia/hipertrofia.md) |
| 4 | **Flexibilidad** | Tríceps (overhead), ingle (aductores) | [flexibilidad.md](../evidencia/flexibilidad.md) |
| 5 | **Estrés** | Cortisol, recuperación | [estres.md](../evidencia/estres.md) |
| 6 | **Salud hormonal** | Testosterona, tiroides | [hormonal.md](../evidencia/hormonal.md) |
| 7 | **Vitalidad** | Energía diaria | [vitalidad.md](../evidencia/vitalidad.md) |
| 8 | **Salud digestiva** | Microbioma, absorción | [digestivo.md](../evidencia/digestivo.md) |
| 9 | **Agilidad** | Coordinación, velocidad | [agilidad.md](../evidencia/agilidad.md) |
| 10 | **Cardio** | Capacidad aeróbica (baja prioridad) | [cardio.md](../evidencia/cardio.md) |

---

## 3. Detalle de Subáreas (Definidas por Usuario)

### #1 — Estética Muscular
- Objetivo: V-taper (hombros anchos, cintura estrecha)
- Métricas: Ratio cintura/hombros, simetría bilateral, grasa corporal

### #2 — Postura
Problemas a corregir:
- Hipercifosis dorsal (joroba)
- Hiperlordosis lumbar
- Hombros rotados internamente
- Caja torácica colapsada
- Cabeza adelantada
- Inclinación pélvica anterior

**🎯 Objetivo concreto: Hacer un WALL ANGEL perfecto**
> Actualmente NO puede hacer wall angels en ninguna variante. Este es un test/objetivo tangible de progreso postural.

> ✅ **Completado**: Ver [postura.md](../evidencia/postura.md) - Protocolo Ruivo 16 semanas, estrés cervical Hansraj.

### #3 — Hipertrofia
Orden de prioridad de grupos musculares:
1. Hombros (clave para V-taper)
2. Bíceps (estética brazos)
3. Espalda (V-taper + postura)
4. Tríceps
5. Pecho (no priorizar sobre hombros)
6. Abdominales
7. Piernas (balance, no prioridad)

> ✅ **Completado**: Ver [hipertrofia.md](../evidencia/hipertrofia.md) - 10+ series/semana, 2x frecuencia, RIR 2-3, descanso 2-3 min.

### #4 — Flexibilidad
Zonas prioritarias:
- Tríceps: Extensión overhead limitada
- Ingle: Aductores y flexores de cadera

> ✅ **Completado**: Ver [flexibilidad.md](../evidencia/flexibilidad.md) - 30s x 2-4 reps, 2-3x/semana, dinámico pre-entreno.

### #5 — Estrés
- Enfoque: Control de cortisol
- Métricas proxy: FC reposo, HRV, Sleep Score

> ✅ **Completado**: Ver [estres.md](../evidencia/estres.md) - Ejercicio reduce cortisol, ratio T/C, entrenamiento vs estrés.

### #6-10 — Prioridades Secundarias
| # | Prioridad | Enfoque |
|---|-----------|---------|
| 6 | Salud hormonal | Optimización natural |
| 7 | Vitalidad | Energía percibida |
| 8 | Salud digestiva | Absorción nutrientes |
| 9 | Agilidad | Coordinación |
| 10 | Cardio | Capacidad aeróbica básica |

---

## 4. Resolución de Conflictos

| Conflicto | Resolución | Justificación |
|-----------|------------|---------------|
| Cardio vs Hipertrofia | Priorizar hipertrofia | P3 > P10 |
| Pecho vs Hombros | Más volumen a hombros | P3 (hombros > pecho) |
| Piernas vs Upper body | Mantener piernas pero priorizar upper | P1 (V-taper) |
| Volumen alto vs Estrés | Reducir si métricas de estrés elevadas | P5 modula P3 |

> ✅ **Completado**: Ver evidencia para detalles (cardio: Wilson 2012, volumen: Schoenfeld 2017).

---

## 5. Uso en el Sistema

1. **Programación**: Asigna recursos según este ranking
2. **Evidencia**: Cada prioridad tiene su archivo 1:1 en `/evidencia/` para completar con papers
3. **Reglas**: Los archivos de `/reglas/` implementan la lógica basada en evidencia

---

## 6. Revisión

> Las prioridades pueden cambiar. Revisar cada 3-6 meses.

| Situación | Posible Cambio |
|-----------|----------------|
| Lesión activa | Subir "recuperación" temporalmente |
| Ramadán | Bajar volumen, subir P5 (estrés) |
| Mucho estrés laboral | Subir P5, bajar volumen |
