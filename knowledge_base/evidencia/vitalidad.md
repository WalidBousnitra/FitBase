---
id: "EVI-07"
nombre: "Evidencia: Vitalidad y Energía"
fecha_modificacion: "17/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-06", "EVI-08", "EVI-05", "EVI-12"]
tags: ["evidencia", "vitalidad", "energia", "fatiga", "recuperacion"]
prioridad: 7
---

# Vitalidad y Energía

> **Prioridad #7** — Alineado con [prioridades.md](../usuario/prioridades.md)

## 1. Alcance
Evidencia científica sobre energía percibida, vitalidad y optimización metabólica.

---

## 2. SÍNTESIS DE EVIDENCIA (De otros papers)

### Esta sección integra hallazgos de múltiples papers ya revisados.

---

## 3. FACTORES QUE AFECTAN LA ENERGÍA

### 3.1 Sueño (De Fullagar et al. 2015)

| Factor | Impacto en Energía |
|--------|-------------------|
| **Déficit de sueño** | ↓ Energía percibida, ↑ fatiga |
| **Calidad del sueño** | Determinante de recuperación |
| **Sueño óptimo** | 8-9 horas para atletas |

```yaml
SUENO_PARA_ENERGIA:
  horas_optimas: 8-9
  calidad: priorizar_sueno_profundo
  rutina: horario_consistente
```

### 3.2 Ejercicio (De Salmon 2001)

| Hallazgo | Descripción |
|----------|-------------|
| Ejercicio regular | **↑ Energía** a largo plazo |
| Ejercicio agudo | Puede causar fatiga temporal |
| Sedentarismo | Asociado a más fatiga y síntomas |

> **Paradoja**: El ejercicio gasta energía pero AUMENTA la vitalidad percibida a largo plazo.

### 3.3 Estrés y Cortisol (De Salmon 2001, Kraemer 2005)

| Estado | Efecto en Energía |
|--------|-------------------|
| Estrés crónico | ↓ Energía, fatiga persistente |
| Sobreentrenamiento | Agotamiento, ratio T/C bajo |
| Ejercicio moderado | Mejora resiliencia al estrés |

### 3.4 Nutrición (De Helms et al. 2014)

| Factor | Efecto |
|--------|--------|
| Déficit calórico severo | ↓ Energía |
| Carbohidratos insuficientes | ↓ Rendimiento, fatiga |
| Deshidratación | ↓ Energía física y mental |
| Deficiencia de hierro | Fatiga crónica |

### 3.5 Microbioma (De Mailing et al. 2019)

| Conexión | Efecto |
|----------|--------|
| Eje intestino-cerebro | Microbioma afecta estado de ánimo |
| SCFAs (butirato) | Energía para células intestinales |
| Diversidad microbiana | Correlaciona con salud general |

---

## 4. CAUSAS COMUNES DE FATIGA

### Causas Relacionadas con Entrenamiento

| Causa | Señales | Solución |
|-------|---------|----------|
| Sobreentrenamiento | Fatiga persistente, ↓ rendimiento | Deload, más descanso |
| Volumen excesivo | Agotamiento post-entreno | Reducir 20-30% |
| Frecuencia alta sin recuperación | Cansancio acumulado | Más días de descanso |

### Causas Nutricionales

| Causa | Señales | Solución |
|-------|---------|----------|
| Déficit calórico severo | Letargo, frío | Aumentar calorías |
| Carbos muy bajos | Sin energía para entrenar | Aumentar carbos peri-entreno |
| Proteína insuficiente | Recuperación lenta | Aumentar a 2+ g/kg |
| Deshidratación | Fatiga mental y física | 35-40 ml/kg/día |

### Causas de Estilo de Vida

| Causa | Señales | Solución |
|-------|---------|----------|
| Sueño insuficiente | Fatiga diurna | Priorizar 8h |
| Estrés crónico | Agotamiento, ansiedad | Técnicas de manejo |
| Falta de luz solar | Bajo ánimo, fatiga | Exponerse a luz AM |

---

## 5. ESTRATEGIAS PARA OPTIMIZAR ENERGÍA

### Basadas en Evidencia

```yaml
OPTIMIZAR_ENERGIA:
  
  SUENO:
    horas: 8-9
    horario: consistente
    ambiente: oscuro_fresco_silencioso
    
  NUTRICION:
    calorias: no_deficit_severo
    carbos: suficientes_para_entreno
    hidratacion: 35-40 ml/kg/dia
    cafeina: moderada_no_tarde
    
  ENTRENAMIENTO:
    periodizar: deloads_cada_4-6_semanas
    volumen: ajustar_segun_recuperacion
    cardio: no_excesivo
    
  ESTILO_VIDA:
    luz_solar_manana: 10-30 min
    ejercicio_regular: consistente
    estres: tecnicas_manejo
```

---

## 6. SEÑALES DE ALARMA (Fatiga Patológica)

| Señal | Acción |
|-------|--------|
| Fatiga >2 semanas sin mejoría | Consultar médico |
| Fatiga + pérdida de peso involuntaria | Consultar médico |
| Fatiga + cambios de ánimo severos | Consultar profesional |
| Fatiga + síntomas físicos (fiebre, dolor) | Consultar médico |

---

## 7. RECOMENDACIONES PARA SISTEMA

```yaml
AUTORREGULACION_ENERGIA:
  
  input_diario:
    - sueno_horas
    - sueno_calidad (1-10)
    - energia_percibida (1-10)
    - estres (1-10)
    
  ajustes_si_energia_baja:
    energia < 5:
      - reducir_volumen_20%
      - priorizar_sueno
      - revisar_nutricion
      
    energia < 3 por 3+ dias:
      - dia_descanso_completo
      - evaluar_sobreentrenamiento
      
  umbrales:
    energia_optima: 7-10
    energia_aceptable: 5-6
    energia_baja: 3-4
    alarma: 1-2
```

---

## 8. RESUMEN EJECUTIVO

| Factor | Impacto en Vitalidad | Prioridad |
|--------|----------------------|-----------|
| Sueño | **ALTO** | 1 |
| Nutrición adecuada | **ALTO** | 2 |
| Ejercicio regular | **ALTO** (paradoja) | 3 |
| Manejo del estrés | MODERADO-ALTO | 4 |
| Hidratación | MODERADO | 5 |
| Microbioma saludable | MODERADO | 6 |

> **Mensaje clave**: La vitalidad es multifactorial. Priorizar sueño y nutrición antes de buscar soluciones complejas.
