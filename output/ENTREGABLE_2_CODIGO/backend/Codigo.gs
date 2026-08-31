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
//   §7b. (ELIMINADO) RELLENAR DATOS FICTICIOS — retirado en limpieza 2026
//   §8. LIMPIAR (borrar logs de test, conservar estructura y planes)
//   §9. BIOMETRÍA INICIO/FIN (ajeno a la app — checkpoint manual para
//       comparar antes/después del plan anual, ver knowledge_base/usuario/biometria.md)
// ═══════════════════════════════════════════════════════════════

// ─── §1. CONFIGURACIÓN ────────────────────────────────────────

const HOJAS = {
  // metricas_zepp centraliza TODO lo que se recoge de Health Connect: sueño,
  // pasos, FC reposo, peso y % grasa (antes peso/grasa vivían en peso_log,Ç
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

  // FIX (2026-j): la versión anterior hacía parseDate_(fechaHoy_()) y luego
  // manana.setDate(manana.getDate()+1) — pero parseDate_ interpreta la
  // cadena "yyyy-MM-dd" como medianoche UTC (new Date(str) es UTC para
  // fechas sin hora), mientras que .getDate()/.setDate() operan en el huso
  // POR DEFECTO DEL PROYECTO de Apps Script (no necesariamente Europe/Madrid
  // ni UTC). Si ese huso por defecto no coincide, "mañana" podía calcularse
  // mal (0, 1 o más días desviado según el huso configurado), rompiendo
  // silenciosamente el rango que regenerarSesionesDesde_ usa para decidir
  // qué conservar y qué generar. Ahora se suma 1 día en milisegundos sobre
  // el instante UTC (independiente de cualquier huso local) y se formatea
  // el resultado explícitamente en Europe/Madrid — sin pasar nunca por
  // getDate()/setDate().
  var hoyDate = parseDate_(fechaHoy_());
  var mananaStr = formatDate_(new Date(hoyDate.getTime() + 86400000));
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
      case 'preview_ramadan':  resultado = getRamadanPreview_(); break;
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
 * Busca la fila de sesiones_plan de hoy; si estamos en pre-temporada (antes
 * de plan.fecha_inicio) y hoy es día de gym según el horario, la genera al
 * vuelo (mismas plantillas T[] del plan real). Devuelve null si hoy no es
 * día de gym. Extraído de getSesionHoy_ para poder reutilizarlo desde
 * getVistaMañana_ sin duplicar esta lógica (evitar que ambas funciones
 * puedan divergir sobre qué cuenta como "sesión de hoy").
 */
function buscarSesionEnHojaPorFecha_(fechaStr) {
  const hoja = getHoja_(HOJAS.SESIONES_PLAN);
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];
  for (let i = 1; i < datos.length; i++) {
    const f = parseDate_(datos[i][cab.indexOf('date_fecha')]);
    if (f && formatDate_(f) === fechaStr) return rowToObj_(cab, datos[i]);
  }
  return null;
}

function buscarOGenerarSesionHoy_() {
  const hoy = fechaHoy_();
  let sesion = buscarSesionEnHojaPorFecha_(hoy);
  if (sesion) return sesion;

  // Pre-temporada (antes de plan.fecha_inicio): rellenarPlanCompleto solo
  // genera filas dentro de las fechas de FASES, así que hoy nunca tiene
  // sesión todavía. Si el horario semanal marca hoy como día de gym, se
  // genera AHORA una sesión real (mismas plantillas T[] que usará el plan
  // real, FAS_01 como fase de referencia) para poder probar el flujo
  // completo de entreno + guardado antes de que el plan arranque de verdad.
  // Se marca con sufijo _TEST en el id para poder identificarla y borrarla
  // a mano — no se limpia sola en limpiarDatosTest() (§8 conserva sesiones_plan).
  const plan = getPlanAnual_();
  if (hoy >= plan.fecha_inicio) return null;
  const tipoSesionHoy = getHorarioSemanal_()[diaSemanaMadrid_(new Date())];
  if (!TIPO_DISPLAY[tipoSesionHoy]) return null;

  // LockService: getVistaMañana_ y getSesionHoy_ pueden llegar en paralelo
  // (el splash lanza varias llamadas al backend a la vez) — sin lock, ambas
  // podrían no encontrar sesión todavía y generar CADA UNA su propia fila
  // _TEST duplicada para el mismo día. Se serializa y se vuelve a comprobar
  // dentro del lock por si la otra petición ya la creó mientras esperábamos.
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    sesion = buscarSesionEnHojaPorFecha_(hoy);
    if (!sesion) sesion = generarSesionTestHoy_(hoy, tipoSesionHoy);
    return sesion;
  } finally {
    lock.releaseLock();
  }
}

/**
 * Sesión de hoy con ejercicios y pesos DINÁMICOS (motor de cargas).
 * Fuente: REG-LOG-02 §5, REG-LOG-01 §6-8
 */
