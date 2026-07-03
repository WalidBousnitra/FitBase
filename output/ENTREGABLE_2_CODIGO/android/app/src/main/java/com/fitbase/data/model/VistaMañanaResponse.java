package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Respuesta del endpoint GET vista_manana.
 * Contiene todo lo que el usuario ve al abrir la app por la mañana:
 * sueño, macros, cardio, movilidad, tipo de día, aviso ausencia.
 */
public class VistaMañanaResponse {

    @SerializedName("fecha")
    private String fecha;

    @SerializedName("tipo_dia")
    private String tipoDia; // "gym", "natacion", "descanso"

    @SerializedName("fase")
    private FaseInfo fase;

    @SerializedName("sueno")
    private Sueno sueno;

    @SerializedName("macros")
    private MacrosResumen macros;

    @SerializedName("cardio")
    private CardioObjetivo cardio;

    @SerializedName("movilidad_matutina")
    private MovilidadMatutina movilidadMatutina;

    @SerializedName("aviso_ausencia")
    private AvisoAusencia avisoAusencia;

    // ─── Inner classes ────────────────────────────────────

    public static class FaseInfo {
        @SerializedName("nombre")
        public String nombre;
        @SerializedName("tipo")
        public String tipo; // VOL/FZA/DEF/MNT/DELOAD
        @SerializedName("nutri")
        public String nutri; // bulk/cut/mantener
    }

    public static class Sueno {
        @SerializedName("sleep_score")
        public Integer sleepScore;
        @SerializedName("hr_reposo")
        public Integer hrReposo;
        @SerializedName("pasos_ayer")
        public Integer pasosAyer;
    }

    public static class MacrosResumen {
        @SerializedName("calorias")
        public int calorias;
        @SerializedName("proteina_g")
        public int proteinaG;
        @SerializedName("carbos_g")
        public int carbosG;
        @SerializedName("grasas_g")
        public int grasasG;
        @SerializedName("agua_ml")
        public int aguaMl;
    }

    public static class CardioObjetivo {
        @SerializedName("pasos_objetivo")
        public int pasosObjetivo;
        @SerializedName("cardio_post_gym_min")
        public int cardioPostGymMin;
        @SerializedName("modalidad")
        public String modalidad;
        @SerializedName("intensidad")
        public String intensidad;
        @SerializedName("justificacion")
        public String justificacion; // Evidence-based reason (Wilson 2012, Viana 2019)
    }

    public static class MovilidadMatutina {
        @SerializedName("duracion_min")
        public int duracionMin;
        @SerializedName("frecuencia")
        public String frecuencia; // "DIARIA" — Ruivo 2017
        @SerializedName("justificacion")
        public String justificacion; // Why always shown (evidence reference)
        @SerializedName("ejercicios")
        public List<EjercicioMovilidad> ejercicios;
        @SerializedName("nota")
        public String nota;
    }

    public static class EjercicioMovilidad {
        @SerializedName("nombre")
        public String nombre;
        @SerializedName("reps")
        public String reps;
        @SerializedName("objetivo")
        public String objetivo;
    }

    public static class AvisoAusencia {
        @SerializedName("fecha")
        public String fecha;
        @SerializedName("tipo")
        public String tipo;
        @SerializedName("mensaje")
        public String mensaje;
    }

    // ─── Getters ──────────────────────────────────────────

    public String getFecha() { return fecha; }
    public String getTipoDia() { return tipoDia; }
    public FaseInfo getFase() { return fase; }
    public Sueno getSueno() { return sueno; }
    public MacrosResumen getMacros() { return macros; }
    public CardioObjetivo getCardio() { return cardio; }
    public MovilidadMatutina getMovilidadMatutina() { return movilidadMatutina; }
    public AvisoAusencia getAvisoAusencia() { return avisoAusencia; }

    // Helpers
    public boolean esGym() { return "gym".equals(tipoDia); }
    public boolean esNatacion() { return "natacion".equals(tipoDia); }
    public boolean esDescanso() { return "descanso".equals(tipoDia); }
    public boolean hayCardio() { return cardio != null && cardio.cardioPostGymMin > 0; }
}
