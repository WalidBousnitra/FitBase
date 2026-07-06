// ═══════════════════════════════════════════════════════════════
// FITBASE - BACKEND SIMPLIFICADO (Google Apps Script)
// ═══════════════════════════════════════════════════════════════
// Organización:
//   §1. CONFIGURACIÓN
//   §2. ENDPOINTS (doGet / doPost)
//   §3. LÓGICA PRINCIPAL (funciones que sirven datos a la app)
//   §4. MOTOR DE CARGAS (autorregulación basada en evidencia)
//   §5. AUXILIARES (helpers genéricos)
//   §6. INICIALIZAR (crear hojas + cabeceras — ejecutar 1 vez)
//   §7. RELLENAR (pre-generar plan anual, semanal, sesiones)
//   §7b. RELLENAR DATOS FICTICIOS (histórico de prueba: sueño/peso/entrenos)
//   §8. LIMPIAR (borrar logs de test, conservar estructura y planes)
//   §9. BIOMETRÍA INICIO/FIN (ajeno a la app — checkpoint manual para
//       comparar antes/después del plan anual, ver knowledge_base/usuario/biometria.md)
// ═══════════════════════════════════════════════════════════════

// ─── §1. CONFIGURACIÓN ────────────────────────────────────────

const HOJAS = {
  // metricas_zepp centraliza TODO lo que se recoge de Health Connect: sueño,
  // pasos, FC reposo, peso y % grasa (antes peso/grasa vivían en peso_log,
  // ahora fusionado aquí — una sola fila por día).
  METRICAS_ZEPP: 'metricas_zepp',
  METRICAS_SUBJETIVAS: 'metricas_subjetivas',
  PLAN_ANUAL: 'plan_anual',
  SESIONES_PLAN: 'sesiones_plan',
  EJERCICIOS_PLAN: 'ejercicios_plan',
  // Solo últimos 7 días — únicamente para el reajuste dinámico del motor de
  // cargas (ver limpiarEjerciciosLogAntiguos_). No es un historial permanente.
  EJERCICIOS_LOG: 'ejercicios_log',
  EJERCICIOS_CATALOGO: 'ejercicios_catalogo'
};

// Días que se conservan en ejercicios_log — el motor de cargas solo necesita
// el rendimiento reciente para reajustar (más peso en fase de adaptación,
// donde lo planeado y lo real difieren más; cada vez más preciso con el tiempo).
const EJERCICIOS_LOG_RETENCION_DIAS = 7;

const CACHE_TTL = 30; // segundos

// ─── Ramadán (usuario/perfil/cultura.md §5-8) ─────────────────
// Fechas confirmadas por el usuario (04/07/2026). El calendario islámico es
// lunar — estas fechas hay que actualizarlas a mano cada año, no se calculan.
// Eid al-Fitr = día siguiente al fin de Ramadán (cultura.md §6: "1-3 días" de
// excepción — se deja en 1 día por defecto; ampliar aquí si la familia lo
// celebra más días).
const RAMADAN_FECHAS = { inicio: '2027-02-08', fin: '2027-03-10' };
const EID_FITR_FECHAS = { inicio: '2027-03-11', fin: '2027-03-11' };

function esRamadan_(fecha) {
  fecha = fecha || fechaHoy_();
  return fecha >= RAMADAN_FECHAS.inicio && fecha <= RAMADAN_FECHAS.fin;
}

function esEidFitr_(fecha) {
  fecha = fecha || fechaHoy_();
  return fecha >= EID_FITR_FECHAS.inicio && fecha <= EID_FITR_FECHAS.fin;
}

// ─── Horario semanal (configurable) ───────────────────────────
// Día de la semana (0=dom..6=sáb) → tipo de sesión. A diferencia de las
// fases (evidencia fija), este split SÍ puede necesitar cambiar: natación
// depende del horario de la piscina del curso/cuatrimestre, que se publica
// cada cierto tiempo y no coincide siempre con los mismos días. Se guarda
// en PropertiesService (persiste entre despliegues, no necesita hoja nueva
// para un objeto tan pequeño) y se actualiza vía accion=actualizar_horario,
// que regenera SOLO las sesiones futuras (desde mañana) — el histórico ya
// vivido (incluidas sesiones completadas) no se toca nunca.
const TIPOS_DIA_VALIDOS = ['PUSH', 'PIERNA', 'PULL', 'HOMBR', 'NATACION', 'DESCANSO'];
const HORARIO_SEMANAL_DEFECTO = { 0: 'DESCANSO', 1: 'PUSH', 2: 'NATACION', 3: 'PIERNA', 4: 'NATACION', 5: 'PULL', 6: 'HOMBR' };

function getHorarioSemanal_() {
  var guardado = PropertiesService.getScriptProperties().getProperty('HORARIO_SEMANAL');
  if (!guardado) return HORARIO_SEMANAL_DEFECTO;
  try {
    var obj = JSON.parse(guardado);
    for (var d = 0; d <= 6; d++) {
      if (TIPOS_DIA_VALIDOS.indexOf(obj[d]) < 0) return HORARIO_SEMANAL_DEFECTO;
    }
    return obj;
  } catch (e) {
    return HORARIO_SEMANAL_DEFECTO;
  }
}

/**
 * Guarda el horario semanal nuevo y regenera las sesiones futuras (desde
 * mañana) para que reflejen el cambio de inmediato. Hoy no se toca — si ya
 * se sirvió o completó la sesión de hoy, cambiar el horario ahora no debe
 * reescribirla por debajo.
 */
function guardarHorarioSemanal_(datos) {
  var nuevo = datos.horario;
  if (!nuevo) return { error: 'Falta horario' };
  for (var d = 0; d <= 6; d++) {
    if (TIPOS_DIA_VALIDOS.indexOf(nuevo[d]) < 0) {
      return { error: 'Día ' + d + ' tiene un tipo inválido: ' + nuevo[d] };
    }
  }

  PropertiesService.getScriptProperties().setProperty('HORARIO_SEMANAL', JSON.stringify(nuevo));

  // fechaHoy_() ya está en huso Europe/Madrid — sumar el día ahí evita
  // desajustes con el huso por defecto del proyecto de Apps Script.
  var manana = parseDate_(fechaHoy_());
  manana.setDate(manana.getDate() + 1);
  var mananaStr = formatDate_(manana);
  var resultado = regenerarSesionesDesde_(mananaStr);

  return {
    ok: true,
    horario: nuevo,
    sesiones_eliminadas: resultado.eliminadas,
    sesiones_generadas: resultado.generadas,
    mensaje: 'Horario actualizado desde mañana (' + mananaStr + '). Hoy y el histórico no se han tocado.'
  };
}

// ─── §2. ENDPOINTS ────────────────────────────────────────────

function doGet(e) {
  try {
    const accion = e.parameter.accion;
    const cache = CacheService.getScriptCache();
    const cacheKey = 'GET:' + accion + ':' + (e.parameter.dias || '') + ':' + (e.parameter.semana || '');

    // Cache para GETs costosos
    const cached = cache.get(cacheKey);
    if (cached) return jsonOutput_(cached);

    let resultado;
    switch (accion) {
      case 'sesion_hoy':       resultado = getSesionHoy_(); break;
      case 'vista_manana':     resultado = getVistaMañana_(); break;
      case 'plan_anual':       resultado = getPlanAnual_(); break;
      case 'macros_hoy':       resultado = getMacrosHoy_(); break;
      case 'check_ausencia':   resultado = checkAusencia_(); break;
      case 'cambio_fase':      resultado = getCambioFase_(); break;
      case 'horario_semanal':  resultado = { horario: getHorarioSemanal_() }; break;
      case 'progresion_metricas': resultado = getProgresionMetricas_(e.parameter.dias); break;
      default: resultado = { error: 'Acción no reconocida' };
    }

    const json = JSON.stringify(resultado);
    if (['sesion_hoy','vista_manana','plan_anual','macros_hoy','progresion_metricas'].includes(accion)) {
      cache.put(cacheKey, json, CACHE_TTL);
    }
    return jsonOutput_(json);
  } catch (err) {
    return jsonOutput_(JSON.stringify({ error: err.message }));
  }
}

function doPost(e) {
  try {
    const datos = JSON.parse(e.postData.contents);
    let resultado;
    switch (datos.accion) {
      case 'guardar_log':       resultado = guardarLog_(datos); break;
      case 'guardar_metricas':  resultado = guardarMetricas_(datos); break;
      case 'guardar_metricas_subjetivas': resultado = guardarMetricasSubjetivas_(datos); break;
      case 'completar_sesion':  resultado = completarSesion_(datos); break;
      case 'registrar_ausencia': resultado = registrarAusencia_(datos); break;
      case 'actualizar_horario': resultado = guardarHorarioSemanal_(datos); break;
      default: resultado = { error: 'Acción POST no reconocida' };
    }
    // Invalidar cache tras escritura
    CacheService.getScriptCache().removeAll(['GET:sesion_hoy:', 'GET:macros_hoy:', 'GET:progresion_metricas:', 'GET:vista_manana:', 'GET:plan_anual:']);
    return jsonOutput_(JSON.stringify(resultado));
  } catch (err) {
    return jsonOutput_(JSON.stringify({ error: err.message }));
  }
}

// ─── §3. LÓGICA PRINCIPAL ─────────────────────────────────────

/**
 * Sesión de hoy con ejercicios y pesos DINÁMICOS (motor de cargas).
 * Fuente: REG-LOG-02 §5, REG-LOG-01 §6-8
 */
function getSesionHoy_() {
  const hoy = fechaHoy_();
  const hoja = getHoja_(HOJAS.SESIONES_PLAN);
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];

  // Buscar sesión
  let sesion = null;
  for (let i = 1; i < datos.length; i++) {
    const f = parseDate_(datos[i][cab.indexOf('date_fecha')]);
    if (f && formatDate_(f) === hoy) {
      sesion = rowToObj_(cab, datos[i]);
      break;
    }
  }

  // Pre-temporada (antes de plan.fecha_inicio): rellenarPlanCompleto solo
  // genera filas dentro de las fechas de FASES, así que hoy nunca tiene
  // sesión todavía. Si el horario semanal marca hoy como día de gym, se
  // genera AHORA una sesión real (mismas plantillas T[] que usará el plan
  // real, FAS_01 como fase de referencia) para poder probar el flujo
  // completo de entreno + guardado antes de que el plan arranque de verdad.
  // Se marca con sufijo _TEST en el id para poder identificarla y borrarla
  // a mano — no se limpia sola en limpiarDatosTest() (§8 conserva sesiones_plan).
  if (!sesion) {
    const plan = getPlanAnual_();
    if (hoy < plan.fecha_inicio) {
      const tipoSesionHoy = getHorarioSemanal_()[new Date().getDay()];
      if (TIPO_DISPLAY[tipoSesionHoy]) {
        sesion = generarSesionTestHoy_(hoy, tipoSesionHoy);
      }
    }
  }
  if (!sesion) return { sesion: null, ejercicios: [], mensaje: 'No hay sesión para hoy' };

  // Ramadán (cultura.md §5): -30% volumen, intensidad se mantiene. Se aplica
  // aquí (al SERVIR la sesión), no en la generación del plan — el ayuno es un
  // periodo lunar fijo por fechas de calendario, independiente de las fases.
  const ramadan = esRamadan_(hoy);

  // Ejercicios con peso calculado DINÁMICAMENTE (no almacenado en plan)
  const ejercicios = getEjerciciosSesion_(sesion.sesion_id);
  const ajuste = calcularAjusteDia_();

  // Contexto nutricional para el motor (Helms 2014: déficit limita progresión)
  const plan = getPlanAnual_();
  const objetivoNutri = (plan.fase_actual && plan.fase_actual.str_objetivo_nutri) || 'bulk';

  const ejerciciosAjustados = ejercicios.map(function(ej) {
    var seriesPlan = ramadan ? Math.max(1, Math.round(ej.num_series_plan * 0.7)) : ej.num_series_plan;
    var resultado = calcularPesoSugerido_(ej.ejercicio_id, {
      ajusteDia: ajuste.factor,
      fase: sesion.str_fase || 'VOL',
      objetivoNutri: objetivoNutri,
      repsObjetivo: ej.str_reps_plan,
      rirObjetivo: ej.num_rir_objetivo
    });
    return {
      ...ej,
      num_series_plan: seriesPlan,
      num_peso_sugerido_kg: resultado.peso,
      motor_detalle: resultado.detalle,
      motor_capas: resultado.capas,
      ajuste_aplicado: ajuste.factor
    };
  });

  return {
    sesion: sesion,
    ejercicios: ejerciciosAjustados,
    ajuste_dia: ajuste,
    ramadan_activo: ramadan,
    ramadan_nota: ramadan
      ? 'Ramadán: volumen -30% (intensidad igual). Ideal entrenar 30-60 min antes de Iftar, o 2-3h después si no puedes antes. Nada de HIIT ni sesiones >60 min.'
      : null,
    calentamiento: getCalentamiento_(sesion.str_tipo),
    estiramientos: getEstiramientos_(sesion.str_tipo)
  };
}

/**
 * Calentamiento: movilidad dinámica (común a toda sesión) + activación
 * específica según el tipo de día. Sin descanso entre items — cada uno
 * lleva su propia duración/reps (no hay "series" que descansar entre sí).
 * Fuente: reglas/entrenamiento/calentamiento.md §3 (Fase 2 y 3).
 * Rodrigues 2020: ningún protocolo mejora fuerza aguda, pero previene
 * lesiones. Page 2012: NO estático pre-entreno (reduce fuerza).
 */
function getCalentamiento_(tipoDia) {
  var items = [
    { nombre: 'Círculos de cadera', reps: '10/lado', objetivo: 'Movilidad dinámica' },
    { nombre: 'Gato-vaca', reps: '10 reps', objetivo: 'Movilidad dinámica' },
    { nombre: 'Dislocaciones con banda', reps: '10 reps', objetivo: 'Movilidad dinámica' }
  ];

  var activacion = {
    'Push': [
      { nombre: 'Face pulls ligeros', reps: '15 reps', objetivo: 'Activación Push' },
      { nombre: 'Rotación externa banda', reps: '10/lado', objetivo: 'Activación Push' }
    ],
    'Pull': [
      { nombre: 'Dead hangs', reps: '20s', objetivo: 'Activación Pull' },
      { nombre: 'Retracción escapular', reps: '15 reps', objetivo: 'Activación Pull' }
    ],
    'Pierna': [
      { nombre: 'Glute bridges', reps: '15 reps', objetivo: 'Activación Pierna' },
      { nombre: 'Sentadillas sin peso', reps: '10 reps', objetivo: 'Activación Pierna' }
    ]
  };
  items = items.concat(activacion[tipoDia] || [
    { nombre: 'Rotación externa banda', reps: '10/lado', objetivo: 'Activación Hombros+Brazos' },
    { nombre: 'Face pulls ligeros', reps: '15 reps', objetivo: 'Activación Hombros+Brazos' }
  ]);

  // Fase 4: series de aproximación (calentamiento.md §3, primer compuesto pesado)
  items.push({
    nombre: 'Series aproximación 1er compuesto',
    reps: '40% → 60% → 75% → 85%',
    objetivo: 'Preparación neuromuscular (calentamiento.md §3)'
  });

  return { duracion_min: 12, ejercicios: items };
}

/**
 * Estiramientos post-entreno: estáticos 3×30s por grupo TRABAJADO en la sesión.
 * Page 2012: estático post-entreno no reduce fuerza (al revés que pre-entreno).
 * Bandy 1997: 30s = 60s, sin beneficio adicional a estirar MÁS TIEMPO por serie.
 * Page 2012 (evidencia/flexibilidad.md §5): sí se recomiendan 2-4 repeticiones
 * por músculo — se usa 3 (punto medio) — antes solo era 1 repetición.
 */