function getSesionHoy_() {
  const hoy = fechaHoy_();
  const sesion = buscarOGenerarSesionHoy_();
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

  // Tipo de fase (VOL/FZA/DEF/MNT/DELOAD) para el volumen adaptativo. La hoja
  // guarda el NOMBRE de la fase en str_fase, no el tipo — se resuelve desde el
  // plan anual (o VOL por defecto).
  const tipoFaseSesion = (plan.fase_actual && plan.fase_actual.str_tipo) || 'VOL';

  // Acumulador COMPARTIDO entre todos los ejercicios de esta sesión — ver
  // grupoAltoVolumenId_/ajustarSeriesAdaptativo_ (fix MAV-01): el tope de +2
  // es del GRUPO muscular en la sesión, no de cada ejercicio por separado.
  const bonoGrupoUsado = {};
  const ejerciciosAjustados = ejercicios.map(function(ej) {
    // CAPA MAV — Volumen Máximo Adaptativo (Schoenfeld 2017 dose-response +
    // hipertrofia.md §3). Se auto-regula por RECUPERACIÓN: sube series hacia el
    // techo del grupo cuando la readiness es buena y baja cuando hay fatiga.
    var vol = ajustarSeriesAdaptativo_(ej, {
      tipoFase: tipoFaseSesion,
      semFase: Number(sesion.num_semana_meso) || 1,
      factorDia: ajuste.factor,
      bonoGrupoUsado: bonoGrupoUsado
    });
    var seriesPlan = vol.series;
    // Ramadán (cultura.md §5): -30% sobre el volumen ya ajustado por MAV.
    // FIX (2026-c, auditoría RAM-01): NO aplicar en DELOAD — el deload ya
    // recorta -40% al generar el plan (generarFilasSesiones_); sumar el -30%
    // de Ramadán encima daba ~58% de reducción combinada sin que nadie lo
    // hubiera decidido así. Mismo criterio que ajustarSeriesAdaptativo_ ya
    // usa para MAV ("FZA/DELOAD no se auto-regulan, ya reducido").
    if (ramadan && tipoFaseSesion !== 'DELOAD') seriesPlan = Math.max(1, Math.round(seriesPlan * 0.7));

    var resultado = calcularPesoSugerido_(ej.ejercicio_id, {
      ajusteDia: ajuste.factor,
      fase: sesion.str_fase || 'VOL',
      objetivoNutri: objetivoNutri,
      repsObjetivo: ej.str_reps_plan,
      rirObjetivo: ej.num_rir_objetivo,
      equipamiento: ej.str_equipamiento
    });
    return {
      ...ej,
      num_series_plan: seriesPlan,
      num_series_base: ej.num_series_plan,
      volumen_adaptativo: vol.motivo, // 'vol-progresion' | 'vol-recuperacion' | null
      num_peso_sugerido_kg: resultado.peso,
      motor_detalle: resultado.detalle,
      motor_capas: resultado.capas,
      ajuste_aplicado: ajuste.factor,
      // Doble progresión sin peso (Suelo/Banda/Pared) — ver calcularProgresionReps_.
      sin_peso: resultado.sinPeso === true,
      reps_sugeridas: resultado.sinPeso === true ? resultado.repsSugeridas : null
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
    // Cut: proteína se calcula sobre LBM real más abajo (fix NUT-02) — protRatio
    // no aplica aquí, solo mult (déficit) y obj.
    if (n === 'cut') { mult = 0.80; obj = 'cut'; }
    // Mantener: TDEE×1.0 (motor_dieta.md §4)
    else if (n === 'mantener') { mult = 1.0; protRatio = 2.0; obj = 'mantener'; }
    // Bulk: TDEE×1.15 = +15% (Iraki 2019: rango 1.10-1.20, elegido 1.15 = punto medio)
  }

  var calorias = Math.round(tdee * mult);

  // Ajuste por actividad diaria (motor_dieta.md §6): pasos extra por encima
  // del objetivo de NEAT queman calorías reales no capturadas por el factor
  // de actividad fijo (1.55) — se compensan con carbos extra (van al remainder).
  // NOTA (auditoría NUT-03): motor_dieta.md §6 también sugiere repartir carbos
  // extra pre/post-entreno — la app solo fija el TOTAL diario, sin reparto
  // horario (no hay tracking de comidas para poder aplicarlo). Alcance real,
  // no un hueco pendiente: no hay forma de hacer cumplir un timing intra-día
  // sin una función de registro de comidas que este proyecto no tiene.
  const pasos = getPasosHoy_();
  if (pasos > 12000) calorias += 175; // +150-200 kcal (motor_dieta.md §6), 175 = punto medio

  // FIX (2026-c, auditoría NUT-02): Helms 2014 especifica la proteína de cut
  // en g/kg de MASA MAGRA (2.3-3.1), no peso total — antes se aplicaba un
  // 2.4 g/kg al peso total con un comentario que asumía "~15%BF" fijo, que
  // ya no coincide con el %BF real documentado (18.9%, biometria.md) y nunca
  // se recalculaba si la composición corporal cambia. Ahora se usa el %grasa
  // MÁS RECIENTE de metricas_zepp (getGrasaActual_) para calcular la LBM real.
  var protG;
  if (obj === 'cut') {
    var grasaPctActual = getGrasaActual_();
    var lbm = peso * (1 - grasaPctActual / 100);
    protG = Math.round(lbm * 2.7); // 2.7 g/kg LBM = punto medio Helms 2014 (2.3-3.1)
  } else {
    protG = Math.round(peso * protRatio);
  }
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
  var agua = Math.round(peso * 35) + (esEntreno ? 500 : 0);

  // Pasos objetivo por fase (programacion.md §13, Wilson 2012)
  var pasosPorFase = { VOL: 8000, FZA: 8000, DEF: 10000, MNT: 9000, DELOAD: 7000 };
  var tipoFase = 'VOL';
  if (plan.fase_actual && plan.fase_actual.str_tipo) tipoFase = plan.fase_actual.str_tipo;
  var pasosObj = pasosPorFase[tipoFase] || 8000;

  // FIX (2026-c, auditoría NUT-01): el motor de dieta no tenía NINGUNA rama de
  // Ramadán — cultura.md §8 lo especifica en detalle y es una fecha real del
  // plan (RAMADAN_FECHAS), no algo hipotético. cultura.md §8 NO pide cambiar
  // el total diario de kcal/macros durante Ramadán — solo colapsar la ventana
  // de comidas a Iftar-Suhur, concentrar la proteína en menos tomas, y
  // priorizar hidratación en la ventana nocturna. La app no controla horarios
  // de comida (nutrición es de solo lectura, FatSecret/Health Connect), así
  // que el fix correcto es advisory — igual que ya hace getSesionHoy_() con
  // ramadan_nota — no fabricar un reparto de macros que la app no puede hacer
  // cumplir. Además, la hidratación SÍ es accionable aquí (agua_ml), así que
  // se prioriza subiéndola sobre el mínimo normal.
  var hoy = fechaHoy_();
  var ramadanActivo = esRamadan_(hoy);
  if (ramadanActivo) {
    // Prioridad #1 de cultura.md §8 durante Ramadán: hidratación crítica en
    // la ventana Iftar-Suhur — sube el objetivo un 15% sobre el normal
    // (mismo agua total, concentrada en menos horas de ventana abierta).
    agua = Math.round(agua * 1.15);
  }

  return {
    fecha: fechaHoy_(), es_dia_entreno: esEntreno, fase: obj,
    calorias_objetivo: calorias, proteina_g: protG, carbos_g: carbosG, grasas_g: grasaG,
    agua_ml: agua, pasos_objetivo: pasosObj, pasos_actuales: pasos,
    calorias_consumidas: 0, proteina_consumida_g: 0,
    carbos_consumidos_g: 0, grasas_consumidas_g: 0,
    agua_consumida_ml: 0, bmr: Math.round(bmr), tdee: tdee,
    ramadan_activo: ramadanActivo,
    ramadan_nota: ramadanActivo
      ? 'Ramadán: mismas kcal/macros totales, concentradas entre Iftar y Suhur. Reparte la proteína en 2-3 tomas (Iftar, cena, Suhur) en vez de todo en una — el total diario no cambia, solo cuándo comes. Hidratación crítica: 2-3L entre Iftar y Suhur.'
      : null,
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
  // FIX (2026-c, auditoría SYNC-01): sin clave de idempotencia en guardarLog_,
  // una respuesta HTTP perdida tras un POST que sí tuvo éxito hace que
  // SyncManager reintente y duplique la fila. Un duplicado idéntico no afecta
  // a la mejor-serie (Capa 1 del motor), pero SÍ inflaba en silencio el
  // volumen diario aquí. Se dedupe por (sesion_id, ejercicio_id, num_serie) —
  // esa combinación identifica una serie real única; si aparece dos veces,
  // es un reintento, no dos series distintas.
  const logData = leerDatosDesdeFecha_(HOJAS.EJERCICIOS_LOG, 'date_timestamp', desde, function(row) { return row; });
  const seriesVistas = {};
  const volPorDia = {};
  logData.forEach(function(r) {
    const d = r.date_timestamp ? formatDate_(parseDate_(r.date_timestamp)) : null;
    if (!d) return;
    const claveSerie = r.sesion_id + '|' + r.ejercicio_id + '|' + r.num_serie;
    if (seriesVistas[claveSerie]) return; // reintento duplicado — ya contado
    seriesVistas[claveSerie] = true;
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
  // Gaps cortos: el motor (Capa 5) reduce ×0.95 si un ejercicio se retrasa a
  // 8-9 días. Gaps largos: al superar la retención del log (7 días) no queda base
  // → la app pide "elige tu peso". NO se redistribuye volumen — la evidencia no
  // soporta "series extra compensatorias".

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
  // str_sensacion eliminado: el RIR percibido (0-3) ya codifica la sensación
  // (los 4 botones de la app fijan RIR 3/2/1/0). El motor usa el RIR.
  hoja.appendRow([
    logId, datos.plan_id || '', datos.sesion_id, datos.ejercicio_id,
    datos.num_serie, datos.num_peso_usado_kg, datos.num_reps_completadas,
    datos.num_rir_percibido, new Date().toISOString()
  ]);

  // date_inicio de la sesión = timestamp de la PRIMERA serie registrada (antes
  // quedaba siempre en blanco: no había endpoint de "empezar entreno"). Junto
  // con date_fin (completarSesion_) da la duración real de la sesión.
  if (datos.sesion_id) marcarInicioSesion_(datos.sesion_id);

  // NO se toca ejercicios_plan. El peso se calcula SIEMPRE dinámicamente
  // desde el último log al servir getSesionHoy_() → calcularPesoSugerido_().
  // Esto elimina O(n) escrituras y hace el POST instantáneo.
  limpiarLogsAntiguos_();
  return { ok: true, log_id: logId };
}

/**
 * Marca date_inicio de la sesión con el instante actual si aún está vacío
 * (idempotente: solo la primera serie de la sesión lo escribe; las siguientes
 * lo dejan igual). Fuente del arreglo: date_inicio nunca se rellenaba.
 */
function marcarInicioSesion_(sesionId) {
  const hoja = getHoja_(HOJAS.SESIONES_PLAN);
  if (!hoja) return;
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];
  const colId = cab.indexOf('sesion_id');
  const colIni = cab.indexOf('date_inicio');
  if (colId < 0 || colIni < 0) return;
  for (let i = 1; i < datos.length; i++) {
    if (datos[i][colId] === sesionId) {
      if (!datos[i][colIni]) hoja.getRange(i + 1, colIni + 1).setValue(new Date().toISOString());
      return;
    }
  }
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

    // Imputación por arrastre (carry-forward) de peso y % grasa: la báscula no
    // se usa a diario, pero macros (getMacrosHoy_) y las gráficas de progresión
    // necesitan un valor cada día. Prioridad: dato de hoy en la llamada →
    // dato ya guardado de hoy → último valor conocido de un día anterior. Así
    // ninguna fila queda con el peso/grasa en blanco (biometria.md §11: el peso
    // se sigue por MEDIA — un hueco de un día sin pesada no debe romper la serie).
    var heredado = getUltimoPesoGrasaConocido_(fecha);
    var pesoKg = datos.peso_kg != null ? datos.peso_kg
        : (existente && existente.num_peso_kg !== '' && existente.num_peso_kg != null) ? existente.num_peso_kg
        : (heredado.peso != null ? heredado.peso : '');
    var grasaPct = datos.grasa_pct != null ? datos.grasa_pct
        : (existente && existente.num_grasa_pct !== '' && existente.num_grasa_pct != null) ? existente.num_grasa_pct
        : (heredado.grasa != null ? heredado.grasa : '');

    const actualizado = upsertPorFecha_(hoja, 'date_fecha', fecha, [
      id, fecha, sleepScore, pasos, hrReposo, pesoKg, grasaPct
    ]);
    return { ok: true, metrica_id: id, actualizado: actualizado, peso_heredado: (datos.peso_kg == null && heredado.peso != null) };
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

  // Nunca tocar el pasado: si piden una fecha_inicio anterior a hoy, se
  // recorta a hoy — evita que un rango mal escrito reescriba sesiones ya
  // vividas (y su bool_completada real) como "AUSENCIA".
  var hoyDate = parseDate_(fechaHoy_());
  if (inicio < hoyDate) inicio = hoyDate;

  var diasAusencia = Math.ceil((fin - inicio) / 86400000);

  // Marcar sesiones en rango como no-entrenables (no se borran — motor las ignora)
  var hoja = getHoja_(HOJAS.SESIONES_PLAN);
  var all = hoja.getDataRange().getValues();
  var cab = all[0];
  var colFecha = cab.indexOf('date_fecha');
  var colCompletada = cab.indexOf('bool_completada');
  var sesionesAfectadas = 0;

  for (var i = 1; i < all.length; i++) {
    var f = parseDate_(all[i][colFecha]);
    // Solo sesiones futuras/pendientes (bool_completada aún false) — nunca
    // se sobrescribe una sesión que el usuario ya completó de verdad.
    if (f && f >= inicio && f <= fin && !all[i][colCompletada]) {
      hoja.getRange(i + 1, colCompletada + 1).setValue(true);
      hoja.getRange(i + 1, cab.indexOf('date_fin') + 1).setValue('AUSENCIA');
      sesionesAfectadas++;
    }
  }

  // Determinar impacto según excepciones.md §2.2
  var impacto;
  if (diasAusencia <= 7) {
    impacto = 'Absorción natural (como deload). Al volver, el motor recalcula desde tu último rendimiento.';
  } else if (diasAusencia <= 21) {
    impacto = 'Readaptación: primera semana con RIR+1 (más conservador).';
  } else {
    impacto = 'Ausencia larga: reiniciar mesociclo actual.';
  }

  return {
    ok: true,
    dias_ausencia: diasAusencia,
    sesiones_suspendidas: sesionesAfectadas,
    impacto: impacto,
    // Tras una ausencia larga, ejercicios_log (retención 7 días) ya no tiene
    // base para esos ejercicios → la app muestra "elige tu peso" y tú reintroduces
    // la carga (naturalmente más conservadora). No hay un multiplicador mágico.
    nota: 'Al volver, si el hueco supera la retención del log, la app pedirá elegir el peso de nuevo; si no, el motor ajusta a la baja por el gap (>7d → ×0.95).',
    rutina_casa: getEntrenamientoCasa_()
  };
}

/**
 * Rutina de mantenimiento sin equipamiento de gimnasio (bandas + peso
 * corporal), para usar durante vacaciones/ausencia extendida.
 * Fuente: knowledge_base/reglas/logica/entrenamiento_casa.md §3.
 */
function getEntrenamientoCasa_() {
  return {
    titulo: 'Cuerpo completo — sin equipamiento (mantenimiento, no progresión)',
    duracion_min: 30,
    nota: 'Puente hasta volver al gimnasio — no sustituye el plan real. RIR 2-3, sin buscar el fallo.',
    ejercicios: [
      { nombre: 'Flexiones (push-ups)', reps: '3x AMRAP-2', objetivo: 'Pecho / hombro anterior / tríceps' },
      { nombre: 'Flexión pies elevados (pike/declinada)', reps: '3x8-12', objetivo: 'Hombro (P1: V-taper)' },
      { nombre: 'Remo con banda elástica', reps: '3x12-15', objetivo: 'Espalda media (P2: postura)' },
      { nombre: 'Face pull con banda', reps: '3x15', objetivo: 'Deltoides posterior (P1+P2)' },
      { nombre: 'Zancada / sentadilla búlgara (peso corporal)', reps: '3x12/lado', objetivo: 'Pierna' },
      { nombre: 'Plancha', reps: '3x30-45seg', objetivo: 'Core' }
    ]
  };
}

/**
 * Vista matutina — todo lo que el usuario necesita al despertar.
 * Fuente: programacion.md §12 (Flujo Diario)
 */
function getVistaMañana_() {
  var hoy = fechaHoy_();
  var diaSemana = diaSemanaMadrid_(new Date()); // 0=dom, 1=lun...6=sab

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

  // 4b. Resumen rápido del entreno de hoy (para decidir de un vistazo si hay
  // que darse prisa, sin esperar a abrir el flujo completo de gym). Usa
  // getEjerciciosSesion_ (lectura simple de ejercicios_plan) en vez de
  // getSesionHoy_ completo — evita recalcular el motor de pesos (caro) solo
  // para contar ejercicios.
  var resumenEntreno = null;
  if (tipoDia === 'gym') {
    var sesionHoyRow = buscarOGenerarSesionHoy_();
    if (sesionHoyRow) {
      var ejsHoy = getEjerciciosSesion_(sesionHoyRow.sesion_id);
      resumenEntreno = {
        num_ejercicios: ejsHoy.length,
        duracion_est_min: Number(sesionHoyRow.num_duracion_est_min) || 75,
        cardio_extra_min: cardio.cardio_post_gym_min || 0
      };
    }
  }

  // 5b. Vistazo de MAÑANA (qué toca el día siguiente) — para saber la noche
  // anterior qué mochila preparar (gym/natación/nada) sin esperar a que
  // amanezca. Mismo criterio que el resumen de hoy, pero con la fecha+1.
  var manana = getPreviewManana_(hoy, plan.fases);

  // 6. Movilidad matutina (programacion.md §14, Ruivo 2017, Hansraj 2014)
  var movilidad = getMovilidadMatutina_(plan.fecha_inicio);

  // 6b. Core del día de descanso (recuperación activa) — sube la frecuencia de
  // core a 2x/sem para cumplir el objetivo de abdominales (hipertrofia.md §3).
  var coreDia = getCoreDia_(tipoDia);

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
    // Split del día (Push/Pierna/Pull/Hombros+Brazos) para que la vista de
    // mañana ya diga qué toca hoy, sin esperar a abrir el flujo de gym —
    // así se sabe qué mochila preparar. Mismo TIPO_DISPLAY que usa
    // populateSesionesPlan_/generarSesionTestHoy_, para que coincida
    // exactamente con lo que luego muestra la pantalla de entreno.
    tipo_sesion: tipoDia === 'gym' ? TIPO_DISPLAY[tipoSesionHoy] : null,
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
    resumen_entreno: resumenEntreno,
    manana: manana,
    movilidad_matutina: movilidad,
    core_dia: coreDia,
    aviso_ausencia: ausencia,
    sesion_completada: sesionHoyEstado.completada,
    resumen_hoy: sesionHoyEstado.resumen,
    ramadan: ramadan
  };
}

/**
 * Vistazo de MAÑANA: qué tipo de día toca (gym/natación/descanso) y, si es
 * gym, cuántos ejercicios/duración/cardio extra — para que la noche anterior
 * ya se sepa qué mochila preparar (o si no hace falta preparar nada).
 * No usa el motor de pesos (caro) — solo cuenta ejercicios del plan.
 */
function getPreviewManana_(hoyStr, fases) {
  var mananaDate = sumarDias_(parseDate_(hoyStr), 1);
  var mananaStr = formatDate_(mananaDate);
  var diaSemanaManana = diaSemanaMadrid_(mananaDate);

  var tipoSesionManana = getHorarioSemanal_()[diaSemanaManana];
  var tipoDiaManana;
  if (tipoSesionManana === 'NATACION') tipoDiaManana = 'natacion';
  else if (tipoSesionManana === 'DESCANSO') tipoDiaManana = 'descanso';
  else tipoDiaManana = 'gym';

  var resultado = {
    fecha: mananaStr,
    tipo_dia: tipoDiaManana,
    tipo_sesion: tipoDiaManana === 'gym' ? TIPO_DISPLAY[tipoSesionManana] : null,
    num_ejercicios: null,
    duracion_est_min: null,
    cardio_extra_min: 0
  };
  if (tipoDiaManana !== 'gym') return resultado;

  // Fase de mañana (normalmente la misma que hoy, salvo que hoy sea el
  // último día de la fase actual — string yyyy-MM-dd, mismo patrón que
  // getCambioFase_ usa para comparar fechas de fase).
  var faseManana = null;
  for (var i = 0; i < fases.length; i++) {
    if (mananaStr >= fases[i].date_inicio && mananaStr <= fases[i].date_fin) { faseManana = fases[i]; break; }
  }
  var cardioManana = getCardioObjetivo_(faseManana ? faseManana.str_tipo : 'VOL', tipoDiaManana);
  resultado.cardio_extra_min = cardioManana.cardio_post_gym_min || 0;

  // Si la fila de mañana aún no existe en sesiones_plan (fuera del rango
  // generado), se devuelve igualmente tipo_dia/tipo_sesion — suficiente
  // para saber qué mochila preparar, aunque no el nº exacto de ejercicios.
  var sesionManana = buscarSesionEnHojaPorFecha_(mananaStr);
  if (sesionManana) {
    var ejsManana = getEjerciciosSesion_(sesionManana.sesion_id);
    resultado.num_ejercicios = ejsManana.length;
    resultado.duracion_est_min = Number(sesionManana.num_duracion_est_min) || 75;
  }
  return resultado;
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
 * Previsualización del banner de Ramadán/Eid para el botón de demo de la app
 * (Constants.MOSTRAR_BOTONES_DEMO). Ramadán solo cae ~1 mes/año — sin esto no
 * hay forma de ver el banner en el resto del año. Reutiliza getRamadanInfo_
 * TAL CUAL, solo con una fecha real dentro de RAMADAN_FECHAS en vez de hoy —
 * no inventa contenido nuevo, solo evalúa la lógica real en otra fecha real.
 */
function getRamadanPreview_() {
  return { ramadan: getRamadanInfo_(RAMADAN_FECHAS.inicio) };
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
 *
 * FIX (2026-c, auditoría MOV-01): el código topaba en tramo 3 (semana 12),
 * un tramo menos de lo que este mismo comentario y Ruivo 2017 respaldan —
 * `Math.min(3, ...)` en vez de `Math.min(4, ...)`. Se corrige al tramo que
 * la evidencia realmente permite en vez de recortar el comentario para que
 * coincida con el código: más semanas de progresión real es la opción más
 * óptima dado que Ruivo 2017 la sostiene.
 */
function getMovilidadMatutina_(fechaInicioPlan) {
  var inicio = parseDate_(fechaInicioPlan);
  var semanas = inicio ? Math.floor((new Date() - inicio) / (7 * 86400000)) : 0;
  var tramo = Math.min(4, Math.max(0, Math.floor(semanas / 4)));
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
 * Bloque de core para el día de DESCANSO (recuperación activa).
 *
 * PROBLEMA que resuelve: el core solo se entrenaba 1×/sem (Hollow en el día de
 * Pierna) → ~3 ser/sem, por debajo del objetivo de abdominales 6-10 ser/sem
 * (hipertrofia.md §3). Añadir un bloque de core el domingo lo sube a 2×/sem
 * (~9 ser/sem) SIN alargar las sesiones de gym.
 *
 * EVIDENCIA:
 *   - hipertrofia.md §3: abdominales 6-10 ser/sem, frecuencia 1-2×/sem.
 *   - programacion.md §12 (FLUJO_DESCANSO): el día de descanso es "recuperación
 *     activa" — el core anti-extensión/anti-rotación es de bajo coste sistémico,
 *     no compromete la recuperación.
 *   - P2 Postura (prioridades.md): el trabajo anti-extensión (plancha, hollow,
 *     dead bug) y anti-rotación (Pallof) corrige hiperlordosis / APT
 *     (biometria.md §8: inclinación pélvica anterior SEVERA).
 *
 * Solo peso corporal + banda → se hace en casa, no necesita gimnasio.
 * Devuelve null en días que no son de descanso (el core de gym ya va en Pierna).
 */
function getCoreDia_(tipoDia) {
  if (tipoDia !== 'descanso') return null;
  return {
    titulo: 'Core — recuperación activa',
    duracion_min: 8,
    frecuencia: '2ª sesión de core de la semana (la 1ª es el día de Pierna)',
    justificacion: 'hipertrofia.md §3 (abdominales 6-10 ser/sem) + programacion.md §12 (recuperación activa) + P2 postura (anti-extensión corrige hiperlordosis/APT).',
    ejercicios: [
      { nombre: 'Plancha', reps: '3x40-60s', objetivo: 'Anti-extensión' },
      { nombre: 'Hollow hold', reps: '3x30s', objetivo: 'Anti-extensión' },
      { nombre: 'Press Pallof con banda', reps: '3x12/lado', objetivo: 'Anti-rotación' },
      { nombre: 'Dead bug lento', reps: '3x10/lado', objetivo: 'Control lumbo-pélvico (P2: APT)' }
    ]
  };
}

/**
 * Comprueba si ayer hubo sesión perdida (no abrió la app).
 * Detecta automáticamente según excepciones.md §2.1.
 */
function checkAusenciaAyer_() {
  var ayer = sumarDias_(parseDate_(fechaHoy_()), -1);
  var ayerStr = formatDate_(ayer);
  var diaSemana = diaSemanaMadrid_(ayer);

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
// 4. El AJUSTE DIARIO modula por fatiga (Kiviniemi: FC, sueño).
// 5. El APRE (Mann 2010) define cuánto subir/bajar basado en rendimiento real.

/**
 * Ajuste global del día basado en fatiga/sueño.
 *
 * EVIDENCIA:
 *   - Kiviniemi 2007: FC reposo como proxy de HRV para autorregulación
 *   - Fullagar 2015: sueño afecta rendimiento cognitivo y físico
 *
 * HEURÍSTICAS (marcadas):
 *   - Sleep score < 60 → ×0.90 (no hay paper que defina umbral exacto)
 *
 * NOTA (2026): estrés y energía subjetivos (metricas_subjetivas) se SIGUEN
 * guardando y se muestran en progresión (tracking puro), pero ya NO entran
 * en este cálculo — decisión explícita del usuario: son datos para
 * apuntárselos y mirarlos en retrospectiva, no para que el motor los use
 * para recortar carga automáticamente.
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
    // Umbral y magnitud recalibrados (2026-d, a petición del usuario): el score
    // de esta app es una ESTIMACIÓN (HealthConnectBridge.kt — Health Connect no
    // expone el score real de Zepp), calculada desde duración+fases con una
    // penalización agresiva (×3 por punto de desviación de %profundo/%REM
    // "típico") que puede hundir el número en una noche buena pero con una
    // distribución de fases distinta a la media poblacional. Con <60 → ×0.90,
    // casi cualquier noche imperfecta disparaba un recorte notable. Ahora solo
    // reacciona ante un score claramente malo, y con un castigo menor.
    if (metrica.num_sleep_score && metrica.num_sleep_score < 30) {
      factor *= 0.96;
      razones.push('Sleep score muy bajo <30 (heurístico, Fullagar 2015)');
    }
  }

  // estrés/energía subjetivos (metricas_subjetivas) — retirados del cálculo
  // (2026): se siguen guardando vía guardarMetricasSubjetivas_ y se ven en
  // progresión, pero el motor ya no los usa para recortar carga. Petición
  // explícita del usuario: "solo quiero trackearlo". Esto también deja el
  // stack máximo en FC(×0.80) × sueño(×0.96) = 0.768, así que el antiguo tope
  // de −30% (0.70) ya no puede alcanzarse con las señales que quedan — se
  // quita también para no dejar código muerto (mismo criterio que el fix
  // de la Capa 5 de calcularPesoSugerido_).

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

  // Doble progresión por el OTRO eje (ACSM 2009: sube peso O reps) para
  // ejercicios sin carga externa controlable — ver calcularProgresionReps_.
  if (esProgresionSinPeso_(ejercicioId, ctx.equipamiento)) {
    return calcularProgresionReps_(ejercicioId, ctx);
  }

  // ── CAPA 1: BASE — mejor set de la sesión más reciente ───────
  //
  // FIX (2026): antes se usaba el ÚLTIMO set registrado (el más reciente en la
  // hoja). Con series RECTAS a un RIR objetivo, el último set siempre tiene
  // MENOS reps por fatiga acumulada dentro de la sesión → el motor lo leía como
  // "te has quedado corto" y BAJABA el peso, aunque la sesión hubiera sido
  // perfecta. Resultado: el peso se erosionaba solo (infraentrenamiento).
  //
  // Ahora se toma el MEJOR set (máxima capacidad = reps + RIR) de la sesión
  // MÁS RECIENTE de ese ejercicio. El mejor set refleja la capacidad real de
  // ese día sin penalizar la fatiga normal entre series — que es justo lo que
  // debe guiar la progresión (doble progresión: subes cuando superas el techo
  // del rango). Sigue anclado en Mann 2010 (rendimiento real vs objetivo), pero
  // aplicado a series rectas, no a la 4ª serie AMRAP del APRE original.
  var ultimo = obtenerMejorSetReciente_(ejercicioId);
  if (!ultimo) {
    resultado.detalle = 'Sin historial — elige tu peso';
    return resultado;
  }

  var pesoBase = Number(ultimo.num_peso_usado_kg) || 0;
  if (pesoBase <= 0) {
    resultado.detalle = 'Último peso fue 0 — elige tu peso';
    return resultado;
  }

  var reps = Number(ultimo.num_reps_completadas) || 0;
  var rir  = Number(ultimo.num_rir_percibido);
  if (isNaN(rir)) rir = 2; // fallback si el log no trae RIR

  resultado.capas.base = pesoBase;
  resultado.capas.ultimoReps = reps;
  resultado.capas.ultimoRIR = rir;

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
  // FIX (2026): repsObjetivo llega como STRING de rango ("8-10", "6-8", "30s").
  // Antes se hacía Number("8-10") = NaN → || 10, así que TODO objetivo con
  // rango se comparaba silenciosamente contra 10 reps, corrompiendo el delta
  // APRE en todos los ejercicios de rango bajo/alto. parseRepsObjetivo_ toma
  // el TOPE del rango: ACSM 2009 progresa "al completar 1-2 reps MÁS que el
  // objetivo" — el techo del rango es la meta a superar antes de subir carga.
  var repsObj = parseRepsObjetivo_(ctx.repsObjetivo);
  var rirObj  = Number(ctx.rirObjetivo)  || 2;
  var deltaCap = (reps + rir) - (repsObj + rirObj);

  // Tabla APRE de 5 niveles (Mann 2010) en porcentaje (ACSM 2009: +2-10%).
  // FIX (2026): el umbral de SUBIDA baja de delta≥2 a delta≥1. ACSM 2009:
  // "completas 1-2 reps MÁS que el objetivo → subir 2-10%". Antes hacía falta
  // superar el objetivo por 2 (reps+RIR) para progresar, así que superar el
  // techo del rango por 1 rep no movía la carga → estancamiento/infra-
  // entrenamiento. Ahora superar el objetivo por 1 ya sube (doble progresión
  // real). La banda de "mantener" queda en delta 0 y −1 (justo en el objetivo
  // o 1 por debajo). Las bajadas NO se tocan (protegen de cargar de más).
  var pctAPRE, nivelAPRE;
  if (deltaCap <= -4)      { pctAPRE = -0.10; nivelAPRE = 'muy_pesado';  }
  else if (deltaCap <= -2) { pctAPRE = -0.05; nivelAPRE = 'pesado';      }
  else if (deltaCap <= 0)  { pctAPRE =  0;    nivelAPRE = 'correcto';    }
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
  // ⚠️ HEURÍSTICO: gaps largos implican desentrenamiento parcial (ACSM 2009
  // recomienda 2-3×/sem). OJO — ejercicios_log solo retiene 7 días: un hueco
  // REALMENTE largo hace que ya no exista ningún log de base y el motor devuelve
  // "elige tu peso" (arriba) antes de llegar aquí. Por eso el único tramo que
  // puede dispararse es el de borde: un ejercicio de 1×/sem retrasado a 8-9 días
  // → ×0.95. El antiguo ">14d → ×0.90" era CÓDIGO MUERTO (nunca sobrevive un
  // log de 14 días para dispararlo) y se eliminó (fix 2026).
  var factorDescanso = 1.0;
  var fechaUltimo = parseDate_(ultimo.date_timestamp);
  if (fechaUltimo) {
    var diasDesde = Math.floor((new Date() - fechaUltimo) / 86400000);
    resultado.capas.diasDesdeUltimo = diasDesde;
    if (diasDesde > 7) {
      factorDescanso = 0.95;
      resultado.capas.gapAlerta = '>7d sin este ejercicio → ×0.95';
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
 * Busca el MEJOR set (máx reps+RIR) de la sesión más reciente en la que se
 * registró este ejercicio. Compartido por calcularPesoSugerido_ (progresión
 * por peso) y calcularProgresionReps_ (progresión por reps/segundos).
 * @returns {Object|null} fila de ejercicios_log, o null si no hay historial.
 */
function obtenerMejorSetReciente_(ejercicioId) {
  var hoja = getHoja_(HOJAS.EJERCICIOS_LOG);
  if (!hoja || hoja.getLastRow() <= 1) return null;

  var datos = hoja.getDataRange().getValues();
  var cab = datos[0];
  var colEj = cab.indexOf('ejercicio_id');
  var colSes = cab.indexOf('sesion_id');

  var sesionReciente = null;
  for (var i = datos.length - 1; i >= 1; i--) {
    if (datos[i][colEj] === ejercicioId) { sesionReciente = datos[i][colSes]; break; }
  }
  if (sesionReciente === null) return null;

  var mejor = null, mejorCap = -Infinity;
  for (var j = 1; j < datos.length; j++) {
    if (datos[j][colEj] === ejercicioId && datos[j][colSes] === sesionReciente) {
      var fila = rowToObj_(cab, datos[j]);
      var rCap = (Number(fila.num_reps_completadas) || 0);
      var rrCap = Number(fila.num_rir_percibido); if (isNaN(rrCap)) rrCap = 2;
      if (rCap + rrCap > mejorCap) { mejorCap = rCap + rrCap; mejor = fila; }
    }
  }
  return mejor;
}

/**
 * ¿Este ejercicio progresa por REPS/segundos en vez de por KG?
 *
 * Equipamiento 'Banda'/'Pared' (Band pull-aparts, Rotación externa banda,
 * Wall angels): no hay carga externa controlable, así que "subir peso" no
 * existe como palanca — el 100% de los ejercicios con este equipamiento en
 * el catálogo actual son así, sin excepción.
 *
 * 'Suelo' es AMBIGUO: incluye tanto Hollow hold (peso corporal puro) como
 * Plancha lastrada (con disco encima — sí tiene carga real y kg que subir).
 * No se puede decidir solo por el string de equipamiento, así que se trata
 * como excepción explícita por ID en vez de forzar una nueva columna en el
 * catálogo para un único caso.
 */
function esProgresionSinPeso_(ejercicioId, equipamiento) {
  if (ejercicioId === 'EJE_HOLLOW') return true;
  if (!equipamiento) return false;
  var e = String(equipamiento).toLowerCase();
  return e.indexOf('banda') >= 0 || e.indexOf('pared') >= 0;
}

/**
 * Doble progresión por el eje de REPS/segundos (ACSM 2009: sube peso O
 * reps) para ejercicios sin carga externa controlable.
 *
 * PROBLEMA que resuelve: estos ejercicios siempre se registran con
 * num_peso_usado_kg = 0 (no hay peso que subir). Antes de esto pasaban por
 * calcularPesoSugerido_ igual que cualquier ejercicio con mancuernas/barra,
 * y su Capa 1 (`pesoBase <= 0`) devolvía SIEMPRE "elige tu peso" — nunca
 * progresaban, sesión tras sesión, por muy bien que fueran las series
 * (código muerto para toda esta categoría de ejercicios).
 *
 * Reutiliza la MISMA fórmula delta_capacidad que la Capa 2 (Mann 2010 +
 * ACSM 2009), aplicada a reps/segundos en vez de a kg. Si el objetivo es
 * temporal ("30s"), el paso es de 5s (mismo incremento que
 * getMovilidadMatutina_ usa para escalar hold times).
 */
// FIX (2026-c, auditoría MOTOR-01): esta función solo aplicaba las Capas 1
// (base), 2 (APRE) y 6 (día) del motor — nunca las Capas 3 (fase/deload), 4
// (cut) ni 5 (gap de descanso), que sí tiene el camino CON peso
// (calcularPesoSugerido_). El caso más grave: un ejercicio sin peso podía
// recibir "sube reps" en plena semana de deload (RIR 5-6, cuando todo debería
// sentirse fácil), porque nada comprobaba cfgFase.esDeload. Ahora aplica las
// 6 capas igual que el camino con peso, adaptando cada una a reps/segundos en
// vez de a kg.
function calcularProgresionReps_(ejercicioId, ctx) {
  var unidad = /s\s*$/i.test(String(ctx.repsObjetivo || '').trim()) ? 's' : ' reps';
  var repsObjTope = parseRepsObjetivo_(ctx.repsObjetivo);
  var resultado = { peso: 0, sinPeso: true, repsSugeridas: repsObjTope, detalle: '', capas: { sinPeso: true } };

  var fase = (ctx.fase || 'VOL').toUpperCase();
  var cfgFase = obtenerConfigFase_(fase);

  var ultimo = obtenerMejorSetReciente_(ejercicioId);
  if (!ultimo) {
    resultado.detalle = 'Peso corporal | primer uso — objetivo ' + repsObjTope + unidad;
    return resultado;
  }

  // ── CAPA 3: FASE — deload primero (Bompa 2019), igual que en el camino con
  // peso: en deload todo debe sentirse fácil, nunca "sube reps". Sin carga
  // que reducir, el equivalente correcto es NO progresar — mantener el
  // objetivo plano (RIR 5-6 esa semana ya viene del propio plan, ver §3 de
  // motor_pesos.md, así que ya se siente ligero sin tocar reps).
  if (cfgFase.esDeload) {
    resultado.repsSugeridas = repsObjTope;
    resultado.detalle = 'Peso corporal | DELOAD: mantener ' + repsObjTope + unidad + ' (no progresar, Bompa 2019)';
    resultado.capas.deload = true;
    return resultado;
  }

  var reps = Number(ultimo.num_reps_completadas) || 0;
  var rir = Number(ultimo.num_rir_percibido); if (isNaN(rir)) rir = 2;
  var rirObj = Number(ctx.rirObjetivo) || 2;
  var deltaCap = (reps + rir) - (repsObjTope + rirObj);

  var paso = unidad === 's' ? 5 : 1;
  var ajuste;
  if (deltaCap <= -4)      ajuste = -2 * paso;
  else if (deltaCap <= -2) ajuste = -1 * paso;
  else if (deltaCap <= 0)  ajuste = 0;
  else if (deltaCap <= 3)  ajuste = 1 * paso;
  else                     ajuste = 2 * paso;

  // Cap por fase (Capa 3, igual que pctAPRE en el camino con peso): el % de
  // capSubida/capBajada de cada fase (calibrados sobre el paso ±2×paso de
  // VOL: capSubida 0.05, capBajada 0.10) se traduce a fracción del mismo
  // paso máximo, para que FZA/DEF/MNT sean más/menos agresivos aquí igual
  // que lo son en kg.
  var ajusteMaxSubida = 2 * paso * (cfgFase.capSubida / 0.05);
  var ajusteMaxBajada = 2 * paso * (cfgFase.capBajada / 0.10);
  if (ajuste > 0) ajuste = Math.min(ajuste, ajusteMaxSubida);
  if (ajuste < 0) ajuste = Math.max(ajuste, -ajusteMaxBajada);

  // ── CAPA 4: NUTRICIÓN (Helms 2014) — en cut, recorta la SUBIDA al 50%,
  // igual que el camino con peso: en déficit calórico la recuperación está
  // reducida y no se puede progresar al mismo ritmo.
  if (ajuste > 0 && ctx.objetivoNutri === 'cut') ajuste = ajuste * 0.5;

  var minimo = unidad === 's' ? 10 : 1;
  var repsSugeridas = Math.max(minimo, Math.round(repsObjTope + ajuste));

  // ── CAPA 5: DESCANSO INTER-SESIÓN — mismo criterio que el camino con peso
  // (gap >7 días → ×0.95): un hueco de una semana justifica un pelín menos
  // de exigencia, no tocarlo sería inconsistente entre los dos caminos.
  var factorDescanso = 1.0;
  var fechaUltimo = parseDate_(ultimo.date_timestamp);
  if (fechaUltimo) {
    var diasDesde = Math.floor((new Date() - fechaUltimo) / 86400000);
    if (diasDesde > 7) factorDescanso = 0.95;
  }
  repsSugeridas = Math.max(minimo, Math.round(repsSugeridas * factorDescanso));

  // Capa 6 (Kiviniemi 2007): el factor del día también reduce el objetivo
  // de reps/segundos en un día de mala recuperación.
  var factorDia = Number(ctx.ajusteDia) || 1.0;
  repsSugeridas = Math.max(minimo, Math.round(repsSugeridas * factorDia));

  resultado.repsSugeridas = repsSugeridas;
  resultado.capas.deltaCap = deltaCap;
  resultado.capas.ultimoReps = reps;
  resultado.capas.ultimoRIR = rir;
  resultado.capas.factorDescanso = factorDescanso;
  resultado.capas.factorDia = factorDia;
  resultado.detalle = 'Peso corporal | objetivo ' + repsSugeridas + unidad +
    (ajuste > 0 ? ' (↑ dobla progresión)' : (ajuste < 0 ? ' (↓ ajustado)' : ''));
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

// ─── VOLUMEN MÁXIMO ADAPTATIVO (MAV) ──────────────────────────
// Fuentes: Schoenfeld, Ogborn & Krieger (2017) — relación dosis-respuesta
// (más series/semana = más hipertrofia hasta el techo del grupo) + rangos por
// grupo de hipertrofia.md §3 / programacion.md §3. Bompa 2019 — la fatiga
// acumulada obliga a modular el volumen a la baja (protección del MRV).
//
// PROBLEMA que resuelve: antes el volumen era solo PERIODIZADO (plantillas +
// especialización + deload), nunca se AUTO-REGULABA por recuperación. Ahora sí:
//   · Buena readiness  → progresión de volumen intra-mesociclo hacia el techo.
//   · Mala readiness   → recorte de 1 serie (protege recuperación / MRV).
//
// Grupos con "alto volumen" = los prioritarios con más margen en la evidencia
// (hipertrofia.md §3): Hombros/Espalda 14-18 ser/sem, Bíceps 10-14. Son P1
// (V-taper) y P3. La progresión se aplica SOLO a sus ejercicios de AISLAMIENTO
// (no compuestos): añadir series de aislamiento sube el volumen con bajo coste
// de fatiga sistémica y sin disparar la duración de la sesión (preferencias.md
// §2: ideal 75 min) — los compuestos pesados ya llevan su volumen fijo.
function esGrupoAltoVolumen_(grupo) {
  return grupoAltoVolumenId_(grupo) !== null;
}

// FIX (2026-c, auditoría MAV-01): antes esGrupoAltoVolumen_ solo decía SÍ/NO,
// así que ajustarSeriesAdaptativo_ no tenía forma de agrupar varios ejercicios
// del MISMO grupo bajo un único tope — cada ejercicio de aislamiento elegible
// recibía su propio +1/+2 de forma independiente. Con 4 ejercicios de
// aislamiento de bíceps o de hombros en la plantilla (la norma, no la
// excepción), el "tope +2 sobre la base" que promete el comentario de más
// abajo nunca se cumplía: Bíceps podía llegar a 20 ser/sem (techo real: 14) y
// Hombros a 24 (techo: 18) en cualquier semana normal de buena recuperación.
// grupoAltoVolumenId_ devuelve la clave de agrupación para que el llamador
// (getSesionHoy_) pueda acumular cuánto bono ya se ha repartido a ESE grupo
// en ESTA sesión, y ajustarSeriesAdaptativo_ pueda negarlo una vez agotado.
function grupoAltoVolumenId_(grupo) {
  if (!grupo) return null;
  var g = String(grupo).toLowerCase();
  if (g.indexOf('hombro') === 0) return 'hombros';
  if (g.indexOf('espalda') === 0) return 'espalda';
  if (g.indexOf('bíceps') === 0 || g.indexOf('biceps') === 0) return 'biceps';
  return null;
}

/**
 * Ajuste adaptativo de series para un ejercicio al servir la sesión.
 * @param {Object} ej  Ejercicio del plan (num_series_plan, str_grupo_principal, bool_compuesto)
 * @param {Object} ctx { tipoFase, semFase, factorDia, bonoGrupoUsado }
 *   bonoGrupoUsado: objeto COMPARTIDO entre todas las llamadas de la misma
 *   sesión (mismo objeto pasado por referencia desde getSesionHoy_) — así el
 *   tope de +2 es del GRUPO acumulado en la sesión, no de cada ejercicio.
 * @returns {Object} { series: Number, motivo: String|null }
 */
function ajustarSeriesAdaptativo_(ej, ctx) {
  var base = Number(ej.num_series_plan) || 0;
  var fase = (ctx.tipoFase || 'VOL').toUpperCase();

  // FZA (intensidad, no volumen) y DELOAD (ya reducido) no se auto-regulan.
  if (fase === 'FZA' || fase === 'DELOAD') return { series: base, motivo: null };

  // ── BAJADA: readiness pobre → −1 serie (Bompa 2019: reducir carga de trabajo
  // cuando la fatiga es alta). Umbral 0.80 = mismo punto donde el motor de
  // cargas considera la sesión "reducida" (calcularAjusteDia_). Aplica a TODOS
  // los grupos, no solo prioritarios: proteger la recuperación es transversal.
  // Sin tope conjunto a propósito — varios recortes del mismo grupo en una
  // semana de mala recuperación protegen más, nunca sobrepasan un techo.
  if (ctx.factorDia <= 0.80 && base > 1) {
    return { series: base - 1, motivo: 'vol-recuperacion' };
  }

  // ── SUBIDA: readiness plena (factor 1.0, sin banderas de fatiga) + semana ≥2
  // del mesociclo → progresión de volumen hacia el techo (Schoenfeld 2017).
  // Solo aislamiento de grupos prioritarios. +1 en sem2, +2 desde sem3, tope
  // +2 sobre la base — y ahora ese tope es del GRUPO completo (todos sus
  // ejercicios de aislamiento elegibles suman contra el mismo +2), no de cada
  // ejercicio por separado (evita descontrol de duración y de MRV; el
  // deload/nuevo meso resetea al volver semFase a 1).
  if (ctx.factorDia >= 1.0 && (Number(ctx.semFase) || 1) >= 2
      && ej.bool_compuesto !== true) {
    var grupoId = grupoAltoVolumenId_(ej.str_grupo_principal);
    if (!grupoId) return { series: base, motivo: null };

    var TOPE_BONO_GRUPO = 2;
    var bonoGrupoUsado = ctx.bonoGrupoUsado || {};
    var yaUsado = bonoGrupoUsado[grupoId] || 0;
    var disponible = TOPE_BONO_GRUPO - yaUsado;
    if (disponible <= 0) return { series: base, motivo: null };

    var deseado = Math.min((Number(ctx.semFase) || 1) - 1, 2);
    var extra = Math.min(deseado, disponible);
    if (extra <= 0) return { series: base, motivo: null };

    bonoGrupoUsado[grupoId] = yaUsado + extra;
    return { series: base + extra, motivo: 'vol-progresion' };
  }

  return { series: base, motivo: null };
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

/**
 * Convierte el objetivo de reps (string del plan) a número para el APRE.
 *   "8-10"    → 10   (tope del rango; ACSM 2009: progresas al superar el techo)
 *   "6-8"     → 8
 *   "30s"     → 30   (hold: se compara en segundos, misma lógica de superación)
 *   "45-60s"  → 60
 *   "12"      → 12
 *   number    → tal cual
 * Fallback 10 solo si no hay ningún dígito (no debería ocurrir).
 */
function parseRepsObjetivo_(v) {
  if (typeof v === 'number' && !isNaN(v)) return v;
  if (!v) return 10;
  var nums = String(v).match(/\d+/g);
  if (!nums || !nums.length) return 10;
  return Number(nums[nums.length - 1]); // último número = tope del rango
}

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

/**
 * Sustituye todas las filas de datos (desde la fila 2) de una hoja por
 * `filas`, sin usar deleteRows()/insertRows() — evita el error de Apps
 * Script "You are editing a row/column that is frozen" cuando el usuario
 * ha inmovilizado filas a mano en Google Sheets (Ver > Inmovilizar), algo
 * que la app no controla. deleteRows() sobre una fila fija aborta con
 * excepción a mitad de la operación, dejando el horario/plan nuevo sin
 * escribir (bug real: cambiar el horario semanal guardaba la propiedad
 * pero sesiones_plan se quedaba con los datos viejos, o directamente
 * vacía, porque regenerarSesionesDesde_ moría aquí antes de escribir
 * nada). Solo manipula VALORES de celda (clearContent/setValues), que sí
 * está permitido sobre filas fijas — nunca cambia la estructura de filas.
 */
function reemplazarFilas_(hoja, filas) {
  var filasActuales = hoja.getLastRow() - 1;
  if (filasActuales > 0) {
    hoja.getRange(2, 1, filasActuales, hoja.getLastColumn()).clearContent();
  }
  if (filas.length) {
    hoja.getRange(2, 1, filas.length, filas[0].length).setValues(filas);
  }
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

// FIX (2026-k): día de la semana SIEMPRE en huso Europe/Madrid, nunca vía
// Date.getDay() (que usa el huso POR DEFECTO DEL PROYECTO de Apps Script,
// no necesariamente Europe/Madrid — mismo problema de raíz que el bug de
// guardarHorarioSemanal_ que vació sesiones_plan). Utilities.formatDate(...,'u')
// da 1=Lun..7=Dom (ISO); se remapea a 0=Dom..6=Sáb para igualar la convención
// de Date.getDay() que usa el resto del código (horario[0]=domingo, etc).
function diaSemanaMadrid_(fecha) {
  var iso = parseInt(Utilities.formatDate(fecha, 'Europe/Madrid', 'u'), 10); // 1..7
  return iso % 7; // 7(Dom)→0, 1..6 igual
}

// Suma/resta días por aritmética pura de milisegundos sobre el instante,
// sin pasar nunca por setDate()/getDate() (huso local del proyecto).
function sumarDias_(fecha, n) {
  return new Date(fecha.getTime() + n * 86400000);
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
      ej.bool_compuesto = cat.bool_compuesto === true || cat.bool_compuesto === 'true' || cat.bool_compuesto === 'TRUE';
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
  for (let i = datos.length - 1; i >= 1; i--) {
    const p = Number(datos[i][colP]);
    if (p > 0) return p;
  }
  return 78.2; // Fallback: biometria.md peso actual
}

/**
 * % de grasa corporal más reciente (metricas_zepp), con fallback al dato
 * documentado en biometria.md. Añadida en la auditoría 2026-c (NUT-02) para
 * que getMacrosHoy_() calcule la proteína de cut sobre MASA MAGRA real
 * (Helms 2014 la especifica en g/kg LBM, no peso total) en vez de asumir un
 * %BF fijo en un comentario que no se recalcula si la composición cambia.
 */
function getGrasaActual_() {
  const hoja = getHoja_(HOJAS.METRICAS_ZEPP);
  if (!hoja) return 18.9; // Fallback: biometria.md % grasa actual
  const datos = hoja.getDataRange().getValues();
  if (datos.length <= 1) return 18.9;
  const cab = datos[0];
  const colG = cab.indexOf('num_grasa_pct');
  for (let i = datos.length - 1; i >= 1; i--) {
    const g = Number(datos[i][colG]);
    if (g > 0) return g;
  }
  return 18.9; // Fallback: biometria.md
}

/**
 * Último peso y % grasa conocidos de un día ESTRICTAMENTE anterior a `fecha`
 * (imputación carry-forward de guardarMetricas_). Recorre metricas_zepp de
 * abajo (más reciente) a arriba y devuelve el primer valor no vacío de cada
 * métrica, de forma independiente (el último peso y la última grasa pueden
 * venir de días distintos). Devuelve { peso, grasa } con null si no hay dato.
 */
function getUltimoPesoGrasaConocido_(fecha) {
  const hoja = getHoja_(HOJAS.METRICAS_ZEPP);
  const res = { peso: null, grasa: null };
  if (!hoja) return res;
  const datos = hoja.getDataRange().getValues();
  if (datos.length <= 1) return res;
  const cab = datos[0];
  const colFecha = cab.indexOf('date_fecha');
  const colP = cab.indexOf('num_peso_kg');
  const colG = cab.indexOf('num_grasa_pct');
  for (let i = datos.length - 1; i >= 1; i--) {
    const f = parseDate_(datos[i][colFecha]);
    if (!f || formatDate_(f) >= fecha) continue; // solo días anteriores
    if (res.peso === null) {
      const p = Number(datos[i][colP]);
      if (p > 0) res.peso = p;
    }
    if (res.grasa === null) {
      const g = Number(datos[i][colG]);
      if (g > 0) res.grasa = g;
    }
    if (res.peso !== null && res.grasa !== null) break;
  }
  return res;
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
    // date_sync eliminado (limpieza 2026): su único uso era el centinela
    // 'FICTICIO' de rellenarDatosFicticios() — al retirar los datos de prueba,
    // el timestamp de sync no lo lee ninguna función (date_fecha ya identifica
    // el día). Un solo campo de fecha con utilidad técnica.
    [HOJAS.METRICAS_ZEPP]: ['metrica_id','date_fecha','num_sleep_score','num_pasos','num_hr_reposo','num_peso_kg','num_grasa_pct'],
    [HOJAS.METRICAS_SUBJETIVAS]: ['subjetiva_id','date_fecha','num_energia','num_estres','str_notas'],
    // num_volumen_objetivo eliminado (auditoría 2026-c, DATA-01): estaba
    // hardcodeado a 16 para las 14 fases sin excepción (incluida deload, que
    // en realidad corre a -40%/-58% con Ramadán) — dato fabricado que ninguna
    // función leía jamás (confirmado por grep), viola el mismo principio que
    // ya llevó a limpiar bool_es_warmup/date_creado/str_sensacion. El volumen
    // real vive en ejercicios_plan (num_series_plan) + el ajuste MAV/deload/
    // Ramadán al servir la sesión — no tiene sentido un "objetivo" fijo aquí.
    [HOJAS.PLAN_ANUAL]: ['fase_id','num_año','num_orden','str_nombre_fase','str_tipo','date_inicio','date_fin','num_semanas','str_rir_rango','str_foco_muscular','str_objetivo_nutri','str_notas'],
    // num_ajuste_volumen y date_creado eliminados (limpieza 2026): ninguna
    // función los leía. num_ajuste_volumen siempre valía 1 — la reducción real
    // de volumen ya la aplican el deload (menos series al generar) y Ramadán
    // (-30% al servir); el ajuste diario de readiness modula CARGA, no volumen
    // (motor_pesos.md §5). date_creado (timestamp de generación de la fila) no
    // se leía en ningún sitio. date_inicio SÍ se conserva y ahora se rellena
    // (ver marcarInicioSesion_): date_inicio = 1ª serie registrada, date_fin =
    // sesión completada → duración real de entreno.
    [HOJAS.SESIONES_PLAN]: ['sesion_id','date_fecha','str_tipo','num_semana_meso','str_fase','num_duracion_est_min','bool_completada','date_inicio','date_fin'],
    // bool_es_warmup eliminado (limpieza 2026): siempre false, nunca se leía.
    // El calentamiento se sirve aparte (getCalentamiento_), no vive aquí.
    [HOJAS.EJERCICIOS_PLAN]: ['plan_id','sesion_id','ejercicio_id','num_orden','num_series_plan','str_reps_plan','num_rir_objetivo','num_descanso_seg','str_notas','str_superset_grupo'],
    // str_sensacion eliminado (limpieza 2026): la app solo pide peso/reps/RIR.
    // Los 4 botones (Fácil/Bien/Duro/Fallo) ya fijan el RIR (3/2/1/0) 1:1, así
    // que la sensación era el mismo dato duplicado — el motor usa el RIR.
    [HOJAS.EJERCICIOS_LOG]: ['log_id','plan_id','sesion_id','ejercicio_id','num_serie','num_peso_usado_kg','num_reps_completadas','num_rir_percibido','date_timestamp'],
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
// REDISEÑO DE VOLUMEN (2026-b): auditoría contra hipertrofia.md §3 usando el
// str_grupo_principal REAL del catálogo (rellenarCatalogo_), no una
// clasificación ad-hoc. Hallazgo: Hombros llegaba a 28 ser/sem (Push 14 +
// Hombros 14) — 55-80% POR ENCIMA de su propio techo ya-elevado-por-P1
// (14-18, hipertrofia.md §3). El hombro además es sinergista en Press
// inclinado (pecho) y en Dominadas/Remo (espalda), aparte de sus 2 días
// directos — la fatiga sistémica real es mayor que el recuento de series
// directas. Schoenfeld 2017 documenta el salto grande de <5 a 10+ series,
// pero no valida que duplicar otra vez el volumen (a 28) siga sumando — la
// prioridad (prioridades.md: hombros > pecho, etc.) debe fijar DÓNDE te
// sitúas dentro de tu propio rango, no multiplicar el rango sin límite.
//
// Volumen semanal por grupo tras el rediseño:
//   Hombros: 14-18 ser/sem → Push(10) + Hombros(6) = 16 ✓ (antes 28 — se
//            quitó la redundancia de Press hombro mancuernas y Elev.
//            laterales sentado, ambos repetidos SIN variar ángulo en los
//            2 días; Wakahara 2013: la hipertrofia sigue la variedad de
//            activación, no repetir el idéntico estímulo). Con el techo de
//            MAV (+2 series máx, ajustarSeriesAdaptativo_) el máximo real
//            ahora es 18 — coincide EXACTO con el techo de la evidencia;
//            antes el +2 de MAV partía ya de una base sobre-inflada (28→30).
//   Espalda: 14-18 ser/sem → Pull(15) ✓ (sin cambios, ya en rango).
//            Frecuencia 1x/sem en vez del 2x recomendado — aceptable:
//            Schoenfeld 2019 ("la frecuencia NO afecta la hipertrofia si el
//            volumen está igualado") la trata como herramienta de reparto,
//            no un requisito independiente, y 15 series caben en 1 sesión
//            sin problema de duración (a diferencia de hombros, donde
//            concentrar 16-18 en 1 solo día sí sería inviable en tiempo).
//   Bíceps: 10-14 ser/sem → Pull(6) + Hombros(6) = 12 ✓ (sin cambios).
//   Tríceps: 10-14 ser/sem → Push(6) + Hombros(3) = 9 (sin cambios; recibe
//            además estímulo indirecto real de Press hombro/Press inclinado
//            como sinergista).
//   Pecho: 7 ser/sem (antes 4, por debajo del mínimo efectivo 5 —
//          hipertrofia.md §3) → se reintroduce Cruces polea alta (ya en
//          catálogo, favorito, solo se había quitado por presupuesto de
//          tiempo) ahora que Push libera minutos al perder 1 serie de
//          laterales redundante. Sigue por debajo de 10-14 A PROPÓSITO
//          (prioridades.md: "no priorizar pecho sobre hombros") pero ya
//          cruza el mínimo efectivo.
//   Piernas/Core: Cuádriceps 10 e Isquios 7 sin cambios (ya en rango o
//          cerca, sin prioridad). Core NO se toca aquí — Pierna(Hollow 3) +
//          getCoreDia_() del día de descanso (Hollow+Pallof, 6 contadas) ya
//          suman ~9 ser/sem, dentro de 6-10 (hipertrofia.md §3); añadir
//          Pallof también en Pierna duplicaría el mismo ejercicio en 2 días
//          sin necesidad real.
//   Postura/Manguito (Wall angels, Rotación externa): sin cambios — es
//          trabajo correctivo P2, no volumen de hipertrofia (no le aplican
//          los rangos de Schoenfeld 2017).
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
      // Elev. laterales polea SALE de este día (rediseño 2026-b): se hacía
      // sin variar ángulo en Push Y Hombros (2x/sem el MISMO estímulo) —
      // redundancia sin beneficio extra (Wakahara 2013: la hipertrofia
      // sigue la variedad de activación). Se libera la serie y se
      // reintroduce Cruces polea alta (pecho, favorito del catálogo, solo
      // se había quitado por presupuesto de tiempo — ver comentario arriba).
      ['EJE_CRUCES','Cruces polea alta',3,'12-15',90,'Pecho','SS1'],
      ['EJE_LAT_SENT','Elev. laterales sentado',3,'12-15',90,'P1: V-taper','SS1'],
      ['EJE_FRANC','Press francés 30°',3,'10-12',120,'⚠️ Dolor codo: NO completar extensión total (biometria.md §9)',''],
      ['EJE_EXT_POLEA','Extensión unilateral polea',3,'12-15',90,'⚠️ Dolor codo: rango controlado, NO extensión completa',''],
      ['EJE_FACE_PULL','Face pulls',3,'15-20',90,'P2: Postura','']
    ],
    PIERNA_VOL: [
      ['EJE_SENTADILLA','Sentadilla barra',4,'6-8',180,'Compuesto',''],
      ['EJE_RDL','RDL',4,'8-10',150,'Isquios+glúteo',''],
      ['EJE_HIP_THRUST','Hip thrust',3,'10-12',120,'',''],
      // Cuádriceps (secundario) se quedaba corto (10-12 ser/sem objetivo,
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
      // Press Pallof (core anti-rotación) sigue FUERA de este día — no es un
      // hueco real: getCoreDia_() ya lo cubre en el día de descanso (junto
      // con Hollow, Plancha y Dead bug), sin lo cual el Core total (gym +
      // descanso) ya está en ~9 ser/sem contadas, dentro del objetivo 6-10
      // (hipertrofia.md §3) — añadirlo aquí también duplicaría el mismo
      // ejercicio en 2 días sin necesidad, el mismo tipo de redundancia que
      // se acaba de quitar de Hombros.
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
    // HOMBR_VOL rediseñado (2026-b): fuera Press hombro mancuernas y Elev.
    // laterales sentado — ambos ya se hacen en PUSH_VOL, mismo ejercicio,
    // mismo ángulo, sin variación (Wakahara 2013: la hipertrofia sigue la
    // variedad de activación, no repetir el idéntico estímulo 2x/sem). Este
    // día se queda con lo que aporta ángulo/función distinta: laterales a
    // media altura (trayectoria distinta a sentado), rear delt (Pájaro, no
    // cubierto en Push), brazos y manguito rotador.
    //
    // FIX (2026-c, auditoría DUR-01): el rediseño 2026-b dejó esta sesión en
    // ~39-50min, por debajo del mínimo aceptable de 60min (preferencias.md
    // §2) — se llevó ~20min de Press hombro/Lat sentado sin compensar nada.
    // Arreglo SIN tocar hombros/bíceps (su techo de volumen ya está ajustado):
    //   1. Lat polea y Pájaro salen de la superserie SS1 y su descanso sube de
    //      90 a 120s (sigue dentro del rango aislamiento 90-120s de
    //      hipertrofia.md §6) — descanso real en vez de saltado, y Schoenfeld
    //      2016 (misma evidencia ya citada en el proyecto) muestra que MÁS
    //      descanso da MÁS hipertrofia, así que esto no es solo relleno de
    //      tiempo, es la evidencia empujando en la misma dirección.
    //   2. Rotación externa (Manguito, correctivo P2 — no cuenta contra el
    //      techo de hipertrofia de ningún grupo) sube de 3 a 4 series.
    //   3. Se añade Wall angels (P2: el objetivo postural CONCRETO de
    //      prioridades.md/biometria.md §2) como 2ª exposición semanal —
    //      correctivo, tampoco compite por el presupuesto de series de
    //      hipertrofia, y da más frecuencia al ejercicio que más importa
    //      para el objetivo de postura.
    HOMBR_VOL: [
      ['EJE_LAT_POLEA','Elev. laterales polea (media altura)',3,'12-15',120,'',''],
      ['EJE_PAJARO','Pájaro inclinado',3,'12-15',120,'Rear delt',''],
      ['EJE_ZOTTMAN','Curl Zottman',3,'10-12',90,'Bíceps+Antebrazo','SS2'],
      ['EJE_CURL_INC','Curl inclinado 45°',3,'10-12',90,'','SS2'],
      // Extensión overhead polea EXCLUIDA (biometria.md §9 + seleccion_ejercicios.md
      // §6: rango de estiramiento profundo, alto riesgo para dolor codo) — se
      // sustituye por la variante ya aprobada de rango controlado (ver catálogo).
      ['EJE_EXT_POLEA','Extensión unilateral polea',3,'10-12',90,'⚠️ Dolor codo: rango controlado, NO extensión completa',''],
      ['EJE_ROT_EXT','Rotación externa banda',4,'15/lado',60,'P2: Manguito rotador',''],
      ['EJE_WALL_ANGEL','Wall angels',3,'8-10',60,'P2: Test postural — 2ª exposición/sem','']
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
    // FIX (2026-c, auditoría VOL-01): Bíceps en FZA caía a 7 ser/sem (Curl Z 3
    // + Curl predicador 4) — 30-40% bajo el suelo de evidencia (10-14,
    // hipertrofia.md §3), inconsistente con que Bíceps es la prioridad #2
    // (por encima de Espalda #3, prioridades.md), y sin documentar como
    // trade-off consciente (a diferencia de Pecho). Curl Z y Curl predicador
    // suben al TOPE permitido por ejercicio (5, seleccion_ejercicios.md §4)
    // en vez de añadir un ejercicio nuevo — Bíceps pasa a 10, dentro de
    // rango, sin tocar el resto de la plantilla FZA (menos ejercicios, más
    // carga, es la filosofía correcta de esta fase — Bompa 2019).
    PULL_FZA: [
      ['EJE_DOMINADAS','Dominadas lastradas',5,'4-6',210,'Pesado',''],
      ['EJE_REMO_NEUTRO','Remo neutro',4,'6-8',180,'',''],
      ['EJE_REMO_ROT','Remo unilateral',3,'8-10',150,'',''],
      ['EJE_CURL_Z','Curl Z',5,'6-8',120,'Pesado',''],
      ['EJE_FACE_PULL','Face pulls',2,'15',60,'Postura mantenimiento','']
    ],
    HOMBR_FZA: [
      ['EJE_PRESS_MIL','Press militar barra',4,'5-7',180,'Compuesto pesado',''],
      ['EJE_LAT_POLEA','Elev. laterales polea',4,'10-12',90,'Volumen medial',''],
      ['EJE_CURL_PRED','Curl predicador',5,'6-8',120,'Pesado',''],
      // Mismo criterio que en PUSH_FZA: rango hipertrofia, no fuerza, por el
      // dolor de codo (seleccion_ejercicios.md §6 + biometria.md §9).
      // Tríceps se queda en 6 ser/sem (Francés 3 + Ext. polea 3) A PROPÓSITO
      // — sin subir, a diferencia de Bíceps: son los 2 únicos ejercicios de
      // tríceps con aviso ⚠️codo, y FZA ya implica cargas relativas más altas
      // (aunque en rango de reps hipertrofia). Priorizar la seguridad del
      // codo sobre cerrar el rango de volumen en la fase más pesada del año
      // es la decisión correcta dado el historial del usuario (biometria.md §9).
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
  // FIX (2026-c, auditoría VOL-02): con los 3 bonos de bíceps (Curl Z, Curl
  // predicador, Zottman) a la vez, Bíceps subía a 15 ser/sem — 1 por encima
  // del techo de evidencia (14, hipertrofia.md §3). Se quita el bono de
  // Zottman (Curl Z y Curl predicador ya son los 2 ejercicios "foco" del
  // nombre de la fase); Bíceps queda en 14, justo en el techo.
  'FAS_04': { 'EJE_CURL_Z':1, 'EJE_CURL_PRED':1, 'EJE_FRANC':1, 'EJE_EXT_POLEA':1 }, // Brazos: +bíceps +tríceps
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
      if (diaSemanaMadrid_(fecha) === 0) semFase++;
      fecha = sumarDias_(fecha, 1);
    }

    while (fecha <= hasta) {
      var dia = diaSemanaMadrid_(fecha); // 0=dom, 1=lun...6=sab
      var fStr = Utilities.formatDate(fecha, 'Europe/Madrid', 'yyyy-MM-dd');
      var tipoSesion = horario[dia];

      if (TIPO_DISPLAY[tipoSesion]) {
        sesN++;
        var sesId = 'SES_' + fStr.replace(/-/g, '') + '_' + String(sesN).padStart(3,'0');

        // [sesion_id, date_fecha, str_tipo, num_semana_meso, str_fase,
        //  num_duracion_est_min, bool_completada, date_inicio, date_fin]
        filasSes.push([sesId, fStr, TIPO_DISPLAY[tipoSesion], semFase, fase.nombre, 75, false, '', '']);

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
            // [plan_id, sesion_id, ejercicio_id, num_orden, num_series_plan,
            //  str_reps_plan, num_rir_objetivo, num_descanso_seg, str_notas,
            //  str_superset_grupo]  (bool_es_warmup eliminado)
            filasEj.push([planId, sesId, ej[0], oi+1, series, ej[3], rirNum, ej[4], ej[5], esDeload ? '' : (ej[6] || '')]);
          }
        }
      }

      if (dia === 0) semFase++; // fin de semana → cambio de semana dentro de la fase
      fecha = sumarDias_(fecha, 1);
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
  const filaSes = [sesId, hoy, TIPO_DISPLAY[tipoSesion], semFase, fase.nombre, 75, false, '', ''];
  hojaSes.appendRow(filaSes);

  const tmplKey = getTemplate(fase.tipo, tipoSesion);
  const tmpl = T[tmplKey] || T[tipoSesion + '_VOL'];
  if (tmpl && tmpl.length) {
    const filasEj = tmpl.map(function(ej, oi) {
      return ['PLA_' + fStr + '_T' + String(oi + 1).padStart(2, '0'),
              sesId, ej[0], oi + 1, ej[2], ej[3], rirNum, ej[4], ej[5], ej[6] || ''];
    });
    hojaEj.getRange(hojaEj.getLastRow() + 1, 1, filasEj.length, filasEj[0].length).setValues(filasEj);
  }

  return {
    sesion_id: sesId, date_fecha: hoy, str_tipo: TIPO_DISPLAY[tipoSesion],
    num_semana_meso: semFase, str_fase: fase.nombre,
    num_duracion_est_min: 75, bool_completada: false, date_inicio: '', date_fin: ''
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

  const todasSes = filasSesHistoricas.concat(gen.filasSes);
  const todasEj = filasEjHistoricas.concat(gen.filasEj);

  // SALVAGUARDA: si no hay NADA que escribir (ni historial preservado ni
  // sesiones nuevas generadas), NO tocar la hoja — este resultado siempre es
  // un bug (fecha corrupta, horario vacío/inválido, etc.), nunca un estado
  // válido real: mientras el plan esté vigente SIEMPRE debe generarse al
  // menos una fila futura. Sin esta guarda, reemplazarFilas_ borraría los
  // datos existentes y no escribiría nada — vaciando sesiones_plan entera
  // en silencio (bug real que ya ocurrió).
  if (todasSes.length === 0) {
    throw new Error('regenerarSesionesDesde_: 0 filas resultantes (fechaDesde=' + fechaDesdeStr + ') — abortado para no vaciar sesiones_plan.');
  }

  reemplazarFilas_(hojaSes, todasSes);
  reemplazarFilas_(hojaEj, todasEj);

  return { eliminadas: eliminadas, generadas: gen.filasSes.length };
}

function rellenarPlanCompleto() {
  const hojaPlan = getHoja_(HOJAS.PLAN_ANUAL);
  const hojaSes = getHoja_(HOJAS.SESIONES_PLAN);
  const hojaEj = getHoja_(HOJAS.EJERCICIOS_PLAN);

  // Plan anual. FIX (2026): num_año se derivaba a 2026 fijo para TODAS las
  // fases, pese a que el plan cruza a 2027 (FAS_06 en adelante). Se toma el
  // año real del inicio de cada fase (date_inicio ya es correcto).
  const filasPlan = FASES.map(function(f, i) {
    return [f.id, parseInt(f.inicio.substring(0, 4), 10), i+1, f.nombre, f.tipo, f.inicio, f.fin, f.sem, f.rir, f.foco, f.nutri, ''];
  });
  reemplazarFilas_(hojaPlan, filasPlan);

  // Sesiones + Ejercicios — horario semanal configurable (getHorarioSemanal_,
  // por defecto lun/mié/vie/sáb gym + mar/jue natación + dom descanso).
  const horario = getHorarioSemanal_();
  const inicio = new Date(FASES[0].inicio);
  const fin = new Date(FASES[FASES.length - 1].fin);
  const gen = generarFilasSesiones_(inicio, fin, horario);

  reemplazarFilas_(hojaSes, gen.filasSes);
  reemplazarFilas_(hojaEj, gen.filasEj);

  // Catálogo de ejercicios
  rellenarCatalogo_();

  Logger.log('Plan generado: ' + gen.sesN + ' sesiones, ' + gen.ejN + ' ejercicios');
  return { ok: true, sesiones: gen.sesN, ejercicios: gen.ejN };
}

function rellenarCatalogo_() {
  const hoja = getHoja_(HOJAS.EJERCICIOS_CATALOGO);
  if (!hoja) return;
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
  reemplazarFilas_(hoja, cat);
}

// ─── §7b. (ELIMINADO) RELLENAR DATOS FICTICIOS ───────────────
// Se retiró rellenarDatosFicticios() en la limpieza 2026 junto con la columna
// date_sync (su centinela 'FICTICIO'). El histórico de prueba con pesos/grasa
// inventados chocaba con la política del proyecto (solo valores reales de
// Zepp/HC) y obligaba a mantener un campo de fecha sin utilidad técnica. Para
// probar Progresión/Home con datos reales, sincroniza unos días desde la app.

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
    reemplazarFilas_(h, []);
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
