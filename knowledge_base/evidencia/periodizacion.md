---
id: "EVI-14"
nombre: "Evidencia: Periodización"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-03", "REG-ENT-01"]
tags: ["evidencia", "periodizacion", "mesociclo", "microciclo", "deload", "tapering"]
prioridad: "soporte"
---

# Periodización

> **Soporte** — Base científica para estructuración del plan anual

## 1. Alcance
Evidencia científica sobre periodización del entrenamiento, estructura de ciclos y optimización del rendimiento.

---

## 2. FUENTES PRINCIPALES

- **Bompa, T.O. & Buzzichelli, C.A. (2019)** - "Periodization: Theory and Methodology of Training" (6ª edición)
- **Page, P. (2012)** - Estiramientos para rehabilitación y ejercicio

---

## 3. CONCEPTOS FUNDAMENTALES (Bompa)

### Definición de Periodización
> División de un plan anual en fases más pequeñas, junto con la periodización de las habilidades biomotoras.

### Jerarquía de Ciclos

| Ciclo | Duración | Contenido |
|-------|----------|-----------|
| **Plan Anual** | 1 año | Toda la temporada |
| **Macrociclo** | 4-6 semanas | Bloque de entrenamiento |
| **Microciclo** | 1 semana | Unidad básica de planificación |
| **Sesión** | 1 día | Entrenamiento individual |

---

## 4. FASES DEL PLAN ANUAL

### Estructura Básica (Monociclo)

| Fase | Duración | Objetivo | Volumen | Intensidad |
|------|----------|----------|---------|------------|
| **Preparación General** | 4-8 sem | Base física | ALTO | BAJO |
| **Preparación Específica** | 4-8 sem | Transferencia | MEDIO | MEDIO-ALTO |
| **Pre-competitiva** | 2-4 sem | Afinamiento | BAJO | ALTO |
| **Competitiva** | Variable | Rendimiento | BAJO | MUY ALTO |
| **Transición** | 2-4 sem | Recuperación | MUY BAJO | BAJO |

### Curvas de Volumen e Intensidad

```
VOLUMEN:   ████████████████████░░░░░░░░░░░░░░░░░░░░
INTENSIDAD: ░░░░░░░░░░██████████████████████████████
            |--- Prep General ---|-- Prep Espec --|-- Comp --|
```

> **Principio**: Al inicio volumen ALTO, intensidad BAJA. Progresivamente se invierte.

---

## 5. PERIODIZACIÓN DE FUERZA (Bompa)

### Fases Secuenciales

| Fase | Objetivo | Series x Reps | Intensidad | Duración |
|------|----------|---------------|------------|----------|
| **AA (Adaptación Anatómica)** | Base, tendones | 2-3 x 12-15 | 40-60% | 4-6 sem |
| **Hipertrofia** | Masa muscular | 3-5 x 8-12 | 65-80% | 4-8 sem |
| **Fuerza Máxima** | Fuerza | 3-6 x 3-6 | 80-95% | 4-6 sem |
| **Conversión (Potencia)** | Potencia/Resistencia | Variable | 50-80% | 4-6 sem |
| **Mantenimiento** | Mantener ganancias | 2-3 x 6-8 | 70-80% | Competición |

### Aplicación para Hipertrofia Pura

```yaml
FASE_1_AA:
  duracion: 4 semanas
  objetivo: preparar_tendones_ligamentos
  series_reps: 2-3 x 12-15
  RIR: 4-5
  
FASE_2_HIPERTROFIA:
  duracion: 6-8 semanas
  objetivo: masa_muscular
  series_reps: 3-5 x 8-12
  RIR: 2-3
  
FASE_3_FUERZA:
  duracion: 4 semanas
  objetivo: aumentar_fuerza_para_mas_hipertrofia
  series_reps: 4-6 x 4-6
  RIR: 1-2
  
FASE_4_METABOLICO:
  duracion: 4 semanas
  objetivo: bombeo_trabajo_metabolico
  series_reps: 3-4 x 12-15
  RIR: 1-2
  descansos: cortos (60-90s)
```

---

## 6. MICROCICLO (SEMANA TIPO)

### Estructura Semanal

| Día | Intensidad | Tipo | Notas |
|-----|------------|------|-------|
| L | ALTA | Entrenamiento principal | Compuestos pesados |
| M | MEDIA | Entrenamiento secundario | Volumen moderado |
| X | BAJA/OFF | Recuperación activa | Movilidad, cardio suave |
| J | ALTA | Entrenamiento principal | Compuestos pesados |
| V | MEDIA | Entrenamiento secundario | Volumen moderado |
| S | BAJA/OFF | Opcional | Debilidades o descanso |
| D | OFF | Descanso | Recuperación completa |

### Ondulación Semanal

```yaml
ONDULACION_DIARIA:
  dia_pesado: 4-6 reps, alto peso
  dia_moderado: 8-12 reps, peso medio
  dia_ligero: 12-15 reps, bajo peso
  
ONDULACION_SEMANAL:
  semana_1: RIR 4
  semana_2: RIR 3
  semana_3: RIR 2
  semana_4: DELOAD
```

