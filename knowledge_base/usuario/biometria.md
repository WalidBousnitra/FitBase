---
id: "USR-02"
nombre: "Biometría y Objetivos"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "USR-03", "USR-MET-01"]
tags: ["biometria", "objetivos", "medidas", "tracking", "xiaomi", "amazfit"]
---

# Biometría y Objetivos

## 1. Alcance
Todas las métricas corporales medibles. Fuente única de verdad para el estado físico actual y objetivos.

---

## 2. Datos Básicos (Fijos o Raramente Cambian)

| Métrica | Valor | Notas |
|---------|-------|-------|
| Fecha nacimiento | 20/07/2001 | — |
| Edad | 24 años | Calcular automáticamente |
| Sexo | Hombre | — |
| Altura | 188 cm | Potencial: **189-191 cm** si corrige postura |
| Envergadura | XXX | Brazos extendidos, pared a pared |
| Talla calzado | 45 EU | — |

---

## 3. Báscula Xiaomi (Bioimpedancia)

> ⚠️ **Nota**: Los datos de bioimpedancia son aproximados (±3-5% error). Útiles para ver tendencias, no valores absolutos.

### Métricas de Composición Corporal
| Métrica | Valor Actual | Fecha | Objetivo 12 meses | Rango Saludable |
|---------|--------------|-------|-------------------|-----------------|
| **Peso** | 78.2 kg | 18/06/2026 | **80-82 kg** | 70-90 kg para 188 cm |
| **Grasa corporal** | 18.9% | 18/06/2026 | **14-15%** | 10-20% (hombre fitness) |
| **Masa muscular** | 60.2 kg | 18/06/2026 | **+2-3 kg** | ↑ Cuanto más mejor |
| **Masa ósea** | ~3 kg | | — | ~2.5-3.5 kg (hombre) |
| **Agua corporal** | ~55% | | — | 50-65% |
| **Proteína** | ~18% | | — | 16-20% |
| **Grasa visceral** | 9 | 18/06/2026 | < 8 | 1-9 (saludable) |
| **Metabolismo basal** | ~1850 kcal | | — | Referencia para dieta |
| **Edad metabólica** | ~22 años | | < edad real ✅ | Menor = mejor |
| **IMC** | 22.1 | | 22-24 | 18.5-24.9 (normal) |

### Contexto del Usuario
```yaml
PERFIL_ENTRENAMIENTO:
  experiencia: "3 años (entrenamiento casual/inconsistente)"
  potencial: "Principiante-Intermedio (aún tiene ganancias rápidas)"
  genetica: "Autopercibida como buena"
  limitaciones: "Dolor crónico codo (evitar extensión completa bajo carga)"
```

### Objetivos por Fase (Ambicioso pero Realista)
```yaml
OBJETIVOS_12_MESES:
  # Basado en: 3 años casual = potencial de ~2-3kg músculo/año
  # Con buena adherencia, nutrición y sueño
  
  FASE_1_CUT (Sep-Nov 2026): # 12 semanas
    peso_inicial: 78.2 kg
    peso_objetivo: 74-75 kg (-3-4 kg)
    grasa_objetivo: 14-15%
    estrategia: "Déficit moderado -500 kcal, mantener proteína alta"
    resultado_visual: "Abdominales visibles, cara más definida"
    
  FASE_2_LEAN_BULK (Dic 2026 - May 2027): # 24 semanas
    peso_inicial: ~74 kg
    peso_objetivo: 78-80 kg (+4-6 kg)
    grasa_objetivo: "Mantener 15-16%"
    estrategia: "Superávit moderado +300 kcal, maximizar músculo"
    resultado_visual: "Más tamaño en hombros/brazos, V-taper visible"
    
  FASE_3_MINI_CUT (Jun-Jul 2027): # 8 semanas
    peso_inicial: ~79 kg
    peso_objetivo: 76-77 kg (-2-3 kg)
    grasa_objetivo: 12-13%
    estrategia: "Cut agresivo para verano"
    resultado_visual: "Look atlético funcional, abs marcados"

OBJETIVO_FINAL_VERANO_2027:
  peso: "76-78 kg"
  grasa: "12-14%"
  look: "Atlético funcional (tipo Brad Pitt Fight Club / Chris Hemsworth Thor 1)"
  postura: "Wall angel perfecto"
```

