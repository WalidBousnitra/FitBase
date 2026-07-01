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
  COMIDAS_LOG: 'comidas_log',
  PLAN_ANUAL: 'plan_anual',
  PLAN_SEMANAL: 'plan_semanal',
  EJERCICIOS_CATALOGO: 'ejercicios_catalogo'
};

// ─── ENDPOINTS REST ───────────────────────────────────────────

/**
 * Maneja peticiones GET.
 * Rutas:
 *   ?accion=sesion_hoy
 *   ?accion=plan_anual
 *   ?accion=plan_semanal&semana=25
 *   ?accion=macros_hoy
 *   ?accion=check_ausencia
 *   ?accion=progresion_metricas
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
      case 'macros_hoy':
        resultado = getMacrosHoy();
        break;
      case 'check_ausencia':
        resultado = checkAusencia();
        break;
      case 'progresion_metricas':
        resultado = getProgresionMetricas(e.parameter.dias);
        break;
      default:
        resultado = { error: 'Acción no reconocida', acciones_validas: ['sesion_hoy', 'plan_anual', 'plan_semanal', 'macros_hoy', 'check_ausencia', 'progresion_metricas'] };
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
 *   accion=completar_sesion
 *   accion=sync_nutricion
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
      case 'completar_sesion':
        resultado = completarSesion(datos);
        break;
      case 'sync_nutricion':
        resultado = syncNutricionDesdeHealthConnect(datos);
        break;
      default:
        resultado = { error: 'Acción POST no reconocida', acciones_validas: ['guardar_log', 'guardar_peso', 'guardar_metricas', 'completar_sesion', 'sync_nutricion'] };
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
 * Obtiene el peso más reciente del usuario desde peso_log.
 * Fallback: 78.2 kg (biometria.md) si no hay registros.
 */
function getPesoActual() {
  const hoja = getHoja(HOJAS.PESO_LOG);
  const datos = hoja.getDataRange().getValues();
  if (datos.length <= 1) return { peso: 78.2, grasa_pct: null, fuente: 'default' };

  const cabeceras = datos[0];
  const idxPeso = cabeceras.indexOf('num_peso_kg');
  const idxGrasa = cabeceras.indexOf('num_grasa_pct');
  const idxFecha = cabeceras.indexOf('date_fecha');

  for (let i = datos.length - 1; i >= 1; i--) {
    const peso = datos[i][idxPeso];
    if (peso && peso > 0) {
      return {
        peso: peso,
        grasa_pct: datos[i][idxGrasa] || null,
        fecha: datos[i][idxFecha],
        fuente: 'peso_log'
      };
    }
  }
  return { peso: 78.2, grasa_pct: null, fuente: 'default' };
}

/**
 * Calcula tendencia de peso: media 7d recientes vs media 7d anteriores.
 * Fuente: Iraki 2019 (target 0.25-0.5% BW/sem en bulk), Helms 2014 (0.5-1% en cut).
 * Necesita ≥3 datos en cada ventana para ser fiable.
 * Devuelve null si no hay suficientes datos (primeras ~2 semanas).
 */
function getTendenciaPeso() {
  const hoja = getHoja(HOJAS.PESO_LOG);
  const datos = hoja.getDataRange().getValues();
  if (datos.length <= 1) return null;

  const cabeceras = datos[0];
  const idxPeso = cabeceras.indexOf('num_peso_kg');
  const idxFecha = cabeceras.indexOf('date_fecha');

  const hoy = new Date();
  const hace7 = new Date(hoy); hace7.setDate(hace7.getDate() - 7);
  const hace14 = new Date(hoy); hace14.setDate(hace14.getDate() - 14);

  let pesosRecientes = [];
  let pesosAnteriores = [];

  for (let i = 1; i < datos.length; i++) {
    const fecha = new Date(datos[i][idxFecha]);
    const peso = datos[i][idxPeso];
    if (!peso || peso <= 0) continue;

    if (fecha >= hace7) {
      pesosRecientes.push(peso);
    } else if (fecha >= hace14) {
      pesosAnteriores.push(peso);
    }
  }

  if (pesosRecientes.length < 3 || pesosAnteriores.length < 3) return null;

  const mediaReciente = pesosRecientes.reduce((a, b) => a + b, 0) / pesosRecientes.length;
  const mediaAnterior = pesosAnteriores.reduce((a, b) => a + b, 0) / pesosAnteriores.length;
  const cambioKgSemana = mediaReciente - mediaAnterior;
  const cambioPctSemana = (cambioKgSemana / mediaAnterior) * 100;

  return {
    media_reciente: Math.round(mediaReciente * 10) / 10,
    media_anterior: Math.round(mediaAnterior * 10) / 10,
    cambio_kg_semana: Math.round(cambioKgSemana * 100) / 100,
    cambio_pct_semana: Math.round(cambioPctSemana * 100) / 100,
    datos_recientes: pesosRecientes.length,
    datos_anteriores: pesosAnteriores.length
  };
}

/**
 * Obtiene los pasos de hoy desde metricas_zepp.
 * Fuente: motor_dieta.md §6 — ajuste NEAT si > 12000 pasos.
 */
function getPasosHoy() {
  const hoja = getHoja(HOJAS.METRICAS_ZEPP);
  const datos = hoja.getDataRange().getValues();
  if (datos.length <= 1) return 0;

  const cabeceras = datos[0];
  const idxPasos = cabeceras.indexOf('num_pasos_ayer');
  const idxFecha = cabeceras.indexOf('date_fecha');
  const hoy = Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd');

  for (let i = datos.length - 1; i >= 1; i--) {
    const fecha = datos[i][idxFecha];
    const fechaStr = fecha instanceof Date
      ? Utilities.formatDate(fecha, 'Europe/Madrid', 'yyyy-MM-dd')
      : String(fecha);
    if (fechaStr === hoy && datos[i][idxPasos]) {
      return datos[i][idxPasos];
    }
  }
  return 0;
}

/**
 * Obtiene el sleep score de hoy desde metricas_zepp.
 * Fuente: motor_pesos.md §3 — sleep score < 60 afecta recomendaciones.
 */
function getSleepScoreHoy() {
  const hoja = getHoja(HOJAS.METRICAS_ZEPP);
  const datos = hoja.getDataRange().getValues();
  if (datos.length <= 1) return null;

  const cabeceras = datos[0];
  const idxSleep = cabeceras.indexOf('num_sleep_score');
  const idxFecha = cabeceras.indexOf('date_fecha');
  const hoy = Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd');

  for (let i = datos.length - 1; i >= 1; i--) {
    const fecha = datos[i][idxFecha];
    const fechaStr = fecha instanceof Date
      ? Utilities.formatDate(fecha, 'Europe/Madrid', 'yyyy-MM-dd')
      : String(fecha);
    if (fechaStr === hoy && datos[i][idxSleep]) {
      return datos[i][idxSleep];
    }
  }
  return null;
}

/**
 * Calcula macros objetivo del día actual — DINÁMICO.
 *
 * Fuentes:
 *  - BMR: Mifflin-St Jeor 1990 (R²=0.71)
 *  - Macros bulk: Iraki 2019 (1.6-2.2 g/kg prot, +10-20% kcal, ≥3-5 g/kg carbs)
 *  - Macros cut: Helms 2014 (2.3-3.1 g/kg LBM prot, -20-25%)
 *  - Ajuste tendencia bulk: Iraki 2019 (target 0.25-0.5% BW/sem)
 *  - Ajuste tendencia cut: Helms 2014 (target 0.5-1% BW/sem)
 *  - NEAT/pasos: motor_dieta.md §6 (⚠️ HEURÍSTICO)
 *  - Agua: 35ml/kg + 500ml si entrena (⚠️ HEURÍSTICO)
 */
