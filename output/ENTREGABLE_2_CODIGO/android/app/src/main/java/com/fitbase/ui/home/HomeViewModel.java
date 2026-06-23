package com.fitbase.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.model.AusenciaResponse;
import com.fitbase.data.model.MacrosResponse;
import com.fitbase.data.model.SesionResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para pantalla mañana.
 * Carga macros (con sync FatSecret), sesión, ausencia auto y modo demo.
 */
public class HomeViewModel extends ViewModel {

    private static final String FECHA_INICIO = "2026-08-31"; // biometria.md

    private final MutableLiveData<MacrosResponse> macros = new MutableLiveData<>();
    private final MutableLiveData<SesionResponse> sesionHoy = new MutableLiveData<>();
    private final MutableLiveData<Boolean> modoDemo = new MutableLiveData<>();
    private final MutableLiveData<AusenciaResponse> ausenciaDetectada = new MutableLiveData<>();

    public LiveData<MacrosResponse> getMacros() { return macros; }
    public LiveData<SesionResponse> getSesionHoy() { return sesionHoy; }
    public LiveData<Boolean> isModoDemo() { return modoDemo; }
    public LiveData<AusenciaResponse> getAusenciaDetectada() { return ausenciaDetectada; }

    public void cargarDatosDelDia() {
        verificarModoDemo();
        cargarMacros();
        cargarSesion();
        syncNutricionDesdeHealthConnect();
    }

    /**
     * Verifica si hay días sin abrir la app → asumir no entrenó.
     * Redistribuye volumen automáticamente.
     */
    public void checkAusencia() {
        if (Boolean.TRUE.equals(modoDemo.getValue())) return;

        ApiClient.getApi().checkAusencia("check_ausencia").enqueue(new Callback<AusenciaResponse>() {
            @Override
            public void onResponse(Call<AusenciaResponse> call, Response<AusenciaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ausenciaDetectada.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<AusenciaResponse> call, Throwable t) {
                // Sin conectividad, no verificar
            }
        });
    }

    /**
     * Registra ausencia extendida (≥1 semana) indicada manualmente por el usuario.
     */
    public void registrarAusenciaExtendida() {
        // TODO: Abrir date picker, luego llamar API registrar_ausencia
    }

    /**
     * Lee datos de nutrición desde Health Connect (donde FatSecret deposita las comidas).
     * Luego envía los datos al backend para guardar en Sheets.
     * Flujo: FatSecret App → Health Connect → FitBase lee → API → Sheets
     */
    private void syncNutricionDesdeHealthConnect() {
        if (Boolean.TRUE.equals(modoDemo.getValue())) return;
        // La lectura real de Health Connect se hace en HealthConnectRepository
        // que lee NutritionRecord del día y lo envía al backend vía POST sync_nutricion
    }

    private void verificarModoDemo() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date fechaInicio = sdf.parse(FECHA_INICIO);
            Date hoy = new Date();
            modoDemo.setValue(hoy.before(fechaInicio));
        } catch (ParseException e) {
            modoDemo.setValue(false);
        }
    }

    private void cargarMacros() {
        // En modo demo, usar datos mock
        if (Boolean.TRUE.equals(modoDemo.getValue())) {
            MacrosResponse mock = new MacrosResponse();
            mock.caloriasObjetivo = 3280;
            mock.proteinaG = 156;
            mock.carbosG = 488;
            mock.grasasG = 78;
            mock.aguaMl = 3200;
            mock.esDiaEntreno = true;
            mock.fase = "bulk";
            macros.setValue(mock);
            return;
        }

        ApiClient.getApi().getMacrosHoy("macros_hoy").enqueue(new Callback<MacrosResponse>() {
            @Override
            public void onResponse(Call<MacrosResponse> call, Response<MacrosResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    macros.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<MacrosResponse> call, Throwable t) {
                // Fallback offline - usar valores por defecto (motor_dieta.md)
                MacrosResponse fallback = new MacrosResponse();
                fallback.caloriasObjetivo = 3280;
                fallback.proteinaG = 156;
                fallback.carbosG = 488;
                fallback.grasasG = 78;
                fallback.aguaMl = 3200;
                macros.postValue(fallback);
            }
        });
    }

    private void cargarSesion() {
        if (Boolean.TRUE.equals(modoDemo.getValue())) {
            // Mock de sesión demo
            sesionHoy.setValue(null); // Sin sesión en demo por defecto
            return;
        }

        ApiClient.getApi().getSesionHoy("sesion_hoy").enqueue(new Callback<SesionResponse>() {
            @Override
            public void onResponse(Call<SesionResponse> call, Response<SesionResponse> response) {
                if (response.isSuccessful()) {
                    sesionHoy.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<SesionResponse> call, Throwable t) {
                sesionHoy.postValue(null);
            }
        });
    }
}
