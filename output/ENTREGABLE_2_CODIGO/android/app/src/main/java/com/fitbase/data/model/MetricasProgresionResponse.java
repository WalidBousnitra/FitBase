package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Respuesta de la API para progresion de metricas.
 * Endpoint: ?accion=progresion_metricas&dias=30
 */
public class MetricasProgresionResponse {

    @SerializedName("dias_solicitados")
    public int diasSolicitados;

    @SerializedName("zepp")
    public List<ZeppEntry> zepp;

    @SerializedName("volumen_entreno")
    public List<VolumenEntry> volumenEntreno;

    @SerializedName("subjetiva")
    public List<SubjetivaEntry> subjetiva;

    @SerializedName("resumen")
    public Resumen resumen;

    /**
     * metricas_zepp centraliza TODO lo de Health Connect: sueño, pasos, FC
     * reposo, peso y % grasa — una fila por día (antes peso/grasa venían de
     * peso_log, separado; ahora todo aquí).
     */
    public static class ZeppEntry {
        public String fecha;
        // Nullable: Health Connect no lo tiene (solo entrada manual desde el backend,
        // ver base_datos.md num_sleep_score) — null cuando no hay dato, nunca inventado.
        @SerializedName("sleep_score")
        public Integer sleepScore;
        @SerializedName("hr_reposo")
        public int hrReposo;
        public int pasos;
        @SerializedName("peso_kg")
        public Float pesoKg;
        @SerializedName("grasa_pct")
        public Float grasaPct;
    }

    public static class VolumenEntry {
        public String fecha;
        @SerializedName("volumen_kg")
        public int volumenKg;
    }

    /** Energía, estrés y notas subjetivas (escala 1-5, entrada manual tras las 22:00). */
    public static class SubjetivaEntry {
        public String fecha;
        public Integer energia;
        public Integer estres;
        public String notas;
    }

    public static class Resumen {
        @SerializedName("peso_actual")
        public Float pesoActual;
        @SerializedName("peso_inicio")
        public Float pesoInicio;
        @SerializedName("grasa_actual")
        public Float grasaActual;
        @SerializedName("grasa_inicio")
        public Float grasaInicio;
        @SerializedName("sleep_media")
        public Integer sleepMedia;
        @SerializedName("pasos_media")
        public Integer pasosMedia;
    }
}
