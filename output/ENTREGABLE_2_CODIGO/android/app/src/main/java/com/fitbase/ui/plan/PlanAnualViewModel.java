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

        plan.fases.add(crearFase("F1", "Adaptación + Postura", "VOL", "2026-08-31", "2026-09-27", 4, "3-4", "Full Body · Correctivos posturales · Wall Angels"));
        plan.fases.add(crearFase("F2", "Hipertrofia I — V-Taper", "VOL", "2026-09-28", "2026-11-08", 6, "2-3", "Hombros, Espalda (V-taper) · Postura"));
        plan.fases.add(crearFase("F3", "Deload 1", "DELOAD", "2026-11-09", "2026-11-15", 1, "5-6", "Movilidad + Flex · Test Wall Angel"));
        plan.fases.add(crearFase("F4", "Hipertrofia II — Brazos", "VOL", "2026-11-16", "2026-12-27", 6, "2-3", "Bíceps, Tríceps, Pecho · Mantener hombros"));
        plan.fases.add(crearFase("F5", "Deload 2", "DELOAD", "2026-12-28", "2027-01-03", 1, "5-6", "Movilidad + Flex · Descanso activo"));
        plan.fases.add(crearFase("F6", "Fuerza — Compuestos", "FZA", "2027-01-04", "2027-02-14", 6, "1-2", "Press militar, Dominadas, Sentadilla"));
        plan.fases.add(crearFase("F7", "Hipertrofia III — Balance", "VOL", "2027-02-15", "2027-03-28", 6, "2-3", "Piernas, Core · Mantener V-taper"));
        plan.fases.add(crearFase("F8", "Deload 3", "DELOAD", "2027-03-29", "2027-04-04", 1, "5-6", "Movilidad + Flex · Test postural final"));
        plan.fases.add(crearFase("F9", "Definición", "DEF", "2027-04-05", "2027-05-16", 6, "2-3", "Mantener masa · Déficit controlado"));
        plan.fases.add(crearFase("F10", "Peak Estético + Mant.", "MNT", "2027-05-17", "2027-07-31", 11, "2-3", "Ratio cintura/hombros · Simetría"));

        // Demo: marcar Hipertrofia I como fase actual
        plan.faseActual = plan.fases.get(1);
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
