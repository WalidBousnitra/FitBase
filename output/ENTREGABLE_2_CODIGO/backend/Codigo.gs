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
      case 'plan_anual':       resultado = getPlanAnual_(); break;
      case 'plan_semanal':     resultado = getPlanSemanal_(e.parameter.semana); break;
      case 'macros_hoy':       resultado = getMacrosHoy_(); break;
      case 'check_ausencia':   resultado = checkAusencia_(); break;
      case 'progresion_metricas': resultado = getProgresionMetricas_(e.parameter.dias); break;
      default: resultado = { error: 'Acción no reconocida' };
    }

    const json = JSON.stringify(resultado);
    if (['sesion_hoy','plan_anual','macros_hoy','progresion_metricas'].includes(accion)) {
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

  // Ejercicios con peso dinámico
  const ejercicios = getEjerciciosSesion_(sesion.sesion_id);
  const ajuste = calcularAjusteDia_();
  const ejerciciosAjustados = ejercicios.map(ej => ({
    ...ej,
    num_peso_sugerido_kg: calcularPesoSugerido_(ej.ejercicio_id, ej.num_peso_sugerido_kg, ajuste.factor),
    ajuste_aplicado: ajuste.factor
  }));

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
 * Fuentes: Mifflin-St Jeor (BMR), Iraki 2019 (bulk), Helms 2014 (cut)
 */
function getMacrosHoy_() {
  const peso = getPesoActual_();
  const altura = 188, edad = 24;
  const bmr = (10 * peso) + (6.25 * altura) - (5 * edad) + 5;
  const tdee = Math.round(bmr * 1.55);

  // Fase actual → objetivo nutricional
  const plan = getPlanAnual_();
  let obj = 'bulk', mult = 1.15, protRatio = 2.0;
  if (plan.fase_actual) {
    const n = plan.fase_actual.str_objetivo_nutri || 'bulk';
    if (n === 'cut') { mult = 0.80; protRatio = 2.4; obj = 'cut'; }
    else if (n === 'mantener') { mult = 1.05; protRatio = 2.0; obj = 'mantener'; }
  }

  const calorias = Math.round(tdee * mult);
  const protG = Math.round(peso * protRatio);
  const grasaG = Math.round(peso * 1.0);
  const carbosG = Math.round((calorias - protG * 4 - grasaG * 9) / 4);

  const sesionHoy = getSesionHoy_();
  const esEntreno = sesionHoy.sesion !== null;
  const agua = Math.round(peso * 35) + (esEntreno ? 500 : 0);
  const pasos = getPasosHoy_();

  return {
    fecha: fechaHoy_(), es_dia_entreno: esEntreno, fase: obj,
    calorias_objetivo: calorias, proteina_g: protG, carbos_g: carbosG, grasas_g: grasaG,
    agua_ml: agua, pasos_objetivo: 8000, pasos_actuales: pasos,
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
  if (diasPerdidos.length > 0) {
    redistribucion = {
      volumen_perdido_series: diasPerdidos.length * 18,
      series_extra_por_sesion: Math.min(4, Math.ceil(diasPerdidos.length * 6)),
      accion: 'Añadir series extra distribuidas en sesiones restantes'
    };
  }

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

  // Motor de cargas: actualizar peso sugerido para PRÓXIMAS sesiones con este ejercicio
  actualizarPesoFuturo_(datos.ejercicio_id, datos.num_peso_usado_kg, datos.num_reps_completadas, datos.num_rir_percibido);
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
  return { ok: true, sesion_id: datos.sesion_id };
}

// ─── §4. MOTOR DE CARGAS ──────────────────────────────────────
// Fuentes: ACSM 2009, Mann 2010 (APRE), Kiviniemi 2007 (FC), Helms 2016 (RIR)
//
// FILOSOFÍA:
// El plan anual define DIRECTRICES (fase, RIR, volumen, foco).
// Los PESOS son siempre dinámicos: se recalculan tras cada serie
// basándose en rendimiento real. Esto hace que el programa se
// adapte automáticamente a la realidad del usuario.

/**
 * Ajuste global del día basado en fatiga/sueño.
 * Adaptado de Kiviniemi 2007 (FC reposo como proxy de HRV).
 */
function calcularAjusteDia_() {
  const hoy = fechaHoy_();
  const metrica = getUltimaFila_(HOJAS.METRICAS_ZEPP, 'date_fecha', hoy);
  var factor = 1.0;
  var razones = [];

  if (metrica) {
    const fcMedia = calcularMediaFC_(10);

    // Kiviniemi 2007: tendencia ascendente 2+ días → recuperación activa
    if (esTendenciaFCAscendente_(2)) {
      factor *= 0.70;
      razones.push('FC ascendente 2+ días (recuperación activa)');
    }
    // Kiviniemi 2007 adaptado: FC reposo elevada +10 vs media
    else if (metrica.num_hr_reposo > fcMedia + 10) {
      factor *= 0.80;
      razones.push('FC reposo elevada (+10 vs media)');
    }

    // Heurístico: sueño pobre
    if (metrica.num_sleep_score && metrica.num_sleep_score < 60) {
      factor *= 0.90;
      razones.push('Sleep score bajo (<60)');
    }
  }

  // Heurístico: estrés subjetivo (si hay registro hoy)
  var subjetiva = getUltimaFila_(HOJAS.METRICAS_SUBJETIVAS, 'date_fecha', hoy);
  if (subjetiva && subjetiva.num_estres > 7) {
    factor *= 0.85;
    razones.push('Estrés subjetivo alto (>7/10)');
  }

  return {
    factor: factor,
    razones: razones.length ? razones : ['Sesión normal'],
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
 * Peso sugerido para un ejercicio — SIEMPRE DINÁMICO.
 *
 * Protocolo APRE simplificado (Mann 2010):
 *   - Reps 13+ en última serie → +5kg
 *   - Reps 8-12 → +2.5kg
 *   - Reps 5-7 → mantener
 *   - Reps ≤4 → -2.5kg
 *
 * Multiplicado por ajuste del día (Kiviniemi 2007).
 * Si no hay historial → devuelve 0 (app muestra "Elige tu peso").
 */
function calcularPesoSugerido_(ejercicioId, pesoBase, ajusteDia) {
  const hoja = getHoja_(HOJAS.EJERCICIOS_LOG);
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];
  const colEj = cab.indexOf('ejercicio_id');

  // Último log de este ejercicio
  var ultimo = null;
  for (let i = datos.length - 1; i >= 1; i--) {
    if (datos[i][colEj] === ejercicioId) {
      ultimo = rowToObj_(cab, datos[i]);
      break;
    }
  }

  // Sin historial → usar peso base del plan (puede ser 0), pero aplicar ajuste del día
  if (!ultimo) return Math.round(pesoBase * ajusteDia * 4) / 4;

  const reps = ultimo.num_reps_completadas;
  const pesoUsado = ultimo.num_peso_usado_kg;
  var nuevoPeso = pesoUsado;

  // APRE simplificado (Mann 2010)
  if (reps >= 13)      nuevoPeso += 5;
  else if (reps >= 8)  nuevoPeso += 2.5;
  else if (reps <= 4)  nuevoPeso = Math.max(0, nuevoPeso - 2.5);
  // 5-7 reps → mantener

  // Aplicar ajuste del día (Kiviniemi 2007)
  nuevoPeso = Math.round(nuevoPeso * ajusteDia * 4) / 4; // Redondear a 0.25kg

  return Math.max(0, nuevoPeso);
}

/**
 * Tras registrar serie: actualizar peso sugerido en ejercicios_plan
 * para TODAS las futuras sesiones con ese ejercicio.
 *
 * Esto hace el plan ADAPTATIVO: el plan anual marca directrices,
 * pero los pesos siempre reflejan el rendimiento real.
 *
 * Criterios (ACSM 2009 + Mann 2010):
 *   - Reps >= 13 → +5kg (muy fácil)
 *   - Reps >= 8 con RIR >= 2 → +2.5kg (bien)
 *   - Reps <= 4 o RIR == 0 → -2.5kg (demasiado pesado)
 *   - Resto → mantener
 */
function actualizarPesoFuturo_(ejercicioId, pesoUsado, reps, rir) {
  var nuevoPeso = pesoUsado;

  if (reps >= 13) nuevoPeso += 5;
  else if (reps >= 8 && rir >= 2) nuevoPeso += 2.5;
  else if (reps <= 4 || rir === 0) nuevoPeso = Math.max(0, nuevoPeso - 2.5);

  nuevoPeso = Math.round(nuevoPeso * 4) / 4; // Redondear a 0.25

  // Obtener sesiones futuras
  const hojaSes = getHoja_(HOJAS.SESIONES_PLAN);
  const sesiones = hojaSes.getDataRange().getValues();
  const cabSes = sesiones[0];
  const hoy = new Date();
  const sesionesFuturas = {};
  for (let i = 1; i < sesiones.length; i++) {
    const f = parseDate_(sesiones[i][cabSes.indexOf('date_fecha')]);
    if (f && f > hoy) sesionesFuturas[sesiones[i][cabSes.indexOf('sesion_id')]] = true;
  }

  // Actualizar peso en ejercicios_plan para todas las instancias futuras
  const hoja = getHoja_(HOJAS.EJERCICIOS_PLAN);
  const datos = hoja.getDataRange().getValues();
  const cab = datos[0];
  const colEj = cab.indexOf('ejercicio_id');
  const colPeso = cab.indexOf('num_peso_sugerido_kg');
  const colSesId = cab.indexOf('sesion_id');

  for (let i = 1; i < datos.length; i++) {
    if (datos[i][colEj] === ejercicioId && sesionesFuturas[datos[i][colSesId]]) {
      hoja.getRange(i + 1, colPeso + 1).setValue(nuevoPeso);
    }
  }
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
  return vals.length ? vals.reduce(function(a, b) { return a + b; }, 0) / vals.length : 53;
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
  if (!hoja) return 78.2;
  const datos = hoja.getDataRange().getValues();
  if (datos.length <= 1) return 78.2;
  const cab = datos[0];
  const colP = cab.indexOf('num_peso_kg');
  for (let i = datos.length - 1; i >= 1; i--) {
    const p = Number(datos[i][colP]);
    if (p > 0) return p;
  }
  return 78.2; // Fallback biometria.md
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
    [HOJAS.EJERCICIOS_PLAN]: ['plan_id','sesion_id','ejercicio_id','num_orden','num_series_plan','num_reps_plan','num_peso_sugerido_kg','num_rir_objetivo','num_descanso_seg','str_notas','bool_es_warmup'],
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
// Los PESOS quedan en 0 — el motor los calcula dinámicamente
// basándose en rendimiento real (APRE Mann 2010).

function rellenarPlanCompleto() {
  // --- FASES (prioridades.md → P1 Estética > P2 Postura > P3 Hipertrofia > P4 Flex) ---
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
  // Formato: [ejercicio_id, nombre, series, reps, descanso_seg, notas]
  const T = {
    PUSH_VOL: [
      ['EJE_PRESS_HOMB','Press hombro mancuernas',4,'8-10',150,'Compuesto hombros'],
      ['EJE_LAT_SENT','Elev. laterales sentado',4,'12-15',90,'P1: V-taper'],
      ['EJE_LAT_POLEA','Elev. laterales polea',3,'12-15',90,''],
      ['EJE_PRESS_INC','Press inclinado mancuernas',4,'8-10',150,'Pecho'],
      ['EJE_CRUCES','Cruces polea alta',3,'10-12',90,''],
      ['EJE_FRANC','Press francés 30°',3,'10-12',120,'Tríceps'],
      ['EJE_EXT_POLEA','Extensión unilateral polea',3,'12-15',60,''],
      ['EJE_FACE_PULL','Face pulls',3,'15-20',60,'P2: Postura']
    ],
    PIERNA_VOL: [
      ['EJE_SENTADILLA','Sentadilla barra',4,'6-8',180,'Compuesto'],
      ['EJE_RDL','RDL',4,'8-10',150,'Isquios+glúteo'],
      ['EJE_HIP_THRUST','Hip thrust',3,'10-12',120,''],
      ['EJE_EXT_QUAD','Extensión cuádriceps',3,'12-15',90,''],
      ['EJE_CURL_FEM','Curl femoral',3,'10-12',90,''],
      ['EJE_HOLLOW','Hollow hold',3,'30s',60,'Core anti-extensión'],
      ['EJE_PALLOF','Press Pallof',3,'12/lado',60,'Core anti-rotación']
    ],
    PULL_VOL: [
      ['EJE_DOMINADAS','Dominadas',4,'6-8',180,'Tirón vertical (P1)'],
      ['EJE_REMO_NEUTRO','Remo neutro polea',4,'8-10',150,''],
      ['EJE_REMO_ROT','Remo unilateral con rotación',3,'10-12',120,''],
      ['EJE_KELSO','Kelso shrug',3,'12-15',90,'P2: Retracción escapular'],
      ['EJE_CURL_Z','Curl Z barra',3,'8-10',90,'Bíceps (P3)'],
      ['EJE_CURL_PRED','Curl predicador',3,'10-12',90,''],
      ['EJE_BAND_PULL','Band pull-aparts',3,'15-20',45,'P2: Postura'],
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
      ['EJE_ROT_EXT','Rotación externa banda',3,'15/lado',45,'P2: Manguito rotador']
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

  // Split semanal FIJO (programacion.md §11):
  // LUN=Push, MAR=Natación, MIE=Pierna, JUE=Natación, VIE=Pull, SAB=Hombros, DOM=Descanso
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
        if (tmpl) {
          for (var oi = 0; oi < tmpl.length; oi++) {
            var ej = tmpl[oi];
            ejN++;
            var planId = 'PLA_' + fStr.replace(/-/g, '') + '_' + String(ejN).padStart(4,'0');
            var series = ej[2];
            if (esDeload) series = Math.max(2, Math.ceil(series * 0.6)); // Bompa 2009: -40% vol
            // RIR por semana en mesociclo (Helms 2016: 4→3→2→deload)
            var rirNum = esDeload ? 5 : (((semFase - 1) % 3 === 0) ? 4 : ((semFase - 1) % 3 === 1) ? 3 : 2);
            filasEj.push([planId, sesId, ej[0], oi+1, series, ej[3], 0, rirNum, ej[4], ej[5], false]);
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
// Los datos que se borran: ejercicios_log, metricas_zepp, peso_log
// Los datos que SE CONSERVAN: plan_anual, plan_semanal, sesiones_plan, ejercicios_plan, catalogo
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

  // Resetear pesos sugeridos a 0 (el motor los recalcula dinámicamente)
  var hEj = getHoja_(HOJAS.EJERCICIOS_PLAN);
  if (hEj && hEj.getLastRow() > 1) {
    var datosEj = hEj.getDataRange().getValues();
    var cabEj = datosEj[0];
    var colPeso = cabEj.indexOf('num_peso_sugerido_kg');
    for (var j = 2; j <= datosEj.length; j++) {
      hEj.getRange(j, colPeso + 1).setValue(0);
    }
    resultados[HOJAS.EJERCICIOS_PLAN] = 'Pesos reseteados a 0 (motor recalcula)';
  }

  Logger.log(JSON.stringify(resultados, null, 2));
  return { ok: true, detalle: resultados, timestamp: new Date().toISOString() };
}
