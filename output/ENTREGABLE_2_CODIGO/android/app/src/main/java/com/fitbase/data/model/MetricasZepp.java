package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

/** Métricas del Amazfit GTS 4 vía Health Connect */
public class MetricasZepp {
    @SerializedName("num_sleep_score")
    public int sleepScore;
    @SerializedName("num_hr_reposo")
    public int hrReposo;
    @SerializedName("num_pasos")
    public int pasos;
    @SerializedName("num_vo2max")
    public float vo2max;
}
