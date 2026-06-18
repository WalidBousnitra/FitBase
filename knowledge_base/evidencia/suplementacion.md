---
id: "EVI-13"
nombre: "Evidencia: Suplementación"
fecha_modificacion: "17/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "EVI-11", "REG-NUT-01"]
tags: ["evidencia", "suplementos", "ergogenicos", "creatina", "cafeina"]
prioridad: "soporte"
---

# Suplementación

> **Soporte** — Base científica para recomendaciones de suplementos

## 1. Alcance
Evidencia científica sobre suplementos con respaldo científico real.

---

## 2. FUENTES

### Fuentes Principales
- **Helms, Aragon & Fitschen (2014)** - "Evidence-based recommendations for natural bodybuilding contest preparation: nutrition and supplementation"
- **Iraki et al. (2019)** - "Nutrition Recommendations for Bodybuilders in the Off-Season"
- **NSCA Essentials of Strength Training and Conditioning**

---

## 3. CLASIFICACIÓN DE SUPLEMENTOS

### TIER 1: EVIDENCIA FUERTE

| Suplemento | Beneficio | Dosis | Timing | Notas |
|------------|-----------|-------|--------|-------|
| **Creatina monohidrato** | Fuerza, potencia, hipertrofia | **3-5 g/día** | Cualquier momento | No necesita carga |
| **Cafeína** | Rendimiento, energía, focus | **3-6 mg/kg** | 30-60 min pre-entreno | Tolerancia variable |
| **Beta-alanina** | Resistencia muscular | **3-5 g/día** | Dividido en dosis | Hormigueo normal |
| **Proteína en polvo** | Conveniencia | Según necesidad | Post-entreno o cuando convenga | No superior a comida |

### TIER 2: EVIDENCIA MODERADA

| Suplemento | Beneficio | Dosis | Notas |
|------------|-----------|-------|-------|
| **Citrulina malato** | Bombeo, rendimiento | **6-8 g** pre-entreno | Para sesiones de volumen |
| **Vitamina D** | Salud general, inmunidad | **1000-4000 UI/día** | Si hay deficiencia |
| **Omega 3 (EPA/DHA)** | Antiinflamatorio | **2-3 g/día** | Si no comes pescado |
| **Multivitamínico** | Cobertura micronutrientes | Según etiqueta | Si dieta es restringida |

### TIER 3: EVIDENCIA LIMITADA/CONDICIONAL

| Suplemento | Veredicto | Notas |
|------------|-----------|-------|
| HMB | Posible en déficit | Solo para prevenir catabolismo |
| Ashwagandha | Posible reducción cortisol | Más investigación necesaria |
| Melatonina | Para sueño | 0.5-3 mg antes de dormir |

### SIN EVIDENCIA SUFICIENTE (No Recomendados)

| Suplemento | Veredicto |
|------------|-----------|
| **BCAAs** | Innecesarios si proteína es suficiente |
| **Glutamina** | No mejora rendimiento ni recuperación |
| **Boosters de testosterona** | NO funcionan |
| **ZMA** | Sin beneficio si no hay deficiencia |
| **CLA** | Efecto mínimo en grasa |
| **L-carnitina** | Sin efecto en composición corporal |
| **Óxido nítrico boosters** | Mejor usar citrulina directamente |

---

## 4. DETALLE DE SUPLEMENTOS TIER 1

### Creatina Monohidrato

| Aspecto | Detalle |
|---------|---------|
| **Mecanismo** | ↑ Fosfocreatina → más ATP para esfuerzos cortos |
| **Beneficios** | +5-10% fuerza, +1-2kg masa muscular |
| **Dosis** | 3-5 g/día (no necesita carga) |
| **Timing** | Cualquier momento, consistencia es clave |
| **Forma** | Monohidrato (la más estudiada y barata) |
| **Efectos secundarios** | Retención de agua intramuscular (positivo) |

```yaml
PROTOCOLO_CREATINA:
  dosis: 5 g/dia
  forma: monohidrato
  timing: con_comida_cualquiera
  duracion: continuo
  carga: no_necesaria
```

### Cafeína

