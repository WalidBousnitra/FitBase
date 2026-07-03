---
id: "EVI-03"
nombre: "Evidencia: Hipertrofia"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-01", "REG-ENT-01"]
tags: ["evidencia", "hipertrofia", "volumen", "intensidad", "frecuencia"]
prioridad: 3
---

# Hipertrofia

> **Prioridad #3** — Alineado con [prioridades.md](../usuario/prioridades.md)

## 1. Alcance
Evidencia científica sobre mecanismos de crecimiento muscular y programación óptima.

## 2. Subáreas Prioritarias (de prioridades.md)
1. Hombros (deltoides)
2. Bíceps
3. Espalda (dorsales, romboides, trapecios)
4. Tríceps
5. Pecho
6. Abdominales

---

## 3. VOLUMEN ÓPTIMO

### Fuente
**Schoenfeld, Ogborn & Krieger (2017)**  
"Dose-response relationship between weekly resistance training volume and increases in muscle mass"  
*Journal of Sports Sciences, 35:11, 1073-1082*

### Hallazgos Clave

| Series/Semana por Músculo | Effect Size | Ganancia Equivalente |
|---------------------------|-------------|---------------------|
| < 5 series | 0.307 | 5.4% |
| 5-9 series | 0.378 | 6.6% |
| **10+ series** | **0.520** | **9.8%** |

> **Conclusión**: Relación dosis-respuesta graduada. Cada serie adicional por semana = +0.37% de ganancia muscular.

### Recomendaciones para el Sistema
```yaml
VOLUMEN_SEMANAL:
  minimo_efectivo: 5 series/músculo
  optimo: 10+ series/músculo
  incremento_por_serie: 0.37%
  
GRUPOS_PRIORITARIOS:  # P1-P3
  hombros: 14-18 series/semana  # Elevado vs base (10+) por prioridad P1 usuario
  biceps: 10-14 series/semana
  espalda: 14-18 series/semana
  triceps: 10-14 series/semana
  pecho: 10-14 series/semana
  
GRUPOS_SECUNDARIOS:
  piernas: 10-12 series/semana
  abdominales: 6-10 series/semana
```

---

## 4. FRECUENCIA DE ENTRENAMIENTO

### Fuente
**Schoenfeld et al. (2019)**  
"How many times per week should a muscle be trained to maximize muscle hypertrophy?"  
*Systematic review and meta-analysis*

### Hallazgos Clave

> **"La frecuencia semanal NO afecta significativamente la hipertrofia cuando el VOLUMEN está igualado."**

| Frecuencia | Efecto en Hipertrofia |
|------------|----------------------|
| 1x/semana | = |
| 2x/semana | = |
| 3x/semana | = |

> **Conclusión**: La frecuencia es una HERRAMIENTA para acumular volumen, no un factor independiente.

### Recomendaciones para el Sistema
```yaml
FRECUENCIA:
  minima: 1x/semana por músculo
  recomendada: 2x/semana  # Permite distribuir volumen
  maxima_util: 3x/semana
  
RAZON_FRECUENCIA_ALTA:
  - Permite más volumen total
  - Mejor tolerancia por sesión
  - NO porque sea mágicamente mejor
```

---

## 5. INTENSIDAD (Sistema RIR/RPE)

### Fuente
**Helms, Cronin, Storey & Zourdos (2016)**  
"Application of the Repetitions in Reserve-Based Rating of Perceived Exertion Scale for Resistance Training"  
*Strength and Conditioning Journal*

### Escala RIR-RPE

| RPE | RIR | Descripción |
|-----|-----|-------------|
| 10 | 0 | Fallo muscular (máximo esfuerzo) |
| 9 | 1 | Quedaba 1 repetición |
| 8 | 2 | Quedaban 2 repeticiones |
| 7 | 3 | Quedaban 3 repeticiones |
| 5-6 | 4-6 | Esfuerzo moderado |
| 3-4 | — | Esfuerzo ligero |
| 1-2 | — | Poco o ningún esfuerzo |

### Hallazgos Clave
- Los atletas pueden estimar RIR con alta precisión (r = 0.93-0.95)
- La precisión AUMENTA con la fatiga (más cerca del fallo = más precisión)
- Mejor que % de 1RM para ajustar cargas diarias

