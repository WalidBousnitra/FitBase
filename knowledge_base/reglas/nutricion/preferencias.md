---
id: "REG-NUT-02"
nombre: "Preferencias Alimentarias"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["USR-PER-01", "REG-NUT-01"]
tags: ["reglas", "nutricion", "preferencias", "alimentos"]
---

# Preferencias Alimentarias

## 1. Propósito
Filtros y preferencias que condicionan la selección de alimentos y la logística de cocina.

---

## 2. Logística de Cocina

### Quién Cocina
| Comida | Responsable | Notas |
|--------|-------------|-------|
| Desayuno | Usuario | Puede prepararlo él |
| Snacks | Usuario | Puede prepararlos él |
| Comida (mediodía) | Madre | Cocina principal de casa |
| Cena | Variable | Depende del día |

### Tiempo de Preparación
| Día | Tiempo Máximo | Notas |
|-----|---------------|-------|
| L-V | 30 min/día | Poco tiempo disponible |
| S-D | Mucho más | Puede dedicar más tiempo |

### Implicación en la App
```
Comidas que prepara él (desayuno, snacks):
  → Recetas simples, <15 min preparación
  → Ingredientes básicos

Comidas que prepara madre:
  → Platos más elaborados permitidos
  → Comunicar macros objetivo, ella adapta
```

---

## 3. Restricciones Obligatorias

| Restricción | Aplica | Notas |
|-------------|--------|-------|
| Halal | ✅ SÍ | Obligatorio (ver cultura.md) |
| Gluten-free | ❌ No | — |
| Lactose-free | ❌ No | — |
| Vegetariano | ❌ No | — |
| Vegano | ❌ No | — |

---

## 4. Alimentos Favoritos

```yaml
PROTEINAS_TOP:
  - pollo           # Principal, económico
  - atun            # Principal, económico
  - huevos          # Principal, económico
  - ternera         # Ocasional (precio)
  - salmon          # Ocasional (precio)
  - whey_iso        # Suplemento
  - caseina         # Suplemento nocturno

CARBOS_TOP:
  - arroz           # Base
  - pasta           # Base
  - avena           # Solo si se prepara rica (no sola)

GRASAS_TOP:
  - aceite_oliva    # Principal
  - frutos_secos    # Snack

VEGETALES_TOP:
  - todos           # Excepto setas

FRUTAS_TOP:
  - platano         # Siempre en casa
  - manzana         # Siempre en casa
  - mandarinas      # Temporada
  - fresas          # Temporada
  - cerezas         # Temporada
  - sandia          # Temporada
```

> 💡 **Nota avena**: El usuario la aborreció por comerla sola. Considerar recetas: overnight oats con fruta, gachas con canela y plátano, o en batidos.

---

## 5. Alimentos Excluidos (NO Me Gustan)

```yaml
ALIMENTOS_EXCLUIDOS:
  - cerdo           # No come
  - setas           # No le gustan
  - champinones     # No incluido en setas
```

> ⚠️ **Motor de dieta**: Nunca sugerir estos alimentos en planes nutricionales.

---

## 6. Equipamiento de Cocina

> Referencia: `usuario/equipamiento.md`

| Equipo | Disponible |
|--------|------------|
| Horno | ✅ |
| Airfryer | ✅ |
| Batidora | ✅ |
| Microondas | ✅ |
| Shakers | ✅ |
| Báscula alimentos | ✅ |

### Prioridad de Cocción
1. Airfryer (rápido, saludable)
2. Horno (para madre, más tiempo)
3. Microondas (calentar, emergencias)

---

## 7. Reglas de Filtrado

1. **Halal obligatorio**: Nunca sugerir cerdo ni derivados
2. **Exclusión absoluta**: Nunca sugerir `ALIMENTOS_EXCLUIDOS`
3. **Priorización**: Ordenar por `*_TOP` cuando posible
4. **Tiempo**: 
   - Recetas usuario: ≤15 min
   - Recetas madre: sin límite
5. **Equipamiento**: Solo métodos disponibles

---

## 8. Uso en el Sistema

```
Generador de menús:
  1. Aplicar filtro Halal
  2. Excluir alimentos no gustados
  3. Priorizar favoritos
  4. Asignar recetas según quién cocina:
     - Usuario → simples, rápidas
     - Madre → pueden ser elaboradas
  5. Generar lista de compra semanal
```
