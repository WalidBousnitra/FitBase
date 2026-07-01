---
id: "EVI-16"
nombre: "Evidencia: Calentamiento"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-03", "REG-ENT-03"]
tags: ["evidencia", "calentamiento", "warmup", "activacion"]
prioridad: "soporte"
---

# Calentamiento

> **Soporte** — Base científica para protocolos de calentamiento pre-entreno

## 1. Alcance
Evidencia científica sobre efectos del calentamiento en el rendimiento muscular.

---

## 2. FUENTES PRINCIPALES

- **Rodrigues, P. et al. (2020)** - "Acute effect of three different warm-up protocols on maximal isokinetic strength in young men" - Rev Andal Med Deporte
- **Page, P. (2012)** - Estiramientos pre-entreno (ver flexibilidad.md)

---

## 3. TIPOS DE CALENTAMIENTO ESTUDIADOS

### Protocolos Comparados (Rodrigues 2020)

| Tipo | Descripción | Duración |
|------|-------------|----------|
| **General (GEWU)** | Cinta a 70% HR max | 10 min |
| **Estiramiento (SEWU)** | 2 sets x 30s estático | ~2 min |
| **Específico (SPWU)** | 15 reps a 50-55% max | ~1-2 min |
| **Control** | Sin calentamiento | — |

### Diseño del Estudio
- 22 hombres recreacionales (entrenamiento >2 años)
- Test: fuerza isocinética máxima extensores rodilla
- Diseño aleatorizado cruzado

---

## 4. HALLAZGOS PRINCIPALES

### Efecto en Fuerza Máxima

| Protocolo | Pico Torque Concéntrico | Pico Torque Excéntrico | Trabajo Total |
|-----------|-------------------------|------------------------|---------------|
| Control | Referencia | Referencia | Referencia |
| General | = | = | = |
| Estiramiento | = | = | = |
| **Específico** | **-12.94%** ⚠️ | = | = |

> **HALLAZGO SORPRENDENTE**: El calentamiento específico REDUJO el pico de torque concéntrico.

### Conclusión del Estudio

> "Ninguno de los protocolos de calentamiento fue capaz de alterar el trabajo total en fuerza isocinética máxima."

---

## 5. INTERPRETACIÓN PRÁCTICA

### Lo que NO hace el calentamiento
- NO aumenta la fuerza máxima inmediata
- NO mejora el rendimiento isocinético

### Lo que SÍ hace el calentamiento (otras fuentes)
- Reduce riesgo de lesiones
- Aumenta temperatura muscular
- Mejora velocidad de conducción nerviosa
- Aumenta flujo sanguíneo

### Por qué el calentamiento específico redujo fuerza

| Factor | Explicación |
|--------|-------------|
| Fatiga pre-test | 15 reps a 50-55% puede causar fatiga temporal |
| Timing | 60 segundos de descanso puede ser insuficiente |

---

## 6. INTEGRACIÓN CON ESTIRAMIENTOS (Page 2012)

### Pre-Entreno

| Tipo Estiramiento | Efecto en Rendimiento | Recomendación |
|-------------------|----------------------|---------------|
| **Estático** | ❌ Reduce fuerza ("stretch-induced strength loss") | EVITAR pre-entreno |
| **Dinámico** | ✅ Puede mejorar potencia/salto | USAR pre-entreno |
| PNF | ❌ Puede reducir fuerza | EVITAR pre-entreno |

---

## 7. PROTOCOLO RECOMENDADO

### Basado en Evidencia Combinada

```yaml
CALENTAMIENTO_OPTIMO:
  fase_1_general:
    tipo: cardio_ligero
    duracion: 5-10 min
    intensidad: 50-60% HR max
    opciones: [bici, eliptica, cinta_rapida]
    
  fase_2_activacion:
    tipo: dinamico
    duracion: 5 min
    movimientos:
      - circulos_articulares
      - balanceos_pierna
      - rotaciones_tronco
      - sentadillas_aire
      - lunges_dinamicos
    
  fase_3_especifico:
    tipo: series_aproximacion
    descripcion: "Series ligeras del primer ejercicio"
    ejemplo_press_banca:
      - set_1: "barra_vacia x 10-15"
      - set_2: "40% x 8"
      - set_3: "60% x 5"
      - set_4: "80% x 2-3"
      - set_trabajo: "peso_objetivo"
    descanso: 60-90s entre sets aproximacion

  NO_HACER:
    - estiramientos_estaticos_pre_entreno
    - demasiadas_reps_en_calentamiento_especifico
    - saltarse_calentamiento_general
```

### Series de Aproximación (Warm-up Sets)

| Ejercicio | Protocolo |
|-----------|-----------|
| **Compuestos pesados** (squat, bench, deadlift) | 3-4 series de aproximación |
| **Compuestos secundarios** (press militar, remo) | 2-3 series |
| **Aislamiento** | 1 serie ligera o ninguna |

```yaml
SERIES_APROXIMACION:
  compuesto_1_del_dia:
    - 40% x 10 (muy ligero)
    - 60% x 6
    - 75% x 3
    - 85% x 1-2
    - SET_TRABAJO
    
  ejercicios_siguientes:
    - 1_set_ligero si es nuevo movimiento
    - directo a peso si es mismo patron
```

---

## 8. TIEMPO TOTAL DE CALENTAMIENTO

| Componente | Duración |
|------------|----------|
| General | 5-10 min |
| Dinámico | 3-5 min |
| Series aproximación | 5-10 min |
| **TOTAL** | **15-25 min** |

---

## 9. RESUMEN EJECUTIVO

| Aspecto | Recomendación |
|---------|---------------|
| Cardio general | 5-10 min a baja intensidad |
| Movilidad | Dinámica, NO estática |
| Series aproximación | 3-4 sets progresivos para compuestos |
| Estiramientos estáticos | Solo POST-entreno |

> **Mensaje clave**: El calentamiento es para PREPARAR el cuerpo y PREVENIR lesiones, no para mejorar el rendimiento agudo. No te saltes las series de aproximación en ejercicios pesados.
