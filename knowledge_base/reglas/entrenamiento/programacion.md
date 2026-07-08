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
| Reducción intensidad | 10-15% (opcional) | Bompa |
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

### Rebalanceo de Volumen (2026-b)

> Los 4 días de gym (arriba) son fijos, pero la distribución de series
> DENTRO de cada día se auditó contra §3 usando el `str_grupo_principal`
> real del catálogo de Codigo.gs — no una clasificación aproximada.

**Hallazgo**: Hombros llegaba a 28 ser/sem (Push 14 + Hombros 14), 55-80% por
encima de su propio techo ya-elevado-por-prioridad (14-18). Causa: Press
hombro mancuernas y Elev. laterales sentado se repetían SIN variar ángulo en
AMBOS días de gym que tocan hombro. El hombro es además sinergista en Press
inclinado (pecho) y en Dominadas/Remo (espalda) — la fatiga sistémica real es
mayor que el recuento de series directas. La prioridad (hombros > pecho, etc.)
debe fijar DÓNDE te sitúas dentro de tu propio rango de la tabla de §3, no
multiplicar ese rango sin límite.

**Ajuste**: se quitó la redundancia (Elev. laterales polea sale de PUSH; Press
hombro mancuernas y Elev. laterales sentado salen de HOMBR), dejando cada día
con ángulos/funciones distintas del hombro. El tiempo liberado se usó para
subir Pecho (4→7 ser/sem, cruza el mínimo efectivo de 5) reintroduciendo
Cruces polea alta (ya en el catálogo, favorito, solo se había quitado por
presupuesto de tiempo de sesión). Core NO se tocó: `T.PIERNA_VOL` (Hollow, 3
ser) + `getCoreDia_()` del día de descanso (Hollow+Pallof, 6 contadas) ya
suman ~9 ser/sem, dentro de 6-10 — añadir Pallof también en Pierna habría
duplicado el mismo ejercicio en 2 días sin necesidad.

| Grupo | Antes | Después | Rango evidencia (§3) |
|---|---|---|---|
| Hombros | 28 | 16 (18 con MAV) | 14-18 |
| Espalda | 15 | 15 (sin cambio) | 14-18 |
| Bíceps | 12 | 12 (sin cambio) | 10-14 |
| Tríceps | 9 | 9 (sin cambio) | 10-14 |
| Pecho | 4 | 7 | 10-14 (deliberadamente bajo — prioridades.md: no priorizar sobre hombros) |
| Core | ~9 (gym+descanso) | ~9 (sin cambio) | 6-10 |

Ver Codigo.gs — comentario sobre `const T` — para el detalle ejercicio por
ejercicio y el razonamiento completo.

> **Nota**: Este split es FIJO todo el año. Lo que cambia por fase son los ejercicios específicos, series, reps y RIR (ver base_datos.md §7 - Pre-Generación).

---

## 12. Flujo Diario del Usuario

### Vista Mañana (al abrir app)
```yaml
PANTALLA_MANANA:
  datos_mostrados:
    - sueño: "Sleep Score + horas (de Health Connect)"
    - macros_dia: "Calorías, P, C, G según fase actual"
    - agua_objetivo: "peso × 35 ml + 500 si entreno"
    - cardio_objetivo: "Pasos/minutos → decidido por fase (§13, Wilson 2012)"
    - movilidad_matutina: "SIEMPRE (Ruivo 2017: frecuencia DIARIA para corrección postural)"
    - tipo_sesion: "Push/Pull/Pierna/Hombros o Natación o Descanso"
  
  REGLA_DECISIONES: >
    Cada campo se muestra o no según la evidencia, NO por prompt del usuario.
    - Movilidad: SIEMPRE → Ruivo 2017 (protocolo 16 sem requiere diario)
    - Cardio: SOLO si la fase lo requiere → Wilson 2012, Viana 2019 (§13)
    - Macros: SIEMPRE → nutrición obligatoria independiente del día
    - Estiramientos post-gym: SOLO días de gym → Page 2012 (post-entreno)
    Si la ciencia no justifica una acción para ese día, NO se muestra.
```

### Durante el Día
```yaml
PANTALLA_DIA:
  macros_restantes: "Objetivo - consumido (FatSecret → Health Connect)"
  agua: "Recordatorio visual siempre presente"
  nota: "La app NO trackea comidas — se lee de Health Connect/FatSecret"
```

### Sesión de Gimnasio
```yaml
FLUJO_SESION:
  1_iniciar: "Botón 'Empezar Entreno'"
  2_calentamiento:
    - movilidad_dinamica: "5 min (ver calentamiento.md §3)"
    - activacion_especifica: "2-3 min según tipo sesión"
  3_ejercicios:
    - por_cada_ejercicio:
      - mostrar: "nombre, peso sugerido (motor), reps objetivo, RIR objetivo"
      - por_cada_serie:
        - usuario_registra: "reps_completadas + sensación (fácil/bien/duro/fallo)"
        - timer_descanso: "Inicia automáticamente al guardar serie"
        - timer_flotante: "Overlay en Hyper Island — visible fuera de la app"
        - fin_timer: "Notificación visual + vibración → siguiente serie"
  4_estiramientos:
    - post_entreno: "Estático 30-60s/grupo trabajado (Page 2012)"
  5_cardio_si_aplica:
    - ver_seccion_13: "Solo en fases con cardio obligatorio post-entreno"
  6_resumen:
    - mostrar: "Volumen total, peso movido, RPE medio"
    - impacto: "Cómo afecta a la progresión del plan anual"
    - cerrar: "App se cierra hasta mañana"
```

