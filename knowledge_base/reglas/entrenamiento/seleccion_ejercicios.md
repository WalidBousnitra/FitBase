---
id: "REG-ENT-02"
nombre: "Reglas de Selección de Ejercicios"
fecha_modificacion: "23/06/2026"
estado: "ACTIVO"
relacionados: ["USR-04", "EVI-03", "REG-ENT-01"]
tags: ["reglas", "ejercicios", "seleccion", "patrones"]
---

# Reglas de Selección de Ejercicios

> **Propósito**: Lógica para que la IA seleccione ejercicios basándose en evidencia.
> Los ejercicios específicos se GENERAN, no están hardcodeados.

---

## 1. Principios de Selección

### Fuentes de Evidencia
- `evidencia/hipertrofia.md` - Patrones de movimiento efectivos
- `evidencia/lesiones.md` - Ejercicios seguros
- `evidencia/postura.md` - Ejercicios correctivos

### Inputs del Usuario
- `usuario/preferencias_ejercicios.md` - Favoritos y exclusiones
- `usuario/biometria.md` - Lesiones y limitaciones
- `reglas/gimnasio/inventario.md` - Equipamiento disponible

---

## 2. Reglas de Priorización

```yaml
ORDEN_SELECCION:
  1. Verificar que NO esté en lista de exclusiones
  2. Verificar que el equipamiento esté disponible
  3. Verificar que NO agrave lesiones del usuario
  4. Priorizar favoritos del usuario
  5. Asegurar cobertura de patrones de movimiento
```

---

## 3. Patrones de Movimiento Requeridos

> Todo plan debe cubrir estos patrones para equilibrio muscular.

| Patrón | Descripción | Ejemplos Genéricos |
|--------|-------------|-------------------|
| **Empuje horizontal** | Alejar carga del torso | Press banco, flexiones |
| **Empuje vertical** | Empujar hacia arriba | Press militar |
| **Empuje lateral** | Abducción hombro | Elevaciones laterales |
| **Tirón horizontal** | Acercar carga al torso | Remos |
| **Tirón vertical** | Tirar hacia abajo | Dominadas, jalones |
| **Extensión rodilla** | Sentadilla, leg press | Sentadilla |
| **Extensión cadera** | Hip hinge | RDL, hip thrust |
| **Core anti-extensión** | Resistir extensión lumbar | Plancha |
| **Core anti-rotación** | Resistir rotación | Pallof press |

---

## 4. Reglas de Volumen por Ejercicio

> Fuente: `evidencia/hipertrofia.md` (Schoenfeld 2017)

```yaml
SERIES_POR_EJERCICIO:
  minimo: 2
  maximo: 5
  tipico: 3-4
  
REPS_POR_OBJETIVO:
  fuerza: 3-6 reps
  hipertrofia: 6-12 reps
  resistencia: 12-20 reps
```

---

## 5. Reglas de Progresión

> Fuente: `evidencia/hipertrofia.md` (Helms 2016)

```yaml
SISTEMA_RIR:
  semana_1: RIR 3-4 (lejos del fallo)
  semana_2: RIR 2-3
  semana_3: RIR 1-2 (cerca del fallo)
  semana_4: DELOAD o test
```

---

## 6. Reglas para Lesiones

```yaml
SI_LESION_CODO:
  evitar:
    - Extensión triceps bajo carga pesada
    - Press francés profundo
    - Fondos con peso
  permitir:
    - Extensiones con cable (rango controlado)
    - Press compuestos (hombro y pecho)
    
SI_LESION_HOMBRO:
  evitar:
    - Press tras nuca
    - Elevaciones por encima de 90°
  permitir:
    - Press inclinado (ángulo moderado)
    - Remos
```

---

## 7. Ejercicios Correctivos (Postura)

> Fuente: `evidencia/postura.md`

```yaml
SI_OBJETIVO_POSTURA:
  incluir_siempre:
    - Face pulls (retracción escapular)
    - Rotación externa hombro
    - Extensión torácica
    - Fortalecimiento core
    
PARA_WALL_ANGELS:
  ejercicios_preparatorios:
    - Chin tucks (cabeza adelantada)
    - Pec stretch (hombros redondeados)
    - Foam roll torácico (cifosis)
    - Prone Y-T-W (debilidad trapecio bajo)
```

---

## 8. Output Esperado de la IA

Cuando la IA genere el plan, debe producir:

```yaml
EJERCICIO_GENERADO:
  nombre: "[Nombre del ejercicio]"
  patron: "[Patrón de movimiento]"
  grupo_principal: "[Músculo principal]"
  equipamiento: "[Del inventario]"
  series: X
  reps: "X-X"
  RIR_objetivo: X
  notas: "[Si hay consideraciones]"
```

**NO debe haber pesos predefinidos.** Los pesos se determinan en la primera sesión basándose en el RIR objetivo.
