package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/** Respuesta de accion=actualizar_horario (POST). */
public class ActualizarHorarioResponse {
    @SerializedName("ok")
    public boolean ok;

    @SerializedName("error")
    public String error;

    @SerializedName("horario")
    public Map<String, String> horario;

    @SerializedName("sesiones_eliminadas")
    public int sesionesEliminadas;

    @SerializedName("sesiones_generadas")
    public int sesionesGeneradas;

    @SerializedName("mensaje")
    public String mensaje;
}
