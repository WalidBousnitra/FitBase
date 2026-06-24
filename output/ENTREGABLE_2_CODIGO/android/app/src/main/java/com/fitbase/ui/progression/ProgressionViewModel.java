package com.fitbase.ui.progression;

import android.app.Application;

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
 * Carga datos historicos: peso, grasa, sueno, HRV, volumen.
 */
public class ProgressionViewModel extends AndroidViewModel {

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
                            datos.postValue(response.body());
                        } else {
                            error.postValue("Error al cargar datos");
                        }
                    }

                    @Override
                    public void onFailure(Call<MetricasProgresionResponse> call, Throwable t) {
                        cargando.postValue(false);
                        error.postValue("Sin conexion: " + t.getMessage());
                    }
                });
    }
}
