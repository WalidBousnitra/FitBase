package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

/** Respuesta plan semanal */
public class PlanSemanalResponse {
    @SerializedName("str_lunes")
    public String lunes;
    @SerializedName("str_martes")
    public String martes;
    @SerializedName("str_miercoles")
    public String miercoles;
    @SerializedName("str_jueves")
    public String jueves;
    @SerializedName("str_viernes")
    public String viernes;
    @SerializedName("str_sabado")
    public String sabado;
    @SerializedName("str_domingo")
    public String domingo;
    @SerializedName("str_rir_semana")
    public String rirSemana;
    @SerializedName("bool_deload")
    public boolean esDeload;
}
