---
id: "EVI-04"
nombre: "Evidencia: Flexibilidad"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-02", "REG-ENT-01"]
tags: ["evidencia", "flexibilidad", "movilidad", "estiramientos", "ROM"]
prioridad: 4
---

# Flexibilidad

> **Prioridad #4** — Alineado con [prioridades.md](../usuario/prioridades.md)

## 1. Alcance
Evidencia científica sobre flexibilidad, movilidad articular y protocolos de estiramiento.

## 2. Subáreas Prioritarias (de prioridades.md)
1. Tríceps (extensión overhead limitada)
2. Ingle (aductores, flexores de cadera)

---

## 3. FUERZA VS ESTIRAMIENTOS PARA ROM

### Fuente
**Afonso, Ramirez-Campillo et al. (2020)**  
"Strength training is as effective as stretching for improving range of motion: A systematic review and meta-analysis"  
*Meta-análisis de 11 RCTs (n=452)*

### Hallazgo Principal

> **El entrenamiento de FUERZA es TAN EFECTIVO como estirar para mejorar ROM**

| Comparación | Effect Size | p-value | Resultado |
|-------------|-------------|---------|-----------|
| Fuerza vs Estiramiento | ES = -0.22 | p = 0.206 | **No diferencia significativa** |

### Sub-análisis (todos sin diferencia)

| Variable | Resultado |
|----------|-----------|
| ROM activo vs pasivo | Sin diferencia |
| Flexión de cadera | Sin diferencia |
| Extensión de rodilla | Sin diferencia |
| Calidad de estudios (RoB) | Sin diferencia |

### Mecanismos de Mejora ROM por Fuerza

| Mecanismo | Efecto |
|-----------|--------|
| ↑ Longitud fascicular | Concéntrico + excéntrico |
| Mejor co-activación agonista-antagonista | Control motor |
| Inhibición recíproca | Menor resistencia |
| Ciclo estiramiento-acortamiento | Mayor eficiencia |

### Implicaciones Prácticas

```yaml
CONCLUSIONES_CLAVE:
  - Estirar NO es estrictamente necesario para ROM
  - Entrenamiento de fuerza = alternativa válida
  - Consistente independiente de población/protocolo
  
APLICACION:
  si_no_responde_estiramiento: cambiar_a_fuerza
  si_no_adhiere_estiramiento: cambiar_a_fuerza
  contraindica_estirar: usar_solo_fuerza
```

---

## 4. RECOMENDACIONES PARA SISTEMA

### Protocolo de Flexibilidad Basado en Evidencia

| Opción | Método | Eficacia para ROM |
|--------|--------|-------------------|
| **A** | Entrenamiento de fuerza (ROM completo) | ✅ Igual de efectivo |
| **B** | Estiramientos | ✅ Efectivo |
| **C** | Combinación | ✅ Efectivo |

### Para Zonas Prioritarias del Usuario

| Zona | Opción Fuerza | Opción Estiramiento |
|------|---------------|---------------------|
| **Tríceps** | Press francés ROM completo | Estiramiento overhead |
| **Aductores** | Sentadilla sumo profunda | Estiramiento mariposa |
| **Flexores cadera** | Zancadas profundas | Estiramiento psoas |

### Decisión del Sistema

```yaml
LOGICA_FLEXIBILIDAD:
  preferencia_usuario: "combinacion"  # Fuerza ROM completo + estiramientos post
  
  si_preferencia == "no_estirar":
    usar: "fuerza_ROM_completo"
    evidencia: "Afonso_2020"
    
  si_preferencia == "tiempo_limitado":
    usar: "fuerza_ROM_completo"
    razon: "mata dos pájaros"
    
  default:
    usar: "combinacion"
    fuerza: "ejercicios ROM completo"
    estirar: "post-entreno 2-3min/zona"
```

---

## 5. PROTOCOLOS DE ESTIRAMIENTO (Page 2012, Bandy 1997)

### Fuentes Adicionales
- **Page, P. (2012)** - "Current concepts in muscle stretching for exercise and rehabilitation" - IJSPT
- **Bandy, W.D. et al. (1997)** - "The effect of time and frequency of static stretching..."
- **ACSM Guidelines** - American College of Sports Medicine

### Parámetros Óptimos de Estiramiento Estático

| Parámetro | Recomendación | Fuente |
|-----------|---------------|--------|
| **Duración** | 15-30 segundos | Page 2012, Bandy 1997 |
| **Repeticiones** | 2-4 por músculo | Page 2012 |
| **Frecuencia** | 2-3 días/semana mínimo | ACSM |
| **Adultos mayores** | 60 segundos | Feland 2001 |

