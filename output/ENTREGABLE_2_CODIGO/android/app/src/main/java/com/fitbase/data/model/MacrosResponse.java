package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

/** Macros y calorías del día (motor_dieta.md) */
public class MacrosResponse {
    @SerializedName("fecha")
    public String fecha;

    @SerializedName("es_dia_entreno")
    public boolean esDiaEntreno;

    @SerializedName("fase")
    public String fase;

    @SerializedName("calorias_objetivo")
    public int caloriasObjetivo;

    @SerializedName("proteina_g")
    public int proteinaG;

    @SerializedName("carbos_g")
    public int carbosG;

    @SerializedName("grasas_g")
    public int grasasG;

    @SerializedName("agua_ml")
    public int aguaMl;

    @SerializedName("bmr")
    public int bmr;

    @SerializedName("tdee")
    public int tdee;
}
