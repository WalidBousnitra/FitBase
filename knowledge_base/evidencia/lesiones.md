---
id: "EVI-15"
nombre: "Evidencia: Lesiones y Dolor"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-04", "REG-ENT-01"]
tags: ["evidencia", "lesiones", "dolor", "rehabilitacion", "cronico"]
prioridad: "soporte"
---

# Lesiones y Dolor

> **Soporte** — Base científica para manejo del dolor durante el entrenamiento

## 1. Alcance
Evidencia científica sobre ejercicio con dolor, rehabilitación de lesiones musculoesqueléticas y manejo del dolor crónico.

---

## 2. FUENTES PRINCIPALES

- **Smith, B.E. et al. (2017)** - "Should exercises be painful in the management of chronic musculoskeletal pain? A systematic review and meta-analysis" - British Journal of Sports Medicine
- 9 papers incluidos, 7 RCTs, n=385 participantes

---

## 3. HALLAZGO PRINCIPAL

### Ejercicios con Dolor vs Sin Dolor

| Plazo | Resultado | Effect Size | Calidad |
|-------|-----------|-------------|---------|
| **Corto plazo (≤3 meses)** | Dolor con dolor = MEJOR | -0.27 (pequeño) | Moderada |
| **Medio plazo (3-12 meses)** | Sin diferencia | — | — |
| **Largo plazo (≥12 meses)** | Sin diferencia | — | — |
| **Función/Discapacidad** | Sin diferencia | — | Todos los plazos |

> **CONCLUSIÓN**: Ejercicios con dolor ofrecen **pequeño beneficio a corto plazo** para dolor crónico. A medio y largo plazo, no hay diferencia.

---

## 4. CONCEPTO "HURT ≠ HARM"

### Principio Fundamental

> **El dolor durante el ejercicio NO indica daño tisular.**

| Creencia Común | Realidad (Evidencia) |
|----------------|---------------------|
| "Si duele, estoy dañando algo" | El dolor no correlaciona con daño tisular |
| "Hay que evitar todo dolor" | El dolor no es barrera para buenos resultados |
| "Solo ejercicios sin dolor" | Ejercicios con dolor pueden ser beneficiosos |

### Factores Psicológicos en Dolor Crónico

| Factor | Efecto |
|--------|--------|
| Catastrofización | Amplifica percepción del dolor |
| Miedo a la evitación | Limita movimiento y recuperación |
| Kinesiofobia | Miedo irracional al movimiento |
| Autoeficacia | Predictor positivo de resultados |

```yaml
PARADIGMA_MODERNO:
  dolor_cronico:
    no_es: indicador_de_daño
    es: experiencia_multifactorial
    factores: [biologicos, psicologicos, sociales]
    
  ejercicio_con_dolor:
    permitido: si (dolor_manejable)
    beneficio: corto_plazo
    sin_daño: correcto
```

---

## 5. APLICACIÓN PRÁCTICA

### Escala de Dolor Durante Ejercicio

| Puntuación | Descripción | Acción |
|------------|-------------|--------|
| 0-2 | Mínimo o nada | ✅ Continuar normalmente |
| 3-4 | Dolor leve, tolerable | ✅ Continuar, monitorear |
| 5-6 | Dolor moderado | ⚠️ Modificar o reducir carga |
| 7-10 | Dolor severo | ❌ Detener, evaluar |

### Dolor Aceptable Durante Ejercicio

```yaml
CRITERIOS_DOLOR_ACEPTABLE:
  durante_ejercicio:
    - puntuacion: "≤5/10"
    - descripcion: "molestia tolerable"
    - patron: "no_aumenta_progresivamente"
    
  despues_ejercicio:
    - duracion_maxima: "24-48 horas"
    - no_peor_que: "antes_de_empezar"
    
  señales_de_alarma:
    - dolor_que_aumenta_cada_sesion
    - dolor_nocturno_que_despierta
    - inflamacion_significativa
    - perdida_de_funcion
```

### Criterios para Continuar vs Detener

| Continuar | Detener |
|-----------|---------|
| Dolor ≤5/10 | Dolor >7/10 |
| Mejora con calentamiento | Empeora durante ejercicio |
| Se resuelve en 24-48h | Persiste >48h |
| No afecta la vida diaria | Interfiere con sueño/trabajo |

---

## 6. PROTOCOLO DE EJERCICIO CON DOLOR CRÓNICO

### Fase 1: Evaluación

