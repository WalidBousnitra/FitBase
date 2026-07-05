package com.fitbase.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.api.FitBaseApi;
import com.fitbase.data.cache.AppDataCache;
import com.fitbase.data.health.HealthConnectBridge;
import com.fitbase.data.health.HealthConnectReader;
import com.fitbase.data.model.GenericResponse;
import com.fitbase.data.model.VistaMañanaResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para pantalla principal (Vista Mañana).
 * Carga todo de un solo endpoint: sueño, macros, cardio, movilidad, tipo día.
 * Pre-inicio: muestra datos locales de Health Connect hasta que el plan arranca.
 */
public class HomeViewModel extends AndroidViewModel {

    private static final String FECHA_INICIO_PLAN = "2026-08-31";

    private final MutableLiveData<VistaMañanaResponse> vistaMañana = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> modoDemo = new MutableLiveData<>(false);

    // Macros consumidas (se leen de Health Connect / FatSecret)
    private final MutableLiveData<Integer> caloriasConsumidas = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> proteinaConsumida = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> carbosConsumidos = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> grasasConsumidas = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> aguaConsumida = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> pasosActuales = new MutableLiveData<>(0);
    // FC reposo real (RestingHeartRateRecord — lo calcula el propio reloj, no se estima nada).
    private final MutableLiveData<Integer> hcFcReposo = new MutableLiveData<>(null);
    // Sueño: datos literales de Health Connect (minutos por fase) + un score
    // ESTIMADO calculado a partir de ellos (HealthConnectBridge). No es el
    // score real de Zepp (HC no lo tiene) — se marca como estimado en la UI.
    private final MutableLiveData<Integer> hcSleepScore = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> hcSleepDuracionMin = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> hcSleepDeepMin = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> hcSleepRemMin = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> hcSleepLightMin = new MutableLiveData<>(null);

