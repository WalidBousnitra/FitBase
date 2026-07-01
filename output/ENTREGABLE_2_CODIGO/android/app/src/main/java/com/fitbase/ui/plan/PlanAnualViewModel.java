package com.fitbase.ui.plan;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.model.PlanAnualResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para Plan Anual.
 * Sin datos ficticios: el plan siempre viene del backend.
 */
public class PlanAnualViewModel extends ViewModel {

    private static final String TAG = "PlanAnualVM";

    private final MutableLiveData<PlanAnualResponse> planAnual = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<PlanAnualResponse> getPlanAnual() { return planAnual; }
    public LiveData<Boolean> isCargando() { return cargando; }
    public LiveData<String> getError() { return error; }

    public void cargarPlan() {
        cargando.setValue(true);
        error.setValue(null);
        ApiClient.getApi().getPlanAnual("plan_anual").enqueue(new Callback<PlanAnualResponse>() {
            @Override
            public void onResponse(Call<PlanAnualResponse> call, Response<PlanAnualResponse> response) {
                cargando.postValue(false);
                if (response.isSuccessful() && response.body() != null
                        && response.body().fases != null && !response.body().fases.isEmpty()) {
                    Log.d(TAG, "Plan anual OK. fases=" + response.body().fases.size());
                    error.postValue(null);
                    planAnual.postValue(response.body());
                } else {
                    Log.w(TAG, "Plan anual vacio o respuesta no valida. code=" + response.code());
                    error.postValue("No hay plan anual cargado en la base de datos");
                    planAnual.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<PlanAnualResponse> call, Throwable t) {
                cargando.postValue(false);
                Log.e(TAG, "Error cargando plan anual", t);
                error.postValue("Error cargando plan anual: " + t.getMessage());
                planAnual.postValue(null);
            }
        });
    }
}
