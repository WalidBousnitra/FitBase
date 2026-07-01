package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Respuesta catálogo de ejercicios */
public class CatalogoResponse {
    @SerializedName("ejercicios")
    public List<Object> ejercicios;
}
