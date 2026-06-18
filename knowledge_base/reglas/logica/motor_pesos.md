---
id: "REG-LOG-01"
nombre: "Motor de Cargas"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-MET-01", "USR-MET-02", "REG-ENT-01"]
tags: ["reglas", "logica", "motor", "pesos", "autorregulacion"]
---

# Motor de Cargas

## 1. Alcance
Algoritmo que ajusta las cargas de entrenamiento basándose en fatiga, sueño y readiness.

## 2. Baseline del Usuario (18/06/2026)

| Métrica | Valor Actual | Interpretación |
|---------|--------------|----------------|
| FC reposo | 53 bpm | ✅ Excelente (atlético) |
| VO2max | 50 ml/kg/min | ✅ Excelente |
| Sleep Score | 83 | ✅ Bueno |
| Horas sueño | 7h | ⚠️ Leve déficit vs 8h ideal |
| Pasos | 7390/día | ⚠️ Bajo vs 8000 objetivo |

> **Estado actual**: El usuario tiene buena capacidad de recuperación. FC reposo y VO2max indican buena condición cardiovascular. Sueño adecuado pero con margen de mejora.

---

## 3. Variables de Entrada

### Métricas de Hardware (USR-MET-01)

> ✅ **Fuente HRV**: Kiviniemi et al. (2007) - `evidencia/sueno.md` § 7

| Variable | Criterio | Acción | Evidencia |
|----------|----------|--------|-----------|
| `ZEPP_HRV` / Readiness | **< (media_10d - 1 SD)** | Reducir intensidad o descanso | ✅ Kiviniemi 2007 |
| `ZEPP_HRV` / Readiness | **↓ 2+ días consecutivos** | Día de recuperación activa | ✅ Kiviniemi 2007 |
| `ZEPP_SLEEP_SCORE` | <60 (heurístico) | Reducir volumen | ⚠️ HEURÍSTICO |
| `ZEPP_HR_REST` | +10bpm vs baseline | Considerar descanso | ⚠️ HEURÍSTICO |

### Métricas Subjetivas (USR-MET-02) - HEURÍSTICAS
| Variable | Umbral | Acción | Nota |
|----------|--------|--------|------|
| `SUB_ENERGIA` | <3/10 | Reducir intensidad | Heurístico |
| `SUB_ESTRES` | >7/10 | Reducir volumen | Heurístico |
| `SUB_DOMS` | Severo en grupo | Evitar ese grupo | Sentido común |

## 4. Protocolo HRV (Evidencia)

> ✅ **Fuente**: Kiviniemi et al. (2007) - `evidencia/sueno.md` § 7

```yaml
HRV_AUTOREGULACION:
  medicion:
    momento: "Al despertar, antes de levantarse"
    duracion: "2-3 minutos"
    posicion: "Supino"
  
  calculo_referencia:
    media_movil: "10 días anteriores"
    umbral_bajo: "media - 1 desviación estándar"
    
  decision:
    HRV_≥_media: "Sesión planificada normal"
    HRV_<_umbral_bajo: "Reducir intensidad"
    tendencia_descendente_2d: "Recuperación activa"
```

> **Resultado del paper**: Grupo guiado por HRV mejoró VO2peak +7.1% vs +1.3% del grupo fijo (p=0.048)

---

## 5. Lógica de Cálculo

> Combinación de HRV (paper) + heurísticas marcadas

```python
def calcular_ajuste(datos_usuario):
    ajuste = 1.0  # 100% = sesión normal
    
    # --- BASADO EN EVIDENCIA (Kiviniemi 2007) ---
    if datos_usuario.hrv < datos_usuario.hrv_media_10d - datos_usuario.hrv_sd:
        ajuste *= 0.80  # Reducción significativa
    elif datos_usuario.hrv_tendencia == "descendente_2d":
        return "RECUPERACION_ACTIVA"
    
    # --- HEURÍSTICAS (marcar claramente) ---
    # Los factores numéricos siguientes son estimaciones prácticas
    if datos_usuario.sleep_score < 60:
        ajuste *= 0.90  # ⚠️ HEURÍSTICO
    if datos_usuario.estres_subjetivo > 7:
        ajuste *= 0.85  # ⚠️ HEURÍSTICO
        
    return ajuste
```

---

## 6. Progresión de Cargas

> ✅ **Fuentes**: ACSM (2009), Mann (2010), Rhea (2002) - `evidencia/periodizacion.md` §§ 11-13

### Criterio de Progresión (ACSM 2009)
```yaml
REGLA_SUBIR_PESO:
  condicion: "Completas 1-2 reps MÁS de las objetivo"
  aumento: "2-10%"
  ejemplo: "Si objetivo era 8 reps y haces 10, subir 2.5-5kg"
```

### Tiempos de Descanso (ACSM 2009)
| Objetivo | Descanso |
|----------|----------|
| Fuerza (1-6 RM) | **3-5 min** |
| Hipertrofia (8-12 RM) | 1-2 min |
| Resistencia (15+ RM) | <1 min |

### Protocolo APRE Simplificado (Mann 2010)
```yaml
APRE_DIARIO:
  set_1: "10 reps @ 50% 6RM"
  set_2: "6 reps @ 75% 6RM"
  set_3: "máximo @ 100% 6RM"
  set_4: "máximo @ peso ajustado según set_3"
  
  ajuste_set_4:
    reps_0-2: "bajar 2.5-5 kg"
    reps_3-4: "bajar 0-2.5 kg"
    reps_5-7: "mantener"
    reps_8-12: "subir 2.5-5 kg"
    reps_13+: "subir 5-7.5 kg"
```

> **Resultado del paper**: APRE produjo **3-5× más mejoras** que periodización lineal.

### Progresión RIR Semanal (Helms)
```yaml
RIR_PROGRESION:
  semana_1: RIR 3-4 (RPE 6-7)
  semana_2: RIR 2-3 (RPE 7-8)
  semana_3: RIR 1-2 (RPE 8-9)
  semana_4: DELOAD
```

---

## 7. Reglas de Seguridad

> ⚠️ **Mayormente HEURÍSTICAS** - Usar criterio clínico

```yaml
BLOQUEOS_ABSOLUTOS:
  - hrv_muy_bajo: "< media - 2 SD"  # Extrapolación de Kiviniemi
  - dolor_agudo: "Usuario reporta dolor ≥7/10"
  - enfermedad: "Usuario reporta fiebre/malestar"
  
ALERTAS:
  - "3+ días sin recuperar HRV → Sugerir semana deload"
  - "Sleep score <50 → Priorizar descanso"
```

---

## 8. Uso en el Sistema
1. Se ejecuta antes de cada sesión
2. Lee HRV/Readiness del día (Zepp) + media 10 días
3. Aplica protocolo Kiviniemi para decisión base
4. Ajusta con heurísticas si hay datos subjetivos
5. Muestra ajuste sugerido con justificación

> **Estado**: Motor FUNCIONAL con base en Kiviniemi (HRV) + ACSM/Mann/Rhea (progresión)