package com.fitbase.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.fitbase.FitBaseApp;
import com.fitbase.R;
import com.fitbase.ui.workout.WorkoutActivity;

/**
 * Foreground Service para el timer de descanso entre series.
 * Muestra notificación persistente con cuenta atrás.
 * Al terminar:
 *   - Vibración en RELOJ (Amazfit GTS 4 vía Zepp/BLE) — NO en móvil.
 *   - Sonido SOLO por canal multimedia (AudioManager.STREAM_MUSIC).
 *   - Sonido SOLO si hay auriculares conectados (BT A2DP o wired).
 *   - Si no hay auriculares → solo vibración en reloj, silencio total en móvil.
 * Referencia: REG-DEV-01 (ui.md) § 4.4, 7.1, 7.2
 */
public class TimerService extends Service {

    private static final int NOTIFICACION_ID = 1001;
    private CountDownTimer timer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int segundos = intent.getIntExtra("segundos", 120);
        String ejercicioNombre = intent.getStringExtra("ejercicio_nombre");

        // Intent para volver a la app al tocar notificación
        Intent volverIntent = new Intent(this, WorkoutActivity.class);
        volverIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, volverIntent, PendingIntent.FLAG_IMMUTABLE);

        // Notificación inicial (foreground)
        Notification notificacion = crearNotificacion(segundos, ejercicioNombre, pendingIntent);
        startForeground(NOTIFICACION_ID, notificacion);

        // Timer con actualización cada segundo
        timer = new CountDownTimer(segundos * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int segRestantes = (int) (millisUntilFinished / 1000);
                actualizarNotificacion(segRestantes, ejercicioNombre, pendingIntent);
            }

            @Override
            public void onFinish() {
                notificarFinTimer(ejercicioNombre, pendingIntent);
                stopSelf();
            }
        };
        timer.start();

        return START_NOT_STICKY;
    }

    private Notification crearNotificacion(int segundos, String ejercicio, PendingIntent pi) {
        int min = segundos / 60;
        int seg = segundos % 60;

        return new NotificationCompat.Builder(this, FitBaseApp.CANAL_TIMER)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("\uD83C\uDFCB\uFE0F FitBase - Descanso")
                .setContentText(String.format("\u23F1 %d:%02d | %s", min, seg, ejercicio))
                .setContentIntent(pi)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void actualizarNotificacion(int segundos, String ejercicio, PendingIntent pi) {
        int min = segundos / 60;
        int seg = segundos % 60;

        Notification notificacion = new NotificationCompat.Builder(this, FitBaseApp.CANAL_TIMER)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("\uD83C\uDFCB\uFE0F FitBase - Descanso")
                .setContentText(String.format("\u23F1 %d:%02d | %s", min, seg, ejercicio))
                .setContentIntent(pi)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(NOTIFICACION_ID, notificacion);
    }

    /**
     * Al terminar el timer:
     * 1. Enviar vibración al reloj Amazfit GTS 4 (vía notificación BLE a Zepp).
     * 2. Reproducir sonido SOLO si hay auriculares conectados (BT A2DP o wired).
     *    - Usa STREAM_MUSIC (multimedia) → NO suena si móvil muteado sin auriculares.
     * 3. NO vibra el móvil (la vibración es solo en el reloj).
     */
    private void notificarFinTimer(String ejercicio, PendingIntent pi) {
        // 1. Vibración en RELOJ: se envía vía notificación con vibración en canal dedicado.
        //    Zepp replica notificaciones al reloj → vibra el reloj.
        enviarVibracionReloj(ejercicio, pi);

        // 2. Sonido solo si hay auriculares
        if (hayAuricularesConectados()) {
            reproducirSonidoMediaChannel();
        }
        // Si no hay auriculares → silencio total en móvil. Solo vibra reloj.
    }

    /**
     * Envía una notificación que Zepp replica al reloj → vibración en muñeca.
     * No vibra el móvil (canal configurado sin vibración local).
     */
    private void enviarVibracionReloj(String ejercicio, PendingIntent pi) {
        // Notificación heads-up para que Zepp la replique al reloj
        Notification notifReloj = new NotificationCompat.Builder(this, FitBaseApp.CANAL_TIMER_RELOJ)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("\uD83C\uDFCB\uFE0F \u00A1VAMOS!")
                .setContentText("Siguiente serie de " + ejercicio)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(null) // NO vibrar móvil
                .setSound(null)   // NO sonar en móvil
                .build();

        android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
        nm.notify(NOTIFICACION_ID + 1, notifReloj);
    }

    /**
     * Detecta si hay auriculares conectados (Bluetooth A2DP o wired).
     */
    private boolean hayAuricularesConectados() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Check wired headphones
        if (audioManager.isWiredHeadsetOn()) {
            return true;
        }

        // Check Bluetooth A2DP (auriculares BT)
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null && btAdapter.isEnabled()) {
            return btAdapter.getProfileConnectionState(BluetoothProfile.A2DP)
                    == BluetoothAdapter.STATE_CONNECTED;
        }

        return false;
    }

    /**
     * Reproduce sonido por canal STREAM_MUSIC (multimedia).
     * No usa tono del sistema → respeta mute del móvil pero suena por auriculares.
     */
    private void reproducirSonidoMediaChannel() {
        try {
            Uri sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mp.setDataSource(this, sonido);
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.prepare();
            mp.start();
        } catch (Exception e) {
            // Silencio si falla — no es crítico
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }
}
