---
id: "REG-NAT-01"
nombre: "Natación"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-PER-02", "REG-ENT-01", "EVI-04"]
tags: ["reglas", "natacion", "piscina", "movilidad", "cardio"]
---

# Natación

## 1. Alcance
Reglas para integrar las clases de natación en la programación semanal.

## 2. Datos de la Actividad

| Variable | Valor | Notas |
|----------|-------|-------|
| Frecuencia | **2 días/semana** | Martes y Jueves (probable, confirmar) |
| Duración | **1 hora** | Clase completa |
| Tipo | **Clases dirigidas** | Aprendizaje (no sabe nadar aún) |
| Distancia | ~3 minutos andando | Muy cerca de casa |
| Obligatoriedad | Flexible | Puede faltar ocasionalmente |
| Nivel | Principiante | Objetivo: aprender a nadar |

## 3. Datos Fisiológicos (Referencia General)

> ⚠️ **Nota**: No hay papers específicos en el repo. Datos de referencia general.

### Gasto Calórico
| Intensidad | kcal/hora (70-80kg) | Notas |
|------------|---------------------|-------|
| Principiante (aprendizaje) | **200-300 kcal** | Mucho descanso, poca técnica |
| Moderado (nado continuo) | 400-500 kcal | Cuando sepa nadar |
| Intenso (intervalos) | 500-700 kcal | Nivel avanzado |

> **Para tu caso**: ~250 kcal/hora (principiante con paradas)

### Efecto en Postura
| Aspecto | Efecto | Relevancia para ti |
|---------|--------|-------------------|
| Extensión torácica | ✅ Positivo | Trabaja en cada brazada |
| Rotación hombros | ✅ Positivo | Rango completo |
| Hiperlordosis | ⚠️ Neutro/Variable | Depende del estilo |
| Fortalece core | ✅ Positivo | Estabilización en agua |

> **Sinergia**: La brazada de crol/espalda trabaja exactamente el patrón de wall angels.

### Efecto en Fuerza
| Aspecto | Efecto | Notas |
|---------|--------|-------|
| Fuerza máxima | ❌ No mejora | Resistencia muy baja |
| Hipertrofia | ❌ Mínima | Sin sobrecarga progresiva |
| Resistencia muscular | ✅ Mejora | Muchas repeticiones |
| Recuperación | ✅ Positivo | Bajo impacto, descomprime |

> **Conclusión**: Natación NO sustituye gym para hipertrofia. Es complemento para postura, cardio y recuperación.

## 4. Beneficios para las Prioridades

| Prioridad | Beneficio | Referencia |
|-----------|-----------|------------|
| P2 (Postura) | Extensión torácica, movilidad hombros | Movimiento de brazada |
| P4 (Flexibilidad) | Trabajo de rango completo | Todo el cuerpo |
| P5 (Estrés) | Efecto relajante del agua | Cardio de bajo impacto |
| P10 (Cardio) | Capacidad aeróbica | Sin impacto articular |

> **Sinergia con postura**: La natación trabaja extensión torácica y rotación de hombros — exactamente lo que necesitas para wall angels.

## 4. Integración con Gym

### Opción A: Días Separados (RECOMENDADA)
```yaml
SEMANA_TIPO_A:
  lunes: GYM (Push)
  martes: NATACIÓN + Movilidad post
  miercoles: GYM (Pull)
  jueves: NATACIÓN + Movilidad post
  viernes: GYM (Legs)
  sabado: GYM (Upper) o Descanso
  domingo: Descanso completo
```

### Opción B: Gym por la mañana + Natación tarde
```yaml
SEMANA_TIPO_B:
  # Si los horarios lo permiten
  martes_am: GYM
  martes_pm: NATACIÓN
  # Pero aumenta fatiga
```

> **Recomendación**: Opción A. Usar días de natación como recuperación activa del gym.

## 5. Movilidad Post-Natación

Después de cada clase de natación, añadir **10-15 min de movilidad específica**:

| Ejercicio | Duración | Objetivo |
|-----------|----------|----------|
| Wall slides (si puede) | 2x10 | Patrón de hombro |
| Face pulls con banda | 2x15 | Rotación externa |
| Cat-cow | 2x10 | Extensión torácica |
| Estiramiento pec en pared | 2x30s/lado | Abrir pecho |

> **Nota**: La natación + movilidad post = trabajo correctivo de postura sin sumar volumen al gym.

## 6. Ajustes de Nutrición

```yaml
DIA_NATACION:
  calorias: +150-200 kcal (1h natación ~300-400 kcal)
  timing:
    - Snack pre-natación: 1h antes (carbos ligeros)
    - Post-natación: proteína + carbos
  hidratacion: +500ml (aunque estés en agua, sudas)
```

## 7. Reglas para el Sistema

```yaml
NATACION_REGLAS:
  dias_fijos: [martes, jueves]  # Confirmar con usuario
  horario: "PENDIENTE"  # Añadir cuando confirme
  
  si_falta_natacion:
    - Puede hacer movilidad en casa
    - O añadir cardio ligero (caminar 30min)
    - NO sustituir por gym extra
    
  si_conflicto_con_gym:
    - Priorizar natación si es día fijo
    - Mover sesión gym a otro día
    - O reducir volumen gym ese día
    
  progresion:
    - Fase 1: Aprender a flotar/respirar
    - Fase 2: Técnica básica
    - Fase 3: Nadar continuo 20+ min
    # Cuando sepa nadar → puede ser cardio LISS
```

## 8. Pendiente Confirmar

- [ ] Días exactos de las clases
- [ ] Horario (mañana/tarde)
- [ ] Fecha de inicio de las clases

## 9. Uso en el Sistema

1. `plan_anual_2026.md` debe incluir natación 2x/semana
2. `horarios.md` debe tener los días/horas fijos
3. La app muestra movilidad post-natación esos días
4. El motor de dieta añade +150-200 kcal días de natación