function getEstiramientos_(tipoDia) {
  var porTipo = {
    'Push': [
      { nombre: 'Pectoral en marco de puerta', reps: '3x30s/lado', objetivo: 'Pecho' },
      { nombre: 'Estiramiento deltoides (brazo cruzado)', reps: '3x30s/lado', objetivo: 'Hombros' },
      { nombre: 'Extensión tríceps overhead', reps: '3x30s/brazo', objetivo: 'Tríceps' }
    ],
    'Pull': [
      { nombre: 'Estiramiento dorsal en barra', reps: '3x30s', objetivo: 'Espalda' },
      { nombre: 'Estiramiento bíceps en pared', reps: '3x30s/brazo', objetivo: 'Bíceps' },
      { nombre: 'Rotación torácica tumbado', reps: '3x30s/lado', objetivo: 'Espalda' }
    ],
    'Pierna': [
      { nombre: 'Cuádriceps de pie', reps: '3x30s/pierna', objetivo: 'Cuádriceps' },
      { nombre: 'Isquios de pie (pierna en banco)', reps: '3x30s/pierna', objetivo: 'Isquios' },
      { nombre: 'Estiramiento psoas/flexor cadera', reps: '3x30s/lado', objetivo: 'Cadera' },
      { nombre: 'Aductores en mariposa', reps: '3x30s', objetivo: 'Aductores' }
    ],
    'Hombros+Brazos': [
      { nombre: 'Deltoides posterior (brazo cruzado)', reps: '3x30s/lado', objetivo: 'Hombros' },
      { nombre: 'Extensión tríceps overhead', reps: '3x30s/brazo', objetivo: 'Tríceps' },
      { nombre: 'Estiramiento bíceps en pared', reps: '3x30s/brazo', objetivo: 'Bíceps' },
      { nombre: 'Rotación externa pasiva', reps: '3x30s/lado', objetivo: 'Manguito rotador' }
    ]
  };
  var items = porTipo[tipoDia] || [
    { nombre: 'Pectoral en marco de puerta', reps: '3x30s/lado', objetivo: 'Pecho' },
    { nombre: 'Estiramiento dorsal', reps: '3x30s', objetivo: 'Espalda' },
    { nombre: 'Cuádriceps de pie', reps: '3x30s/pierna', objetivo: 'Cuádriceps' }
  ];
  return { duracion_min: Math.round(items.length * 1.5), ejercicios: items };
}

/**
 * Plan anual: fases con directrices (no pesos fijos).
 * Los pesos son SIEMPRE dinámicos — el plan define tipo de trabajo, RIR y volumen.
 */
function getPlanAnual_() {
  const hoja = getHoja_(HOJAS.PLAN_ANUAL);
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];
  const fases = [];
  for (let i = 1; i < datos.length; i++) fases.push(rowToObj_(cab, datos[i]));

  // Fase actual
  const hoy = new Date();
  let faseActual = null;
  for (const fase of fases) {
    const ini = parseDate_(fase.date_inicio);
    const fin = parseDate_(fase.date_fin);
    if (ini && fin && hoy >= ini && hoy <= fin) { faseActual = fase; break; }
  }

  return {
    fases: fases,
    fase_actual: faseActual,
    total_semanas: 48,
    fecha_inicio: '2026-08-31',
    fecha_fin: '2027-08-01'
  };
}

/**
 * Macros del día — cálculo dinámico basado en fase actual.
 * Fuentes:
 *   - BMR: Mifflin-St Jeor 1990 (R²=0.71, más preciso que Harris-Benedict)
 *   - Factor actividad: 1.55 = Moderado (motor_dieta.md §3)
 *     Justificación: trabajo sedentario + 6 días ejercicio pero natación baja intensidad
 *     → conservador para evitar sobreestimar → ajustar si peso estanca 2+ semanas en bulk
 *   - Bulk: Iraki 2019 (+10-20% TDEE, 1.6-2.2 g/kg prot, 0.5-1.5 g/kg grasa)
 *   - Cut: Helms 2014 (0.80 TDEE, 2.3-3.1 g/kg LBM prot)
 *   - Agua: 35-40 ml/kg/día (evidencia/vitalidad.md, EFSA) + 500ml entreno
 */
function getMacrosHoy_() {
  const peso = getPesoActual_();
  const altura = 188, edad = 24; // biometria.md
  // Mifflin-St Jeor (1990): BMR = (10 × peso) + (6.25 × altura) - (5 × edad) + 5 [hombres]
  const bmr = (10 * peso) + (6.25 * altura) - (5 * edad) + 5;
  // Factor actividad 1.55 = Moderado (motor_dieta.md §3: conservador justificado)
  const tdee = Math.round(bmr * 1.55);

  // Fase actual → objetivo nutricional
  // Fuentes: Iraki 2019 (bulk: +10-20%, 1.6-2.2 g/kg prot)
  //          Helms 2014 (cut: TDEE×0.80, 2.3-3.1 g/kg LBM prot)
  //          motor_dieta.md §4-5
  const plan = getPlanAnual_();
  let obj = 'bulk', mult = 1.15, protRatio = 2.0;
  if (plan.fase_actual) {
    const n = plan.fase_actual.str_objetivo_nutri || 'bulk';
    // Cut: 2.4 g/kg total ≈ 2.8 g/kg LBM @ ~15%BF (Helms 2014: 2.3-3.1 rango)
    if (n === 'cut') { mult = 0.80; protRatio = 2.4; obj = 'cut'; }
    // Mantener: TDEE×1.0 (motor_dieta.md §4)
    else if (n === 'mantener') { mult = 1.0; protRatio = 2.0; obj = 'mantener'; }
    // Bulk: TDEE×1.15 = +15% (Iraki 2019: rango 1.10-1.20, elegido 1.15 = punto medio)
  }

  var calorias = Math.round(tdee * mult);

  // Ajuste por actividad diaria (motor_dieta.md §6): pasos extra por encima
  // del objetivo de NEAT queman calorías reales no capturadas por el factor
  // de actividad fijo (1.55) — se compensan con carbos extra (van al remainder).
  const pasos = getPasosHoy_();
  if (pasos > 12000) calorias += 175; // +150-200 kcal (motor_dieta.md §6), 175 = punto medio

  const protG = Math.round(peso * protRatio);
  // Grasas: Iraki 2019 (bulk: 0.5-1.5 g/kg, ~20-30% kcal). Usar 1.0 g/kg (punto medio)
  // En cut: mínimo 0.5 g/kg para función hormonal (Helms 2014), usamos ~25% kcal
  var grasaG;
  if (obj === 'cut') {
    grasaG = Math.round(calorias * 0.25 / 9); // 25% kcal de grasa (Helms 2014: 15-30%)
  } else {
    grasaG = Math.round(peso * 1.0); // 1.0 g/kg (Iraki 2019: 0.5-1.5)
  }
  const carbosG = Math.round((calorias - protG * 4 - grasaG * 9) / 4);

  const sesionHoy = getSesionHoy_();
  const esEntreno = sesionHoy.sesion !== null;
  // Agua: 35 ml/kg/día (evidencia/vitalidad.md) + 500ml extra en día entreno (compensar sudor)
  const agua = Math.round(peso * 35) + (esEntreno ? 500 : 0);

  // Pasos objetivo por fase (programacion.md §13, Wilson 2012)
  var pasosPorFase = { VOL: 8000, FZA: 8000, DEF: 10000, MNT: 9000, DELOAD: 7000 };
  var tipoFase = 'VOL';
  if (plan.fase_actual && plan.fase_actual.str_tipo) tipoFase = plan.fase_actual.str_tipo;
  var pasosObj = pasosPorFase[tipoFase] || 8000;

  return {
    fecha: fechaHoy_(), es_dia_entreno: esEntreno, fase: obj,
    calorias_objetivo: calorias, proteina_g: protG, carbos_g: carbosG, grasas_g: grasaG,
    agua_ml: agua, pasos_objetivo: pasosObj, pasos_actuales: pasos,
    calorias_consumidas: 0, proteina_consumida_g: 0,
    carbos_consumidos_g: 0, grasas_consumidas_g: 0,
    agua_consumida_ml: 0, bmr: Math.round(bmr), tdee: tdee,
    origen_datos: 'backend', es_fallback: false
  };
}

/**
 * Progresión de métricas: peso corporal, sueño, volumen de entrenamiento.
 */
function getProgresionMetricas_(dias) {
  dias = parseInt(dias) || 30;
  const desde = new Date(Date.now() - dias * 86400000);

  // Zepp — centraliza sueño, pasos, FC reposo, peso y % grasa (una fila/día;
  // peso_kg/grasa_pct pueden venir vacíos los días sin pesada).
  const zeppData = leerDatosDesdeFecha_(HOJAS.METRICAS_ZEPP, 'date_fecha', desde, function(row) {
    return {
      fecha: row.date_fecha, sleep_score: row.num_sleep_score || 0,
      hr_reposo: row.num_hr_reposo || 0, pasos: row.num_pasos || 0,
      peso_kg: row.num_peso_kg || null, grasa_pct: row.num_grasa_pct || null
    };
  });
  const conPeso = zeppData.filter(function(d) { return d.peso_kg; });

  // Energía / estrés subjetivos (escala 1-5, entrada manual tras las 22:00)
  const subjetivaData = leerDatosDesdeFecha_(HOJAS.METRICAS_SUBJETIVAS, 'date_fecha', desde, function(row) {
    return {
      fecha: row.date_fecha,
      energia: row.num_energia || null,
      estres: row.num_estres || null,
      notas: row.str_notas || null
    };
  });

  // Volumen entrenamiento
  const logData = leerDatosDesdeFecha_(HOJAS.EJERCICIOS_LOG, 'date_timestamp', desde, function(row) { return row; });
  const volPorDia = {};
  logData.forEach(function(r) {
    const d = r.date_timestamp ? formatDate_(parseDate_(r.date_timestamp)) : null;
    if (!d) return;
    volPorDia[d] = (volPorDia[d] || 0) + ((r.num_peso_usado_kg || 0) * (r.num_reps_completadas || 0));
  });
  const volumenData = Object.keys(volPorDia).map(function(f) { return { fecha: f, volumen_kg: Math.round(volPorDia[f]) }; });
  volumenData.sort(function(a, b) { return a.fecha.localeCompare(b.fecha); });

  // Resumen
  const resumen = {
    peso_actual: conPeso.length > 0 ? conPeso[conPeso.length - 1].peso_kg : null,
    peso_inicio: conPeso.length > 0 ? conPeso[0].peso_kg : null,
    grasa_actual: conPeso.length > 0 ? conPeso[conPeso.length - 1].grasa_pct : null,
    grasa_inicio: conPeso.length > 0 ? conPeso[0].grasa_pct : null,
    sleep_media: zeppData.length > 0 ? Math.round(zeppData.reduce(function(s, d) { return s + d.sleep_score; }, 0) / zeppData.length) : null,
    pasos_media: zeppData.length > 0 ? Math.round(zeppData.reduce(function(s, d) { return s + d.pasos; }, 0) / zeppData.length) : null
  };

  return {
    dias_solicitados: dias, zepp: zeppData,
    subjetiva: subjetivaData, volumen_entreno: volumenData, resumen: resumen
  };
}

function checkAusencia_() {
  const hoja = getHoja_(HOJAS.SESIONES_PLAN);
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];
  const hoy = new Date();
  const hace7 = new Date(hoy.getTime() - 7 * 86400000);
  const diasPerdidos = [];

  for (let i = 1; i < datos.length; i++) {
    const f = parseDate_(datos[i][cab.indexOf('date_fecha')]);
    if (f && f >= hace7 && f < hoy && !datos[i][cab.indexOf('bool_completada')]) {
      diasPerdidos.push({
        fecha: formatDate_(f),
        tipo: datos[i][cab.indexOf('str_tipo')],
        sesion_id: datos[i][cab.indexOf('sesion_id')]
      });
    }
  }

  var redistribucion = null;
  // excepciones.md §2.1: "NO modificar el plan anual (un día suelto no afecta)"
  // El motor de pesos (Capa 5) ya contempla gaps automáticamente:
  //   >7d → ×0.95, >14d → ×0.90
  // NO se redistribuye volumen — la evidencia no soporta "series extra compensatorias"

  return {
    dias_perdidos: diasPerdidos,
    total_perdidos: diasPerdidos.length,
    redistribucion: redistribucion,
    mensaje: diasPerdidos.length > 0
      ? 'No entrenaste ' + diasPerdidos.length + ' día(s). Se redistribuye volumen.'
      : 'Todo al día'
  };
}

// ─── POST handlers ───

function guardarLog_(datos) {
  const hoja = getHoja_(HOJAS.EJERCICIOS_LOG);
  const logId = genId_('LOG');
  hoja.appendRow([
    logId, datos.plan_id || '', datos.sesion_id, datos.ejercicio_id,
    datos.num_serie, datos.num_peso_usado_kg, datos.num_reps_completadas,
    datos.num_rir_percibido, datos.str_sensacion || 'bien', new Date().toISOString()
  ]);

  // NO se toca ejercicios_plan. El peso se calcula SIEMPRE dinámicamente
  // desde el último log al servir getSesionHoy_() → calcularPesoSugerido_().
  // Esto elimina O(n) escrituras y hace el POST instantáneo.
  limpiarLogsAntiguos_();
  return { ok: true, log_id: logId };
}

/**
 * Inserta una fila nueva o ACTUALIZA la existente si ya hay una fila con esa
 * fecha en la hoja (mismo día = mismo registro). Evita duplicados cuando el
 * sync diario de la app (Health Connect → BBDD) se ejecuta más de una vez el
 * mismo día (reinstalación, abrir la app varias veces, etc). Si actualiza,
 * conserva el ID original de la fila (columna 1).
 */
function upsertPorFecha_(hoja, colFecha, fecha, filaCompleta) {
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];
  const idx = cab.indexOf(colFecha);
  if (idx >= 0) {
    for (let i = datos.length - 1; i >= 1; i--) {
      const f = parseDate_(datos[i][idx]);
      if (f && formatDate_(f) === fecha) {
        const filaActualizada = [datos[i][0]].concat(filaCompleta.slice(1));
        hoja.getRange(i + 1, 1, 1, filaActualizada.length).setValues([filaActualizada]);
        return true; // actualizado
      }
    }
  }
  hoja.appendRow(filaCompleta);
  return false; // insertado
}

/**
 * Guarda TODO lo que se recoge de Health Connect en una sola fila por día:
 * sueño, pasos, FC reposo, peso y % grasa (antes peso/grasa vivían en
 * peso_log, separado — ahora centralizado aquí). Los campos que no vengan
 * en la llamada mantienen el valor ya guardado ese día (no se pisan a '0'
 * si, por ejemplo, solo se sincroniza el peso más tarde).
 *
 * La app manda pasos (cada ~15s mientras está abierta) y sueño/FC/peso
 * (1 vez al día) como llamadas HTTP INDEPENDIENTES y casi simultáneas —
 * sin lock, dos ejecuciones de Apps Script podían solaparse: ambas leen
 * "existente" ANTES de que la otra escriba, y la que termina de escribir
 * segunda pisa a la primera con sus valores por defecto (0 / '' para los
 * campos que ella no traía) — o, peor, cada una hace appendRow_ por
 * separado y quedan DOS filas para el mismo día. LockService serializa el
 * read-modify-write para que la segunda llamada siempre vea ya escrita la
 * primera.
 */
function guardarMetricas_(datos) {
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const hoja = getHoja_(HOJAS.METRICAS_ZEPP);
    const fecha = datos.fecha || fechaHoy_();
    const existente = getUltimaFila_(HOJAS.METRICAS_ZEPP, 'date_fecha', fecha);
    const id = existente ? existente.metrica_id : genId_('ZEP');

    const sleepScore = datos.sleep_score != null ? datos.sleep_score : (existente ? existente.num_sleep_score : 0);
    const pasos = datos.pasos != null ? datos.pasos : (existente ? existente.num_pasos : 0);
    const hrReposo = datos.hr_reposo != null ? datos.hr_reposo : (existente ? existente.num_hr_reposo : 0);
    const pesoKg = datos.peso_kg != null ? datos.peso_kg : (existente ? existente.num_peso_kg : '');
    const grasaPct = datos.grasa_pct != null ? datos.grasa_pct : (existente ? existente.num_grasa_pct : '');

    const actualizado = upsertPorFecha_(hoja, 'date_fecha', fecha, [
      id, fecha, sleepScore, pasos, hrReposo, pesoKg, grasaPct, new Date().toISOString()
    ]);
    return { ok: true, metrica_id: id, actualizado: actualizado };
  } finally {
    lock.releaseLock();
  }
}

/**
 * Energía, estrés y notas subjetivas — entrada manual (escala 1-5, selector
 * de 5 niveles en la app). Se pregunta una vez al día, después de las 22:00
 * (ver HomeActivity), para captar el desgaste físico/mental del día completo.
 */
