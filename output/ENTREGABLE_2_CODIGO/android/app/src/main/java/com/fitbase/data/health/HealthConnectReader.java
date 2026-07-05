package com.fitbase.data.health;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * Lee datos REALES de Health Connect:
 * - Pasos (Zepp/Amazfit → Health Connect)
 * - Nutrición (FatSecret → Health Connect)
 * - Peso corporal (balanza o Mi Fitness → Health Connect)
 * - Sueño (Zepp sleep tracking → Health Connect)
 * - FC reposo (Zepp heart rate → Health Connect)
 *
 * Delega a HealthConnectBridge.kt (Kotlin-first SDK).
 */
public class HealthConnectReader {

    private final Context context;

    public HealthConnectReader(Context context) {
        this.context = context.getApplicationContext();
    }

    public static boolean isAvailable(Context context) {
        return HealthConnectBridge.isAvailable(context);
    }

    /**
     * Resultado con pasos y nutrición del día actual.
     */
    public static class DatosHoy {
        public int pasos = 0;
        public int caloriasConsumidas = 0;
        public int proteinaG = 0;
        public int carbosG = 0;
        public int grasasG = 0;
    }

    /**
     * Resultado con datos de recuperación para progresión y motor de pesos.
     */
    public static class DatosRecuperacion {
        public java.util.List<PesoEntry> pesos = new java.util.ArrayList<>();
        public java.util.List<SleepEntry> suenos = new java.util.ArrayList<>();
        public java.util.List<HrEntry> fcReposo = new java.util.ArrayList<>();
    }

    public static class PesoEntry {
        public String fecha;
        public double kg;
        /** % grasa corporal (BodyFatRecord) — null si HC no tiene dato ese día. */
        public Double grasaPct;
        /** % hidratación, calculada como masa de agua / peso — null si falta alguno. */
        public Double hidratacionPct;
        public PesoEntry(String fecha, double kg, Double grasaPct, Double hidratacionPct) {
            this.fecha = fecha; this.kg = kg;
            this.grasaPct = grasaPct; this.hidratacionPct = hidratacionPct;
        }
    }

    public static class SleepEntry {
        public String fecha;
        public int duracionMin;
        /** Score ESTIMADO 0-100 (no el de Zepp — Health Connect no lo tiene). */
        public int score;
        public int deepMin;
        public int remMin;
        public int lightMin;
        public SleepEntry(String fecha, int duracionMin, int score, int deepMin, int remMin, int lightMin) {
            this.fecha = fecha; this.duracionMin = duracionMin; this.score = score;
            this.deepMin = deepMin; this.remMin = remMin; this.lightMin = lightMin;
        }
    }

    public static class HrEntry {
        public String fecha;
        public int bpm;
        public HrEntry(String fecha, int bpm) { this.fecha = fecha; this.bpm = bpm; }
    }

    /**
     * Lee pasos y nutrición del día de hoy desde Health Connect.
     * Ejecuta en hilo de fondo, devuelve resultado vía callback en main thread.
     */
    public void leerDatosHoy(OnDatosListener listener) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            DatosHoy datos = new DatosHoy();
            try {
                HealthConnectBridge.HealthData hcData = HealthConnectBridge.readTodayData(context);
                datos.pasos = hcData.pasos;
                datos.caloriasConsumidas = hcData.caloriasConsumidas;
                datos.proteinaG = hcData.proteinaG;
                datos.carbosG = hcData.carbosG;
                datos.grasasG = hcData.grasasG;
            } catch (Exception e) {
                // HC no disponible o sin permisos — datos quedan en 0
            }
            mainHandler.post(() -> listener.onDatos(datos));
        }).start();
    }

    /**
     * Lee datos de recuperación (peso, sueño, FC reposo) de los últimos N días.
     * Para pantalla de progresión y sincronización con backend.
     */
    public void leerDatosRecuperacion(int diasAtras, OnRecuperacionListener listener) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            DatosRecuperacion datos = new DatosRecuperacion();
            try {
                HealthConnectBridge.RecoveryData recovery = HealthConnectBridge.readRecoveryData(context, diasAtras);

                for (HealthConnectBridge.PesoEntry pe : recovery.pesosKg) {
                    datos.pesos.add(new PesoEntry(pe.fecha, pe.kg, pe.grasaPct, pe.hidratacionPct));
                }
                for (HealthConnectBridge.SleepEntry se : recovery.suenos) {
                    datos.suenos.add(new SleepEntry(se.fecha, se.duracionMin, se.score,
                            se.deepMin, se.remMin, se.lightMin));
                }
                for (HealthConnectBridge.HrEntry hr : recovery.fcReposo) {
                    datos.fcReposo.add(new HrEntry(hr.fecha, hr.bpm));
                }
            } catch (Exception e) {
                // Sin datos de recuperación — arrays vacíos
            }
            mainHandler.post(() -> listener.onDatos(datos));
        }).start();
    }

    public interface OnDatosListener {
        void onDatos(DatosHoy datos);
    }

    public interface OnRecuperacionListener {
        void onDatos(DatosRecuperacion datos);
    }
}

