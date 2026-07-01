---
id: "EVI-11"
nombre: "Evidencia: Nutrición"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "REG-NUT-01", "REG-NUT-02"]
tags: ["evidencia", "nutricion", "macros", "timing", "ramadan", "bmr", "tdee"]
prioridad: "soporte"
---

# Nutrición

> **Soporte** — Base científica para motor de dieta

## 1. Alcance
Evidencia científica sobre metabolismo basal, macronutrientes, timing nutricional y estrategias dietéticas.

---

## 2. METABOLISMO BASAL (REE/BMR)

### Fuente
**Mifflin, St Jeor, Hill, Scott, Daugherty & Koh (1990)**  
"A new predictive equation for resting energy expenditure in healthy individuals"  
*American Journal of Clinical Nutrition, 51:241-7*

### Datos del Estudio
| Parámetro | Valor |
|-----------|-------|
| Sujetos | 498 (247 mujeres, 251 hombres) |
| Edad | 19-78 años (45 ± 14) |
| Peso normal | n=264 |
| Obesos | n=234 |
| R² | **0.71** |

### Fórmula Mifflin-St Jeor (VALIDADA)
```
Hombres: REE = (10 × peso_kg) + (6.25 × altura_cm) - (5 × edad) + 5
Mujeres: REE = (10 × peso_kg) + (6.25 × altura_cm) - (5 × edad) - 161
```

### Comparación con Otras Fórmulas
| Fórmula | Error vs Medido |
|---------|-----------------|
| **Mifflin-St Jeor** | **Referencia** |
| Harris-Benedict (1919) | **Sobreestima 5%** |
| Cunningham (FFM) | Sobreestima 14-15% |
| Owen | Subestima 4% (mujeres), 0.1% (hombres) |

### Predictores de REE
| Predictor | R² |
|-----------|-----|
| Masa libre de grasa (FFM) | 0.64 |
| Peso total | 0.56 |

### Fórmula Alternativa (con FFM)
```
REE = (19.7 × FFM_kg) + 413
```

> **Conclusión**: Harris-Benedict está obsoleta (+5% error). Usar Mifflin-St Jeor.

---

## 3. MACRONUTRIENTES PARA CULTURISMO NATURAL

### Fuente
**Helms, Aragon & Fitschen (2014)**  
"Evidence-based recommendations for natural bodybuilding contest preparation: nutrition and supplementation"  
*Journal of the International Society of Sports Nutrition, 11:20*

### Pérdida de Peso Recomendada
| Velocidad | % Peso Corporal/Semana |
|-----------|------------------------|
| **Óptima** | **0.5 - 1%** |
| Ejemplo (80kg) | 0.4 - 0.8 kg/semana |

> **Objetivo**: Maximizar retención muscular durante déficit.

### Proteína

| Contexto | g/kg Masa Magra/día | g/kg Peso Total/día (aprox) |
|----------|---------------------|----------------------------|
| Déficit (corte) | **2.3 - 3.1** | ~2.0 - 2.6 |
| Mantenimiento | 1.8 - 2.2 | ~1.6 - 2.0 |
| Superávit (bulk) | 1.6 - 2.2 | ~1.4 - 1.8 |

> **Nota**: En déficit se necesita MÁS proteína para preservar músculo.

### Grasas

| Porcentaje de Calorías | Notas |
|------------------------|-------|
| **15 - 30%** | Mínimo para salud hormonal |
| < 15% | Riesgo hormonal |
| > 30% | Reduce espacio para carbos |

### Carbohidratos

| Estrategia | Notas |
|------------|-------|
| **Resto de calorías** | Después de proteína y grasa |
| Prioridad | Peri-entreno (antes/después) |

### Distribución de Comidas

| Parámetro | Recomendación |
|-----------|---------------|
| Comidas/día | **3 - 6** |
| Proteína pre-entreno | **0.4 - 0.5 g/kg** |
| Proteína post-entreno | **0.4 - 0.5 g/kg** |

> **Ventana anabólica**: Existe pero es más amplia de lo que se creía (~4-6h).

### Recomendaciones para el Sistema
```yaml
MACROS_DEFICIT:
  proteina: 2.3-3.1 g/kg masa_magra
  grasa: 15-30% calorias
  carbohidratos: resto
  
MACROS_MANTENIMIENTO:
  proteina: 1.8-2.2 g/kg masa_magra
  grasa: 20-30% calorias
  carbohidratos: resto
  
COMIDAS:
  minimo: 3
  optimo: 4-5
  maximo: 6
  proteina_por_comida: 0.4-0.5 g/kg
  
PERDIDA_PESO:
  velocidad_optima: 0.5-1% peso/semana
  ejemplo_80kg: 0.4-0.8 kg/semana
```

---

## 3. SUPLEMENTACIÓN CON EVIDENCIA

### Fuente
**Helms et al. (2014)** - Mismo paper

