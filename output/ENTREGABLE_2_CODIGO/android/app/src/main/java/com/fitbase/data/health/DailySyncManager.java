package com.fitbase.data.health;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.model.GenericResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Sincroniza los datos de Health Connect (sueño, pasos, FC reposo, peso, %
 * grasa) hacia el backend (Google Sheets, metricas_zepp — centraliza TODO lo
 * de Health Connect en una sola fila por día), para que Progresión tenga
 * histórico real en la BBDD sin depender de Health Connect cada vez que se
 * abre la app.
 *
 * DOS RITMOS DISTINTOS, a propósito:
 *   - Sueño/FC reposo/peso/% grasa: YA ESTÁN CERRADOS cuando abres la app por
 *     la mañana (dormiste, te pesaste) — sincronizar una vez al día (gating
 *     por SharedPreferences) es suficiente, {@link #sincronizarSiHaceFalta}.
 *   - Pasos: se ACUMULAN durante el día. Sincronizarlos solo 1 vez (p.ej. si
 *     abres la app a las 8am) dejaría la fila de hoy congelada con un valor
 *     bajo el resto del día — afectaría al bonus de +175kcal por >12000 pasos
 *     (motor_dieta.md §6) y a la media de pasos en Progresión. Por eso
 *     {@link #actualizarPasosDelDia} NO tiene gating — se llama cada vez que
 *     Home lee Health Connect (onResume), para que la fila de hoy siempre
 *     refleje el total más reciente.
 *
 * El backend hace upsert por fecha con merge parcial (ver Codigo.gs
 * guardarMetricas_/upsertPorFecha_), así que llamar a los dos métodos por
 * separado y varias veces al día no pisa datos ni duplica filas.
 *
 * sleep_score es un ESTIMADO calculado por HealthConnectBridge a partir de
 * datos crudos (duración/fases) — no el score real de Zepp, que HC no tiene.
 * No sincroniza: hidratación, vo2max, hrv, estrés — fuera del esquema actual
 * de metricas_zepp; estrés sigue siendo entrada manual (ver metricas_subjetivas).
 */
public class DailySyncManager {

    private static final String TAG = "DailySyncManager";
    private static final String PREFS = "fitbase_sync";
    private static final String KEY_ULTIMA_SYNC = "ultima_sync_fecha";

    /**
     * Sincroniza sueño/FC reposo/peso/grasa si hoy aún no se ha hecho, usando
     * datos de Health Connect YA LEÍDOS por el llamador (evita leer HC dos
     * veces). BLOQUEANTE — llamar desde un hilo de fondo (red síncrona).
     * Pasos NO se manda aquí — ver {@link #actualizarPasosDelDia}.
     *
     * @param hoyData      resultado de {@link HealthConnectBridge#readTodayData}
     * @param recuperacion resultado de {@link HealthConnectBridge#readRecoveryData}
     *                     (con al menos 2 días de margen para capturar lo de anoche)
     */
    public static void sincronizarSiHaceFalta(Context context,
                                               HealthConnectBridge.HealthData hoyData,
                                               HealthConnectBridge.RecoveryData recuperacion) {
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (hoy.equals(prefs.getString(KEY_ULTIMA_SYNC, ""))) {
            Log.d(TAG, "Ya sincronizado hoy (" + hoy + ") — nada que hacer");
            return;
        }
        if (hoyData == null || recuperacion == null) {
            Log.d(TAG, "Sin datos de Health Connect — no se sincroniza");
            return;
        }

        try {
            boolean ok = sincronizarMetricas(context, recuperacion, hoy);
            if (ok) {
                prefs.edit().putString(KEY_ULTIMA_SYNC, hoy).apply();
                Log.d(TAG, "Sync diario completado para " + hoy);
            } else {
                Log.d(TAG, "Sync diario: Health Connect no tenía datos nuevos que enviar");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en sync diario", e);
        }
    }

    /**
     * Actualiza SOLO los pasos de hoy — SIN gating, se puede llamar cada vez
     * que Home refresca datos de Health Connect (onResume). El backend hace
     * merge parcial, así que no afecta a sueño/FC/peso ya guardados hoy.
     */
    public static void actualizarPasosDelDia(Context context, int pasosHoy) {
        if (pasosHoy <= 0) return;
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Map<String, Object> datos = new HashMap<>();
        datos.put("accion", "guardar_metricas");
        datos.put("fecha", hoy);
        datos.put("pasos", pasosHoy);
        try {
            postSincrono(context, ApiClient.getApi().guardarMetricas(datos), datos, "guardar_metricas (pasos)");
        } catch (Exception e) {
            Log.w(TAG, "Error actualizando pasos", e);
        }
    }

    private static boolean sincronizarMetricas(Context context, HealthConnectBridge.RecoveryData recuperacion, String hoy) {
        Integer hrReposo = null;
        if (!recuperacion.fcReposo.isEmpty()) {
            hrReposo = recuperacion.fcReposo.get(recuperacion.fcReposo.size() - 1).bpm;
        }
        Integer sleepScore = null;
        if (!recuperacion.suenos.isEmpty()) {
            sleepScore = recuperacion.suenos.get(recuperacion.suenos.size() - 1).score;
        }
        // Peso/grasa: solo si la última pesada de Health Connect es de HOY.
        // Si aún no has sincronizado la báscula (o simplemente no te has
        // pesado hoy), el último registro puede ser de días atrás — sin este
        // check se guardaría esa pesada vieja COMO SI fuera de hoy, y
        // getPesoActual_() ya no podría distinguir un dato real de hoy de un
        // dato viejo mal etiquetado. Si no hay pesada de hoy, se deja vacío
        // y el backend sigue usando el último peso real disponible.
        Double pesoKg = null, grasaPct = null;
        if (!recuperacion.pesosKg.isEmpty()) {
            HealthConnectBridge.PesoEntry ultimo = recuperacion.pesosKg.get(recuperacion.pesosKg.size() - 1);
            if (hoy.equals(ultimo.fecha)) {
                pesoKg = ultimo.kg;
                grasaPct = ultimo.grasaPct;
            }
        }

        if (hrReposo == null && sleepScore == null && pesoKg == null) {
            return false;
        }

        Map<String, Object> datos = new HashMap<>();
        datos.put("accion", "guardar_metricas");
        datos.put("fecha", hoy);
        if (hrReposo != null) datos.put("hr_reposo", hrReposo);
        if (sleepScore != null) datos.put("sleep_score", sleepScore);
        if (pesoKg != null) datos.put("peso_kg", pesoKg);
        if (grasaPct != null) datos.put("grasa_pct", grasaPct);

        return postSincrono(context, ApiClient.getApi().guardarMetricas(datos), datos, "guardar_metricas");
    }

    /**
     * Si falla (sin red, o el servidor devuelve error), encola en Room para
     * que SyncManager lo reintente al recuperar conexión — así ni el sueño/
     * peso/FC de hoy ni los pasos se pierden por un corte de red puntual.
     */
    private static boolean postSincrono(Context context, Call<GenericResponse> call,
                                         Map<String, Object> datos, String nombre) {
        try {
            Response<GenericResponse> response = call.execute(); // ya estamos en background
            boolean ok = response.isSuccessful();
            Log.d(TAG, nombre + " → " + (ok ? "OK" : "HTTP " + response.code()));
            if (!ok) com.fitbase.data.local.SyncManager.encolar(context, datos);
            return ok;
        } catch (Exception e) {
            Log.w(TAG, nombre + " falló: " + e.getMessage());
            com.fitbase.data.local.SyncManager.encolar(context, datos);
            return false;
        }
    }
}
