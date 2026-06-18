---
id: "REG-ENT-02"
nombre: "Inventario de Ejercicios"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-03", "REG-ENT-01", "EVI-03"]
tags: ["reglas", "ejercicios", "inventario", "seleccion"]
---

# Inventario de Ejercicios

## 1. Alcance
Catálogo de ejercicios disponibles, clasificados por grupo muscular, equipamiento requerido y objetivo.

## 2. Ejercicios del Usuario Actual

> Extraído de `Planificación M.xlsx` y preferencias declaradas (18/06/2026)

### Por Día (Split actual: Pierna/Upper/Push/Pull)

#### PIERNA
| Ejercicio | Series | Reps | Peso | Patrón |
|-----------|--------|------|------|--------|
| Sentadilla | 3-4 | 6-8 | 80kg (actual), 130kg (PR) | Extensión rodilla |
| Hip Thrust | 3 | 8 | 20kg | Extensión cadera |
| RDL | 3 | 12 | 14kg (manc.) | Extensión cadera |
| Haka (Hack squat) | 3 | 12 | 20kg | Extensión rodilla |
| Bird dog | 3 | 12 | — | Core/estabilidad |

#### UPPER
| Ejercicio | Series | Reps | Peso | Patrón |
|-----------|--------|------|------|--------|
| Kelso Shrug | 3 | 15 | 10kg | Tirón vertical |
| Remo Neutro | 3 | 10 | 40kg | Tirón horizontal |
| Curl Inclinado | 3 | 12 | 8kg | Aislamiento bíceps |
| Elev. Lat. Inclinado | 4 | 15 | 4kg | Empuje lateral |
| Curl Predicador | 3 | 12 | 15kg | Aislamiento bíceps |
| Extension Unilateral | 3 | 12 | 7.5kg | Aislamiento tríceps |

#### PULL
| Ejercicio | Series | Reps | Peso | Patrón |
|-----------|--------|------|------|--------|
| Dominadas | 3 | 3-4 | BW (78kg) | Tirón vertical |
| Remo Unilateral | 3 | 12 | — | Tirón horizontal |
| Kelso Shrug | 3 | 15 | 10kg | Tirón vertical |
| Curl Z | 3 | 10 | — | Aislamiento bíceps |
| Zottman | 3 | 12 | 6kg | Aislamiento bíceps |
| Granjero | 3 | 45" | 24kg | Grip/core |

#### PUSH
| Ejercicio | Series | Reps | Peso | Patrón |
|-----------|--------|------|------|--------|
| Press Inclinado | 4 | 10 | 18kg (manc.) | Empuje inclinado |
| Press Francés | 3 | 10 | 5kg | Aislamiento tríceps ⚠️ codo |
| Cruces | 3 | 12 | — | Aislamiento pecho |
| Elev. Lat. Polea | 3 | 20 | — | Empuje lateral |
| Elev. Maq | 3 | 15 | 10kg | Empuje lateral |
| Dead bug | 3 | 12 | — | Core |

---

## 3. Ejercicios Favoritos (declarados)

| Grupo | Ejercicios |
|-------|------------|
| **Pecho** | Press inclinado ⭐, Cruces polea alta, (Fondos - sin fuerza aún) |
| **Espalda** | Remo neutro ⭐, Dominadas, Remo unilateral con rotación, Kelso shrug ⭐ |
| **Hombros** | Elev. laterales sentado ⭐, Elev. laterales polea media |
| **Bíceps** | Curl Z ⭐, Zottman ⭐, Curl predicador |
| **Tríceps** | Press francés 30°, Extensiones unilaterales polea ⚠️ |
| **Pierna** | Sentadilla ⭐, Hip thrust, RDL |
| **Core** | Plancha, Hollow hold/rock, Press Pallof ⭐ |

> ⚠️ **Limitación codo**: Evitar extensión completa bajo carga en tríceps

---

## 4. Ejercicios EXCLUIDOS

| Ejercicio | Razón |
|-----------|-------|
| Press banca plano | Evita (preferencia) |
| Curl martillo | No le gusta |
| Peso muerto sumo | No le gusta |
| Agarres cerrados (espalda) | No le gusta |
| Prensas en máquina | No efectivos (percepción) |
| Ejercicios que duelen codo | ⚠️ Dolor crónico |

---

## 5. Ejercicios Correctivos (Prioridad #2: Postura)

> 🎯 **Objetivo**: Lograr un **wall angel perfecto** (actualmente NO puede hacerlo)

### Problemas Detectados del Usuario
| Problema | Severidad | Ejercicios Correctivos |
|----------|-----------|------------------------|
| **Cabeza adelantada** | Moderada | Chin tucks, SCM stretch, deep neck flexors |
| **Hombros redondeados** | Moderada | Face pulls, band pull-apart, rotación externa |
| **Cifosis torácica** | Probable | Extensión torácica, foam roll, prone Y-T-W |

### Progresión hacia Wall Angel
| Nivel | Ejercicio | Criterio para avanzar |
|-------|-----------|----------------------|
| 1 | Foam roll torácico (2 min) | Puede relajarse |
| 2 | Estiramiento pectoral en puerta | 30s sin dolor |
| 3 | Floor angel (tumbado) | Contacto completo con suelo |
| 4 | Slide de pared parcial | Brazos llegan a 45° |
| 5 | Wall angel asistido (banda) | Rango completo con ayuda |
| 6 | **Wall angel completo** | 🎯 OBJETIVO |

### Protocolo Correctivo Diario (5-10 min)
```yaml
RUTINA_CORRECTIVA:
  frecuencia: "Diario (mañana o pre-entreno)"
  duracion: "5-10 min"
  ejercicios:
    - chin_tucks: "3x10, 5s hold"
    - foam_roll_toracico: "2 min"
    - estiramiento_pectoral: "2x30s cada lado"
    - band_pull_apart: "3x15"
    - floor_angel: "3x10"
```

---

## 6. Uso en el Sistema
1. El generador de rutinas consulta este inventario.
2. Se filtra por USR-03 (equipamiento disponible).