function guardarMetricasSubjetivas_(datos) {
  const hoja = getHoja_(HOJAS.METRICAS_SUBJETIVAS);
  const fecha = datos.fecha || fechaHoy_();
  const id = genId_('SUB');
  const actualizado = upsertPorFecha_(hoja, 'date_fecha', fecha, [
    id, fecha, datos.energia || '', datos.estres || '', datos.notas || ''
  ]);
  return { ok: true, subjetiva_id: id, actualizado: actualizado };
}

function completarSesion_(datos) {
  const hoja = getHoja_(HOJAS.SESIONES_PLAN);
  const all = hoja.getDataRange().getValues();
  const cab = all[0];
  const col = cab.indexOf('sesion_id');
  const colComp = cab.indexOf('bool_completada');
  const colFin = cab.indexOf('date_fin');
  for (let i = 1; i < all.length; i++) {
    if (all[i][col] === datos.sesion_id) {
      hoja.getRange(i + 1, colComp + 1).setValue(true);
      hoja.getRange(i + 1, colFin + 1).setValue(new Date().toISOString());
      break;
    }
  }

  // Generar resumen de sesión para mostrar al usuario
  var resumen = getResumenSesion_(datos.sesion_id);
  return { ok: true, sesion_id: datos.sesion_id, resumen: resumen };
}

/**
 * Registrar ausencia extendida (vacaciones).
 * El motor ya contempla gaps >7d/14d (Capa 5). Aquí se suspenden sesiones.
 * Al volver: primera semana con RIR+1 (readaptación).
 */
function registrarAusencia_(datos) {
  if (!datos.fecha_inicio || !datos.fecha_fin) {
    return { error: 'Se requiere fecha_inicio y fecha_fin' };
  }
  var inicio = parseDate_(datos.fecha_inicio);
  var fin = parseDate_(datos.fecha_fin);
  if (!inicio || !fin || fin <= inicio) {
    return { error: 'Fechas inválidas' };
  }

  var diasAusencia = Math.ceil((fin - inicio) / 86400000);

  // Marcar sesiones en rango como no-entrenables (no se borran — motor las ignora)
  var hoja = getHoja_(HOJAS.SESIONES_PLAN);
  var all = hoja.getDataRange().getValues();
  var cab = all[0];
  var colFecha = cab.indexOf('date_fecha');
  var colNotas = cab.indexOf('str_fase');
  var sesionesAfectadas = 0;

  for (var i = 1; i < all.length; i++) {
    var f = parseDate_(all[i][colFecha]);
    if (f && f >= inicio && f <= fin) {
      // Marcar como completada=true para que no aparezca como "perdida"
      hoja.getRange(i + 1, cab.indexOf('bool_completada') + 1).setValue(true);
      hoja.getRange(i + 1, cab.indexOf('date_fin') + 1).setValue('AUSENCIA');
      sesionesAfectadas++;
    }
  }

  // Determinar impacto según excepciones.md §2.2
  var impacto;
  if (diasAusencia <= 7) {
    impacto = 'Absorción natural (como deload). Motor reducirá peso al volver.';
  } else if (diasAusencia <= 21) {
    impacto = 'Readaptación: primera semana con RIR+1. Motor Capa 5: ×0.90.';
  } else {
    impacto = 'Ausencia larga: reiniciar mesociclo actual. Motor Capa 5: ×0.90.';
  }

  return {
    ok: true,
    dias_ausencia: diasAusencia,
    sesiones_suspendidas: sesionesAfectadas,
    impacto: impacto,
    nota: 'Al volver, el motor ajustará automáticamente los pesos a la baja (Capa 5: gap >14d → ×0.90)'
  };
}

/**
 * Vista matutina — todo lo que el usuario necesita al despertar.
 * Fuente: programacion.md §12 (Flujo Diario)
 */
function getVistaMañana_() {
  var hoy = fechaHoy_();
  var diaSemana = new Date().getDay(); // 0=dom, 1=lun...6=sab

  // 1. Sueño (de Health Connect vía metricas_zepp)
  var metrica = getUltimaFila_(HOJAS.METRICAS_ZEPP, 'date_fecha', hoy);
  var sueno = {
    sleep_score: metrica ? metrica.num_sleep_score : null,
    hr_reposo: metrica ? metrica.num_hr_reposo : null,
    pasos_ayer: metrica ? metrica.num_pasos : null
  };

  // 2. Macros del día (cambian por fase — ver motor_dieta.md)
  var macros = getMacrosHoy_();

  // 3. Fase actual y tipo de día
  var plan = getPlanAnual_();
  var faseActual = plan.fase_actual;
  var tipoFase = faseActual ? faseActual.str_tipo : 'VOL';

  // 4. Tipo de día: gym/natación/descanso
  // Antes de plan.fecha_inicio no hay sesiones reales pre-generadas en
  // sesiones_plan (rellenarPlanCompleto solo genera filas dentro de las
  // fechas de FASES) — pero el horario semanal SÍ es real (es el mismo que
  // regirá cuando el plan arranque), así que se usa igual para decidir el
  // tipo de día. getSesionHoy_() genera la sesión real de hoy sobre la
  // marcha en pre-temporada (plantillas reales, ver generarSesionTestHoy_)
  // para poder probar el flujo completo de entreno antes del 31 de agosto.
  // La app sigue mostrando el aviso de pretemporada vía pre_temporada +
  // fecha_inicio_plan (más abajo), aunque tipo_dia ya sea 'gym'/'natacion'.
  var preTemporada = hoy < plan.fecha_inicio;
  var tipoSesionHoy = getHorarioSemanal_()[diaSemana];
  var tipoDia;
  if (tipoSesionHoy === 'NATACION') tipoDia = 'natacion';
  else if (tipoSesionHoy === 'DESCANSO') tipoDia = 'descanso';
  else tipoDia = 'gym'; // PUSH/PIERNA/PULL/HOMBR

  // 5. Cardio objetivo del día (programacion.md §13, Wilson 2012)
  var cardio = getCardioObjetivo_(tipoFase, tipoDia);

  // 6. Movilidad matutina (programacion.md §14, Ruivo 2017, Hansraj 2014)
  var movilidad = getMovilidadMatutina_(plan.fecha_inicio);

  // 7. Aviso de día perdido (excepciones.md §2.1)
  var ausencia = checkAusenciaAyer_();

  // 8. ¿Ya se completó la sesión de gym de hoy? Si sí, no se puede empezar
  // otra — solo revisar el resumen (evita duplicar series en ejercicios_log).
  var sesionHoyEstado = getSesionCompletadaHoy_(hoy);

  // 9. Ramadán / Eid (cultura.md §5-6)
  var ramadan = getRamadanInfo_(hoy);

  return {
    fecha: hoy,
    tipo_dia: tipoDia,
    pre_temporada: preTemporada,
    fecha_inicio_plan: plan.fecha_inicio,
    fase: faseActual ? { fase_id: faseActual.fase_id, nombre: faseActual.str_nombre_fase, tipo: tipoFase, nutri: faseActual.str_objetivo_nutri } : null,
    sueno: sueno,
    macros: {
      calorias: macros.calorias_objetivo,
      proteina_g: macros.proteina_g,
      carbos_g: macros.carbos_g,
      grasas_g: macros.grasas_g,
      agua_ml: macros.agua_ml
    },
    cardio: cardio,
    movilidad_matutina: movilidad,
    aviso_ausencia: ausencia,
    sesion_completada: sesionHoyEstado.completada,
    resumen_hoy: sesionHoyEstado.resumen,
    ramadan: ramadan
  };
}

/**
 * Guía de Ramadán/Eid para la vista matutina (cultura.md §5-6).
 * Los OBJETIVOS de calorías/macros NO cambian (el ayuno no cambia tus
 * necesidades calóricas) — lo que cambia es CUÁNDO se reparten: colapsadas
 * en Iftar + Cena + Suhur en vez de 3 comidas + snacks repartidas por el día.
 * Horario de ayuno aproximado (no se calcula por astronomía/ubicación,
 * cultura.md §5 solo da rangos estacionales) — el usuario debe verificar el
 * horario exacto local (mezquita/app islámica) día a día.
 */
function getRamadanInfo_(hoy) {
  if (esEidFitr_(hoy)) {
    return {
      activo: false,
      es_eid: true,
      nota: 'Eid al-Fitr — día de excepción (cultura.md §6). No trackear estrictamente, disfruta con la familia. Mañana vuelves a la rutina normal.'
    };
  }
  if (!esRamadan_(hoy)) return { activo: false, es_eid: false };

  var diaAyuno = Math.round((parseDate_(hoy) - parseDate_(RAMADAN_FECHAS.inicio)) / 86400000) + 1;
  return {
    activo: true,
    es_eid: false,
    dia_ayuno: diaAyuno,
    horario_aproximado: 'Ayuno Fajr→Maghrib, aprox. 7:00-19:00 (varía cada día — verifica horario exacto local)',
    timing_entreno: '30-60 min antes de Iftar (entrenas en ayunas, comes justo después) — alternativa: 2-3h después de Iftar',
    hidratacion: 'Toda el agua (2-3L) entre Iftar y Suhur — nada durante el ayuno',
    nutricion: 'Mismas calorías/macros de hoy, pero repartidas en Iftar + Cena + Suhur (no 3 comidas + snacks)',
    iftar_orden: 'Dátiles + agua → Harira/sopa → proteína + verduras (evita fritos y dulces en exceso)',
    suhur_incluir: 'Carbos complejos (avena/integral) + proteína + frutos secos — evita muy salado y cafeína',
    entreno_prohibido: 'Nada de HIIT, sesiones >60 min, ni entrenar en horas centrales del ayuno'
  };
}

/**
 * Resumen de cambio de fase — se llama SOLO cuando la app detecta que la
 * fase actual es distinta a la última vista (comparando fase_id en el
 * cliente). Da un cierre a la fase que acaba de terminar y presenta la que
 * empieza.
 *
 * NO depende de ejercicios_log (solo guarda 7 días) — usa sesiones_plan
 * (permanente, para adherencia: completadas/totales) y metricas_zepp
 * (permanente, para peso/sueño) dentro del rango de fechas de la fase.
 */
function getCambioFase_() {
  const plan = getPlanAnual_();
  const faseActual = plan.fase_actual;
  if (!faseActual) return { hay_cambio: false };

  const fases = plan.fases.slice().sort(function(a, b) { return a.num_orden - b.num_orden; });
  const idxActual = fases.findIndex(function(f) { return f.fase_id === faseActual.fase_id; });

  const faseActualInfo = {
    fase_id: faseActual.fase_id, nombre: faseActual.str_nombre_fase, tipo: faseActual.str_tipo,
    foco: faseActual.str_foco_muscular, nutri: faseActual.str_objetivo_nutri,
    rir_rango: faseActual.str_rir_rango, semanas: faseActual.num_semanas
  };

  if (idxActual <= 0) {
    // Primera fase del plan — no hay fase anterior que resumir.
    return { hay_cambio: true, fase_anterior: null, resumen_fase_anterior: null, fase_actual: faseActualInfo };
  }

  const faseAnterior = fases[idxActual - 1];

  // Adherencia: sesiones completadas vs totales dentro del rango de fechas
  var hSes = getHoja_(HOJAS.SESIONES_PLAN);
  var datosSes = hSes.getDataRange().getValues();
  var cabSes = datosSes[0];
  var colFecha = cabSes.indexOf('date_fecha');
  var colComp = cabSes.indexOf('bool_completada');
  var totalSesiones = 0, completadas = 0;
  for (var i = 1; i < datosSes.length; i++) {
    var f = parseDate_(datosSes[i][colFecha]);
    var fStr = f ? formatDate_(f) : null;
    if (fStr && fStr >= faseAnterior.date_inicio && fStr <= faseAnterior.date_fin) {
      totalSesiones++;
      if (datosSes[i][colComp]) completadas++;
    }
  }

  // Peso/sueño dentro del rango de la fase (metricas_zepp, permanente)
  var zeppFase = leerDatosDesdeFecha_(HOJAS.METRICAS_ZEPP, 'date_fecha', parseDate_(faseAnterior.date_inicio), function(row) {
    return { fecha: row.date_fecha, peso_kg: row.num_peso_kg || null, sleep_score: row.num_sleep_score || 0 };
  }).filter(function(r) { return r.fecha <= faseAnterior.date_fin; });

  var conPeso = zeppFase.filter(function(z) { return z.peso_kg; });
  var pesoInicio = conPeso.length ? conPeso[0].peso_kg : null;
  var pesoFin = conPeso.length ? conPeso[conPeso.length - 1].peso_kg : null;
  var sleepMedia = zeppFase.length
    ? Math.round(zeppFase.reduce(function(s, z) { return s + z.sleep_score; }, 0) / zeppFase.length)
    : null;

  return {
    hay_cambio: true,
    fase_anterior: {
      fase_id: faseAnterior.fase_id, nombre: faseAnterior.str_nombre_fase, tipo: faseAnterior.str_tipo,
      foco: faseAnterior.str_foco_muscular
    },
    resumen_fase_anterior: {
      sesiones_completadas: completadas, sesiones_totales: totalSesiones,
      peso_inicio: pesoInicio, peso_fin: pesoFin, sleep_media: sleepMedia
    },
    fase_actual: faseActualInfo
  };
}

/**
 * Busca la sesión de hoy en sesiones_plan y, si ya está marcada como
 * completada, adjunta su resumen (mismo cálculo que completar_sesion).
 */
function getSesionCompletadaHoy_(hoy) {
  var hoja = getHoja_(HOJAS.SESIONES_PLAN);
  if (!hoja) return { completada: false, resumen: null };
  var datos = hoja.getDataRange().getValues();
  var cab = datos[0];
  var colFecha = cab.indexOf('date_fecha');
  var colComp = cab.indexOf('bool_completada');
  var colId = cab.indexOf('sesion_id');

  for (var i = 1; i < datos.length; i++) {
    var f = parseDate_(datos[i][colFecha]);
    if (f && formatDate_(f) === hoy) {
      var completada = !!datos[i][colComp];
      return {
        completada: completada,
        resumen: completada ? getResumenSesion_(datos[i][colId]) : null
      };
    }
  }
  return { completada: false, resumen: null };
}

/**
 * Objetivo de cardio/pasos según fase (programacion.md §13 + §12 FLUJO_DESCANSO).
 * DECISIÓN BASADA EN EVIDENCIA:
 *   - Wilson 2012: bici/elíptica NO interfiere con hipertrofia (correr SÍ: -31%)
 *   - Viana 2019: LISS (60-70% FC) = HIIT para pérdida de grasa, menor fatiga
 *   - VOL/FZA: 0 min extra → minimizar interferencia (Wilson 2012)
 *   - DEF: 15-20 min bici → aumentar NEAT + déficit (Viana 2019)
 *   - MNT: 10 min opcional → balance sin interferencia
 *   - DELOAD: 0 min → recuperación total
 * En días de NATACIÓN el cardio extra se anula: la propia clase (1h, bajo
 * impacto) ya cuenta como cardio (programacion.md §11 "natacion_cuenta: true").
 * En días de DESCANSO SÍ se prescribe (programacion.md §12 FLUJO_DESCANSO:
 * "cardio_suave: SOLO si fase = DEF o MNT") — antes se anulaba en cualquier
 * día que no fuera gym, lo que dejaba el domingo siempre en 0 incluso en
 * fases de déficit/mantenimiento, contradiciendo el flujo documentado.
 * En Ramadán se anula SIEMPRE (cultura.md §5: "cardio: mínimo o eliminar" —
 * sin agua/comida durante el ayuno, cardio extra no tiene sentido).
 * La app decide automáticamente — el usuario NO elige si hacer cardio o no.
 */
