---
id: "REG-ENT-01"
nombre: "Programación de Entrenamiento"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "USR-02", "REG-ENT-02", "REG-LOG-01", "EVI-03", "EVI-14"]
tags: ["reglas", "entrenamiento", "programacion", "split"]
---

# Programación de Entrenamiento

## 1. Alcance
Reglas para estructurar la programación semanal: split, frecuencia, periodización y distribución de volumen.

> **Fuente**: Datos extraídos de `/evidencia/` (Schoenfeld, Bompa, Helms)

---

## 2. Variables del Usuario

> Referencia: `reglas/entrenamiento/preferencias.md`

| Variable | Valor | Fuente |
|----------|-------|--------|
| `DURACION_SESION_MIN` | 60 min | Usuario |
| `DURACION_SESION_IDEAL` | 75 min | Usuario |
| `DURACION_SESION_MAX` | 90 min | Usuario |
| `PREFERENCIA_EJERCICIOS` | Peso libre > Máquinas | Usuario |
| `CARDIO_SEPARADO` | Sí | Usuario |

| Variable | Valor | Fuente |
|----------|-------|--------|
| `SPLIT_TIPO` | Upper/Lower o PPL | Recomendado |
| `FRECUENCIA_SEMANAL` | 4-6 días | Recomendado |
| `PRIORIDAD_MUSCULAR` | Ver prioridades.md | Usuario |

---

## 3. Reglas de Volumen

> **Fuente**: Schoenfeld 2017 (dose-response meta-analysis)

### Evidencia Clave
- **10+ series/músculo/semana** = óptimo para hipertrofia
- Cada serie adicional = +0.37% ganancia
- Mínimo efectivo: 5 series/semana

### Asignación de Volumen por Prioridad
| Prioridad | Grupos | Series/Semana | Justificación |
|-----------|--------|---------------|---------------|
| **P1-P3** (V-taper) | Hombros | 14-18 | Máxima prioridad estética |
| **P1-P3** | Espalda | 14-18 | V-taper + postura |
| **P1-P3** | Bíceps | 10-14 | Estética brazos |
| **P1-P3** | Tríceps | 10-14 | Estética brazos |
| **P1-P3** | Pecho | 10-14 | No priorizar sobre hombros |
| Secundario | Cuádriceps | 10-12 | Balance |
| Secundario | Isquios | 8-10 | Balance |
| Secundario | Abdominales | 6-10 | Estética |

---

## 4. Reglas de Frecuencia

> **Fuente**: Schoenfeld 2019 (frequency meta-analysis)

### Evidencia Clave
- La frecuencia NO afecta hipertrofia si el volumen está igualado
- Frecuencia es HERRAMIENTA para distribuir volumen
- 2x/semana permite más volumen por sesión que 1x

| Grupo | Frecuencia/Semana | Motivo |
|-------|-------------------|--------|
| Prioritarios | **2x/semana** | Distribuir 12-18 series |
| Secundarios | **1-2x/semana** | Menor volumen total |

---

## 5. Reglas de Descanso Entre Series

> **Fuente**: Schoenfeld 2016 (rest intervals study)

### Evidencia Clave
- **3 minutos > 1 minuto** para hipertrofia Y fuerza
- Mito desmentido: "descansos cortos = más hipertrofia"

| Tipo Ejercicio | Descanso | Justificación |
|----------------|----------|---------------|
| Compuestos pesados | **3-5 min** | Máxima recuperación |
| Compuestos secundarios | **2-3 min** | Balance tiempo/calidad |
| Aislamiento | **1.5-2 min** | Menor demanda neural |

---

## 6. Orden de Ejercicios en Sesión

> Basado en preferencias del usuario + evidencia

1. **Compuestos de peso libre** (preferencia usuario)
2. Compuestos secundarios / máquinas
3. Aislamiento de grupos prioritarios
4. Trabajo correctivo/postura (si aplica)
5. ~~Cardio~~ → En sesión separada (preferencia usuario)

---

## 7. Reglas de Periodización

> **Fuente**: Bompa 2019 (Periodization 6th ed)

### Estructura de Ciclos
| Ciclo | Duración | Contenido |
|-------|----------|-----------|
| Microciclo | 1 semana | Unidad básica |
| Mesociclo | 4-6 semanas | Bloque de entrenamiento |
| Macrociclo | 3-6 meses | Fase completa |

### Deload
| Parámetro | Valor | Fuente |
|-----------|-------|--------|
| Frecuencia | Cada **4-6 semanas** | Bompa |
| Reducción volumen | **40-50%** | Bompa |
| Reducción intensidad | 10-15% (opcional) | Bomba |
| Duración | 1 semana | Bompa |

