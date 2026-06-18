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
| Altura | 188 cm | Medir descalzo |
| Envergadura | 📏 Pendiente medir | Brazos extendidos, pared a pared |
| Talla calzado | 45 EU | — |

---

## 3. Báscula Xiaomi (Bioimpedancia)

> ⚠️ **Nota**: Los datos de bioimpedancia son aproximados (±3-5% error). Útiles para ver tendencias, no valores absolutos.

### Métricas de Composición Corporal
| Métrica | Valor Actual | Fecha | Objetivo | Rango Saludable |
|---------|--------------|-------|----------|-----------------|
| **Peso** | 78.2 kg | 18/06/2026 | 83 kg | Según altura/objetivo |
| **Grasa corporal** | 18.9% | 18/06/2026 | 15% | 10-20% (hombre fitness) |
| **Masa muscular** | 60.2 kg | 18/06/2026 | +3 kg | ↑ Cuanto más mejor |
| **Masa ósea** | ~3 kg | | — | ~2.5-3.5 kg (hombre) |
| **Agua corporal** | ~55% | | — | 50-65% |
| **Proteína** | ~18% | | — | 16-20% |
| **Grasa visceral** | 9 | 18/06/2026 | < 10 ✅ | 1-9 (saludable) |
| **Metabolismo basal** | ~1850 kcal | | — | Referencia para dieta |
| **Edad metabólica** | ~22 años | | < edad real ✅ | Menor = mejor |
| **IMC** | 22.1 | | 22-25 ✅ | 18.5-24.9 (normal) |

> 💡 **Objetivo**: Recomposición corporal (+4.8 kg peso, -3.9% grasa = ganar ~5kg músculo, perder ~1kg grasa)

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

### Circunferencias Principales
| Zona | Punto de Medición | Actual (cm) | Objetivo (cm) | Fecha |
|------|-------------------|-------------|---------------|-------|
| **Cuello** | Bajo la nuez | [RELLENAR] | — | |
| **Hombros** | Punto más ancho (deltoides) | [RELLENAR] | [RELLENAR] | |
| **Pecho** | Línea de pezones, relajado | [RELLENAR] | [RELLENAR] | |
| **Cintura** | Ombligo, relajado | [RELLENAR] | < 94 cm | |
| **Cadera** | Punto más ancho (glúteos) | [RELLENAR] | — | |

### Circunferencias de Brazos
| Zona | Punto de Medición | Derecho (cm) | Izquierdo (cm) | Objetivo | Fecha |
|------|-------------------|--------------|----------------|----------|-------|
| **Bíceps relajado** | Punto más grueso | [RELLENAR] | [RELLENAR] | [RELLENAR] | |
| **Bíceps contraído** | Flexionado, pico | [RELLENAR] | [RELLENAR] | [RELLENAR] | |
| **Antebrazo** | Punto más grueso | [RELLENAR] | [RELLENAR] | — | |
| **Muñeca** | Justo encima del hueso | [RELLENAR] | [RELLENAR] | — | |

### Circunferencias de Piernas
| Zona | Punto de Medición | Derecha (cm) | Izquierda (cm) | Objetivo | Fecha |
|------|-------------------|--------------|----------------|----------|-------|
| **Muslo proximal** | Justo bajo glúteo | [RELLENAR] | [RELLENAR] | [RELLENAR] | |
| **Muslo medio** | Punto más grueso | [RELLENAR] | [RELLENAR] | [RELLENAR] | |
| **Pantorrilla** | Punto más grueso | [RELLENAR] | [RELLENAR] | — | |
| **Tobillo** | Justo encima del hueso | [RELLENAR] | [RELLENAR] | — | |

### Ratios Estéticos (Prioridad #1)
| Ratio | Fórmula | Valor Actual | Objetivo | Notas |
|-------|---------|--------------|----------|-------|
| **Cintura/Hombros** | Cintura ÷ Hombros | [CALCULAR] | < 0.62 | Ratio V-taper |
| **Cintura/Cadera** | Cintura ÷ Cadera | [CALCULAR] | < 0.90 | Salud cardiovascular |
| **Hombros/Cintura** | Hombros ÷ Cintura | [CALCULAR] | > 1.6 | "Golden ratio" ~1.618 |

```yaml
MEDIDAS_FRECUENCIA:
  peso: "Diario"
  circunferencias: "Cada 2-4 semanas"
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

## 9. Lesiones y Limitaciones

### Actuales
| Zona | Problema | Síntomas | Ejercicios que lo provocan |
|------|----------|----------|---------------------------|
| **Codo** | Dolor nervioso/tendinoso | Chispazo puntual, como calambre | Press inclinado, press francés, fondos |

### Historial
| Lesión | Fecha | Estado |
|--------|-------|--------|
| Pendiente registrar | — | — |

> ⚠️ **Acción requerida**: Evaluar el dolor de codo con profesional. Posible compresión del nervio cubital o epicondilitis. Evitar extensión completa de codo bajo carga hasta diagnóstico.

---

## 9. Historial Clínico

### Lesiones Activas
| Zona | Descripción | Desde | Limitación | Tratamiento |
|------|-------------|-------|------------|-------------|
| — | [Ninguna] | — | — | — |

### Lesiones Pasadas (Relevantes)
| Zona | Descripción | Año | Recuperación |
|------|-------------|-----|--------------|
| — | [Codo con daño] | — | — |

### Condiciones Médicas
| Condición | Estado | Medicación | Impacto en Entreno |
|-----------|--------|------------|-------------------|
| — | [Ninguna] | — | — |

### Alergias
| Tipo | Sustancia | Severidad |
|------|-----------|-----------|
| — | [polvo] | — |

---

## 10. Resumen de Objetivos por Prioridad

| Prioridad | Área | Métrica Clave | Actual | Objetivo | Plazo |
|-----------|------|---------------|--------|----------|-------|
| #1 | Estética | Ratio cintura/hombros | [CALC] | < 0.62 | — |
| #2 | Postura | Desviaciones corregidas | [0]/6 | 6/6 | — |
| #3 | Hipertrofia | Circunf. hombros | [RELLENAR] cm | [125] cm | — |
| #3 | Hipertrofia | Circunf. bíceps | [RELLENAR] cm | [40] cm | — |
| #4 | Flexibilidad | Test tocarse pies | [Punta de dedos en el pie] | Palmas suelo | — |
| #5 | Estrés | FC reposo | [53] bpm | < 50 bpm | — |
| — | Composición | Grasa corporal | [18,9] % | [15] % | — |
| — | Composición | Peso | [78] kg | [83] kg | — |

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