### Checkpoints de Progreso
| Fecha | Peso Esperado | Grasa % | Checkpoint |
|-------|---------------|---------|------------|
| 1 Oct 2026 | 77 kg | 17% | Inicio cut visible |
| 1 Dic 2026 | 74-75 kg | 14-15% | **FIN CUT - Abs visibles** |
| 1 Mar 2027 | 76-77 kg | 15% | Mitad bulk |
| 1 Jun 2027 | 78-80 kg | 15-16% | **FIN BULK - Máximo tamaño** |
| 1 Ago 2027 | 76-78 kg | 12-14% | **OBJETIVO FINAL - Beach ready** |

> 💡 **Nota**: Estos objetivos son ambiciosos pero alcanzables con adherencia >90% a entrenamiento y nutrición. Si la genética responde bien, podrías superar estas expectativas.

### Frecuencia de Medición
```yaml
BASCULA_XIAOMI:
  frecuencia: "Diaria (mañana, en ayunas, post-baño)"
  condiciones:
    - Mismo momento del día
    - Después de ir al baño
    - Antes de desayunar
    - Pies secos
  tracking:
    peso: "Diario (media semanal)"
    composicion: "Semanal (domingo mañana)"
```

---

## 4. Amazfit GTS 4 (Wearable)

### Métricas Cardiovasculares
| Métrica | Valor Actual | Fecha | Objetivo | Rango Óptimo |
|---------|--------------|-------|----------|--------------|
| **VO2max estimado** | 50 ml/kg/min | 18/06/2026 | > 45 ✅ | 40-50 (bueno), >50 (excelente) |
| **FC reposo** | 53 bpm | 18/06/2026 | < 60 ✅ | 50-70 (atleta: <50) |
| **FC máxima (medida)** | ~196 bpm | | — | ~220 - edad |

### Métricas de Sueño (Promedio)
| Métrica | Valor Actual | Objetivo | Notas |
|---------|--------------|----------|-------|
| **Sleep Score** | 83 /100 | > 80 ✅ | Puntuación global |
| **Sueño profundo** | 18% | > 90 min | Recuperación física |
| **Sueño REM** | 27% | > 90 min | Recuperación cognitiva |
| **Sueño ligero** | 5 minutos | — | Referencia |
| **Despertares** | 0 | < 2 | Fragmentación |
| **Duración total** | 7h | 7-8h ✅ | — |

### Métricas de Actividad (Promedio Diario)
| Métrica | Valor Actual | Objetivo | Notas |
|---------|--------------|----------|-------|
| **Pasos diarios** | 7390 | > 8000 | NEAT (↑ ~600 pasos) |


### Métricas de Estrés
| Métrica | Valor Actual | Objetivo | Notas |
|---------|--------------|----------|-------|
| **Estrés promedio** | 5-60 | < 50 | Medición continua |


```yaml
AMAZFIT_GTS4:
  sincronizacion: "Automática vía Zepp app"
  metricas_clave_para_motor:
    - sleep_score: "Ajusta cargas si < 60"
    - hrv: "Indica recuperación"
    - fc_reposo: "Alerta si > 70 bpm"
    - estres: "Ajusta volumen si > 70"
```

---

## 5. Medidas Corporales (Cinta Métrica)

> 📏 **Protocolo**: Medir siempre en el mismo punto, relajado (no flexionando), por la mañana.

### GUÍA DE OBJETIVOS REALISTAS (12 meses)

> 📊 **Basado en**: Tu perfil (principiante-intermedio, 3 años casual, buena genética autopercibida)
> Con adherencia >90% a entrenamiento y nutrición.

| Zona | Ganancia REALISTA | Ganancia ÓPTIMA | Notas |
|------|-------------------|-----------------|-------|
| **Hombros** | +3-4 cm | +5-6 cm | Deltoides responden bien |
| **Pecho** | +2-3 cm | +4 cm | Depende de genética de inserciones |
| **Bíceps** | +1-2 cm | +2-3 cm | Músculo pequeño, crece lento |
| **Antebrazo** | +0.5-1 cm | +1-1.5 cm | Muy genético |
| **Muslo** | +2-3 cm | +4 cm | Responde bien a volumen |
| **Pantorrilla** | +0.5-1 cm | +1-2 cm | Muy genético, difícil |
| **Cintura** | -2-4 cm (cut) | -5 cm | Depende de grasa inicial |

**Cómo calcular tu objetivo**: `Tu medida actual + ganancia realista = objetivo`

**Ejemplo**: Bíceps actual 35cm + 2cm realista = **objetivo 37cm**

---

### Circunferencias Principales


