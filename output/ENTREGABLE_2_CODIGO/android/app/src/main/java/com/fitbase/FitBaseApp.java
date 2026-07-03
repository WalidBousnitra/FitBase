package com.fitbase;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.fitbase.data.local.SyncManager;
import com.fitbase.service.TimerService;

/**
 * Clase Application de FitBase.
 * Configura tema oscuro, canales de notificación y gestión de primer plano.
 */
public class FitBaseApp extends Application {

    public static final String CANAL_TIMER = "canal_timer_v2";
    public static final String CANAL_TIMER_RELOJ = "canal_timer_reloj";
    public static final String CANAL_RECORDATORIOS = "canal_recordatorios";

    private int actividadesVisibles = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        // Forzar tema oscuro (Sistema_Diseno_Fitness.md - PREFERIDO)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        // Crear canales de notificación
        crearCanalesNotificacion();

        // Sincronizar operaciones pendientes al abrir la app + al recuperar red
        SyncManager.sincronizar(this);
        SyncManager.registrarCallbackConectividad(this);

        // Detectar si la app está en primer plano (para TimerService)
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                actividadesVisibles++;
                if (actividadesVisibles == 1) {
                    TimerService.setAppEnPrimerPlano(true);
                }
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                actividadesVisibles--;
                if (actividadesVisibles <= 0) {
                    actividadesVisibles = 0;
                    TimerService.setAppEnPrimerPlano(false);
                }
            }

            @Override public void onActivityCreated(@NonNull Activity a, @Nullable Bundle b) {}
            @Override public void onActivityResumed(@NonNull Activity a) {}
            @Override public void onActivityPaused(@NonNull Activity a) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) {}
            @Override public void onActivityDestroyed(@NonNull Activity a) {}
        });
    }

    private void crearCanalesNotificacion() {
        NotificationManager nm = getSystemService(NotificationManager.class);

        // Canal para timer de descanso (progreso silencioso)
        NotificationChannel canalTimer = new NotificationChannel(
                CANAL_TIMER,
                "Timer de Descanso",
            NotificationManager.IMPORTANCE_DEFAULT
        );
        canalTimer.setDescription("Cuenta atrás durante descanso entre series");
        canalTimer.enableVibration(false);
        canalTimer.setSound(null, null);
        nm.createNotificationChannel(canalTimer);

        // Canal para notificación FIN del timer (expandida tipo Live Activity)
        // Alta prioridad = heads-up garantizado. Vibra brevemente el móvil.
        NotificationChannel canalReloj = new NotificationChannel(
                CANAL_TIMER_RELOJ,
                "Alerta Timer (Fin de Descanso)",
                NotificationManager.IMPORTANCE_HIGH
        );
        canalReloj.setDescription("Notificación expandida cuando termina el descanso + vibración reloj");
        canalReloj.enableVibration(true);
        canalReloj.setVibrationPattern(new long[]{0, 200, 100, 200});
        canalReloj.setSound(null, null);   // NO sonar en móvil
        nm.createNotificationChannel(canalReloj);

        // Canal para recordatorios
        NotificationChannel canalRecordatorios = new NotificationChannel(
                CANAL_RECORDATORIOS,
                "Recordatorios",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        canalRecordatorios.setDescription("Recordatorios de entrenamiento y nutrición");
        nm.createNotificationChannel(canalRecordatorios);
    }
}