    // Refresco automático: así las macros restantes y los pasos se actualizan solos
    // mientras se usa la app, sin tener que cerrarla y volver a abrirla.
    private static final long INTERVALO_REFRESCO_MS = 15000; // 15 segundos
    private final android.os.Handler refreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            cargarDatosHealthConnect();
            refreshHandler.postDelayed(this, INTERVALO_REFRESCO_MS);
        }
    };

    public HomeViewModel(@NonNull Application application) {
        super(application);
        pintarDesdeCache();
        refreshHandler.post(refreshRunnable);
    }

    /**
     * Si SplashActivity ya precargó datos (backend + Health Connect), los pinta
     * al instante — así Home no muestra "—"/spinners mientras se usa la app,
     * la espera ya ocurrió detrás de la pantalla de carga inicial.
     */
    private void pintarDesdeCache() {
        if (!AppDataCache.isCargaInicialCompleta()) return;

        if (AppDataCache.getVistaMañana() != null) {
            vistaMañana.setValue(AppDataCache.getVistaMañana());
        }

        HealthConnectBridge.HealthData hoy = AppDataCache.getHealthHoy();
        if (hoy != null) {
            pasosActuales.setValue(hoy.pasos);
            caloriasConsumidas.setValue(hoy.caloriasConsumidas);
            proteinaConsumida.setValue(hoy.proteinaG);
            carbosConsumidos.setValue(hoy.carbosG);
            grasasConsumidas.setValue(hoy.grasasG);
        }

        HealthConnectBridge.RecoveryData recuperacion = AppDataCache.getHealthRecuperacion30d();
        if (recuperacion != null) {
            if (!recuperacion.suenos.isEmpty()) {
                HealthConnectBridge.SleepEntry ultimo = recuperacion.suenos.get(recuperacion.suenos.size() - 1);
                hcSleepScore.setValue(ultimo.score);
                hcSleepDuracionMin.setValue(ultimo.duracionMin);
                hcSleepDeepMin.setValue(ultimo.deepMin);
                hcSleepRemMin.setValue(ultimo.remMin);
                hcSleepLightMin.setValue(ultimo.lightMin);
            }
            if (!recuperacion.fcReposo.isEmpty()) {
                HealthConnectBridge.HrEntry ultimo = recuperacion.fcReposo.get(recuperacion.fcReposo.size() - 1);
                hcFcReposo.setValue(ultimo.bpm);
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    // ─── LiveData getters ─────────────────────────────────

    public LiveData<VistaMañanaResponse> getVistaMañana() { return vistaMañana; }
    public LiveData<Boolean> getCargando() { return cargando; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> isModoDemo() { return modoDemo; }
    public LiveData<Integer> getCaloriasConsumidas() { return caloriasConsumidas; }
    public LiveData<Integer> getProteinaConsumida() { return proteinaConsumida; }
    public LiveData<Integer> getCarbosConsumidos() { return carbosConsumidos; }
    public LiveData<Integer> getGrasasConsumidas() { return grasasConsumidas; }
    public LiveData<Integer> getAguaConsumida() { return aguaConsumida; }
    public LiveData<Integer> getPasosActuales() { return pasosActuales; }
    public LiveData<Integer> getHcFcReposo() { return hcFcReposo; }
    public LiveData<Integer> getHcSleepScore() { return hcSleepScore; }
    public LiveData<Integer> getHcSleepDuracionMin() { return hcSleepDuracionMin; }
    public LiveData<Integer> getHcSleepDeepMin() { return hcSleepDeepMin; }
    public LiveData<Integer> getHcSleepRemMin() { return hcSleepRemMin; }
    public LiveData<Integer> getHcSleepLightMin() { return hcSleepLightMin; }

    // ─── Carga de datos ───────────────────────────────────

    public void cargarDatosDelDia() {
        verificarModoDemo();
        cargarVistaMañana();
        cargarDatosHealthConnect();
    }

    // ─── Energía / estrés subjetivos (diálogo nocturno, ver HomeActivity) ───

    private final MutableLiveData<Boolean> metricasSubjetivasGuardadas = new MutableLiveData<>();
    public LiveData<Boolean> getMetricasSubjetivasGuardadas() { return metricasSubjetivasGuardadas; }

    public void guardarMetricasSubjetivas(int energia, int estres, String notas) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("accion", "guardar_metricas_subjetivas");
        datos.put("energia", energia);
        datos.put("estres", estres);
        if (notas != null && !notas.isEmpty()) datos.put("notas", notas);

        ApiClient.getApi().guardarMetricasSubjetivas(datos).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (!response.isSuccessful()) {
                    com.fitbase.data.local.SyncManager.encolar(getApplication(), datos);
                }
                // Se marca "guardado" igualmente aunque haya ido a la cola —
                // ya no hace falta reintentar desde el diálogo (solo se
                // pregunta una vez al día), SyncManager se encarga del resto.
                metricasSubjetivasGuardadas.postValue(true);
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                com.fitbase.data.local.SyncManager.encolar(getApplication(), datos);
                metricasSubjetivasGuardadas.postValue(true);
            }
        });
    }

    private void verificarModoDemo() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date inicio = sdf.parse(FECHA_INICIO_PLAN);
            modoDemo.postValue(new Date().before(inicio));
        } catch (ParseException e) {
            modoDemo.postValue(false);
        }
    }

    private void cargarVistaMañana() {
        cargando.postValue(true);

        ApiClient.getApi().getVistaMañana("vista_manana").enqueue(new Callback<VistaMañanaResponse>() {
            @Override
            public void onResponse(Call<VistaMañanaResponse> call, Response<VistaMañanaResponse> response) {
                cargando.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    vistaMañana.postValue(response.body());
                } else {
                    error.postValue("Error al cargar datos del servidor");
                }
            }

            @Override
            public void onFailure(Call<VistaMañanaResponse> call, Throwable t) {
                cargando.postValue(false);
                error.postValue("Sin conexión — mostrando datos locales");
            }
        });
    }

    /**
     * Lee datos reales de Health Connect: pasos actuales y nutrición (de FatSecret).
     * Estos se restan de los objetivos para mostrar "macros restantes".
     */
    private void cargarDatosHealthConnect() {
        try {
            HealthConnectReader reader = new HealthConnectReader(getApplication());
            // 1. Datos del día (pasos y nutrición)
            reader.leerDatosHoy(datos -> {
                pasosActuales.postValue(datos.pasos);
                caloriasConsumidas.postValue(datos.caloriasConsumidas);
                proteinaConsumida.postValue(datos.proteinaG);
                carbosConsumidos.postValue(datos.carbosG);
                grasasConsumidas.postValue(datos.grasasG);
                // Sin gating de "1 vez al día" — los pasos suben durante el
                // día, así que la fila de hoy en metricas_zepp se mantiene al
                // día cada vez que Home refresca (onResume), no solo al abrir
                // la app por la mañana (ver DailySyncManager).
                com.fitbase.data.health.DailySyncManager.actualizarPasosDelDia(getApplication(), datos.pasos);
            });

            // 2. Datos de recuperación (sueño y FC reposo) - leemos últimos 2 días para asegurar hoy
            reader.leerDatosRecuperacion(2, datos -> {
                if (datos.suenos != null && !datos.suenos.isEmpty()) {
                    // Tomamos el último registro (el más reciente; la lista viene ordenada por fecha)
                    HealthConnectReader.SleepEntry ultimo = datos.suenos.get(datos.suenos.size() - 1);
                    hcSleepScore.postValue(ultimo.score);
                    hcSleepDuracionMin.postValue(ultimo.duracionMin);
                    hcSleepDeepMin.postValue(ultimo.deepMin);
                    hcSleepRemMin.postValue(ultimo.remMin);
                    hcSleepLightMin.postValue(ultimo.lightMin);
                }
                if (datos.fcReposo != null && !datos.fcReposo.isEmpty()) {
                    HealthConnectReader.HrEntry ultimo = datos.fcReposo.get(datos.fcReposo.size() - 1);
                    hcFcReposo.postValue(ultimo.bpm);
                }
            });
        } catch (Exception ignored) {
            // Health Connect no disponible — mantener 0
        }
    }

    // ─── Helpers ──────────────────────────────────────────

    public String getSaludoHora() {
        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hora < 12) return "Buenos días";
        if (hora < 20) return "Buenas tardes";
        return "Buenas noches";
    }

    public String getFechaFormateada() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "ES"));
        return sdf.format(new Date());
    }
}