### Tier 1: Evidencia Fuerte

| Suplemento | Beneficio | Dosis |
|------------|-----------|-------|
| **Creatina monohidrato** | Fuerza, hipertrofia | 3-5 g/día |
| **Cafeína** | Rendimiento, energía | 3-6 mg/kg pre-entreno |
| **Beta-alanina** | Resistencia muscular | 3-5 g/día |
| **Proteína en polvo** | Conveniencia | Según necesidad |

### Tier 2: Evidencia Moderada

| Suplemento | Beneficio | Notas |
|------------|-----------|-------|
| Citrulina | Bombeo, rendimiento | 6-8 g pre-entreno |
| Vitamina D | Salud general | Si hay deficiencia |
| Omega 3 | Antiinflamatorio | 2-3 g/día |

### Sin Evidencia Suficiente

| Suplemento | Veredicto |
|------------|-----------|
| BCAAs | Innecesarios si proteína suficiente |
| Glutamina | No mejora rendimiento ni recuperación |
| Boosters testosterona | No funcionan |
| ZMA | Sin evidencia en no-deficientes |

---

## 4. 🌙 ENTRENAMIENTO DURANTE RAMADÁN

### Fuente
**Chaouachi, Leiper, Souissi, Coutts & Chamari (2009)**  
"Effects of Ramadan Intermittent Fasting on Sports Performance and Training: A Review"  
*International Journal of Sports Physiology and Performance, 4, 419-434*

> ⭐ **MUY RELEVANTE** para tu contexto cultural (ver cultura.md)

### Hallazgos Clave

| Aspecto | Hallazgo |
|---------|----------|
| Rendimiento físico | **Pocos aspectos negativamente afectados** |
| Decrementos | Solo modestos en algunos individuos |
| Fatiga subjetiva | Aumenta, pero NO se refleja en rendimiento |
| Hidratación | Deshidratación diurna, pero recuperación nocturna |

> **Conclusión del paper**: "El desarrollo e implementación temprana de estrategias sensatas de alimentación y sueño puede aliviar en gran medida las disrupciones al entrenamiento y competitividad."

### Riesgos Durante Ramadán

| Riesgo | Descripción |
|--------|-------------|
| Deshidratación diurna | Pérdida de >2% BM puede afectar rendimiento |
| Déficit calórico no planeado | Cambios en patrones alimentarios |
| Alteración del sueño | Comidas nocturnas (Suhoor, Iftar) |

### Recomendaciones para el Sistema
```yaml
RAMADAN_AJUSTES:
  hidratacion:
    - Maximizar ingesta entre Iftar y Suhoor
    - Priorizar agua sobre bebidas azucaradas
    - Evitar comidas muy saladas
    
  timing_entreno:
    opcion_1: Después de Iftar (tras romper ayuno)
    opcion_2: Antes de Suhoor (madrugada)
    evitar: Horas centrales del día
    
  nutricion:
    - Iftar: comida principal + hidratación
    - Snack nocturno: proteína + carbos
    - Suhoor: carbos complejos + proteína + grasas
    
  entrenamiento:
    - Considerar reducir volumen 10-20%
    - Mantener intensidad si es posible
    - Priorizar ejercicios compuestos
    - Sesiones más cortas
    
  estrategia_general:
    - Planificar ANTES de que empiece Ramadán
    - Adaptar horarios de sueño progresivamente
    - No intentar PRs ni entrenamientos máximos
```

---

## 5. RECOMPOSICIÓN CORPORAL (GANAR MÚSCULO + PERDER GRASA)

### Fuente
**Barakat, Pearson, Escalante, Campbell & De Souza (2020)**  
"Body Recomposition: Can Trained Individuals Build Muscle and Lose Fat at the Same Time?"  
*Strength and Conditioning Journal, 42(5)*

### Hallazgo Principal

> **SÍ es posible ganar músculo y perder grasa simultáneamente**, incluso en personas entrenadas.

| Mito | Realidad |
|------|----------|
| "Solo funciona en novatos" | Demostrado en entrenados con años de experiencia |
| "Necesitas déficit para perder grasa" | Posible incluso en superávit si es por proteína |
| "Tienes que elegir bulk o cut" | Recomposición es una tercera opción viable |

### Factores Clave para Recomposición

| Factor | Recomendación | Evidencia |
|--------|---------------|-----------|
| **Proteína alta** | **>2.0 g/kg/día** | Múltiples estudios |
| Entrenamiento resistencia | Progresivo | Estímulo hipertrófico |
| Entrenamiento concurrente | RT + Cardio | Optimiza recomposición |

### Estudios en Entrenados

| Estudio | Proteína | Resultado FFM | Resultado FM |
|---------|----------|---------------|--------------|
| Antonio et al. (3.4 g/kg) | Alta | +1.5 kg | **-1.6 kg** |
| Campbell et al. (2.5 g/kg) | Alta | +2.1 kg | **-1.1 kg** |
| Haun et al. (2.2 g/kg) | Alta | +2.9 kg | **-1.0 kg** |

