package com.fitbase.ui.progression;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.cache.AppDataCache;
import com.fitbase.data.model.MetricasProgresionResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para la pantalla de progresión de métricas.
 * Carga datos históricos: peso, sueño, FC reposo, volumen.
 *
 * Fuente ÚNICA: la BBDD (backend/Google Sheets). Health Connect NO se lee
 * aquí — {@link com.fitbase.data.health.DailySyncManager} ya se encarga de
 * copiar Health Connect → BBDD una vez al día desde SplashActivity, así que
 * la BBDD siempre tiene lo último. Si no hay datos, es porque aún no se ha
 * sincronizado ningún día (p.ej. antes de empezar a usar la app de verdad).
 */
public class ProgressionViewModel extends AndroidViewModel {

    private static final String TAG = "ProgressionVM";

    private final MutableLiveData<MetricasProgresionResponse> datos = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ProgressionViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<MetricasProgresionResponse> getDatos() { return datos; }
    public LiveData<Boolean> isCargando() { return cargando; }
    public LiveData<String> getError() { return error; }

    public void cargar(int dias) {
        // Si SplashActivity ya precargó los 7 días por defecto, usarlo al instante
        // (sin spinner) en vez de repetir la llamada de red.
        if (dias == 7 && AppDataCache.getProgresion7d() != null && tieneData(AppDataCache.getProgresion7d())) {
            Log.d(TAG, "Progresión 7d servida desde cache de Splash");
            error.postValue(null);
            datos.postValue(AppDataCache.getProgresion7d());
            cargando.postValue(false);
            return;
        }

        cargando.setValue(true);

        ApiClient.getApi().getProgresionMetricas("progresion_metricas", dias)
                .enqueue(new Callback<MetricasProgresionResponse>() {
                    @Override
                    public void onResponse(Call<MetricasProgresionResponse> call,
                                           Response<MetricasProgresionResponse> response) {
                        cargando.postValue(false);
                        if (response.isSuccessful() && response.body() != null && tieneData(response.body())) {
                            MetricasProgresionResponse body = response.body();
                            Log.d(TAG, "Backend OK: zepp=" + size(body.zepp) + " vol=" + size(body.volumenEntreno));
                            error.postValue(null);
                            datos.postValue(body);
                        } else {
                            Log.w(TAG, "Backend sin datos para " + dias + " días");
                            error.postValue("Aún no hay histórico en la base de datos para este rango.\n\n" +
                                    "Los datos de Health Connect (Zepp/Mi Fitness) se sincronizan a la BBDD " +
                                    "una vez al día al abrir la app. Si acabas de empezar, dale unos días, o " +
                                    "usa rellenarDatosFicticios() en Apps Script para probar con datos de prueba.");
                            datos.postValue(null);
                        }
                    }

                    @Override
                    public void onFailure(Call<MetricasProgresionResponse> call, Throwable t) {
                        cargando.postValue(false);
                        Log.w(TAG, "Backend falló: " + t.getMessage());
                        error.postValue("No se pudo conectar con la base de datos: " + t.getMessage());
                        datos.postValue(null);
                    }
                });
    }

    private boolean tieneData(MetricasProgresionResponse r) {
        return (r.zepp != null && !r.zepp.isEmpty())
                || (r.volumenEntreno != null && !r.volumenEntreno.isEmpty());
    }

    private int size(List<?> list) {
        return list != null ? list.size() : 0;
    }
}
