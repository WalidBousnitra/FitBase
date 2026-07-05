package com.fitbase.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.VibratorManager;

/**
 * Para la alarma de "descanso terminado" (vibración + notificación) sin
 * depender de que TimerService esté vivo.
 *
 * ANTES el botón "Detener" usaba PendingIntent.getService() — pero
 * onTimerFinish() llama a stopSelf() justo después de mostrar el aviso, así
 * que para cuando el usuario pulsaba "Detener" el Service ya estaba parado,
 * y reiniciarlo desde un PendingIntent puede fallar en MIUI si la app no
 * tiene autoinicio permitido (con la app cerrada, "Detener" no hacía nada).
 * Un BroadcastReceiver registrado en el manifest no tiene ese problema: no
 * necesita ningún componente vivo para ejecutarse.
 */
public class TimerDetenerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        detenerAlarma(context);
    }

    /** También se llama desde onResume() de Home/Workout — si vuelves a la
     * app, la alarma se calla sola, no hace falta pulsar "Detener". */
    public static void detenerAlarma(Context context) {
        try {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) vm.getDefaultVibrator().cancel();
        } catch (Exception ignored) {}

        try {
            android.app.NotificationManager nm = context.getSystemService(android.app.NotificationManager.class);
            if (nm != null) nm.cancel(TimerService.NOTIF_FIN_ID);
        } catch (Exception ignored) {}
    }
}
