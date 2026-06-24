package com.fitbase.data.health;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.records.NutritionRecord;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.time.TimeRangeFilter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Lee datos REALES de Health Connect:
 * - Pasos (Zepp/Amazfit → Health Connect)
 * - Nutrición (FatSecret → Health Connect)
 *
 * Se usa incluso en modo demo para verificar que la integración funciona.
 */
public class HealthConnectReader {

    private final Context context;

    public HealthConnectReader(Context context) {
        this.context = context.getApplicationContext();
    }

    public static boolean isAvailable(Context context) {
        try {
            int status = HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata");
            return status == HealthConnectClient.SDK_AVAILABLE;
        } catch (Exception e) {
            return false;
        }
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
                HealthConnectClient client = HealthConnectClient.getOrCreate(context);

                Instant inicioHoy = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
                Instant ahora = Instant.now();
                TimeRangeFilter filtro = TimeRangeFilter.between(inicioHoy, ahora);

                // Leer pasos
                ReadRecordsRequest<StepsRecord> reqPasos = new ReadRecordsRequest.Builder<>(StepsRecord.class)
                        .setTimeRangeFilter(filtro)
                        .build();

                // Health Connect usa coroutines, llamamos de forma bloqueante desde hilo background
                List<StepsRecord> resultPasos = BlockingHealthConnect.readRecords(client, reqPasos);
                if (resultPasos != null) {
                    for (StepsRecord r : resultPasos) {
                        datos.pasos += (int) r.getCount();
                    }
                }

                // Leer nutrición
                ReadRecordsRequest<NutritionRecord> reqNutri = new ReadRecordsRequest.Builder<>(NutritionRecord.class)
                        .setTimeRangeFilter(filtro)
                        .build();

                List<NutritionRecord> resultNutri = BlockingHealthConnect.readRecords(client, reqNutri);
                if (resultNutri != null) {
                    for (NutritionRecord r : resultNutri) {
                        if (r.getEnergy() != null) {
                            datos.caloriasConsumidas += (int) r.getEnergy().getInKilocalories();
                        }
                        if (r.getProtein() != null) {
                            datos.proteinaG += (int) r.getProtein().getInGrams();
                        }
                        if (r.getTotalCarbohydrate() != null) {
                            datos.carbosG += (int) r.getTotalCarbohydrate().getInGrams();
                        }
                        if (r.getTotalFat() != null) {
                            datos.grasasG += (int) r.getTotalFat().getInGrams();
                        }
                    }
                }
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