function getCardioObjetivo_(tipoFase, tipoDia) {
  var pasosPorFase = { VOL: 8000, FZA: 8000, DEF: 10000, MNT: 9000, DELOAD: 7000 };
  var cardioPorFase = { VOL: 0, FZA: 0, DEF: 20, MNT: 10, DELOAD: 0 }; // minutos
  var justificaciones = {
    VOL: 'Wilson 2012: minimizar interferencia durante volumen',
    FZA: 'Wilson 2012: priorizar recuperación neural en fuerza',
    DEF: 'Viana 2019: LISS aumenta déficit sin interferir',
    MNT: 'Balance: mantener capacidad aeróbica sin exceso',
    DELOAD: 'Recuperación total — sin carga adicional'
  };

  var pasos = pasosPorFase[tipoFase] || 8000;
  var cardioMin = cardioPorFase[tipoFase] || 0;

  // Natación ya cubre el cardio de bajo impacto ese día (§11) — no se suma más.
  if (tipoDia === 'natacion') cardioMin = 0;
  // Ramadán: eliminar cardio extra siempre, sea cual sea la fase.
  if (esRamadan_()) cardioMin = 0;

  return {
    pasos_objetivo: pasos,
    cardio_post_gym_min: cardioMin,
    contexto: tipoDia === 'gym' ? 'post-gym' : (tipoDia === 'descanso' ? 'dia_descanso' : null),
    modalidad: cardioMin > 0 ? 'bici estática o elíptica (Wilson 2012: no interfiere)' : null,
    intensidad: cardioMin > 0 ? '60-70% FC máx (LISS — Viana 2019)' : null,
    justificacion: justificaciones[tipoFase] || null
  };
}

/**
 * Rutina de movilidad matutina (programacion.md §14).
 * Fuente: Ruivo 2017 (16 sem correctivo), Hansraj 2014 (estrés cervical), Afonso 2020.
 * DECISIÓN BASADA EN EVIDENCIA: se muestra SIEMPRE (todos los días).
 * Razón: Ruivo 2017 demuestra que sin frecuencia diaria no hay corrección postural.
 * ÚNICO caso de NO mostrar: lesión activa o enfermedad aguda.
 */
/**
 * Rutina diaria postural (P2) con progresión gradual (programacion.md §14:
 * "Cada 4 semanas añadir 1 ejercicio o reps"). Se sube reps/duración +1 tramo
 * cada 4 semanas, tope en el tramo 4 (semana 16) — Ruivo 2017 solo validó su
 * protocolo correctivo durante 16 semanas, así que no hay evidencia para
 * seguir progresando pasado ese punto; se mantiene en el nivel máximo.
 */
function getMovilidadMatutina_(fechaInicioPlan) {
  var inicio = parseDate_(fechaInicioPlan);
  var semanas = inicio ? Math.floor((new Date() - inicio) / (7 * 86400000)) : 0;
  var tramo = Math.min(3, Math.max(0, Math.floor(semanas / 4)));
  var reps = 10 + tramo * 2;   // +2 reps por tramo de 4 semanas
  var seg = 30 + tramo * 5;    // +5s por tramo de 4 semanas

  return {
    duracion_min: 6 + tramo,
    frecuencia: 'DIARIA',
    nivel_progresion: tramo + 1,
    justificacion: 'Ruivo 2017: protocolo correctivo requiere frecuencia diaria y progresión gradual cada 4 semanas. Hansraj 2014: estrés cervical constante requiere corrección constante.',
    ejercicios: [
      { nombre: 'Retracción cervical (chin tucks)', reps: reps + ' reps', objetivo: 'Forward head (Hansraj 2014)' },
      { nombre: 'Extensión torácica foam roller', reps: seg + ' segundos', objetivo: 'Hipercifosis (Ruivo 2017)' },
      { nombre: 'Cat-cow (gato-vaca)', reps: reps + ' reps', objetivo: 'Movilidad columna' },
      { nombre: 'Rotación externa con banda', reps: reps + '/lado', objetivo: 'Hombros internos (Ruivo 2017)' },
      { nombre: 'Dead bugs', reps: reps + '/lado', objetivo: 'Hiperlordosis/APT' }
    ],
    nota: 'Rutina diaria postural — la ciencia exige frecuencia diaria para corrección (P2)'
  };
}

/**
 * Comprueba si ayer hubo sesión perdida (no abrió la app).
 * Detecta automáticamente según excepciones.md §2.1.
 */
function checkAusenciaAyer_() {
  var ayer = new Date();
  ayer.setDate(ayer.getDate() - 1);
  var ayerStr = formatDate_(ayer);
  var diaSemana = ayer.getDay();

  // Solo comprobar en días de gym según el horario semanal vigente
  var tipoAyer = getHorarioSemanal_()[diaSemana];
  if (['PUSH', 'PIERNA', 'PULL', 'HOMBR'].indexOf(tipoAyer) < 0) return null;

  var hoja = getHoja_(HOJAS.SESIONES_PLAN);
  var datos = hoja.getDataRange().getValues();
  var cab = datos[0];

  for (var i = 1; i < datos.length; i++) {
    var f = parseDate_(datos[i][cab.indexOf('date_fecha')]);
    if (f && formatDate_(f) === ayerStr && !datos[i][cab.indexOf('bool_completada')]) {
      return {
        fecha: ayerStr,
        tipo: datos[i][cab.indexOf('str_tipo')],
        mensaje: 'Ayer no entrenaste — hoy retomas normal. El motor ajustará pesos automáticamente.'
      };
    }
  }
  return null;
}

/**
 * Resumen de sesión completada — impacto en plan.
 * Se muestra al usuario al finalizar el entreno.
 */
function getResumenSesion_(sesionId) {
  var hoja = getHoja_(HOJAS.EJERCICIOS_LOG);
  if (!hoja) return { mensaje: 'Sin datos de log' };
  var datos = hoja.getDataRange().getValues();
  var cab = datos[0];
  var colSes = cab.indexOf('sesion_id');

  var seriesTotal = 0, volumenTotal = 0, rirSum = 0;
  for (var i = 1; i < datos.length; i++) {
    if (datos[i][colSes] === sesionId) {
      seriesTotal++;
      var peso = Number(datos[i][cab.indexOf('num_peso_usado_kg')]) || 0;
      var reps = Number(datos[i][cab.indexOf('num_reps_completadas')]) || 0;
      volumenTotal += peso * reps;
      rirSum += Number(datos[i][cab.indexOf('num_rir_percibido')]) || 0;
    }
  }

  if (seriesTotal === 0) return { mensaje: 'Sesión sin series registradas' };

  var rirMedio = Math.round(rirSum / seriesTotal * 10) / 10;
  // Helms 2016: RIR-RPE scale classification
  // RIR 0-1 = RPE 9-10 (cerca fallo), RIR 2-3 = RPE 7-8 (hipertrofia óptima)
  var intensidad;
  if (rirMedio <= 1) intensidad = 'Muy alta — cerca del fallo (RPE 9-10, Helms 2016)';
  else if (rirMedio <= 2.5) intensidad = 'Alta — zona óptima hipertrofia (RPE 7-8, Helms 2016)';
  else if (rirMedio <= 3.5) intensidad = 'Moderada — margen de progresión (RPE 6-7)';
  else intensidad = 'Conservadora — podrías aumentar carga (RPE <6)';

  return {
    series_totales: seriesTotal,
    volumen_total_kg: Math.round(volumenTotal),
    rir_medio: rirMedio,
    intensidad_percibida: intensidad,
    impacto: 'El motor usará estos datos para ajustar tu próxima sesión de este tipo.'
  };
}

// ─── §4. MOTOR DE CARGAS ──────────────────────────────────────
// Fuentes: ACSM 2009, Mann 2010 (APRE), Kiviniemi 2007 (FC), Helms 2016 (RIR),
//          Bompa 2019 (periodización), Schoenfeld 2017 (volumen)
//
// FILOSOFÍA:
// 1. El PLAN ANUAL define DIRECTRICES inmutables: fase, RIR objetivo, volumen, foco.
// 2. Los PESOS son dinámicos: se calculan al servir la sesión, no almacenados.
// 3. La FASE ACTUAL modula la progresión (Bompa: AA=conservador, FZA=agresivo).
// 4. El AJUSTE DIARIO modula por fatiga (Kiviniemi: FC, sueño, estrés).
// 5. El APRE (Mann 2010) define cuánto subir/bajar basado en rendimiento real.

/**
 * Ajuste global del día basado en fatiga/sueño/estrés.
 *
 * EVIDENCIA:
 *   - Kiviniemi 2007: FC reposo como proxy de HRV para autorregulación
 *   - Fullagar 2015: sueño afecta rendimiento cognitivo y físico
 *
 * HEURÍSTICAS (marcadas):
 *   - Sleep score < 60 → ×0.90 (no hay paper que defina umbral exacto)
 *   - Estrés subjetivo > 7 → ×0.85 (no hay paper que defina umbral exacto)
 */
function calcularAjusteDia_() {
  const hoy = fechaHoy_();
  const metrica = getUltimaFila_(HOJAS.METRICAS_ZEPP, 'date_fecha', hoy);
  var factor = 1.0;
  var razones = [];

  if (metrica) {
    const fcMedia = calcularMediaFC_(10);

    // ✅ Kiviniemi 2007: tendencia ascendente 2+ días → RECUPERACIÓN ACTIVA
    // Regla: FC trending es un early-return, no se acumula con otros factores.
    if (esTendenciaFCAscendente_(2)) {
      return {
        factor: 0.70,
        razones: ['FC ascendente 2+ días → RECUPERACIÓN ACTIVA (Kiviniemi 2007)'],
        tipo: 'recuperacion'
      };
    }
    // ✅ Kiviniemi 2007 adaptado: FC reposo elevada +10 vs media 10d
    if (metrica.num_hr_reposo > fcMedia + 10) {
      factor *= 0.80;
      razones.push('FC reposo elevada +10 vs media (Kiviniemi 2007 adaptado)');
    }

    // ⚠️ HEURÍSTICO: sueño pobre (basado en Fullagar 2015 sin umbral específico)
    if (metrica.num_sleep_score && metrica.num_sleep_score < 60) {
      factor *= 0.90;
      razones.push('Sleep score bajo <60 (heurístico, Fullagar 2015)');
    }
  }

  // ⚠️ HEURÍSTICO: estrés subjetivo (escala 1-5). Se pregunta tras las 22:00,
  // así que lo que hay guardado con fecha de HOY se refiere al desgaste de
  // AYER-NOCHE→esta mañana — es el dato relevante para ajustar la sesión de
  // HOY (se pidió anoche, sobre el día que terminaba). Si por lo que sea no
  // hay dato de ayer, se prueba con el de hoy por si se guardó ya avanzado el día.
  var ayer = formatDate_(new Date(new Date(hoy).getTime() - 86400000));
  var subjetiva = getUltimaFila_(HOJAS.METRICAS_SUBJETIVAS, 'date_fecha', ayer)
      || getUltimaFila_(HOJAS.METRICAS_SUBJETIVAS, 'date_fecha', hoy);
  if (subjetiva && subjetiva.num_estres >= 4) {
    factor *= 0.85;
    razones.push('Estrés subjetivo alto (4-5/5, heurístico)');
  }

  return {
    factor: factor,
    razones: razones.length ? razones : ['Sesión normal — sin ajustes'],
    tipo: factor >= 1 ? 'normal' : (factor >= 0.8 ? 'reducida' : 'recuperacion')
  };
}

/**
 * Detecta si FC reposo está en tendencia ascendente los últimos N días.
 * Adaptado de Kiviniemi 2007.
 */
function esTendenciaFCAscendente_(dias) {
  const hoja = getHoja_(HOJAS.METRICAS_ZEPP);
  if (!hoja) return false;
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];
  const colFC = cab.indexOf('num_hr_reposo');
  if (colFC < 0) return false;

  var ultimos = [];
  for (let i = Math.max(1, datos.length - dias - 1); i < datos.length; i++) {
    if (datos[i][colFC]) ultimos.push(Number(datos[i][colFC]));
  }
  if (ultimos.length < dias + 1) return false;

  // Verificar tendencia: cada día mayor que el anterior
  for (let i = ultimos.length - dias; i < ultimos.length; i++) {
    if (ultimos[i] <= ultimos[i - 1]) return false;
  }
  return true;
}

/**
 * Peso sugerido para un ejercicio — CALCULADO DINÁMICAMENTE.
 *
 * CASO DE USO:
 *   Usuario llega al gym → app muestra peso → entrena → registra series →
 *   próxima sesión → motor recalcula basándose en rendimiento + contexto.
 *
 * CAPAS DE AJUSTE (multiplicativas, aplicadas en orden):
 *
 *   CAPA 1 — BASE: último peso registrado en ejercicios_log.
 *   CAPA 2 — APRE (Mann 2010 + ACSM 2009): rendimiento real vs objetivo.
 *            Usa delta_capacidad = (reps + RIR real) − (reps obj + RIR obj).
 *   CAPA 3 — FASE (Bompa 2019): modula agresividad de progresión.
 *            VOL=moderado, FZA=agresivo, DEF/MNT=conservador, DELOAD=reducir.
 *   CAPA 4 — NUTRICIÓN (Helms 2014): en cutting, cap la subida.
 *   CAPA 5 — DESCANSO INTER-SESIÓN: gaps >7d → reducción de seguridad.
 *   CAPA 6 — DÍA (Kiviniemi 2007): fatiga/sueño/estrés → factor externo.
 *
 * @param {string} ejercicioId - ID del ejercicio
 * @param {Object} ctx - Contexto completo de la sesión actual
 * @param {number} ctx.ajusteDia   - Factor diario 0.70-1.0 (Kiviniemi)
 * @param {string} ctx.fase         - VOL/FZA/DEF/MNT/DELOAD
 * @param {string} ctx.objetivoNutri - bulk/cut/mantener
 * @param {number} ctx.repsObjetivo  - Reps planificadas para hoy
 * @param {number} ctx.rirObjetivo   - RIR planificado para hoy
 * @returns {Object} { peso: Number, detalle: String, capas: Object }
 */
