---
id: "REG-NUT-01"
nombre: "Motor de Dieta"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-01", "USR-02", "EVI-11", "REG-NUT-02"]
tags: ["reglas", "nutricion", "motor", "calorias", "macros"]
---

# Motor de Dieta

## 1. Alcance
Algoritmo para calcular y ajustar calorías y macronutrientes basados en objetivos y actividad.

## 2. Datos del Usuario Actual

| Variable | Valor | Fuente |
|----------|-------|--------|
| Peso | 78.2 kg | `biometria.md` |
| Altura | 188 cm | `biometria.md` |
| Edad | 24 años | `biometria.md` |
| Sexo | Hombre | `biometria.md` |
| Actividad | 4 gym + 2 natación + trabajo sedentario | `horarios.md` |
| Factor actividad | 1.55 (conservador) → ver nota §3 | Heurístico |
| Pasos promedio | 7390/día | `biometria.md` |
| Objetivo | **Bulk limpio** (+4.8 kg) | `prioridades.md` |
| Comidas | 3 + snacks | Input usuario |

## 3. Cálculo de Metabolismo Basal

> ✅ **Fuente**: Mifflin et al. (1990) - `evidencia/nutricion.md` § 2

### Fórmula Mifflin-St Jeor (VALIDADA)
```
Hombres: BMR = (10 × peso_kg) + (6.25 × altura_cm) - (5 × edad) + 5
Mujeres: BMR = (10 × peso_kg) + (6.25 × altura_cm) - (5 × edad) - 161
```

> **R² = 0.71** — Más precisa que Harris-Benedict (que sobreestima 5%)

### Factor de Actividad (HEURÍSTICO)
> ⚠️ **Nota**: Estos factores son estimaciones heurísticas comúnmente usadas.
> No hay paper único que los valide; usar con criterio y ajustar según feedback.
>
> **Decisión**: Se usa 1.55 (Moderado) a pesar de entrenar 6 días porque:
> 1. El trabajo es 100% sedentario (oficina)
> 2. Natación 2x/sem es baja intensidad (principiante, ~250 kcal/sesión)
> 3. Es mejor infraestimar y ajustar al alza que sobreestimar y acumular grasa
> 4. Si el peso se estanca en bulk durante 2+ semanas → subir a 1.65 manualmente

| Nivel | Factor | Descripción |
|-------|--------|-------------|
| Sedentario | 1.2 | Trabajo oficina, sin ejercicio |
| Ligero | 1.375 | Ejercicio 1-3 días/semana |
| Moderado | 1.55 | Ejercicio 3-5 días/semana |
| Activo | 1.725 | Ejercicio 6-7 días/semana |
| Muy activo | 1.9 | Trabajo físico + ejercicio diario |

## 4. Ajuste por Objetivo

> ✅ **Fuente**: Iraki et al. (2019) - `evidencia/nutricion.md`

| Objetivo | Ajuste Calórico | Ganancia/Pérdida Peso |
|----------|-----------------|----------------------|
| Pérdida grasa | TDEE × 0.80-0.90 | 0.5-1% peso/semana (Helms 2014) |
| Mantenimiento | TDEE × 1.0 | — |
| Ganancia muscular | TDEE × **1.10-1.20** | **0.25-0.5% peso/semana** |

> **Nota**: Avanzados deben ser más conservadores (menor potencial de ganancia).

## 5. Distribución de Macros

> ✅ **Fuentes**: Helms et al. (2014), Iraki et al. (2019) - `evidencia/nutricion.md`

### Proteína (Prioridad máxima)
| Contexto | g/kg/día | Fuente |
|----------|----------|--------|
| **Ganancia muscular (bulk)** | **1.6 - 2.2** | Iraki 2019 |
| Pérdida grasa (cut) | 2.3 - 3.1 g/kg masa magra | Helms 2014 |
| Recomposición | >2.0 | Barakat 2020 |

### Grasas
| Contexto | Recomendación | Fuente |
|----------|---------------|--------|
| **Bulk** | **0.5-1.5 g/kg/día** (~20-30% kcal) | Iraki 2019 |
| Cut | 15-30% calorías | Helms 2014 |

### Carbohidratos
| Contexto | Recomendación | Fuente |
|----------|---------------|--------|
| **Bulk** | **≥3-5 g/kg/día** | Iraki 2019 |
| General | Resto de calorías | — |

## 6. Ajustes Dinámicos

> **Fuente datos**: Variables `HC_*` vienen de Health Connect → [hardware.md](../../usuario/metricas/hardware.md)