| Zona | Cómo Medir | Actual (cm) | Objetivo (cm) |
|------|------------|-------------|---------------|
| **Hombros** | Punto más ancho (deltoides) | XXX | +4 cm |
| **Pecho** | Línea de pezones, relajado | XXX | +3 cm |
| **Cintura** | Ombligo, relajado | XXX | < 85 cm |
| **Cadera** | Punto más ancho (glúteos) | XXX | — |

### Circunferencias de Brazos
| Zona | Cómo Medir | Derecho | Izquierdo | Objetivo |
|------|------------|---------|-----------|----------|
| **Bíceps contraído** | Flexionado, pico | XXX | XXX | +2 cm |
| **Antebrazo** | Punto más grueso | XXX | XXX | +1 cm |

### Circunferencias de Piernas
| Zona | Cómo Medir | Derecha | Izquierda | Objetivo |
|------|------------|---------|-----------|----------|
| **Muslo** | Punto más grueso | XXX | XXX | +3 cm |
| **Pantorrilla** | Punto más grueso | XXX | XXX | +1 cm |

---

### Ratio V-Taper (Tu Objetivo Principal)

| Ratio | Fórmula | Tu Valor | Objetivo | Significado |
|-------|---------|----------|----------|-------------|
| **Hombros ÷ Cintura** | Hombros / Cintura | ⏳ Calcular | **> 1.6** | Golden ratio = 1.618 |

**Cómo mejorar el ratio**:
- Opción A: Aumentar hombros (+cm deltoides)
- Opción B: Reducir cintura (-grasa abdominal)
- Opción C: Ambos (lo ideal)

**Ejemplo**: 
- Actual: Hombros 115cm, Cintura 85cm → Ratio = 1.35
- Objetivo: Hombros 120cm (+5), Cintura 80cm (-5) → Ratio = 1.50
- Ambicioso: Hombros 122cm, Cintura 78cm → Ratio = 1.56 ✨

```yaml
MEDIDAS_FRECUENCIA:
  peso: "Diario"
  circunferencias: "Cada 4 semanas"
  fotos_progreso: "Mensual (misma luz, hora, poses)"
```

---

## 6. Fuerza (1RM Estimado)

> 💪 **Cálculo**: 1RM = Peso × (36 / (37 - reps)) [Brzycki]
> **Nota**: Evita press banca plano (prefiere inclinado)

### Levantamientos Principales
| Ejercicio | Peso × Reps | 1RM Estimado | PR Histórico | Ratio vs BW |
|-----------|-------------|--------------|--------------|-------------|
| **Sentadilla** | 80 kg × ? | ~80-90 kg | **130 kg** 🏆 | 1.66× BW |
| **Press inclinado** | 18 kg × 10 (manc.) | ~24 kg/manc. | — | ~0.6× BW total |
| **RDL** | 14 kg × 12 (manc.) | ~20 kg/manc. | — | — |

> ⚠️ **Gap significativo**: Sentadilla PR 130kg vs actual 80kg. Evaluar si fue pérdida de fuerza o lesión.

### Levantamientos Secundarios
| Ejercicio | Peso × Reps | 1RM Estimado | Notas |
|-----------|-------------|--------------|-------|
| **Dominadas** | BW × 3-4 reps | ~85 kg | Solo peso corporal, objetivo: +lastre |
| **Remo neutro** | 40 kg × 10 | ~53 kg | Polea |
| **Hip thrust** | 20 kg × 8 | ~25 kg | En progreso |
| **Curl predicador** | 15 kg × 12 | ~21 kg | — |
| **Kelso shrug** | 10 kg × 15 | ~15 kg | — |

### Nivel Actual (vs Estándares)
| Ejercicio | Actual | Principiante | Intermedio | Avanzado |
|-----------|--------|--------------|------------|----------|
| Sentadilla | 1.02× BW (actual) / **1.66× BW (PR)** | 0.75× | 1.25× | 1.75× |
| Press inclinado | ~0.6× BW | 0.5× | 0.85× | 1.2× |
| Dominadas | BW × 4 | BW × 1 | BW × 10 | BW+25% × 10 |

> 💡 **Objetivo inmediato**: Recuperar nivel de sentadilla (volver a ~100kg de trabajo)

---

## 7. Flexibilidad y Movilidad (Prioridad #4)

| Test | Resultado | Objetivo | Notas |
|------|-----------|----------|-------|
| **Tocarse los pies** | ✅ Sí | Palmas al suelo | Isquiotibiales OK |
| **Sentadilla profunda** | ✅ Sí | Talones suelo, torso recto | Tobillo + cadera OK |
| **Wall angels** | ❌ NO | Ejecución perfecta | **OBJETIVO PRINCIPAL** |
| **Rotación hombro** | Limitada | Manos se tocan por espalda | Relacionado con wall angels |
| **Extensión torácica** | Limitada | — | Relacionado con wall angels |

