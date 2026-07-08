---
id: "REG-ENT-04"
nombre: "Preferencias de Entrenamiento"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "USR-03", "REG-ENT-01"]
tags: ["reglas", "entrenamiento", "preferencias", "gustos"]
---

# Preferencias de Entrenamiento

## 1. Propósito
Gustos y preferencias personales que guían la programación del entrenamiento.

---

## 2. Duración de la Sesión

| Parámetro | Valor |
|-----------|-------|
| Mínimo aceptable | 60 min |
| Ideal | 75 min |
| Máximo | 90 min |

> **Nota:** Prefiere descansos cortos para no alargar el entreno.

---

## 3. Calentamiento

| Aspecto | Preferencia |
|---------|-------------|
| Tipo preferido | Movilidad |
| Duración máxima | 10 min |
| Cardio previo | No necesario |

---

## 4. Orden y Estructura

### Preferencia de Ejercicios
| Prioridad | Tipo |
|-----------|------|
| 1º | Peso libre (preferido) |
| 2º | Mezcla según el día |
| 3º | Máquinas (para aislar) |

### Ejemplo de Preferencia
> Para isquios: prefiere RDL (peso libre) sobre curl femoral (máquina)

### Cardio
- **NO** al final del entreno de fuerza
- Prefiere hacerlo en sesión separada

---

## 5. Descansos Entre Series

| Aspecto | Preferencia | Evidencia (Schoenfeld 2016) |
|---------|-------------|----------------------------|
| Duración preferida | Cortos (2-3 min) | 3 min > 1 min para hipertrofia |
| Para multiarticulares | 2-3 min | Más volumen total = más ganancia |
| Para accesorios | 1-2 min | Menor demanda sistémica |
| Razón personal | Se aburre esperando | Usar superseries para accesorios |

> 💡 **Ajuste**: Descansos "cortos" de 2 min en multiarticulares, superseries en accesorios para no aburrirse mientras cumple evidencia.

---

## 6. Ejercicios Favoritos

| Grupo Muscular | Ejercicios Favoritos | Notas |
|----------------|---------------------|-------|
| Pecho | Press inclinado, Cruces polea alta | Fondos quitado (auditoría 2026-c, KB-01): biometria.md §9/seleccion_ejercicios.md §6 lo excluyen explícitamente por el dolor de codo — esta tabla se contradecía a sí misma con el resto de la KB |
| Espalda | Remo polea agarre neutro, Dominadas, Remo unilateral con rotación, Kelso shrug | Dominadas: pocas reps |
| Hombros | Elevaciones laterales sentado, Elevaciones laterales polea media altura | Prefiere sentado |
| Bíceps | Curl barra Z de pie, Zottman, Curl predicador | — |
| Tríceps | Press francés banco 30°, Extensiones unilaterales polea alta | ⚠️ Dolor codo: evitar extensión completa |
| Cuádriceps | Sentadilla | Ejercicio principal |
| Isquios | RDL | Prefiere vs máquina |
| Glúteos | Hip thrust | — |
| Core | Plancha, Hollow hold, Hollow rock, Press Pallof | Le gustan todas las variantes |

---

## 7. Ejercicios que NO Le Gustan

> Lista completa y autoritativa en [preferencias_ejercicios.md](../../usuario/preferencias_ejercicios.md) §2-3 —
> consolidado ahí (auditoría 2026-c, KB-02) para no mantener la misma tabla
> duplicada en dos archivos. Resumen: ejercicios que duelen el codo (⚠️ dolor
> crónico), Press banca plano, Prensas en máquina, Agarres cerrados espalda,
> Curl martillo, Peso muerto sumo.

---

## 8. Entrenamiento Actual (Contexto)

| Aspecto | Valor |
|---------|-------|
| **Split** | Pierna / Upper / Push / Pull |
| **Días/semana** | 4 |
| **Duración** | ~1h 15min |

### Ejercicios Habituales
- Kelso shrug
- Jalón al pecho
- Press inclinado
- Curls

> 💡 **Nota**: Split híbrido que combina Upper con Push/Pull para mayor frecuencia de torso.

---

## 9. Uso en el Sistema

```
Generador de rutinas:
  1. Duración objetivo: 60-90 min
  2. Priorizar peso libre sobre máquinas
  3. Descansos cortos (según evidencia)
  4. Cardio en sesión separada
  5. Calentamiento: movilidad ≤10 min
  6. Filtrar ejercicios NO gustados
```
