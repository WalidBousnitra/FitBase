package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Ajuste del día calculado por motor de cargas (REG-LOG-01) */
public class AjusteDia {
    @SerializedName("factor")
    public float factor;

    @SerializedName("razones")
    public List<String> razones;

    @SerializedName("tipo")
    public String tipo; // normal, reducida, recuperacion
}