### Aplicación para el Sistema

```yaml
RECOMPOSICION:
  viable_para:
    - principiantes
    - intermedios
    - avanzados (con alta proteína)
    
  requisitos:
    proteina: ">2.0 g/kg/día"
    entrenamiento: "progresivo de fuerza"
    deficit: "leve o mantenimiento"
    
  cuando_usar:
    - Usuario cerca de su peso ideal
    - No quiere hacer bulk tradicional
    - Quiere mejorar composición sin cambiar peso
    
  cuando_NO_usar:
    - Muy delgado (bulk primero)
    - Muy alto % grasa (cut primero)
    - Competidor de fisiculturismo (fases definidas)
```

---

## 6. NUTRICIÓN EN OFF-SEASON (FASE DE GANANCIA)

### Fuente
**Iraki, Fitschen, Espinar & Helms (2019)**  
"Nutrition Recommendations for Bodybuilders in the Off-Season: A Narrative Review"  
*Sports, 7(7), 154*

### Objetivo Off-Season
> **Aumentar masa muscular minimizando ganancia de grasa.**

### Superávit Calórico

| Nivel | Superávit | Ganancia de Peso Semanal |
|-------|-----------|--------------------------|
| **Novato/Intermedio** | **10-20%** | **0.25-0.5% peso corporal** |
| Avanzado | 5-10% | 0.25% peso corporal |

> **Nota**: Avanzados deben ser más conservadores (menor potencial de ganancia).

### Macronutrientes Off-Season

| Macro | Recomendación | Distribución |
|-------|---------------|--------------|
| **Proteína** | **1.6-2.2 g/kg/día** | 0.40-0.55 g/kg por comida |
| **Grasa** | **0.5-1.5 g/kg/día** | ~20-30% calorías |
| **Carbohidratos** | **≥3-5 g/kg/día** | Resto de calorías |

### Timing Nutricional

| Momento | Recomendación |
|---------|---------------|
| Pre-entreno | Proteína + carbos 1-2h antes |
| Post-entreno | Proteína + carbos 1-2h después |
| Comidas/día | **3-6** (distribuir proteína) |
| Proteína por comida | **0.40-0.55 g/kg** |

### Suplementos Off-Season

| Suplemento | Dosis | Beneficio |
|------------|-------|-----------|
| Creatina | 3-5 g/día | Fuerza, hipertrofia |
| Cafeína | 5-6 mg/kg | Rendimiento |
| Beta-alanina | 3-5 g/día | Resistencia muscular |
| Citrulina malato | 8 g/día | Rendimiento, bombeo |

### Recomendaciones para el Sistema

```yaml
FASE_OFFSEASON:
  calorias: mantenimiento + 10-20%
  ganancia_peso: 0.25-0.5%/semana
  
  macros:
    proteina: 1.6-2.2 g/kg
    grasa: 0.5-1.5 g/kg (20-30%)
    carbos: ≥3-5 g/kg
    
  timing:
    comidas: 3-6/dia
    proteina_por_comida: 0.4-0.55 g/kg
    peri_entreno: proteina + carbos
    
  duracion_tipica: 6-12 meses
  
  transicion_a_deficit:
    cuando: ">15-18% grasa"
    o: "objetivo alcanzado"
```

---

## 7. COMPARATIVA: DÉFICIT VS OFF-SEASON VS RECOMP

| Aspecto | Déficit (Helms) | Off-Season (Iraki) | Recomposición |
|---------|-----------------|--------------------| --------------|
| **Calorías** | -20-25% | +10-20% | Mantenimiento |
| **Proteína** | 2.3-3.1 g/kg LBM | 1.6-2.2 g/kg | >2.0 g/kg |
| **Carbos** | Reducidos | ≥3-5 g/kg | Moderados |
| **Grasa** | 15-30% | 20-30% | 20-30% |
| **Peso** | -0.5-1%/sem | +0.25-0.5%/sem | ±0 |
| **Duración** | 8-16 semanas | 6-12 meses | Indefinida |

---

## 8. IMPLICACIONES PARA REGLAS

### Actualizar en `/reglas/`

| Archivo | Parámetro | Valor |
|---------|-----------|-------|
| `motor_dieta.md` | Proteína déficit | 2.3-3.1 g/kg LBM |
| `motor_dieta.md` | Grasa | 15-30% kcal |
| `motor_dieta.md` | Pérdida peso | 0.5-1%/semana |
| `preferencias.md` | Comidas/día | 3-6 |

### Crear Protocolo Ramadán
- Archivo: `reglas/nutricion/ramadan.md`
- Contenido: Ajustes específicos basados en Chaouachi 2009
*Cálculos de macros, distribución de comidas, ajustes por objetivo*