### Días de Natación (Martes/Jueves)
```yaml
FLUJO_NATACION:
  manana: "Movilidad matutina SIEMPRE (Ruivo 2017: diaria para corrección)"
  sesion: "Clase dirigida 1h — NO se trackea en la app"
  nota: "No hay ejercicios de gym. Movilidad matutina sí porque es independiente."
  justificacion: "Ruivo 2017: la frecuencia diaria es requisito del protocolo correctivo"
```

### Días de Descanso (Domingo)
```yaml
FLUJO_DESCANSO:
  manana: "Movilidad matutina SIEMPRE (Ruivo 2017) + cardio SOLO si fase lo exige (§13)"
  contenido:
    - movilidad_postura: "Rutina correctiva completa (§14) — diaria por protocolo"
    - cardio_suave: "SOLO si fase = DEF o MNT (Wilson 2012: bici no interfiere)"
  justificacion_movilidad: "Ruivo 2017: sin frecuencia diaria no hay corrección postural"
  justificacion_cardio: "Wilson 2012 + Viana 2019: solo en déficit/mantenimiento se añade"
  nota: "Sin intensidad alta, objetivo = recuperación activa"
```

---

## 13. Cardio y Pasos por Fase

> **Fuente**: Wilson 2012 — bicicleta no interfiere. Cardio ≤3x/sem ≤30 min.
> La natación 2x/sem CUENTA como cardio bajo impacto.

### Objetivos por Fase
| Fase (tipo) | Pasos/día | Cardio extra (gym) | Justificación |
|-------------|-----------|-------------------|---------------|
| **VOL** (bulk) | 8.000 | 0 min | Minimizar interferencia (Wilson 2012) |
| **FZA** (bulk) | 8.000 | 0 min | Priorizar recuperación neural |
| **DEF** (cut) | 10.000 | 15-20 min post-gym (bici) | Aumentar NEAT + déficit (Viana 2019) |
| **MNT** (mantener) | 9.000 | 10 min post-gym (opcional) | Balance |
| **DELOAD** | 7.000 | 0 min | Recuperación total |

### Reglas de Cardio
```yaml
CARDIO_REGLAS:
  modalidad: "bici estática o elíptica (Wilson 2012: no interfiere)"
  evitar: "correr (Wilson 2012: SÍ interfiere -31% hipertrofia)"
  timing: "post-entreno de gym o en día separado"
  natacion_cuenta: true  # 2x/sem ya cubre cardio bajo impacto
  intensidad: "60-70% FC máx (LISS) — Viana 2019: igual pérdida grasa que HIIT"
```

---

## 14. Movilidad Matutina (Prioridad #2: Postura)

> **Fuente**: Ruivo 2017 (protocolo 16 sem), Hansraj 2014 (estrés cervical), Afonso 2020 (fuerza = ROM)
> Esta rutina es INDEPENDIENTE del gym. Se hace al despertar, 5-8 min.

### Rutina Base (todos los días)
| Ejercicio | Reps/Duración | Objetivo |
|-----------|---------------|----------|
| Retracción cervical (chin tucks) | 10 reps | P2: Forward head (Hansraj) |
| Extensión torácica en foam roller | 30s | P2: Hipercifosis |
| Cat-cow (gato-vaca) | 10 reps | Movilidad columna |
| Rotación externa con banda | 10/lado | P2: Hombros internos |
| Dead bugs | 10/lado | P2: Hiperlordosis/APT |

### Reglas
```yaml
MOVILIDAD_MATUTINA:
  duracion: "5-8 minutos"
  frecuencia: "DIARIA — sin excepciones (Ruivo 2017: protocolo 16 sem requiere consistencia)"
  cuando_NO: "Lesión activa en zona afectada O enfermedad aguda (fiebre, infección)"
  justificacion_diaria: >
    Ruivo 2017: el protocolo correctivo postural SOLO funciona con frecuencia diaria.
    Hansraj 2014: el estrés cervical por móvil es CONSTANTE → corrección debe ser constante.
    Conclusión: la app SIEMPRE muestra movilidad. No es decisión del usuario ni del prompt.
  progresion: "Cada 4 semanas añadir 1 ejercicio o reps (Ruivo 2017: progresión gradual)"
  vinculo_con_gym: "Los ejercicios correctivos del gym (face pulls, wall angels) complementan — NO sustituyen"
```

---

## 15. Uso en el Sistema

```yaml
GENERADOR_PROGRAMA:
  1_leer_preferencias:
    - duracion_sesion: 60-90 min
    - split: PPL + Hombros/Brazos
    - frecuencia: 4 días gym + 2 natación + 1 descanso
    
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
    
  5_cardio_por_fase:
    - VOL/FZA: solo pasos 8000
    - DEF: +15-20 min bici post-gym
    - MNT: +10 min opcional
    
  6_natacion:
    - 2x/semana (cuenta como cardio)
    - no_sustituye_gym
    
  7_periodizacion:
    - mesociclo: 4 semanas
    - deload: semana 4
    - progresion_RIR: 3-4→2-3→1-2→deload (VOL/DEF/MNT) | 2→2→1→deload (FZA)
```
