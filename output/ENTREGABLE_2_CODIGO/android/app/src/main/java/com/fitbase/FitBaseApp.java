package com.fitbase;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.appcompat.app.AppCompatDelegate;

import com.fitbase.data.local.SyncManager;

/**
 * Clase Application de FitBase.
 * Configura tema oscuro y canal de notificaciones.
 */
public class FitBaseApp extends Application {

    public static final String CANAL_TIMER = "canal_timer";
    public static final String CANAL_TIMER_RELOJ = "canal_timer_reloj";
    public static final String CANAL_RECORDATORIOS = "canal_recordatorios";

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
    }

    private void crearCanalesNotificacion() {
        NotificationManager nm = getSystemService(NotificationManager.class);

        // Canal para timer de descanso (progreso silencioso)
        NotificationChannel canalTimer = new NotificationChannel(
                CANAL_TIMER,
                "Timer de Descanso",
                NotificationManager.IMPORTANCE_LOW
        );
        canalTimer.setDescription("Cuenta atrás durante descanso entre series");
        canalTimer.enableVibration(false);
        canalTimer.setSound(null, null);
        nm.createNotificationChannel(canalTimer);

        // Canal para notificación al reloj (Zepp replica al Amazfit GTS 4)
        // Alta prioridad para que Zepp la capture → vibra en muñeca
        // Sin vibración ni sonido LOCAL (solo el reloj vibra)
        NotificationChannel canalReloj = new NotificationChannel(
                CANAL_TIMER_RELOJ,
                "Alerta Reloj (Vibración Amazfit)",
                NotificationManager.IMPORTANCE_HIGH
        );
        canalReloj.setDescription("Vibra tu reloj cuando termina el descanso");
        canalReloj.enableVibration(false); // NO vibrar móvil
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