### Recomendaciones para el Sistema
```yaml
RIR_OBJETIVO:
  series_hipertrofia: 2-3 RIR (RPE 7-8)
  series_fuerza: 1-2 RIR (RPE 8-9)
  series_al_fallo: 0 RIR (RPE 10) - usar con moderación
  
PROGRESION_RIR:
  semana_1: RIR 3-4
  semana_2: RIR 2-3
  semana_3: RIR 1-2
  semana_4: DELOAD
```

---

## 6. DESCANSO ENTRE SERIES

### Fuente
**Schoenfeld, B.J. et al. (2016)**  
"Longer inter-set rest periods enhance muscle strength and hypertrophy in resistance-trained men"  
*Journal of Strength and Conditioning Research*

### Diseño del Estudio
- 21 hombres entrenados
- 8 semanas, 3 sesiones/semana
- 3 series x 8-12 RM
- Comparación: **1 minuto vs 3 minutos** de descanso

### Hallazgos Clave

| Variable | 1 min descanso | 3 min descanso | Ganador |
|----------|----------------|----------------|---------|
| **Fuerza (1RM squat)** | ↑ | ↑↑ | **3 min** |
| **Fuerza (1RM bench)** | ↑ | ↑↑ | **3 min** |
| **Hipertrofia cuádriceps** | ↑ | ↑↑ | **3 min** |
| **Hipertrofia tríceps** | ↑ | ↑↑ (tendencia) | **3 min** |
| Resistencia muscular | ↑ | ↑ | = |

> **CONCLUSIÓN PRINCIPAL**: Descansos de **3 minutos** producen **MAYOR hipertrofia y fuerza** que descansos de 1 minuto.

### Por qué Funciona Mejor

| Mecanismo | Explicación |
|-----------|-------------|
| Mayor recuperación | Permite mantener más peso/reps en series siguientes |
| Mayor volumen total | Más trabajo acumulado = más estímulo |
| Menor fatiga acumulada | Mejor calidad de cada serie |

### Mito Desmentido

| Creencia Tradicional | Realidad (Evidencia) |
|----------------------|---------------------|
| "Descansos cortos = más hipertrofia" | **FALSO** |
| "El 'pump' indica crecimiento" | No correlaciona con hipertrofia |
| "1 min para hipertrofia, 3+ min para fuerza" | 3 min es mejor para AMBOS |

### Recomendaciones para el Sistema

```yaml
DESCANSO_ENTRE_SERIES:
  compuestos_pesados: 3-5 minutos
  compuestos_moderados: 2-3 minutos
  aislamiento: 1.5-2 minutos
  
  PRIORIZAR: calidad_de_serie > fatiga_metabolica
  
  APLICACION:
    press_banca: 3 min
    sentadilla: 3-5 min
    dominadas: 2-3 min
    curl_biceps: 1.5-2 min
    elevaciones_laterales: 1-2 min
```

---

## 7. INTERFERENCIA CARDIO-FUERZA

### Fuente
**Wilson et al. (2012)**  
"Concurrent Training: A Meta-Analysis Examining Interference of Aerobic and Resistance Exercise"  
*Journal of Strength and Conditioning Research*

### Hallazgos Clave

| Tipo de Entrenamiento | ES Hipertrofia | ES Fuerza | ES Potencia |
|-----------------------|---------------|-----------|-------------|
| Solo fuerza | **1.23** | **1.76** | **0.91** |
| Concurrente | 0.85 | 1.44 | 0.55 |
| Solo cardio | 0.27 | 0.78 | 0.11 |

> **Conclusión**: El cardio SÍ interfiere con hipertrofia (-31% ES) y potencia (-40% ES).

### Tipo de Cardio
| Modalidad | Interferencia |
|-----------|---------------|
| **Correr** | ALTA (interfiere significativamente) |
| **Bicicleta** | BAJA (no interfiere significativamente) |

### Correlaciones Negativas
- Frecuencia cardio ↔ hipertrofia: r = -0.26 a -0.35
- Duración cardio ↔ hipertrofia: r = -0.29 a -0.75

### Recomendaciones para el Sistema
```yaml
CARDIO_PARA_HIPERTROFIA:
  modalidad_preferida: bicicleta, remo, elíptica
  modalidad_evitar: correr (alta interferencia)
  
  frecuencia_maxima: 2-3 sesiones/semana
  duracion_maxima: 20-30 min por sesión
  
  timing:
    opcion_1: días separados de pesas
    opcion_2: después de pesas (nunca antes)
    opcion_3: mañana cardio, tarde pesas
```

