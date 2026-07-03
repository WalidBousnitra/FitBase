package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Respuesta de GET sesion_hoy.
 * Incluye sesión + ejercicios con pesos calculados dinámicamente (motor 6 capas)
 * + ajuste del día (fatiga/sueño/estrés).
 */
public class SesionResponse {

    @SerializedName("sesion")
    private Sesion sesion;

    @SerializedName("ejercicios")
    private List<Ejercicio> ejercicios;

    @SerializedName("ajuste_dia")
    private AjusteDia ajusteDia;

    @SerializedName("mensaje")
    private String mensaje;

    public Sesion getSesion() { return sesion; }
    public List<Ejercicio> getEjercicios() { return ejercicios; }
    public AjusteDia getAjusteDia() { return ajusteDia; }
    public String getMensaje() { return mensaje; }

    public boolean tieneSesion() { return sesion != null; }
}