---

## 7. DELOAD (DESCARGA)

### Cuándo Hacer Deload

| Criterio | Frecuencia |
|----------|------------|
| Por tiempo | Cada 4-6 semanas |
| Por síntomas | Fatiga acumulada, estancamiento |
| Por fase | Al final de cada macrociclo |

### Cómo Hacer Deload

| Opción | Reducción | Mejor Para |
|--------|-----------|------------|
| **Reducir Volumen** | -40-50% series | Mayoría de personas |
| **Reducir Intensidad** | -10-15% peso | Articulaciones sensibles |
| **Reducir Ambos** | -30% volumen, -10% peso | Fatiga severa |
| **Descanso Activo** | Solo movilidad/cardio | Fatiga extrema |

```yaml
DELOAD_ESTANDAR:
  duracion: 1 semana
  volumen: 50% de la semana anterior
  intensidad: mantener o reducir 10%
  RIR: 4-5
  objetivo: supercompensacion
```

---

## 8. TAPERING (PICO DE RENDIMIENTO)

### Concepto
> Reducción progresiva del volumen manteniendo intensidad para alcanzar pico de rendimiento.

### Índice de Pico de Rendimiento (Bompa)

| Índice | Nivel de Forma | Aplicación |
|--------|----------------|------------|
| 1 | 100% | Competición principal |
| 2 | 90% | Competición secundaria |
| 3 | 70-80% | Competición menor |
| 4 | 60% | Entrenamiento |
| 5 | 50% | Preparación |

### Protocolo de Tapering

| Semana | Volumen | Intensidad | Notas |
|--------|---------|------------|-------|
| -3 | 80% | 100% | Inicio taper |
| -2 | 60% | 100% | Reducción progresiva |
| -1 | 40% | 95-100% | Semana pre-pico |
| 0 | 20% | 90% | Día del pico |

---

## 9. ESTRUCTURA PARA 1 AÑO (HIPERTROFIA)

### Plan Anual Simplificado

| Mes | Fase | Enfoque | Volumen | Intensidad |
|-----|------|---------|---------|------------|
| **1** | AA | Adaptación | Medio | Bajo |
| **2-3** | Hipertrofia I | Masa | Alto | Medio |
| **4** | Fuerza | Fuerza máxima | Medio | Alto |
| **5-6** | Hipertrofia II | Masa | Alto | Medio |
| **7** | Metabólico | Definición | Alto | Medio-Bajo |
| **8-9** | Hipertrofia III | Masa | Alto | Medio |
| **10** | Fuerza | Fuerza máxima | Medio | Alto |
| **11** | Hipertrofia IV | Masa | Alto | Medio |
| **12** | Transición | Recuperación | Bajo | Bajo |

### Deloads en el Año

```yaml
DELOADS_PROGRAMADOS:
  - semana_4: fin_AA
  - semana_8: mitad_hipertrofia_I
  - semana_12: fin_fase_fuerza
  - semana_16: mitad_hipertrofia_II
  - semana_20: fin_metabolico
  - semana_24: mitad_hipertrofia_III
  - semana_28: mitad_fuerza
  - semana_32: transicion
```

---

## 10. RECOMENDACIONES PARA SISTEMA

```yaml
PERIODIZACION:
  ciclo_basico:
    duracion: 4 semanas
    semanas_1-3: progresion
    semana_4: deload
    
  mesociclo:
    duracion: 4-8 semanas
    objetivo: una_cualidad_principal
    
  plan_anual:
    fases: [AA, hipertrofia, fuerza, hipertrofia, metabolico]
    transicion: 2-4 semanas al final
    
  progresion_volumen:
    semana_1: 100%
    semana_2: 105-110%
    semana_3: 110-115%
    semana_4: 50-60% (deload)
    
  progresion_intensidad:
    semana_1: RIR 3-4
    semana_2: RIR 2-3
    semana_3: RIR 1-2
    semana_4: RIR 4-5 (deload)
```

---

## 11. MODELOS DE PROGRESIÓN (ACSM 2009)

### Fuente
**ACSM Position Stand (2009)**  
"Progression Models in Resistance Training for Healthy Adults"  
*Medicine & Science in Sports & Exercise*

### Recomendaciones por Nivel

| Nivel | Experiencia | Carga (RM) | Progresión |
|-------|-------------|------------|------------|
| **Novato** | Sin experiencia RT | **8-12 RM** | +2-10% cuando completas 1-2 reps extra |
| **Intermedio** | ~6 meses | 1-12 RM periodizado | Énfasis gradual en cargas pesadas |
| **Avanzado** | Años | **1-6 RM** con periodización | Variación sistemática |

### Criterio de Progresión de Carga
> Cuando el individuo puede realizar 1-2 repeticiones MÁS de las deseadas con la carga actual, aumentar **2-10%**.

