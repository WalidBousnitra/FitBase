package com.fitbase.data.api;

import com.fitbase.data.model.ActualizarHorarioResponse;
import com.fitbase.data.model.AusenciaRegistroResponse;
import com.fitbase.data.model.AusenciaResponse;
import com.fitbase.data.model.CambioFaseResponse;
import com.fitbase.data.model.GenericResponse;
import com.fitbase.data.model.HorarioSemanalResponse;
import com.fitbase.data.model.MacrosResponse;
import com.fitbase.data.model.MetricasProgresionResponse;
import com.fitbase.data.model.PlanAnualResponse;
import com.fitbase.data.model.ResumenSesionResponse;
import com.fitbase.data.model.SesionResponse;
import com.fitbase.data.model.VistaMañanaResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Interface Retrofit — API FitBase (Google Apps Script).
 * Endpoints alineados con Codigo.gs §2.
 */
public interface FitBaseApi {

    // ─── GET ──────────────────────────────────────────────

    @GET("exec")
    Call<VistaMañanaResponse> getVistaMañana(@Query("accion") String accion);

    @GET("exec")
    Call<SesionResponse> getSesionHoy(@Query("accion") String accion);

    @GET("exec")
    Call<PlanAnualResponse> getPlanAnual(@Query("accion") String accion);

    @GET("exec")
    Call<MacrosResponse> getMacrosHoy(@Query("accion") String accion);

    @GET("exec")
    Call<AusenciaResponse> checkAusencia(@Query("accion") String accion);

    @GET("exec")
    Call<MetricasProgresionResponse> getProgresionMetricas(@Query("accion") String accion, @Query("dias") int dias);

    @GET("exec")
    Call<CambioFaseResponse> getCambioFase(@Query("accion") String accion);

    @GET("exec")
    Call<HorarioSemanalResponse> getHorarioSemanal(@Query("accion") String accion);

    @GET("exec")
    Call<com.fitbase.data.model.RamadanPreviewResponse> getRamadanPreview(@Query("accion") String accion);

    // ─── POST ─────────────────────────────────────────────

    @POST("exec")
    Call<GenericResponse> guardarLog(@Body Map<String, Object> datos);

    @POST("exec")
    Call<GenericResponse> guardarMetricas(@Body Map<String, Object> datos);

    @POST("exec")
    Call<GenericResponse> guardarMetricasSubjetivas(@Body Map<String, Object> datos);

    @POST("exec")
    Call<ResumenSesionResponse> completarSesion(@Body Map<String, Object> datos);

    @POST("exec")
    Call<AusenciaRegistroResponse> registrarAusencia(@Body Map<String, Object> datos);

    @POST("exec")
    Call<GenericResponse> enviarDatos(@Body Map<String, Object> datos);

    @POST("exec")
    Call<ActualizarHorarioResponse> actualizarHorario(@Body Map<String, Object> datos);
}
