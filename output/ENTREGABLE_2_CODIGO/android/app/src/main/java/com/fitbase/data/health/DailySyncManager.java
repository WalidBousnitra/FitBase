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
    // Distinta de KEY_ULTIMA_SYNC (esa depende de que haya datos de sueño/FC/
    // peso que enviar) — esta se actualiza SIEMPRE que se abre la app, para
    // detectar el cambio de día natural con total fiabilidad.
    private static final String KEY_ULTIMO_DIA_ABIERTO = "ultimo_dia_abierto";

    /**
     * Sincroniza sueño/FC reposo/peso/grasa si hoy aún no se ha hecho, usando
     * datos de Health Connect YA LEÍDOS por el llamador (evita leer HC dos
     * veces). BLOQUEANTE — llamar desde un hilo de fondo (red síncrona).
     * Pasos NO se manda aquí — ver {@link #actualizarPasosDelDia}.
     *
     * Llamado desde SplashActivity (arranque en frío). Ver también la
     * sobrecarga que recibe {@link HealthConnectReader.DatosRecuperacion},
     * llamada desde Home en cada refresco — SplashActivity solo se ejecuta
     * en un arranque en frío real; si el usuario reabre la app desde
     * recientes (tarea ya viva) Splash nunca se vuelve a ejecutar ese día,
     * así que sin esa segunda vía sueño/FC/peso podían quedarse sin
     * sincronizar días enteros aunque la gating por fecha diga "1 vez al día".
     *
     * @param hoyData      resultado de {@link HealthConnectBridge#readTodayData}
     * @param recuperacion resultado de {@link HealthConnectBridge#readRecoveryData}
     *                     (con al menos 2 días de margen para capturar lo de anoche)
     */
    public static void sincronizarSiHaceFalta(Context context,
                                               HealthConnectBridge.HealthData hoyData,
                                               HealthConnectBridge.RecoveryData recuperacion) {
        if (hoyData == null || recuperacion == null) {
            Log.d(TAG, "Sin datos de Health Connect — no se sincroniza");
            return;
        }
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Integer hrReposo = recuperacion.fcReposo.isEmpty() ? null
                : recuperacion.fcReposo.get(recuperacion.fcReposo.size() - 1).bpm;
        Integer sleepScore = recuperacion.suenos.isEmpty() ? null
                : recuperacion.suenos.get(recuperacion.suenos.size() - 1).score;
        Double pesoKg = null, grasaPct = null;
        if (!recuperacion.pesosKg.isEmpty()) {
            HealthConnectBridge.PesoEntry ultimo = recuperacion.pesosKg.get(recuperacion.pesosKg.size() - 1);
            if (hoy.equals(ultimo.fecha)) {
                pesoKg = ultimo.kg;
                grasaPct = ultimo.grasaPct;
            }
        }
        intentarSincronizar(context, hoy, hrReposo, sleepScore, pesoKg, grasaPct);
    }

    /**
     * Misma sincronización diaria que {@link #sincronizarSiHaceFalta(Context,
     * HealthConnectBridge.HealthData, HealthConnectBridge.RecoveryData)}, pero
     * a partir de datos que Home ya leyó vía {@link HealthConnectReader}
     * (evita una segunda lectura de Health Connect). Se llama en cada
     * refresco de Home (onResume) — la gating por fecha en
     * {@link #intentarSincronizar} hace que sea barato si ya se sincronizó
     * hoy, así que es seguro llamarla a menudo.
     */
    public static void sincronizarSiHaceFalta(Context context,
                                               HealthConnectReader.DatosRecuperacion recuperacion) {
        if (recuperacion == null) return;
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Integer hrReposo = recuperacion.fcReposo.isEmpty() ? null
                : recuperacion.fcReposo.get(recuperacion.fcReposo.size() - 1).bpm;
        Integer sleepScore = recuperacion.suenos.isEmpty() ? null
                : recuperacion.suenos.get(recuperacion.suenos.size() - 1).score;
        Double pesoKg = null, grasaPct = null;
        if (!recuperacion.pesos.isEmpty()) {
            HealthConnectReader.PesoEntry ultimo = recuperacion.pesos.get(recuperacion.pesos.size() - 1);
            if (hoy.equals(ultimo.fecha)) {
                pesoKg = ultimo.kg;
                grasaPct = ultimo.grasaPct;
            }
        }
        intentarSincronizar(context, hoy, hrReposo, sleepScore, pesoKg, grasaPct);
    }

    private static void intentarSincronizar(Context context, String hoy, Integer hrReposo,
                                             Integer sleepScore, Double pesoKg, Double grasaPct) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (hoy.equals(prefs.getString(KEY_ULTIMA_SYNC, ""))) {
            Log.d(TAG, "Ya sincronizado hoy (" + hoy + ") — nada que hacer");
            return;
        }
        if (hrReposo == null && sleepScore == null && pesoKg == null) {
            Log.d(TAG, "Sin datos nuevos de Health Connect que sincronizar");
            return;
        }

        Map<String, Object> datos = new HashMap<>();
        datos.put("accion", "guardar_metricas");
        datos.put("fecha", hoy);
        if (hrReposo != null) datos.put("hr_reposo", hrReposo);
        if (sleepScore != null) datos.put("sleep_score", sleepScore);
        if (pesoKg != null) datos.put("peso_kg", pesoKg);
        if (grasaPct != null) datos.put("grasa_pct", grasaPct);

        try {
            boolean ok = postSincrono(context, ApiClient.getApi().guardarMetricas(datos), datos, "guardar_metricas");
            if (ok) {
                prefs.edit().putString(KEY_ULTIMA_SYNC, hoy).apply();
                Log.d(TAG, "Sync diario completado para " + hoy);
            } else {
                Log.d(TAG, "Sync diario: fallo al enviar — se reintentará (SyncManager)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en sync diario", e);
        }
    }

    /**
     * Detecta si ha cambiado el día natural desde la última vez que se abrió
     * la app y, si es así, CIERRA el día anterior: lee de Health Connect el
     * total de pasos de ESE día completo (00:00-24:00, ya no "hasta ahora")
     * y lo manda como valor final. {@link #actualizarPasosDelDia} solo
     * corre mientras la app está abierta — un paseo nocturno después de la
     * última vez que se usó la app ese día se quedaría fuera si no se hace
     * esta comprobación al abrir la app al día siguiente.
     * BLOQUEANTE (red síncrona) — llamar desde un hilo de fondo.
     */
    public static void cerrarDiaAnteriorSiHaceFalta(Context context) {
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String diaAnterior = prefs.getString(KEY_ULTIMO_DIA_ABIERTO, "");

        if (!diaAnterior.isEmpty() && !diaAnterior.equals(hoy)) {
            try {
                int pasosFinales = HealthConnectBridge.readStepsForDate(context, diaAnterior);
                if (pasosFinales > 0) {
                    Map<String, Object> datos = new HashMap<>();
                    datos.put("accion", "guardar_metricas");
                    datos.put("fecha", diaAnterior);
                    datos.put("pasos", pasosFinales);
                    postSincrono(context, ApiClient.getApi().guardarMetricas(datos), datos, "cierre_pasos_dia_anterior");
                }
            } catch (Exception e) {
                Log.w(TAG, "Error cerrando pasos del día anterior (" + diaAnterior + ")", e);
            }
        }
        prefs.edit().putString(KEY_ULTIMO_DIA_ABIERTO, hoy).apply();
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
