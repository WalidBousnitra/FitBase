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

### Métricas de Hardware (vía Health Connect)

> **Fuente datos**: La app lee de Health Connect, que recibe de Zepp/Mi Fitness.
> Ver [USR-MET-01](../../usuario/metricas/hardware.md) para flujo completo.

> ✅ **Fuente HRV**: Kiviniemi et al. (2007) - `evidencia/sueno.md` § 7

| Variable | Criterio | Acción | Evidencia |
|----------|----------|--------|-----------|
| `HC_SLEEP_SCORE` (calculado) | **< 60** | Reducir volumen | ⚠️ HEURÍSTICO |
| `HC_HR_REST` | **+10bpm vs baseline** | Considerar descanso | ⚠️ HEURÍSTICO |
| `HC_HR_REST` | **↑ 2+ días consecutivos** | Día de recuperación activa | ✅ Adaptado de Kiviniemi 2007 |

> ⚠️ **HRV no disponible**: Zepp no exporta RMSSD a Health Connect. Como alternativa, usamos:
> - Sleep Score (calculado desde duración + fases de sueño)
> - FC reposo elevada (indicador indirecto de estrés/fatiga)

### Métricas Subjetivas (USR-MET-02) - HEURÍSTICAS
| Variable | Umbral | Acción | Nota |
|----------|--------|--------|------|
| `SUB_ENERGIA` | <3/10 | Reducir intensidad | Heurístico |
| `SUB_ESTRES` | >7/10 | Reducir volumen | Heurístico |
| `SUB_DOMS` | Severo en grupo | Evitar ese grupo | Sentido común |

## 4. Protocolo FC Reposo (Adaptado de Evidencia)

> ✅ **Fuente original**: Kiviniemi et al. (2007) usaba HRV. Adaptamos a FC reposo.
> La FC reposo elevada es indicador indirecto de estrés/recuperación incompleta.

```yaml
FC_AUTOREGULACION:
  fuente_datos: "Health Connect (HC_HR_REST)"
  
  medicion_original:  # Usuario ve en Zepp app
    momento: "Al despertar, antes de levantarse"
    duracion: "Automático (Amazfit mide durante la noche)"
  
  calculo_referencia:
    media_movil: "10 días anteriores"
    umbral_alto: "media + 10 bpm"
    
  decision:
    FC_≤_media: "Sesión planificada normal"
    FC_>_umbral_alto: "Reducir intensidad"
    tendencia_ascendente_2d: "Considerar recuperación activa"
```

> **Nota**: FC reposo elevada indica que el sistema nervioso simpático está activado (estrés, recuperación incompleta, enfermedad incipiente).

---

## 5. Lógica de Cálculo

> Combinación de FC reposo (adaptado de paper) + Sleep Score + heurísticas marcadas

```python
def calcular_ajuste(datos_usuario):
    ajuste = 1.0  # 100% = sesión normal
    
    # --- ADAPTADO DE EVIDENCIA (Kiviniemi 2007) ---
    # Usando FC reposo como proxy de HRV (Health Connect: HC_HR_REST)
    if datos_usuario.fc_reposo > datos_usuario.fc_media_10d + 10:
        ajuste *= 0.80  # Reducción significativa
    elif datos_usuario.fc_tendencia == "ascendente_2d":
        return "RECUPERACION_ACTIVA"
    
    # --- HEURÍSTICAS (marcar claramente) ---
    # Sleep Score: calculado desde HC_SLEEP_DURATION + fases
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

> **IMPORTANTE**: El motor NO genera sesiones ni selecciona ejercicios.
> Las sesiones están PRE-GENERADAS (ver `base_datos.md` §7).
> El motor SOLO ajusta el campo `num_peso_sugerido_kg` de ejercicios_plan.

### Cuándo se ejecuta:
1. **Post-registro de serie** (inmediato): Actualiza peso sugerido de la PRÓXIMA sesión que tenga el mismo ejercicio
2. **Al abrir la app** (si hay métricas nuevas): Aplica ajuste de volumen (×0.80 si FC alta)

### Qué NO hace:
- NO genera sesiones (ya están pre-cargadas)
- NO selecciona ejercicios (ya están en ejercicios_plan)
- NO cambia series ni reps (definidas por fase en el template)
- NO es imprescindible: si falla, el usuario ve el último peso conocido o "Elige tu peso"

### Fallback sin red:
- La app usa el ÚLTIMO `num_peso_sugerido_kg` cacheado en Room
- Si es la primera sesión (peso = 0): la app muestra "Elige tu peso" y el usuario introduce manualmente
- El peso manual se registra en ejercicios_log y sirve de base para la siguiente sugerencia

> **Estado**: Motor FUNCIONAL. Solo modifica peso sugerido. No es bloqueante.