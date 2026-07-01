---
id: "EVI-05"
nombre: "Evidencia: Estrés y Cortisol"
fecha_modificacion: "17/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-06", "REG-LOG-03"]
tags: ["evidencia", "estres", "cortisol", "recuperacion", "ansiedad", "depresion"]
prioridad: 5
---

# Estrés y Cortisol

> **Prioridad #5** — Alineado con [prioridades.md](../usuario/prioridades.md)

## 1. Alcance
Evidencia científica sobre manejo de estrés, niveles de cortisol y su impacto en el entrenamiento.

---

## 2. EJERCICIO, ANSIEDAD, DEPRESIÓN Y ESTRÉS

### Fuente Principal
**Salmon, P. (2001)**  
"Effects of Physical Exercise on Anxiety, Depression and Sensitivity to Stress: A Unifying Theory"  
*Clinical Psychology Review, 21(1), 33-61*

---

## 3. EFECTOS DEL EJERCICIO EN SALUD MENTAL

### Hallazgos de Estudios Longitudinales

| Estudio | Muestra | Seguimiento | Hallazgo |
|---------|---------|-------------|----------|
| Paffenbarger et al. 1994 | 10,201 hombres | 25 años | Actividad física → menor depresión |
| Camacho et al. 1991 | 4,848 adultos | 18 años (2x9) | Falta de ejercicio → depresión |
| Farmer et al. 1988 | 1,900 adultos | 8 años | Ejercicio predice libertad de depresión |
| Mobily et al. 1996 | 2,084 ancianos | 3 años | Caminar diario → mejor depresión |

> **Conclusión**: El ejercicio habitual PREDICE menor depresión futura (efecto protector).

### Meta-análisis de Intervenciones

| Resultado | Tamaño del Efecto |
|-----------|-------------------|
| Reducción de depresión | **0.3 - 1.3 SD** vs controles |
| Efecto aeróbico | Demostrado |
| Efecto anaeróbico | También efectivo |

---

## 4. TEORÍA UNIFICADORA: RESILIENCIA AL ESTRÉS

### Concepto Central

> El entrenamiento físico **recluta un proceso** que confiere **resiliencia duradera al estrés**.

### Mecanismos Propuestos

| Mecanismo | Descripción |
|-----------|-------------|
| Adaptación al estrés | Ejercicio = estresor controlado |
| Cross-stressor adaptation | Tolerancia se transfiere a otros estresores |
| Regulación emocional | Mejor manejo de estados negativos |
| Componente social | Interacción grupal es terapéutica |

### Efectos Protectores

| Aspecto | Efecto del Ejercicio Regular |
|---------|------------------------------|
| Ansiedad | **↓ Reducida** |
| Depresión | **↓ Reducida** |
| Reactividad al estrés | **↓ Menor** |
| Síntomas somáticos | **↓ Reducidos** |

---

## 5. INTENSIDAD Y TIPO DE EJERCICIO

### Hallazgos sobre Intensidad

| Tipo | Efecto en Salud Mental |
|------|------------------------|
| Ejercicio vigoroso | Mayor efecto protector |
| Ejercicio moderado | Efectivo |
| Sedentarismo | Asociado a más síntomas |

> **Dato**: Actividades sedentarias se asociaron a MÁS síntomas psicológicos y somáticos (Steptoe & Butler, 1996).

### Edad y Efectos

| Población | Efecto |
|-----------|--------|
| Adultos mayores | **Mayor beneficio** que jóvenes |
| Adolescentes | Efectivo |
| Adultos | Efectivo |

---

## 6. CORTISOL Y ENTRENAMIENTO

### Del paper de Kraemer & Ratamess (2005)

| Respuesta | Descripción |
|-----------|-------------|
| Aguda | Cortisol ↑ durante entrenamiento intenso |
| Crónica | Ratio testosterona/cortisol mejora con entrenamiento |
| Sobreentrenamiento | Cortisol crónicamente elevado = marcador |

### Variables que Afectan Cortisol

| Factor | Efecto en Cortisol |
|--------|-------------------|
| Volumen alto | ↑ Mayor elevación |
| Intensidad alta | ↑ Mayor elevación |
| Descansos cortos | ↑ Mayor elevación |
| Déficit sueño | ↑ Elevado crónico |
| Déficit calórico severo | ↑ Elevado |

---

## 7. RECOMENDACIONES PARA SISTEMA

```yaml
EJERCICIO_SALUD_MENTAL:
  frecuencia: 3-5x/semana
  tipo: aeróbico_o_anaeróbico  # ambos funcionan
  intensidad: moderada_a_vigorosa
  
PROTECCION_ESTRES:
  - Ejercicio regular = resiliencia
  - Mejor que sedentarismo
  - Efecto dosis-respuesta
  
CORTISOL_MANAGEMENT:
  evitar:
    - sobreentrenamiento
    - deficit_sueno_cronico
    - deficit_calorico_severo
  optimizar:
    - descanso_adecuado_entre_sesiones
    - volumen_periodizado
    - nutricion_suficiente

INDICADORES_SOBREENTRENAMIENTO:
  - cortisol_cronico_alto
  - ratio_T:C_bajo
  - fatiga_persistente
  - ansiedad_elevada
```

---

## 8. RESUMEN EJECUTIVO

| Aspecto | Evidencia |
|---------|-----------|
| Ejercicio → menos depresión | **FUERTE** (múltiples longitudinales) |
| Ejercicio → menos ansiedad | **FUERTE** |
| Ejercicio → resiliencia al estrés | **FUERTE** |
| Mecanismo | Adaptación cruzada al estrés |
| Beneficio mayor | Adultos mayores |

> **Para el sistema**: El ejercicio regular es una intervención efectiva para salud mental. Incluir en recomendaciones generales.

## 4. Síntesis para el Sistema
*Completar tras revisar papers*

## 5. Implicaciones para Reglas
*Límites de volumen, deloads, timing de entrenamiento, protocolos de recuperación*
