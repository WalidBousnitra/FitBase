// ═══════════════════════════════════════════════════════════════
// FITBASE - BACKEND (Google Apps Script)
// Archivo: Codigo.gs
// Fuente: REG-LOG-02 (base_datos.md), REG-LOG-01 (motor_pesos.md)
// ═══════════════════════════════════════════════════════════════

// ─── CONFIGURACIÓN ────────────────────────────────────────────
// Script container-bound: ya está asociado al Spreadsheet (creado desde Extensiones → Apps Script)
// No necesita SPREADSHEET_ID — usa getActiveSpreadsheet() directamente.

const USER_ID = 'USR_001';

// Nombres de hojas (REG-LOG-02)
const HOJAS = {
  USUARIOS: 'usuarios',
  METRICAS_ZEPP: 'metricas_zepp',
  PESO_LOG: 'peso_log',
  SESIONES_PLAN: 'sesiones_plan',
  EJERCICIOS_PLAN: 'ejercicios_plan',
  EJERCICIOS_LOG: 'ejercicios_log',
  PROGRESION_LOG: 'progresion_log',
  COMIDAS_LOG: 'comidas_log',
  HIDRATACION_LOG: 'hidratacion_log',
  SUPLEMENTOS_LOG: 'suplementos_log',
  EXCEPCIONES_LOG: 'excepciones_log',
  PLAN_ANUAL: 'plan_anual',
  PLAN_SEMANAL: 'plan_semanal',
  EJERCICIOS_CATALOGO: 'ejercicios_catalogo',
  MEDICIONES_LOG: 'mediciones_log'
};

// ─── ENDPOINTS REST ───────────────────────────────────────────

/**
 * Maneja peticiones GET.
 * Rutas:
 *   ?accion=sesion_hoy
 *   ?accion=plan_anual
 *   ?accion=plan_semanal&semana=25
 *   ?accion=metricas_hoy
 *   ?accion=progresion&ejercicio_id=XXX
 *   ?accion=catalogo
 *   ?accion=macros_hoy
 *   ?accion=check_ausencia (detecta días sin abrir app)
 *   ?accion=composicion_semanal (última medición Zepp semanal)
 */
