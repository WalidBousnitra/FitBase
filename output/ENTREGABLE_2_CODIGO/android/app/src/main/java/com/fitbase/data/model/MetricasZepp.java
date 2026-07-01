package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

/** Métricas del Amazfit GTS 4 vía Health Connect */
public class MetricasZepp {
    @SerializedName("num_sleep_score")
    public int sleepScore;
    @SerializedName("num_sleep_horas")
    public float sleepHoras;
    @SerializedName("num_hr_reposo")
    public int hrReposo;
    @SerializedName("num_pasos_ayer")
    public int pasosAyer;
    @SerializedName("num_readiness")
    public int readiness;
}
