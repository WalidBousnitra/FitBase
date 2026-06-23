package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Modelo de un ejercicio planificado en una sesión.
 * Referencia: REG-LOG-02 (tabla ejercicios_plan)
 */
public class Ejercicio {

    @SerializedName("plan_id")
    private String planId;

    @SerializedName("sesion_id")
    private String sesionId;

    @SerializedName("ejercicio_id")
    private String ejercicioId;

    @SerializedName("num_orden")
    private int orden;

    @SerializedName("num_series_plan")
    private int seriesPlan;

    @SerializedName("num_reps_plan")
    private String repsPlan; // "8-10"

    @SerializedName("num_peso_sugerido_kg")
    private float pesoSugerido;

    @SerializedName("num_rir_objetivo")
    private int rirObjetivo;

    @SerializedName("num_descanso_seg")
    private int descansoSeg;

    @SerializedName("str_notas")
    private String notas;

    @SerializedName("bool_es_warmup")
    private boolean esWarmup;

    // Campos adicionales del catálogo (se populan en la app)
    private String nombre;
    private String grupoMuscular;
    private String patron;

    // Getters
    public String getPlanId() { return planId; }
    public String getSesionId() { return sesionId; }
    public String getEjercicioId() { return ejercicioId; }
    public int getOrden() { return orden; }
    public int getSeriesPlan() { return seriesPlan; }
    public String getRepsPlan() { return repsPlan; }
    public float getPesoSugerido() { return pesoSugerido; }
    public int getRirObjetivo() { return rirObjetivo; }
    public int getDescansoSeg() { return descansoSeg; }
    public String getNotas() { return notas; }
    public boolean isEsWarmup() { return esWarmup; }
    public String getNombre() { return nombre; }
    public String getGrupoMuscular() { return grupoMuscular; }
    public String getPatron() { return patron; }

    // Setters
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setGrupoMuscular(String grupo) { this.grupoMuscular = grupo; }
    public void setPatron(String patron) { this.patron = patron; }
    public void setPesoSugerido(float peso) { this.pesoSugerido = peso; }
}
