package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Respuesta de GET cambio_fase — se pide SOLO cuando la app detecta (en
 * cliente) que la fase actual es distinta a la última vista. Resume la fase
 * que acaba de terminar (adherencia + peso + sueño, todo de fuentes
 * permanentes — sesiones_plan y metricas_zepp, NO ejercicios_log que solo
 * guarda 7 días) y presenta la fase nueva.
 */
public class CambioFaseResponse {

    @SerializedName("hay_cambio")
    private boolean hayCambio;

    @SerializedName("fase_anterior")
    private FaseResumen faseAnterior;

    @SerializedName("resumen_fase_anterior")
    private ResumenFaseAnterior resumenFaseAnterior;

    @SerializedName("fase_actual")
    private FaseNueva faseActual;

    public boolean isHayCambio() { return hayCambio; }
    public FaseResumen getFaseAnterior() { return faseAnterior; }
    public ResumenFaseAnterior getResumenFaseAnterior() { return resumenFaseAnterior; }
    public FaseNueva getFaseActual() { return faseActual; }

    public static class FaseResumen {
        @SerializedName("fase_id")
        public String faseId;
        @SerializedName("nombre")
        public String nombre;
        @SerializedName("tipo")
        public String tipo;
        @SerializedName("foco")
        public String foco;
    }

    public static class ResumenFaseAnterior {
        @SerializedName("sesiones_completadas")
        public int sesionesCompletadas;
        @SerializedName("sesiones_totales")
        public int sesionesTotales;
        @SerializedName("peso_inicio")
        public Float pesoInicio;
        @SerializedName("peso_fin")
        public Float pesoFin;
        @SerializedName("sleep_media")
        public Integer sleepMedia;
    }

    public static class FaseNueva {
        @SerializedName("fase_id")
        public String faseId;
        @SerializedName("nombre")
        public String nombre;
        @SerializedName("tipo")
        public String tipo;
        @SerializedName("foco")
        public String foco;
        @SerializedName("nutri")
        public String nutri;
        @SerializedName("rir_rango")
        public String rirRango;
        @SerializedName("semanas")
        public int semanas;
    }
}
