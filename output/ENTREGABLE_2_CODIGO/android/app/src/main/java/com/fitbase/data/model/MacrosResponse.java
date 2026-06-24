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

    @SerializedName("calorias_consumidas")
    public int caloriasConsumidas;

    @SerializedName("proteina_g")
    public int proteinaG;

    @SerializedName("proteina_consumida_g")
    public int proteinaConsumidaG;

    @SerializedName("carbos_g")
    public int carbosG;

    @SerializedName("carbos_consumidos_g")
    public int carbosConsumidosG;

    @SerializedName("grasas_g")
    public int grasasG;

    @SerializedName("grasas_consumidas_g")
    public int grasasConsumidasG;

    @SerializedName("agua_ml")
    public int aguaMl;

    @SerializedName("agua_consumida_ml")
    public int aguaConsumidaMl;

    @SerializedName("pasos_actuales")
    public int pasosActuales;

    @SerializedName("pasos_objetivo")
    public int pasosObjetivo;

    @SerializedName("bmr")
    public int bmr;

    @SerializedName("tdee")
    public int tdee;

    // Helpers
    public int getCaloriasRestantes() { return Math.max(0, caloriasObjetivo - caloriasConsumidas); }
    public int getProteinaRestante() { return Math.max(0, proteinaG - proteinaConsumidaG); }
    public int getCarbosRestantes() { return Math.max(0, carbosG - carbosConsumidosG); }
    public int getGrasasRestantes() { return Math.max(0, grasasG - grasasConsumidasG); }
    public float getProgresoCalorias() { return caloriasObjetivo > 0 ? (float) caloriasConsumidas / caloriasObjetivo : 0; }
    public float getProgresoPasos() { return pasosObjetivo > 0 ? (float) pasosActuales / pasosObjetivo : 0; }
}
