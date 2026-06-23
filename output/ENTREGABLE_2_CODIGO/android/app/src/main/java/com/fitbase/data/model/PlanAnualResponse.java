package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Respuesta plan anual */
public class PlanAnualResponse {
    @SerializedName("fases")
    public List<Fase> fases;

    @SerializedName("fase_actual")
    public Fase faseActual;

    @SerializedName("total_semanas")
    public int totalSemanas;

    @SerializedName("fecha_inicio")
    public String fechaInicio;

    @SerializedName("fecha_fin")
    public String fechaFin;
}

