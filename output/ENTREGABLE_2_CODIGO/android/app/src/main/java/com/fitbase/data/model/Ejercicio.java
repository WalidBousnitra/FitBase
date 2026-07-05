package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Ejercicio planificado con peso calculado dinámicamente por el motor de 6 capas.
 * El campo pesoSugerido viene del backend (calcularPesoSugerido_), NO del plan.
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

    @SerializedName("str_reps_plan")
    private String repsPlan; // "8-10", "6-8", "30s"

    @SerializedName("num_rir_objetivo")
    private int rirObjetivo;

    @SerializedName("num_descanso_seg")
    private int descansoSeg;

    @SerializedName("str_notas")
    private String notas;

    @SerializedName("bool_es_warmup")
    private boolean esWarmup;

    // Superserie (preferencias.md §5): si coincide con el grupo del siguiente
    // ejercicio, no hay descanso entre ambos — ver WorkoutViewModel.
    @SerializedName("str_superset_grupo")
    private String supersetGrupo;

    // ─── Campos del motor (calculados en getSesionHoy_) ───
    @SerializedName("num_peso_sugerido_kg")
    private float pesoSugerido;

    @SerializedName("motor_detalle")
    private String motorDetalle; // "80kg | ↑ fácil | Hipertrofia | → 82.5kg"

    @SerializedName("motor_capas")
    private MotorCapas motorCapas;

    @SerializedName("ajuste_aplicado")
    private float ajusteAplicado; // Factor del día (0.70-1.0)

    // ─── Campos del catálogo (enriquecidos) ───
    @SerializedName("nombre")
    private String nombre;

    @SerializedName("str_grupo_principal")
    private String grupoMuscular;

    // ─── Motor capas (detalle de la decisión) ───
    public static class MotorCapas {
        @SerializedName("base")
        public float base;
        @SerializedName("ultimoReps")
        public int ultimoReps;
        @SerializedName("ultimoRIR")
        public int ultimoRIR;
        @SerializedName("deltaCap")
        public int deltaCap;
        @SerializedName("pctAPRE")
        public float pctAPRE;
        @SerializedName("nivelAPRE")
        public String nivelAPRE; // "correcto", "facil", "muy_facil", "pesado", "muy_pesado"
        @SerializedName("fase")
        public String fase;
        @SerializedName("faseNombre")
        public String faseNombre;
        @SerializedName("factorDescanso")
        public float factorDescanso;
        @SerializedName("factorDia")
        public float factorDia;
        @SerializedName("deload")
        public boolean deload;
        @SerializedName("nutriCut")
        public boolean nutriCut;
        @SerializedName("gapAlerta")
        public String gapAlerta;
    }

    // ─── Estado local (UI) ───
    private transient int serieCompletada = 0;

    // ─── Getters ──────────────────────────────────────────
    public String getPlanId() { return planId; }
    public String getSesionId() { return sesionId; }
    public String getEjercicioId() { return ejercicioId; }
    public int getOrden() { return orden; }
    public int getSeriesPlan() { return seriesPlan; }
    public String getRepsPlan() { return repsPlan; }
    public int getRirObjetivo() { return rirObjetivo; }
    public int getDescansoSeg() { return descansoSeg; }
    public String getNotas() { return notas; }
    public boolean isEsWarmup() { return esWarmup; }
    public String getSupersetGrupo() { return supersetGrupo; }
    public float getPesoSugerido() { return pesoSugerido; }
    public String getMotorDetalle() { return motorDetalle; }
    public MotorCapas getMotorCapas() { return motorCapas; }
    public float getAjusteAplicado() { return ajusteAplicado; }
    public String getNombre() { return nombre; }
    public String getGrupoMuscular() { return grupoMuscular; }
    public int getSerieCompletada() { return serieCompletada; }
    public void setSerieCompletada(int s) { this.serieCompletada = s; }

    // ─── Setters (para demo — ver WorkoutViewModel.crearSesionDemo) ───
    public void setEjercicioId(String id) { this.ejercicioId = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setSeriesPlan(int series) { this.seriesPlan = series; }
    public void setRepsPlan(String reps) { this.repsPlan = reps; }
    public void setRirObjetivo(int rir) { this.rirObjetivo = rir; }
    public void setDescansoSeg(int seg) { this.descansoSeg = seg; }
    public void setPesoSugerido(float peso) { this.pesoSugerido = peso; }
    public void setMotorDetalle(String detalle) { this.motorDetalle = detalle; }

    /** Texto legible del peso: "82.5 kg" o "Elige tu peso" si es 0. */
    public String getPesoTexto() {
        if (pesoSugerido <= 0) return "Elige tu peso";
        return String.format("%.1f kg", pesoSugerido).replace(",0 ", " ");
    }

    /** Si el motor no pudo calcular peso (primer uso del ejercicio). */
    public boolean necesitaPesoManual() { return pesoSugerido <= 0; }

    /** Nombre corto para UI. */
    public String getNombreCorto() {
        if (nombre == null) return ejercicioId;
        return nombre;
    }
}
