package com.fitbase.ui.plan;

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

    private final MutableLiveData<PlanAnualResponse> planAnual = new MutableLiveData<>();

    public LiveData<PlanAnualResponse> getPlanAnual() { return planAnual; }

    public void cargarPlan() {
        ApiClient.getApi().getPlanAnual("plan_anual").enqueue(new Callback<PlanAnualResponse>() {
            @Override
            public void onResponse(Call<PlanAnualResponse> call, Response<PlanAnualResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().fases != null && !response.body().fases.isEmpty()) {
                    planAnual.postValue(response.body());
                } else {
                    planAnual.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<PlanAnualResponse> call, Throwable t) {
                planAnual.postValue(null);
            }
        });
    }
}