### Por Actividad Diaria
```
Si HC_STEPS > 12000:
  → Añadir 150-200 kcal (pasos extra queman calorías)

Si DIA_ENTRENAMIENTO:
  → Añadir carbos pre/post entreno
```

### Por Métricas de Sueño
```
Si HC_SLEEP_SCORE < 60:
  → Reducir volumen de entrenamiento, mantener calorías
  → (Nota: Sleep Score calculado desde duración + fases, no nativo de Zepp)
```

## 7. Cálculo Actual del Usuario

> ⚠️ **Nota**: BMR y TDEE calculados con fórmulas de referencia (sin paper). Macros SÍ respaldados por evidencia.

```yaml
# BMR (Mifflin-St Jeor) - REFERENCIA GENERAL
BMR_estimado: ~1842 kcal
  # (10 × 78.2) + (6.25 × 188) - (5 × 24) + 5

# TDEE (Factor ~1.55) - REFERENCIA GENERAL
TDEE_estimado: ~2855 kcal

# Objetivo: Bulk limpio
# Superávit: +10-20% (Iraki 2019) → +15% = +428 kcal
CALORIAS_OBJETIVO: ~3280 kcal

# Macros (CON EVIDENCIA - Iraki 2019)
PROTEINA:
  rango_evidencia: "1.6-2.2 g/kg"
  aplicado: 2.0 g/kg
  gramos: 156g
  calorias: 624 kcal
  
GRASAS:
  rango_evidencia: "0.5-1.5 g/kg (20-30% kcal)"
  aplicado: 1.0 g/kg (~24% kcal)
  gramos: 78g
  calorias: 702 kcal
  
CARBOS:
  rango_evidencia: "≥3-5 g/kg"
  aplicado: resto (~488g = 6.2 g/kg)
  gramos: 488g
  calorias: 1952 kcal
```

### Distribución por Comida (3 + snacks)

> ✅ **Fuente**: 0.40-0.55 g/kg proteína/comida (Iraki 2019)
> A 78.2 kg → máximo óptimo por comida: ~31-43g

| Comida | Calorías | Proteína | Carbos | Grasas |
|--------|----------|----------|--------|--------|
| **Desayuno** | ~720 kcal | 40g | 105g | 16g |
| **Comida** | ~920 kcal | 42g | 140g | 23g |
| **Cena** | ~920 kcal | 42g | 140g | 23g |
| **Snacks** | ~720 kcal | 32g | 103g | 16g |
| **TOTAL** | ~3280 kcal | 156g | 488g | 78g |

> 💡 **Días de entreno**: Priorizar carbos pre/post entreno (evidencia: Helms 2014)

### Snacks Preferidos
| Alimento | Porción | Calorías | Carbos | Notas |
|----------|---------|----------|--------|-------|
| **Dátiles** | 3-4 unidades (~30g) | ~80 kcal | ~20g | Favorito, carbos rápidos pre-entreno |
| Frutos secos | 30g | ~180 kcal | ~5g | Grasas saludables |
| Fruta fresca | 1 pieza | ~60-100 kcal | ~15-25g | Variable |

---

## 8. Suplementación Actual

| Suplemento | Dosis | Momento | Propósito |
|------------|-------|---------|----------|
| **Whey ISO** | ~25g | Post-entreno | Proteína rápida |
| **Caseína** | ~25g | Antes dormir | Proteína lenta |
| **Vitamina D + K** | Según etiqueta | Mañana con grasa | Huesos, inmunidad |
| **Omega-3** | ~2g EPA+DHA | Con comida | Inflamación, cerebro |
| **Magnesio** | ~400mg | Noche | Sueño, recuperación |
| **Ashwagandha** | ~600mg | Mañana o noche | Cortisol, estrés |
| **Picolinato de cromo** | Según etiqueta | Con comida | Sensibilidad insulina |

> ✅ **Stack sólido** para bulk + estrés + sueño. 
> 
> ⚠️ **Creatina**: NO toma por preocupación de caída de pelo (tiene alopecia androgénica). Aunque la evidencia es limitada (1 estudio sobre DHT), se respeta la preferencia del usuario.

---

## 9. Restricciones Culturales
Ver `usuario/perfil/cultura.md`:
- Aplicar filtro Halal si activo
- Ajustar timing si Ramadán activo

## 10. Uso en el Sistema
1. El backend calcula macros diarios.
2. La app muestra progreso y sugiere ajustes.
