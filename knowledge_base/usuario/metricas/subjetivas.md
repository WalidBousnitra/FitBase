---
id: "USR-MET-02"
nombre: "Métricas Subjetivas"
fecha_modificacion: "17/06/2026"
estado: "ACTIVO"
relacionados: ["USR-MET-01", "REG-LOG-01"]
tags: ["metricas", "subjetivas", "rpe", "fatiga"]
---

# Métricas Subjetivas

## 1. Propósito

Capturar lo que el hardware **NO puede medir**: sensaciones, percepciones y estado mental.

| El reloj dice... | Tú sientes... | Métrica subjetiva |
|------------------|---------------|-------------------|
| "Dormiste 8h" | "Pero descansé fatal" | `SUB_SUENO` |
| "FC en reposo normal" | "Estoy reventado" | `SUB_ENERGIA` |
| — | "Tengo agujetas brutales" | `SUB_DOMS` |
| — | "Esa serie fue al fallo" | `SUB_RPE` / `SUB_RIR` |
| — | "Hoy el curro me tiene frito" | `SUB_ESTRES` |

---

## 2. Métricas Pre-Entreno

> Se capturan **ANTES** de empezar la sesión.

| Métrica | Escala | Variable | Qué mide |
|---------|--------|----------|----------|
| Energía percibida | 1-10 | `SUB_ENERGIA` | ¿Cómo te sientes de activo? |
| Calidad sueño percibida | 1-10 | `SUB_SUENO` | ¿Descansaste bien? (independiente de horas) |
| Estrés mental | 1-10 | `SUB_ESTRES` | Carga mental del día (trabajo, vida) |
| Dolor muscular (DOMS) | 0-10 | `SUB_DOMS` | Agujetas / dolor residual |
| Motivación | 1-10 | `SUB_MOTIVACION` | Ganas de entrenar |
| Hambre | 1-10 | `SUB_HAMBRE` | ¿Has comido bien antes? |

---

## 3. Métricas Intra-Entreno

> Se capturan **DURANTE** la sesión (por serie o ejercicio).

| Métrica | Escala | Variable | Qué mide |
|---------|--------|----------|----------|
| RPE (esfuerzo percibido) | 1-10 | `SUB_RPE` | Intensidad subjetiva de la serie |
| RIR (reps en reserva) | 0-5 | `SUB_RIR` | Cuántas reps te quedaban |
| Pump | 1-10 | `SUB_PUMP` | Congestión muscular |
| Conexión mente-músculo | 1-10 | `SUB_MMC` | ¿Sentiste el músculo trabajar? |
| Técnica percibida | 1-10 | `SUB_TECNICA` | ¿Ejecutaste bien? |

### Relación RPE ↔ RIR
| RPE | RIR | Descripción |
|-----|-----|-------------|
| 10 | 0 | Fallo muscular |
| 9 | 1 | Quedaba 1 rep |
| 8 | 2 | Quedaban 2 reps |
| 7 | 3 | Quedaban 3 reps |
| 6 | 4+ | Calentamiento / técnica |

---

## 4. Métricas Post-Entreno

> Se capturan **DESPUÉS** de terminar la sesión.

| Métrica | Escala | Variable | Qué mide |
|---------|--------|----------|----------|
| Satisfacción sesión | 1-10 | `SUB_SATISFACCION` | ¿Buen entreno? |
| Fatiga acumulada | 1-10 | `SUB_FATIGA_POST` | ¿Cómo quedaste? |
| Dolor articular | 0-10 | `SUB_DOLOR_ARTICULAR` | Molestias en articulaciones |

---

## 5. Métricas de Nutrición

> Se capturan diariamente o por comida.

| Métrica | Escala | Variable | Qué mide |
|---------|--------|----------|----------|
| Adherencia dieta | 1-10 | `SUB_ADHERENCIA` | ¿Seguiste el plan? |
| Antojos | 0-10 | `SUB_ANTOJOS` | Ganas de comer fuera del plan |
| Saciedad | 1-10 | `SUB_SACIEDAD` | ¿Te quedas satisfecho? |
| Digestión | 1-10 | `SUB_DIGESTION` | ¿Sientes pesadez, gases, etc.? |

---

## 6. Umbrales y Reglas

> ⏳ **Pendiente**: Los umbrales específicos para tomar acciones deben definirse en `evidencia/` con papers.
> 
> Ejemplo pendiente: ¿A partir de qué RPE se considera "demasiado"? ¿Qué nivel de DOMS justifica evitar un músculo?

---

## 7. Uso en el Sistema

```
┌─────────────────┐
│  UI: Captura    │ → Usuario introduce 1-10 antes/durante/después
└────────┬────────┘
         ↓
┌─────────────────┐
│  motor_pesos.md │ → Combina SUB_* + ZEPP_* para autorregular
└────────┬────────┘
         ↓
┌─────────────────┐
│  Decisión       │ → Ajusta cargas, sugiere descanso, etc.
└─────────────────┘
```

1. La **UI** captura estos inputs con sliders/botones
2. El **motor_pesos.md** combina métricas objetivas (hardware) + subjetivas
3. Las **reglas** (pendientes de evidencia) determinan las acciones
