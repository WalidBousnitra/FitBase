---
id: "EVI-12"
nombre: "Evidencia: Sueño y Recuperación"
fecha_modificacion: "17/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-05", "EVI-06", "REG-LOG-01"]
tags: ["evidencia", "sueno", "recuperacion", "descanso"]
prioridad: "soporte"
---

# Sueño y Recuperación

> **Soporte** — Base científica para autorregulación

## 1. EVIDENCIA: SUEÑO Y RENDIMIENTO ATLÉTICO

### Fuente Principal
**Fullagar, Skorski, Duffield, Hammes, Coutts & Meyer (2015)**  
"Sleep and Athletic Performance: The Effects of Sleep Loss on Exercise Performance"  
*Sports Medicine 45:161-186*

---

## 2. RECOMENDACIONES DE SUEÑO

| Población | Recomendación |
|-----------|---------------|
| Adulto sano | **7-9 horas** (National Sleep Foundation) |
| Atletas | **9-10 horas** (algunos autores sugieren) |
| Realidad atletas | 6-8 horas (75% de atletas élite) |

> **Problema**: Atletas duermen MENOS de lo recomendado

---

## 3. CAUSAS DE MAL SUEÑO EN ATLETAS

### Pre-competición
- 64% de atletas élite reportan mal sueño antes de competición importante
- Nerviosismo y ansiedad
- Deterioro de confianza y estado de ánimo
- Estrés físico y mental elevado

### Entrenamiento
- Volumen alto de entrenamiento altera movimientos durante sueño
- Entrenamientos matutinos reducen duración (5.4h vs 7-8h normal)
- Intensidad alta puede aumentar latencia de inicio del sueño

### Ambientales
- Ruido y luz
- Viajes y jet lag
- Tecnología (exposición a luz azul)

---

## 4. EFECTOS DEL DÉFICIT DE SUEÑO

### En Rendimiento Físico

| Tipo de Esfuerzo | Efecto de Déficit | Notas |
|------------------|-------------------|-------|
| **Fuerza máxima** | **Mantenida** | Back/grip strength OK |
| **Potencia** | Variable | Resultados mixtos |
| **Aeróbico submáximo** | **↓ VO2max** | Más afectado |
| **Tolerancia al ejercicio** | **↓** | Vía percepción |
| **RPE (esfuerzo percibido)** | **↑** | Mismo trabajo = más duro |

### En Respuestas Fisiológicas (con restricción 3-4h)

| Variable | Efecto |
|----------|--------|
| FC en ejercicio | ↑ Aumentada |
| Ventilación | ↑ Aumentada |
| Lactato sanguíneo | ↑ Aumentado |
| Cortisol | Variable |
| Testosterona | ↑ (compensatorio?) |
| Growth Hormone | ↑ |
| IL-6 (inflamación) | ↑ |
| Glucógeno muscular | ↓ Reducido (30h privación) |

### En Cognición

| Función | Efecto |
|---------|--------|
| Tiempo de reacción | **↓ Más lento** |
| Precisión | **↓ Menos preciso** |
| Toma de decisiones | **↓ Peor** |
| Aprendizaje motor | **↓ Afectado** |

> **Conclusión**: Cognición es lo MÁS afectado por déficit de sueño

---

## 5. IMPORTANCIA DEL SUEÑO PARA APRENDIZAJE MOTOR

- REM, NREM stage 2 y SWS involucrados en consolidación de memoria
- Mejoras en tareas motoras ocurren DESPUÉS de dormir
- Sin sueño = sin mejora overnight en aprendizaje motor
- Crítico para atletas que necesitan adaptación neurocognitiva

---

## 6. RECOMENDACIONES PARA SISTEMA

### Del Paper Fullagar (SÍ EVIDENCIA)
```yaml
UMBRALES_SUENO_HORAS:
  optimo_atletas: 9-10 horas  # Algunos autores sugieren
  recomendado_adulto: 7-9 horas  # National Sleep Foundation
  realidad_atletas: 6-8 horas  # 75% élite

EFECTOS_DEFICIT:
  fuerza_maxima: "SE MANTIENE"  # Back/grip OK
  resistencia: "SE REDUCE"
  cognicion: "LO MAS AFECTADO"
  RPE: "AUMENTA (mismo trabajo = más duro)"
  aprendizaje_motor: "REQUIERE SUEÑO"

PRIORIZAR_SI_DEFICIT:
  hacer: "Fuerza máxima (esfuerzos cortos)"
  evitar: "Cardio/resistencia prolongada"
```

