package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Respuesta progresión */
public class ProgresionResponse {
    @SerializedName("ejercicio_id")
    public String ejercicioId;
    @SerializedName("registros")
    public List<Object> registros;
    @SerializedName("total")
    public int total;
}

/** Respuesta catálogo */
class CatalogoResponse {
    @SerializedName("ejercicios")
    public List<Object> ejercicios;
}
