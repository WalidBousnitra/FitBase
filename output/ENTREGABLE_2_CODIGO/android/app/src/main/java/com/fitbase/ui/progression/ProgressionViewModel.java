package com.fitbase.ui.progression;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.model.MetricasProgresionResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para la pantalla de progresion de metricas.
 * Carga datos historicos: peso, grasa, hidratacion, visceral, sueno, pasos y volumen.
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
        ApiClient.getApi().getProgresionMetricas("progresion_metricas", dias)
                .enqueue(new Callback<MetricasProgresionResponse>() {
                    @Override
                    public void onResponse(Call<MetricasProgresionResponse> call,
                                           Response<MetricasProgresionResponse> response) {
                        cargando.postValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            MetricasProgresionResponse body = response.body();
                            int peso = body.peso != null ? body.peso.size() : 0;
                            int zepp = body.zepp != null ? body.zepp.size() : 0;
                            int vol = body.volumenEntreno != null ? body.volumenEntreno.size() : 0;
                            Log.d(TAG, "Progresion OK dias=" + dias + " peso=" + peso + " zepp=" + zepp + " volumen=" + vol);
                            error.postValue(null);
                            datos.postValue(body);
                        } else {
                            Log.w(TAG, "Progresion vacia/invalida code=" + response.code());
                            error.postValue("No hay datos de progresion disponibles");
                            datos.postValue(null);
                        }
                    }

                    @Override
                    public void onFailure(Call<MetricasProgresionResponse> call, Throwable t) {
                        cargando.postValue(false);
                        Log.e(TAG, "Error cargando progresion", t);
                        error.postValue("Error cargando progresion: " + t.getMessage());
                        datos.postValue(null);
                    }
                });
    }
}
