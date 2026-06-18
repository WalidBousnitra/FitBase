---
id: "EVI-06"
nombre: "Evidencia: Salud Hormonal"
fecha_modificacion: "17/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-05", "EVI-07"]
tags: ["evidencia", "hormonas", "testosterona", "GH", "cortisol", "IGF-1"]
prioridad: 6
---

# Salud Hormonal

> **Prioridad #6** — Alineado con [prioridades.md](../usuario/prioridades.md)

## 1. Alcance
Evidencia científica sobre optimización hormonal natural a través de entrenamiento, nutrición y estilo de vida.

---

## 2. RESPUESTAS HORMONALES AL ENTRENAMIENTO

### Fuente Principal
**Kraemer, W.J. & Ratamess, N.A. (2005)**  
"Hormonal Responses and Adaptations to Resistance Exercise and Training"  
*Sports Medicine, 35(4), 339-361*

---

## 3. TESTOSTERONA

### Respuesta Aguda al Entrenamiento

| Factor | Efecto en Testosterona |
|--------|------------------------|
| Masa muscular ejercitada | **MAYOR** → mayor ↑ T |
| Ejercicios compuestos | **Óptimo** (sentadilla, peso muerto, clean) |
| Volumen alto | ↑ Mayor respuesta |
| Intensidad moderada-alta | ↑ Mayor respuesta |
| Descansos cortos (1-2 min) | ↑ Mayor respuesta |

### Protocolo Óptimo para ↑ Testosterona

```yaml
PROTOCOLO_TESTOSTERONA:
  ejercicios: compuestos_grandes_musculos
  series: 3-6
  repeticiones: 6-12
  descanso: 1-2 min
  volumen: moderado-alto
```

### Estudios de Respuesta

| Estudio | Protocolo | Resultado |
|---------|-----------|-----------|
| Weiss et al. | 3x4 ejercicios, 80% 1RM, 2min | ↑ Significativo T |
| Ratamess et al. | 6x10 sentadilla vs 1x10 | 6x10 > 1x10 en ↑T |
| Gotshalk et al. | 3 sets vs 1 set | 3 sets > 1 set |

### Testosterona Libre
- Paralela a testosterona total en algunos estudios
- Entrenados muestran MAYOR respuesta aguda que no entrenados
- 10 semanas de entrenamiento ↑ respuesta aguda

---

## 4. HORMONA DE CRECIMIENTO (GH)

### Respuesta Aguda

| Factor | Efecto en GH |
|--------|--------------|
| Volumen alto | ↑ Mayor respuesta |
| Intensidad moderada | ↑ Mayor respuesta |
| Descansos cortos | ↑ **MAYOR** respuesta |
| Lactato alto | ↑ Mayor respuesta |

### Protocolo Óptimo para ↑ GH

```yaml
PROTOCOLO_GH:
  series: 3-4
  repeticiones: 10-12
  descanso: 60-90 seg  # CLAVE: descansos cortos
  intensidad: 70-85% 1RM
  tipo: metabolico_alto_lactato
```

> **Dato clave**: GH puede elevarse 15-30 minutos post-ejercicio con el estímulo adecuado.

---

## 5. CORTISOL

### Respuesta Aguda

| Factor | Efecto en Cortisol |
|--------|-------------------|
| Volumen alto | ↑ Mayor elevación |
| Intensidad alta | ↑ Mayor elevación |
| Descansos cortos | ↑ Mayor elevación |
| Estrés metabólico | ↑ Mayor elevación |

### Adaptaciones Crónicas

| Adaptación | Efecto |
|------------|--------|
| Ratio T/C (testosterona/cortisol) | Mejora con entrenamiento |
| Cortisol basal | Puede ↓ con entrenamiento |
| Receptores glucocorticoides | Se adaptan |

### Ratio Testosterona/Cortisol

> **Indicador de estado anabólico vs catabólico**

| Estado | Ratio T/C |
|--------|-----------|
| Óptimo | Alto |
| Sobreentrenamiento | Bajo |
| Recuperación insuficiente | Bajo |

---

## 6. IGF-1 (Factor de Crecimiento Similar a Insulina)

### Hallazgos

| Aspecto | Efecto |
|---------|--------|
| Respuesta aguda | ↑ Elevado post-ejercicio |
| MGF (Mechano Growth Factor) | ↑ Por estímulo mecánico |
| Hipertrofia local | Mediada por IGF-1 muscular |

> **Dato**: El estiramiento/tensión mecánica del músculo ↑ expresión genética de IGF-1 → ↑ síntesis proteica.

---

## 7. VARIABLES DE ENTRENAMIENTO Y HORMONAS

### Resumen de Efectos

| Variable | T | GH | Cortisol |
|----------|---|----|----|
| Ejercicios grandes | ↑↑ | ↑↑ | ↑ |
| Volumen alto | ↑ | ↑↑ | ↑↑ |
| Intensidad alta | ↑ | ↑ | ↑↑ |
| Descansos cortos | ↑ | ↑↑ | ↑↑ |
| Descansos largos | ↓ | ↓ | ↓ |

### Periodización Hormonal

```yaml
FASE_HIPERTROFIA:
  objetivo: maximizar_GH_y_T
  volumen: alto
  intensidad: moderada (70-85%)
  descanso: 60-120 seg
  
FASE_FUERZA:
  objetivo: maximizar_T
  volumen: moderado
  intensidad: alta (85-95%)
  descanso: 2-5 min
  
DELOAD:
  objetivo: normalizar_cortisol
  volumen: reducido_50%
  frecuencia: cada_4-6_semanas
```

---

## 8. FACTORES NO-ENTRENAMIENTO

### Sueño
- GH se libera principalmente durante sueño profundo
- Déficit de sueño ↓ testosterona

### Nutrición
- Grasas dietarias → precursores hormonales
- Deficiencia calórica severa ↓ T
- Proteína suficiente mantiene anabolismo

### Circadiano
- T más alta por la mañana
- Cortisol pico al despertar
- Considerar timing de entrenamiento

---

## 9. RECOMENDACIONES PARA SISTEMA

```yaml
OPTIMIZACION_HORMONAL:
  entrenamiento:
    - ejercicios_compuestos_primero
    - volumen_moderado_alto
    - descansos_60-120s_para_GH
    - descansos_2-3min_para_fuerza
    
  nutricion:
    - grasas_20-30%_calorias
    - proteina_suficiente
    - evitar_deficit_severo
    
  recuperacion:
    - sueno_7-9h
    - deload_cada_4-6_semanas
    
  señales_alarma:
    - fatiga_cronica
    - perdida_fuerza
    - ansiedad_elevada
    - libido_reducida
```

---

## 10. RESUMEN EJECUTIVO

| Hormona | Cómo Optimizar |
|---------|----------------|
| **Testosterona** | Ejercicios grandes, volumen moderado-alto, descansos 1-2min |
| **GH** | Volumen alto, descansos cortos (60-90s), alto lactato |
| **IGF-1** | Tensión mecánica, ROM completo |
| **Cortisol** | Controlar con periodización y deloads |

> **Mensaje clave**: La respuesta aguda hormonal es MÁS importante que los niveles basales para hipertrofia.

## 4. Síntesis para el Sistema
*Completar tras revisar papers*

## 5. Implicaciones para Reglas
*Macros específicos, timing nutricional, tipo de entrenamiento*