function calcularPesoSugerido_(ejercicioId, ctx) {
  var resultado = { peso: 0, detalle: '', capas: {} };

  // ── CAPA 1: BASE — último log de este ejercicio ──────────────
  var hoja = getHoja_(HOJAS.EJERCICIOS_LOG);
  if (!hoja || hoja.getLastRow() <= 1) {
    resultado.detalle = 'Sin historial — elige tu peso';
    return resultado;
  }

  var datos = hoja.getDataRange().getValues();
  var cab = datos[0];
  var colEj = cab.indexOf('ejercicio_id');

  // Mann 2010: usar el ÚLTIMO set (refleja fatiga acumulada = realista)
  var ultimo = null;
  for (var i = datos.length - 1; i >= 1; i--) {
    if (datos[i][colEj] === ejercicioId) {
      ultimo = rowToObj_(cab, datos[i]);
      break;
    }
  }

  if (!ultimo) {
    resultado.detalle = 'Primer uso — elige tu peso';
    return resultado;
  }

  var pesoBase = Number(ultimo.num_peso_usado_kg) || 0;
  if (pesoBase <= 0) {
    resultado.detalle = 'Último peso fue 0 — elige tu peso';
    return resultado;
  }

  var reps = Number(ultimo.num_reps_completadas) || 0;
  var rir  = Number(ultimo.num_rir_percibido);
  var sensacion = (ultimo.str_sensacion || 'bien').toLowerCase();

  // Validación cruzada sensación ↔ RIR (Helms 2016: RIR es una habilidad aprendida).
  // Si hay contradicción, la sensación gana — es más intuitiva para novatos.
  if (sensacion === 'fallo' && rir > 1) rir = 0;
  else if (sensacion === 'facil' && rir < 3) rir = 3;

  resultado.capas.base = pesoBase;
  resultado.capas.ultimoReps = reps;
  resultado.capas.ultimoRIR = rir;
  resultado.capas.sensacion = sensacion;

  // ── CAPA 3: FASE — check deload primero (Bompa 2019) ────────
  var fase = (ctx.fase || 'VOL').toUpperCase();
  var cfgFase = obtenerConfigFase_(fase);

  // DELOAD: override completo. Bompa 2019: reducir intensidad 10-15%.
  // El volumen ya está reducido en el plan (menos series pre-generadas).
  if (cfgFase.esDeload) {
    var pesoDeload = redondear025_(pesoBase * cfgFase.factorIntensidad);
    resultado.peso = Math.max(0, pesoDeload);
    resultado.detalle = 'DELOAD: ' + Math.round(cfgFase.factorIntensidad * 100) +
      '% intensidad (Bompa 2019) | ' + pesoBase + 'kg → ' + resultado.peso + 'kg';
    resultado.capas.deload = true;
    resultado.capas.factorIntensidad = cfgFase.factorIntensidad;
    return resultado;
  }

  // ── CAPA 2: APRE — rendimiento vs objetivo (Mann 2010 + ACSM 2009) ──
  //
  // delta_capacidad = capacidad_real − capacidad_objetivo
  //   real    = reps_hechas + RIR_percibido  (cuántas PODRÍA haber hecho)
  //   objetivo = reps_plan  + RIR_plan       (cuántas DEBERÍA poder hacer)
  //
  // Positivo → demasiado fácil → subir peso.
  // Negativo → demasiado duro  → bajar peso.
  // ~0       → correcto        → mantener.
  //
  // NOTA: como el RIR objetivo cambia cada semana (Helms 2016:
  //   sem1 RIR 3-4, sem2 RIR 2-3, sem3 RIR 1-2, sem4 deload),
  //   la fórmula se auto-ajusta al microciclo sin lógica extra.
  //
  var repsObj = Number(ctx.repsObjetivo) || 10;
  var rirObj  = Number(ctx.rirObjetivo)  || 2;
  var deltaCap = (reps + rir) - (repsObj + rirObj);

  // Tabla APRE de 5 niveles (Mann 2010) en porcentaje (ACSM 2009: +2-10%)
  var pctAPRE, nivelAPRE;
  if (deltaCap <= -4)      { pctAPRE = -0.10; nivelAPRE = 'muy_pesado';  }
  else if (deltaCap <= -2) { pctAPRE = -0.05; nivelAPRE = 'pesado';      }
  else if (deltaCap <= 1)  { pctAPRE =  0;    nivelAPRE = 'correcto';    }
  else if (deltaCap <= 3)  { pctAPRE =  0.05; nivelAPRE = 'facil';       }
  else                     { pctAPRE =  0.10; nivelAPRE = 'muy_facil';   }

  // Cap según fase (Bompa 2019: cada fase tiene agresividad distinta)
  if (pctAPRE > 0) pctAPRE = Math.min(pctAPRE, cfgFase.capSubida);
  if (pctAPRE < 0) pctAPRE = Math.max(pctAPRE, -cfgFase.capBajada);

  var ajusteKg = pesoBase * pctAPRE;

  // Incremento mínimo significativo: 1.25 kg (placa gym estándar = 0.625 kg/lado)
  if (ajusteKg !== 0 && Math.abs(ajusteKg) < 1.25) {
    ajusteKg = Math.sign(ajusteKg) * 1.25;
  }

  resultado.capas.repsObj = repsObj;
  resultado.capas.rirObj = rirObj;
  resultado.capas.deltaCap = deltaCap;
  resultado.capas.pctAPRE = pctAPRE;
  resultado.capas.nivelAPRE = nivelAPRE;
  resultado.capas.ajusteKg = redondear025_(ajusteKg);
  resultado.capas.fase = fase;
  resultado.capas.faseNombre = cfgFase.nombre;

  // ── CAPA 4: NUTRICIÓN (Helms 2014 + ACSM 2009) ─────────────
  // En déficit calórico es fisiológicamente difícil ganar fuerza.
  // Se permite progresión pero se reduce al 50% del APRE.
  var objNutri = (ctx.objetivoNutri || 'bulk').toLowerCase();
  if (objNutri === 'cut' && ajusteKg > 0) {
    ajusteKg *= 0.5;
    resultado.capas.nutriCut = true;
  }
  resultado.capas.objetivoNutri = objNutri;

  var pesoProg = pesoBase + ajusteKg;

  // ── CAPA 5: DESCANSO INTER-SESIÓN ──────────────────────────
  // ⚠️ HEURÍSTICO: no hay paper con umbral exacto, pero ACSM 2009
  // recomienda frecuencia 2-3×/sem. Gaps largos implican desentrenamiento
  // parcial; reducir por seguridad.
  var factorDescanso = 1.0;
  var fechaUltimo = parseDate_(ultimo.date_timestamp);
  if (fechaUltimo) {
    var diasDesde = Math.floor((new Date() - fechaUltimo) / 86400000);
    resultado.capas.diasDesdeUltimo = diasDesde;
    if (diasDesde > 14) {
      factorDescanso = 0.90;
      resultado.capas.gapAlerta = '>14d sin ejercicio → ×0.90';
    } else if (diasDesde > 7) {
      factorDescanso = 0.95;
      resultado.capas.gapAlerta = '>7d sin ejercicio → ×0.95';
    }
  }
  resultado.capas.factorDescanso = factorDescanso;

  // ── CAPA 6: AJUSTE DIARIO (Kiviniemi 2007) — calculado externamente ──
  var factorDia = Number(ctx.ajusteDia) || 1.0;
  resultado.capas.factorDia = factorDia;

  // ── PESO FINAL ──────────────────────────────────────────────
  var pesoFinal = redondear025_(pesoProg * factorDescanso * factorDia);
  pesoFinal = Math.max(0, pesoFinal);

  resultado.peso = pesoFinal;
  resultado.detalle = construirDetalleMotor_(
    pesoBase, pesoFinal, nivelAPRE, cfgFase.nombre,
    factorDescanso, factorDia, objNutri
  );
  return resultado;
}

/**
 * Configuración de progresión por fase (Bompa 2019).
 *
 * capSubida/capBajada: máximo % de cambio por sesión.
 *   VOL → Schoenfeld 2017: driver es volumen, progresión moderada.
 *   FZA → ACSM 2009: «1-6 RM» requiere saltos mayores.
 *   DEF → Helms 2014: déficit calórico limita adaptaciones neurales.
 *   MNT → Objetivo: preservar ganancias, no progresar.
 *   DELOAD → Bompa 2019: «reducir intensidad 10-15% (opcional)».
 */
function obtenerConfigFase_(fase) {
  var configs = {
    'VOL':    { capSubida: 0.05, capBajada: 0.10, esDeload: false, nombre: 'Hipertrofia' },
    'FZA':    { capSubida: 0.10, capBajada: 0.10, esDeload: false, nombre: 'Fuerza' },
    'DEF':    { capSubida: 0.03, capBajada: 0.10, esDeload: false, nombre: 'Definición' },
    'MNT':    { capSubida: 0.025, capBajada: 0.05, esDeload: false, nombre: 'Mantenimiento' },
    'DELOAD': { capSubida: 0, capBajada: 0, esDeload: true, factorIntensidad: 0.875, nombre: 'Descarga' }
  };
  return configs[fase] || configs['VOL'];
}

/** Resumen legible del cálculo para debug/UI. */
function construirDetalleMotor_(base, final, nivel, fase, fDesc, fDia, nutri) {
  var p = [base + 'kg'];
  var flechas = {
    'muy_pesado': '↓↓ muy pesado', 'pesado': '↓ pesado', 'correcto': '= bien',
    'facil': '↑ fácil', 'muy_facil': '↑↑ muy fácil'
  };
  p.push(flechas[nivel] || nivel);
  p.push(fase);
  if (nutri === 'cut') p.push('cut');
  if (fDesc < 1) p.push('gap-' + Math.round((1 - fDesc) * 100) + '%');
  if (fDia < 1) p.push('fatiga-' + Math.round((1 - fDia) * 100) + '%');
  p.push('→ ' + final + 'kg');
  return p.join(' | ');
}

/** Redondea al 0.25 kg más cercano (placa mínima estándar). */
function redondear025_(v) { return Math.round(v * 4) / 4; }

function calcularMediaFC_(dias) {
  const hoja = getHoja_(HOJAS.METRICAS_ZEPP);
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];
  const colFC = cab.indexOf('num_hr_reposo');
  var vals = [];
  for (let i = Math.max(1, datos.length - dias); i < datos.length; i++) {
    if (datos[i][colFC]) vals.push(datos[i][colFC]);
  }
  return vals.length ? vals.reduce(function(a, b) { return a + b; }, 0) / vals.length : 53; // Fallback: motor_pesos.md §2 (FC reposo baseline usuario = 53 bpm)
}

// ─── §5. AUXILIARES ───────────────────────────────────────────

function getHoja_(nombre) {
  return SpreadsheetApp.getActiveSpreadsheet().getSheetByName(nombre);
}

function jsonOutput_(json) {
  return ContentService.createTextOutput(typeof json === 'string' ? json : JSON.stringify(json))
    .setMimeType(ContentService.MimeType.JSON);
}

function genId_(pre) {
  return pre + '_' + Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyyMMdd') + '_' + Math.floor(Math.random() * 9999).toString().padStart(4, '0');
}

function fechaHoy_() {
  return Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd');
}

function formatDate_(d) {
  return d ? Utilities.formatDate(d, 'Europe/Madrid', 'yyyy-MM-dd') : null;
}

function parseDate_(v) {
  if (!v && v !== 0) return null;
  if (v instanceof Date) return v;
  if (typeof v === 'number') {
    var d = new Date((v - 25569) * 86400000);
    return isNaN(d.getTime()) ? null : d;
  }
  var d = new Date(v);
  return isNaN(d.getTime()) ? null : d;
}

function rowToObj_(cab, fila) {
  var o = {};
  for (var i = 0; i < cab.length; i++) o[cab[i]] = fila[i];
  return o;
}

function getEjerciciosSesion_(sesionId) {
  const hoja = getHoja_(HOJAS.EJERCICIOS_PLAN);
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];
  const col = cab.indexOf('sesion_id');
  var res = [];
  for (let i = 1; i < datos.length; i++) {
    if (datos[i][col] === sesionId) res.push(rowToObj_(cab, datos[i]));
  }
  res.sort(function(a, b) { return a.num_orden - b.num_orden; });

  // Enriquecer con nombre del catálogo
  const catalogo = getCatalogoMap_();
  res.forEach(function(ej) {
    var cat = catalogo[ej.ejercicio_id];
    if (cat) {
      ej.nombre = cat.str_nombre;
      ej.str_grupo_principal = cat.str_grupo_principal;
      ej.str_equipamiento = cat.str_equipamiento;
    }
  });
  return res;
}

function getCatalogoMap_() {
  const hoja = getHoja_(HOJAS.EJERCICIOS_CATALOGO);
  if (!hoja) return {};
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];
  var mapa = {};
  for (let i = 1; i < datos.length; i++) {
    var obj = rowToObj_(cab, datos[i]);
    mapa[obj.ejercicio_id] = obj;
  }
  return mapa;
}

function getUltimaFila_(hoja, colFecha, fecha) {
  const h = getHoja_(hoja);
  if (!h) return null;
  const datos = h.getDataRange().getValues();
  const cab = datos[0];
  const idx = cab.indexOf(colFecha);
  if (idx < 0) return null;
  for (let i = datos.length - 1; i >= 1; i--) {
    const f = parseDate_(datos[i][idx]);
    if (f && formatDate_(f) === fecha) return rowToObj_(cab, datos[i]);
  }
  return null;
}

function leerDatosDesdeFecha_(nombreHoja, colFecha, desde, mapper) {
  const hoja = getHoja_(nombreHoja);
  if (!hoja) return [];
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];
  const idx = cab.indexOf(colFecha);
  if (idx < 0) return [];
  var res = [];
  for (let i = 1; i < datos.length; i++) {
    const f = parseDate_(datos[i][idx]);
    if (f && f >= desde) {
      var row = rowToObj_(cab, datos[i]);
      row[colFecha] = formatDate_(f);
      res.push(mapper(row));
    }
  }
  return res.sort(function(a, b) { return (a.fecha || '').localeCompare(b.fecha || ''); });
}

function getPesoActual_() {
  const hoja = getHoja_(HOJAS.METRICAS_ZEPP);
  if (!hoja) return 78.2; // Fallback: biometria.md peso actual
  const datos = hoja.getDataRange().getValues();
  if (datos.length <= 1) return 78.2; // Fallback: biometria.md
  const cab = datos[0];
  const colP = cab.indexOf('num_peso_kg');
  const colSync = cab.indexOf('date_sync');
  for (let i = datos.length - 1; i >= 1; i--) {
    // Ignorar filas de rellenarDatosFicticios() (date_sync = 'FICTICIO') — el
    // peso real usado para calcular macros nunca debe venir de datos de prueba.
    if (colSync >= 0 && datos[i][colSync] === 'FICTICIO') continue;
    const p = Number(datos[i][colP]);
    if (p > 0) return p;
  }
  return 78.2; // Fallback: biometria.md peso actual
}

function getPasosHoy_() {
  const m = getUltimaFila_(HOJAS.METRICAS_ZEPP, 'date_fecha', fechaHoy_());
  return m ? (m.num_pasos || 0) : 0;
}

/**
 * ejercicios_log SOLO guarda la última semana (EJERCICIOS_LOG_RETENCION_DIAS)
 * — únicamente sirve para el reajuste dinámico del motor de cargas
 * (calcularPesoSugerido_ lee el último rendimiento real). No es un historial
 * permanente: sobre todo en la fase de adaptación habrá bastante diferencia
 * entre lo planeado y lo real, y con el tiempo el motor se vuelve más
 * preciso — pero eso lo capta el peso ya recalculado en ejercicios_plan
 * implícitamente (vía el motor), no hace falta guardar meses de logs.
 * Se llama tras cada guardarLog_(), así se mantiene solo sin cron externo.
 */
function limpiarLogsAntiguos_() {
  const hoja = getHoja_(HOJAS.EJERCICIOS_LOG);
  if (!hoja) return;
  const datos = hoja.getDataRange().getValues();
  if (datos.length <= 1) return;
  const cab = datos[0];
  const col = cab.indexOf('date_timestamp');
  if (col < 0) return;
  const limite = new Date();
  limite.setDate(limite.getDate() - EJERCICIOS_LOG_RETENCION_DIAS);
  // Borrar de abajo a arriba para no desplazar índices
  for (let i = datos.length - 1; i >= 1; i--) {
    const f = parseDate_(datos[i][col]);
    if (!f || f < limite) hoja.deleteRow(i + 1);
  }
}

// ─── §6. INICIALIZAR ──────────────────────────────────────────
// Ejecutar UNA VEZ desde el editor de Apps Script.
// Crea todas las hojas con sus cabeceras correctas.

function inicializarHojas() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const esquema = {
    [HOJAS.METRICAS_ZEPP]: ['metrica_id','date_fecha','num_sleep_score','num_pasos','num_hr_reposo','num_peso_kg','num_grasa_pct','date_sync'],
    [HOJAS.METRICAS_SUBJETIVAS]: ['subjetiva_id','date_fecha','num_energia','num_estres','str_notas'],
    [HOJAS.PLAN_ANUAL]: ['fase_id','num_año','num_orden','str_nombre_fase','str_tipo','date_inicio','date_fin','num_semanas','num_volumen_objetivo','str_rir_rango','str_foco_muscular','str_objetivo_nutri','str_notas'],
    [HOJAS.SESIONES_PLAN]: ['sesion_id','date_fecha','str_tipo','num_semana_meso','str_fase','num_ajuste_volumen','num_duracion_est_min','bool_completada','date_inicio','date_fin','date_creado'],
    [HOJAS.EJERCICIOS_PLAN]: ['plan_id','sesion_id','ejercicio_id','num_orden','num_series_plan','str_reps_plan','num_rir_objetivo','num_descanso_seg','str_notas','bool_es_warmup','str_superset_grupo'],
    [HOJAS.EJERCICIOS_LOG]: ['log_id','plan_id','sesion_id','ejercicio_id','num_serie','num_peso_usado_kg','num_reps_completadas','num_rir_percibido','str_sensacion','date_timestamp'],
    [HOJAS.EJERCICIOS_CATALOGO]: ['ejercicio_id','str_nombre','str_nombre_en','str_grupo_principal','arr_grupos_secundarios','str_patron','str_equipamiento','bool_compuesto','bool_favorito','bool_excluido','str_razon_exclusion','str_alternativa']
  };

  for (const [nombre, cabs] of Object.entries(esquema)) {
    var h = ss.getSheetByName(nombre);
    if (!h) h = ss.insertSheet(nombre);
    h.getRange(1, 1, 1, cabs.length).setValues([cabs]).setFontWeight('bold');
    h.setFrozenRows(1);

    // Forzar formato de texto plano en columnas str_* — sin esto, Sheets
    // autodetecta el tipo al escribir y algo como "8-10" (str_reps_plan) se
    // interpreta como fecha (8 de octubre), corrompiendo el valor. Se aplica
    // a toda la columna (no solo filas existentes) para que también cubra
    // filas que se añadan después (populate, generarSesionTestHoy_, etc).
    cabs.forEach((cab, i) => {
      if (cab.indexOf('str_') === 0) {
        const letra = columnaALetra_(i + 1);
        h.getRange(letra + '2:' + letra).setNumberFormat('@');
      }
    });
  }
  return { ok: true, mensaje: 'Hojas creadas con cabeceras' };
}

