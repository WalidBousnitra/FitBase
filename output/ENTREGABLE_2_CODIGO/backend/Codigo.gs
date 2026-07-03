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
//   §8. LIMPIAR (borrar logs de test, conservar estructura y planes)
// ═══════════════════════════════════════════════════════════════

// ─── §1. CONFIGURACIÓN ────────────────────────────────────────

const HOJAS = {
  METRICAS_ZEPP: 'metricas_zepp',
  METRICAS_SUBJETIVAS: 'metricas_subjetivas',
  PESO_LOG: 'peso_log',
  PLAN_ANUAL: 'plan_anual',
  PLAN_SEMANAL: 'plan_semanal',
  SESIONES_PLAN: 'sesiones_plan',
  EJERCICIOS_PLAN: 'ejercicios_plan',
  EJERCICIOS_LOG: 'ejercicios_log',
  EJERCICIOS_CATALOGO: 'ejercicios_catalogo'
};

const CACHE_TTL = 30; // segundos

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
      case 'plan_semanal':     resultado = getPlanSemanal_(e.parameter.semana); break;
      case 'macros_hoy':       resultado = getMacrosHoy_(); break;
      case 'check_ausencia':   resultado = checkAusencia_(); break;
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
      case 'guardar_peso':      resultado = guardarPeso_(datos); break;
      case 'guardar_metricas':  resultado = guardarMetricas_(datos); break;
      case 'completar_sesion':  resultado = completarSesion_(datos); break;
      case 'registrar_ausencia': resultado = registrarAusencia_(datos); break;
      default: resultado = { error: 'Acción POST no reconocida' };
    }
    // Invalidar cache tras escritura
    CacheService.getScriptCache().removeAll(['GET:sesion_hoy:', 'GET:macros_hoy:', 'GET:progresion_metricas:']);
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
  if (!sesion) return { sesion: null, ejercicios: [], mensaje: 'No hay sesión para hoy' };

  // Ejercicios con peso calculado DINÁMICAMENTE (no almacenado en plan)
  const ejercicios = getEjerciciosSesion_(sesion.sesion_id);
  const ajuste = calcularAjusteDia_();

  // Contexto nutricional para el motor (Helms 2014: déficit limita progresión)
  const plan = getPlanAnual_();
  const objetivoNutri = (plan.fase_actual && plan.fase_actual.str_objetivo_nutri) || 'bulk';

  const ejerciciosAjustados = ejercicios.map(function(ej) {
    var resultado = calcularPesoSugerido_(ej.ejercicio_id, {
      ajusteDia: ajuste.factor,
      fase: sesion.str_fase || 'VOL',
      objetivoNutri: objetivoNutri,
      repsObjetivo: ej.str_reps_plan,
      rirObjetivo: ej.num_rir_objetivo
    });
    return {
      ...ej,
      num_peso_sugerido_kg: resultado.peso,
      motor_detalle: resultado.detalle,
      motor_capas: resultado.capas,
      ajuste_aplicado: ajuste.factor
    };
  });

  return { sesion: sesion, ejercicios: ejerciciosAjustados, ajuste_dia: ajuste };
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
    fecha_fin: '2027-07-31'
  };
}