function doGet(e) {
  try {
    const accion = e.parameter.accion;
    let resultado;

    switch (accion) {
      case 'sesion_hoy':
        resultado = getSesionHoy();
        break;
      case 'plan_anual':
        resultado = getPlanAnual();
        break;
      case 'plan_semanal':
        resultado = getPlanSemanal(e.parameter.semana);
        break;
      case 'metricas_hoy':
        resultado = getMetricasHoy();
        break;
      case 'progresion':
        resultado = getProgresion(e.parameter.ejercicio_id);
        break;
      case 'catalogo':
        resultado = getCatalogo();
        break;
      case 'macros_hoy':
        resultado = getMacrosHoy();
        break;
      case 'check_ausencia':
        resultado = checkAusencia();
        break;
      case 'composicion_semanal':
        resultado = getComposicionSemanal();
        break;
      case 'sync_fatsecret':
        resultado = syncFatSecret();
        break;
      default:
        resultado = { error: 'Acción no reconocida', acciones_validas: ['sesion_hoy', 'plan_anual', 'plan_semanal', 'metricas_hoy', 'progresion', 'catalogo', 'macros_hoy', 'check_ausencia', 'composicion_semanal', 'sync_fatsecret'] };
    }

    return ContentService.createTextOutput(JSON.stringify(resultado))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({ error: error.message }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Maneja peticiones POST.
 * Rutas:
 *   accion=guardar_log (ejercicio completado)
 *   accion=guardar_peso
 *   accion=guardar_metricas
 *   accion=guardar_comida
 *   accion=guardar_suplementos
 *   accion=completar_sesion
 *   accion=guardar_excepcion
 */
function doPost(e) {
  try {
    const datos = JSON.parse(e.postData.contents);
    const accion = datos.accion;
    let resultado;

    switch (accion) {
      case 'guardar_log':
        resultado = guardarEjercicioLog(datos);
        break;
      case 'guardar_peso':
        resultado = guardarPeso(datos);
        break;
      case 'guardar_metricas':
        resultado = guardarMetricas(datos);
        break;
      case 'guardar_comida':
        resultado = guardarComida(datos);
        break;
      case 'guardar_suplementos':
        resultado = guardarSuplemento(datos);
        break;
      case 'completar_sesion':
        resultado = completarSesion(datos);
        break;
      case 'guardar_excepcion':
        resultado = guardarExcepcion(datos);
        break;
      case 'registrar_ausencia':
        resultado = registrarAusenciaExtendida(datos);
        break;
      case 'subir_peso_manual':
        resultado = subirPesoManual(datos);
        break;
      case 'guardar_medicion':
        resultado = guardarMedicion(datos);
        break;
      default:
        resultado = { error: 'Acción POST no reconocida' };
    }

    return ContentService.createTextOutput(JSON.stringify(resultado))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({ error: error.message }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

// ─── SERVICIOS GET ────────────────────────────────────────────

/**
 * Obtiene la sesión planificada para hoy.
 * Incluye ejercicios con peso sugerido por el motor de cargas.
 */
function getSesionHoy() {
  const hoy = Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd');
  const hoja = getHoja(HOJAS.SESIONES_PLAN);
  const datos = hoja.getDataRange().getValues();
  const cabeceras = datos[0];

  // Buscar sesión de hoy
  let sesion = null;
  for (let i = 1; i < datos.length; i++) {
    const fechaFila = Utilities.formatDate(new Date(datos[i][cabeceras.indexOf('date_fecha')]), 'Europe/Madrid', 'yyyy-MM-dd');
    if (fechaFila === hoy && datos[i][cabeceras.indexOf('user_id')] === USER_ID) {
      sesion = filaAObjeto(cabeceras, datos[i]);
      break;
    }
  }

  if (!sesion) {
    return { sesion: null, mensaje: 'No hay sesión planificada para hoy' };
  }

  // Obtener ejercicios de la sesión
  const ejercicios = getEjerciciosDeSesion(sesion.sesion_id);

  // Aplicar motor de cargas (REG-LOG-01)
  const ajuste = calcularAjusteDia();
  const ejerciciosAjustados = ejercicios.map(ej => {
    return {
      ...ej,
      num_peso_sugerido_kg: calcularPesoSugerido(ej.ejercicio_id, ej.num_peso_sugerido_kg),
      ajuste_aplicado: ajuste.factor
    };
  });

  return {
    sesion: sesion,
    ejercicios: ejerciciosAjustados,
    ajuste_dia: ajuste
  };
}

/**
 * Obtiene el plan anual completo con fases.
 */
function getPlanAnual() {
  const hoja = getHoja(HOJAS.PLAN_ANUAL);
  const datos = hoja.getDataRange().getValues();
  const cabeceras = datos[0];

  const fases = [];
  for (let i = 1; i < datos.length; i++) {
    if (datos[i][cabeceras.indexOf('user_id')] === USER_ID) {
      fases.push(filaAObjeto(cabeceras, datos[i]));
    }
  }

  // Determinar fase actual
  const hoy = new Date();
  let faseActual = null;
  for (const fase of fases) {
    const inicio = new Date(fase.date_inicio);
    const fin = new Date(fase.date_fin);
    if (hoy >= inicio && hoy <= fin) {
      faseActual = fase;
      break;
    }
  }

  return {
    fases: fases,
    fase_actual: faseActual,
    total_semanas: 48,
    fecha_inicio: '2026-08-31',
    fecha_fin: '2027-07-31'
  };
}

/**
 * Obtiene el plan semanal (microciclo).
 */
function getPlanSemanal(numSemana) {
  const hoja = getHoja(HOJAS.PLAN_SEMANAL);
  const datos = hoja.getDataRange().getValues();
  const cabeceras = datos[0];

  for (let i = 1; i < datos.length; i++) {
    if (datos[i][cabeceras.indexOf('num_semana_año')] == numSemana &&
        datos[i][cabeceras.indexOf('user_id')] === USER_ID) {
      return filaAObjeto(cabeceras, datos[i]);
    }
  }
  return { error: 'Semana no encontrada' };
}

/**
 * Obtiene métricas del día (sueño, peso, readiness).
 */
function getMetricasHoy() {
  const hoy = Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd');

  // Métricas Zepp
  const metricasZepp = getUltimaMetrica(HOJAS.METRICAS_ZEPP, hoy);

  // Peso
  const pesoHoy = getUltimaMetrica(HOJAS.PESO_LOG, hoy);

  // Media móvil peso 7 días
  const mediaPeso7d = calcularMediaPeso(7);

  return {
    metricas_zepp: metricasZepp,
    peso: pesoHoy,
    peso_media_7d: mediaPeso7d,
    fecha: hoy
  };
}

/**
 * Obtiene historial de progresión de un ejercicio.
 */
function getProgresion(ejercicioId) {
  const hoja = getHoja(HOJAS.PROGRESION_LOG);
  const datos = hoja.getDataRange().getValues();
  const cabeceras = datos[0];

  const registros = [];
  for (let i = 1; i < datos.length; i++) {
    if (datos[i][cabeceras.indexOf('ejercicio_id')] === ejercicioId &&
        datos[i][cabeceras.indexOf('user_id')] === USER_ID) {
      registros.push(filaAObjeto(cabeceras, datos[i]));
    }
  }

  return {
    ejercicio_id: ejercicioId,
    registros: registros,
    total: registros.length
  };
}

/**
 * Obtiene el catálogo completo de ejercicios.
 */
function getCatalogo() {
  const hoja = getHoja(HOJAS.EJERCICIOS_CATALOGO);
  const datos = hoja.getDataRange().getValues();
  const cabeceras = datos[0];

  const catalogo = [];
  for (let i = 1; i < datos.length; i++) {
    catalogo.push(filaAObjeto(cabeceras, datos[i]));
  }
  return { ejercicios: catalogo };
}

/**
 * Calcula macros objetivo del día actual.
 * Fuente: Motor de dieta (REG-NUT-01), Mifflin 1990, Iraki 2019
 */
function getMacrosHoy() {
  const hoy = Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd');
  const sesionHoy = getSesionHoy();
  const esTraining = sesionHoy.sesion !== null;

  // Datos fijos usuario (biometria.md)
  const peso = 78.2;
  const altura = 188;
  const edad = 24;

  // BMR Mifflin-St Jeor (EVI-11)
  const bmr = (10 * peso) + (6.25 * altura) - (5 * edad) + 5; // 1842

  // TDEE
  const factorActividad = 1.55;
  const tdee = Math.round(bmr * factorActividad); // 2855

  // Fase actual para determinar objetivo nutricional
  const planAnual = getPlanAnual();
  const faseActual = planAnual.fase_actual;
  let objetivoNutri = 'bulk'; // default
  if (faseActual) {
    objetivoNutri = faseActual.str_objetivo_nutri || 'bulk';
  }

  // Ajuste calórico según fase (Iraki 2019)
  let caloriasObjetivo;
  switch (objetivoNutri) {
    case 'bulk':
      caloriasObjetivo = Math.round(tdee * 1.15); // +15%
      break;
    case 'cut':
      caloriasObjetivo = Math.round(tdee * 0.80); // -20%
      break;
    default:
      caloriasObjetivo = tdee;
  }

  // Macros (Iraki 2019, Helms 2014)
  const proteinaG = Math.round(peso * 2.0); // 2.0 g/kg en bulk
  const grasaG = Math.round(peso * 1.0);    // 1.0 g/kg
  const calsProt = proteinaG * 4;
  const calsGrasa = grasaG * 9;
  const carbosG = Math.round((caloriasObjetivo - calsProt - calsGrasa) / 4);

  // Objetivo agua (35ml/kg + 500ml si entrena)
  const aguaBase = Math.round(peso * 35);
  const aguaObjetivo = esTraining ? aguaBase + 500 : aguaBase;

  return {
    fecha: hoy,
    es_dia_entreno: esTraining,
    fase: objetivoNutri,
    calorias_objetivo: caloriasObjetivo,
    proteina_g: proteinaG,
    carbos_g: carbosG,
    grasas_g: grasaG,
    agua_ml: aguaObjetivo,
    bmr: Math.round(bmr),
    tdee: tdee
  };
}

// ─── SERVICIOS POST ───────────────────────────────────────────

/**
 * Guarda el log de una serie completada.
 * Alimenta el motor de progresión (ACSM 2009 — REG-LOG-01).
 */
function guardarEjercicioLog(datos) {
  const hoja = getHoja(HOJAS.EJERCICIOS_LOG);
  const logId = generarId('LOG');

  const fila = [
    logId,
    datos.plan_id || '',
    datos.sesion_id,
    datos.ejercicio_id,
    datos.num_serie,
    datos.num_peso_usado_kg,
    datos.num_reps_completadas,
    datos.num_rir_percibido,
    10 - datos.num_rir_percibido, // RPE = 10 - RIR
    datos.str_sensacion || 'bien',
    datos.str_notas || '',
    datos.bool_dolor || false,
    datos.str_zona_dolor || '',
    new Date().toISOString()
  ];

  hoja.appendRow(fila);

  // Actualizar progresión
  actualizarProgresion(datos.ejercicio_id, datos.num_peso_usado_kg, datos.num_reps_completadas);

  return { ok: true, log_id: logId };
}

/**
 * Guarda medición de peso.
 */
function guardarPeso(datos) {
  const hoja = getHoja(HOJAS.PESO_LOG);
  const pesoId = generarId('PES');

  const fila = [
    pesoId,
    USER_ID,
    datos.fecha || Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd'),
    datos.peso_kg,
    datos.grasa_pct || '',
    datos.musculo_kg || '',
    datos.fuente || 'manual',
    datos.condiciones || '',
    new Date().toISOString()
  ];

  hoja.appendRow(fila);
  return { ok: true, peso_id: pesoId };
}

/**
 * Guarda métricas de Zepp/Health Connect.
 */
function guardarMetricas(datos) {
  const hoja = getHoja(HOJAS.METRICAS_ZEPP);
  const metricaId = generarId('ZEP');

  const fila = [
    metricaId,
    USER_ID,
    datos.fecha,
    datos.sleep_score || 0,
    datos.sleep_horas || 0,
    datos.sleep_deep_min || 0,
    datos.sleep_rem_min || 0,
    datos.hrv_rmssd || 0,
    datos.hr_reposo || 0,
    datos.readiness || 0,
    datos.stress_avg || 0,
    datos.pasos_ayer || 0,
    datos.calorias_activas || 0,
    new Date().toISOString()
  ];

  hoja.appendRow(fila);
  return { ok: true, metrica_id: metricaId };
}

/**
 * Guarda una comida en el log nutricional.
 */
function guardarComida(datos) {
  const hoja = getHoja(HOJAS.COMIDAS_LOG);
  const comidaId = generarId('COM');

  const fila = [
    comidaId,
    USER_ID,
    datos.fecha || Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd'),
    datos.tipo_comida, // desayuno/comida/cena/snack
    datos.calorias,
    datos.proteina_g,
    datos.carbos_g,
    datos.grasas_g,
    datos.pre_entreno || false,
    datos.post_entreno || false,
    datos.notas || '',
    new Date().toISOString()
  ];

  hoja.appendRow(fila);
  return { ok: true, comida_id: comidaId };
}

/**
 * Guarda adherencia a suplementos del día.
 */
function guardarSuplemento(datos) {
  const hoja = getHoja(HOJAS.SUPLEMENTOS_LOG);
  const suppId = generarId('SUP');

  const fila = [
    suppId,
    USER_ID,
    datos.fecha || Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd'),
    datos.whey || false,
    datos.caseina || false,
    datos.vitd_k || false,
    datos.omega3 || false,
    datos.magnesio || false,
    datos.ashwagandha || false,
    datos.cromo || false,
    datos.notas || ''
  ];

  hoja.appendRow(fila);
  return { ok: true, supp_id: suppId };
}

/**
 * Marca sesión como completada.
 */
function completarSesion(datos) {
  const hoja = getHoja(HOJAS.SESIONES_PLAN);
  const allDatos = hoja.getDataRange().getValues();
  const cabeceras = allDatos[0];
  const colSesionId = cabeceras.indexOf('sesion_id');
  const colCompletada = cabeceras.indexOf('bool_completada');
  const colFin = cabeceras.indexOf('date_fin');

  for (let i = 1; i < allDatos.length; i++) {
    if (allDatos[i][colSesionId] === datos.sesion_id) {
      hoja.getRange(i + 1, colCompletada + 1).setValue(true);
      hoja.getRange(i + 1, colFin + 1).setValue(new Date().toISOString());
      break;
    }
  }

  return { ok: true, sesion_id: datos.sesion_id };
}

/**
 * Guarda una excepción (viaje, enfermedad, lesión).
 */
function guardarExcepcion(datos) {
  const hoja = getHoja(HOJAS.EXCEPCIONES_LOG);
  const excId = generarId('EXC');

  const fila = [
    excId,
    USER_ID,
    datos.tipo, // viaje/enfermedad/lesion/ramadan/estres
    datos.fecha_inicio,
    datos.fecha_fin || '',
    datos.detalles || '',
    datos.zona_afectada || '',
    datos.severidad || '',
    true // activa
  ];

  hoja.appendRow(fila);
  return { ok: true, exc_id: excId };
}

// ─── DETECCIÓN AUSENCIA (abre app y no entrenó) ──────────────

/**
 * Detecta días sin abrir la app (sesiones no completadas).
 * Si el usuario no abrió la app en todo el día → asumir que no entrenó.
 * Al día siguiente se notifica y se redistribuye volumen semanal.
 */
function checkAusencia() {
  const hoy = new Date();
  const hoja = getHoja(HOJAS.SESIONES_PLAN);
  const datos = hoja.getDataRange().getValues();
  const cabeceras = datos[0];
  const colFecha = cabeceras.indexOf('date_fecha');
  const colCompletada = cabeceras.indexOf('bool_completada');
  const colUserId = cabeceras.indexOf('user_id');

  const diasPerdidos = [];
  const hace7dias = new Date(hoy.getTime() - 7 * 24 * 60 * 60 * 1000);

  for (let i = 1; i < datos.length; i++) {
    if (datos[i][colUserId] !== USER_ID) continue;
    const fechaFila = new Date(datos[i][colFecha]);
    if (fechaFila >= hace7dias && fechaFila < hoy && !datos[i][colCompletada]) {
      diasPerdidos.push({
        fecha: Utilities.formatDate(fechaFila, 'Europe/Madrid', 'yyyy-MM-dd'),
        tipo: datos[i][cabeceras.indexOf('str_tipo')],
        sesion_id: datos[i][cabeceras.indexOf('sesion_id')]
      });
    }
  }

  // Calcular redistribución si hay días perdidos esta semana
  let redistribucion = null;
  if (diasPerdidos.length > 0) {
    redistribucion = calcularRedistribucion(diasPerdidos);
  }

  return {
    dias_perdidos: diasPerdidos,
    total_perdidos: diasPerdidos.length,
    redistribucion: redistribucion,
    mensaje: diasPerdidos.length > 0
      ? `No entrenaste ${diasPerdidos.length} día(s). Se redistribuye volumen.`
      : 'Todo al día'
  };
}

/**
 * Calcula redistribución del volumen perdido en los días restantes de la semana.
 * Fuente: Schoenfeld 2019 — volumen semanal total es lo que importa.
 */
function calcularRedistribucion(diasPerdidos) {
  // Volumen perdido estimado por tipo de sesión
  const volPorTipo = { 'Push': 16, 'Pull': 16, 'Pierna': 14, 'Upper': 12 };
  let volumenPerdido = 0;
  diasPerdidos.forEach(d => { volumenPerdido += volPorTipo[d.tipo] || 12; });

  return {
    volumen_perdido_series: volumenPerdido,
    accion: 'Añadir series extra distribuidas en sesiones restantes de la semana',
    series_extra_por_sesion: Math.min(Math.ceil(volumenPerdido / 3), 4) // Máx +4 series/sesión
  };
}

/**
 * Registra ausencia extendida (≥1 semana) con opción de redistribuir plan.
 * El usuario indica manualmente fechas de inicio/fin.
 */
function registrarAusenciaExtendida(datos) {
  // Guardar como excepción
  const excResult = guardarExcepcion({
    tipo: datos.tipo || 'ausencia',
    fecha_inicio: datos.fecha_inicio,
    fecha_fin: datos.fecha_fin,
    detalles: datos.motivo || 'Ausencia extendida',
    severidad: datos.severidad || 'media'
  });

  // Recalcular plan: mover fechas de sesiones pendientes
  const diasAusencia = Math.ceil(
    (new Date(datos.fecha_fin) - new Date(datos.fecha_inicio)) / (1000 * 60 * 60 * 24)
  );

  // Ajustar plan post-ausencia
  const ajuste = {
    semana_readaptacion: diasAusencia >= 7,
    reduccion_volumen_primera_semana: diasAusencia >= 7 ? 0.6 : 0.8,
    redistribuir_fases: diasAusencia >= 14
  };

  return {
    ok: true,
    exc_id: excResult.exc_id,
    dias_ausencia: diasAusencia,
    ajuste_plan: ajuste,
    mensaje: `Ausencia registrada (${diasAusencia} días). Plan ajustado.`
  };
}

/**
 * Sube peso de un ejercicio manualmente (progresión manual).
 * Fuente: ACSM 2009 — subida controlada 2.5-5%
 */
function subirPesoManual(datos) {
  const hoja = getHoja(HOJAS.EJERCICIOS_PLAN);
  const allDatos = hoja.getDataRange().getValues();
  const cabeceras = allDatos[0];
  const colEjId = cabeceras.indexOf('ejercicio_id');
  const colPeso = cabeceras.indexOf('num_peso_sugerido_kg');

  let actualizado = false;
  for (let i = allDatos.length - 1; i >= 1; i--) {
    if (allDatos[i][colEjId] === datos.ejercicio_id) {
      const pesoActual = allDatos[i][colPeso];
      const nuevoPeso = datos.nuevo_peso_kg || pesoActual * 1.025; // +2.5% por defecto
      hoja.getRange(i + 1, colPeso + 1).setValue(Math.round(nuevoPeso * 10) / 10);
      actualizado = true;
      break;
    }
  }

  return { ok: actualizado, ejercicio_id: datos.ejercicio_id, mensaje: 'Peso actualizado manualmente' };
}

/**
 * Sincroniza datos de FatSecret vía su API.
 * Importa comidas del día a comidas_log.
 * Fuente: Integración FatSecret REST API (OAuth 2.0)
 */
function syncFatSecret() {
  // FatSecret API credentials (configurar en Script Properties)
  const props = PropertiesService.getScriptProperties();
  const accessToken = props.getProperty('FATSECRET_ACCESS_TOKEN');

  if (!accessToken) {
    return { ok: false, error: 'FatSecret no configurado. Añadir FATSECRET_ACCESS_TOKEN en Properties.' };
  }

  const hoy = Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd');
  // Convertir fecha a days_since_epoch para FatSecret
  const epoch = new Date(1970, 0, 1);
  const daysSinceEpoch = Math.floor((new Date() - epoch) / (1000 * 60 * 60 * 24));

  try {
    const response = UrlFetchApp.fetch(
      `https://platform.fatsecret.com/rest/food-entries/v2?date=${daysSinceEpoch}&format=json`,
      {
        headers: { 'Authorization': 'Bearer ' + accessToken },
        muteHttpExceptions: true
      }
    );

    const data = JSON.parse(response.getContentText());
    const entries = data.food_entries ? data.food_entries.food_entry : [];

    let totalCal = 0, totalProt = 0, totalCarbs = 0, totalGrasa = 0;
    const comidas = [];

    for (const entry of (Array.isArray(entries) ? entries : [entries])) {
      const comida = {
        tipo_comida: entry.meal || 'snack',
        calorias: parseFloat(entry.calories) || 0,
        proteina_g: parseFloat(entry.protein) || 0,
        carbos_g: parseFloat(entry.carbohydrate) || 0,
        grasas_g: parseFloat(entry.fat) || 0,
        notas: entry.food_entry_description || ''
      };
      totalCal += comida.calorias;
      totalProt += comida.proteina_g;
      totalCarbs += comida.carbos_g;
      totalGrasa += comida.grasas_g;
      comidas.push(comida);

      // Guardar cada comida en la BD
      guardarComida({ ...comida, fecha: hoy });
    }

    return {
      ok: true,
      fecha: hoy,
      total_comidas: comidas.length,
      totales: { calorias: totalCal, proteina_g: totalProt, carbos_g: totalCarbs, grasas_g: totalGrasa }
    };
  } catch (e) {
    return { ok: false, error: 'Error sincronizando FatSecret: ' + e.message };
  }
}

/**
 * Obtiene la última composición corporal semanal de Zepp (peso, grasa, músculo).
 * Se extrae al final de cada semana del último pesaje registrado.
 */
function getComposicionSemanal() {
  const hoja = getHoja(HOJAS.PESO_LOG);
  const datos = hoja.getDataRange().getValues();
  const cabeceras = datos[0];

  // Buscar último registro con datos de composición
  for (let i = datos.length - 1; i >= 1; i--) {
    if (datos[i][cabeceras.indexOf('user_id')] === USER_ID &&
        datos[i][cabeceras.indexOf('num_grasa_pct')]) {
      return {
        fecha: datos[i][cabeceras.indexOf('date_fecha')],
        peso_kg: datos[i][cabeceras.indexOf('num_peso_kg')],
        grasa_pct: datos[i][cabeceras.indexOf('num_grasa_pct')],
        musculo_kg: datos[i][cabeceras.indexOf('num_musculo_kg')],
        fuente: datos[i][cabeceras.indexOf('str_fuente')]
      };
    }
  }
  return { error: 'Sin datos de composición corporal' };
}

/**
 * Guarda mediciones corporales (circunferencias).
 * Solo al inicio del plan y al final de cada fase.
 */
function guardarMedicion(datos) {
  const hoja = getHoja(HOJAS.MEDICIONES_LOG);
  const medId = generarId('MED');

  const fila = [
    medId,
    USER_ID,
    datos.fecha || Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd'),
    datos.tipo || 'inicio', // 'inicio' | 'fin_fase'
    datos.fase || '',
    datos.hombros_cm || '',
    datos.pecho_cm || '',
    datos.cintura_cm || '',
    datos.cadera_cm || '',
    datos.bicep_cm || '',
    datos.muslo_cm || '',
    datos.pantorrilla_cm || '',
    new Date().toISOString()
  ];
    new Date().toISOString()
  ];

  hoja.appendRow(fila);
  return { ok: true, medicion_id: medId };
}

// ─── MOTOR DE CARGAS (REG-LOG-01) ────────────────────────────

/**
 * Calcula el ajuste del día basado en métricas.
 * Fuente: Adaptado de Kiviniemi 2007 (FC reposo como proxy de HRV)
 */
function calcularAjusteDia() {
  const hoy = Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd');
  const metricasHoy = getUltimaMetrica(HOJAS.METRICAS_ZEPP, hoy);

  let ajuste = 1.0;
  let razones = [];

  if (metricasHoy) {
    // FC reposo elevada (Kiviniemi 2007 adaptado)
    const fcMedia10d = calcularMediaFC(10);
    if (metricasHoy.num_hr_reposo > fcMedia10d + 10) {
      ajuste *= 0.80;
      razones.push('FC reposo elevada (+10 vs media)');
    }

    // Sleep Score bajo (heurístico)
    if (metricasHoy.num_sleep_score < 60) {
      ajuste *= 0.90;
      razones.push('Sleep Score bajo (<60)');
    }
  }

  return {
    factor: ajuste,
    razones: razones.length > 0 ? razones : ['Sin ajustes - sesión normal'],
    tipo: ajuste >= 1.0 ? 'normal' : (ajuste >= 0.80 ? 'reducida' : 'recuperacion')
  };
}

/**
 * Calcula peso sugerido para próxima sesión.
 * Regla ACSM 2009: Si completó +1-2 reps sobre objetivo con RIR ≥ 2 → subir 2.5%
 * Si el usuario no indica progresión manual, la app sube automáticamente.
 * También se puede subir manualmente vía 'subir_peso_manual'.
 */
function calcularPesoSugerido(ejercicioId, pesoBase) {
  const hoja = getHoja(HOJAS.EJERCICIOS_LOG);
  const datos = hoja.getDataRange().getValues();
  const cabeceras = datos[0];

  // Buscar último log de este ejercicio
  let ultimoLog = null;
  for (let i = datos.length - 1; i >= 1; i--) {
    if (datos[i][cabeceras.indexOf('ejercicio_id')] === ejercicioId) {
      ultimoLog = filaAObjeto(cabeceras, datos[i]);
      break;
    }
  }

  if (!ultimoLog) return pesoBase;

  const repsCompletadas = ultimoLog.num_reps_completadas;
  const pesoUsado = ultimoLog.num_peso_usado_kg;

  // APRE simplificado (Mann 2010)
  if (repsCompletadas >= 13) {
    return pesoUsado + 5;
  } else if (repsCompletadas >= 8) {
    return pesoUsado + 2.5;
  } else if (repsCompletadas <= 4) {
    return Math.max(pesoUsado - 2.5, 0);
  }

  return pesoUsado;
}

/**
 * Actualiza log de progresión (1RM estimado, PRs).
 */
function actualizarProgresion(ejercicioId, peso, reps) {
  const hoja = getHoja(HOJAS.PROGRESION_LOG);
  const progId = generarId('PRO');

  // 1RM estimado (fórmula Epley)
  const rm1Estimado = peso * (1 + reps / 30);
  const volumenTotal = peso * reps;

  // Verificar si es PR
  const ultimoPR = getUltimo1RM(ejercicioId);
  const esPR = rm1Estimado > ultimoPR ? '1rm' : '';

  const fila = [
    progId,
    USER_ID,
    ejercicioId,
    Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd'),
    Math.round(rm1Estimado * 10) / 10,
    peso,
    volumenTotal,
    reps,
    esPR
  ];

  hoja.appendRow(fila);
}

// ─── UTILIDADES ───────────────────────────────────────────────

function getHoja(nombre) {
  return SpreadsheetApp.getActiveSpreadsheet().getSheetByName(nombre);
}

function generarId(prefijo) {
  const ahora = Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyyMMdd');
  const random = Math.floor(Math.random() * 9999).toString().padStart(4, '0');
  return `${prefijo}_${ahora}_${random}`;
}

function filaAObjeto(cabeceras, fila) {
  const obj = {};
  for (let i = 0; i < cabeceras.length; i++) {
    obj[cabeceras[i]] = fila[i];
  }
  return obj;
}

function getEjerciciosDeSesion(sesionId) {
  const hoja = getHoja(HOJAS.EJERCICIOS_PLAN);
  const datos = hoja.getDataRange().getValues();
  const cabeceras = datos[0];
  const ejercicios = [];

  for (let i = 1; i < datos.length; i++) {
    if (datos[i][cabeceras.indexOf('sesion_id')] === sesionId) {
      ejercicios.push(filaAObjeto(cabeceras, datos[i]));
    }
  }
  return ejercicios.sort((a, b) => a.num_orden - b.num_orden);
}

function getUltimaMetrica(nombreHoja, fecha) {
  const hoja = getHoja(nombreHoja);
  const datos = hoja.getDataRange().getValues();
  const cabeceras = datos[0];
  const colFecha = cabeceras.indexOf('date_fecha');

  for (let i = datos.length - 1; i >= 1; i--) {
    const fechaFila = Utilities.formatDate(new Date(datos[i][colFecha]), 'Europe/Madrid', 'yyyy-MM-dd');
    if (fechaFila === fecha && datos[i][cabeceras.indexOf('user_id')] === USER_ID) {
      return filaAObjeto(cabeceras, datos[i]);
    }
  }
  return null;
}

function calcularMediaFC(dias) {
  const hoja = getHoja(HOJAS.METRICAS_ZEPP);
  const datos = hoja.getDataRange().getValues();
  const cabeceras = datos[0];
  const colFC = cabeceras.indexOf('num_hr_reposo');

  const valores = [];
  for (let i = Math.max(1, datos.length - dias); i < datos.length; i++) {
    if (datos[i][cabeceras.indexOf('user_id')] === USER_ID && datos[i][colFC]) {
      valores.push(datos[i][colFC]);
    }
  }

  if (valores.length === 0) return 53; // baseline del usuario
  return valores.reduce((a, b) => a + b, 0) / valores.length;
}

function calcularMediaPeso(dias) {
  const hoja = getHoja(HOJAS.PESO_LOG);
  const datos = hoja.getDataRange().getValues();
  const cabeceras = datos[0];
  const colPeso = cabeceras.indexOf('num_peso_kg');

  const valores = [];
  for (let i = Math.max(1, datos.length - dias); i < datos.length; i++) {
    if (datos[i][cabeceras.indexOf('user_id')] === USER_ID && datos[i][colPeso]) {
      valores.push(datos[i][colPeso]);
    }
  }

  if (valores.length === 0) return 78.2;
  return Math.round((valores.reduce((a, b) => a + b, 0) / valores.length) * 10) / 10;
}

function getUltimo1RM(ejercicioId) {
  const hoja = getHoja(HOJAS.PROGRESION_LOG);
  const datos = hoja.getDataRange().getValues();
  const cabeceras = datos[0];
  const col1RM = cabeceras.indexOf('num_1rm_estimado');
  const colEjId = cabeceras.indexOf('ejercicio_id');

  let max1RM = 0;
  for (let i = 1; i < datos.length; i++) {
    if (datos[i][colEjId] === ejercicioId && datos[i][col1RM] > max1RM) {
      max1RM = datos[i][col1RM];
    }
  }
  return max1RM;
}

// ─── INICIALIZACIÓN ───────────────────────────────────────────

/**
 * Crea todas las hojas con cabeceras.
 * Ejecutar UNA SOLA VEZ al configurar.
 */
function inicializarHojas() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();

  const estructuras = {
    [HOJAS.USUARIOS]: ['user_id', 'str_nombre', 'date_nacimiento', 'num_altura_cm', 'str_sexo', 'str_objetivo', 'num_dias_entreno', 'str_split', 'bool_ramadan', 'bool_halal', 'date_creado', 'date_modificado'],
    [HOJAS.METRICAS_ZEPP]: ['metrica_id', 'user_id', 'date_fecha', 'num_sleep_score', 'num_sleep_horas', 'num_sleep_deep_min', 'num_sleep_rem_min', 'num_hrv_rmssd', 'num_hr_reposo', 'num_readiness', 'num_stress_avg', 'num_pasos_ayer', 'num_calorias_activas', 'date_sync'],
    [HOJAS.PESO_LOG]: ['peso_id', 'user_id', 'date_fecha', 'num_peso_kg', 'num_grasa_pct', 'num_musculo_kg', 'str_fuente', 'str_condiciones', 'date_creado'],
    [HOJAS.SESIONES_PLAN]: ['sesion_id', 'user_id', 'date_fecha', 'str_tipo', 'num_semana_meso', 'str_fase', 'num_ajuste_volumen', 'str_razon_ajuste', 'num_duracion_est_min', 'bool_completada', 'date_inicio', 'date_fin', 'date_creado'],
    [HOJAS.EJERCICIOS_PLAN]: ['plan_id', 'sesion_id', 'ejercicio_id', 'num_orden', 'num_series_plan', 'num_reps_plan', 'num_peso_sugerido_kg', 'num_rir_objetivo', 'num_descanso_seg', 'str_notas', 'bool_es_warmup'],
    [HOJAS.EJERCICIOS_LOG]: ['log_id', 'plan_id', 'sesion_id', 'ejercicio_id', 'num_serie', 'num_peso_usado_kg', 'num_reps_completadas', 'num_rir_percibido', 'num_rpe', 'str_sensacion', 'str_notas', 'bool_dolor', 'str_zona_dolor', 'date_timestamp'],
    [HOJAS.PROGRESION_LOG]: ['prog_id', 'user_id', 'ejercicio_id', 'date_fecha', 'num_1rm_estimado', 'num_peso_trabajo', 'num_volumen_total', 'num_reps_max', 'str_pr_tipo'],
    [HOJAS.COMIDAS_LOG]: ['comida_id', 'user_id', 'date_fecha', 'str_tipo_comida', 'num_calorias', 'num_proteina_g', 'num_carbos_g', 'num_grasas_g', 'bool_pre_entreno', 'bool_post_entreno', 'str_notas', 'date_hora'],
    [HOJAS.HIDRATACION_LOG]: ['hidra_id', 'user_id', 'date_fecha', 'num_agua_ml', 'num_objetivo_ml', 'date_modificado'],
    [HOJAS.SUPLEMENTOS_LOG]: ['supp_id', 'user_id', 'date_fecha', 'bool_whey', 'bool_caseina', 'bool_vitd_k', 'bool_omega3', 'bool_magnesio', 'bool_ashwagandha', 'bool_cromo', 'str_notas'],
    [HOJAS.EXCEPCIONES_LOG]: ['exc_id', 'user_id', 'str_tipo', 'date_inicio', 'date_fin', 'str_detalles', 'str_zona_afectada', 'num_severidad', 'bool_activa'],
    [HOJAS.PLAN_ANUAL]: ['fase_id', 'user_id', 'num_año', 'num_orden', 'str_nombre_fase', 'str_tipo', 'date_inicio', 'date_fin', 'num_semanas', 'num_volumen_objetivo', 'str_rir_rango', 'str_foco_muscular', 'str_objetivo_nutri', 'str_notas'],
    [HOJAS.PLAN_SEMANAL]: ['semana_id', 'fase_id', 'user_id', 'num_semana_año', 'num_semana_fase', 'str_lunes', 'str_martes', 'str_miercoles', 'str_jueves', 'str_viernes', 'str_sabado', 'str_domingo', 'str_rir_semana', 'bool_deload'],
    [HOJAS.EJERCICIOS_CATALOGO]: ['ejercicio_id', 'str_nombre', 'str_nombre_en', 'str_grupo_principal', 'arr_grupos_secundarios', 'str_patron', 'str_equipamiento', 'bool_compuesto', 'bool_favorito', 'bool_excluido', 'str_razon_exclusion', 'str_alternativa'],
    [HOJAS.MEDICIONES_LOG]: ['medicion_id', 'user_id', 'date_fecha', 'str_tipo', 'str_fase', 'num_hombros_cm', 'num_pecho_cm', 'num_cintura_cm', 'num_cadera_cm', 'num_bicep_cm', 'num_muslo_cm', 'num_pantorrilla_cm', 'date_creado']
  };

  for (const [nombre, cabeceras] of Object.entries(estructuras)) {
    let hoja = ss.getSheetByName(nombre);
    if (!hoja) {
      hoja = ss.insertSheet(nombre);
    }
    hoja.getRange(1, 1, 1, cabeceras.length).setValues([cabeceras]);
    // Formato cabeceras
    hoja.getRange(1, 1, 1, cabeceras.length).setFontWeight('bold');
    hoja.setFrozenRows(1);
  }

  // Insertar datos iniciales del usuario
  const hojaUsuarios = ss.getSheetByName(HOJAS.USUARIOS);
  hojaUsuarios.appendRow([
    USER_ID, 'Usuario', '2001-07-20', 188, 'M', 'bulk', 4, 'Push/Pull/Pierna/Upper',
    true, true, new Date().toISOString(), new Date().toISOString()
  ]);

  return { ok: true, mensaje: 'Hojas inicializadas correctamente (14 hojas)' };
}
