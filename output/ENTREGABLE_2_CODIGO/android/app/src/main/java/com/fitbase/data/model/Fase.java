package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

/** Fase del plan anual (macrociclo) */
public class Fase {
    @SerializedName("fase_id")
    public String faseId;

    @SerializedName("num_orden")
    public int orden;

    @SerializedName("str_nombre_fase")
    public String nombre;

    @SerializedName("str_tipo")
    public String tipo; // VOL, FZA, DEF, MNT, DELOAD

    @SerializedName("date_inicio")
    public String fechaInicio;

    @SerializedName("date_fin")
    public String fechaFin;

    @SerializedName("num_semanas")
    public int semanas;

    @SerializedName("num_volumen_objetivo")
    public int volumenObjetivo;

    @SerializedName("str_rir_rango")
    public String rirRango;

    @SerializedName("str_foco_muscular")
    public String focoMuscular;

    @SerializedName("str_objetivo_nutri")
    public String objetivoNutri;

    @SerializedName("str_notas")
    public String notas;
}
