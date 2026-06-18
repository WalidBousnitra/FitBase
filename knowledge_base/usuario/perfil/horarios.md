---
id: "USR-PER-02"
nombre: "Horarios y Disponibilidad"
fecha_modificacion: "17/06/2026"
estado: "PROD_ACTUAL"
relacionados: ["USR-PER-01", "USR-01", "REG-ENT-01"]
tags: ["perfil", "horarios", "cronotipo", "trabajo"]
---

# Horarios y Disponibilidad

## 1. Alcance
Ventanas de tiempo disponibles para entrenamiento, comidas y descanso. Define cuándo el sistema puede programar actividades.

## 2. Cronotipo y Sueño

| Variable | Valor |
|----------|-------|
| Cronotipo | **Matutino** (persona de mañanas) |
| Horas sueño ideal | 8h |
| Horas sueño real | ~7h (déficit crónico leve) |

### Horarios de Sueño por Día
| Día | Despertar | Acostarse | Horas Sueño |
|-----|-----------|-----------|-------------|
| Lunes | 8:30 | 23:00 | ~7h (teletrabajo) |
| Martes | 7:00 | 23:00 | ~8h |
| Miércoles | 7:00 | 23:00 | ~8h |
| Jueves | 7:00 | 23:00 | ~8h |
| Viernes | 8:30 | Variable (tarde) | ~6-7h |
| Sábado | 10:00 | Variable (tarde) | Variable |
| Domingo | 10:00 | 23:00 | ~8h |

```yaml
SUENO:
  objetivo: 8h
  realidad: 7h promedio
  deficit_semanal: ~7h (1h/día × 7 días)
  peor_dia: "Viernes noche → Sábado (trasnochar)"
  mejor_dias: "M, X, J (madrugar pero acostar temprano)"
  ajuste_sistema:
    - Priorizar recuperación de sueño en fines de semana
    - Motor de pesos debe considerar déficit crónico leve
```

## 3. Horario Laboral

### Estructura Semanal
| Día | Modalidad | Horario | Horas |
|-----|-----------|---------|-------|
| Lunes | **Teletrabajo** | 9:00 - 19:00 | 10h |
| Martes | Oficina | 9:00 - 19:00 | 10h |
| Miércoles | Oficina | 9:00 - 19:00 | 10h |
| Jueves | Oficina | 9:00 - 19:00 | 10h |
| Viernes | **Teletrabajo** | 9:00 - 15:00 | 6h |

```yaml
TRABAJO:
  tipo: "Oficina (sedentario)"
  sector: "Consultoría financiera tecnológica"
  horas_semanales: 46h
  
  desplazamiento:
    actual: "2h/día (1h ida + 1h vuelta)"
    futuro: "1h/día (con coche)"
    dias_desplazamiento: ["Martes", "Miércoles", "Jueves"]
  
  gasto_energetico:
    nivel: "Bajo (sedentario)"
    compensacion: "Intenta moverse hacia escritorios de compañeros"
    NEAT_estimado: "Bajo en oficina, moderado en teletrabajo"
  
  teletrabajo:
    dias: ["Lunes", "Viernes"]
    ventaja: "Más tiempo libre, mejor para entrenar"
```

### Tiempo Libre por Día (para entrenar)
| Día | Tiempo Libre | Notas |
|-----|--------------|-------|
| Lunes | **Alto** | Teletrabajo, sin desplazamiento |
| Martes | Bajo | Oficina + 2h desplazamiento |
| Miércoles | Bajo | Oficina + 2h desplazamiento |
| Jueves | Bajo | Oficina + 2h desplazamiento |
| Viernes | **Alto** | Teletrabajo + jornada corta (termina 15:00) |
| Sábado | **Máximo** | Libre |
| Domingo | **Máximo** | Libre (cuscús familiar) |

## 4. Ventanas de Entrenamiento

### Preferencia Personal
> **NO le gusta ir al gimnasio después de trabajar en oficina.** Intentar reducir al mínimo.

### Ventanas Óptimas por Día
| Día | Ventana Óptima | Alternativa | Prioridad |
|-----|----------------|-------------|-----------|
| Lunes | 7:00-8:30 (mañana) | 19:30-21:00 | ⭐⭐⭐ Ideal |
| Martes | ❌ Evitar | 19:30-21:00 (si no queda otra) | ⭐ Peor día |
| Miércoles | ❌ Evitar | 19:30-21:00 (si no queda otra) | ⭐ Peor día |
| Jueves | ❌ Evitar | 19:30-21:00 (si no queda otra) | ⭐ Peor día |
| Viernes | 15:30-18:00 | Mañana 7:00-8:30 | ⭐⭐⭐ Ideal |
| Sábado | 10:30-13:00 | Cualquier hora | ⭐⭐⭐ Ideal |
| Domingo | 10:30-13:00 | Evitar tarde (cuscús) | ⭐⭐ Bueno |