| Aspecto | Detalle |
|---------|---------|
| **Mecanismo** | Bloquea adenosina → ↓ fatiga percibida |
| **Beneficios** | ↑ Fuerza, ↑ resistencia, ↑ focus |
| **Dosis** | 3-6 mg/kg (200-400 mg típico) |
| **Timing** | 30-60 min pre-entreno |
| **Tolerancia** | Se desarrolla, ciclar si es necesario |
| **Corte** | 6+ horas antes de dormir |

```yaml
PROTOCOLO_CAFEINA:
  dosis_baja: 3 mg/kg
  dosis_alta: 6 mg/kg
  timing: 30-60_min_pre
  max_diario: 400 mg
  no_despues_de: 14:00-16:00 (para sueño)
```

### Beta-Alanina

| Aspecto | Detalle |
|---------|---------|
| **Mecanismo** | ↑ Carnosina → buffer de lactato |
| **Beneficios** | +2-3% rendimiento en esfuerzos 1-4 min |
| **Dosis** | 3-5 g/día dividido |
| **Timing** | No importa, efecto es crónico |
| **Efecto secundario** | Parestesia (hormigueo) - inofensivo |

---

## 5. RECOMENDACIONES SEGÚN OBJETIVO

### Para Hipertrofia
```yaml
STACK_HIPERTROFIA:
  esencial:
    - creatina: 5 g/dia
    - proteina: segun_necesidad
  opcional:
    - cafeina: pre_entreno
    - citrulina: 6-8 g pre
```

### Para Fuerza
```yaml
STACK_FUERZA:
  esencial:
    - creatina: 5 g/dia
    - cafeina: 3-6 mg/kg pre
  opcional:
    - beta_alanina: 3-5 g/dia
```

### Para Déficit Calórico
```yaml
STACK_DEFICIT:
  esencial:
    - proteina: aumentar_a_2.3-3.1 g/kg LBM
    - creatina: mantener_5 g/dia
  opcional:
    - cafeina: para_energia
    - vitamina_D: si_deficiente
```

---

## 6. SEÑALES DE ALERTA (Evitar)

| Categoría | Ejemplos | Por qué evitar |
|-----------|----------|----------------|
| **Propietarios** | "Matrix anabólica", mezclas secretas | No sabes qué hay |
| **Milagrosos** | "Gana 10kg en 1 mes" | Mentira |
| **Hormonales** | Pro-hormonas, SARMs | Riesgo legal y de salud |
| **Exageradamente caros** | Creatina "especial" | Monohidrato funciona igual |

---

## 7. RESUMEN PRÁCTICO

### Prioridad de Compra

| Prioridad | Suplemento | Costo/Beneficio |
|-----------|------------|-----------------|
| 1 | Creatina monohidrato | EXCELENTE |
| 2 | Proteína en polvo | BUENO (si conveniencia) |
| 3 | Cafeína | EXCELENTE |
| 4 | Vitamina D (si deficiente) | BUENO |
| 5 | Beta-alanina | MODERADO |
| 6 | Citrulina | MODERADO |

> **Regla**: Si el presupuesto es limitado, solo creatina y proteína si hace falta.

### Tier 2 (Evidencia Moderada)
| Suplemento | Beneficio | Dosis | Timing | Fuente |
|------------|-----------|-------|--------|--------|
| Beta-alanina | ⏳ | ⏳ | ⏳ | Helms |
| Citrulina | ⏳ | ⏳ | ⏳ | Helms |
| Vitamina D | ⏳ | ⏳ | ⏳ | — |
| Omega 3 | ⏳ | ⏳ | ⏳ | — |

### Tier 3 (Evidencia Limitada / No Recomendado)
| Suplemento | Veredicto | Notas |
|------------|-----------|-------|
| BCAAs | ⏳ | ¿Necesarios si proteína suficiente? |
| Glutamina | ⏳ | — |
| Boosters de testosterona | ⏳ | — |

---

## 4. Implicaciones para Reglas

Cuando se completen los datos:
- Crear `reglas/nutricion/suplementos.md` con recomendaciones
- Solo recomendar Tier 1 por defecto
- Tier 2 opcional según objetivos
