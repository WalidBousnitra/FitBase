---
id: "REG-NAT-01"
nombre: "Natación"
fecha_modificacion: "23/06/2026"
estado: "ACTIVO"
relacionados: ["USR-PER-02", "REG-ENT-01"]
tags: ["reglas", "natacion", "piscina"]
---

# Natación

## 1. Alcance
Datos del usuario sobre clases de natación. La IA debe integrar esto en la programación.

---

## 2. Datos de la Actividad (Usuario)

| Variable | Valor | Notas |
|----------|-------|-------|
| Frecuencia | 2 días/semana | Días específicos: XXX |
| Duración | 1 hora | Clase completa |
| Tipo | Clases dirigidas | Aprendizaje |
| Nivel | Principiante | Objetivo: aprender a nadar |

---

## 3. Integración en Programación

```yaml
CONSIDERACIONES:
  - Contar como día de actividad física
  - NO reemplaza entrenamiento de fuerza
  - Puede usarse como día de recuperación activa
  
LA_IA_DEBE:
  - Consultar evidencia/cardio.md para efectos cardiovasculares
  - Evitar entrenar tren superior el mismo día (fatiga hombros)
  - Ajustar TDEE si es significativo para el gasto calórico
```

---

## 4. Notas

> La IA debe buscar evidencia sobre natación en `/evidencia/` si necesita datos específicos.
> Si no hay evidencia disponible, NO inventar datos.
