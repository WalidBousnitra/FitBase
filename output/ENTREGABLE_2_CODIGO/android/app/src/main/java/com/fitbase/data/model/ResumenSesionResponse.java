package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Respuesta del POST completar_sesion.
 * Contiene el resumen que se muestra al usuario al finalizar el entreno.
 */
public class ResumenSesionResponse {

    @SerializedName("ok")
    private boolean ok;

    @SerializedName("sesion_id")
    private String sesionId;

    @SerializedName("resumen")
    private Resumen resumen;

    public static class Resumen {
        @SerializedName("series_totales")
        public int seriesTotales;

        @SerializedName("volumen_total_kg")
        public int volumenTotalKg;

        @SerializedName("rir_medio")
        public float rirMedio;

        @SerializedName("intensidad_percibida")
        public String intensidadPercibida;

        @SerializedName("impacto")
        public String impacto;

        @SerializedName("mensaje")
        public String mensaje;
    }

    public boolean isOk() { return ok; }
    public String getSesionId() { return sesionId; }
    public Resumen getResumen() { return resumen; }
}
