---
id: "REG-ENT-03"
nombre: "Calentamiento y Activación"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["REG-ENT-01", "REG-ENT-02", "EVI-02", "EVI-04", "EVI-16"]
tags: ["reglas", "calentamiento", "activacion", "movilidad"]
---

# Calentamiento y Activación

## 1. Alcance
Protocolos de preparación neuromuscular antes del entrenamiento principal.

> **Fuente**: Ver [calentamiento.md](../../evidencia/calentamiento.md) y [flexibilidad.md](../../evidencia/flexibilidad.md)

## 2. Preferencias del Usuario

> Referencia: `reglas/entrenamiento/preferencias.md`

| Aspecto | Preferencia |
|---------|-------------|
| Tipo preferido | Movilidad dinámica |
| Duración máxima | 10-15 min |
| Cardio previo | Opcional (5 min máx) |

---

## 3. Estructura del Calentamiento

> **Evidencia**: Rodrigues 2020 - Ningún protocolo mejora fuerza aguda, pero previene lesiones.  
> **Evidencia**: Page 2012 - Estiramiento estático PRE-entreno reduce fuerza.

### Fase 1: Cardio General (Opcional)
| Parámetro | Valor |
|-----------|-------|
| Duración | 5 min máx |
| Intensidad | 50-60% HR max |
| Opciones | Bici, elíptica, cinta rápida |

### Fase 2: Movilidad Dinámica (5 min)

| Zona | Ejercicio | Reps |
|------|-----------|------|
| Cadera | Círculos de cadera | 10/lado |
| Cadera | Balanceos frontales | 10/lado |
| Columna | Gato-vaca | 10 |
| Columna | Rotaciones de tronco | 10/lado |
| Hombros | Círculos de brazos | 10/dirección |
| Hombros | Dislocaciones con banda | 10 |
| Tobillos | Círculos de tobillo | 10/lado |

> ⚠️ **NO HACER**: Estiramientos estáticos pre-entreno (reduce fuerza)

### Fase 3: Activación Específica (2-3 min)

| Día | Activación |
|-----|------------|
| **Push** | Face pulls ligeros, rotación externa |
| **Pull** | Retracción escapular, dead hangs |
| **Legs** | Glute bridges, sentadillas sin peso |
| **Upper** | Combinar Push + Pull |

### Fase 4: Series de Aproximación

> **Fuente**: Evidencia/calentamiento.md

| Tipo Ejercicio | Protocolo |
|----------------|-----------|
| **Compuesto pesado** (squat, bench, deadlift) | 40% x10 → 60% x6 → 75% x3 → 85% x1-2 |
| **Compuesto secundario** | 50% x8 → 70% x4 |
| **Aislamiento** | 1 serie ligera o directo |

---

## 4. Trabajo Correctivo (Prioridad #2)

> **Fuente**: [postura.md](../../evidencia/postura.md) - Protocolo Ruivo 16 semanas

| Problema Postural | Ejercicios Correctivos | Series x Reps |
|-------------------|------------------------|---------------|
| **Forward head** | Retracción cervical, chin tucks | 2-3 x 10-15 |
| **Hipercifosis** | Extensión torácica, face pulls | 2-3 x 10-15 |
| **Hombros internos** | Rotación externa, band pull-aparts | 2-3 x 15-20 |
| **Hiperlordosis** | Dead bugs, plancha, glute bridges | 2-3 x 10-15 |

---

## 5. Tiempo Total

| Componente | Duración |
|------------|----------|
| Cardio (opcional) | 0-5 min |
| Movilidad dinámica | 5 min |
| Activación específica | 2-3 min |
| Series aproximación | 5-10 min |
| **TOTAL** | **12-23 min** |

---

## 6. Uso en el Sistema

```yaml
GENERADOR_CALENTAMIENTO:
  1_cardio_opcional:
    si_usuario_prefiere: 5_min_bici_suave
    si_no: saltar
    
  2_movilidad_dinamica:
    duracion: 5_min
    ejercicios: circulos_cadera + balanceos + gato_vaca + rotaciones
    
  3_activacion:
    segun_dia_entrenamiento:
      push: face_pulls + rotacion_externa
      pull: retraccion_escapular
      legs: glute_bridges
      
  4_series_aproximacion:
    primer_ejercicio_compuesto:
      - 40% x 10
      - 60% x 6
      - 75% x 3
      - 85% x 1-2
      
  5_correctivos:
    si_problemas_posturales:
      incluir_2-3_ejercicios_relevantes
```
