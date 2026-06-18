---
id: "EVI-02"
nombre: "Evidencia: Corrección Postural"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-01", "REG-ENT-01"]
tags: ["evidencia", "postura", "cifosis", "lordosis", "correccion", "cervical"]
prioridad: 2
---

# Corrección Postural

> **Prioridad #2** — Alineado con [prioridades.md](../usuario/prioridades.md)

## 1. Alcance
Evidencia científica sobre corrección postural, desequilibrios musculares y protocolos de mejora.

## 2. Subáreas (de prioridades.md)
- Hipercifosis dorsal
- Hiperlordosis lumbar
- Hombros rotados internamente
- Caja torácica colapsada
- Posición cervical (cuello adelantado)
- Patrón de cadera (anterior pelvic tilt)

---

## 3. ESTRÉS CERVICAL POR POSICIÓN DE CABEZA

### Fuente
**Hansraj K.K. (2014)**  
"Assessment of Stresses in the Cervical Spine Caused by Posture and Position of the Head"  
*Surgical Technology International XXV*

### Hallazgos Principales

#### Peso Efectivo por Ángulo de Flexión

| Ángulo | Peso en Columna Cervical |
|--------|--------------------------|
| **Neutral (0°)** | **10-12 lbs (4.5-5.4 kg)** |
| 15° | 27 lbs (12 kg) |
| 30° | 40 lbs (18 kg) |
| 45° | 49 lbs (22 kg) |
| **60°** | **60 lbs (27 kg)** |

> **Impacto**: La cabeza en posición de "text neck" (60°) genera **5x más estrés** en columna cervical.

#### Datos de Uso
- Promedio 2-4 horas/día con cabeza flexionada
- = 700-1400 horas/año de estrés cervical excesivo
- Estudiantes: hasta 5000 horas/año adicionales

### Buena Postura (Definición)
- Orejas alineadas con hombros
- Escápulas retraídas ("angel wings")
- Posición más eficiente para la columna

### Beneficios Posturales (Amy Cuddy)
| Postura Alta | Postura Baja |
|--------------|--------------|
| ↑ Testosterona | ↓ Testosterona |
| ↑ Serotonina | ↓ Serotonina |
| ↓ Cortisol | ↑ Cortisol |
| ↑ Tolerancia al riesgo | ↓ Tolerancia al riesgo |

---

## 4. PROTOCOLO CORRECTIVO (16 SEMANAS)

### Fuente
**Ruivo, Pezarat-Correia & Carita (2016)**  
"Effects of a Resistance and Stretching Training Program on Forward Head and Protracted Shoulder Posture in Adolescents"  
*Journal of Manipulative and Physiological Therapeutics*

### Resultado Principal
> **16 semanas de fuerza + estiramientos** = mejora significativa en:
> - Ángulo cervical (forward head)
> - Ángulo de hombros (protracción)

### Parámetros del Programa

| Aspecto | Valor |
|---------|-------|
| Duración | **16 semanas** |
| Frecuencia | **2x/semana** |
| Tiempo/sesión | **15-20 min** |

### Ejercicios de Fortalecimiento

| Ejercicio | Músculos | Descripción |
|-----------|----------|-------------|
| **Rotación externa acostado** | Infraespinoso, Teres minor | Lado, codo 90°, rotar hombro |
| **Prone Y-to-I** | Trapecio medio/bajo, Serrato | Prone, abducir brazos formando Y→I |
| **Prone horizontal abduction** | Trapecio medio, Romboides | Prone, abducir horizontal con rotación externa |
| **Chin tuck** | Longus colli, Longus capitis | Supino, retracción cervical suave |

### Ejercicios de Estiramiento

| Ejercicio | Músculos |
|-----------|----------|
| Estiramiento pectoral en pared | Pectoral menor |
| Estiramiento SCM | Esternocleidomastoideo |
| Estiramiento elevador escápula | Elevador de la escápula |

### Aplicación para el Sistema

| Problema Usuario | Ejercicios Indicados |
|------------------|---------------------|
| Forward head | Chin tucks, estiramiento SCM |
| Hombros internos | Rotación externa, prone Y-I, estiramiento pectoral |
| Hipercifosis | Prone horizontal abduction, Y-to-I |

---

## 5. RECOMENDACIONES PARA SISTEMA

