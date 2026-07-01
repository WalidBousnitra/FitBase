package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Respuesta de la sesión del día */
public class SesionResponse {
    @SerializedName("sesion")
    public Sesion sesion;

    @SerializedName("ejercicios")
    public List<Ejercicio> ejercicios;

    @SerializedName("ajuste_dia")
    public AjusteDia ajusteDia;

    @SerializedName("mensaje")
    public String mensaje;
}

