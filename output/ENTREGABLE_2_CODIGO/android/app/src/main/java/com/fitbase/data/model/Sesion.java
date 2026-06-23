package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Modelo de sesión planificada.
 * Referencia: REG-LOG-02 (tabla sesiones_plan)
 */
public class Sesion {

    @SerializedName("sesion_id")
    private String sesionId;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("date_fecha")
    private String fecha;

    @SerializedName("str_tipo")
    private String tipo; // Push, Pull, Pierna, Upper

    @SerializedName("num_semana_meso")
    private int semanaMeso;

    @SerializedName("str_fase")
    private String fase; // acumulacion, intensificacion, deload

    @SerializedName("num_ajuste_volumen")
    private float ajusteVolumen;

    @SerializedName("str_razon_ajuste")
    private String razonAjuste;

    @SerializedName("num_duracion_est_min")
    private int duracionEstimadaMin;

    @SerializedName("bool_completada")
    private boolean completada;

    @SerializedName("date_inicio")
    private String fechaInicio;

    @SerializedName("date_fin")
    private String fechaFin;

    // Getters
    public String getSesionId() { return sesionId; }
    public String getUserId() { return userId; }
    public String getFecha() { return fecha; }
    public String getTipo() { return tipo; }
    public int getSemanaMeso() { return semanaMeso; }
    public String getFase() { return fase; }
    public float getAjusteVolumen() { return ajusteVolumen; }
    public String getRazonAjuste() { return razonAjuste; }
    public int getDuracionEstimadaMin() { return duracionEstimadaMin; }
    public boolean isCompletada() { return completada; }
    public String getFechaInicio() { return fechaInicio; }
    public String getFechaFin() { return fechaFin; }
}
