package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Respuesta métricas */
public class MetricasResponse {
    @SerializedName("metricas_zepp")
    public MetricasZepp metricasZepp;
    @SerializedName("peso")
    public Object peso;
    @SerializedName("peso_media_7d")
    public float pesoMedia7d;
}
