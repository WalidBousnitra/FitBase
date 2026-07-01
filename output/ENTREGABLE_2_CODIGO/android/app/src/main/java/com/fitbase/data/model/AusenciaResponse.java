package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Respuesta de check_ausencia.
 * Detecta días sin abrir la app y redistribuye volumen.
 */
public class AusenciaResponse {

    @SerializedName("dias_perdidos")
    public List<DiaPerdido> diasPerdidos;

    @SerializedName("total_perdidos")
    public int totalPerdidos;

    @SerializedName("redistribucion")
    public Redistribucion redistribucion;

    @SerializedName("mensaje")
    public String mensaje;

    public static class DiaPerdido {
        public String fecha;
        public String tipo;

        @SerializedName("sesion_id")
        public String sesionId;
    }

    public static class Redistribucion {
        @SerializedName("volumen_perdido_series")
        public int volumenPerdidoSeries;

        @SerializedName("series_extra_por_sesion")
        public int seriesExtraPorSesion;

        public String accion;
    }
}
