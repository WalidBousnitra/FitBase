---
id: "USR-PER-01"
nombre: "Cultura y Gastronomía"
fecha_modificacion: "17/06/2026"
estado: "PROD_ACTUAL"
relacionados: ["USR-PER-02", "REG-NUT-02", "REG-LOG-03"]
tags: ["perfil", "cultura", "gastronomia", "halal", "ramadan"]
---

# Cultura y Gastronomía

## 1. Alcance
Contexto cultural hispano-marroquí que condiciona la alimentación, el calendario y las restricciones dietéticas.

## 2. Identidad Cultural

| Variable | Valor |
|----------|-------|
| Nacimiento | España |
| Residencia actual | España |
| Origen familiar | Marruecos (ambos padres) |
| Identidad gastronómica | Fusión hispano-marroquí |
| Religión | Islam (practicante de Ramadán) |

## 3. Restricciones Alimentarias

### Obligatorias (Halal)
| Restricción | Detalle |
|-------------|---------|
| Carne | Solo Halal (sacrificio ritual) |
| Cerdo | Prohibido (jamón, bacon, chorizo, etc.) |
| Alcohol | Prohibido (incluye en cocina) |

### Logísticas
- Carnicerías Halal disponibles en zona
- Meal prep necesario para proteína

## 4. Dieta Familiar Típica

### Platos Marroquíes Habituales

La cocina marroquí casera es muy variada. Estos son los platos más frecuentes en el día a día:

#### Margas (Guisos/Estofados) — Base de la cocina diaria
| Plato | Composición | Notas Nutricionales |
|-------|-------------|---------------------|
| Marga de pollo | Pollo, patatas, zanahorias, aceitunas, caldo | Proteína alta, carbos moderados |
| Marga de ternera | Ternera, patatas, judías verdes, tomate | Proteína + carbos, grasa variable |
| Marga de judías verdes | Judías, patatas, tomate, carne opcional | Fibra alta, proteína si lleva carne |
| Marga de guisantes | Guisantes, patatas, carne, especias | Proteína vegetal + animal |
| Marga de alcachofas | Alcachofas, habas, guisantes, cordero | Rica en fibra, proteína moderada |

#### Tajines — Cocción lenta con tapa cónica
| Plato | Composición | Notas Nutricionales |
|-------|-------------|---------------------|
| Tajine de pollo con aceitunas | Pollo, aceitunas, limón encurtido | Alto en proteína, grasas por aceitunas |
| Tajine de pollo con verduras | Pollo, calabacín, zanahoria, patata | Equilibrado, proteína + carbos |
| Tajine de ternera con ciruelas | Ternera, ciruelas pasas, almendras, miel | Proteína + azúcares naturales |
| Tajine de kefta | Albóndigas, huevos, tomate | Muy alto en proteína |
| Tajine de cordero | Cordero, verduras variadas | Proteína alta, grasa moderada-alta |
| Tajine de pescado | Pescado, pimientos, tomate, chermoula | Proteína magra, omega 3 |

#### Legumbres — Muy frecuentes
| Plato | Composición | Notas Nutricionales |
|-------|-------------|---------------------|
| Lentejas (Adess) | Lentejas, tomate, cebolla, comino | Proteína vegetal, fibra alta, hierro |
| Loubia (Judías blancas) | Judías blancas, tomate, pimentón | Proteína + carbos complejos |
| Garbanzos guisados | Garbanzos, carne, verduras | Proteína mixta, carbos altos |
| Bessara | Crema de habas secas, aceite, comino | Proteína vegetal, grasas por aceite |
| Habas con huevo | Habas frescas, huevos, comino | Proteína alta, fibra |

#### Sopas
| Plato | Composición | Notas Nutricionales |
|-------|-------------|---------------------|
| Harira | Lentejas, garbanzos, tomate, fideos, carne | Proteína mixta, carbos, típica Ramadán |
| Chorba | Sopa de verduras con fideos | Baja en calorías, hidratante |
| Bessara (sopa) | Habas trituradas, aceite de oliva | Proteína vegetal |

