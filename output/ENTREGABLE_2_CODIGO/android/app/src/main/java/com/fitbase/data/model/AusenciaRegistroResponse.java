package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Respuesta de POST registrar_ausencia (vacaciones/ausencia extendida).
 * Ver Codigo.gs registrarAusencia_ / excepciones.md §2.2.
 */
public class AusenciaRegistroResponse {

    @SerializedName("ok")
    public boolean ok;

    @SerializedName("error")
    public String error;

    @SerializedName("dias_ausencia")
    public int diasAusencia;

    @SerializedName("sesiones_suspendidas")
    public int sesionesSuspendidas;

    @SerializedName("impacto")
    public String impacto;

    @SerializedName("nota")
    public String nota;

    @SerializedName("rutina_casa")
    public RutinaCasa rutinaCasa;

    public static class RutinaCasa {
        @SerializedName("titulo")
        public String titulo;
        @SerializedName("duracion_min")
        public int duracionMin;
        @SerializedName("nota")
        public String nota;
        @SerializedName("ejercicios")
        public List<EjercicioCasa> ejercicios;
    }

    public static class EjercicioCasa {
        @SerializedName("nombre")
        public String nombre;
        @SerializedName("reps")
        public String reps;
        @SerializedName("objetivo")
        public String objetivo;
    }
}