```yaml
PROTOCOLO_POSTURA:
  duracion_minima: 16 semanas
  frecuencia: 2x/semana
  tiempo_sesion: 15-20 min
  
EJERCICIOS_FORTALECIMIENTO:
  - chin_tuck: cervical
  - rotacion_externa_lateral: manguito
  - prone_YI: trapecio_bajo
  - prone_horizontal_abd: romboides

EJERCICIOS_ESTIRAMIENTO:
  - pectoral_pared: pectoral_menor
  - estiramiento_SCM: cuello
  - levator_scapulae: elevador
  
POSTURA_NEUTRAL:
  cervical: orejas_sobre_hombros
  escapulas: retraidas
  beneficios: [menor_cortisol, mayor_testosterona]
```

---

## 6. CONTROL SENSORIMOTOR CERVICAL

### Fuente
**Treleaven, J. (2008)**  
"Sensorimotor disturbances in neck disorders affecting postural stability, head and eye movement control"  
*Manual Therapy 13(2008) 2-11*

### Concepto Clave

> Los receptores en la columna cervical tienen conexiones importantes con el aparato vestibular y visual. La disfunción de estos receptores puede **alterar el control sensorimotor**.

### Síntomas Relacionados con Disfunción Cervical

| Síntoma | Descripción |
|---------|-------------|
| **Mareo** | Sensación de inestabilidad |
| **Visión borrosa** | Dificultad de enfoque |
| **Pérdida de equilibrio** | Caídas, tropiezos |
| **Dificultad caminando** | En oscuridad, escaleras |

### Mecanismos de Alteración

| Causa | Efecto |
|-------|--------|
| Trauma a mecanorreceptores | Disfunción directa |
| Fatiga muscular aumentada | Señales alteradas |
| Cambios morfológicos | Infiltración grasa, atrofia |
| Dolor | Altera sensibilidad de husos musculares |
| Estrés (SNS) | Altera actividad de husos vía simpático |

### Test de Propiocepción Cervical (JPS)

| Método | Descripción |
|--------|-------------|
| **Laser pointer test** | Puntero en cabeza, ojos cerrados |
| Distancia | 90 cm de la pared |
| Movimiento | Extensión, flexión, rotación |
| Error normal | < 3-4° (4-5 cm) |
| **Error anormal** | **> 4-5 cm = déficit** |

### Tratamiento Recomendado

| Tipo | Evidencia |
|------|-----------|
| Acupuntura | Mejora JPS, vértigo |
| Terapia manual | Mejora equilibrio |
| Entrenamiento flexión craneocervical | Mejora JPS |
| Ejercicios de estabilidad de mirada | Mejora coordinación ojo-cabeza |

### Protocolo Sensorimotor Sugerido

```yaml
EJERCICIOS_SENSORIOMOTORES:
  gaze_stability:
    - fijar_punto_mover_cabeza
    - seguir_objeto_con_ojos
    
  coordinacion_ojo_cabeza:
    - mirar_punto_mover_cabeza_opuesto
    - seguimiento_suave
    
  propiocepcion_cervical:
    - reposicionamiento_con_ojos_cerrados
    - chin_tucks_lentos
    
  equilibrio_progresivo:
    - pies_juntos_ojos_cerrados
    - apoyo_unipodal
    - superficie_inestable
```

### Relación con Postura

| Problema Postural | Efecto Sensorimotor |
|-------------------|---------------------|
| Forward head crónico | Altera input cervical → mareo |
| Rigidez cervical | Reduce propiocepción |
| Debilidad flexores profundos | Peor control motor |

---

## 7. RESUMEN EJECUTIVO

| Aspecto | Recomendación | Fuente |
|---------|---------------|--------|
| **Estrés cervical** | Mantener neutral (0°), evitar flexión prolongada | Hansraj 2014 |
| **Protocolo correctivo** | 16 semanas, 2x/semana, 15-20 min | Ruivo 2016 |
| **Ejercicios fuerza** | Chin tucks, rotación externa, prone Y-I | Ruivo 2016 |
| **Ejercicios estiramiento** | Pectoral, SCM, elevador escápula | Ruivo 2016 |
| **Control sensorimotor** | Gaze stability, equilibrio, propiocepción | Treleaven 2008 |

> **Mensaje clave**: La postura no es solo estética. La posición cervical afecta el control motor, el equilibrio y puede causar mareos. Corregir con fuerza + estiramientos + entrenamiento sensorimotor.

---

## 8. Implicaciones para Reglas

✅ Datos completos, actualizar:
- `reglas/entrenamiento/calentamiento.md` → ejercicios correctivos
- `usuario/biometria.md` → evaluación postural
