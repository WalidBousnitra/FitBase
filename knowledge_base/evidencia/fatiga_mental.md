---
id: "EVI-17"
nombre: "Evidencia: Fatiga Mental"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-05", "EVI-12"]
tags: ["evidencia", "fatiga", "mental", "rendimiento", "RPE"]
prioridad: "soporte"
---

# Fatiga Mental

> **Soporte** — Impacto de la fatiga mental en el rendimiento físico

## 1. Alcance
Evidencia científica sobre cómo la fatiga mental afecta el entrenamiento y el rendimiento.

---

## 2. FUENTES PRINCIPALES

- **Van Cutsem, J. et al. (2017)** - "The Effects of Mental Fatigue on Physical Performance: A Systematic Review" - Sports Medicine
- Revisión sistemática de 11 estudios

---

## 3. HALLAZGOS CLAVE

### Key Points del Paper

| Hallazgo | Implicación |
|----------|-------------|
| **Fatiga mental SÍ afecta resistencia** | El cardio/endurance se ve perjudicado |
| **Fatiga mental NO afecta fuerza máxima** | Puedes entrenar pesado aunque estés mentalmente cansado |
| **Fatiga mental NO afecta potencia** | Los explosivos se mantienen |
| **Fatiga mental NO afecta trabajo anaeróbico** | Series cortas e intensas no se ven afectadas |

### Mecanismo Principal

> **La fatiga mental aumenta la PERCEPCIÓN DEL ESFUERZO (RPE) sin cambiar la capacidad física real.**

| Tipo de Ejercicio | ¿Afectado por Fatiga Mental? | Motivo |
|-------------------|------------------------------|--------|
| Resistencia/Endurance | ✅ SÍ | Mayor RPE → abandono prematuro |
| Fuerza máxima | ❌ NO | Esfuerzos cortos, no dependen de RPE sostenido |
| Potencia | ❌ NO | Movimientos explosivos, corta duración |
| Trabajo anaeróbico | ❌ NO | Alta intensidad, corta duración |

---

## 4. ¿QUÉ CAUSA FATIGA MENTAL?

### Actividades Inductoras (del estudio)

| Actividad | Duración | Efecto |
|-----------|----------|--------|
| Tareas cognitivas prolongadas | 30-90 min | Fatiga moderada-alta |
| Trabajo de oficina intenso | 4+ horas | Fatiga alta |
| Estudio concentrado | 2+ horas | Fatiga moderada |
| Videojuegos competitivos | 1+ hora | Fatiga variable |
| Decisiones continuas | Variable | Fatiga por "decision fatigue" |

### Señales de Fatiga Mental

| Síntoma | Descripción |
|---------|-------------|
| Menor motivación | "No tengo ganas de entrenar" |
| Mayor RPE percibido | El ejercicio se siente más duro de lo normal |
| Dificultad concentración | Errores en técnica, distracción |
| Irritabilidad | Menor tolerancia al esfuerzo |

---

## 5. APLICACIÓN PRÁCTICA

### Cuándo Entrenar con Fatiga Mental

| Tipo de Entrenamiento | Recomendación |
|----------------------|---------------|
| **Fuerza (series cortas)** | ✅ Adelante - no afecta |
| **Potencia/Explosivos** | ✅ Adelante - no afecta |
| **Cardio prolongado** | ⚠️ Reducir expectativas |
| **HIIT** | ⚠️ Puede sentirse más duro |
| **Técnica compleja** | ⚠️ Mayor riesgo de errores |

### Estrategias de Manejo

```yaml
FATIGA_MENTAL_ALTA:
  opcion_1_entrenar_fuerza:
    viable: true
    ajustes:
      - usar_autoregulacion_RIR
      - no_confiar_en_RPE_para_cardio
      - simplificar_ejercicios
      
  opcion_2_entrenar_cardio:
    viable: parcial
    ajustes:
      - reducir_duracion_planificada
      - usar_HR_en_vez_de_RPE
      - aceptar_menor_rendimiento
      
  opcion_3_descansar:
    viable: true
    cuando:
      - fatiga_extrema
      - acumulada_varios_dias
      - señales_de_sobreentrenamiento

SI_TRABAJO_COGNITIVO_ANTES:
  timing_optimo: "Entrenar ANTES del trabajo mental"
  si_no_es_posible: "Esperar 1-2h post-trabajo antes de entrenar"
  pre_entreno: "Puede ayudar cafeína (ver suplementacion.md)"
```

---

## 6. IMPLICACIONES PARA PROGRAMACIÓN

### Timing de Entrenamientos

| Situación | Recomendación |
|-----------|---------------|
| Día laboral intenso | Entrenar fuerza > cardio |
| Exámenes/deadlines | Reducir volumen de cardio |
| Trabajo nocturno | Priorizar entrenos cortos e intensos |
| Fin de semana | Mejor momento para cardio largo |

### Ajustes de Intensidad

```yaml
AUTOREGULACION_FATIGA_MENTAL:
  fuerza:
    usar: RIR (funciona igual con fatiga mental)
    porque: "Fuerza máxima no afectada"
    
  cardio:
    NO_usar: RPE (distorsionado por fatiga)
    usar: frecuencia_cardiaca
    porque: "RPE elevado artificialmente"
    
  ejemplo:
    normal: "30 min cardio zona 2 (RPE 4-5)"
    con_fatiga_mental: "30 min cardio, mantener HR zona 2, ignorar RPE"
```

---

## 7. INTERACCIÓN CON OTROS FACTORES

### Fatiga Mental + Otros Estresores

| Combinación | Efecto | Acción |
|-------------|--------|--------|
| Fatiga mental + falta de sueño | Amplificado | Reducir volumen total |
| Fatiga mental + déficit calórico | Amplificado | Priorizar fuerza, reducir cardio |
| Fatiga mental + estrés emocional | Amplificado | Considerar día de descanso |

---

## 8. RECOMENDACIONES PARA SISTEMA

```yaml
LOGICA_FATIGA_MENTAL:
  input_usuario:
    - pregunta: "¿Nivel de fatiga mental hoy? (1-5)"
    - pregunta: "¿Horas de trabajo cognitivo?"
    
  si_fatiga_alta_3-5:
    ajustar_cardio: reducir_20-40%
    ajustar_fuerza: mantener_o_reducir_ligeramente
    mensaje: "Tu fuerza no se ve afectada. El cardio puede sentirse más duro."
    
  si_fatiga_extrema_5:
    sugerir: "Considera descanso activo o solo movilidad"
    alternativa: "Entreno corto de fuerza (30 min max)"
```

---

## 9. RESUMEN EJECUTIVO

| Tipo Ejercicio | Impacto Fatiga Mental | Acción |
|----------------|----------------------|--------|
| Fuerza máxima | ❌ No afecta | Entrenar normal |
| Potencia | ❌ No afecta | Entrenar normal |
| Anaeróbico | ❌ No afecta | Entrenar normal |
| Resistencia/Cardio | ✅ Afecta (↑ RPE) | Ajustar expectativas |

> **Mensaje clave**: Si llegas al gym mentalmente agotado, puedes entrenar fuerza sin problema. El cardio se sentirá más duro de lo normal, pero tu capacidad física real está intacta.
