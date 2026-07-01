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

    @SerializedName("peso")
    public List<PesoEntry> peso;

    @SerializedName("zepp")
    public List<ZeppEntry> zepp;

    @SerializedName("volumen_entreno")
    public List<VolumenEntry> volumenEntreno;

    @SerializedName("resumen")
    public Resumen resumen;

    public static class PesoEntry {
        public String fecha;
        @SerializedName("peso_kg")
        public float pesoKg;
        @SerializedName("grasa_pct")
        public Float grasaPct;
        @SerializedName("hidratacion_pct")
        public Float hidratacionPct;
        @SerializedName("grasa_visceral")
        public Float grasaVisceral;
    }

    public static class ZeppEntry {
        public String fecha;
        @SerializedName("sleep_score")
        public int sleepScore;
        @SerializedName("sleep_horas")
        public float sleepHoras;
        @SerializedName("sleep_deep_min")
        public int sleepDeepMin;
        @SerializedName("hrv_rmssd")
        public int hrvRmssd;
        @SerializedName("hr_reposo")
        public int hrReposo;
        @SerializedName("stress_avg")
        public int stressAvg;
        public int pasos;
        public float vo2max;
    }

    public static class VolumenEntry {
        public String fecha;
        @SerializedName("volumen_kg")
        public int volumenKg;
    }

    public static class Resumen {
        @SerializedName("peso_actual")
        public Float pesoActual;
        @SerializedName("peso_inicio")
        public Float pesoInicio;
        @SerializedName("grasa_actual")
        public Float grasaActual;
        @SerializedName("sleep_media")
        public Integer sleepMedia;
        @SerializedName("pasos_media")
        public Integer pasosMedia;
    }
}