/** Convierte índice de columna (1-based) a letra A1 ('A', 'B', ..., 'AA', ...). */
function columnaALetra_(col) {
  let letra = '';
  while (col > 0) {
    const resto = (col - 1) % 26;
    letra = String.fromCharCode(65 + resto) + letra;
    col = Math.floor((col - 1) / 26);
  }
  return letra;
}

// ─── §7. RELLENAR ─────────────────────────────────────────────
// Genera plan anual, semanal, sesiones y ejercicios.
// Ejecutar DESPUÉS de inicializarHojas().
// ejercicios_plan almacena SOLO lógica (series, reps, RIR, descanso).
// Los PESOS NO se almacenan — se calculan dinámicamente en cada
// petición GET desde ejercicios_log (APRE Mann 2010).

// --- FASES (periodización basada en Bompa 2019 + prioridades.md) ---
// Estructura: VOL(6)→DELOAD(1)→VOL(6)→DELOAD(1)→FZA(6)→VOL(6)→DELOAD(1)→DEF(6)→MNT(11)
// Justificación:
//   - Mesociclos de 6 sem (Bompa 2019: 4-6 sem óptimo, se usa 6 por mayor volumen)
//   - Deloads cada 6 sem (Bompa: cada 4-6 semanas, -40% volumen)
//   - VOL→FZA→DEF (ondulación clásica Bompa: AA→MF→P)
//   - RIR progresión intra-meso: VOL 4→3→2 / FZA 2→2→1 (Helms 2016)
//   - Foco por fase según prioridades.md: P1(V-taper) → P3(brazos) → balance → cut
//   - Nutrición por fase: bulk → mantener(deloads) → cut(DEF) → mantener(MNT)
// Revisión evidence-based (periodizacion.md §7: deload "cada 4-6 semanas,
// por fatiga acumulada, o al final de cada macrociclo"). La versión anterior
// tenía dos huecos sin deload que violaban esto: FZA(6sem)+Hipertrofia
// III(6sem) = 12 semanas seguidas al RIR más bajo del año sin descarga, y
// Definición(6sem)+Peak(11sem) = 17 semanas seguidas — la segunda mitad
// encima en déficit calórico (Helms 2014: recuperación reducida), justo el
// peor momento para acumular fatiga sin válvula de escape. Se añaden 3
// deloads (FAS_07, FAS_11, FAS_13) y se parte el bloque Peak Estético en
// dos para respetar la cadencia — el total sigue en 48 semanas (11 meses)
// recortando Peak de 11 a 4+4 semanas.
//
// Módulo (no local a rellenarPlanCompleto): regenerarSesionesDesde_ también
// necesita FASES/T/ESPECIALIZACION al cambiar el horario semanal.
const FASES = [
  {id:'FAS_01', nombre:'Adaptación + Postura', tipo:'VOL', inicio:'2026-08-31', fin:'2026-09-27', sem:4, rir:'3-4', foco:'Full Body + Correctivos posturales', nutri:'bulk'},
  {id:'FAS_02', nombre:'Hipertrofia I — V-Taper', tipo:'VOL', inicio:'2026-09-28', fin:'2026-11-08', sem:6, rir:'2-3', foco:'Hombros+Espalda (P1: V-taper)', nutri:'bulk'},
  {id:'FAS_03', nombre:'Deload 1', tipo:'DELOAD', inicio:'2026-11-09', fin:'2026-11-15', sem:1, rir:'5-6', foco:'Movilidad + Test Wall Angel', nutri:'mantener'},
  {id:'FAS_04', nombre:'Hipertrofia II — Brazos', tipo:'VOL', inicio:'2026-11-16', fin:'2026-12-27', sem:6, rir:'2-3', foco:'Bíceps+Tríceps+Pecho', nutri:'bulk'},
  {id:'FAS_05', nombre:'Deload 2', tipo:'DELOAD', inicio:'2026-12-28', fin:'2027-01-03', sem:1, rir:'5-6', foco:'Descanso activo + Flex', nutri:'mantener'},
  {id:'FAS_06', nombre:'Fuerza — Compuestos', tipo:'FZA', inicio:'2027-01-04', fin:'2027-02-14', sem:6, rir:'1-2', foco:'Press militar+Dominadas+Sentadilla', nutri:'bulk'},
  {id:'FAS_07', nombre:'Deload 3', tipo:'DELOAD', inicio:'2027-02-15', fin:'2027-02-21', sem:1, rir:'5-6', foco:'Movilidad + Descarga tras bloque Fuerza', nutri:'mantener'},
  {id:'FAS_08', nombre:'Hipertrofia III — Balance', tipo:'VOL', inicio:'2027-02-22', fin:'2027-04-04', sem:6, rir:'2-3', foco:'Piernas+Core + Mantener V-taper', nutri:'bulk'},
  {id:'FAS_09', nombre:'Deload 4', tipo:'DELOAD', inicio:'2027-04-05', fin:'2027-04-11', sem:1, rir:'5-6', foco:'Test postural + Flex', nutri:'mantener'},
  {id:'FAS_10', nombre:'Definición', tipo:'DEF', inicio:'2027-04-12', fin:'2027-05-23', sem:6, rir:'2-3', foco:'Mantener masa + Déficit controlado', nutri:'cut'},
  {id:'FAS_11', nombre:'Deload 5', tipo:'DELOAD', inicio:'2027-05-24', fin:'2027-05-30', sem:1, rir:'5-6', foco:'Transición cut→mantenimiento + Flex', nutri:'mantener'},
  {id:'FAS_12', nombre:'Peak Estético I', tipo:'MNT', inicio:'2027-05-31', fin:'2027-06-27', sem:4, rir:'2-3', foco:'Ratio cintura/hombros + Simetría', nutri:'mantener'},
  {id:'FAS_13', nombre:'Deload 6', tipo:'DELOAD', inicio:'2027-06-28', fin:'2027-07-04', sem:1, rir:'5-6', foco:'Descarga media-temporada peak', nutri:'mantener'},
  {id:'FAS_14', nombre:'Peak Estético II + Mant.', tipo:'MNT', inicio:'2027-07-05', fin:'2027-08-01', sem:4, rir:'2-3', foco:'Ratio cintura/hombros + Simetría', nutri:'mantener'}
];

// --- TEMPLATES EJERCICIOS (por tipo sesión × tipo fase) ---
// Volumen semanal por grupo (Schoenfeld 2017 + programacion.md §3):
//   Hombros: 14-18 ser/sem → Push(11) + Hombros(11) = 22 ✓ (Prioridad #1: V-taper — SIN TOCAR)
//   Espalda: 14-18 ser/sem → Dominadas+Remo neutro+Remo rotación = 11 directo + correctivos ✓
//            (P1 V-taper: Dominadas es el que da ANCHURA — intacto)
//   Bíceps: 10-14 ser/sem → Pull(6) + Hombros(6) = 12 ✓ (P3, sin tocar)
//   Tríceps: 10-14 ser/sem → Push(6 directo) + press(8 indirecto) = ~10 ✓ (sin tocar)
//   Pecho: 4 ser/sem (antes 7) → Trade-off MÁS FUERTE por duración de sesión
//          (ver revisión 2026: PUSH_VOL rondaba 81min, por encima del ideal
//          75min de preferencias.md). Pecho NO es prioridad (programacion.md
//          §2: "no priorizar sobre hombros") — se eliminó Cruces polea alta
//          (aislamiento puro), Press inclinado se mantiene como único directo.
//          Por debajo del "mínimo efectivo 5 ser/sem" (programacion.md §3) —
//          trade-off consciente y máximo dado que hombros/espalda/postura no
//          se tocan; si en la práctica notas estancamiento de pecho, revisar.
//   Pierna: Pierna(4+4+3+3+3+3) = 20 directo, cuádriceps ~10 (Sentadilla+Leg
//           press+Ext.) ✓ — se quitó Pallof (core anti-rotación, sin
//           prioridad ninguna) para bajar de ~90min a ~85min en FAS_08.
//   Postura (P2): Kelso shrug y Band pull-aparts bajan de 3→2 series (Wall
//           Angels, el objetivo postural PRINCIPAL de biometria.md §8, se
//           mantiene intacto en 3) — recorte mínimo, repartido entre 2
//           ejercicios en vez de eliminar ninguno, para bajar PULL_VOL de
//           ~79min a ~75min sin tocar Dominadas/Remo neutro (espalda P1) ni
//           Curl Z/Curl predicador (bíceps P3).
//
// Revisión de duración de sesión (2026): 3 de las 4 sesiones VOL superaban
// el ideal de 75min (preferencias.md §2) y PIERNA_VOL llegaba a ~90-91min
// en semanas de especialización (FAS_08) — por encima incluso del máximo.
// Los recortes de arriba se hicieron SIEMPRE sobre grupos sin prioridad
// (pecho, core, pierna) o repartidos en accesorios menores (postura), nunca
// sobre hombros, espalda-anchura (Dominadas), bíceps ni el ejercicio
// postural principal — ver prioridades.md (P1 V-taper hombros+espalda, P2
// postura, P3 hipertrofia hombros>bíceps>espalda).
//
// Descansos (programacion.md §5, Schoenfeld 2016):
//   Compuestos pesados: 150-270s (evidencia: 3-5 min = 180-300s)
//   Aislamiento: 90-120s (evidencia: 1.5-2 min)
//   Correctivos/postura: 60s (no buscan hipertrofia)
//
// Formato: [ejercicio_id, nombre, series, reps, descanso_seg, notas, superserie]
// superserie: exercises con el mismo grupo (ej. 'SS1') van seguidos y sin
// descanso entre ellos — preferencias.md §5: "usar superseries para
// accesorios" (evidencia cumplida con descansos, pero el usuario se aburre
// esperando; las superseries eliminan esa espera muerta). NUNCA se ponen en
// superserie los ejercicios con aviso ⚠️ de codo — necesitan ejecución
// cuidadosa y pausada, no un ritmo acelerado.
const T = {
    PUSH_VOL: [
      ['EJE_PRESS_HOMB','Press hombro mancuernas',4,'8-10',150,'Compuesto hombros',''],
      ['EJE_PRESS_INC','Press inclinado mancuernas',4,'8-10',150,'Pecho',''],
      ['EJE_LAT_SENT','Elev. laterales sentado',4,'12-15',90,'P1: V-taper','SS1'],
      ['EJE_LAT_POLEA','Elev. laterales polea',3,'12-15',90,'','SS1'],
      // Cruces polea alta ELIMINADO (revisión 2026): PUSH_VOL rondaba ~81min,
      // por encima del ideal 75min (preferencias.md §2). Pecho es la prioridad
      // más baja de hipertrofia (prioridades.md: "hombros > pecho") — Press
      // inclinado se mantiene como único directo de pecho.
      ['EJE_FRANC','Press francés 30°',3,'10-12',120,'⚠️ Dolor codo: NO completar extensión total (biometria.md §9)',''],
      ['EJE_EXT_POLEA','Extensión unilateral polea',3,'12-15',90,'⚠️ Dolor codo: rango controlado, NO extensión completa',''],
      ['EJE_FACE_PULL','Face pulls',3,'15-20',90,'P2: Postura','']
    ],
    PIERNA_VOL: [
      ['EJE_SENTADILLA','Sentadilla barra',4,'6-8',180,'Compuesto',''],
      ['EJE_RDL','RDL',4,'8-10',150,'Isquios+glúteo',''],
      ['EJE_HIP_THRUST','Hip thrust',3,'10-12',120,'',''],
      // Cuádriceps (P7, secundario) se quedaba corto (10-12 ser/sem objetivo,
      // solo llegaba con sentadilla+extensión) — se necesitaba un compuesto
      // extra de cuádriceps. Revisión anterior añadió "Leg press", pero eso
      // SÍ viola una exclusión explícita del usuario (usuario/
      // preferencias_ejercicios.md §2: "Prensas en máquina — Inefectivos
      // (percepción)"), pese a que el comentario decía lo contrario — el
      // orden de selección (seleccion_ejercicios.md §2) pone las exclusiones
      // ANTES que la disponibilidad de equipo. Sustituido por Hack squat
      // (equipamiento.md: máquina distinta, no es una "prensa" — patrón
      // guiado de sentadilla, no press horizontal — y no está en ninguna
      // lista de exclusión).
      ['EJE_HACK_SQUAT','Hack squat',3,'10-12',120,'Cuádriceps extra',''],
      ['EJE_EXT_QUAD','Extensión cuádriceps',3,'12-15',90,'','SS1'],
      ['EJE_CURL_FEM','Curl femoral',3,'10-12',90,'','SS1'],
      // Press Pallof (core anti-rotación) ELIMINADO (revisión 2026): PIERNA_VOL
      // llegaba a ~90-91min en FAS_08 (especialización), por encima incluso
      // del máximo 90min (preferencias.md §2). Pierna/core no son prioridad
      // (prioridades.md: "mantener piernas pero priorizar upper" — P1
      // V-taper) — el candidato más seguro para recortar sin tocar hombros,
      // espalda, bíceps ni postura.
      ['EJE_HOLLOW','Hollow hold',3,'30s',90,'Core anti-extensión','']
    ],
    PULL_VOL: [
      ['EJE_DOMINADAS','Dominadas',4,'6-8',180,'Tirón vertical (P1)',''],
      ['EJE_REMO_NEUTRO','Remo neutro polea',4,'8-10',150,'',''],
      ['EJE_REMO_ROT','Remo unilateral con rotación',3,'10-12',120,'',''],
      // Kelso shrug y Band pull-aparts bajan de 3→2 (revisión 2026, PULL_VOL
      // rondaba ~79min): recorte mínimo repartido entre 2 correctivos en vez
      // de eliminar ninguno — Wall angels (objetivo postural PRINCIPAL,
      // biometria.md §8) se mantiene en 3. Dominadas/Remo neutro (espalda
      // P1, V-taper) y Curl Z/Curl predicador (bíceps P3) intactos.
      ['EJE_KELSO','Kelso shrug',2,'12-15',90,'P2: Retracción escapular',''],
      ['EJE_CURL_Z','Curl Z barra',3,'8-10',90,'Bíceps (P3)','SS1'],
      ['EJE_CURL_PRED','Curl predicador',3,'10-12',90,'','SS1'],
      ['EJE_BAND_PULL','Band pull-aparts',2,'15-20',60,'P2: Postura','SS2'],
      ['EJE_WALL_ANGEL','Wall angels',3,'8-10',60,'P2: Test postural','SS2']
    ],
    HOMBR_VOL: [
      ['EJE_PRESS_HOMB','Press hombro mancuernas',4,'8-10',150,'',''],
      ['EJE_LAT_SENT','Elev. laterales sentado',4,'12-15',90,'P1: V-taper extra','SS1'],
      ['EJE_LAT_POLEA','Elev. laterales polea (media altura)',3,'12-15',90,'','SS1'],
      ['EJE_PAJARO','Pájaro inclinado',3,'12-15',90,'Rear delt',''],
      ['EJE_ZOTTMAN','Curl Zottman',3,'10-12',90,'Bíceps+Antebrazo','SS2'],
      ['EJE_CURL_INC','Curl inclinado 45°',3,'10-12',90,'','SS2'],
      // Extensión overhead polea EXCLUIDA (biometria.md §9 + seleccion_ejercicios.md
      // §6: rango de estiramiento profundo, alto riesgo para dolor codo) — se
      // sustituye por la variante ya aprobada de rango controlado (ver catálogo).
      ['EJE_EXT_POLEA','Extensión unilateral polea',3,'10-12',90,'⚠️ Dolor codo: rango controlado, NO extensión completa',''],
      ['EJE_ROT_EXT','Rotación externa banda',3,'15/lado',60,'P2: Manguito rotador','']
    ],
    // FZA sin superseries: el objetivo es fuerza máxima, cada serie pesada
    // necesita su descanso completo (ACSM 2009: 3-5min) — mezclar con
    // superseries reduciría la recuperación justo donde más importa.
    PUSH_FZA: [
      ['EJE_PRESS_INC','Press inclinado mancuernas',5,'4-6',210,'Pesado',''],
      ['EJE_PRESS_HOMB','Press hombro sentado',4,'5-7',180,'',''],
      ['EJE_LAT_SENT','Elev. laterales sentado',4,'10-12',90,'Mantener volumen hombros',''],
      // Reps mantenidas en rango hipertrofia (NO rango fuerza 4-6 como el resto
      // de la sesión): seleccion_ejercicios.md §6 — "evitar extensión tríceps
      // bajo carga pesada" para dolor codo. Un aislamiento no necesita ir a
      // rango de fuerza de todos modos (seleccion_ejercicios.md §4).
      ['EJE_FRANC','Press francés 30°',3,'10-12',120,'⚠️ Dolor codo: NO completar extensión total, carga moderada (biometria.md §9)','']
    ],
    PIERNA_FZA: [
      ['EJE_SENTADILLA','Sentadilla barra',5,'4-6',270,'Pesado',''],
      ['EJE_RDL','RDL',4,'5-7',180,'',''],
      ['EJE_HIP_THRUST','Hip thrust',3,'6-8',150,'',''],
      ['EJE_PLANCHA','Plancha lastrada',3,'45-60s',90,'Core','']
    ],
    PULL_FZA: [
      ['EJE_DOMINADAS','Dominadas lastradas',5,'4-6',210,'Pesado',''],
      ['EJE_REMO_NEUTRO','Remo neutro',4,'6-8',180,'',''],
      ['EJE_REMO_ROT','Remo unilateral',3,'8-10',150,'',''],
      ['EJE_CURL_Z','Curl Z',3,'6-8',120,'Pesado',''],
      ['EJE_FACE_PULL','Face pulls',2,'15',60,'Postura mantenimiento','']
    ],
    HOMBR_FZA: [
      ['EJE_PRESS_MIL','Press militar barra',4,'5-7',180,'Compuesto pesado',''],
      ['EJE_LAT_POLEA','Elev. laterales polea',4,'10-12',90,'Volumen medial',''],
      ['EJE_CURL_PRED','Curl predicador',4,'6-8',120,'Pesado',''],
      // Mismo criterio que en PUSH_FZA: rango hipertrofia, no fuerza, por el
      // dolor de codo (seleccion_ejercicios.md §6 + biometria.md §9).
      ['EJE_EXT_POLEA','Extensión unilateral polea',3,'10-12',120,'⚠️ Dolor codo: NO completar extensión total, carga moderada (biometria.md §9)','']
    ]
  };

