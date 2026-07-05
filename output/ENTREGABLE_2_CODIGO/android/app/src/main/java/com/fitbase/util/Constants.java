package com.fitbase.util;

/**
 * Constantes globales de la app.
 * Referencia: biometria.md, motor_dieta.md
 */
public final class Constants {

    private Constants() {}

    // Fecha inicio del programa (biometria.md)
    public static final String FECHA_INICIO_PROGRAMA = "2026-08-31";
    public static final int DURACION_MESES = 11;

    // Datos fijos del usuario (biometria.md)
    public static final float PESO_INICIAL_KG = 78.2f;
    public static final int ALTURA_CM = 188;
    public static final int EDAD = 24;
    public static final String SEXO = "M";

    // Timer defaults (Schoenfeld 2016)
    public static final int DESCANSO_COMPUESTO_SEG = 180; // 3 min
    public static final int DESCANSO_AISLAMIENTO_SEG = 120; // 2 min
    public static final int DESCANSO_DELOAD_SEG = 90; // 1.5 min

    public static final String API_BASE_URL = "https://script.google.com/macros/s/AKfycbye7bjaH3F8xiH2a68h1q8fvEypQLgMlx5pGFAlvaSFZv2QM8Rkx1pAD2mJ_SGL_Fyd/exec/";
}
