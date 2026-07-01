package com.fitbase.data.health;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * Lee datos REALES de Health Connect:
 * - Pasos (Zepp/Amazfit → Health Connect)
 * - Nutrición (FatSecret → Health Connect)
 *
 * Delega a HealthConnectBridge.kt (Kotlin-first SDK).
 * Se usa incluso en modo demo para verificar que la integración funciona.
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

    public interface OnDatosListener {
        void onDatos(DatosHoy datos);
    }
}
