package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Respuesta de accion=preview_ramadan (GET) — botón de demo
 * (Constants.MOSTRAR_BOTONES_DEMO) para ver el banner de Ramadán/Eid fuera
 * de los ~30 días/año en que cae de verdad. Reutiliza el mismo tipo Ramadan
 * que vista_manana, así el renderizado es idéntico al banner real.
 */
public class RamadanPreviewResponse {
    @SerializedName("ramadan")
    private VistaMañanaResponse.Ramadan ramadan;

    public VistaMañanaResponse.Ramadan getRamadan() { return ramadan; }
}