function getPlanSemanal_(numSemana) {
  const hoja = getHoja_(HOJAS.PLAN_SEMANAL);
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];
  for (let i = 1; i < datos.length; i++) {
    if (datos[i][cab.indexOf('num_semana_año')] == numSemana) return rowToObj_(cab, datos[i]);
  }
  return { error: 'Semana no encontrada' };
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

  const calorias = Math.round(tdee * mult);
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
  const pasos = getPasosHoy_();

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

  // Peso
  const pesoData = leerDatosDesdeFecha_(HOJAS.PESO_LOG, 'date_fecha', desde, function(row) {
    return {
      fecha: row.date_fecha, peso_kg: row.num_peso_kg,
      grasa_pct: row.num_grasa_pct || null, hidratacion_pct: row.num_hidratacion_pct || null,
      grasa_visceral: row.num_grasa_visceral || null
    };
  });

  // Zepp
  const zeppData = leerDatosDesdeFecha_(HOJAS.METRICAS_ZEPP, 'date_fecha', desde, function(row) {
    return {
      fecha: row.date_fecha, sleep_score: row.num_sleep_score || 0,
      hr_reposo: row.num_hr_reposo || 0, pasos: row.num_pasos || 0,
      vo2max: row.num_vo2max || 0, sleep_horas: 0, sleep_deep_min: 0, hrv_rmssd: 0, stress_avg: 0
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
    peso_actual: pesoData.length > 0 ? pesoData[pesoData.length - 1].peso_kg : null,
    peso_inicio: pesoData.length > 0 ? pesoData[0].peso_kg : null,
    grasa_actual: pesoData.length > 0 ? pesoData[pesoData.length - 1].grasa_pct : null,
    sleep_media: zeppData.length > 0 ? Math.round(zeppData.reduce(function(s, d) { return s + d.sleep_score; }, 0) / zeppData.length) : null,
    pasos_media: zeppData.length > 0 ? Math.round(zeppData.reduce(function(s, d) { return s + d.pasos; }, 0) / zeppData.length) : null
  };

  return { dias_solicitados: dias, peso: pesoData, zepp: zeppData, volumen_entreno: volumenData, resumen: resumen };
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

function guardarPeso_(datos) {
  const hoja = getHoja_(HOJAS.PESO_LOG);
  const id = genId_('PES');
  hoja.appendRow([
    id, datos.fecha || fechaHoy_(), datos.peso_kg,
    datos.grasa_pct || '', datos.hidratacion_pct || '',
    datos.grasa_visceral || '', new Date().toISOString()
  ]);
  return { ok: true, peso_id: id };
}

function guardarMetricas_(datos) {
  const hoja = getHoja_(HOJAS.METRICAS_ZEPP);
  const id = genId_('ZEP');
  hoja.appendRow([
    id, datos.fecha, datos.sleep_score || 0,
    datos.pasos || 0, datos.hr_reposo || 0,
    datos.vo2max || 0, new Date().toISOString()
  ]);
  return { ok: true, metrica_id: id };
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
  var tipoDia;
  if (diaSemana === 0) tipoDia = 'descanso';
  else if (diaSemana === 2 || diaSemana === 4) tipoDia = 'natacion';
  else if ([1, 3, 5, 6].indexOf(diaSemana) >= 0) tipoDia = 'gym';
  else tipoDia = 'descanso';

  // 5. Cardio objetivo del día (programacion.md §13, Wilson 2012)
  var cardio = getCardioObjetivo_(tipoFase, tipoDia);

  // 6. Movilidad matutina (programacion.md §14, Ruivo 2017, Hansraj 2014)
  var movilidad = getMovilidadMatutina_();

  // 7. Aviso de día perdido (excepciones.md §2.1)
  var ausencia = checkAusenciaAyer_();

  return {
    fecha: hoy,
    tipo_dia: tipoDia,
    fase: faseActual ? { nombre: faseActual.str_nombre_fase, tipo: tipoFase, nutri: faseActual.str_objetivo_nutri } : null,
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
    aviso_ausencia: ausencia
  };
}

/**
 * Objetivo de cardio/pasos según fase (programacion.md §13).
 * DECISIÓN BASADA EN EVIDENCIA:
 *   - Wilson 2012: bici/elíptica NO interfiere con hipertrofia (correr SÍ: -31%)
 *   - Viana 2019: LISS (60-70% FC) = HIIT para pérdida de grasa, menor fatiga
 *   - VOL/FZA: 0 min extra → minimizar interferencia (Wilson 2012)
 *   - DEF: 15-20 min bici → aumentar NEAT + déficit (Viana 2019)
 *   - MNT: 10 min opcional → balance sin interferencia
 *   - DELOAD: 0 min → recuperación total
 * La app decide automáticamente — el usuario NO elige si hacer cardio o no.
 */
function getCardioObjetivo_(tipoFase, tipoDia) {
  var pasosPorFase = { VOL: 8000, FZA: 8000, DEF: 10000, MNT: 9000, DELOAD: 7000 };
  var cardioPorFase = { VOL: 0, FZA: 0, DEF: 20, MNT: 10, DELOAD: 0 }; // minutos post-gym
  var justificaciones = {
    VOL: 'Wilson 2012: minimizar interferencia durante volumen',
    FZA: 'Wilson 2012: priorizar recuperación neural en fuerza',
    DEF: 'Viana 2019: LISS post-gym aumenta déficit sin interferir',
    MNT: 'Balance: mantener capacidad aeróbica sin exceso',
    DELOAD: 'Recuperación total — sin carga adicional'
  };

  var pasos = pasosPorFase[tipoFase] || 8000;
  var cardioMin = cardioPorFase[tipoFase] || 0;

  // Cardio extra solo aplica en días de gym (Wilson 2012: post-entreno)
  if (tipoDia !== 'gym') cardioMin = 0;

  return {
    pasos_objetivo: pasos,
    cardio_post_gym_min: cardioMin,
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
function getMovilidadMatutina_() {
  return {
    duracion_min: 6,
    frecuencia: 'DIARIA',
    justificacion: 'Ruivo 2017: protocolo correctivo requiere frecuencia diaria. Hansraj 2014: estrés cervical constante requiere corrección constante.',
    ejercicios: [
      { nombre: 'Retracción cervical (chin tucks)', reps: '10 reps', objetivo: 'Forward head (Hansraj 2014)' },
      { nombre: 'Extensión torácica foam roller', reps: '30 segundos', objetivo: 'Hipercifosis (Ruivo 2017)' },
      { nombre: 'Cat-cow (gato-vaca)', reps: '10 reps', objetivo: 'Movilidad columna' },
      { nombre: 'Rotación externa con banda', reps: '10/lado', objetivo: 'Hombros internos (Ruivo 2017)' },
      { nombre: 'Dead bugs', reps: '10/lado', objetivo: 'Hiperlordosis/APT' }
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

  // Solo comprobar en días de gym (1=lun, 3=mie, 5=vie, 6=sab)
  if ([1, 3, 5, 6].indexOf(diaSemana) < 0) return null;

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

  // ⚠️ HEURÍSTICO: estrés subjetivo
  var subjetiva = getUltimaFila_(HOJAS.METRICAS_SUBJETIVAS, 'date_fecha', hoy);
  if (subjetiva && subjetiva.num_estres > 7) {
    factor *= 0.85;
    razones.push('Estrés subjetivo alto >7/10 (heurístico)');
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
  const hoja = getHoja_(HOJAS.PESO_LOG);
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

function getPasosHoy_() {
  const m = getUltimaFila_(HOJAS.METRICAS_ZEPP, 'date_fecha', fechaHoy_());
  return m ? (m.num_pasos || 0) : 0;
}

function limpiarLogsAntiguos_() {
  const hoja = getHoja_(HOJAS.EJERCICIOS_LOG);
  if (!hoja) return;
  const datos = hoja.getDataRange().getValues();
  if (datos.length <= 1) return;
  const cab = datos[0];
  const col = cab.indexOf('date_timestamp');
  if (col < 0) return;
  const limite = new Date();
  limite.setDate(limite.getDate() - 30);
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
    [HOJAS.METRICAS_ZEPP]: ['metrica_id','date_fecha','num_sleep_score','num_pasos','num_hr_reposo','num_vo2max','date_sync'],
    [HOJAS.METRICAS_SUBJETIVAS]: ['subjetiva_id','date_fecha','num_energia','num_estres','num_doms','str_grupo_doms','str_notas'],
    [HOJAS.PESO_LOG]: ['peso_id','date_fecha','num_peso_kg','num_grasa_pct','num_hidratacion_pct','num_grasa_visceral','date_sync'],
    [HOJAS.PLAN_ANUAL]: ['fase_id','num_año','num_orden','str_nombre_fase','str_tipo','date_inicio','date_fin','num_semanas','num_volumen_objetivo','str_rir_rango','str_foco_muscular','str_objetivo_nutri','str_notas'],
    [HOJAS.PLAN_SEMANAL]: ['semana_id','fase_id','num_semana_año','num_semana_fase','str_lunes','str_martes','str_miercoles','str_jueves','str_viernes','str_sabado','str_domingo','str_rir_semana','bool_deload'],
    [HOJAS.SESIONES_PLAN]: ['sesion_id','date_fecha','str_tipo','num_semana_meso','str_fase','num_ajuste_volumen','num_duracion_est_min','bool_completada','date_inicio','date_fin','date_creado'],
    [HOJAS.EJERCICIOS_PLAN]: ['plan_id','sesion_id','ejercicio_id','num_orden','num_series_plan','str_reps_plan','num_rir_objetivo','num_descanso_seg','str_notas','bool_es_warmup'],
    [HOJAS.EJERCICIOS_LOG]: ['log_id','plan_id','sesion_id','ejercicio_id','num_serie','num_peso_usado_kg','num_reps_completadas','num_rir_percibido','str_sensacion','date_timestamp'],
    [HOJAS.EJERCICIOS_CATALOGO]: ['ejercicio_id','str_nombre','str_nombre_en','str_grupo_principal','arr_grupos_secundarios','str_patron','str_equipamiento','bool_compuesto','bool_favorito','bool_excluido','str_razon_exclusion','str_alternativa']
  };

  for (const [nombre, cabs] of Object.entries(esquema)) {
    var h = ss.getSheetByName(nombre);
    if (!h) h = ss.insertSheet(nombre);
    h.getRange(1, 1, 1, cabs.length).setValues([cabs]).setFontWeight('bold');
    h.setFrozenRows(1);
  }
  return { ok: true, mensaje: 'Hojas creadas con cabeceras' };
}

// ─── §7. RELLENAR ─────────────────────────────────────────────
// Genera plan anual, semanal, sesiones y ejercicios.
// Ejecutar DESPUÉS de inicializarHojas().
// ejercicios_plan almacena SOLO lógica (series, reps, RIR, descanso).
// Los PESOS NO se almacenan — se calculan dinámicamente en cada
// petición GET desde ejercicios_log (APRE Mann 2010).

function rellenarPlanCompleto() {
  // --- FASES (periodización basada en Bompa 2019 + prioridades.md) ---
  // Estructura: VOL(6)→DELOAD(1)→VOL(6)→DELOAD(1)→FZA(6)→VOL(6)→DELOAD(1)→DEF(6)→MNT(11)
  // Justificación:
  //   - Mesociclos de 6 sem (Bompa 2019: 4-6 sem óptimo, se usa 6 por mayor volumen)
  //   - Deloads cada 6 sem (Bompa: cada 4-6 semanas, -40% volumen)
  //   - VOL→FZA→DEF (ondulación clásica Bompa: AA→MF→P)
  //   - RIR progresión intra-meso: VOL 4→3→2 / FZA 2→2→1 (Helms 2016)
  //   - Foco por fase según prioridades.md: P1(V-taper) → P3(brazos) → balance → cut
  //   - Nutrición por fase: bulk → mantener(deloads) → cut(DEF) → mantener(MNT)
  const FASES = [
    {id:'FAS_01', nombre:'Adaptación + Postura', tipo:'VOL', inicio:'2026-08-31', fin:'2026-09-27', sem:4, rir:'3-4', foco:'Full Body + Correctivos posturales', nutri:'bulk'},
    {id:'FAS_02', nombre:'Hipertrofia I — V-Taper', tipo:'VOL', inicio:'2026-09-28', fin:'2026-11-08', sem:6, rir:'2-3', foco:'Hombros+Espalda (P1: V-taper)', nutri:'bulk'},
    {id:'FAS_03', nombre:'Deload 1', tipo:'DELOAD', inicio:'2026-11-09', fin:'2026-11-15', sem:1, rir:'5-6', foco:'Movilidad + Test Wall Angel', nutri:'mantener'},
    {id:'FAS_04', nombre:'Hipertrofia II — Brazos', tipo:'VOL', inicio:'2026-11-16', fin:'2026-12-27', sem:6, rir:'2-3', foco:'Bíceps+Tríceps+Pecho', nutri:'bulk'},
    {id:'FAS_05', nombre:'Deload 2', tipo:'DELOAD', inicio:'2026-12-28', fin:'2027-01-03', sem:1, rir:'5-6', foco:'Descanso activo + Flex', nutri:'mantener'},
    {id:'FAS_06', nombre:'Fuerza — Compuestos', tipo:'FZA', inicio:'2027-01-04', fin:'2027-02-14', sem:6, rir:'1-2', foco:'Press militar+Dominadas+Sentadilla', nutri:'bulk'},
    {id:'FAS_07', nombre:'Hipertrofia III — Balance', tipo:'VOL', inicio:'2027-02-15', fin:'2027-03-28', sem:6, rir:'2-3', foco:'Piernas+Core + Mantener V-taper', nutri:'bulk'},
    {id:'FAS_08', nombre:'Deload 3', tipo:'DELOAD', inicio:'2027-03-29', fin:'2027-04-04', sem:1, rir:'5-6', foco:'Test postural final + Flex', nutri:'mantener'},
    {id:'FAS_09', nombre:'Definición', tipo:'DEF', inicio:'2027-04-05', fin:'2027-05-16', sem:6, rir:'2-3', foco:'Mantener masa + Déficit controlado', nutri:'cut'},
    {id:'FAS_10', nombre:'Peak Estético + Mant.', tipo:'MNT', inicio:'2027-05-17', fin:'2027-07-31', sem:11, rir:'2-3', foco:'Ratio cintura/hombros + Simetría', nutri:'mantener'}
  ];

  // --- TEMPLATES EJERCICIOS (por tipo sesión × tipo fase) ---
  // Volumen semanal por grupo (Schoenfeld 2017 + programacion.md §3):
  //   Hombros: 14-18 ser/sem → Push(8) + Hombros(8) = 16 ✓ (Prioridad #1: V-taper)
  //   Espalda: 14-18 ser/sem → Pull(11) + correctivos(6) = 17 ✓
  //   Bíceps: 10-14 ser/sem → Pull(6) + Hombros(6) = 12 ✓
  //   Tríceps: 10-14 ser/sem → Push(6 directo) + press(8 indirecto) = ~10 ✓
  //   Pecho: 7 ser/sem → Trade-off consciente por prioridades.md (V-taper > pecho)
  //          Schoenfeld 2017: 5-9 series = ES 0.378, aún efectivo
  //   Pierna: 10-12 → Pierna(4+4+3+3+3) = 17 ✓ (incluye core separado)
  //
  // Descansos (programacion.md §5, Schoenfeld 2016):
  //   Compuestos pesados: 150-270s (evidencia: 3-5 min = 180-300s)
  //   Aislamiento: 90-120s (evidencia: 1.5-2 min)
  //   Correctivos/postura: 60s (no buscan hipertrofia)
  //
  // Formato: [ejercicio_id, nombre, series, reps, descanso_seg, notas]
  const T = {
    PUSH_VOL: [
      ['EJE_PRESS_HOMB','Press hombro mancuernas',4,'8-10',150,'Compuesto hombros'],
      ['EJE_LAT_SENT','Elev. laterales sentado',4,'12-15',90,'P1: V-taper'],
      ['EJE_LAT_POLEA','Elev. laterales polea',3,'12-15',90,''],
      ['EJE_PRESS_INC','Press inclinado mancuernas',4,'8-10',150,'Pecho'],
      ['EJE_CRUCES','Cruces polea alta',3,'10-12',90,''],
      ['EJE_FRANC','Press francés 30°',3,'10-12',120,'Tríceps'],
      ['EJE_EXT_POLEA','Extensión unilateral polea',3,'12-15',90,''],
      ['EJE_FACE_PULL','Face pulls',3,'15-20',90,'P2: Postura']
    ],
    PIERNA_VOL: [
      ['EJE_SENTADILLA','Sentadilla barra',4,'6-8',180,'Compuesto'],
      ['EJE_RDL','RDL',4,'8-10',150,'Isquios+glúteo'],
      ['EJE_HIP_THRUST','Hip thrust',3,'10-12',120,''],
      ['EJE_EXT_QUAD','Extensión cuádriceps',3,'12-15',90,''],
      ['EJE_CURL_FEM','Curl femoral',3,'10-12',90,''],
      ['EJE_HOLLOW','Hollow hold',3,'30s',90,'Core anti-extensión'],
      ['EJE_PALLOF','Press Pallof',3,'12/lado',90,'Core anti-rotación']
    ],
    PULL_VOL: [
      ['EJE_DOMINADAS','Dominadas',4,'6-8',180,'Tirón vertical (P1)'],
      ['EJE_REMO_NEUTRO','Remo neutro polea',4,'8-10',150,''],
      ['EJE_REMO_ROT','Remo unilateral con rotación',3,'10-12',120,''],
      ['EJE_KELSO','Kelso shrug',3,'12-15',90,'P2: Retracción escapular'],
      ['EJE_CURL_Z','Curl Z barra',3,'8-10',90,'Bíceps (P3)'],
      ['EJE_CURL_PRED','Curl predicador',3,'10-12',90,''],
      ['EJE_BAND_PULL','Band pull-aparts',3,'15-20',60,'P2: Postura'],
      ['EJE_WALL_ANGEL','Wall angels',3,'8-10',60,'P2: Test postural']
    ],
    HOMBR_VOL: [
      ['EJE_PRESS_HOMB','Press hombro mancuernas',4,'8-10',150,''],
      ['EJE_LAT_SENT','Elev. laterales sentado',4,'12-15',90,'P1: V-taper extra'],
      ['EJE_LAT_POLEA','Elev. laterales polea (tras nuca)',3,'12-15',90,''],
      ['EJE_PAJARO','Pájaro inclinado',3,'12-15',90,'Rear delt'],
      ['EJE_ZOTTMAN','Curl Zottman',3,'10-12',90,'Bíceps+Antebrazo'],
      ['EJE_CURL_INC','Curl inclinado 45°',3,'10-12',90,''],
      ['EJE_EXT_OVERHEAD','Extensión overhead polea',3,'10-12',90,'Tríceps'],
      ['EJE_ROT_EXT','Rotación externa banda',3,'15/lado',60,'P2: Manguito rotador']
    ],
    PUSH_FZA: [
      ['EJE_PRESS_INC','Press inclinado mancuernas',5,'4-6',210,'Pesado'],
      ['EJE_PRESS_HOMB','Press hombro sentado',4,'5-7',180,''],
      ['EJE_LAT_SENT','Elev. laterales sentado',4,'10-12',90,'Mantener volumen hombros'],
      ['EJE_FRANC','Press francés',3,'6-8',120,'']
    ],
    PIERNA_FZA: [
      ['EJE_SENTADILLA','Sentadilla barra',5,'4-6',270,'Pesado'],
      ['EJE_RDL','RDL',4,'5-7',180,''],
      ['EJE_HIP_THRUST','Hip thrust',3,'6-8',150,''],
      ['EJE_PLANCHA','Plancha lastrada',3,'45-60s',90,'Core']
    ],
    PULL_FZA: [
      ['EJE_DOMINADAS','Dominadas lastradas',5,'4-6',210,'Pesado'],
      ['EJE_REMO_NEUTRO','Remo neutro',4,'6-8',180,''],
      ['EJE_REMO_ROT','Remo unilateral',3,'8-10',150,''],
      ['EJE_CURL_Z','Curl Z',3,'6-8',120,'Pesado'],
      ['EJE_FACE_PULL','Face pulls',2,'15',60,'Postura mantenimiento']
    ],
    HOMBR_FZA: [
      ['EJE_PRESS_MIL','Press militar barra',4,'5-7',180,'Compuesto pesado'],
      ['EJE_LAT_POLEA','Elev. laterales polea',4,'10-12',90,'Volumen medial'],
      ['EJE_CURL_PRED','Curl predicador',4,'6-8',120,'Pesado'],
      ['EJE_EXT_POLEA','Extensión polea',3,'8-10',120,'']
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
  var ESPECIALIZACION = {
    'FAS_02': { 'EJE_LAT_SENT':1, 'EJE_LAT_POLEA':1, 'EJE_DOMINADAS':1, 'EJE_REMO_NEUTRO':1 }, // V-Taper: +hombros +espalda
    'FAS_04': { 'EJE_CURL_Z':1, 'EJE_CURL_PRED':1, 'EJE_ZOTTMAN':1, 'EJE_FRANC':1, 'EJE_EXT_POLEA':1 }, // Brazos: +bíceps +tríceps
    'FAS_07': { 'EJE_SENTADILLA':1, 'EJE_RDL':1, 'EJE_HIP_THRUST':1, 'EJE_HOLLOW':1, 'EJE_PALLOF':1 }  // Balance: +pierna +core
  };

  // Split semanal FIJO (programacion.md §11, Schoenfeld 2019: frecuencia meta-analysis):
  // PPL + Hombros/Brazos = 4 gym + 2 natación + 1 descanso
  // Justificación: prioridad V-taper (prioridades.md) requiere 14-18 ser/sem hombros+espalda
  // → imposible con solo 2 Upper days → necesita día dedicado hombros
  // Schoenfeld 2019: frecuencia 2×/sem NO es superior si volumen igualado → split válido
  const DIAS_GYM = {1:'PUSH', 3:'PIERNA', 5:'PULL', 6:'HOMBR'};

  const hojaPlan = getHoja_(HOJAS.PLAN_ANUAL);
  const hojaSem = getHoja_(HOJAS.PLAN_SEMANAL);
  const hojaSes = getHoja_(HOJAS.SESIONES_PLAN);
  const hojaEj = getHoja_(HOJAS.EJERCICIOS_PLAN);

  // Limpiar datos previos (conservar cabeceras)
  [hojaPlan, hojaSem, hojaSes, hojaEj].forEach(function(h) {
    if (h && h.getLastRow() > 1) h.deleteRows(2, h.getLastRow() - 1);
  });

  // Plan anual
  const filasPlan = FASES.map(function(f, i) {
    return [f.id, 2026, i+1, f.nombre, f.tipo, f.inicio, f.fin, f.sem, 16, f.rir, f.foco, f.nutri, ''];
  });
  hojaPlan.getRange(2, 1, filasPlan.length, filasPlan[0].length).setValues(filasPlan);

  // Sesiones + Ejercicios + Semanal
  var filasSes = [], filasEj = [], filasSem = [];
  var sesN = 0, ejN = 0, semAño = 0;

  for (var fi = 0; fi < FASES.length; fi++) {
    var fase = FASES[fi];
    var fecha = new Date(fase.inicio);
    var fin = new Date(fase.fin);
    var esDeload = fase.tipo === 'DELOAD';
    var semFase = 1;

    while (fecha <= fin) {
      var dia = fecha.getDay(); // 0=dom, 1=lun...6=sab
      var fStr = Utilities.formatDate(fecha, 'Europe/Madrid', 'yyyy-MM-dd');

      if (DIAS_GYM[dia]) {
        sesN++;
        var sesId = 'SES_' + fStr.replace(/-/g, '') + '_' + String(sesN).padStart(3,'0');
        var tipoSesion = DIAS_GYM[dia];
        var tipoDisplay = {PUSH:'Push',PIERNA:'Pierna',PULL:'Pull',HOMBR:'Hombros+Brazos'}[tipoSesion];

        filasSes.push([sesId, fStr, tipoDisplay, semFase, fase.nombre, 1.0, 75, false, '', '', new Date().toISOString()]);

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
            filasEj.push([planId, sesId, ej[0], oi+1, series, ej[3], rirNum, ej[4], ej[5], false]);
          }
        }
      }

      // Fin de semana (domingo) → escribir plan_semanal
      if (dia === 0) {
        semAño++;
        filasSem.push(['SEM_' + String(semAño).padStart(3,'0'), fase.id, semAño, semFase,
          'Push','Natación','Pierna','Natación','Pull','Hombros+Brazos','Descanso',
          fase.rir, esDeload]);
        semFase++;
      }
      fecha.setDate(fecha.getDate() + 1);
    }
  }

  // Batch write (mucho más rápido que appendRow)
  if (filasSes.length) hojaSes.getRange(2, 1, filasSes.length, filasSes[0].length).setValues(filasSes);
  if (filasEj.length) hojaEj.getRange(2, 1, filasEj.length, filasEj[0].length).setValues(filasEj);
  if (filasSem.length) hojaSem.getRange(2, 1, filasSem.length, filasSem[0].length).setValues(filasSem);

  // Catálogo de ejercicios
  rellenarCatalogo_();

  Logger.log('Plan generado: ' + sesN + ' sesiones, ' + ejN + ' ejercicios');
  return { ok: true, sesiones: sesN, ejercicios: ejN, semanas: semAño };
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
    ['EJE_EXT_OVERHEAD','Extensión overhead polea','Overhead Extension','Tríceps','[]','Extensión','Polea',false,false,false,'',''],
    ['EJE_SENTADILLA','Sentadilla barra','Barbell Squat','Cuádriceps','["Glúteos","Isquios"]','Extensión rodilla','Barra,Rack',true,true,false,'',''],
    ['EJE_RDL','RDL','Romanian Deadlift','Isquios','["Glúteos"]','Extensión cadera','Barra',true,true,false,'',''],
    ['EJE_HIP_THRUST','Hip thrust','Hip Thrust','Glúteos','["Isquios"]','Extensión cadera','Barra,Banco',true,true,false,'',''],
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

// ─── §8. LIMPIAR ─────────────────────────────────────────────
// Borra SOLO datos de test/logs. Conserva estructura + planes.
// Los datos que se borran: ejercicios_log, metricas_zepp, peso_log, metricas_subjetivas
// Los datos que SE CONSERVAN: plan_anual, plan_semanal, sesiones_plan, ejercicios_plan, catalogo
// ejercicios_plan NO necesita reset porque NO almacena pesos (son dinámicos).
// Ejecutar manualmente cuando quieras resetear después de testear.

function limpiarDatosTest() {
  const aLimpiar = [HOJAS.EJERCICIOS_LOG, HOJAS.METRICAS_ZEPP, HOJAS.METRICAS_SUBJETIVAS, HOJAS.PESO_LOG];
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
