---
id: "USR-03"
nombre: "Equipamiento Disponible"
fecha_modificacion: "17/06/2026"
estado: "ACTIVO"
relacionados: ["USR-02", "REG-ENT-02"]
tags: ["equipamiento", "gimnasio", "cocina", "dispositivos"]
---

# Equipamiento Disponible

## 1. Alcance
Inventario de recursos físicos disponibles para entrenamiento, cocina y tracking.

---

## 2. Gimnasio Comercial

### Datos Generales
| Campo | Valor |
|-------|-------|
| Nombre | Fitness Park |
| Tipo | Comercial |
| Distancia | 20 min en bus |

### Máquinas
| Máquina | Notas |
|---------|-------|
| Hack squat clásica | Pierna |
| Hammer péndulo | Pierna |
| Leg press | Pierna |
| Curl femoral tumbado | Isquios |
| Extensión de cuádriceps | Cuádriceps |
| Aductores | Pierna |
| Abductores | Pierna |
| Glute machine / Hip thrust | Glúteo |
| Jalón al pecho (polea) | Múltiples agarres |
| Remo sentado (polea) | Múltiples agarres |
| Remo máquina (no polea) | Espalda |
| Pec deck / Aperturas | Pecho |
| Press máquina multiángulo | Todos los ángulos |
| Press hombro máquina | Hombros |
| Elevaciones laterales máquina | Sentado |
| Curl predicador (máquina) | Bíceps |

### Barras
| Tipo | Detalles |
|------|----------|
| Olímpica | Set Eleiko (discos) |
| Barra Z | Curl/extensiones |
| Trap bar / Hex bar | Peso muerto, encogimientos |

### Mancuernas
| Rango | Incrementos |
|-------|-------------|
| 4 - 60 kg | Saltos de 2 kg |

### Poleas
- Alta, baja, cruce ✓

### Cardio
| Equipo |
|--------|
| Cintas |
| Escaleras (stepper) |
| Máquinas aeróbicas |
| Bicicletas |
| Remos |

### Otros
| Equipo |
|--------|
| Racks de sentadilla |
| Plataforma de peso muerto |
| Barras de dominadas |
| Bancos (plano, inclinado, declinado) |
| Smith machine |
| Kettlebells (4 pesos distintos) |
| Rueda de abdominales |
| Balones medicinales |
| Cuerdas battle rope |
| Seal row (banco elevado) |
| Barra T con banco |

---

## 3. Casa

### Equipamiento Disponible
| Equipo | Uso |
|--------|-----|
| Bandas elásticas | Resistencia variable |
| Foam roller | Liberación miofascial |
| Máquina de agarre | Grip/antebrazo |
| Esterilla/colchoneta | Estiramientos, core |

### Posibles Compras
| Equipo | Prioridad |
|--------|----------|
| Rueda abdominal | Si necesario |
| Cuerda de saltar | Si necesario |

---

## 4. Parque (Calistenia)

### Ubicación
- Junto a casa (andando)

### Equipamiento
| Equipo | Notas |
|--------|-------|
| Barra de calistenia | Dominadas, colgarse |
| Espalderas | Movilidad, colgarse |

### Pista
| Tipo | Superficie |
|------|------------|
| Correr/caminar | Tierra dura |

---

## 5. Cocina

### Electrodomésticos
| Equipo | Disponible | Uso Típico |
|--------|------------|------------|
| Horno | ✓ | Asados, gratinados |
| Airfryer | ✓ | Fritura saludable |
| Batidora | ✓ | Batidos, salsas |
| Microondas | ✓ | Calentar, descongelar |
| Shakers | ✓ | Batidos proteína |
| Báscula alimentos | ✓ | Pesar ingredientes |

---

## 6. Dispositivos de Tracking

| Dispositivo | Modelo | Métricas |
|-------------|--------|----------|
| Smartwatch | Amazfit GTS 4 | Sueño, pasos, FC, HRV, VO2max |
| Báscula | Xiaomi (bioimpedancia) | Peso, grasa, agua, músculo |
| Móvil | Android | Health Connect |

---

## 7. Uso en el Sistema

### Filtrado de Ejercicios
```
SI ejercicio.requiere("hack_squat") → gym_disponible ✓
SI ejercicio.requiere("kettlebell") → gym_disponible ✓
SI ejercicio.requiere("trap_bar") → gym_disponible ✓
SI ejercicio.requiere("barra_dominadas") → parque_disponible ✓
SI ejercicio.requiere("cable_crossover") → gym_disponible ✓
SI ejercicio.requiere("GHD") → NO disponible ✗
```

### Generador de Dietas
- Usa electrodomésticos para métodos de cocción
- Prioriza: Airfryer > Horno > Microondas

### Ubicaciones por Día
| Día | Ubicación Preferida |
|-----|---------------------|
| L (teletrabajo) | Fitness Park |
| V (teletrabajo) | Fitness Park |
| S | Fitness Park o Parque |
| Otros | Casa (bandas, foam roller) |
