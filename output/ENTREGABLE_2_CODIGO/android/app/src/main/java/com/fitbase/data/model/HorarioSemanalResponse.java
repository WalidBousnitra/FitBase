package com.fitbase.data.model;

import java.util.Map;

/**
 * Respuesta de accion=horario_semanal (GET).
 * Claves del mapa: "0" (domingo) .. "6" (sábado). Valores:
 * PUSH / PIERNA / PULL / HOMBR / NATACION / DESCANSO.
 */
public class HorarioSemanalResponse {
    public Map<String, String> horario;
}