> **Nota usuario**: Prefiere cardio en sesión SEPARADA ✓ (ver preferencias.md)

---

## 8. IMPLICACIONES PARA REGLAS

### Actualizar en `/reglas/`

| Archivo | Parámetro | Valor |
|---------|-----------|-------|
| `programacion.md` | Volumen prioritarios | 10-16 series/semana |
| `programacion.md` | Frecuencia | 2x/semana |
| `motor_pesos.md` | RIR objetivo | 2-3 (RPE 7-8) |
| `preferencias.md` | Cardio | Separado ✓, preferir bici |
| `programacion.md` | Descanso series | 2-3 min (compuestos), 1.5-2 min (aislamiento) |

---

## 9. HIPERTROFIA NO UNIFORME (Selección de Ejercicios)

### Fuente
**Wakahara, T., Fukutani, A., Kawakami, Y. & Yanai, T. (2013)**  
"Nonuniform Muscle Hypertrophy: Its Relation to Muscle Activation in Training Session"  
*Medicine & Science in Sports & Exercise, 45(11), 2158-2165*

### Hallazgo Principal

> **La hipertrofia ocurre de forma NO UNIFORME a lo largo del músculo.**  
> La región que más se activa durante el ejercicio es la que más hipertrofia.

### Mecanismo

| Concepto | Explicación |
|----------|-------------|
| Activación regional | Diferentes ejercicios activan diferentes partes del músculo |
| Hipertrofia regional | Las zonas más activadas crecen más |
| Implicación | Necesitas VARIEDAD de ejercicios para desarrollo completo |

### Estudio: Tríceps Braquial

| Región | Activación 1ª Sesión | Hipertrofia 12 semanas |
|--------|---------------------|------------------------|
| **Medio** | **ALTA** | **MAYOR** |
| Proximal | Baja | Menor |

> **Conclusión**: La región del tríceps más activada en el ejercicio fue la que más creció.

### Diferencias entre Ejercicios

| Tipo de Ejercicio | Activación | Implicación |
|-------------------|------------|-------------|
| Monoarticular (extensión rodilla) | Más recto femoral | Hipertrofia localizada |
| Multiarticular (sentadilla, prensa) | Más vastos | Hipertrofia diferente |

### Recomendaciones para Desarrollo Completo

```yaml
SELECCION_EJERCICIOS:
  principio: "Variedad de ángulos y ejercicios para activar todas las regiones"
  
  EJEMPLO_TRICEPS:
    - press_frances: porcion_larga
    - extension_polea: porcion_lateral
    - dips: activacion_general
    
  EJEMPLO_PECHO:
    - press_plano: porcion_media
    - press_inclinado: porcion_clavicular
    - aperturas: estiramiento
    
  EJEMPLO_CUADRICEPS:
    - sentadilla: vastos
    - extension: recto_femoral
    
  FRECUENCIA_VARIACION:
    - rotar_ejercicios_cada_4-6_semanas
    - incluir_al_menos_2_ejercicios_por_musculo
```

### Implicación Práctica

| Músculo | Ejercicios Recomendados para Desarrollo Completo |
|---------|--------------------------------------------------|
| Tríceps | Press francés + Extensiones + Dips |
| Bíceps | Curl inclinado + Curl predicador + Curl martillo |
| Hombros | Press + Elevaciones laterales + Elevaciones posteriores |
| Pecho | Press plano + Press inclinado + Aperturas |
| Espalda | Dominadas + Remo + Pullover |
| Cuádriceps | Sentadilla + Prensa + Extensiones |

---

## 10. RESUMEN EJECUTIVO

| Aspecto | Recomendación | Fuente |
|---------|---------------|--------|
| Volumen | 10+ series/músculo/semana | Schoenfeld 2017 |
| Frecuencia | 2x/semana (para distribuir volumen) | Schoenfeld 2019 |
| Intensidad | RIR 2-3 (RPE 7-8) | Helms 2016 |
| **Descanso** | **2-3 min compuestos, 1.5-2 min aislamiento** | **Schoenfeld 2016** |
| Cardio | Bici sí, correr no, separado | Wilson 2012 |
| Ejercicios | Variedad para desarrollo completo | Wakahara 2013 |
