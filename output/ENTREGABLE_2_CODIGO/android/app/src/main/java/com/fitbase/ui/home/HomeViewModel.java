package com.fitbase.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.api.FitBaseApi;
import com.fitbase.data.health.HealthConnectReader;
import com.fitbase.data.model.VistaMañanaResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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

    public HomeViewModel(@NonNull Application application) {
        super(application);
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

    // ─── Carga de datos ───────────────────────────────────

    public void cargarDatosDelDia() {
        verificarModoDemo();
        cargarVistaMañana();
        cargarDatosHealthConnect();
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
            reader.leerDatosHoy(datos -> {
                pasosActuales.postValue(datos.pasos);
                caloriasConsumidas.postValue(datos.caloriasConsumidas);
                proteinaConsumida.postValue(datos.proteinaG);
                carbosConsumidos.postValue(datos.carbosG);
                grasasConsumidas.postValue(datos.grasasG);
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