function getMacrosHoy() {
  const hoy = Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd');
  const sesionHoy = getSesionHoy();
  const esTraining = sesionHoy.sesion !== null;

  // ═══ 1. PESO REAL (no hardcodeado) ═══
  const pesoData = getPesoActual();
  const peso = pesoData.peso;
  const grasaPct = pesoData.grasa_pct;
  const altura = 188;
  const edad = 24;

  // ═══ 2. BMR Mifflin-St Jeor (EVI-11 — R²=0.71) ═══
  const bmr = (10 * peso) + (6.25 * altura) - (5 * edad) + 5;

  // ═══ 3. TDEE (⚠️ factor HEURÍSTICO) ═══
  const factorActividad = 1.55;
  const tdee = Math.round(bmr * factorActividad);

  // ═══ 4. FASE Y OBJETIVO NUTRICIONAL ═══
  const planAnual = getPlanAnual();
  const faseActual = planAnual.fase_actual;
  let objetivoNutri = 'bulk';
  if (faseActual) {
    objetivoNutri = faseActual.str_objetivo_nutri || 'bulk';
  }

  // ═══ 5. SUPERÁVIT/DÉFICIT BASE (Iraki 2019, Helms 2014) ═══
  let multiplicador;
  let proteinaRatio;

  switch (objetivoNutri) {
    case 'bulk':
      multiplicador = 1.15;  // +15% default (Iraki 2019: +10-20%)
      proteinaRatio = 2.0;   // g/kg (Iraki 2019: 1.6-2.2)
      break;
    case 'cut':
      multiplicador = 0.80;  // -20% default (Helms 2014)
      proteinaRatio = 2.4;   // g/kg total ≈ 2.8 g/kg LBM (Helms 2014: 2.3-3.1 LBM)
      break;
    default:
      multiplicador = 1.05;  // +5% mantener
      proteinaRatio = 2.0;
  }

  // ═══ 6. AJUSTE POR TENDENCIA DE PESO (Iraki 2019, Helms 2014) ═══
  const tendencia = getTendenciaPeso();
  let ajusteTendencia = 'sin_datos';
  let razonTendencia = 'Menos de 2 semanas de datos — usando default';

  if (tendencia) {
    const pctSem = tendencia.cambio_pct_semana;

    if (objetivoNutri === 'bulk') {
      // Iraki 2019: target 0.25-0.5% BW/semana
      if (pctSem > 0.5) {
        multiplicador = 1.10; // Reducir: ganando demasiado rápido (probablemente grasa)
        ajusteTendencia = 'reducido';
        razonTendencia = `Ganando ${pctSem}%/sem (>0.5%) → surplus reducido a +10%`;
      } else if (pctSem >= 0.25) {
        // On track — no cambiar
        ajusteTendencia = 'optimo';
        razonTendencia = `Ganando ${pctSem}%/sem (0.25-0.5%) → en rango óptimo`;
      } else if (pctSem >= 0) {
        multiplicador = 1.20; // Subir: ganando demasiado lento
        ajusteTendencia = 'aumentado';
        razonTendencia = `Ganando ${pctSem}%/sem (<0.25%) → surplus aumentado a +20%`;
      } else {
        multiplicador = 1.20; // Perdiendo peso en bulk → subir
        ajusteTendencia = 'aumentado';
        razonTendencia = `Perdiendo peso en bulk (${pctSem}%/sem) → surplus aumentado a +20%`;
      }
    } else if (objetivoNutri === 'cut') {
      // Helms 2014: target 0.5-1% BW/semana de pérdida
      const perdidaPct = -pctSem; // convertir a positivo
      if (perdidaPct > 1.0) {
        multiplicador = 0.85; // Reducir déficit: perdiendo demasiado rápido (riesgo muscular)
        ajusteTendencia = 'reducido';
        razonTendencia = `Perdiendo ${perdidaPct}%/sem (>1%) → déficit reducido a -15%`;
      } else if (perdidaPct >= 0.5) {
        ajusteTendencia = 'optimo';
        razonTendencia = `Perdiendo ${perdidaPct}%/sem (0.5-1%) → en rango óptimo`;
      } else if (perdidaPct >= 0) {
        multiplicador = 0.75; // Aumentar déficit: perdiendo demasiado lento
        ajusteTendencia = 'aumentado';
        razonTendencia = `Perdiendo ${perdidaPct}%/sem (<0.5%) → déficit aumentado a -25%`;
      } else {
        multiplicador = 0.75; // Ganando peso en cut → más déficit
        ajusteTendencia = 'aumentado';
        razonTendencia = `Ganando peso en cut (${pctSem}%/sem) → déficit aumentado a -25%`;
      }
    }
    // 'mantener': no ajustar por tendencia (el objetivo es estabilidad)
  }

  // ═══ 7. PROTEÍNA CON COMPOSICIÓN CORPORAL (Helms 2014) ═══
  // Si hay dato de grasa corporal (Zepp), usar LBM para proteína en cut
  if (objetivoNutri === 'cut' && grasaPct && grasaPct > 0 && grasaPct < 50) {
    const lbm = peso * (1 - grasaPct / 100);
    // Helms 2014: 2.3-3.1 g/kg LBM → usar 2.6 (centro del rango)
    const protPorLbm = Math.round(lbm * 2.6);
    // Solo usar si es MAYOR que el cálculo por peso total (más conservador)
    if (protPorLbm > Math.round(peso * proteinaRatio)) {
      proteinaRatio = protPorLbm / peso; // convertir a ratio equivalente
    }
  }

  // ═══ 8. CALORÍAS BASE ═══
  let caloriasObjetivo = Math.round(tdee * multiplicador);

  // ═══ 9. AJUSTE NEAT POR PASOS (motor_dieta.md §6 — ⚠️ HEURÍSTICO) ═══
  const pasosHoy = getPasosHoy();
  let ajusteNeat = 0;
  let razonNeat = '';
  if (pasosHoy > 12000) {
    ajusteNeat = 150;
    razonNeat = `${pasosHoy} pasos (>12k) → +150 kcal NEAT`;
    caloriasObjetivo += ajusteNeat;
  }

  // ═══ 10. MACROS FINALES (Iraki 2019, Helms 2014) ═══
  const proteinaG = Math.round(peso * proteinaRatio);
  const grasaG = Math.round(peso * 1.0);    // 1.0 g/kg (Iraki 2019: 0.5-1.5)
  const calsProt = proteinaG * 4;
  const calsGrasa = grasaG * 9;
  const carbosG = Math.round((caloriasObjetivo - calsProt - calsGrasa) / 4);

  // ═══ 11. AGUA (⚠️ HEURÍSTICO: 35ml/kg + 500ml si entrena) ═══
  const aguaBase = Math.round(peso * 35);
  const aguaObjetivo = esTraining ? aguaBase + 500 : aguaBase;

  // ═══ 12. SLEEP SCORE (informativo — no afecta nutrición per se) ═══
  const sleepScore = getSleepScoreHoy();

  // ═══ 13. CONSUMO REAL SYNC (Health Connect -> comidas_log) ═══
  const comidasHoja = getHoja(HOJAS.COMIDAS_LOG);
  const comidasData = comidasHoja.getDataRange().getValues();
  const cabComidas = comidasData[0];
  const comColFecha = cabComidas.indexOf('date_fecha');
  const comColUser = cabComidas.indexOf('user_id');
  const comColCal = cabComidas.indexOf('num_calorias');
  const comColProt = cabComidas.indexOf('num_proteina_g');
  const comColCarbs = cabComidas.indexOf('num_carbos_g');
  const comColGrasas = cabComidas.indexOf('num_grasas_g');

  let caloriasConsumidas = 0;
  let proteinaConsumidaG = 0;
  let carbosConsumidosG = 0;
  let grasasConsumidasG = 0;

  for (let i = 1; i < comidasData.length; i++) {
    if (comidasData[i][comColFecha] === hoy && comidasData[i][comColUser] === USER_ID) {
      caloriasConsumidas += comidasData[i][comColCal] || 0;
      proteinaConsumidaG += comidasData[i][comColProt] || 0;
      carbosConsumidosG += comidasData[i][comColCarbs] || 0;
      grasasConsumidasG += comidasData[i][comColGrasas] || 0;
    }
  }
  const origenDatos = (caloriasConsumidas > 0 || proteinaConsumidaG > 0 || carbosConsumidosG > 0 || grasasConsumidasG > 0)
    ? 'health_connect_sync'
    : 'sin_datos';

  return {
    // Contrato original (Android compatible)
    fecha: hoy,
    es_dia_entreno: esTraining,
    fase: objetivoNutri,
    calorias_objetivo: caloriasObjetivo,
    proteina_g: proteinaG,
    carbos_g: carbosG,
    grasas_g: grasaG,
    agua_ml: aguaObjetivo,
    pasos_objetivo: 8000,
    calorias_consumidas: caloriasConsumidas,
    proteina_consumida_g: proteinaConsumidaG,
    carbos_consumidos_g: carbosConsumidosG,
    grasas_consumidas_g: grasasConsumidasG,
    agua_consumida_ml: 0,
    pasos_actuales: pasosHoy,
    bmr: Math.round(bmr),
    tdee: tdee,
    origen_datos: origenDatos,
    es_fallback: false,

    // Campos nuevos (Android los ignora con Gson — no rompe nada)
    peso_actual: peso,
    peso_fuente: pesoData.fuente,
    grasa_pct: grasaPct,
    multiplicador_usado: multiplicador,
    ajuste_tendencia: ajusteTendencia,
    razon_tendencia: razonTendencia,
    ajuste_neat_kcal: ajusteNeat,
    razon_neat: razonNeat,
    tendencia: tendencia,
    sleep_score: sleepScore
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
    datos.str_sensacion || 'bien',
    new Date().toISOString()
  ];

  hoja.appendRow(fila);

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
    datos.hr_reposo || 0,
    datos.pasos_ayer || 0,
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
  const volPorTipo = { 'Push': 21, 'Pull': 23, 'Pierna': 22, 'Hombros+Brazos': 21 };
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
 * Guarda datos de nutrición recibidos desde Health Connect (Android).
 * FatSecret → Health Connect → FitBase Android → API → Sheets.
 * No se usa la API REST de FatSecret (la cuenta Platform es solo para devs).
 */
function syncNutricionDesdeHealthConnect(datos) {
  const hoja = getHoja(HOJAS.COMIDAS_LOG);
  const fecha = datos.fecha || Utilities.formatDate(new Date(), 'Europe/Madrid', 'yyyy-MM-dd');

  let totalCal = 0, totalProt = 0, totalCarbs = 0, totalGrasa = 0;

  const comidas = datos.comidas || [];
  for (const comida of comidas) {
    const comidaId = generarId('COM');
    const fila = [
      comidaId,
      USER_ID,
      fecha,
      comida.tipo_comida || 'snack',
      comida.calorias || 0,
      comida.proteina_g || 0,
      comida.carbos_g || 0,
      comida.grasas_g || 0,
      comida.nombre || '',
      new Date().toISOString()
    ];
    hoja.appendRow(fila);

    totalCal += comida.calorias || 0;
    totalProt += comida.proteina_g || 0;
    totalCarbs += comida.carbos_g || 0;
    totalGrasa += comida.grasas_g || 0;
  }

  return {
    ok: true,
    fecha: fecha,
    total_comidas: comidas.length,
    totales: { calorias: totalCal, proteina_g: totalProt, carbos_g: totalCarbs, grasas_g: totalGrasa }
  };
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

function appendRowsBatch(hoja, filas, batchSize = 500) {
  if (!filas || filas.length === 0) return;
  const totalCols = filas[0].length;
  for (let i = 0; i < filas.length; i += batchSize) {
    const bloque = filas.slice(i, i + batchSize);
    hoja.getRange(hoja.getLastRow() + 1, 1, bloque.length, totalCols).setValues(bloque);
  }
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

// ─── PROGRESIÓN DE MÉTRICAS (pantalla de seguimiento) ─────────

/**
 * Devuelve historial de métricas clave para la pantalla de progresión.
 * Métricas: peso, grasa%, sueño (score + horas), FC reposo, HRV, pasos.
 * @param {number} dias — últimos N días (default 30)
 */
function getProgresionMetricas(dias) {
  dias = parseInt(dias) || 30;
  const hoy = new Date();
  const desde = new Date(hoy.getTime() - dias * 24 * 60 * 60 * 1000);

  // 1. Peso + composición
  const pesoData = [];
  const hojaPeso = getHoja(HOJAS.PESO_LOG);
  const datosPeso = hojaPeso.getDataRange().getValues();
  const cabPeso = datosPeso[0];
  for (let i = 1; i < datosPeso.length; i++) {
    if (datosPeso[i][cabPeso.indexOf('user_id')] !== USER_ID) continue;
    const fecha = new Date(datosPeso[i][cabPeso.indexOf('date_fecha')]);
    if (fecha >= desde) {
      pesoData.push({
        fecha: Utilities.formatDate(fecha, 'Europe/Madrid', 'yyyy-MM-dd'),
        peso_kg: datosPeso[i][cabPeso.indexOf('num_peso_kg')],
        grasa_pct: datosPeso[i][cabPeso.indexOf('num_grasa_pct')] || null,
        musculo_kg: datosPeso[i][cabPeso.indexOf('num_musculo_kg')] || null
      });
    }
  }

  // 2. Métricas Zepp (sueño, FC, HRV, estrés, pasos)
  const zeppData = [];
  const hojaZepp = getHoja(HOJAS.METRICAS_ZEPP);
  const datosZepp = hojaZepp.getDataRange().getValues();
  const cabZepp = datosZepp[0];
  for (let i = 1; i < datosZepp.length; i++) {
    if (datosZepp[i][cabZepp.indexOf('user_id')] !== USER_ID) continue;
    const fecha = new Date(datosZepp[i][cabZepp.indexOf('date_fecha')]);
    if (fecha >= desde) {
      zeppData.push({
        fecha: Utilities.formatDate(fecha, 'Europe/Madrid', 'yyyy-MM-dd'),
        sleep_score: datosZepp[i][cabZepp.indexOf('num_sleep_score')] || 0,
        sleep_horas: 0,
        sleep_deep_min: 0,
        hrv_rmssd: 0,
        hr_reposo: datosZepp[i][cabZepp.indexOf('num_hr_reposo')] || 0,
        stress_avg: 0,
        pasos: datosZepp[i][cabZepp.indexOf('num_pasos_ayer')] || 0
      });
    }
  }

  // 3. Volumen de entrenamiento por día
  const volumenData = [];
  const hojaLog = getHoja(HOJAS.EJERCICIOS_LOG);
  const datosLog = hojaLog.getDataRange().getValues();
  const cabLog = datosLog[0];
  const volPorDia = {};
  for (let i = 1; i < datosLog.length; i++) {
    const fechaStr = datosLog[i][cabLog.indexOf('date_timestamp')];
    if (!fechaStr) continue;
    const fecha = new Date(fechaStr);
    if (fecha < desde) continue;
    const dia = Utilities.formatDate(fecha, 'Europe/Madrid', 'yyyy-MM-dd');
    const peso = datosLog[i][cabLog.indexOf('num_peso_usado_kg')] || 0;
    const reps = datosLog[i][cabLog.indexOf('num_reps_completadas')] || 0;
    volPorDia[dia] = (volPorDia[dia] || 0) + (peso * reps);
  }
  for (const [dia, vol] of Object.entries(volPorDia)) {
    volumenData.push({ fecha: dia, volumen_kg: Math.round(vol) });
  }
  volumenData.sort((a, b) => a.fecha.localeCompare(b.fecha));

  return {
    dias_solicitados: dias,
    peso: pesoData,
    zepp: zeppData,
    volumen_entreno: volumenData,
    resumen: {
      peso_actual: pesoData.length > 0 ? pesoData[pesoData.length - 1].peso_kg : null,
      peso_inicio: pesoData.length > 0 ? pesoData[0].peso_kg : null,
      grasa_actual: pesoData.length > 0 ? pesoData[pesoData.length - 1].grasa_pct : null,
      sleep_media: zeppData.length > 0
        ? Math.round(zeppData.reduce((s, d) => s + d.sleep_score, 0) / zeppData.length)
        : null,
      pasos_media: zeppData.length > 0
        ? Math.round(zeppData.reduce((s, d) => s + d.pasos, 0) / zeppData.length)
        : null
    }
  };
}

// ─── VACIAR BASE DE DATOS (reset para demos) ─────────────────

/**
 * Vacía TODAS las hojas de datos (elimina filas, conserva cabeceras).
 * CUIDADO: Irreversible. Usar solo para resetear antes de la fecha de inicio real.
 *
 * Requiere: datos.confirmar === 'VACIAR_TODO'
 * Opción: datos.hojas_excluir — array de nombres de hojas que NO se vacían
 *         (p.ej. ['ejercicios_catalogo', 'plan_anual'] para mantener el plan)
 */
function vaciarBaseDatos(datos) {
  // Seguridad: requiere confirmación explícita
  if (datos.confirmar !== 'VACIAR_TODO') {
    return {
      ok: false,
      error: 'Debes enviar confirmar: "VACIAR_TODO" para proceder',
      mensaje: 'Esto borra TODOS los datos de la BBDD (excepto cabeceras y catálogo)'
    };
  }

  const excluir = datos.hojas_excluir || [];
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const resultados = {};

  for (const [key, nombre] of Object.entries(HOJAS)) {
    if (excluir.includes(nombre)) {
      resultados[nombre] = 'EXCLUIDA (no vaciada)';
      continue;
    }

    const hoja = ss.getSheetByName(nombre);
    if (!hoja) {
      resultados[nombre] = 'NO EXISTE';
      continue;
    }

    const ultimaFila = hoja.getLastRow();
    if (ultimaFila <= 1) {
      resultados[nombre] = 'YA VACÍA (solo cabeceras)';
      continue;
    }

    // Borrar todas las filas excepto la primera (cabeceras)
    hoja.deleteRows(2, ultimaFila - 1);
    resultados[nombre] = `VACIADA (${ultimaFila - 1} filas eliminadas)`;
  }

  return {
    ok: true,
    mensaje: 'Base de datos vaciada correctamente',
    detalle: resultados,
    timestamp: new Date().toISOString()
  };
}

/**
 * Función auxiliar para ejecutar desde el editor de Apps Script directamente.
 * Vacía toda la BBDD excepto datos pre-generados (catálogo, plan, sesiones, ejercicios plan).
 * EJECUTAR MANUALMENTE antes de la fecha de inicio real (31/08/2026).
 * Solo borra LOGS (registros de uso durante pruebas).
 */
function resetParaInicio() {
  const resultado = vaciarBaseDatos({
    confirmar: 'VACIAR_TODO',
    hojas_excluir: [
      'ejercicios_catalogo',  // Catálogo de ejercicios (referencia)
      'plan_anual',           // Fases del año (pre-generado)
      'plan_semanal',         // Microciclos (pre-generado)
      'sesiones_plan',        // 192 sesiones pre-generadas
      'ejercicios_plan'       // 1300 ejercicios pre-generados
    ]
  });
  Logger.log(JSON.stringify(resultado, null, 2));
  return resultado;
}

/**
 * Crea todas las hojas con cabeceras.
 * Ejecutar UNA SOLA VEZ al configurar.
 */
function inicializarHojas() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();

  const estructuras = {
    [HOJAS.USUARIOS]: ['user_id', 'str_nombre', 'num_altura_cm', 'str_sexo', 'str_objetivo', 'str_split', 'date_creado', 'date_modificado'],
    [HOJAS.METRICAS_ZEPP]: ['metrica_id', 'user_id', 'date_fecha', 'num_sleep_score', 'num_hr_reposo', 'num_pasos_ayer', 'date_sync'],
    [HOJAS.PESO_LOG]: ['peso_id', 'user_id', 'date_fecha', 'num_peso_kg', 'num_grasa_pct', 'num_musculo_kg', 'str_fuente', 'date_creado'],
    [HOJAS.SESIONES_PLAN]: ['sesion_id', 'user_id', 'date_fecha', 'str_tipo', 'num_semana_meso', 'str_fase', 'num_ajuste_volumen', 'num_duracion_est_min', 'bool_completada', 'date_inicio', 'date_fin', 'date_creado'],
    [HOJAS.EJERCICIOS_PLAN]: ['plan_id', 'sesion_id', 'ejercicio_id', 'num_orden', 'num_series_plan', 'num_reps_plan', 'num_peso_sugerido_kg', 'num_rir_objetivo', 'num_descanso_seg', 'str_notas', 'bool_es_warmup'],
    [HOJAS.EJERCICIOS_LOG]: ['log_id', 'plan_id', 'sesion_id', 'ejercicio_id', 'num_serie', 'num_peso_usado_kg', 'num_reps_completadas', 'num_rir_percibido', 'str_sensacion', 'date_timestamp'],
    [HOJAS.COMIDAS_LOG]: ['comida_id', 'user_id', 'date_fecha', 'str_tipo_comida', 'num_calorias', 'num_proteina_g', 'num_carbos_g', 'num_grasas_g', 'str_notas', 'date_hora'],
    [HOJAS.PLAN_ANUAL]: ['fase_id', 'user_id', 'num_año', 'num_orden', 'str_nombre_fase', 'str_tipo', 'date_inicio', 'date_fin', 'num_semanas', 'num_volumen_objetivo', 'str_rir_rango', 'str_foco_muscular', 'str_objetivo_nutri', 'str_notas'],
    [HOJAS.PLAN_SEMANAL]: ['semana_id', 'fase_id', 'user_id', 'num_semana_año', 'num_semana_fase', 'str_lunes', 'str_martes', 'str_miercoles', 'str_jueves', 'str_viernes', 'str_sabado', 'str_domingo', 'str_rir_semana', 'bool_deload'],
    [HOJAS.EJERCICIOS_CATALOGO]: ['ejercicio_id', 'str_nombre', 'str_nombre_en', 'str_grupo_principal', 'arr_grupos_secundarios', 'str_patron', 'str_equipamiento', 'bool_compuesto', 'bool_favorito', 'bool_excluido', 'str_razon_exclusion', 'str_alternativa']
  };

  for (const [nombre, cabeceras] of Object.entries(estructuras)) {
    let hoja = ss.getSheetByName(nombre);
    if (!hoja) {
      hoja = ss.insertSheet(nombre);
    }

    // Mantener cabeceras y limpiar contenido previo para evitar duplicados acumulados.
    if (hoja.getLastRow() > 1) {
      hoja.getRange(2, 1, hoja.getLastRow() - 1, hoja.getMaxColumns()).clearContent();
    }

    hoja.getRange(1, 1, 1, cabeceras.length).setValues([cabeceras]);
    // Formato cabeceras
    hoja.getRange(1, 1, 1, cabeceras.length).setFontWeight('bold');
    hoja.setFrozenRows(1);
  }

  // Insertar datos iniciales del usuario
  const hojaUsuarios = ss.getSheetByName(HOJAS.USUARIOS);
  appendRowsBatch(hojaUsuarios, [[
    USER_ID, 'Usuario', 188, 'M', 'bulk', 'Push/Pierna/Pull/Hombros+Brazos',
    new Date().toISOString(), new Date().toISOString()
  ]]);

  return { ok: true, mensaje: 'Hojas inicializadas correctamente (10 hojas simplificadas)' };
}

// ═══════════════════════════════════════════════════════════════
// PRE-GENERACIÓN DEL PLAN COMPLETO (ejecutar UNA VEZ post-inicialización)
// Fuente: base_datos.md §7, programacion.md, ENTREGABLE_5_PLAN_EJERCICIOS.html
// ═══════════════════════════════════════════════════════════════

/**
 * Genera TODAS las sesiones del año (31/08/2026 → 31/07/2027).
 * Pre-carga ~192 sesiones + ~1300 ejercicios.
 * Ejecutar DESPUÉS de inicializarHojas().
 * El usuario abre la app cualquier día y su sesión ya está lista.
 */
function generarPlanCompleto() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();

  const hojaPlanAnual = getHoja(HOJAS.PLAN_ANUAL);
  const hojaSesiones = getHoja(HOJAS.SESIONES_PLAN);
  const hojaEjercicios = getHoja(HOJAS.EJERCICIOS_PLAN);
  const hojaSemanal = getHoja(HOJAS.PLAN_SEMANAL);
  const hojaCatalogo = getHoja(HOJAS.EJERCICIOS_CATALOGO);

  // Limpiar datos previos (mantener cabeceras) para que re-ejecutar no multiplique filas.
  [hojaPlanAnual, hojaSesiones, hojaEjercicios, hojaSemanal, hojaCatalogo].forEach(hoja => {
    if (hoja && hoja.getLastRow() > 1) {
      hoja.getRange(2, 1, hoja.getLastRow() - 1, hoja.getMaxColumns()).clearContent();
    }
  });

  // ═══ 1. DEFINIR FASES (plan_anual) ═══
  // Refleja prioridades: P1 Estética V-taper > P2 Postura > P3 Hipertrofia > P4 Flexibilidad
  const FASES = [
    { id: 'FAS_01', nombre: 'Adaptación + Postura', tipo: 'VOL', inicio: '2026-08-31', fin: '2026-09-27', semanas: 4, rir: '3-4', foco: 'Full Body · Correctivos posturales · Wall Angels', nutri: 'bulk', kcal: 3280 },
    { id: 'FAS_02', nombre: 'Hipertrofia I — V-Taper', tipo: 'VOL', inicio: '2026-09-28', fin: '2026-11-08', semanas: 6, rir: '2-3', foco: 'Hombros, Espalda (V-taper) · Postura', nutri: 'bulk', kcal: 3280 },
    { id: 'FAS_03', nombre: 'Deload 1', tipo: 'DELOAD', inicio: '2026-11-09', fin: '2026-11-15', semanas: 1, rir: '5-6', foco: 'Movilidad + Flex · Test Wall Angel', nutri: 'mantener', kcal: 3100 },
    { id: 'FAS_04', nombre: 'Hipertrofia II — Brazos', tipo: 'VOL', inicio: '2026-11-16', fin: '2026-12-27', semanas: 6, rir: '2-3', foco: 'Bíceps, Tríceps, Pecho · Mantener hombros', nutri: 'bulk', kcal: 3280 },
    { id: 'FAS_05', nombre: 'Deload 2', tipo: 'DELOAD', inicio: '2026-12-28', fin: '2027-01-03', semanas: 1, rir: '5-6', foco: 'Movilidad + Flex · Descanso activo', nutri: 'mantener', kcal: 3100 },
    { id: 'FAS_06', nombre: 'Fuerza — Compuestos', tipo: 'FZA', inicio: '2027-01-04', fin: '2027-02-14', semanas: 6, rir: '1-2', foco: 'Press militar, Dominadas, Sentadilla', nutri: 'bulk', kcal: 3280 },
    { id: 'FAS_07', nombre: 'Hipertrofia III — Balance', tipo: 'VOL', inicio: '2027-02-15', fin: '2027-03-28', semanas: 6, rir: '2-3', foco: 'Piernas, Core · Mantener V-taper', nutri: 'bulk', kcal: 3280 },
    { id: 'FAS_08', nombre: 'Deload 3', tipo: 'DELOAD', inicio: '2027-03-29', fin: '2027-04-04', semanas: 1, rir: '5-6', foco: 'Movilidad + Flex · Test postural final', nutri: 'mantener', kcal: 3100 },
    { id: 'FAS_09', nombre: 'Definición', tipo: 'DEF', inicio: '2027-04-05', fin: '2027-05-16', semanas: 6, rir: '2-3', foco: 'Mantener masa · Déficit controlado', nutri: 'cut', kcal: 2460 },
    { id: 'FAS_10', nombre: 'Peak Estético + Mant.', tipo: 'MNT', inicio: '2027-05-17', fin: '2027-07-31', semanas: 11, rir: '2-3', foco: 'Ratio cintura/hombros · Simetría', nutri: 'mantener', kcal: 3050 }
  ];

  // ═══ 2. DEFINIR TEMPLATES DE EJERCICIOS POR FASE ═══
  // Formato: [ejercicio_id, nombre, series, reps_min, reps_max, descanso_seg, notas]
  const TEMPLATES = {
    PUSH_ADAPT: [
      ['EJE_PRESS_INC', 'Press inclinado mancuernas', 3, 10, 12, 150, 'Ligero - sentir el pecho'],
      ['EJE_CRUCES', 'Cruces polea alta', 3, 12, 15, 90, 'Contracción pico'],
      ['EJE_PRESS_HOMB', 'Press hombro mancuernas sentado', 3, 10, 12, 120, ''],
      ['EJE_LAT_SENT', 'Elevaciones laterales sentado', 3, 15, 15, 90, 'Peso mínimo - técnica'],
      ['EJE_FRANC', 'Press francés banco 30°', 2, 12, 15, 90, 'Rango parcial codo'],
      ['EJE_EXT_POLEA', 'Extensión unilateral polea', 2, 15, 15, 60, 'Peso ligero']
    ],
    PIERNA_ADAPT: [
      ['EJE_SENTADILLA', 'Sentadilla barra', 3, 10, 12, 150, 'Solo barra + poco peso'],
      ['EJE_RDL', 'RDL', 3, 10, 12, 150, 'Aprender hip hinge'],
      ['EJE_HIP_THRUST', 'Hip thrust', 3, 12, 15, 120, 'Activación glúteo'],
      ['EJE_EXT_QUAD', 'Extensión cuádriceps', 2, 15, 15, 90, 'Máquina ligero'],
      ['EJE_CURL_FEM', 'Curl femoral tumbado', 2, 12, 15, 90, ''],
      ['EJE_PLANCHA', 'Plancha', 3, 30, 45, 60, 'Core base (segundos)']
    ],
    PULL_ADAPT: [
      ['EJE_DOMINADAS', 'Dominadas asistidas', 3, 6, 8, 150, 'Agarre prono ancho'],
      ['EJE_REMO_NEUTRO', 'Remo polea agarre neutro', 3, 10, 12, 120, ''],
      ['EJE_REMO_ROT', 'Remo unilateral con rotación', 3, 12, 12, 120, 'Aprender rotación'],
      ['EJE_FACE_PULL', 'Face pulls', 3, 15, 20, 90, 'Postura'],
      ['EJE_CURL_Z', 'Curl Z de pie', 3, 12, 12, 90, 'Peso muy ligero'],
      ['EJE_ROT_EXT', 'Rotación externa hombro', 2, 15, 15, 60, 'Manguito rotador']
    ],
    HOMBR_ADAPT: [
      ['EJE_LAT_POLEA', 'Elevaciones laterales polea media', 3, 15, 15, 90, ''],
      ['EJE_LAT_SENT', 'Elevaciones laterales sentado', 2, 15, 15, 90, 'Técnica'],
      ['EJE_CURL_PRED', 'Curl predicador', 3, 12, 12, 90, ''],
      ['EJE_ZOTTMAN', 'Zottman curl', 2, 12, 12, 90, ''],
      ['EJE_EXT_POLEA', 'Extensión unilateral polea', 2, 15, 15, 90, 'Rango parcial codo'],
      ['EJE_FACE_PULL', 'Face pulls', 2, 15, 15, 60, 'Postura']
    ],
    PUSH_H1: [
      ['EJE_PRESS_INC', 'Press inclinado mancuernas', 4, 8, 10, 150, 'Compuesto principal'],
      ['EJE_CRUCES', 'Cruces polea alta', 3, 10, 12, 120, 'Aislamiento pecho'],
      ['EJE_PRESS_HOMB', 'Press hombro mancuernas sentado', 3, 8, 10, 150, 'Deltoides ant+med'],
      ['EJE_LAT_SENT', 'Elevaciones laterales sentado', 4, 12, 15, 90, 'SS con Press francés'],
      ['EJE_FRANC', 'Press francés banco 30°', 4, 10, 12, 90, 'Rango parcial codo'],
      ['EJE_EXT_POLEA', 'Extensión unilateral polea', 3, 12, 15, 0, 'SS con Laterales']
    ],
    PIERNA_H1: [
      ['EJE_SENTADILLA', 'Sentadilla barra', 4, 6, 8, 180, 'Compuesto principal'],
      ['EJE_RDL', 'RDL', 4, 8, 10, 150, 'Isquios + glúteo'],
      ['EJE_HIP_THRUST', 'Hip thrust', 3, 10, 12, 120, 'Glúteos'],
      ['EJE_EXT_QUAD', 'Extensión cuádriceps', 3, 12, 15, 0, 'SS con Curl femoral'],
      ['EJE_CURL_FEM', 'Curl femoral tumbado', 3, 10, 12, 90, 'Isquios extra'],
      ['EJE_HOLLOW', 'Hollow hold/rock', 3, 30, 45, 0, 'SS con Pallof (seg)'],
      ['EJE_PALLOF', 'Press Pallof', 2, 12, 12, 60, 'Anti-rotación por lado']
    ],
    PULL_H1: [
      ['EJE_DOMINADAS', 'Dominadas', 4, 6, 8, 150, 'Asistidas→libres'],
      ['EJE_REMO_NEUTRO', 'Remo polea agarre neutro', 4, 8, 10, 150, 'Tirón horizontal'],
      ['EJE_REMO_ROT', 'Remo unilateral con rotación', 3, 10, 12, 120, 'Espalda media'],
      ['EJE_KELSO', 'Kelso shrug', 3, 12, 15, 0, 'SS con Face pulls'],
      ['EJE_FACE_PULL', 'Face pulls', 3, 15, 20, 90, 'Postura + rear delt'],
      ['EJE_CURL_Z', 'Curl Z de pie', 3, 8, 10, 0, 'SS con Zottman'],
      ['EJE_ZOTTMAN', 'Zottman curl', 3, 10, 12, 90, 'Bíceps + antebrazo']
    ],
    HOMBR_H1: [
      ['EJE_LAT_POLEA', 'Elevaciones laterales polea media', 4, 12, 15, 90, 'Deltoides medial'],
      ['EJE_LAT_SENT', 'Elevaciones laterales sentado', 3, 12, 15, 0, 'SS con Curl predicador'],
      ['EJE_CURL_PRED', 'Curl predicador', 3, 10, 12, 90, 'Bíceps pico'],
      ['EJE_CURL_Z', 'Curl Z de pie', 3, 8, 10, 0, 'SS con Extensión polea'],
      ['EJE_EXT_POLEA', 'Extensión unilateral polea', 3, 12, 15, 90, 'Rango parcial codo'],
      ['EJE_ROT_EXT', 'Rotación externa hombro', 2, 15, 15, 0, 'SS con Face pulls'],
      ['EJE_FACE_PULL', 'Face pulls', 3, 15, 20, 60, 'Postura + rear delt']
    ],
    PUSH_H2: [
      ['EJE_PRESS_INC', 'Press inclinado mancuernas', 4, 8, 10, 150, 'Compuesto principal'],
      ['EJE_CRUCES', 'Cruces polea alta', 3, 10, 12, 120, 'Aislamiento pecho'],
      ['EJE_FONDOS', 'Fondos (lastre progresivo)', 3, 8, 10, 150, 'Especialización pecho'],
      ['EJE_PRESS_HOMB', 'Press hombro mancuernas sentado', 3, 8, 10, 150, ''],
      ['EJE_LAT_SENT', 'Elevaciones laterales sentado', 4, 12, 15, 0, 'SS con Press francés'],
      ['EJE_FRANC', 'Press francés banco 30°', 4, 10, 12, 90, ''],
      ['EJE_EXT_POLEA', 'Extensión unilateral polea', 3, 12, 15, 0, 'SS con Laterales']
    ],
    HOMBR_H2: [
      ['EJE_LAT_POLEA', 'Elevaciones laterales polea media', 4, 12, 15, 90, ''],
      ['EJE_LAT_SENT', 'Elevaciones laterales sentado', 3, 12, 15, 0, 'SS con Curl predicador'],
      ['EJE_CURL_PRED', 'Curl predicador', 3, 10, 12, 90, ''],
      ['EJE_CURL_Z', 'Curl Z de pie', 3, 8, 10, 0, 'SS con Extensión polea'],
      ['EJE_EXT_POLEA', 'Extensión unilateral polea', 3, 12, 15, 90, ''],
      ['EJE_CURL_INC', 'Curl inclinado mancuernas', 3, 10, 12, 0, 'Especialización bíceps'],
      ['EJE_FRANC', 'Press francés banco 30°', 2, 10, 12, 90, 'Extra tríceps'],
      ['EJE_FACE_PULL', 'Face pulls', 3, 15, 20, 60, 'Postura']
    ],
    PUSH_FZA: [
      ['EJE_PRESS_INC', 'Press inclinado mancuernas', 5, 4, 6, 210, 'Carga alta'],
      ['EJE_PRESS_HOMB', 'Press hombro mancuernas sentado', 4, 5, 7, 180, 'Pesado'],
      ['EJE_LAT_SENT', 'Elevaciones laterales sentado', 4, 10, 12, 90, 'Mantener volumen'],
      ['EJE_FRANC', 'Press francés banco 30°', 3, 6, 8, 120, 'Más carga menos reps']
    ],
    PIERNA_FZA: [
      ['EJE_SENTADILLA', 'Sentadilla barra', 5, 4, 6, 270, 'Compuesto rey'],
      ['EJE_RDL', 'RDL', 4, 5, 7, 180, 'Pesado'],
      ['EJE_HIP_THRUST', 'Hip thrust', 3, 6, 8, 150, 'Carga alta'],
      ['EJE_PLANCHA', 'Plancha lastrada', 3, 45, 60, 90, 'Core anti-extensión (seg)']
    ],
    PULL_FZA: [
      ['EJE_DOMINADAS', 'Dominadas lastradas', 5, 4, 6, 210, 'Tirón vertical pesado'],
      ['EJE_REMO_NEUTRO', 'Remo polea agarre neutro', 4, 6, 8, 180, 'Pesado'],
      ['EJE_REMO_ROT', 'Remo unilateral con rotación', 3, 8, 10, 150, ''],
      ['EJE_CURL_Z', 'Curl Z de pie', 3, 6, 8, 120, 'Pesado'],
      ['EJE_FACE_PULL', 'Face pulls', 2, 15, 15, 60, 'Postura mantenimiento']
    ],
    HOMBR_FZA: [
      ['EJE_PRESS_MIL', 'Press militar barra de pie', 4, 5, 7, 180, 'Compuesto hombros pesado'],
      ['EJE_LAT_POLEA', 'Elevaciones laterales polea media', 4, 10, 12, 90, 'Volumen medial'],
      ['EJE_CURL_PRED', 'Curl predicador', 4, 6, 8, 120, 'Pesado'],
      ['EJE_EXT_POLEA', 'Extensión unilateral polea', 3, 8, 10, 120, '']
    ],
    PIERNA_H3: [
      ['EJE_SENTADILLA', 'Sentadilla barra', 4, 6, 8, 180, 'Compuesto principal'],
      ['EJE_RDL', 'RDL', 4, 8, 10, 150, 'Isquios'],
      ['EJE_LEG_PRESS', 'Leg press', 3, 10, 12, 120, 'Cuádriceps extra'],
      ['EJE_HIP_THRUST', 'Hip thrust', 3, 10, 12, 120, 'Glúteos'],
      ['EJE_EXT_QUAD', 'Extensión cuádriceps', 3, 12, 15, 0, 'SS con Curl femoral'],
      ['EJE_CURL_FEM', 'Curl femoral tumbado', 3, 10, 12, 90, ''],
      ['EJE_HOLLOW', 'Hollow hold/rock', 3, 30, 45, 0, 'SS con Pallof (seg)'],
      ['EJE_PALLOF', 'Press Pallof', 3, 12, 12, 0, 'SS con Plancha'],
      ['EJE_PLANCHA', 'Plancha lastrada', 2, 45, 60, 60, 'Core extra (seg)']
    ],
    PUSH_DEF: [
      ['EJE_PRESS_INC', 'Press inclinado mancuernas', 4, 8, 10, 150, 'Mantener carga'],
      ['EJE_CRUCES', 'Cruces polea alta', 3, 12, 15, 0, 'SS con Press hombro'],
      ['EJE_PRESS_HOMB', 'Press hombro mancuernas sentado', 3, 8, 10, 120, ''],
      ['EJE_LAT_SENT', 'Elevaciones laterales sentado', 4, 15, 15, 0, 'SS con Press francés'],
      ['EJE_FRANC', 'Press francés banco 30°', 4, 12, 15, 60, 'Reps más altas'],
      ['EJE_EXT_POLEA', 'Extensión unilateral polea', 3, 15, 15, 0, 'SS con Laterales']
    ]
  };

  // ═══ 3. MAPA FASE → TEMPLATES ═══
  const FASE_TEMPLATES = {
    'FAS_01': { push: 'PUSH_ADAPT', pierna: 'PIERNA_ADAPT', pull: 'PULL_ADAPT', hombr: 'HOMBR_ADAPT' },
    'FAS_02': { push: 'PUSH_H1', pierna: 'PIERNA_H1', pull: 'PULL_H1', hombr: 'HOMBR_H1' },
    'FAS_03': { push: 'PUSH_H1', pierna: 'PIERNA_H1', pull: 'PULL_H1', hombr: 'HOMBR_H1' }, // Deload = mismo template, menos series
    'FAS_04': { push: 'PUSH_H2', pierna: 'PIERNA_H1', pull: 'PULL_H1', hombr: 'HOMBR_H2' },
    'FAS_05': { push: 'PUSH_H2', pierna: 'PIERNA_H1', pull: 'PULL_H1', hombr: 'HOMBR_H2' }, // Deload
    'FAS_06': { push: 'PUSH_FZA', pierna: 'PIERNA_FZA', pull: 'PULL_FZA', hombr: 'HOMBR_FZA' },
    'FAS_07': { push: 'PUSH_H1', pierna: 'PIERNA_H3', pull: 'PULL_H1', hombr: 'HOMBR_H1' },
    'FAS_08': { push: 'PUSH_H1', pierna: 'PIERNA_H3', pull: 'PULL_H1', hombr: 'HOMBR_H1' }, // Deload
    'FAS_09': { push: 'PUSH_DEF', pierna: 'PIERNA_H1', pull: 'PULL_H1', hombr: 'HOMBR_H1' },
    'FAS_10': { push: 'PUSH_H1', pierna: 'PIERNA_H1', pull: 'PULL_H1', hombr: 'HOMBR_H1' }
  };

  // ═══ 4. SPLIT SEMANAL (programacion.md) ═══
  // LUN=Push, MAR=Nadar, MIÉ=Pierna, JUE=Nadar, VIE=Pull, SÁB=Hombros, DOM=Descanso
  const DIAS_GYM = { 1: 'push', 3: 'pierna', 5: 'pull', 6: 'hombr' }; // 0=DOM, 1=LUN...

  // ═══ 5. ESCRIBIR PLAN_ANUAL ═══
  const filasPlanAnual = [];
  FASES.forEach((fase, idx) => {
    filasPlanAnual.push([
      fase.id, USER_ID, 2026, idx + 1, fase.nombre, fase.tipo,
      fase.inicio, fase.fin, fase.semanas, 16, fase.rir, fase.foco, fase.nutri, ''
    ]);
  });
  appendRowsBatch(hojaPlanAnual, filasPlanAnual);

  // ═══ 6. GENERAR SESIONES + EJERCICIOS ═══
  const filasSesiones = [];
  const filasEjercicios = [];
  const filasSemanal = [];

  let sesionCount = 0;
  let ejercicioCount = 0;
  let semanaAño = 0;

  for (const fase of FASES) {
    const fechaInicio = new Date(fase.inicio);
    const fechaFin = new Date(fase.fin);
    const esDeload = fase.tipo === 'DELOAD';
    const templates = FASE_TEMPLATES[fase.id];

    // Iterar día a día dentro de la fase
    let fechaActual = new Date(fechaInicio);
    let semanaFase = 1;
    let diaEnSemana = 0;

    while (fechaActual <= fechaFin) {
      const diaSemana = fechaActual.getDay(); // 0=DOM, 1=LUN...
      const fechaStr = Utilities.formatDate(fechaActual, 'Europe/Madrid', 'yyyy-MM-dd');

      // RIR de esta semana (necesita estar en scope para plan_semanal que se escribe el domingo)
      let rirSemana = fase.rir;
      if (!esDeload && fase.semanas >= 4) {
        const semMod = ((semanaFase - 1) % 3); // 0, 1, 2 → ciclo de 3 semanas
        if (semMod === 0) rirSemana = '3-4';
        else if (semMod === 1) rirSemana = '2-3';
        else rirSemana = '1-2';
      }

      // ¿Es día de gym?
      if (DIAS_GYM[diaSemana]) {
        const tipoSesion = DIAS_GYM[diaSemana]; // push/pierna/pull/hombr
        const tipoDisplay = { push: 'Push', pierna: 'Pierna', pull: 'Pull', hombr: 'Hombros+Brazos' }[tipoSesion];

        // Crear sesión
        sesionCount++;
        const sesionId = `SES_${fechaStr.replace(/-/g, '')}_${String(sesionCount).padStart(3, '0')}`;

        filasSesiones.push([
          sesionId, USER_ID, fechaStr, tipoDisplay, semanaFase, fase.nombre,
          1.0, '', 75, false, '', '', new Date().toISOString()
        ]);

        // Crear ejercicios de la sesión
        const templateName = templates[tipoSesion];
        const ejerciciosTemplate = TEMPLATES[templateName];

        if (ejerciciosTemplate) {
          ejerciciosTemplate.forEach((ej, orden) => {
            ejercicioCount++;
            const planId = `PLA_${fechaStr.replace(/-/g, '')}_${String(ejercicioCount).padStart(4, '0')}`;

            // En deload: reducir series ×0.6 (Bompa 2009)
            let series = ej[2];
            if (esDeload) {
              series = Math.max(2, Math.ceil(series * 0.6));
            }

            const repsStr = ej[3] === ej[4] ? String(ej[3]) : `${ej[3]}-${ej[4]}`;

            filasEjercicios.push([
              planId, sesionId, ej[0], orden + 1, series, repsStr,
              0, // num_peso_sugerido_kg = 0 (desconocido hasta primera sesión)
              parseInt(rirSemana) || 3, // RIR numérico
              ej[5], // descanso_seg
              ej[6], // notas
              false  // no es warmup
            ]);
          });
        }
      }

      // Control de semana
      if (diaSemana === 0) { // Domingo = fin de semana
        semanaAño++;
        // Escribir plan_semanal
        filasSemanal.push([
          `SEM_${String(semanaAño).padStart(3, '0')}`, fase.id, USER_ID,
          semanaAño, semanaFase,
          'Push', 'Natación', 'Pierna', 'Natación', 'Pull', 'Hombros+Brazos', 'Descanso',
          rirSemana || fase.rir, esDeload
        ]);
        semanaFase++;
      }

      // Siguiente día
      fechaActual.setDate(fechaActual.getDate() + 1);
    }
  }

  // Persistir lotes principales antes del catálogo.
  appendRowsBatch(hojaSesiones, filasSesiones);
  appendRowsBatch(hojaEjercicios, filasEjercicios);
  appendRowsBatch(hojaSemanal, filasSemanal);

  // ═══ 7. ESCRIBIR CATÁLOGO DE EJERCICIOS ═══
  const CATALOGO = [
    ['EJE_PRESS_INC', 'Press inclinado mancuernas', 'Incline DB Press', 'Pecho', '["Hombro","Tríceps"]', 'Empuje horizontal', 'Mancuernas,Banco inclinado', true, true, false, '', ''],
    ['EJE_CRUCES', 'Cruces polea alta', 'High Cable Fly', 'Pecho', '[]', 'Empuje horizontal', 'Poleas cruce', false, true, false, '', ''],
    ['EJE_FONDOS', 'Fondos (lastre progresivo)', 'Weighted Dips', 'Pecho', '["Tríceps","Hombro"]', 'Empuje vertical', 'Barras paralelas', true, true, false, '', ''],
    ['EJE_PRESS_HOMB', 'Press hombro mancuernas sentado', 'Seated DB Shoulder Press', 'Hombros', '["Tríceps"]', 'Empuje vertical', 'Mancuernas,Banco 90°', true, false, false, '', ''],
    ['EJE_PRESS_MIL', 'Press militar barra de pie', 'Standing Barbell OHP', 'Hombros', '["Tríceps","Core"]', 'Empuje vertical', 'Barra olímpica', true, false, false, '', ''],
    ['EJE_LAT_SENT', 'Elevaciones laterales sentado', 'Seated Lateral Raise', 'Hombros', '[]', 'Empuje lateral', 'Mancuernas,Banco', false, true, false, '', ''],
    ['EJE_LAT_POLEA', 'Elevaciones laterales polea media', 'Cable Lateral Raise', 'Hombros', '[]', 'Empuje lateral', 'Polea', false, true, false, '', ''],
    ['EJE_FRANC', 'Press francés banco 30°', 'Incline Skullcrusher', 'Tríceps', '[]', 'Extensión', 'Barra Z,Banco 30°', false, true, false, '', ''],
    ['EJE_EXT_POLEA', 'Extensión unilateral polea', 'Single Arm Cable Extension', 'Tríceps', '[]', 'Extensión', 'Polea alta', false, true, false, '', ''],
    ['EJE_SENTADILLA', 'Sentadilla barra', 'Barbell Squat', 'Cuádriceps', '["Glúteos","Isquios"]', 'Extensión rodilla', 'Barra,Rack', true, true, false, '', ''],
    ['EJE_RDL', 'RDL', 'Romanian Deadlift', 'Isquios', '["Glúteos","Espalda baja"]', 'Extensión cadera', 'Barra o Mancuernas', true, true, false, '', ''],
    ['EJE_HIP_THRUST', 'Hip thrust', 'Barbell Hip Thrust', 'Glúteos', '["Isquios"]', 'Extensión cadera', 'Barra,Banco', true, true, false, '', ''],
    ['EJE_LEG_PRESS', 'Leg press', 'Leg Press', 'Cuádriceps', '["Glúteos"]', 'Extensión rodilla', 'Leg press máquina', true, false, false, '', ''],
    ['EJE_EXT_QUAD', 'Extensión cuádriceps', 'Leg Extension', 'Cuádriceps', '[]', 'Extensión rodilla', 'Máquina extensión', false, false, false, '', ''],
    ['EJE_CURL_FEM', 'Curl femoral tumbado', 'Lying Leg Curl', 'Isquios', '[]', 'Flexión rodilla', 'Máquina curl', false, false, false, '', ''],
    ['EJE_DOMINADAS', 'Dominadas', 'Pull-ups', 'Espalda', '["Bíceps"]', 'Tirón vertical', 'Barra dominadas', true, true, false, '', ''],
    ['EJE_REMO_NEUTRO', 'Remo polea agarre neutro', 'Neutral Grip Cable Row', 'Espalda', '["Bíceps"]', 'Tirón horizontal', 'Polea,Agarre neutro', true, true, false, '', ''],
    ['EJE_REMO_ROT', 'Remo unilateral con rotación', 'Single Arm Row w/ Rotation', 'Espalda', '["Bíceps","Core"]', 'Tirón horizontal', 'Mancuerna', true, true, false, '', ''],
    ['EJE_KELSO', 'Kelso shrug', 'Kelso Shrug', 'Espalda', '[]', 'Tirón', 'Banco + mancuernas', false, true, false, '', ''],
    ['EJE_FACE_PULL', 'Face pulls', 'Face Pulls', 'Hombros', '["Trapecios"]', 'Tirón horizontal', 'Polea + cuerda', false, false, false, '', ''],
    ['EJE_CURL_Z', 'Curl Z de pie', 'EZ Bar Curl', 'Bíceps', '[]', 'Flexión codo', 'Barra Z', false, true, false, '', ''],
    ['EJE_ZOTTMAN', 'Zottman curl', 'Zottman Curl', 'Bíceps', '["Antebrazo"]', 'Flexión codo', 'Mancuernas', false, true, false, '', ''],
    ['EJE_CURL_PRED', 'Curl predicador', 'Preacher Curl', 'Bíceps', '[]', 'Flexión codo', 'Máquina predicador', false, true, false, '', ''],
    ['EJE_CURL_INC', 'Curl inclinado mancuernas', 'Incline DB Curl', 'Bíceps', '[]', 'Flexión codo', 'Mancuernas,Banco 45°', false, false, false, '', ''],
    ['EJE_ROT_EXT', 'Rotación externa hombro', 'External Rotation', 'Manguito rotador', '[]', 'Rotación externa', 'Mancuerna o Polea', false, false, false, '', ''],
    ['EJE_HOLLOW', 'Hollow hold/rock', 'Hollow Hold', 'Core', '[]', 'Anti-extensión', 'Suelo', false, true, false, '', ''],
    ['EJE_PALLOF', 'Press Pallof', 'Pallof Press', 'Core', '[]', 'Anti-rotación', 'Polea', false, true, false, '', ''],
    ['EJE_PLANCHA', 'Plancha (lastrada)', 'Weighted Plank', 'Core', '[]', 'Anti-extensión', 'Suelo,Disco', false, false, false, '', '']
  ];

  appendRowsBatch(hojaCatalogo, CATALOGO);

  // ═══ 8. RESULTADO ═══
  Logger.log(`Plan generado: ${sesionCount} sesiones, ${ejercicioCount} ejercicios, ${CATALOGO.length} en catálogo`);
  return {
    ok: true,
    mensaje: 'Plan completo generado',
    sesiones: sesionCount,
    ejercicios: ejercicioCount,
    catalogo: CATALOGO.length,
    fases: FASES.length,
    periodo: '31/08/2026 → 31/07/2027'
  };
}
