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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.widget.RemoteViews;

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
    static final int NOTIF_FIN_ID = 1002; // package-private: TimerDetenerReceiver también lo usa
    public static final String ACTION_TIMER_FINISHED = "com.fitbase.TIMER_FINISHED";
    public static final String ACTION_TIMER_TICK = "com.fitbase.TIMER_TICK";
    public static final String EXTRA_SEGUNDOS_RESTANTES = "segundos_restantes";
    public static final String ACTION_TIMER_DETENER = "com.fitbase.TIMER_DETENER";

    private Vibrator vibrator;

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
    public void onCreate() {
        super.onCreate();
        VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
        vibrator = vm != null ? vm.getDefaultVibrator() : null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int segundos = intent.getIntExtra("segundos", 120);
        if (segundos <= 0) segundos = 1;
        ejercicioNombre = intent.getStringExtra("ejercicio");
        if (ejercicioNombre == null) ejercicioNombre = "";

        // Tiempo absoluto de finalización — SIEMPRE el mismo instante que usa la
        // pantalla (WorkoutActivity.mostrarTimer calcula esto UNA vez y lo manda
        // aquí), para que Chronometer de la pantalla, notificación e Hyper Island
        // cuenten exactamente lo mismo. Si no viene (llamada externa), se calcula
        // aquí como respaldo.
        long finishElapsedExtra = intent.getLongExtra("finish_elapsed_ms", -1);
        long finishWhenExtra = intent.getLongExtra("finish_when_ms", -1);
        finishElapsedMs = finishElapsedExtra > 0
                ? finishElapsedExtra : SystemClock.elapsedRealtime() + (segundos * 1000L);
        long whenMs = finishWhenExtra > 0
                ? finishWhenExtra : System.currentTimeMillis() + (segundos * 1000L);
        finishHandled = false;

        // Intent para volver a la app
        Intent volverIntent = new Intent(this, WorkoutActivity.class);
        volverIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, volverIntent, PendingIntent.FLAG_IMMUTABLE);

        // Notificación con Chronometer nativo (cuenta atrás sin depender del proceso)
        Notification notificacion = crearNotificacion(whenMs, pendingIntent);
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
     * Notificación con layout custom — estilo "Live Activity" de iOS.
     * Compact: icono + nombre ejercicio + cronómetro countdown naranja.
     * El Chronometer del sistema funciona independientemente del proceso.
     *
     * En dispositivos Xiaomi con HyperOS (Redmi Note 14 Pro 5G incluido) esta
     * misma notificación además se muestra como Hyper Island (cápsula +
     * expandida) con cuenta atrás nativa — ver HyperIslandTimerBridge.
     */
    private Notification crearNotificacion(long whenMs, PendingIntent pi) {
        // Custom compact view con cronómetro naranja — usa finishElapsedMs (mismo
        // instante que el tick loop y que el Chronometer de la pantalla) en vez de
        // recalcularlo, para que los tres cuenten exactamente lo mismo.
        RemoteViews compactView = new RemoteViews(getPackageName(), R.layout.notification_timer_compact);
        compactView.setTextViewText(R.id.tvNotifExercise, ejercicioNombre);
        compactView.setChronometerCountDown(R.id.chrono, true);
        compactView.setChronometer(R.id.chrono, finishElapsedMs, null, true);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, FitBaseApp.CANAL_TIMER)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Descanso")
                .setContentText(ejercicioNombre)
                .setContentIntent(pi)
                .setCustomContentView(compactView)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(whenMs)
                .setShowWhen(true)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // Hyper Island (solo si el dispositivo lo soporta — si no, la notificación
        // normal de arriba funciona exactamente igual que antes, sin cambios).
        HyperIslandTimerBridge.IslandPayload island = null;
        if (HyperIslandTimerBridge.isSupported(this)) {
            island = HyperIslandTimerBridge.buildCountdownPayload(this, ejercicioNombre, whenMs, R.drawable.ic_timer);
        }
        if (island == null) {
            return builder.build();
        }

        builder.addExtras(island.resourceBundle);
        Notification notificacion = builder.build();
        notificacion.extras.putString("miui.focus.param", island.jsonParam);
        return notificacion;
    }

    /**
     * Lógica al terminar el timer.
     * Dentro de la app → SILENCIO (UI observa LiveData y transiciona sola).
     * Fuera → UNA sola notificación (antes eran 2): en dispositivos con Hyper
     * Island, la cápsula flota/se expande mostrando "¡Descanso terminado!" en
     * vez de un popup tradicional a pantalla completa; en el resto de
     * dispositivos se mantiene el comportamiento anterior (heads-up). La
     * vibración para el reloj (vía Zepp) va en esa misma notificación.
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
                mostrarFinDescanso(pi);
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
     * Única notificación de fin de descanso — heads-up "de verdad" (el
     * rectángulo grande que se superpone a lo que tengas abierto, igual que
     * WhatsApp con un mensaje entrante), NO la cápsula de Hyper Island: una
     * isla, por grande que se haga, sigue siendo una píldora flotante, no un
     * banner a todo el ancho — para ESTE aviso concreto (a diferencia de la
     * cuenta atrás en curso, que sigue usando Hyper Island sin cambios) se
     * usa siempre la notificación expandida clásica de Android. Persiste
     * hasta que se detiene (sin auto-cancelar) y vibra en bucle hasta pararla.
     */
    private void mostrarFinDescanso(PendingIntent pi) {
        // Broadcast, NO Service: onTimerFinish() para el Service justo después
        // de esto (stopSelf()), así que para cuando el usuario pulse "Detener"
        // ya no habría Service que reiniciar — y reiniciarlo desde un
        // PendingIntent puede fallar en MIUI con la app cerrada. Un
        // BroadcastReceiver estático no depende de que nada esté vivo.
        Intent detenerIntent = new Intent(ACTION_TIMER_DETENER);
        detenerIntent.setClass(this, TimerDetenerReceiver.class);
        PendingIntent piDetener = PendingIntent.getBroadcast(
                this, 1, detenerIntent, PendingIntent.FLAG_IMMUTABLE);

        RemoteViews expandedView = new RemoteViews(getPackageName(), R.layout.notification_timer_expanded);
        expandedView.setTextViewText(R.id.tvFinExercise, ejercicioNombre);

        Notification notificacion = new NotificationCompat.Builder(this, FitBaseApp.CANAL_TIMER_RELOJ)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("¡Siguiente serie!")
                .setContentText(ejercicioNombre)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true) // Persiste — como una alarma, no se descarta solo
                .addAction(R.drawable.ic_timer, "Detener", piDetener)
                .setSound(null)
                .setCustomBigContentView(expandedView)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setFullScreenIntent(pi, true) // Máxima urgencia → heads-up garantizado
                .build();

        android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
        if (nm != null) {
            nm.cancel(NOTIFICACION_ID);
            nm.notify(NOTIF_FIN_ID, notificacion);
        }

        // Vibración en bucle (como una alarma) hasta que se pulse "Detener".
        // Zepp la replica en el reloj mientras la notificación esté activa.
        if (vibrator != null) {
            long[] patron = {0, 400, 300};
            vibrator.vibrate(VibrationEffect.createWaveform(patron, 0));
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
        // OJO: NO cancelar el vibrator aquí. onTimerFinish() llama a stopSelf()
        // justo después de lanzar la vibración en bucle (mostrarFinDescanso) —
        // si se cancelase aquí, la vibración se pararía casi al instante en vez
        // de seguir hasta que el usuario pulse "Detener". El Vibrator es un
        // servicio del sistema — la vibración sigue activa aunque este Service
        // se destruya; detenerAlarmaFin() la para explícitamente.
    }

    /**
     * El usuario cerró la app de verdad (swipe en Recientes) — a diferencia
     * de cambiar a otra app un momento, que debe seguir contando/avisando en
     * segundo plano (ese es el comportamiento normal ya implementado). Cerrar
     * la tarea entera se interpreta como "abandono el descanso": se para todo
     * — Handler, vibración (aquí SÍ, a diferencia de onDestroy) y notificación
     * (que se lleva la isla de Hyper Island con ella, ya que vive dentro de la
     * misma notificación, no aparte).
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        handler.removeCallbacksAndMessages(null);
        if (vibrator != null) vibrator.cancel();

        android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
        if (nm != null) {
            nm.cancel(NOTIFICACION_ID);
            nm.cancel(NOTIF_FIN_ID);
        }

        stopSelf();
        super.onTaskRemoved(rootIntent);
    }
}