#### Huevos — Desayunos y cenas
| Plato | Composición | Notas Nutricionales |
|-------|-------------|---------------------|
| Huevos con tomate | Huevos, tomate frito, especias | Proteína alta, grasas moderadas |
| Huevos con khlii | Huevos, carne seca conservada | Muy alto en proteína y sal |
| Tortilla marroquí | Huevos, hierbas, cebolla | Proteína, fácil de trackear |
| Baghrir con miel | Crêpes esponjosos, miel, mantequilla | Carbos altos, desayuno típico |

#### Carnes y Proteínas
| Plato | Composición | Notas Nutricionales |
|-------|-------------|---------------------|
| Kefta (albóndigas) | Carne picada, especias, perejil | Proteína alta, fácil de pesar |
| Brochetas | Carne en pincho, especias | Proteína pura, control fácil |
| Pollo asado marroquí | Pollo entero, limón, aceitunas | Proteína alta, grasa en piel |
| Pollo al horno con patatas | Pollo, patatas, cebolla | Equilibrado |

#### Acompañamientos de Verduras
| Plato | Composición | Notas Nutricionales |
|-------|-------------|---------------------|
| Ensalada marroquí | Tomate, pepino, cebolla, aceite | Bajo en calorías, vitaminas |
| Zaalouk | Berenjenas, tomate, ajo, aceite | Fibra, grasas por aceite |
| Taktouka | Pimientos, tomate, ajo | Similar a pisto, bajo en kcal |
| Ensalada de zanahorias | Zanahoria, naranja, canela | Vitamina A, carbos simples |

#### Carbohidratos
| Plato | Composición | Notas Nutricionales |
|-------|-------------|---------------------|
| Cuscús | Sémola, verduras, garbanzos, carne | Muy alto en carbos, comida de viernes |
| Espaguetis con carne | Pasta, carne picada, tomate | Fusión hispano-marroquí, alto en carbos |
| Rfissa | Msemen desmenuzado, pollo, lentejas | Carbos + proteína, muy calórico |
| Pan marroquí (khobz) | Sémola, harina | Acompaña TODO, difícil evitar |

### Características de la Cocina Familiar
```yaml
COCINA_FAMILIAR:
  grasas_predominantes: [aceite oliva, aceite girasol]
  especias_comunes: [comino, cúrcuma, jengibre, canela, ras el hanout]
  problemas_tracking:
    - Aceite abundante en tajines (difícil cuantificar)
    - Porciones familiares (no individuales)
    - Comidas compartidas del mismo plato
  ventajas:
    - Alta en verduras y legumbres
    - Proteína animal frecuente
    - Especias con propiedades antiinflamatorias
```

## 5. Ramadán

### Período
- **Duración**: ~29-30 días (calendario lunar, cada año ~11 días antes)
- **Ayuno**: Desde Fajr (amanecer) hasta Maghrib (puesta de sol)
- **Horario**: Varía según el año y estación:
  - **Si cae en verano**: ~4:30 - 21:30 (~17h de ayuno) — Más duro
  - **Si cae en invierno**: ~6:30 - 18:00 (~11.5h de ayuno) — Más llevadero
  - **Si cae en primavera/otoño**: ~5:30 - 20:00 (~14.5h de ayuno)
- **Nota**: Consultar calendario islámico del año actual para fechas exactas

### Variables del Sistema
```yaml
RAMADAN:
  activo: false  # Cambiar a true durante el mes
  año_actual: 2026
  fecha_inicio: "2026-02-XX"  # Actualizar según calendario lunar
  fecha_fin: "2026-03-XX"
```

### Estructura de Comidas en Ramadán

| Comida | Hora | Objetivo | Composición Ideal |
|--------|------|----------|-------------------|
| **Iftar** | Maghrib (~21:30 verano) | Romper ayuno, rehidratar | Dátiles + agua → Harira → Plato principal |
| **Cena** | ~23:00 | Comida principal | Proteína + carbos + verduras |
| **Suhur** | Pre-Fajr (~3:30 verano) | Energía sostenida | Carbos complejos + proteína + grasas |