// Mapa fase→template: DEF y MNT usan VOL (mantener masa)
function getTemplate(faseTipo, sesionTipo) {
  if (faseTipo === 'FZA') return sesionTipo + '_FZA';
  return sesionTipo + '_VOL'; // VOL, DELOAD, DEF, MNT → todos usan VOL template
}

// Especialización de volumen por fase (Schoenfeld 2017: volumen es el driver)
// Las fases de hipertrofia especializadas añaden +1 serie a sus grupos foco.
// Esto se aplica post-template según el foco de la fase.
// Formato: { ejercicio_id: series_extra }
const ESPECIALIZACION = {
  'FAS_02': { 'EJE_LAT_SENT':1, 'EJE_LAT_POLEA':1, 'EJE_DOMINADAS':1, 'EJE_REMO_NEUTRO':1 }, // V-Taper: +hombros +espalda
  'FAS_04': { 'EJE_CURL_Z':1, 'EJE_CURL_PRED':1, 'EJE_ZOTTMAN':1, 'EJE_FRANC':1, 'EJE_EXT_POLEA':1 }, // Brazos: +bíceps +tríceps
  'FAS_08': { 'EJE_SENTADILLA':1, 'EJE_RDL':1, 'EJE_HIP_THRUST':1, 'EJE_HOLLOW':1 }  // Balance: +pierna +core (EJE_PALLOF ya no está en el template, ver PIERNA_VOL)
};

const TIPO_DISPLAY = { PUSH:'Push', PIERNA:'Pierna', PULL:'Pull', HOMBR:'Hombros+Brazos' };

/**
 * Genera filas de sesiones_plan + ejercicios_plan para el rango
 * [fechaDesde, fechaHasta] (recortado al solape con cada fase), usando el
 * horario semanal dado (día de la semana → PUSH/PIERNA/PULL/HOMBR/
 * NATACION/DESCANSO). No escribe nada en las hojas — el llamador decide
 * cómo insertar las filas (todo el plan de una vez en rellenarPlanCompleto,
 * o solo el tramo futuro en regenerarSesionesDesde_).
 */
function generarFilasSesiones_(fechaDesde, fechaHasta, horario) {
  var filasSes = [], filasEj = [];
  var sesN = 0, ejN = 0;

  for (var fi = 0; fi < FASES.length; fi++) {
    var fase = FASES[fi];
    var faseInicio = new Date(fase.inicio);
    var faseFin = new Date(fase.fin);
    var esDeload = fase.tipo === 'DELOAD';

    var desde = faseInicio > fechaDesde ? faseInicio : fechaDesde;
    var hasta = faseFin < fechaHasta ? faseFin : fechaHasta;
    if (desde > hasta) continue; // esta fase no solapa con el rango pedido

    // La semana-dentro-de-fase (para el RIR progresivo) se cuenta SIEMPRE
    // desde el inicio real de la fase, no desde `desde` — si se regenera
    // solo un tramo futuro, la progresión RIR no debe reiniciarse.
    var fecha = new Date(faseInicio);
    var semFase = 1;
    while (fecha < desde) {
      if (fecha.getDay() === 0) semFase++;
      fecha.setDate(fecha.getDate() + 1);
    }

    while (fecha <= hasta) {
      var dia = fecha.getDay(); // 0=dom, 1=lun...6=sab
      var fStr = Utilities.formatDate(fecha, 'Europe/Madrid', 'yyyy-MM-dd');
      var tipoSesion = horario[dia];

      if (TIPO_DISPLAY[tipoSesion]) {
        sesN++;
        var sesId = 'SES_' + fStr.replace(/-/g, '') + '_' + String(sesN).padStart(3,'0');

        filasSes.push([sesId, fStr, TIPO_DISPLAY[tipoSesion], semFase, fase.nombre, 1.0, 75, false, '', '', new Date().toISOString()]);

        // Ejercicios
        var tmplKey = getTemplate(fase.tipo, tipoSesion);
        var tmpl = T[tmplKey] || T[tipoSesion + '_VOL'];
        var especFase = ESPECIALIZACION[fase.id] || {};
        if (tmpl) {
          for (var oi = 0; oi < tmpl.length; oi++) {
            var ej = tmpl[oi];
            ejN++;
            var planId = 'PLA_' + fStr.replace(/-/g, '') + '_' + String(ejN).padStart(4,'0');
            var series = ej[2];
            if (esDeload) series = Math.max(2, Math.ceil(series * 0.6)); // Bompa 2009: -40% vol
            // Especialización: +series en grupos foco de la fase (Schoenfeld 2017)
            if (especFase[ej[0]]) series += especFase[ej[0]];
            // RIR por semana en mesociclo (Helms 2016 + ACSM 2009 + programacion.md §7-8):
            //   VOL/DEF/MNT: sem1=4, sem2=3, sem3=2, [repite] (RPE 6→7→8 = hipertrofia)
            //   FZA: sem1=2, sem2=2, sem3=1, [repite] (RPE 8→8→9 = fuerza, ACSM 2009)
            //   DELOAD: siempre 5 (RPE 5 = recuperación activa, Bompa 2019)
            var rirNum;
            if (esDeload) {
              rirNum = 5;
            } else if (fase.tipo === 'FZA') {
              rirNum = ((semFase - 1) % 3 === 2) ? 1 : 2;
            } else {
              rirNum = ((semFase - 1) % 3 === 0) ? 4 : ((semFase - 1) % 3 === 1) ? 3 : 2;
            }
            // SIN peso — se calcula dinámicamente desde ejercicios_log (APRE Mann 2010)
            // Superserie (ej[6]) se anula en deload: en deload todo va a RIR
            // 4-5 y volumen mínimo, no tiene sentido acelerar el ritmo.
            filasEj.push([planId, sesId, ej[0], oi+1, series, ej[3], rirNum, ej[4], ej[5], false, esDeload ? '' : (ej[6] || '')]);
          }
        }
      }

      if (dia === 0) semFase++; // fin de semana → cambio de semana dentro de la fase
      fecha.setDate(fecha.getDate() + 1);
    }
  }

  return { filasSes: filasSes, filasEj: filasEj, sesN: sesN, ejN: ejN };
}

/**
 * Genera y guarda una única sesión real de HOY (pre-temporada, ver
 * getSesionHoy_) usando la fase FAS_01 (la primera del plan real) como
 * plantilla — mismas series/reps/rir/superseries que se usarán cuando el
 * plan arranque de verdad (Schoenfeld 2017, misma tabla T[] de
 * generarFilasSesiones_). Solo la fecha es "de prueba"; el contenido de la
 * sesión es idéntico a lo que generaría el plan real ese día de la semana.
 */
function generarSesionTestHoy_(hoy, tipoSesion) {
  const hojaSes = getHoja_(HOJAS.SESIONES_PLAN);
  const hojaEj = getHoja_(HOJAS.EJERCICIOS_PLAN);
  const fase = FASES[0];
  const esDeload = fase.tipo === 'DELOAD';
  const semFase = 1;
  const rirNum = esDeload ? 5 : (fase.tipo === 'FZA' ? 2 : 4); // progresión RIR, semana 1

  const fStr = hoy.replace(/-/g, '');
  const sesId = 'SES_' + fStr + '_TEST';
  const filaSes = [sesId, hoy, TIPO_DISPLAY[tipoSesion], semFase, fase.nombre, 1.0, 75, false, '', '', new Date().toISOString()];
  hojaSes.appendRow(filaSes);

  const tmplKey = getTemplate(fase.tipo, tipoSesion);
  const tmpl = T[tmplKey] || T[tipoSesion + '_VOL'];
  if (tmpl && tmpl.length) {
    const filasEj = tmpl.map(function(ej, oi) {
      return ['PLA_' + fStr + '_T' + String(oi + 1).padStart(2, '0'),
              sesId, ej[0], oi + 1, ej[2], ej[3], rirNum, ej[4], ej[5], false, ej[6] || ''];
    });
    hojaEj.getRange(hojaEj.getLastRow() + 1, 1, filasEj.length, filasEj[0].length).setValues(filasEj);
  }

  return {
    sesion_id: sesId, date_fecha: hoy, str_tipo: TIPO_DISPLAY[tipoSesion],
    num_semana_meso: semFase, str_fase: fase.nombre, num_ajuste_volumen: 1.0,
    num_duracion_est_min: 75, bool_completada: false, date_inicio: '', date_fin: '',
    date_creado: filaSes[10]
  };
}

/**
 * Regenera sesiones_plan + ejercicios_plan SOLO desde `fechaDesdeStr`
 * (inclusive) hasta el final del plan, con el horario semanal actual.
 * Las filas anteriores a esa fecha (histórico ya vivido, sesiones
 * completadas incluidas) se conservan tal cual — nunca se tocan.
 */
function regenerarSesionesDesde_(fechaDesdeStr) {
  const hojaSes = getHoja_(HOJAS.SESIONES_PLAN);
  const hojaEj = getHoja_(HOJAS.EJERCICIOS_PLAN);
  const desde = parseDate_(fechaDesdeStr);

  const datosSes = hojaSes.getDataRange().getValues();
  const cabSes = datosSes[0];
  const colFecha = cabSes.indexOf('date_fecha');
  const colSesionId = cabSes.indexOf('sesion_id');
  const filasSesHistoricas = [];
  const sesionesHistoricas = {};
  for (var i = 1; i < datosSes.length; i++) {
    var f = parseDate_(datosSes[i][colFecha]);
    if (f && f < desde) {
      filasSesHistoricas.push(datosSes[i]);
      sesionesHistoricas[datosSes[i][colSesionId]] = true;
    }
  }

  const datosEj = hojaEj.getDataRange().getValues();
  const cabEj = datosEj[0];
  const colSesionIdEj = cabEj.indexOf('sesion_id');
  const filasEjHistoricas = [];
  for (var j = 1; j < datosEj.length; j++) {
    if (sesionesHistoricas[datosEj[j][colSesionIdEj]]) filasEjHistoricas.push(datosEj[j]);
  }

  const horario = getHorarioSemanal_();
  const fin = new Date(FASES[FASES.length - 1].fin);
  const gen = generarFilasSesiones_(desde, fin, horario);

  const eliminadas = (datosSes.length - 1) - filasSesHistoricas.length;

  if (hojaSes.getLastRow() > 1) hojaSes.deleteRows(2, hojaSes.getLastRow() - 1);
  if (hojaEj.getLastRow() > 1) hojaEj.deleteRows(2, hojaEj.getLastRow() - 1);

  const todasSes = filasSesHistoricas.concat(gen.filasSes);
  const todasEj = filasEjHistoricas.concat(gen.filasEj);
  if (todasSes.length) hojaSes.getRange(2, 1, todasSes.length, todasSes[0].length).setValues(todasSes);
  if (todasEj.length) hojaEj.getRange(2, 1, todasEj.length, todasEj[0].length).setValues(todasEj);

  return { eliminadas: eliminadas, generadas: gen.filasSes.length };
}

function rellenarPlanCompleto() {
  const hojaPlan = getHoja_(HOJAS.PLAN_ANUAL);
  const hojaSes = getHoja_(HOJAS.SESIONES_PLAN);
  const hojaEj = getHoja_(HOJAS.EJERCICIOS_PLAN);

  // Limpiar datos previos (conservar cabeceras)
  [hojaPlan, hojaSes, hojaEj].forEach(function(h) {
    if (h && h.getLastRow() > 1) h.deleteRows(2, h.getLastRow() - 1);
  });

  // Plan anual
  const filasPlan = FASES.map(function(f, i) {
    return [f.id, 2026, i+1, f.nombre, f.tipo, f.inicio, f.fin, f.sem, 16, f.rir, f.foco, f.nutri, ''];
  });
  hojaPlan.getRange(2, 1, filasPlan.length, filasPlan[0].length).setValues(filasPlan);

  // Sesiones + Ejercicios — horario semanal configurable (getHorarioSemanal_,
  // por defecto lun/mié/vie/sáb gym + mar/jue natación + dom descanso).
  const horario = getHorarioSemanal_();
  const inicio = new Date(FASES[0].inicio);
  const fin = new Date(FASES[FASES.length - 1].fin);
  const gen = generarFilasSesiones_(inicio, fin, horario);

  if (gen.filasSes.length) hojaSes.getRange(2, 1, gen.filasSes.length, gen.filasSes[0].length).setValues(gen.filasSes);
  if (gen.filasEj.length) hojaEj.getRange(2, 1, gen.filasEj.length, gen.filasEj[0].length).setValues(gen.filasEj);

  // Catálogo de ejercicios
  rellenarCatalogo_();

  Logger.log('Plan generado: ' + gen.sesN + ' sesiones, ' + gen.ejN + ' ejercicios');
  return { ok: true, sesiones: gen.sesN, ejercicios: gen.ejN };
}