```yaml
ENTRENO_VENTANAS:
  preferencia: "Mañanas o días de teletrabajo"
  evitar: "Post-trabajo en días de oficina (M, X, J)"
  
  dias_ideales: ["Lunes", "Viernes", "Sábado"]
  dias_aceptables: ["Domingo mañana"]
  dias_evitar: ["Martes", "Miércoles", "Jueves"]
  
  split_sugerido:
    opcion_1: "L-V-S (3 días, alta frecuencia)"
    opcion_2: "L-V-S-D (4 días)"
    opcion_3: "L-X-V-S (4 días, 1 día oficina inevitable)"
  
  duracion_maxima:
    dias_trabajo: 60 min
    fines_semana: 90 min
```

## 5. Horarios de Comidas

### Estructura Diaria
| Comida | Hora | Lugar | Notas |
|--------|------|-------|-------|
| Desayuno | 7:00 | Casa | Antes de salir |
| Café/Snack | 11:00 | Trabajo | Media mañana |
| Comida | 14:00 | Trabajo (tupper) / Casa (teletrabajo) | Comida principal |
| Cena | 21:00 | Casa | Segunda comida principal |

```yaml
COMIDAS:
  estructura: "Fija (predecible)"
  
  distribucion:
    desayuno_7: "Ligero-moderado"
    snack_11: "Café + algo pequeño"
    comida_14: "Comida principal 1"
    cena_21: "Comida principal 2"
  
  preparacion:
    desayuno: "Casa (rápido)"
    comida_oficina: "Tupper (meal prep)"
    comida_teletrabajo: "Casa (más flexible)"
    cena: "Casa"
  
  timing_entreno:
    si_entreno_manana: "Desayuno ligero pre → Desayuno completo post"
    si_entreno_tarde: "Snack 16:00 → Entreno → Cena post"
```

## 6. Compromisos Fijos

### Semanales
| Día | Compromiso | Horario | Impacto |
|-----|------------|---------|---------|
| Viernes | Quedada con amigos | Noche | Cena fuera, trasnochar |
| Domingo | Cuscús familiar | Mediodía-tarde | Comida alta en carbos |

### Estacionales
- **Ramadán**: Horarios de comida cambian completamente (ver cultura.md)
- **Eid**: Días festivos con comidas familiares

```yaml
COMPROMISOS:
  viernes_noche:
    tipo: "Social (amigos)"
    impacto_sueno: "Acostarse tarde"
    impacto_dieta: "Coca-Cola Zero + bravas (controlado)"
    ajuste: "No programar entreno sábado temprano"
  
  domingo_cuscus:
    tipo: "Familiar"
    impacto_dieta: "Comida alta en carbos (~1000 kcal)"
    ajuste: "Planificar como día alto en carbos"
```

## 7. Resumen para el Sistema

### Variables Clave
```yaml
HORARIOS_SISTEMA:
  # Sueño
  despertar_semana: "7:00-8:30"
  despertar_finde: "10:00"
  acostar_semana: "23:00"
  sueno_promedio: 7h
  
  # Trabajo
  dias_oficina: ["Martes", "Miércoles", "Jueves"]
  dias_teletrabajo: ["Lunes", "Viernes"]
  fin_jornada_normal: "19:00"
  fin_jornada_viernes: "15:00"
  
  # Entrenamiento
  ventana_ideal: "Mañanas o post-teletrabajo"
  evitar_entreno: "19:00-21:00 en M/X/J"
  dias_entreno_recomendados: ["Lunes", "Viernes", "Sábado"]
  
  # Comidas
  horario_fijo: true
  comidas: [7:00, 11:00, 14:00, 21:00]
  
  # Notificaciones
  no_molestar: "23:00 - 7:00"
  recordatorio_entreno: "Solo en ventanas definidas"
```

## 8. Uso en el Sistema
1. `REG-ENT-01` programa sesiones en ventanas óptimas (L, V, S)
2. `REG-NUT-01` distribuye macros según horario de comidas fijo
3. `REG-LOG-01` ajusta cargas considerando déficit de sueño crónico
4. La UI no envía notificaciones fuera de horarios permitidos