> ⚠️ **Limitación crítica**: No puede realizar wall angels en ninguna variante. Indica restricción severa en:
> - Rotación externa de hombros
> - Extensión de columna torácica
> - Movilidad escapular
>
> 🎯 **Objetivo declarado**: Llegar a hacer un wall angel perfecto

### Progreso Wall Angels (tracking)
| Fecha | Variante Intentada | Resultado | Notas |
|-------|-------------------|-----------|-------|
| 18/06/2026 | Todas | No puede | Línea base |

---

## 8. Postura (Prioridad #2)

### Evaluación Visual
| Desviación | Presente | Severidad | Notas |
|------------|----------|-----------|-------|
| **Cabeza adelantada** | ✅ Sí | Moderada | Forward head posture |
| **Hombros redondeados** | ✅ Sí | Moderada | Rotación interna |
| **Hipercifosis dorsal** | ✅ Sí | Severa | Muy pronunciada |
| **Hiperlordosis lumbar** | ✅ Sí | Severa | Muy pronunciada |
| **Inclinación pélvica anterior** | ✅ Sí | Severa | Muy pronunciada |

> ⚠️ **Foco correctivo**: Upper Cross Syndrome (cabeza adelantada + hombros redondeados + no puede hacer wall angels). Ver `evidencia/postura.md` para protocolo.

---

## 9. Salud y Lesiones

### Lesión Activa
| Zona | Problema | Síntomas | Ejercicios a evitar | Desde |
|------|----------|----------|---------------------|-------|
| **Codo** | Dolor nervioso/tendinoso | Chispazo puntual, calambre | Press francés, fondos, extensión completa bajo carga | — |

> ⚠️ **Acción requerida**: Evaluar con profesional. Posible compresión nervio cubital o epicondilitis.

### Historial de Lesiones
| Zona | Descripción | Año | Estado |
|------|-------------|-----|--------|
| Codo | Lesión previa (detalles pendientes) | — | Crónico |

### Condiciones Médicas
| Condición | Medicación | Impacto en Entreno |
|-----------|------------|-------------------|
| Ninguna | — | — |

### Alergias
| Sustancia | Severidad |
|-----------|----------|
| Polvo | Leve |

---

## 10. Resumen de Objetivos por Prioridad

| Prioridad | Área | Métrica Clave | Actual | Objetivo | Plazo |
|-----------|------|---------------|--------|----------|-------|
| #1 | Estética | Ratio cintura/hombros | ⏳ Medir | < 0.62 | 12 meses |
| #2 | Postura | Wall angel perfecto | ❌ No puede | ✅ Ejecución limpia | 6 meses |
| #3 | Hipertrofia | Circunf. hombros | ⏳ Medir | +5 cm | 12 meses |
| #3 | Hipertrofia | Circunf. bíceps | ⏳ Medir | +2 cm | 12 meses |
| #4 | Flexibilidad | Tocarse pies | ✅ Sí | Palmas suelo | 6 meses |
| #5 | Estrés | FC reposo | 53 bpm | < 50 bpm | 12 meses |
| — | Composición | Grasa corporal | 18.9% | 14-15% | Dic 2026 |
| — | Composición | Peso | 78.2 kg | 80-82 kg | Ago 2027 |

---

## 11. Frecuencia de Tracking

| Métrica | Frecuencia | Herramienta | Notas |
|---------|------------|-------------|-------|
| Peso | Diaria | Báscula Xiaomi | Media semanal |
| Composición corporal | Semanal (domingo) | Báscula Xiaomi | Mismas condiciones |
| Sueño | Automática | Amazfit GTS 4 | Revisar semanal |
| FC reposo/HRV | Automática | Amazfit GTS 4 | Revisar si cansancio |
| Circunferencias | Cada 2-4 semanas | Cinta métrica | Fotos también |
| Fuerza (1RM) | Cada 4-8 semanas | Test en gym | O cuando PB |
| Postura | Mensual | Fotos/espejo | Comparar con anterior |
| Flexibilidad | Mensual | Tests manuales | — |

---

## 12. Uso en el Sistema
1. `motor_pesos.md` usa 1RM y composición para calcular cargas
2. `motor_dieta.md` usa metabolismo basal y peso para calcular macros
3. `REG-ENT-01` prioriza trabajo correctivo según evaluación postural
4. La app sincroniza automáticamente datos de Xiaomi y Amazfit
