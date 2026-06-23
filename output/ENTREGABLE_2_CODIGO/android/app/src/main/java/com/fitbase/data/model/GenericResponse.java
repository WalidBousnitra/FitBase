package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

/** Respuesta genérica para POST */
public class GenericResponse {
    @SerializedName("ok")
    public boolean ok;

    @SerializedName("error")
    public String error;
}