### Descanso Entre Series (Grado A)
| Objetivo | Descanso |
|----------|----------|
| Fuerza máxima (1-6 RM) | **3-5 minutos** |
| Hipertrofia (8-12 RM) | 1-2 minutos |
| Resistencia muscular (15+ RM) | <1 minuto |

### Secuencia de Ejercicios (Grado C)
1. Grupos grandes antes que pequeños
2. Multiarticulares antes que monoarticulares
3. Alta intensidad antes que baja intensidad

---

## 12. DUP vs PERIODIZACIÓN LINEAL

### Fuente
**Rhea, Ball, Phillips & Burkett (2002)**  
"A Comparison of Linear and Daily Undulating Periodized Programs with Equated Volume and Intensity for Strength"  
*Journal of Strength and Conditioning Research, 16(2):250-255*

### Diseño del Estudio
| Parámetro | Valor |
|-----------|-------|
| Sujetos | 20 hombres entrenados (~5 años experiencia) |
| Duración | 12 semanas |
| Ejercicios | Bench press, Leg press |
| Frecuencia | 3 días/semana |

### Protocolos Comparados
| Modelo | Estructura |
|--------|------------|
| **Lineal (LP)** | Sem 1-4: 8RM → Sem 5-8: 6RM → Sem 9-12: 4RM |
| **DUP** | Lunes: 8RM, Miércoles: 6RM, Viernes: 4RM |

### Resultados
| Medida | LP | DUP | Diferencia |
|--------|-----|-----|------------|
| Bench Press 1RM | +14.4% | **+28.8%** | DUP 2× mejor |
| Leg Press 1RM | +25.7% | **+55.8%** | DUP 2× mejor |

> **Conclusión**: Cambios DIARIOS en estímulo son más efectivos que cambios cada 4 semanas (con volumen e intensidad igualados).

### Aplicación para Sistema
```yaml
DUP_RECOMENDADO:
  dia_1: 3x8 RM (hipertrofia)
  dia_2: 3x6 RM (fuerza-hipertrofia)
  dia_3: 3x4 RM (fuerza)
  
  ventaja: "Mayor adaptación que LP"
  cuando_usar: "Intermedios y avanzados"
```

---

## 13. AUTORREGULACIÓN (APRE)

### Fuente
**Mann, Thyfault, Ivey & Sayers (2010)**  
"The Effect of Autoregulatory Progressive Resistance Exercise vs Linear Periodization on Strength Improvement in College Athletes"  
*Journal of Strength and Conditioning Research, 24(7):1718-1723*

### Concepto
> APRE = Progresión basada en rendimiento DIARIO, no en esquema fijo.

### Diseño del Estudio
| Parámetro | Valor |
|-----------|-------|
| Sujetos | 23 jugadores football NCAA Division I |
| Experiencia | 2.65 ± 0.8 años entrenando |
| Duración | 6 semanas pretemporada |

### Resultados APRE vs LP
| Medida | LP | APRE | p |
|--------|-----|------|---|
| Bench Press 1RM | +20.4 N | **+93.4 N** | 0.02 |
| Squat 1RM (estimado) | +37.2 N | **+192.7 N** | 0.05 |
| Reps @225lb | -0.09 | **+3.17** | 0.02 |

> **APRE fue 3-5× más efectivo que LP** en 6 semanas.

### Protocolo APRE (6RM como ejemplo)
| Set | Reps | Peso |
|-----|------|------|
| 1 | 10 | 50% de 6RM estimado |
| 2 | 6 | 75% de 6RM estimado |
| 3 | Máximo | 100% de 6RM estimado |
| 4 | Máximo | Ajustado según set 3 |

### Ajuste del Set 4
| Reps en Set 3 | Ajuste para Set 4 |
|---------------|-------------------|
| 0-2 | Bajar 2.5-5 kg |
| 3-4 | Bajar 0-2.5 kg |
| 5-7 | Mantener |
| 8-12 | Subir 2.5-5 kg |
| 13+ | Subir 5-7.5 kg |

### Aplicación para Sistema
```yaml
APRE_PROTOCOLO:
  principio: "Ajustar peso según rendimiento del día"
  ventaja: "Respeta variabilidad individual"
  
  regla_progresion:
    si_completas_facil: "Subir 2.5-5 kg"
    si_completas_justo: "Mantener"
    si_no_completas: "Bajar 2.5-5 kg"
```

---

## 14. RESUMEN EJECUTIVO

| Aspecto | Recomendación |
|---------|---------------|
| Deload | Cada 4-6 semanas, reducir 40-50% volumen |
| Fases | AA → Hipertrofia → Fuerza → repetir |
| Ondulación | Variar intensidad dentro de la semana |
| Tapering | Reducir volumen, mantener intensidad |
| Transición | 2-4 semanas al año de descanso activo |

> **Mensaje clave**: La periodización evita el estancamiento y optimiza las adaptaciones. Nunca entrenar igual todo el año.