### Reglas de Entrenamiento en Ramadán
```yaml
ENTRENO_RAMADAN:
  timing_optimo: "30-60 min antes de Iftar"  # Entrenas en ayunas, comes justo después
  timing_alternativo: "2-3h después de Iftar"  # Si no puedes pre-Iftar
  ajustes:
    volumen: -30%  # Reducir series totales
    intensidad: "mantener"  # No bajar pesos, solo volumen
    cardio: "mínimo o eliminar"
    hidratacion: "crítica en ventana nocturna"
  prohibido:
    - Entrenar en horas centrales del ayuno
    - Sesiones > 60 min
    - HIIT intenso
```

### Reglas de Nutrición en Ramadán
```yaml
NUTRICION_RAMADAN:
  prioridades:
    1: "Hidratación (2-3L entre Iftar y Suhur)"
    2: "Proteína distribuida en 2-3 comidas"
    3: "Carbos complejos en Suhur"
    4: "Evitar azúcares simples que dan sed"
  
  iftar:
    primero: "3 dátiles + vaso agua"
    luego: "Harira o sopa"
    principal: "Proteína + verduras"
    evitar: "Fritos excesivos, dulces"
  
  suhur:
    incluir: 
      - "Avena o pan integral"
      - "Huevos o lácteos"
      - "Frutos secos"
      - "Plátano"
    evitar:
      - "Comidas muy saladas (sed)"
      - "Cafeína excesiva"
      - "Azúcares simples"
  
  suplementacion:
    creatina: "5g con Suhur"
    proteina_polvo: "Si no llegas a mínimo con comida"
    electrolitos: "Añadir a agua nocturna si necesario"
```

## 6. Calendario Festivo y Tradiciones

### Festividades Islámicas

#### Eid al-Fitr (Fin de Ramadán)
```yaml
EID_FITR:
  duracion: "1-3 días"
  caracteristicas:
    - Desayuno festivo tras un mes de ayuno
    - Dulces abundantes (chebakia, kaab el ghazal, sellou)
    - Visitas familiares con comida en cada casa
    - Presión social para comer mucho ("has adelgazado en Ramadán")
  ajuste_sistema:
    - Marcar como "días de excepción"
    - No trackear estrictamente, disfrutar
    - Volver a rutina después
```

#### Eid al-Adha (Fiesta del Cordero)
```yaml
EID_ADHA:
  duracion: "3-4 días"
  caracteristicas:
    - Sacrificio del cordero (mucha carne disponible)
    - Comidas abundantes: hígado, brochetas, tajine de cordero
    - Alta ingesta de proteína y grasa
    - Varios días seguidos de festín
  ajuste_sistema:
    - Días de superávit calórico inevitable
    - Aprovechar proteína alta para entreno
    - No culpabilizar, es tradición
```

### Tradiciones Semanales

#### Domingo = Día de Cuscús
```yaml
DOMINGO_CUSCUS:
  frecuencia: "Semanal (tradición familiar fija)"
  composicion: "Cuscús con 7 verduras, garbanzos, carne"
  calorias_estimadas: "800-1200 kcal por ración"
  ajuste_sistema:
    - Planificar domingo como día alto en carbos
    - Reducir carbos resto del día
    - Si entrenas domingo: ideal post-entreno
    - Si descansas domingo: ajustar calorías totales
```

### Dulces Marroquíes — ⚠️ PROBLEMA FRECUENTE

> **Nota personal**: Los dulces marroquíes son un problema frecuente, NO solo en fiestas. Siempre hay en casa.

| Dulce | Ocasión | Calorías aprox. | Notas |
|-------|---------|-----------------|-------|
| Chebakia | Ramadán (pero hay todo el año) | ~150-200 kcal/ud | Miel, sésamo, frito |
| Kaab el ghazal | Fiestas (pero hay siempre) | ~100-150 kcal/ud | Almendra, azúcar glas |
| Sellou | Ramadán, postparto | ~200 kcal/porción | Almendra, sésamo, MUY calórico |
| Briwat | Fiestas | ~120 kcal/ud | Almendra o carne, frito |
| Ghriba | Cualquier día | ~80-100 kcal/ud | Galletas de almendra/coco |
| Msemen con miel | Desayuno | ~250 kcal/ud | Hojaldre, mantequilla, miel |

