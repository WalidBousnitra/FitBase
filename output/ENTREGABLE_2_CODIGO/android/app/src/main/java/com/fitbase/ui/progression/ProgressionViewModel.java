package com.fitbase.ui.progression;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.health.HealthConnectReader;
import com.fitbase.data.model.MetricasProgresionResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para la pantalla de progresión de métricas.
 * Carga datos históricos: peso, sueño, FC reposo, volumen.
 *
 * Flujo:
 *   1. Intenta backend (datos persistidos en Sheets).
 *   2. Si backend vacío/error → lee DIRECTAMENTE de Health Connect.
 *   3. Muestra lo que haya disponible (nunca "sin datos" si HC tiene algo).
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
        cargando.setValue(true);

        // Primero intentar backend
        ApiClient.getApi().getProgresionMetricas("progresion_metricas", dias)
                .enqueue(new Callback<MetricasProgresionResponse>() {
                    @Override
                    public void onResponse(Call<MetricasProgresionResponse> call,
                                           Response<MetricasProgresionResponse> response) {
                        if (response.isSuccessful() && response.body() != null && tieneData(response.body())) {
                            MetricasProgresionResponse body = response.body();
                            Log.d(TAG, "Backend OK: peso=" + size(body.peso) + " zepp=" + size(body.zepp) + " vol=" + size(body.volumenEntreno));
                            error.postValue(null);
                            datos.postValue(body);
                            cargando.postValue(false);
                        } else {
                            // Backend vacío o no disponible → intentar Health Connect
                            Log.w(TAG, "Backend vacío/error, intentando Health Connect");
                            cargarDesdeHealthConnect(dias);
                        }
                    }

                    @Override
                    public void onFailure(Call<MetricasProgresionResponse> call, Throwable t) {
                        Log.w(TAG, "Backend falló: " + t.getMessage() + " → intentando HC");
                        cargarDesdeHealthConnect(dias);
                    }
                });
    }

    /**
     * Carga progresión directamente de Health Connect.
     * Esto funciona aunque el backend esté caído o sin datos.
     */
    private void cargarDesdeHealthConnect(int dias) {
        if (!HealthConnectReader.isAvailable(getApplication())) {
            cargando.postValue(false);
            error.postValue("Sin conexión al backend y Health Connect no disponible");
            Log.e(TAG, "Ni backend ni HC disponibles");
            return;
        }

        HealthConnectReader reader = new HealthConnectReader(getApplication());
        reader.leerDatosRecuperacion(dias, datosHC -> {
            MetricasProgresionResponse resp = new MetricasProgresionResponse();
            resp.diasSolicitados = dias;

            // Convertir pesos HC → formato progresión
            resp.peso = new ArrayList<>();
            for (HealthConnectReader.PesoEntry pe : datosHC.pesos) {
                MetricasProgresionResponse.PesoEntry pd = new MetricasProgresionResponse.PesoEntry();
                pd.fecha = pe.fecha;
                pd.pesoKg = (float) pe.kg;
                resp.peso.add(pd);
            }

            // Convertir sueño HC → formato zepp
            resp.zepp = new ArrayList<>();
            for (HealthConnectReader.SleepEntry se : datosHC.suenos) {
                MetricasProgresionResponse.ZeppEntry zd = new MetricasProgresionResponse.ZeppEntry();
                zd.fecha = se.fecha;
                zd.sleepScore = se.score;
                zd.sleepHoras = se.duracionMin / 60.0f;
                resp.zepp.add(zd);
            }

            // FC reposo desde HC
            for (HealthConnectReader.HrEntry hr : datosHC.fcReposo) {
                // Buscar o crear el ZeppEntry de ese día
                MetricasProgresionResponse.ZeppEntry existente = null;
                for (MetricasProgresionResponse.ZeppEntry z : resp.zepp) {
                    if (hr.fecha.equals(z.fecha)) { existente = z; break; }
                }
                if (existente != null) {
                    existente.hrReposo = hr.bpm;
                } else {
                    MetricasProgresionResponse.ZeppEntry zd = new MetricasProgresionResponse.ZeppEntry();
                    zd.fecha = hr.fecha;
                    zd.hrReposo = hr.bpm;
                    resp.zepp.add(zd);
                }
            }

            // Resumen
            resp.resumen = new MetricasProgresionResponse.Resumen();
            if (!datosHC.pesos.isEmpty()) {
                HealthConnectReader.PesoEntry ultimo = datosHC.pesos.get(datosHC.pesos.size() - 1);
                HealthConnectReader.PesoEntry primero = datosHC.pesos.get(0);
                resp.resumen.pesoActual = (float) ultimo.kg;
                resp.resumen.pesoInicio = (float) primero.kg;
            }
            if (!datosHC.suenos.isEmpty()) {
                int totalScore = 0;
                for (HealthConnectReader.SleepEntry s : datosHC.suenos) totalScore += s.score;
                resp.resumen.sleepMedia = totalScore / datosHC.suenos.size();
            }

            int totalEntries = size(resp.peso) + size(resp.zepp);
            if (totalEntries > 0) {
                Log.d(TAG, "HC OK: peso=" + size(resp.peso) + " zepp=" + size(resp.zepp));
                error.postValue(null);
                datos.postValue(resp);
            } else {
                Log.w(TAG, "HC también vacío");
                error.postValue("No hay datos de progresión. Usa Zepp/FatSecret para registrar datos.");
                datos.postValue(null);
            }
            cargando.postValue(false);
        });
    }

    private boolean tieneData(MetricasProgresionResponse r) {
        return (r.peso != null && !r.peso.isEmpty())
                || (r.zepp != null && !r.zepp.isEmpty())
                || (r.volumenEntreno != null && !r.volumenEntreno.isEmpty());
    }

    private int size(List<?> list) {
        return list != null ? list.size() : 0;
    }
}