> **DATO CLAVE (Bandy 1997)**: 30 segundos es igual de efectivo que 60 segundos. No hay beneficio adicional al estirar más tiempo.

### Tipos de Estiramiento

| Tipo | Descripción | Cuándo Usar |
|------|-------------|-------------|
| **Estático** | Mantener posición 15-30s | Post-entreno, recuperación |
| **Dinámico** | Movimientos controlados | Pre-entreno (calentamiento) |
| **PNF/Contract-Relax** | Contracción 75-100% + estiramiento | Ganancia rápida de ROM |
| **Balístico** | Rebotes | ⚠️ NO recomendado |

### Estiramiento y Rendimiento

| Momento | Tipo | Efecto en Rendimiento |
|---------|------|----------------------|
| **Pre-entreno** | Estático | ❌ Pérdida de fuerza ("stretch-induced strength loss") |
| **Pre-entreno** | Dinámico | ✅ Mejora potencia y salto |
| **Post-entreno** | Estático | ✅ Seguro, mejora ROM |
| **Sesión separada** | Cualquiera | ✅ Óptimo para flexibilidad |

### Hallazgos Clave del Meta-análisis

```yaml
ESTIRAMIENTO_ESTATICO:
  duracion_optima: 30 segundos
  sin_beneficio_adicional: "> 30 segundos"
  sin_beneficio_adicional: "> 1 vez/día"
  mecanismo: aumento_tolerancia_al_estiramiento
  no_es: alargamiento_muscular_real

ESTIRAMIENTO_DINAMICO:
  efecto_fuerza: sin_deficit
  efecto_potencia: puede_mejorar
  efecto_salto: puede_mejorar
  ideal_para: calentamiento_pre_entreno

PNF_CONTRACT_RELAX:
  contraccion: 75-100% max o 20-60% (igual efectivo)
  duracion_contraccion: 3-6 segundos
  seguido_de: estiramiento_pasivo
  resultado: mayor_ROM_que_estatico
```

### Protocolo Recomendado Basado en Evidencia

```yaml
PRE_ENTRENO:
  tipo: dinamico
  duracion: 5-10 minutos
  movimientos: circulos_articulares, balanceos, sentadillas_aire
  NO_HACER: estiramientos_estaticos

POST_ENTRENO:
  tipo: estatico
  duracion_por_musculo: 30 segundos
  repeticiones: 2-4
  musculos: principales_trabajados + zonas_tensas

SESION_FLEXIBILIDAD:
  frecuencia: 2-3 dias/semana
  tipo: estatico o PNF
  duracion_total: 15-30 minutos
  timing: separada de fuerza o post-entreno
```

---

## 6. STRETCH-INDUCED STRENGTH LOSS

### Fenómeno (Page 2012)

> El estiramiento estático antes del ejercicio puede reducir la fuerza y el rendimiento.

| Efecto | Magnitud |
|--------|----------|
| Pérdida de fuerza | Documentada en dinamómetro |
| Pérdida de rendimiento | Salto, carrera |
| Causas posibles | Factores neurales y mecánicos |

### Cómo Evitarlo

| Estrategia | Efectividad |
|------------|-------------|
| Usar estiramiento dinámico pre-entreno | ✅ |
| Contraer antes de estirar | ✅ Reduce pérdida |
| Limitar volumen (≤4 reps x 15s) | ✅ Sin efecto en salto vertical |
| Estirar después del calentamiento activo | ✅ No disminuye fuerza |

---

## 7. RESUMEN EJECUTIVO

| Mito | Realidad (Evidencia) |
|------|----------------------|
| "Hay que estirar para ser flexible" | Fuerza funciona igual |
| "Fuerza te acorta" | Fuerza con ROM completo MEJORA ROM |
| "Estirar es obligatorio" | Es UNA opción, no la única |
| "Más tiempo = mejor" | 30s = 60s (Bandy) |
| "Estirar antes de entrenar" | Estático NO, dinámico SÍ |

### Tabla de Decisión Rápida

| Objetivo | Recomendación |
|----------|---------------|
| Mejorar ROM | Fuerza ROM completo O estirar |
| Pre-entreno | Calentamiento dinámico |
| Post-entreno | Estático 30s x 2-4 reps |
| Ganancia rápida ROM | PNF/Contract-relax |
| Adultos mayores | Estático 60s |

> **Recomendación Final**: Priorizar calentamiento dinámico pre-entreno. Estirar estático post-entreno o en sesión separada. 30 segundos por músculo es suficiente.