### Progresión Intra-Mesociclo
```yaml
PROGRESION_RIR:
  semana_1: RIR 3-4
  semana_2: RIR 2-3
  semana_3: RIR 1-2
  semana_4: DELOAD (RIR 4-5)
```

---

## 8. Reglas de Intensidad (RIR)

> **Fuente**: Helms 2016 (RIR-RPE scale)

| Tipo de Serie | RIR Objetivo | RPE |
|---------------|--------------|-----|
| Series hipertrofia | **2-3** | 7-8 |
| Series fuerza | 1-2 | 8-9 |
| Series al fallo | 0 (usar con moderación) | 10 |

### Escala RIR-RPE
| RPE | RIR | Descripción |
|-----|-----|-------------|
| 10 | 0 | Fallo muscular |
| 9 | 1 | Quedaba 1 rep |
| 8 | 2 | Quedaban 2 reps |
| 7 | 3 | Quedaban 3 reps |

---

## 9. Integración con Motor de Pesos

Las cargas diarias son calculadas por `motor_pesos.md` basándose en:
- Métricas de hardware (sueño, HRV) → `usuario/metricas/hardware.md`
- Métricas subjetivas (energía, estrés) → `usuario/metricas/subjetivas.md`
- Historial de sesiones anteriores

---

## 10. Cardio

> **Fuente**: Wilson 2012 (Concurrent training meta-analysis)

### Evidencia Clave
- Correr **INTERFIERE** con hipertrofia (-31%)
- Bicicleta **NO INTERFIERE** significativamente
- Cardio debe ser **SEPARADO** de fuerza

| Parámetro | Recomendación |
|-----------|---------------|
| Modalidad | Bicicleta, remo, elíptica |
| Evitar | Correr (alta interferencia) |
| Frecuencia máx | 2-3x/semana |
| Timing | Días separados o post-fuerza |

---

## 11. Natación

> Referencia: [piscina.md](../natacion/piscina.md)

### Integración Semanal
| Parámetro | Valor |
|-----------|-------|
| Frecuencia | **2x/semana** (Martes y Jueves probable) |
| Duración | 1 hora (clase dirigida) |
| Gasto calórico | ~250 kcal/sesión (principiante) |

### Impacto en Programación
- **NO sustituye gym** para hipertrofia (sin sobrecarga progresiva)
- **SÍ beneficia postura** (extensión torácica, rotación hombros)
- **SÍ cuenta como cardio** bajo impacto
- Considerar menor volumen de hombros en días post-natación

### Distribución Semanal (DEFINITIVA)

> Split PPL + Hombros/Brazos (4 gym + 2 natación + 1 descanso)
> Justificación: prioridad V-taper (prioridades.md) requiere 14-18 ser/sem hombros+espalda → imposible con solo 2 Upper days.

```yaml
SEMANA_TIPO:
  lunes: GYM (PUSH - Pecho + Hombros + Tríceps)
  martes: NATACIÓN (clase)
  miercoles: GYM (PIERNA + Core)
  jueves: NATACIÓN (clase)
  viernes: GYM (PULL - Espalda + Bíceps + Postura)
  sabado: GYM (HOMBROS + BRAZOS + Postura)
  domingo: DESCANSO
```

> **Nota**: Este split es FIJO todo el año. Lo que cambia por fase son los ejercicios específicos, series, reps y RIR (ver base_datos.md §7 - Pre-Generación).

---

## 12. Uso en el Sistema

```yaml
GENERADOR_PROGRAMA:
  1_leer_preferencias:
    - duracion_sesion: 60-90 min
    - split: upper_lower o PPL
    - frecuencia: 4-6 dias
    
  2_distribuir_volumen:
    - hombros: 14-18 series/semana
    - espalda: 14-18 series/semana
    - biceps: 10-14 series/semana
    - triceps: 10-14 series/semana
    - pecho: 10-14 series/semana
    
  3_asignar_frecuencia:
    - prioritarios: 2x/semana
    - secundarios: 1-2x/semana
    
  4_ordenar_ejercicios:
    - compuestos_peso_libre_primero
    
  5_cardio:
    - sesion_separada
    - preferir_bici
    
  6_natacion:
    - 2x/semana
    - no_sustituye_gym
    
  7_periodizacion:
    - mesociclo: 4 semanas
    - deload: semana 4
    - progresion_RIR: 4→3→2→deload
```
