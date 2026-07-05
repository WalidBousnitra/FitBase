package com.fitbase.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Respuesta de GET sesion_hoy.
 * Incluye sesión + ejercicios con pesos calculados dinámicamente (motor 6 capas)
 * + ajuste del día (fatiga/sueño/estrés).
 */
public class SesionResponse {

    @SerializedName("sesion")
    private Sesion sesion;

    @SerializedName("ejercicios")
    private List<Ejercicio> ejercicios;

    @SerializedName("ajuste_dia")
    private AjusteDia ajusteDia;

    @SerializedName("mensaje")
    private String mensaje;

    @SerializedName("calentamiento")
    private RutinaInfo calentamiento;

    @SerializedName("estiramientos")
    private RutinaInfo estiramientos;

    // Ramadán (cultura.md §5): series ya vienen recalculadas -30% en
    // ejercicios[].numSeriesPlan cuando ramadanActivo es true — esto es solo
    // el aviso textual para el usuario.
    @SerializedName("ramadan_activo")
    private boolean ramadanActivo;

    @SerializedName("ramadan_nota")
    private String ramadanNota;

    public Sesion getSesion() { return sesion; }
    public List<Ejercicio> getEjercicios() { return ejercicios; }
    public AjusteDia getAjusteDia() { return ajusteDia; }
    public String getMensaje() { return mensaje; }
    public RutinaInfo getCalentamiento() { return calentamiento; }
    public RutinaInfo getEstiramientos() { return estiramientos; }
    public boolean isRamadanActivo() { return ramadanActivo; }
    public String getRamadanNota() { return ramadanNota; }

    public boolean tieneSesion() { return sesion != null; }

    // ─── Setters (para demo — ver WorkoutViewModel.crearSesionDemo) ───
    public void setSesion(Sesion sesion) { this.sesion = sesion; }
    public void setEjercicios(List<Ejercicio> ejercicios) { this.ejercicios = ejercicios; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public void setCalentamiento(RutinaInfo r) { this.calentamiento = r; }
    public void setEstiramientos(RutinaInfo r) { this.estiramientos = r; }

    /**
     * Calentamiento/estiramientos: vienen del backend (getCalentamiento_ /
     * getEstiramientos_ en Codigo.gs), citando evidencia — NUNCA hardcodeados
     * en el cliente. Mismo shape para ambos.
     */
    public static class RutinaInfo {
        @SerializedName("duracion_min")
        public int duracionMin;
        @SerializedName("ejercicios")
        public List<ItemRutina> ejercicios;
    }

    public static class ItemRutina {
        @SerializedName("nombre")
        public String nombre;
        @SerializedName("reps")
        public String reps;
        @SerializedName("objetivo")
        public String objetivo;
    }
}

