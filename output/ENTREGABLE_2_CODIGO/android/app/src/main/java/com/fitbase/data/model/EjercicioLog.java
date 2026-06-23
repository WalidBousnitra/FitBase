package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Log de una serie completada por el usuario.
 * Referencia: REG-LOG-02 (tabla ejercicios_log)
 */
public class EjercicioLog {

    @SerializedName("log_id")
    private String logId;

    @SerializedName("plan_id")
    private String planId;

    @SerializedName("sesion_id")
    private String sesionId;

    @SerializedName("ejercicio_id")
    private String ejercicioId;

    @SerializedName("num_serie")
    private int numSerie;

    @SerializedName("num_peso_usado_kg")
    private float pesoUsado;

    @SerializedName("num_reps_completadas")
    private int repsCompletadas;

    @SerializedName("num_rir_percibido")
    private int rirPercibido;

    @SerializedName("num_rpe")
    private int rpe;

    @SerializedName("str_sensacion")
    private String sensacion; // facil, bien, duro, fallo

    @SerializedName("str_notas")
    private String notas;

    @SerializedName("bool_dolor")
    private boolean dolor;

    @SerializedName("str_zona_dolor")
    private String zonaDolor;

    // Getters y Setters
    public String getLogId() { return logId; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getSesionId() { return sesionId; }
    public void setSesionId(String sesionId) { this.sesionId = sesionId; }
    public String getEjercicioId() { return ejercicioId; }
    public void setEjercicioId(String ejercicioId) { this.ejercicioId = ejercicioId; }
    public int getNumSerie() { return numSerie; }
    public void setNumSerie(int numSerie) { this.numSerie = numSerie; }
    public float getPesoUsado() { return pesoUsado; }
    public void setPesoUsado(float pesoUsado) { this.pesoUsado = pesoUsado; }
    public int getRepsCompletadas() { return repsCompletadas; }
    public void setRepsCompletadas(int repsCompletadas) { this.repsCompletadas = repsCompletadas; }
    public int getRirPercibido() { return rirPercibido; }
    public void setRirPercibido(int rirPercibido) { this.rirPercibido = rirPercibido; }
    public int getRpe() { return rpe; }
    public String getSensacion() { return sensacion; }
    public void setSensacion(String sensacion) { this.sensacion = sensacion; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public boolean isDolor() { return dolor; }
    public void setDolor(boolean dolor) { this.dolor = dolor; }
    public String getZonaDolor() { return zonaDolor; }
    public void setZonaDolor(String zonaDolor) { this.zonaDolor = zonaDolor; }
}