```yaml
DULCES_ESTRATEGIA:
  problema: "Siempre hay dulces en casa, difícil resistir"
  ajuste_sistema:
    - Trackear consumo aunque sea aproximado
    - Reservar 100-200 kcal diarias para dulces si es inevitable
    - No prohibir, pero limitar cantidad
    - Priorizar ghriba (menos calórico) sobre chebakia/sellou
```

### Té Moruno
```yaml
TE_MORUNO:
  frecuencia: "1-2 veces a la semana"
  composicion: "Té verde + menta + azúcar"
  calorias_por_vaso: "~60-80 kcal (15-20g azúcar)"
  consumo_actual: "Con azúcar (tradicional)"
  ajuste_sistema:
    - Contabilizar ~70 kcal por vaso
    - 1-2 veces/semana = ~140 kcal/semana (manejable)
    - No es prioridad cambiar este hábito
```

## 7. Situaciones Sociales

### En Contexto Español

#### Tapas y Cervezas con Amigos
```yaml
TAPAS_AMIGOS:
  afecta_socialmente: false  # No es un problema
  pedido_habitual:
    bebida: "Coca-Cola Zero"  # 0 kcal
    comida: "Patatas bravas"  # ~300-400 kcal
  ajuste_sistema:
    - Contabilizar bravas (~350 kcal) si sale con amigos
    - Sin impacto calórico por bebida
    - Situación social resuelta, no requiere estrategias especiales
```

#### Comidas de Empresa / Navidad
```yaml
NAVIDAD_SOCIAL:
  celebra_religiosamente: false
  participa_socialmente: true
  ocasiones:
    - Comida/cena de empresa (Navidad)
    - Cenas con amigos (Nochevieja, Reyes)
  estrategias:
    restaurante:
      - Revisar menú antes, buscar opciones sin cerdo
      - Pescado suele ser opción segura
      - Marisco (si no hay alergia)
      - Preguntar si la carne es Halal (raro, pero a veces sí)
    evitar:
      - Cochinillo, jamón, embutidos
      - Postres con alcohol (tarta de whisky, etc.)
    actitud: "Comer lo que puedas, no llamar la atención"
```

### En Contexto Familiar Marroquí

#### Presión para Comer
```yaml
PRESION_FAMILIAR:
  situacion: "En reuniones familiares hay presión constante para repetir"
  frases_tipicas:
    - "Come más, estás muy flaco"
    - "¿No te gusta? ¿Por qué no repites?"
    - "Tu madre se ha esforzado mucho"
  estrategias:
    - Servirse porción moderada inicial
    - Comer despacio para parecer que comes más
    - Repetir verduras/ensalada si insisten
    - "Estoy lleno, pero estaba buenísimo"
```

#### Comidas Compartidas
```yaml
COMIDA_COMPARTIDA:
  problema: "Se come del mismo plato, difícil trackear porción exacta"
  solucion:
    - Estimar tu zona del plato
    - Priorizar proteína y verdura
    - El pan (khobz) es el mayor problema (se usa como cubierto)
```

## 8. Adaptaciones para el Sistema

### Filtros de Alimentos
```yaml
FILTROS_HALAL:
  excluir_siempre:
    - cerdo
    - jamon
    - bacon
    - chorizo
    - morcilla
    - gelatina_animal
    - alcohol
  verificar_etiqueta:
    - embutidos
    - snacks
    - salsas
    - dulces
```

### Ajustes de Motor de Dieta
1. Si `RAMADAN.activo == true`:
   - Colapsar macros diarios a ventana Iftar-Suhur
   - Aumentar proteína por comida (menos comidas)
   - Priorizar hidratación en recomendaciones
   
2. Si comida familiar (tajine, cuscús):
   - Estimar aceite: +15-20g grasas sobre receta base
   - Ajustar resto del día en consecuencia

## 7. Uso en el Sistema
1. `REG-NUT-01` (motor_dieta) aplica filtros Halal
2. `REG-NUT-02` (preferencias) incluye platos familiares
3. `REG-LOG-03` (excepciones) activa modo Ramadán
4. La app detecta `RAMADAN.activo` y ajusta UI/notificaciones
