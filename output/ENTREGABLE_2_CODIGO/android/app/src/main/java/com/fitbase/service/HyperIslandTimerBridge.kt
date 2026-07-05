package com.fitbase.service

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.TimerInfo

/**
 * Puente Kotlin hacia HyperIsland ToolKit (librería Kotlin-first, con DSL de
 * argumentos con nombre y valores por defecto que Java no puede llamar
 * directamente). Llamado desde TimerService.java, igual que
 * HealthConnectBridge.kt hace de puente para Health Connect.
 *
 * Análisis de las plantillas disponibles en la librería (ver
 * HyperIslandNotification.Builder): Chat/texto, Media, Progreso lineal,
 * Progreso circular, Countdown/CountUp (Timer), Highlight, Cover, pasos, etc.
 *
 * Para el timer de descanso, la plantilla que mejor encaja es el
 * COUNTDOWN nativo (setBigIslandCountdown + setSmallIsland + TimerInfo):
 * el sistema hace el "tick" de la cuenta atrás él solo (no hay que actualizar
 * la notificación cada segundo desde la app, a diferencia de progreso lineal
 * o circular, que si quisiéramos verlos moverse en tiempo real sí requerirían
 * refrescar la notificación nosotros). Es exactamente el mismo patrón que ya
 * usamos con el Chronometer nativo de Android en la notificación normal
 * (TimerService.crearNotificacion), así que no añade coste de batería extra.
 */
object HyperIslandTimerBridge {

    private const val TAG = "HyperIslandTimer"
    private const val ICON_KEY = "fitbase_timer_icon"

    @JvmStatic
    fun isSupported(context: Context): Boolean {
        return try {
            HyperIslandNotification.isSupported(context)
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Construye el payload de Hyper Island para el countdown de descanso.
     *
     * @param whenMs        instante (epoch millis, System.currentTimeMillis())
     *                       en que termina el descanso — mismo valor usado ya
     *                       para el Chronometer nativo, para que ambos coincidan.
     * @param ejercicioNombre nombre del ejercicio, mostrado junto a la cuenta atrás.
     * @param iconRes       drawable del icono (se reutiliza ic_timer existente).
     * @return el bundle de recursos + el JSON del parámetro, o null si algo falla
     *         (p.ej. dispositivo no-Xiaomi o versión de HyperOS no compatible).
     */
    @JvmStatic
    fun buildCountdownPayload(
        context: Context,
        ejercicioNombre: String,
        whenMs: Long,
        iconRes: Int
    ): IslandPayload? {
        return try {
            val icon = Icon.createWithResource(context, iconRes)
            val timer = TimerInfo(-1, whenMs, System.currentTimeMillis(), System.currentTimeMillis())

            val builder = HyperIslandNotification.Builder(context, "fitbase_timer", "Descanso")
                .addPicture(HyperPicture(ICON_KEY, icon))
                .setChatInfo(title = "Descanso", content = ejercicioNombre, pictureKey = ICON_KEY, timer = timer)
                .setBigIslandCountdown(whenMs, ICON_KEY)
                .setSmallIslandIcon(ICON_KEY)

            IslandPayload(builder.buildResourceBundle(), builder.buildJsonParam())
        } catch (e: Throwable) {
            Log.w(TAG, "No se pudo construir el payload de Hyper Island: ${e.message}")
            null
        }
    }

    class IslandPayload(@JvmField val resourceBundle: Bundle, @JvmField val jsonParam: String)
}