function rellenarCatalogo_() {
  const hoja = getHoja_(HOJAS.EJERCICIOS_CATALOGO);
  if (!hoja) return;
  if (hoja.getLastRow() > 1) hoja.deleteRows(2, hoja.getLastRow() - 1);
  const cat = [
    ['EJE_PRESS_INC','Press inclinado mancuernas','Incline DB Press','Pecho','["Hombro","Tríceps"]','Empuje horizontal','Mancuernas,Banco',true,true,false,'',''],
    ['EJE_CRUCES','Cruces polea alta','High Cable Fly','Pecho','[]','Empuje horizontal','Poleas',false,true,false,'',''],
    ['EJE_PRESS_HOMB','Press hombro mancuernas','Seated DB Press','Hombros','["Tríceps"]','Empuje vertical','Mancuernas',true,true,false,'',''],
    ['EJE_PRESS_MIL','Press militar barra','Standing OHP','Hombros','["Tríceps","Core"]','Empuje vertical','Barra',true,false,false,'',''],
    ['EJE_LAT_SENT','Elev. laterales sentado','Seated Lat Raise','Hombros','[]','Lateral','Mancuernas',false,true,false,'',''],
    ['EJE_LAT_POLEA','Elev. laterales polea','Cable Lat Raise','Hombros','[]','Lateral','Polea',false,true,false,'',''],
    ['EJE_FRANC','Press francés 30°','Incline Skullcrusher','Tríceps','[]','Extensión','Barra Z',false,true,false,'',''],
    ['EJE_EXT_POLEA','Extensión unilateral polea','Cable Extension','Tríceps','[]','Extensión','Polea',false,true,false,'',''],
    // Excluido: biometria.md §9 (dolor codo) + seleccion_ejercicios.md §6 —
    // el rango de estiramiento profundo overhead es de las variantes más
    // exigentes para el codo. Ya no se usa en ningún template (ver EJE_EXT_POLEA).
    ['EJE_EXT_OVERHEAD','Extensión overhead polea','Overhead Extension','Tríceps','[]','Extensión','Polea',false,false,true,'Dolor codo (biometria.md §9) — rango profundo overhead','EJE_EXT_POLEA'],
    ['EJE_SENTADILLA','Sentadilla barra','Barbell Squat','Cuádriceps','["Glúteos","Isquios"]','Extensión rodilla','Barra,Rack',true,true,false,'',''],
    ['EJE_RDL','RDL','Romanian Deadlift','Isquios','["Glúteos"]','Extensión cadera','Barra',true,true,false,'',''],
    ['EJE_HIP_THRUST','Hip thrust','Hip Thrust','Glúteos','["Isquios"]','Extensión cadera','Barra,Banco',true,true,false,'',''],
    // Cuádriceps se quedaba corto de volumen (objetivo 10-12 ser/sem,
    // programacion.md §3). "Leg press" (revisión anterior) viola la
    // exclusión explícita "Prensas en máquina" (usuario/
    // preferencias_ejercicios.md §2) — sustituido por Hack squat, disponible
    // en equipamiento.md y no excluido (es un patrón guiado de sentadilla,
    // no una prensa horizontal).
    ['EJE_HACK_SQUAT','Hack squat','Hack Squat','Cuádriceps','["Glúteos"]','Extensión rodilla','Máquina Hack Squat',true,true,false,'',''],
    ['EJE_EXT_QUAD','Extensión cuádriceps','Leg Extension','Cuádriceps','[]','Extensión rodilla','Máquina',false,false,false,'',''],
    ['EJE_CURL_FEM','Curl femoral','Leg Curl','Isquios','[]','Flexión rodilla','Máquina',false,false,false,'',''],
    ['EJE_DOMINADAS','Dominadas','Pull-ups','Espalda','["Bíceps"]','Tirón vertical','Barra',true,true,false,'',''],
    ['EJE_REMO_NEUTRO','Remo neutro polea','Cable Row','Espalda','["Bíceps"]','Tirón horizontal','Polea',true,true,false,'',''],
    ['EJE_REMO_ROT','Remo unilateral con rotación','Single Arm Row','Espalda','["Core"]','Tirón horizontal','Mancuerna',true,true,false,'',''],
    ['EJE_KELSO','Kelso shrug','Kelso Shrug','Espalda','[]','Tirón','Mancuernas',false,true,false,'',''],
    ['EJE_FACE_PULL','Face pulls','Face Pulls','Hombros','["Trapecios"]','Tirón','Polea',false,false,false,'',''],
    ['EJE_CURL_Z','Curl Z barra','EZ Curl','Bíceps','[]','Flexión codo','Barra Z',false,true,false,'',''],
    ['EJE_ZOTTMAN','Curl Zottman','Zottman Curl','Bíceps','["Antebrazo"]','Flexión codo','Mancuernas',false,true,false,'',''],
    ['EJE_CURL_PRED','Curl predicador','Preacher Curl','Bíceps','[]','Flexión codo','Máquina',false,true,false,'',''],
    ['EJE_CURL_INC','Curl inclinado 45°','Incline Curl','Bíceps','[]','Flexión codo','Mancuernas',false,false,false,'',''],
    ['EJE_ROT_EXT','Rotación externa','External Rotation','Manguito','[]','Rotación','Banda',false,false,false,'',''],
    ['EJE_HOLLOW','Hollow hold','Hollow Hold','Core','[]','Anti-extensión','Suelo',false,true,false,'',''],
    ['EJE_PALLOF','Press Pallof','Pallof Press','Core','[]','Anti-rotación','Polea',false,true,false,'',''],
    ['EJE_PLANCHA','Plancha lastrada','Weighted Plank','Core','[]','Anti-extensión','Suelo',false,false,false,'',''],
    ['EJE_BAND_PULL','Band pull-aparts','Band Pull-Aparts','Espalda','["Hombros"]','Tirón','Banda',false,false,false,'',''],
    ['EJE_WALL_ANGEL','Wall angels','Wall Angels','Postura','["Hombros"]','Correctivo','Pared',false,false,false,'',''],
    ['EJE_PAJARO','Pájaro inclinado','Reverse Fly','Hombros','[]','Tirón','Mancuernas',false,false,false,'','']
  ];
  hoja.getRange(2, 1, cat.length, cat[0].length).setValues(cat);
}

// ─── §7b. RELLENAR DATOS FICTICIOS (SOLO PARA TESTING) ────────
// Genera histórico de prueba en metricas_zepp y ejercicios_log
// para poder comprobar que Progresión/Home funcionan con datos reales
// de la BBDD, sin depender de semanas de tracking real.
//
// NO se expone por HTTP (doGet/doPost) — se ejecuta manualmente desde
// el editor de Apps Script, igual que inicializarHojas() y
// rellenarPlanCompleto(). Los valores son ficticios (marcados con
// date_sync = 'FICTICIO') — usa limpiarDatosTest() para borrarlos
// antes de empezar a trackear datos reales.

function rellenarDatosFicticios(dias) {
  dias = dias || 30;
  const hoy = new Date();
  const pesoBase = 78.2; // biometria.md
  const grasaBase = 18.9; // biometria.md
  const resultados = {};

  // 1. metricas_zepp: sueño/pasos/FC/peso/grasa de los últimos N días, TODO
  // en una fila por día (centralizado — ver HOJAS.METRICAS_ZEPP).
  const hZepp = getHoja_(HOJAS.METRICAS_ZEPP);
  const filasZepp = [];
  for (let i = dias - 1; i >= 0; i--) {
    const fechaStr = formatDate_(new Date(hoy.getTime() - i * 86400000));
    const progreso = (dias - i) / dias; // 0 → 1 a lo largo del periodo
    const sleepScore = 65 + Math.round(Math.random() * 25); // 65-90
    const pasos = 4000 + Math.round(Math.random() * 6000); // 4000-10000
    const hrReposo = 55 + Math.round(Math.random() * 12); // 55-67
    const peso = Math.round((pesoBase + progreso * 1.2 + (Math.random() - 0.5) * 0.4) * 10) / 10;
    const grasa = Math.round((grasaBase - progreso * 0.3 + (Math.random() - 0.5) * 0.3) * 10) / 10;
    filasZepp.push([genId_('ZEP'), fechaStr, sleepScore, pasos, hrReposo, peso, grasa, 'FICTICIO']);
  }
  hZepp.getRange(hZepp.getLastRow() + 1, 1, filasZepp.length, filasZepp[0].length).setValues(filasZepp);
  resultados[HOJAS.METRICAS_ZEPP] = filasZepp.length + ' filas ficticias añadidas';

  // 2. ejercicios_log: series ficticias cada 2 días — SOLO dentro de la
  // ventana de retención real (EJERCICIOS_LOG_RETENCION_DIAS), porque en
  // producción cualquier fila más antigua se borra sola. Generar más días
  // aquí sería simular algo que nunca pasaría de verdad.
  const diasLog = Math.min(dias, EJERCICIOS_LOG_RETENCION_DIAS);
  const hLog = getHoja_(HOJAS.EJERCICIOS_LOG);
  const ejerciciosFicticios = ['EJE_PRESS_HOMB', 'EJE_DOMINADAS', 'EJE_SENTADILLA', 'EJE_REMO_NEUTRO'];
  const filasLog = [];
  for (let i = diasLog - 1; i >= 0; i -= 2) {
    const fecha = new Date(hoy.getTime() - i * 86400000);
    const ejercicio = ejerciciosFicticios[Math.floor(Math.random() * ejerciciosFicticios.length)];
    for (let serie = 1; serie <= 4; serie++) {
      const peso = 20 + Math.round(Math.random() * 60);
      const reps = 6 + Math.round(Math.random() * 6);
      filasLog.push([
        genId_('LOG'), 'FICTICIO', 'FICTICIO', ejercicio,
        serie, peso, reps, 2, 'FICTICIO', fecha.toISOString()
      ]);
    }
  }
  if (filasLog.length > 0) {
    hLog.getRange(hLog.getLastRow() + 1, 1, filasLog.length, filasLog[0].length).setValues(filasLog);
  }
  resultados[HOJAS.EJERCICIOS_LOG] = filasLog.length + ' filas ficticias añadidas (limitado a ' + diasLog + ' días — ventana de retención real)';

  Logger.log(JSON.stringify(resultados, null, 2));
  return { ok: true, detalle: resultados, dias: dias, timestamp: new Date().toISOString() };
}

// ─── §8. LIMPIAR ─────────────────────────────────────────────
// Borra SOLO datos de test/logs. Conserva estructura + planes.
// Los datos que se borran: ejercicios_log, metricas_zepp, metricas_subjetivas
// Los datos que SE CONSERVAN: plan_anual, sesiones_plan, ejercicios_plan, catalogo
// ejercicios_plan NO necesita reset porque NO almacena pesos (son dinámicos).
// Ejecutar manualmente cuando quieras resetear después de testear.

function limpiarDatosTest() {
  const aLimpiar = [HOJAS.EJERCICIOS_LOG, HOJAS.METRICAS_ZEPP, HOJAS.METRICAS_SUBJETIVAS];
  var resultados = {};

  aLimpiar.forEach(function(nombre) {
    var h = getHoja_(nombre);
    if (!h) { resultados[nombre] = 'NO EXISTE'; return; }
    var filas = h.getLastRow();
    if (filas <= 1) { resultados[nombre] = 'YA VACÍA'; return; }
    h.deleteRows(2, filas - 1);
    resultados[nombre] = 'LIMPIA (' + (filas - 1) + ' filas borradas)';
  });

  // Resetear bool_completada en sesiones (como si no hubieras entrenado)
  var hSes = getHoja_(HOJAS.SESIONES_PLAN);
  if (hSes && hSes.getLastRow() > 1) {
    var datos = hSes.getDataRange().getValues();
    var cab = datos[0];
    var colComp = cab.indexOf('bool_completada');
    var colFin = cab.indexOf('date_fin');
    var colIni = cab.indexOf('date_inicio');
    for (var i = 2; i <= datos.length; i++) {
      hSes.getRange(i, colComp + 1).setValue(false);
      hSes.getRange(i, colFin + 1).setValue('');
      if (colIni >= 0) hSes.getRange(i, colIni + 1).setValue('');
    }
    resultados[HOJAS.SESIONES_PLAN] = 'Completadas reseteadas';
  }

  // ejercicios_plan NO necesita reset — no almacena pesos.
  // Los pesos se calculan dinámicamente desde ejercicios_log (que ya se limpió).
  resultados[HOJAS.EJERCICIOS_PLAN] = 'Sin cambios (pesos son dinámicos)';

  Logger.log(JSON.stringify(resultados, null, 2));
  return { ok: true, detalle: resultados, timestamp: new Date().toISOString() };
}

// ─── §9. BIOMETRÍA INICIO/FIN ──────────────────────────────────
// Totalmente AJENO a la app: ninguna otra función lee esta hoja, no tiene
// endpoint, no la toca inicializarHojas() ni limpiarDatosTest(). Es solo un
// checkpoint manual para poder comparar antes/después del plan de 11 meses.
//
// Uso:
//   1. Ejecutar registrarBiometriaCheckpoint() AHORA (antes de empezar el
//      plan, 31/08/2026) — añade la fila "Inicio" con los datos actuales de
//      knowledge_base/usuario/biometria.md.
//   2. Al terminar el plan (11 meses después), tomar las medidas de nuevo
//      (báscula, cinta métrica, pesos actuales de los mismos ejercicios),
//      actualizar los valores de aquí abajo con los datos NUEVOS, y volver
//      a ejecutar la función — añade la fila "Fin" en la MISMA hoja, debajo,
//      para comparar fila a fila.
//
// Los valores de abajo son los de biometria.md §2-5 (medición 18/06/2026) —
// al re-ejecutar para el checkpoint "Fin", sustituir por las medidas reales
// de ese momento (esta función nunca inventa números).
function registrarBiometriaCheckpoint() {
  const NOMBRE_HOJA = 'biometria_checkpoints';
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let hoja = ss.getSheetByName(NOMBRE_HOJA);

  const cabeceras = [
    'fecha', 'etiqueta',
    'peso_kg', 'grasa_pct', 'masa_muscular_kg', 'grasa_visceral', 'agua_pct', 'imc',
    'hombros_cm', 'pecho_cm', 'cintura_cm', 'cadera_cm',
    'biceps_d_cm', 'biceps_i_cm', 'antebrazo_d_cm', 'antebrazo_i_cm',
    'muslo_d_cm', 'muslo_i_cm', 'pantorrilla_d_cm', 'pantorrilla_i_cm',
    'sentadilla_kg', 'sentadilla_reps', 'press_inclinado_kg', 'press_inclinado_reps',
    'rdl_kg', 'rdl_reps', 'dominadas_reps', 'remo_neutro_kg', 'remo_neutro_reps',
    'hip_thrust_kg', 'hip_thrust_reps', 'curl_predicador_kg', 'curl_predicador_reps',
    'kelso_shrug_kg', 'kelso_shrug_reps',
    'vo2max', 'fc_reposo'
  ];

  if (!hoja) {
    hoja = ss.insertSheet(NOMBRE_HOJA);
    hoja.getRange(1, 1, 1, cabeceras.length).setValues([cabeceras]).setFontWeight('bold');
    hoja.setFrozenRows(1);
  }

  // ── Datos de knowledge_base/usuario/biometria.md (medición 18/06/2026) ──
  // Al ejecutar para el checkpoint "Fin", sustituir por las medidas reales
  // tomadas al terminar el plan — NUNCA inventar, siempre desde una medición real.
  const datos = {
    peso_kg: 78.2, grasa_pct: 18.9, masa_muscular_kg: 60.2, grasa_visceral: 9, agua_pct: 55, imc: 22.1,
    hombros_cm: 114, pecho_cm: 94, cintura_cm: 86, cadera_cm: 100,
    biceps_d_cm: 36, biceps_i_cm: 35, antebrazo_d_cm: 27, antebrazo_i_cm: 27,
    muslo_d_cm: 57, muslo_i_cm: 57, pantorrilla_d_cm: 36, pantorrilla_i_cm: 36,
    sentadilla_kg: 80, sentadilla_reps: '?', press_inclinado_kg: 18, press_inclinado_reps: 10,
    rdl_kg: 14, rdl_reps: 12, dominadas_reps: '3-4', remo_neutro_kg: 40, remo_neutro_reps: 10,
    hip_thrust_kg: 20, hip_thrust_reps: 8, curl_predicador_kg: 15, curl_predicador_reps: 12,
    kelso_shrug_kg: 10, kelso_shrug_reps: 15,
    vo2max: 50, fc_reposo: 53
  };

  // Primera fila de datos → "Inicio"; a partir de ahí, "Fin" (o "Checkpoint N"
  // si se ejecuta más de dos veces, aunque el uso previsto es solo 2 veces).
  const filasExistentes = hoja.getLastRow() - 1; // -1 por la cabecera
  let etiqueta;
  if (filasExistentes <= 0) etiqueta = 'Inicio';
  else if (filasExistentes === 1) etiqueta = 'Fin';
  else etiqueta = 'Checkpoint ' + (filasExistentes + 1);

  const fila = [fechaHoy_(), etiqueta].concat(cabeceras.slice(2).map(function(campo) {
    return datos[campo] !== undefined ? datos[campo] : '';
  }));
  hoja.appendRow(fila);

  Logger.log('Checkpoint "' + etiqueta + '" registrado en ' + NOMBRE_HOJA);
  return { ok: true, etiqueta: etiqueta, fila: fila };
}