```yaml
EVALUACION_INICIAL:
  historia:
    - duracion_dolor: "> 3 meses = cronico"
    - patron_dolor: "mecanico vs inflamatorio"
    - factores_aliviadores: identificar
    - factores_agravantes: identificar
    
  banderas_rojas:
    - perdida_peso_inexplicable
    - fiebre
    - trauma_reciente
    - deficit_neurologico
    - historial_cancer
```

### Fase 2: Carga Progresiva

| Semana | Enfoque | Dolor Permitido |
|--------|---------|-----------------|
| 1-2 | Movimiento activo | 2-3/10 |
| 3-4 | Carga ligera | 3-4/10 |
| 5-6 | Carga moderada | 4-5/10 |
| 7+ | Carga funcional | Según tolerancia |

### Fase 3: Progresión

```yaml
PROGRESION_SEGURA:
  criterios_avance:
    - dolor_post_ejercicio: "resuelto en 24h"
    - funcion: "igual o mejor"
    - confianza: "aumentando"
    
  si_empeora:
    - reducir_carga: 20-30%
    - mantener_movimiento: si
    - reevaluar: 1_semana
```

---

## 7. TIPOS DE DOLOR

### Diferenciación Importante

| Tipo | Características | Acción |
|------|-----------------|--------|
| **Dolor muscular** (DOMS) | Aparece 24-72h, mejora con movimiento | ✅ Normal, continuar |
| **Dolor articular agudo** | Durante/después ejercicio, localizado | ⚠️ Modificar técnica/carga |
| **Dolor neuropático** | Hormigueo, quemazón, irradiación | ❌ Derivar a especialista |
| **Dolor inflamatorio** | Rigidez matutina, caliente, hinchado | ⚠️ Evaluar, posible reposo |

### DOMS vs Lesión

| DOMS (Normal) | Lesión (Problema) |
|---------------|-------------------|
| Bilateral/simétrico | Unilateral/asimétrico |
| Pico 24-72h, mejora después | No mejora o empeora |
| Difuso en músculo | Puntual, localizado |
| Mejora con movimiento suave | Empeora con movimiento |
| Sin debilidad funcional | Pérdida de fuerza/ROM |

---

## 8. RECOMENDACIONES PARA SISTEMA

```yaml
LOGICA_DOLOR:
  dolor_cronico:
    permitir_ejercicio: true
    hasta_puntuacion: 5/10
    mensaje: "El dolor no indica daño"
    
  dolor_agudo:
    permitir_ejercicio: condicional
    si_mejora_con_calentamiento: continuar
    si_empeora: detener_evaluar
    
  post_lesion:
    fase_1: "movimiento sin carga"
    fase_2: "carga progresiva"
    fase_3: "vuelta a normalidad"
    
  educacion_usuario:
    - "hurt_not_harm"
    - "dolor_cronico ≠ daño_tisular"
    - "evitar_kinesiofobia"
```

### Mensajes para Usuario

| Situación | Mensaje |
|-----------|---------|
| Dolor leve durante ejercicio | "Puedes continuar si el dolor es ≤5/10 y no aumenta" |
| Dolor post-entreno | "Si se resuelve en 24-48h, es normal. Si persiste, reduce carga" |
| Miedo al dolor | "El dolor no siempre indica daño. El movimiento suele ayudar" |
| Dolor crónico | "El ejercicio es parte del tratamiento, no lo evites" |

---

## 9. RESUMEN EJECUTIVO

| Mito | Realidad (Evidencia) |
|------|----------------------|
| "Si duele, para" | Dolor ≤5/10 es aceptable durante ejercicio |
| "El dolor indica daño" | NO correlaciona con daño tisular |
| "Reposo = recuperación" | Movimiento controlado suele ser mejor |
| "Evitar todo dolor" | Ejercicios con dolor = pequeño beneficio a corto plazo |

### Tabla de Decisión Rápida

| Tipo de Dolor | Duración | Intensidad | Acción |
|---------------|----------|------------|--------|
| Muscular (DOMS) | <72h | Cualquiera | Continuar |
| Durante ejercicio | Momentáneo | ≤5/10 | Continuar |
| Durante ejercicio | Persistente | >5/10 | Modificar |
| Post-ejercicio | >48h | Cualquiera | Reducir carga |
| Con banderas rojas | — | — | Derivar médico |

> **Mensaje Final**: El dolor durante el ejercicio terapéutico para condiciones crónicas NO es barrera para buenos resultados. "Hurt not equaling harm."
