package com.fitbase.ui.plan;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fitbase.data.api.ApiClient;
import com.fitbase.data.model.Fase;
import com.fitbase.data.model.PlanAnualResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel para Plan Anual.
 * En demo/error de red → muestra plan ficticio.
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
                    planAnual.postValue(crearPlanDemo());
                }
            }

            @Override
            public void onFailure(Call<PlanAnualResponse> call, Throwable t) {
                planAnual.postValue(crearPlanDemo());
            }
        });
    }

    private PlanAnualResponse crearPlanDemo() {
        PlanAnualResponse plan = new PlanAnualResponse();
        plan.fechaInicio = "2026-08-31";
        plan.fechaFin = "2027-07-31";
        plan.totalSemanas = 48;
        plan.fases = new ArrayList<>();

        plan.fases.add(crearFase("F1", "Adaptación", "VOL", "31/08", "27/09", 4, "3-4", "Full Body"));
        plan.fases.add(crearFase("F2", "Hipertrofia I", "VOL", "28/09", "08/11", 6, "2-3", "Hombros, Espalda"));
        plan.fases.add(crearFase("F3", "Deload 1", "DELOAD", "09/11", "15/11", 1, "5-6", "Recuperación"));
        plan.fases.add(crearFase("F4", "Hipertrofia II", "VOL", "16/11", "27/12", 6, "2-3", "Pecho, Brazos"));
        plan.fases.add(crearFase("F5", "Fuerza", "FZA", "28/12", "07/02", 6, "1-2", "Compuestos"));
        plan.fases.add(crearFase("F6", "Deload 2", "DELOAD", "08/02", "14/02", 1, "5-6", "Recuperación"));
        plan.fases.add(crearFase("F7", "Hipertrofia III", "VOL", "15/02", "28/03", 6, "2-3", "Piernas, Core"));
        plan.fases.add(crearFase("F8", "Definición", "DEF", "29/03", "09/05", 6, "2-3", "Full Body"));
        plan.fases.add(crearFase("F9", "Deload 3", "DELOAD", "10/05", "16/05", 1, "5-6", "Recuperación"));
        plan.fases.add(crearFase("F10", "Peak", "FZA", "17/05", "27/06", 6, "0-1", "Compuestos"));
        plan.fases.add(crearFase("F11", "Mantenimiento", "MNT", "28/06", "31/07", 5, "3-4", "Full Body"));

        plan.faseActual = plan.fases.get(0);
        return plan;
    }

    private Fase crearFase(String id, String nombre, String tipo, String inicio, String fin,
                           int semanas, String rir, String foco) {
        Fase f = new Fase();
        f.faseId = id;
        f.nombre = nombre;
        f.tipo = tipo;
        f.fechaInicio = inicio;
        f.fechaFin = fin;
        f.semanas = semanas;
        f.rirRango = rir;
        f.focoMuscular = foco;
        return f;
    }
}
