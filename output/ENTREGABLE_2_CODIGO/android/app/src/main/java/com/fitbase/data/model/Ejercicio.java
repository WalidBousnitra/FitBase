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

    @SerializedName("str_equipamiento")
    private String equipamiento;

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

    // Peso que el usuario va a levantar de verdad esta serie/ejercicio —
    // empieza igual al sugerido por el motor, pero es AJUSTABLE (equipamiento
    // disponible en gimnasio, sensación del día, o motor sin histórico
    // todavía — "Elige tu peso"). Sin esto la app solo mostraba una cifra sin
    // forma de indicarle al motor lo que realmente se levantó, así que
    // ejercicios_log (y por tanto la autorregulación de la próxima sesión)
    // quedaba ciega. -1 = sin ajustar, usar el sugerido por el motor.
    private transient float pesoActual = -1;

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
    public String getEquipamiento() { return equipamiento; }

    /**
     * Salto del +/- de peso según el equipo (usuario/equipamiento.md):
     *   - Mancuernas: saltos de 2kg (rango disponible 4-60kg, §2 Mancuernas).
     *   - Barra (incl. Barra Z): cargada con discos en pareja — el salto más
     *     fino real con un set olímpico estándar (1.25/2.5/5/10/15/20/25kg)
     *     es 2*1.25=2.5kg por lado, así que cualquier combinación posible de
     *     discos cae en un múltiplo de 2.5kg.
     *   - Máquina/polea/banda: no hay un incremento fijo de fábrica (varía
     *     por máquina), así que se deja el control más fino (1kg) para poder
     *     ajustar a cualquier número real.
     */
    public float getPasoPesoKg() {
        if (equipamiento == null) return 1f;
        String eq = equipamiento.toLowerCase(java.util.Locale.ROOT);
        if (eq.contains("barra")) return 2.5f;
        if (eq.contains("mancuerna")) return 2f;
        return 1f;
    }
    public int getSerieCompletada() { return serieCompletada; }
    public void setSerieCompletada(int s) { this.serieCompletada = s; }

    /**
     * Peso real que se va a levantar — el sugerido por el motor hasta que el
     * usuario lo ajuste (stepper +/- en WorkoutActivity). Este es el valor
     * que se manda a ejercicios_log, NUNCA el sugerido a secas.
     */
    public float getPesoActual() { return pesoActual >= 0 ? pesoActual : pesoSugerido; }

    public void setPesoActual(float peso) { this.pesoActual = Math.max(0, peso); }

    public void ajustarPesoActual(float delta) { setPesoActual(getPesoActual() + delta); }

    /** Texto legible del peso actual: "82.5 kg" o "Elige tu peso" si es 0. */
    public String getPesoTexto() {
        float p = getPesoActual();
        if (p <= 0) return "Elige tu peso";
        return String.format("%.1f kg", p).replace(",0 ", " ");
    }

    /** Si aún no hay un peso real fijado (ni sugerido por el motor, ni ajustado a mano). */
    public boolean necesitaPesoManual() { return getPesoActual() <= 0; }

    /** Nombre corto para UI. */
    public String getNombreCorto() {
        if (nombre == null) return ejercicioId;
        return nombre;
    }
}
