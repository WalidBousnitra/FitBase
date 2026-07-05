package com.fitbase.data.cache;

import com.fitbase.data.health.HealthConnectBridge;
import com.fitbase.data.model.MetricasProgresionResponse;
import com.fitbase.data.model.PlanAnualResponse;
import com.fitbase.data.model.VistaMañanaResponse;

/**
 * Cache en memoria de los datos que SplashActivity precarga al abrir la app
 * (backend + Health Connect, incluyendo histórico). Las pantallas (Home,
 * Progresión, Plan Anual) leen de aquí primero para pintarse al instante,
 * sin esperar a la red — la espera ya ocurrió detrás de la pantalla de carga.
 */
public final class AppDataCache {

    private static volatile VistaMañanaResponse vistaMañana;
    private static volatile PlanAnualResponse planAnual;
    private static volatile MetricasProgresionResponse progresion7d;
    private static volatile HealthConnectBridge.HealthData healthHoy;
    private static volatile HealthConnectBridge.RecoveryData healthRecuperacion30d;
    private static volatile boolean cargaInicialCompleta = false;

    private AppDataCache() {}

    public static VistaMañanaResponse getVistaMañana() { return vistaMañana; }
    public static void setVistaMañana(VistaMañanaResponse v) { vistaMañana = v; }

    public static PlanAnualResponse getPlanAnual() { return planAnual; }
    public static void setPlanAnual(PlanAnualResponse p) { planAnual = p; }

    public static MetricasProgresionResponse getProgresion7d() { return progresion7d; }
    public static void setProgresion7d(MetricasProgresionResponse p) { progresion7d = p; }

    public static HealthConnectBridge.HealthData getHealthHoy() { return healthHoy; }
    public static void setHealthHoy(HealthConnectBridge.HealthData h) { healthHoy = h; }

    public static HealthConnectBridge.RecoveryData getHealthRecuperacion30d() { return healthRecuperacion30d; }
    public static void setHealthRecuperacion30d(HealthConnectBridge.RecoveryData r) { healthRecuperacion30d = r; }

    public static boolean isCargaInicialCompleta() { return cargaInicialCompleta; }
    public static void marcarCargaCompleta() { cargaInicialCompleta = true; }
}