### NO del Paper (PENDIENTE EVIDENCIA)
> 🚨 Los porcentajes de reducción (20-30%, 10%) NO están en Fullagar.
> Necesitan paper específico de autorregulación.

```yaml
# ELIMINADO - Sin evidencia:
# reducir_volumen: 20-30%  ← ¿De dónde sale?
# reducir_volumen: 10%     ← ¿De dónde sale?
```

### Higiene del Sueño (Del Paper - SÍ EVIDENCIA)
```yaml
FACTORES_QUE_AFECTAN:
  evitar:
    - entrenamientos_matutinos_muy_temprano  # Reducen a 5.4h
    - intensidad_alta_cerca_de_dormir        # Aumenta latencia
    - luz_azul                                # General, no del paper
  ambiente:
    - ruido_y_luz                            # Del paper
```

---

## 7. AUTORREGULACIÓN BASADA EN HRV

### Fuente
**Kiviniemi, Hautala, Kinnunen & Tulppo (2007)**  
"Endurance training guided individually by daily heart rate variability measurements"  
*European Journal of Applied Physiology, 101:743-751*

### Diseño del Estudio
| Parámetro | Valor |
|-----------|-------|
| Sujetos | 26 hombres recreativamente activos |
| Duración | 4 semanas |
| Grupos | HRV-guiado (n=13) vs Predefinido (n=13) |
| Medición HRV | **Diaria, matutina (2 min supino)** |

### Protocolo de Decisión HRV
```yaml
REGLA_HRV:
  si_HRV_sube_o_estable:
    accion: "Entrenar alta intensidad"
    umbral: "≥ media de 10 días"
    
  si_HRV_baja:
    accion: "Entrenar baja intensidad o descanso"
    criterio_1: "Por debajo de (media_10d - 1 SD)"
    criterio_2: "Tendencia decreciente 2+ días consecutivos"
```

### Resultados: HRV vs Predefinido
| Medida | Grupo Predefinido | Grupo HRV | Diferencia |
|--------|-------------------|-----------|------------|
| VO2peak inicial | 54.3 | 56.1 | - |
| VO2peak final | 55.0 | **60.1** | HRV +7.1% |
| Loadmax mejora | Menor | **Mayor** | p=0.048 |
| Días de entrenamiento | ~igual | ~igual | - |

> **Conclusión**: Guiar el entrenamiento por HRV produjo **mejoras significativamente mayores** que un programa predefinido, a pesar de volumen total similar.

### Aplicación para Sistema
```yaml
HRV_AUTOREGULACION:
  medicion:
    momento: "Al despertar, antes de levantarse"
    duracion: "2-3 minutos"
    posicion: "Supino"
  
  referencia:
    media_movil: "10 días"
    umbral_bajo: "media - 1 SD"
    
  decision:
    HRV_normal_o_alto: "Sesión planificada normal"
    HRV_bajo_puntual: "Reducir intensidad 20%"
    HRV_bajo_2_dias: "Día de recuperación activa"
    
  nota: "Las reducciones porcentuales son heurísticas, no del paper"
```

### Zepp/Amazfit Integration
| Métrica Amazfit | Uso |
|-----------------|-----|
| Readiness Score | Proxy de HRV |
| Sleep Score | Complemento |
| Stress | Proxy inverso de HRV |

---

## 8. RESUMEN EJECUTIVO

| Aspecto | Evidencia |
|---------|-----------|
| Horas óptimas | 8-10h para atletas |
| Realidad | Mayoría duerme 6-8h |
| Fuerza máxima | Se mantiene con déficit |
| Resistencia | Se reduce |
| Cognición | LO MÁS AFECTADO |
| Aprendizaje motor | Requiere sueño para consolidar |

> **Para el sistema**: Usar horas de sueño como input para autorregulación de volumen
