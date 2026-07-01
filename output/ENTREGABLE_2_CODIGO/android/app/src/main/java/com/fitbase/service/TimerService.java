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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;

import com.fitbase.FitBaseApp;
import com.fitbase.R;
import com.fitbase.ui.workout.WorkoutActivity;

/**
 * Foreground Service - Timer de descanso entre series.
 *
 * Enfoque: TIEMPO ABSOLUTO + HANDLER (no CountDownTimer, no MediaSession).
 * - Calcula el instante exacto en que debe terminar (elapsedRealtime).
 * - Un Handler con postDelayed(1s) actualiza la notificación cada segundo.
 * - Chronometer nativo de la notificación funciona independiente del proceso.
 * - Si el SO mata el handler, el Chronometer sigue contando visualmente.
 *
 * UX:
 *   - DURANTE: Notificación compacta con cronómetro nativo (cuenta atrás).
 *   - AL ACABAR (fuera de la app):
 *       -> Notificación HEADS-UP con "Siguiente serie!"
 *       -> Sonido sutil SOLO con auriculares
 *       -> Vibración en reloj Amazfit via Zepp (notificación alta prioridad)
 *       -> NO vibra el móvil, NO suena por altavoz
 *   - AL ACABAR (dentro de la app):
 *       -> SILENCIO TOTAL. La UI transiciona automáticamente.
 *
 * Referencia: REG-DEV-01 (ui.md) 4.4, 7.1, 7.2
 */
public class TimerService extends Service {

    private static final int NOTIFICACION_ID = 1001;
    private static final int NOTIF_FIN_ID = 1002;
    public static final String ACTION_TIMER_FINISHED = "com.fitbase.TIMER_FINISHED";
    public static final String ACTION_TIMER_TICK = "com.fitbase.TIMER_TICK";
    public static final String EXTRA_SEGUNDOS_RESTANTES = "segundos_restantes";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long finishElapsedMs; // SystemClock.elapsedRealtime() cuando debe acabar
    private boolean finishHandled = false;
    private String ejercicioNombre = "";

    // Flag estático para saber si la app está en primer plano
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
        if (segundos <= 0) segundos = 1;
        ejercicioNombre = intent.getStringExtra("ejercicio_nombre");
        if (ejercicioNombre == null) ejercicioNombre = "";

        // Tiempo absoluto de finalización (inmune a delays del SO)
        finishElapsedMs = SystemClock.elapsedRealtime() + (segundos * 1000L);
        finishHandled = false;

        // Intent para volver a la app
        Intent volverIntent = new Intent(this, WorkoutActivity.class);
        volverIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, volverIntent, PendingIntent.FLAG_IMMUTABLE);

        // Notificación con Chronometer nativo (cuenta atrás sin depender del proceso)
        Notification notificacion = crearNotificacion(segundos, pendingIntent);
        try {
            startForeground(NOTIFICACION_ID, notificacion);
        } catch (Exception e) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Handler que verifica cada segundo si el timer terminó
        handler.removeCallbacksAndMessages(null);
        handler.post(tickRunnable);

        return START_NOT_STICKY;
    }

    /**
     * Runnable que se ejecuta cada segundo.
     * Calcula segundos restantes desde tiempo absoluto (preciso incluso si hay delays).
     * Envía broadcast con segundos restantes para que la UI se actualice.
     */
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            long ahora = SystemClock.elapsedRealtime();
            long restanteMs = finishElapsedMs - ahora;
            int restanteSeg = (int) Math.ceil(restanteMs / 1000.0);

            if (restanteSeg <= 0) {
                // TERMINÓ
                if (!finishHandled) {
                    finishHandled = true;
                    // Broadcast tick final (0 segundos)
                    Intent tickIntent = new Intent(ACTION_TIMER_TICK);
                    tickIntent.putExtra(EXTRA_SEGUNDOS_RESTANTES, 0);
                    tickIntent.setPackage(getPackageName());
                    sendBroadcast(tickIntent);

                    onTimerFinish();
                }
                return;
            }

            // Broadcast con segundos restantes (la UI escucha esto)
            Intent tickIntent = new Intent(ACTION_TIMER_TICK);
            tickIntent.putExtra(EXTRA_SEGUNDOS_RESTANTES, restanteSeg);
            tickIntent.setPackage(getPackageName());
            sendBroadcast(tickIntent);

            // Siguiente tick en 1 segundo
            handler.postDelayed(this, 1000);
        }
    };

    /**
     * Notificación con Chronometer nativo — cuenta atrás visible en barra de estado.
     * El Chronometer del sistema funciona independientemente del proceso de la app.
     */
    private Notification crearNotificacion(int segundos, PendingIntent pi) {
        long whenMs = System.currentTimeMillis() + (segundos * 1000L);

        return new NotificationCompat.Builder(this, FitBaseApp.CANAL_TIMER)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Descanso")
                .setContentText(ejercicioNombre)
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(whenMs)
                .setShowWhen(true)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    /**
     * Lógica al terminar el timer.
     * Dentro de la app → SILENCIO (UI observa LiveData y transiciona sola).
     * Fuera → notificación expandida + sonido auriculares + vibrar reloj.
     */
    private void onTimerFinish() {
        try {
            Intent volverIntent = new Intent(this, WorkoutActivity.class);
            volverIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pi = PendingIntent.getActivity(
                    this, 0, volverIntent, PendingIntent.FLAG_IMMUTABLE);

            if (appEnPrimerPlano) {
                // DENTRO de la app → SILENCIO TOTAL
                android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
                if (nm != null) nm.cancel(NOTIFICACION_ID);
            } else {
                // FUERA de la app → Expandir notificación + sonido auriculares
                mostrarNotificacionFin(pi);
                enviarVibracionReloj(pi);
                if (hayAuricularesConectados()) {
                    reproducirSonidoSutil();
                }
            }
        } catch (Exception ignored) {
            // No crashear el servicio por errores de notificación
        }

        // Broadcast para que WorkoutActivity sepa que terminó
        Intent finishIntent = new Intent(ACTION_TIMER_FINISHED);
        finishIntent.setPackage(getPackageName());
        sendBroadcast(finishIntent);
        stopSelf();
    }

    /**
     * Notificación heads-up al terminar (tipo llamada entrante).
     */
    private void mostrarNotificacionFin(PendingIntent pi) {
        Notification expandida = new NotificationCompat.Builder(this, FitBaseApp.CANAL_TIMER_RELOJ)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("¡Siguiente serie!")
                .setContentText(ejercicioNombre)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setVibrate(new long[]{0}) // Trigger heads-up sin vibrar
                .setSound(null)
                .build();

        android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
        if (nm != null) {
            nm.cancel(NOTIFICACION_ID);
            nm.notify(NOTIF_FIN_ID, expandida);
        }
    }

    /**
     * Notificación que Zepp replica al Amazfit GTS 4 → vibración en muñeca.
     */
    private void enviarVibracionReloj(PendingIntent pi) {
        Notification notifReloj = new NotificationCompat.Builder(this, FitBaseApp.CANAL_TIMER_RELOJ)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("VAMOS!")
                .setContentText("Siguiente serie: " + ejercicioNombre)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(null)
                .setSound(null)
                .build();

        android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
        if (nm != null) {
            nm.notify(NOTIF_FIN_ID + 1, notifReloj);
        }
    }

    /**
     * Detecta auriculares (cable, BT A2DP, BLE headset, USB).
     */
    private boolean hayAuricularesConectados() {
        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return false;
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
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Sonido sutil por STREAM_MUSIC (solo audible por auriculares).
     * Volumen reducido (30%).
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
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
