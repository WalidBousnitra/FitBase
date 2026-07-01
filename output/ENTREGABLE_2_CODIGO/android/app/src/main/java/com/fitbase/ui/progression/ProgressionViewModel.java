package com.fitbase.ui.progression;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.model.MetricasProgresionResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para la pantalla de progresion de metricas.
 * Carga datos historicos: peso, grasa, sueno, HRV, volumen.
 * En demo/error → muestra datos ficticios.
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
                        if (response.isSuccessful() && response.body() != null
                                && response.body().resumen != null) {
                            datos.postValue(response.body());
                        } else {
                            datos.postValue(crearDatosDemo(dias));
                        }
                    }

                    @Override
                    public void onFailure(Call<MetricasProgresionResponse> call, Throwable t) {
                        cargando.postValue(false);
                        datos.postValue(crearDatosDemo(dias));
                    }
                });
    }

    private MetricasProgresionResponse crearDatosDemo(int dias) {
        MetricasProgresionResponse resp = new MetricasProgresionResponse();
        resp.diasSolicitados = dias;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        // Peso: tendencia descendente leve (72→70.5 en 30 días)
        resp.peso = new ArrayList<>();
        float pesoBase = 72.0f;
        for (int i = dias; i >= 0; i -= 3) {
            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -i);
            MetricasProgresionResponse.PesoEntry pe = new MetricasProgresionResponse.PesoEntry();
            pe.fecha = sdf.format(cal.getTime());
            pe.pesoKg = pesoBase - ((dias - i) * 0.05f) + (float)(Math.random() * 0.4 - 0.2);
            pe.grasaPct = 18.5f - ((dias - i) * 0.02f);
            resp.peso.add(pe);
        }

        // Zepp: sueño y pasos
        resp.zepp = new ArrayList<>();
        for (int i = dias; i >= 0; i--) {
            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -i);
            MetricasProgresionResponse.ZeppEntry ze = new MetricasProgresionResponse.ZeppEntry();
            ze.fecha = sdf.format(cal.getTime());
            ze.sleepScore = 65 + (int)(Math.random() * 25);
            ze.sleepHoras = 6.5f + (float)(Math.random() * 1.5);
            ze.sleepDeepMin = 45 + (int)(Math.random() * 30);
            ze.hrvRmssd = 35 + (int)(Math.random() * 20);
            ze.hrReposo = 58 + (int)(Math.random() * 10);
            ze.stressAvg = 30 + (int)(Math.random() * 25);
            ze.pasos = 5000 + (int)(Math.random() * 6000);
            resp.zepp.add(ze);
        }

        // Volumen de entreno
        resp.volumenEntreno = new ArrayList<>();
        for (int i = dias; i >= 0; i -= 2) {
            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -i);
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) continue;
            MetricasProgresionResponse.VolumenEntry ve = new MetricasProgresionResponse.VolumenEntry();
            ve.fecha = sdf.format(cal.getTime());
            ve.volumenKg = 3000 + (int)(Math.random() * 2000);
            resp.volumenEntreno.add(ve);
        }

        // Resumen
        resp.resumen = new MetricasProgresionResponse.Resumen();
        resp.resumen.pesoActual = 70.8f;
        resp.resumen.pesoInicio = 72.0f;
        resp.resumen.grasaActual = 17.8f;
        resp.resumen.sleepMedia = 78;
        resp.resumen.pasosMedia = 7200;

        return resp;
    }
}
