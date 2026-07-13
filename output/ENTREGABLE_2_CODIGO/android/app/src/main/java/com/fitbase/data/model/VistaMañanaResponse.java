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

    // Split del día (Push/Pierna/Pull/Hombros+Brazos) — solo presente si
    // tipoDia es "gym"; null en natación/descanso (tipoDia ya lo dice todo).
    @SerializedName("tipo_sesion")
    private String tipoSesion;

    @SerializedName("pre_temporada")
    private boolean preTemporada; // true antes de que empiece el plan de 11 meses

    @SerializedName("fecha_inicio_plan")
    private String fechaInicioPlan; // yyyy-MM-dd, solo relevante si preTemporada

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

    // Core del día de descanso (recuperación activa) — solo presente en días de
    // descanso; null en gym/natación (ahí el core va en el día de Pierna).
    @SerializedName("core_dia")
    private CoreDia coreDia;

    @SerializedName("aviso_ausencia")
    private AvisoAusencia avisoAusencia;

    @SerializedName("sesion_completada")
    private boolean sesionCompletada;

    @SerializedName("resumen_hoy")
    private ResumenSesionResponse.Resumen resumenHoy;

    @SerializedName("ramadan")
    private Ramadan ramadan;

    // ─── Inner classes ────────────────────────────────────

    public static class FaseInfo {
        @SerializedName("fase_id")
        public String faseId;
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
        // "post-gym" o "dia_descanso" — en descanso también se prescribe
        // cardio si la fase es DEF/MNT (programacion.md §12 FLUJO_DESCANSO).
        @SerializedName("contexto")
        public String contexto;
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

    /** Bloque de core del día de descanso (recuperación activa, hipertrofia.md §3).
     *  Reutiliza EjercicioMovilidad (misma forma: nombre/reps/objetivo). */
    public static class CoreDia {
        @SerializedName("titulo")
        public String titulo;
        @SerializedName("duracion_min")
        public int duracionMin;
        @SerializedName("frecuencia")
        public String frecuencia;
        @SerializedName("justificacion")
        public String justificacion;
        @SerializedName("ejercicios")
        public List<EjercicioMovilidad> ejercicios;
    }

    public static class AvisoAusencia {
        @SerializedName("fecha")
        public String fecha;
        @SerializedName("tipo")
        public String tipo;
        @SerializedName("mensaje")
        public String mensaje;
    }

    /** Ramadán/Eid (cultura.md §5-6). Las calorías/macros de arriba NO cambian
     *  — este objeto solo dice CUÁNDO/CÓMO repartirlas y qué esperar del día. */
    public static class Ramadan {
        @SerializedName("activo")
        public boolean activo;
        @SerializedName("es_eid")
        public boolean esEid;
        @SerializedName("dia_ayuno")
        public Integer diaAyuno;
        @SerializedName("horario_aproximado")
        public String horarioAproximado;
        @SerializedName("timing_entreno")
        public String timingEntreno;
        @SerializedName("hidratacion")
        public String hidratacion;
        @SerializedName("nutricion")
        public String nutricion;
        @SerializedName("iftar_orden")
        public String iftarOrden;
        @SerializedName("suhur_incluir")
        public String suhurIncluir;
        @SerializedName("entreno_prohibido")
        public String entrenoProhibido;
        @SerializedName("nota")
        public String nota; // Solo presente en es_eid
    }

    // ─── Getters ──────────────────────────────────────────

    public String getFecha() { return fecha; }
    public String getTipoDia() { return tipoDia; }
    public String getTipoSesion() { return tipoSesion; }
    public boolean isPreTemporada() { return preTemporada; }
    public String getFechaInicioPlan() { return fechaInicioPlan; }
    public FaseInfo getFase() { return fase; }
    public Sueno getSueno() { return sueno; }
    public MacrosResumen getMacros() { return macros; }
    public CardioObjetivo getCardio() { return cardio; }
    public MovilidadMatutina getMovilidadMatutina() { return movilidadMatutina; }
    public CoreDia getCoreDia() { return coreDia; }
    public AvisoAusencia getAvisoAusencia() { return avisoAusencia; }
    public boolean isSesionCompletada() { return sesionCompletada; }
    public ResumenSesionResponse.Resumen getResumenHoy() { return resumenHoy; }
    public Ramadan getRamadan() { return ramadan; }

    // Helpers
    public boolean esGym() { return "gym".equals(tipoDia); }
    public boolean esNatacion() { return "natacion".equals(tipoDia); }
    public boolean esDescanso() { return "descanso".equals(tipoDia); }
    public boolean hayCardio() { return cardio != null && cardio.cardioPostGymMin > 0; }
}
