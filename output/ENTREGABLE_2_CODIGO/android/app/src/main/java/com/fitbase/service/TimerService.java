package com.fitbase.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;

import com.fitbase.FitBaseApp;
import com.fitbase.R;
import com.fitbase.ui.workout.WorkoutActivity;

/**
 * Foreground Service - Timer de descanso entre series.
 *
 * UX tipo "Dynamic Island" en Android:
 *   - DURANTE: Notificacion compacta con cronometro nativo (visible en barra/isla).
 *   - AL ACABAR (fuera de la app):
 *       -> Notificacion HEADS-UP expandida (como WhatsApp) con "Siguiente serie!"
 *       -> Sonido sutil SOLO si hay auriculares conectados
 *       -> Vibracion en reloj Amazfit via Zepp (notificacion alta prioridad)
 *       -> NO vibra el movil, NO suena por altavoz
 *   - AL ACABAR (dentro de la app):
 *       -> NADA. Ni sonido ni vibracion. La UI ya transiciona automaticamente.
 *
 * Referencia: REG-DEV-01 (ui.md) 4.4, 7.1, 7.2
 */
public class TimerService extends Service {

    private static final int NOTIFICACION_ID = 1001;
    private static final int NOTIF_FIN_ID = 1002;
    public static final String ACTION_TIMER_FINISHED = "com.fitbase.TIMER_FINISHED";

    private CountDownTimer timer;
    private long finishTimeMs;

    // Flag estatico para saber si la app esta en primer plano
    private static boolean appEnPrimerPlano = false;

    public static void setAppEnPrimerPlano(boolean enPrimerPlano) {
        appEnPrimerPlano = enPrimerPlano;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int segundos = intent.getIntExtra("segundos", 120);
        String ejercicioNombre = intent.getStringExtra("ejercicio_nombre");
        if (ejercicioNombre == null) ejercicioNombre = "";

        // Calcular cuando termina (para Chronometer nativo de la notificacion)
        finishTimeMs = System.currentTimeMillis() + (segundos * 1000L);

        // Intent para volver a la app
        Intent volverIntent = new Intent(this, WorkoutActivity.class);
        volverIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, volverIntent, PendingIntent.FLAG_IMMUTABLE);

        // Notificacion compacta con cronometro (estilo Dynamic Island)
        Notification notificacion = crearNotificacionCronometro(ejercicioNombre, pendingIntent);
        startForeground(NOTIFICACION_ID, notificacion);

        // Timer interno para detectar finalizacion
        if (timer != null) timer.cancel();
        final String ejercicio = ejercicioNombre;
        timer = new CountDownTimer(segundos * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Chronometer nativo actualiza solo. No necesitamos rebuilds.
            }

            @Override
            public void onFinish() {
                onTimerFinish(ejercicio, pendingIntent);
            }
        };
        timer.start();

        return START_NOT_STICKY;
    }

    /**
     * Notificacion con Chronometer nativo - cuenta atras visible en barra/isla.
     * En Android 14+ con dispositivos compatibles se ve como "Dynamic Island".
     * En otros se ve como notificacion compacta con timer en tiempo real.
     */
    private Notification crearNotificacionCronometro(String ejercicio, PendingIntent pi) {
        return new NotificationCompat.Builder(this, FitBaseApp.CANAL_TIMER)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Descanso")
                .setContentText(ejercicio)
                .setContentIntent(pi)
                .setOngoing(true)
                .setSilent(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(finishTimeMs)
                .setShowWhen(true)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    /**
     * Logica al terminar el timer.
     * Si la app esta en primer plano -> SILENCIO TOTAL (UI ya muestra transicion).
     * Si esta fuera -> notificacion expandida + sonido auriculares + vibrar reloj.
     */
    private void onTimerFinish(String ejercicio, PendingIntent pi) {
        if (appEnPrimerPlano) {
            // DENTRO de la app -> SILENCIO TOTAL
            // WorkoutActivity observa timerSegundos=0 y transiciona sola
            android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
            nm.cancel(NOTIFICACION_ID);
        } else {
            // FUERA de la app -> Expandir notificacion (tipo WhatsApp heads-up)
            mostrarNotificacionExpandida(ejercicio, pi);

            // Vibrar reloj (via Zepp)
            enviarVibracionReloj(ejercicio, pi);

            // Sonido SOLO con auriculares
            if (hayAuricularesConectados()) {
                reproducirSonidoSutil();
            }
        }

        // Broadcast para que WorkoutActivity sepa que termino
        sendBroadcast(new Intent(ACTION_TIMER_FINISHED));
        stopSelf();
    }

    /**
     * Notificacion expandida (heads-up) - aparece como burbuja grande
     * tipo "Dynamic Island expandida" o notificacion de WhatsApp entrante.
     * Se auto-descarta al tocar.
     */
    private void mostrarNotificacionExpandida(String ejercicio, PendingIntent pi) {
        Notification expandida = new NotificationCompat.Builder(this, FitBaseApp.CANAL_TIMER_RELOJ)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Siguiente serie!")
                .setContentText(ejercicio)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setVibrate(new long[]{0}) // Trigger heads-up sin vibrar realmente
                .setSound(null)
                .setFullScreenIntent(pi, false)
                .build();

        android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
        nm.cancel(NOTIFICACION_ID);
        nm.notify(NOTIF_FIN_ID, expandida);
    }

    /**
     * Notificacion que Zepp replica al Amazfit GTS 4 -> vibracion en muneca.
     * No vibra el movil (canal configurado sin vibracion local).
     */
    private void enviarVibracionReloj(String ejercicio, PendingIntent pi) {
        Notification notifReloj = new NotificationCompat.Builder(this, FitBaseApp.CANAL_TIMER_RELOJ)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("VAMOS!")
                .setContentText("Siguiente serie: " + ejercicio)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(null)
                .setSound(null)
                .build();

        android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
        nm.notify(NOTIF_FIN_ID + 1, notifReloj);
    }

    /**
     * Detecta auriculares (cable, BT A2DP, BLE headset, USB).
     * Usa AudioDeviceInfo (API 23+) que es mas fiable que BluetoothAdapter.
     */
    private boolean hayAuricularesConectados() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device : devices) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                    || type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                    || type == AudioDeviceInfo.TYPE_BLE_HEADSET
                    || type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sonido sutil por STREAM_MUSIC - solo audible por auriculares.
     * Volumen reducido (30%). No es una alarma, es un "ding" discreto.
     */
    private void reproducirSonidoSutil() {
        try {
            Uri sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mp.setDataSource(this, sonido);
            mp.setVolume(0.3f, 0.3f);
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.prepare();
            mp.start();
        } catch (Exception ignored) {
            // No es critico
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
