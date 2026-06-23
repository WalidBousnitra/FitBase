---
id: "USR-02"
nombre: "Biometría"
fecha_modificacion: "23/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-03", "EVI-11", "EVI-14"]
tags: ["biometria", "medidas", "tracking"]
---

# Biometría

> **Propósito**: Datos físicos del usuario. Los OBJETIVOS se calculan dinámicamente usando reglas de `/evidencia/`.

---

## 1. Datos Fijos

| Métrica | Valor | Notas |
|---------|-------|-------|
| Fecha nacimiento | 20/07/2001 | — |
| Sexo | Hombre | — |
| Altura | 188 cm | — |
| Envergadura | 193 cm | Medir: brazos extendidos |
| Talla calzado | 45 EU | — |

---

## 1.1 Fechas del Programa

| Campo | Valor | Notas |
|-------|-------|-------|
| **FECHA_INICIO** | 31/08/2026 | Día que empiezas el programa real |
| **DURACION_MESES** | 11 | Duración total del plan |

---

## 2. Composición Corporal (Báscula Xiaomi)

**Fecha medición**: 18/06/2026

| Métrica | Valor | Unidad |
|---------|-------|--------|
| Peso | 78.2 | kg |
| Grasa corporal | 18.9 | % |
| Masa muscular | 60.2 | kg |
| Grasa visceral | 9 | nivel |
| Masa ósea | ~3 | kg |
| Agua corporal | ~55 | % |
| Proteína | ~18 | % |
| Metabolismo basal | ~1850 | kcal |
| Edad metabólica | ~22 | años |
| IMC | 22.1 | — |

---

## 3. Circunferencias (Cinta Métrica)

> 📏 **Protocolo**: Relajado (no flexionar), mañana, mismo punto anatómico.

| Zona | Cómo Medir | Valor (cm) |
|------|------------|------------|
| Hombros | Punto más ancho deltoides | 107cm |
| Pecho | Línea de pezones | 93cm |
| Cintura | A nivel del ombligo | 88cm |
| Cadera | Punto más ancho glúteos | 101cm |
| Bíceps (D) | Flexionado, pico | 35,5cm |
| Bíceps (I) | Flexionado, pico | 36cm |
| Antebrazo (D) | Punto más grueso | 28cm |
| Antebrazo (I) | Punto más grueso | 28cm |
| Muslo (D) | Punto más grueso | 58cm |
| Muslo (I) | Punto más grueso | 58cm |
| Pantorrilla (D) | Punto más grueso | 36cm |
| Pantorrilla (I) | Punto más grueso | 36cm |

---

## 4. Fuerza (Registros Actuales)

**Fecha**: 18/06/2026

| Ejercicio | Peso | Reps | Notas |
|-----------|------|------|-------|
| Sentadilla | 80 kg | ? | Actual (PR histórico: **130 kg**) |
| Press inclinado | 18 kg | 10 | Mancuernas |
| RDL | 14 kg | 12 | Mancuernas |
| Dominadas | BW (78kg) | 3-4 | Solo peso corporal |
| Remo neutro | 40 kg | 10 | Polea |
| Hip thrust | 20 kg | 8 | — |
| Curl predicador | 15 kg | 12 | — |
| Kelso shrug | 10 kg | 15 | — |

---

## 5. Métricas Cardiovasculares (Amazfit GTS 4)

**Fecha**: 18/06/2026

| Métrica | Valor | Unidad |
|---------|-------|--------|
| VO2max estimado | 50 | ml/kg/min |
| FC reposo | 53 | bpm |
| FC máxima (medida) | ~196 | bpm |

---

## 6. Sueño (Amazfit GTS 4)

**Promedio**

| Métrica | Valor |
|---------|-------|
| Sleep Score | 83 /100 |
| Sueño profundo | 18% |
| Sueño REM | 27% |
| Sueño ligero | 5 min |
| Despertares | 0 |
| Duración total | 7h |

---

## 7. Actividad Diaria

| Métrica | Valor |
|---------|-------|
| Pasos diarios (promedio) | 7390 |
| Estrés promedio | 5-60 |

---

## 8. Movilidad y Postura

### Tests de Movilidad
| Test | Resultado | Notas |
|------|-----------|-------|
| Tocarse pies | ✅ Sí | Isquiotibiales OK |
| Sentadilla profunda | ✅ Sí | Talones suelo, torso recto |
| Wall angels | ❌ **NO** | **OBJETIVO PRINCIPAL** - No puede en ninguna variante |

### Evaluación Postural
| Desviación | Presente | Severidad |
|------------|----------|-----------|
| Cabeza adelantada | ✅ Sí | Moderada |
| Hombros redondeados | ✅ Sí | Moderada |
| Cifosis torácica | ✅ Sí | **Severa** |
| Lordosis lumbar | ✅ Sí | **Severa** |
| Inclinación pélvica anterior | ✅ Sí | **Severa** |

> ⚠️ **Síndrome cruzado superior** (Upper Cross Syndrome) detectado.

---

## 9. Lesiones y Limitaciones

| Zona | Problema | Ejercicios a Evitar |
|------|----------|---------------------|
| **Codo** | Dolor nervioso/tendinoso (chispazo, calambre) | Press francés, fondos, extensión completa bajo carga |

---

## 10. Perfil de Entrenamiento

```yaml
EXPERIENCIA: "3 años (entrenamiento casual/inconsistente)"
NIVEL: "Principiante-Intermedio"
GENETICA: "Autopercibida como buena"
```

---

## 11. Protocolo de Tracking

| Métrica | Frecuencia | Herramienta |
|---------|------------|-------------|
| Peso | Diaria (media semanal) | Báscula Xiaomi |
| Composición | Semanal (domingo mañana) | Báscula Xiaomi |
| Circunferencias | Cada 4 semanas | Cinta métrica |
| Fotos progreso | Mensual | Cámara |
| Tests movilidad | Cada 4 semanas | Manual |
