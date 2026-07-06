package com.fitbase.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.fitbase.R;
import com.fitbase.data.api.ApiClient;
import com.fitbase.data.cache.AppDataCache;
import com.fitbase.data.health.DailySyncManager;
import com.fitbase.data.health.HealthConnectBridge;
import com.fitbase.data.model.MetricasProgresionResponse;
import com.fitbase.data.model.PlanAnualResponse;
import com.fitbase.data.model.VistaMañanaResponse;
import com.fitbase.ui.home.HomeActivity;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla de carga inicial.
 *
 * Precarga en segundo plano TODO lo que Home/Progresión/Plan Anual necesitan
 * (backend: vista_manana, plan_anual, progresion_metricas 7d — la ventana por
 * defecto de Progresión; Health Connect: datos de hoy + histórico 30d) y lo
 * deja en {@link AppDataCache}. Solo cuando
 * termina (o pasa un tiempo máximo de espera) navega a Home — así, mientras se
 * usa la app, las pantallas no tienen que esperar a la red.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long TIMEOUT_CARGA_MS = 8000;

    @SuppressWarnings("unchecked")
    private final ActivityResultLauncher<Set<String>> hcPermLauncher =
            registerForActivityResult(HealthConnectBridge.getPermissionContract(),
                    granted -> precargarYContinuar());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        com.fitbase.util.InsetsHelper.aplicarInsetsSistema(this);

        if (HealthConnectBridge.isAvailable(this) && !HealthConnectBridge.hasPermissions(this)) {
            try {
                hcPermLauncher.launch(HealthConnectBridge.getRequiredPermissions());
                return; // continuamos en el callback del launcher
            } catch (Exception ignored) {
                // Si el launcher falla, seguimos sin permisos (los datos HC quedarán vacíos)
            }
        }
        precargarYContinuar();
    }

    private void precargarYContinuar() {
        new Thread(() -> {
            // 1) Health Connect PRIMERO (bloqueante, en este mismo hilo de fondo).
            try {
                if (HealthConnectBridge.isAvailable(this) && HealthConnectBridge.hasPermissions(this)) {
                    HealthConnectBridge.HealthData hoyData = HealthConnectBridge.readTodayData(this);
                    HealthConnectBridge.RecoveryData recuperacion30d = HealthConnectBridge.readRecoveryData(this, 30);
                    AppDataCache.setHealthHoy(hoyData);
                    AppDataCache.setHealthRecuperacion30d(recuperacion30d);

                    // 2) Si ha cambiado el día natural desde la última apertura, cierra
                    // el día anterior con el total de pasos definitivo (paseo nocturno
                    // tras la última apertura de ese día incluido).
                    DailySyncManager.cerrarDiaAnteriorSiHaceFalta(this);

                    // 3) Sync diario HC → BBDD (idempotente) ANTES de pedir al backend,
                    // para que el histórico que lea Progresión ya incluya lo de hoy.
                    DailySyncManager.sincronizarSiHaceFalta(this, hoyData, recuperacion30d);
                }
            } catch (Exception ignored) {
                // Sin HC — Home/Progresión mostrarán lo que ya haya en la BBDD
            }

            // 3) Backend: 3 llamadas en paralelo (Retrofit ya es asíncrono)
            CountDownLatch latch = new CountDownLatch(3);

            ApiClient.getApi().getVistaMañana("vista_manana").enqueue(new Callback<VistaMañanaResponse>() {
                @Override
                public void onResponse(Call<VistaMañanaResponse> call, Response<VistaMañanaResponse> response) {
                    if (response.isSuccessful()) AppDataCache.setVistaMañana(response.body());
                    latch.countDown();
                }
                @Override
                public void onFailure(Call<VistaMañanaResponse> call, Throwable t) { latch.countDown(); }
            });

            ApiClient.getApi().getPlanAnual("plan_anual").enqueue(new Callback<PlanAnualResponse>() {
                @Override
                public void onResponse(Call<PlanAnualResponse> call, Response<PlanAnualResponse> response) {
                    if (response.isSuccessful()) AppDataCache.setPlanAnual(response.body());
                    latch.countDown();
                }
                @Override
                public void onFailure(Call<PlanAnualResponse> call, Throwable t) { latch.countDown(); }
            });

            ApiClient.getApi().getProgresionMetricas("progresion_metricas", 7)
                    .enqueue(new Callback<MetricasProgresionResponse>() {
                        @Override
                        public void onResponse(Call<MetricasProgresionResponse> call,
                                                Response<MetricasProgresionResponse> response) {
                            if (response.isSuccessful()) AppDataCache.setProgresion7d(response.body());
                            latch.countDown();
                        }
                        @Override
                        public void onFailure(Call<MetricasProgresionResponse> call, Throwable t) { latch.countDown(); }
                    });

            try {
                latch.await(TIMEOUT_CARGA_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {}

            AppDataCache.marcarCargaCompleta();

            new Handler(Looper.getMainLooper()).post(() -> {
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                finish();
            });
        }).start();
    }
}
