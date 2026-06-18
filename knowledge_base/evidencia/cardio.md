---
id: "EVI-10"
nombre: "Evidencia: Capacidad Cardiovascular"
fecha_modificacion: "17/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-09", "REG-ENT-01"]
tags: ["evidencia", "cardio", "vo2max", "resistencia", "HIIT", "interferencia"]
prioridad: 10
---

# Capacidad Cardiovascular

> **Prioridad #10** — Alineado con [prioridades.md](../usuario/prioridades.md)

## 1. Alcance
Evidencia científica sobre capacidad aeróbica, salud cardiovascular y resistencia.

---

## 2. INTERFERENCIA CARDIO-FUERZA

### Fuente
**Wilson, Marin, Rhea, Wilson, Loenneke & Anderson (2012)**  
"Concurrent Training: A Meta-Analysis Examining Interference of Aerobic and Resistance Exercise"  
*Journal of Strength and Conditioning Research*

### Hallazgos Principales

#### Effect Sizes por Tipo de Entrenamiento

| Tipo | ES Hipertrofia | ES Fuerza | ES Potencia |
|------|---------------|-----------|-------------|
| **Solo fuerza** | **1.23** | **1.76** | **0.91** |
| Concurrente | 0.85 | 1.44 | 0.55 |
| Solo cardio | 0.27 | 0.78 | 0.11 |

> **Conclusión**: El entrenamiento concurrente reduce hipertrofia en ~31%, fuerza en ~18%, y potencia en ~40%.

#### Modalidad de Cardio

| Modalidad | Interferencia en Hipertrofia | Interferencia en Fuerza |
|-----------|------------------------------|------------------------|
| **Correr** | **SIGNIFICATIVA** | **SIGNIFICATIVA** |
| **Bicicleta** | No significativa | No significativa |

> **Conclusión clave**: CORRER interfiere significativamente. BICICLETA no interfiere.

#### Correlaciones Negativas

| Variable | Correlación con Ganancias |
|----------|--------------------------|
| Frecuencia cardio | r = -0.26 a -0.35 |
| Duración cardio | r = -0.29 a -0.75 |

> A mayor frecuencia y duración del cardio → mayores decrementos en hipertrofia/fuerza.

### Recomendaciones
```yaml
CARDIO_OPTIMO:
  modalidad_preferir: [bicicleta, remo, elíptica]
  modalidad_evitar: [correr, trotar]
  frecuencia_max: 2-3x/semana
  duracion_max: 30 min/sesión
  timing: días separados de pesas
```

---

## 3. HIIT VS LISS

### Fuente
**Viana et al. (2019)** - "Is interval training the magic bullet for fat loss?"

### Hallazgos

| Aspecto | HIIT | LISS |
|---------|------|------|
| Pérdida de grasa | Similar | Similar |
| Eficiencia tiempo | Mayor | Menor |
| Adherencia | Variable | Mayor |

> **Conclusión**: Ambos funcionan igual para grasa. HIIT más eficiente en tiempo.

---

## 4. RECOMENDACIONES PARA SISTEMA

| Aspecto | Recomendación | Razón |
|---------|---------------|-------|
| Modalidad | Bici, remo, elíptica | No interfiere |
| Evitar | Correr | Interfiere significativamente |
| Frecuencia | ≤2-3x/semana | Correlación negativa |
| Duración | ≤30 min | Correlación negativa |
| Timing | Días separados | Usuario prefiere ✓ |

> **Nota**: Usuario ya prefiere cardio separado → Óptimo según evidencia
