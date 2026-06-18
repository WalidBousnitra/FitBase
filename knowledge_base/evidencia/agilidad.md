---
id: "EVI-09"
nombre: "Evidencia: Agilidad"
fecha_modificacion: "17/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-04", "EVI-10"]
tags: ["evidencia", "agilidad", "pliometria", "COD", "velocidad"]
prioridad: 9
---

# Agilidad

> **Prioridad #9** — Alineado con [prioridades.md](../usuario/prioridades.md)

## 1. Alcance
Evidencia científica sobre agilidad, coordinación neuromuscular y velocidad de reacción.

---

## 2. ENTRENAMIENTO PLIOMÉTRICO Y CAMBIO DE DIRECCIÓN

### Fuente Principal
**Asadi, A., Arazi, H., Young, W.B. & Saez de Villarreal, E.**  
"The Effects of Plyometric Training on Change of Direction Ability: A Meta-Analysis"  
*International Journal of Sports Physiology and Performance*

---

## 3. DEFINICIONES

### Agilidad
> Movimiento rápido de todo el cuerpo con cambio de velocidad o dirección **en respuesta a un estímulo**.

### Cambio de Dirección (COD)
> Movimiento donde el cambio de dirección es **pre-planificado** (sin reacción a estímulo).

### Pliometría
> Ejercicios que usan el **ciclo estiramiento-acortamiento (SSC)**: contracción excéntrica seguida inmediatamente de concéntrica.

---

## 4. HALLAZGOS DEL META-ANÁLISIS

### Efectividad General

> **El entrenamiento pliométrico ES EFECTIVO para mejorar la capacidad de cambio de dirección.**

### Factores que Influyen en el Efecto

| Factor | Hallazgo |
|--------|----------|
| **Nivel de fitness** | Mayor beneficio en individuos con buen fitness |
| **Deporte** | Baloncesto > otros deportes |
| **Sexo** | Hombres = Mujeres (similar beneficio) |

---

## 5. PROTOCOLO ÓPTIMO

### Parámetros de Entrenamiento

| Variable | Óptimo |
|----------|--------|
| **Duración** | **7 semanas** |
| **Frecuencia** | **2 sesiones/semana** |
| **Intensidad** | **Moderada** |
| **Volumen** | **~100 saltos/sesión** |
| **Descanso entre sesiones** | **72 horas** |

### Tipo de Ejercicios

| Combinación | Efectividad |
|-------------|-------------|
| **DJ + VJ + SLJ** (variedad) | **MEJOR** |
| Solo un tipo | Menos efectivo |

**Leyenda:**
- DJ = Drop Jump (salto desde altura)
- VJ = Vertical Jump (salto vertical)
- SLJ = Standing Long Jump (salto largo parado)

### Especificidad

| Ejercicios | Efecto en COD |
|------------|---------------|
| Saltos verticales (no específicos) | Sin efecto |
| Saltos laterales, bounds, ángulos | **Mejor efecto** |

> **Clave**: Los ejercicios deben ser ESPECÍFICOS al movimiento de cambio de dirección.

---

## 6. MECANISMOS

### Ciclo Estiramiento-Acortamiento (SSC)

| Componente | Función |
|------------|---------|
| Fase excéntrica | Almacena energía elástica |
| Fase concéntrica | Libera energía + fuerza activa |
| Resultado | Mayor fuerza en menor tiempo |

### Adaptaciones Neurales

| Adaptación | Efecto |
|------------|--------|
| Coordinación intermuscular | Mejor sincronización |
| Activación de unidades motoras | Más rápida |
| Stiffness muscular | Optimizado |

---

## 7. PROGRESIÓN RECOMENDADA

### Semanas 1-2: Base
```yaml
ejercicios:
  - saltos_en_sitio
  - skipping
  - saltos_bipodales_bajos
volumen: 60-80 saltos/sesion
```

### Semanas 3-4: Desarrollo
```yaml
ejercicios:
  - saltos_laterales
  - bounds
  - box_jumps_bajos
volumen: 80-100 saltos/sesion
```

### Semanas 5-7: Específico
```yaml
ejercicios:
  - drop_jumps
  - saltos_angulares
  - cambios_direccion_explosivos
volumen: 100 saltos/sesion
```

---

## 8. EJERCICIOS ESPECÍFICOS PARA COD

### Ejercicios Laterales (Más Efectivos)

| Ejercicio | Descripción |
|-----------|-------------|
| Lateral bounds | Saltos laterales amplios |
| Side hops | Saltos laterales rápidos |
| Angle hops | Saltos en ángulos diversos |
| Shuffle to sprint | Desplazamiento lateral → sprint |

### Ejercicios de Potencia General

| Ejercicio | Beneficio |
|-----------|-----------|
| Drop jump | Potencia reactiva |
| Box jump | Potencia concéntrica |
| Broad jump | Potencia horizontal |

---

## 9. RECOMENDACIONES PARA SISTEMA

```yaml
ENTRENAMIENTO_AGILIDAD:
  frecuencia: 2x/semana
  duracion: 7+ semanas
  volumen: 100 saltos/sesion
  descanso: 72h entre sesiones
  
SELECCION_EJERCICIOS:
  priorizar:
    - saltos_laterales
    - bounds
    - ejercicios_angulares
  incluir:
    - drop_jumps
    - vertical_jumps
    - standing_long_jumps
  evitar:
    - solo_saltos_verticales
    
PROGRESION:
  semanas_1-2: base_bilateral
  semanas_3-4: desarrollo_lateral
  semanas_5-7: especifico_COD
  
INTEGRACION_CON_FUERZA:
  - pliometria_post_fuerza_maxima
  - no_mismo_dia_que_piernas_pesado
  - complementa_no_reemplaza
```

---

## 10. ADVERTENCIAS

| Aspecto | Consideración |
|---------|---------------|
| Fatiga | No hacer con fatiga acumulada |
| Superficie | Firme pero con algo de absorción |
| Calzado | Apropiado para impacto |
| Técnica | Priorizar antes de volumen |

---

## 11. RESUMEN EJECUTIVO

| Aspecto | Evidencia |
|---------|-----------|
| Pliometría mejora COD | **SÍ** (meta-análisis) |
| Protocolo óptimo | 7 sem, 2x/sem, 100 saltos |
| Ejercicios específicos | Laterales > verticales |
| Variedad | Combinar tipos es MEJOR |

> **Para el sistema**: Incluir bloque de pliometría de 7 semanas, 2x/semana, con ejercicios laterales y variados.
*Completar tras revisar papers*

## 5. Implicaciones para Reglas
*Ejercicios de agilidad a incluir, frecuencia, integración con entrenamiento de fuerza*
