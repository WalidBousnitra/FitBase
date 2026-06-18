---
id: "REG-LOG-03"
nombre: "Excepciones y Contingencias"
fecha_modificacion: "18/06/2026"
estado: "ACTIVO"
relacionados: ["REG-LOG-01", "REG-ENT-01", "REG-NUT-01"]
tags: ["reglas", "logica", "excepciones", "contingencia", "ramadan"]
---

# Excepciones y Contingencias

## 1. Alcance
Reglas para manejar situaciones atípicas que alteran la programación normal.

> ⚠️ **Usuario practica Ramadán** - La sección de Ramadán está activa y personalizada.

## 2. Tipos de Excepción

### Viajes
```yaml
EXCEPCION_VIAJE:
  trigger: "usuario marca viaje activo"
  duracion: [fecha_inicio, fecha_fin]
  ajustes:
    - Reducir frecuencia a 2-3 días/semana
    - Priorizar ejercicios bodyweight
    - Relajar tracking nutricional (mantener proteína)
    - Aumentar meta de pasos (+2000)
```

### Enfermedad
```yaml
EXCEPCION_ENFERMEDAD:
  trigger: "usuario reporta enfermedad"
  ajustes:
    - Suspender entrenamiento intenso
    - Permitir cardio ligero si síntomas son cuello-arriba
    - Mantener calorías de mantenimiento
    - Priorizar sueño y hidratación
  retorno:
    - 1 semana leve tras síntomas
    - Reducir cargas 20% primera semana
```

### Lesión
```yaml
EXCEPCION_LESION:
  trigger: "usuario reporta lesión"
  datos_requeridos: [zona_afectada, severidad, restricciones_medicas]
  ajustes:
    - Excluir ejercicios que involucren zona
    - Sugerir alternativas que no comprometan
    - Reducir volumen total 30%
    - Añadir trabajo de rehabilitación
```

### Ramadán
```yaml
EXCEPCION_RAMADAN:
  trigger: "RAMADAN_ACTIVO == true"
  usuario_practica: true  # Confirmado 18/06/2026
  
  ajustes_entrenamiento:
    - Entrenar después de Iftar (preferido) o antes de Suhur
    - Reducir volumen 20-30%
    - Mantener intensidad, reducir series
    - Evitar entrenos largos (max 45-60 min)
    - Priorizar ejercicios compuestos sobre accesorios
    
  ajustes_nutricion:
    - Meta calorías: Mantenimiento (2855 kcal) vs bulk
    - Proteína: Mantener 156g distribuidos en 2-3 comidas
    - Hidratación: 3-4L en ventana nocturna
    - Suhur: Proteína lenta (caseína) + carbos complejos + grasas
    - Iftar: Comenzar ligero, luego comida principal
    
  distribucion_comidas_ramadan:
    suhur: 
      - "~1000 kcal"
      - "50g proteína (caseína + huevos)"
      - "Avena/arroz + frutos secos"
    iftar_inicial:
      - "Dátiles + agua"
      - "Sopa/caldo"
    cena_principal:
      - "~1200 kcal"
      - "60g proteína"
      - "Carbos + vegetales"
    pre_sueno:
      - "~650 kcal"
      - "46g proteína (caseína)"
      - "Snack con grasas"
```

### Estrés Extremo
```yaml
EXCEPCION_ESTRES:
  trigger: "SUB_ESTRES > 8 por 3+ días"
  ajustes:
    - Reducir volumen 40%
    - Priorizar trabajo de baja intensidad
    - Sugerir meditación/respiración
    - Mantener rutina pero reducir presión
```

## 3. Prioridad de Excepciones
```
1. Lesión (máxima prioridad - seguridad)
2. Enfermedad
3. Estrés extremo
4. Ramadán
5. Viajes
```

## 4. Reglas de Retorno
1. Toda excepción tiene protocolo de retorno gradual
2. Nunca volver a 100% inmediatamente
3. Monitorear métricas subjetivas en retorno

## 5. Uso en el Sistema
1. La app detecta excepciones y aplica ajustes automáticos.
2. El usuario puede activar/desactivar excepciones manualmente.