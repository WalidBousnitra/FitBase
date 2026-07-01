package com.fitbase.data.api;

import com.fitbase.data.model.AusenciaResponse;
import com.fitbase.data.model.MacrosResponse;
import com.fitbase.data.model.PlanAnualResponse;
import com.fitbase.data.model.PlanSemanalResponse;
import com.fitbase.data.model.SesionResponse;
import com.fitbase.data.model.MetricasProgresionResponse;
import com.fitbase.data.model.GenericResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Interface Retrofit para la API de FitBase (Google Apps Script).
 * Referencia: REG-LOG-02 (base_datos.md)
 */
public interface FitBaseApi {

    // ─── GET ──────────────────────────────────────────────

    @GET("exec")
    Call<SesionResponse> getSesionHoy(@Query("accion") String accion);

    @GET("exec")
    Call<PlanAnualResponse> getPlanAnual(@Query("accion") String accion);

    @GET("exec")
    Call<PlanSemanalResponse> getPlanSemanal(
            @Query("accion") String accion,
            @Query("semana") int semana
    );

    @GET("exec")
    Call<MacrosResponse> getMacrosHoy(@Query("accion") String accion);

    @GET("exec")
    Call<AusenciaResponse> checkAusencia(@Query("accion") String accion);

    @GET("exec")
    Call<MetricasProgresionResponse> getProgresionMetricas(
            @Query("accion") String accion,
            @Query("dias") int dias
    );

    // ─── POST ─────────────────────────────────────────────

    @POST("exec")
    Call<GenericResponse> enviarDatos(@Body Map<String, Object> datos);
}
