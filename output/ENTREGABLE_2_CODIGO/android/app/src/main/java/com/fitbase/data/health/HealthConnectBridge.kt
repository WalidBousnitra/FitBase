package com.fitbase.data.health

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Kotlin bridge for Health Connect SDK (Kotlin-first API).
 * Called from Java via HealthConnectReader.
 *
 * Lee TODOS los datos que necesita FitBase:
 * - Pasos (Zepp/Amazfit → Health Connect)
 * - Nutrición (FatSecret → Health Connect)
 * - Peso corporal (balanza manual o Mi Fitness)
 * - Sueño (Zepp sleep tracking)
 * - FC reposo (Zepp heart rate, min nocturno)
 */
object HealthConnectBridge {

    private const val TAG = "HealthConnectBridge"

    private val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(BodyWaterMassRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class)
    )

    @JvmStatic
    fun isAvailable(context: Context): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata")
            status == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            Log.w(TAG, "HC SDK check failed", e)
            false
        }
    }

    /** Returns the permission contract for use with registerForActivityResult */
    @JvmStatic
    fun getPermissionContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    /** Returns the set of permissions needed */
    @JvmStatic
    fun getRequiredPermissions(): Set<String> = PERMISSIONS

    /** Checks if all required permissions are already granted */
    @JvmStatic
    fun requestPermissionsIfNeeded(context: Context, launcher: ActivityResultLauncher<Set<String>>) {
        if (isAvailable(context) && !hasPermissions(context)) {
            launcher.launch(PERMISSIONS)
        }
    }

    /** Checks if all required permissions are already granted */
    @JvmStatic
    fun hasPermissions(context: Context): Boolean {
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            runBlocking {
                client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "HC permissions check failed", e)
            false
        }
    }

    /**
     * Total de pasos de un día YA CERRADO (00:00-24:00 completas), a
     * diferencia de {@link #readTodayData} que corta en el instante actual.
     * Usado para cerrar el día anterior al detectar el cambio de fecha (ver
     * DailySyncManager#cerrarDiaAnteriorSiHaceFalta) — así un paseo nocturno
     * posterior a la última vez que se abrió la app ese día no se pierde.
     * Debe llamarse desde background thread.
     */
    @JvmStatic
    fun readStepsForDate(context: Context, fechaStr: String): Int {
        var pasos = 0
        try {
            val client = HealthConnectClient.getOrCreate(context)
            val fecha = LocalDate.parse(fechaStr)
            val inicio = fecha.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val fin = fecha.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val timeFilter = TimeRangeFilter.between(inicio, fin)

            runBlocking {
                val stepsResult = client.readRecords(
                    ReadRecordsRequest(StepsRecord::class, timeRangeFilter = timeFilter)
                )
                for (record in stepsResult.records) {
                    pasos += record.count.toInt()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading steps for date $fechaStr", e)
        }
        return pasos
    }

    /**
     * Lee datos del día de hoy: pasos, nutrición.
     * Debe llamarse desde background thread.
     */
    @JvmStatic
    fun readTodayData(context: Context): HealthData {
        val data = HealthData()
        try {
            val client = HealthConnectClient.getOrCreate(context)
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val now = Instant.now()
            val timeFilter = TimeRangeFilter.between(startOfDay, now)

            runBlocking {
                // PASOS
                val stepsResult = client.readRecords(
                    ReadRecordsRequest(StepsRecord::class, timeRangeFilter = timeFilter)
                )
                for (record in stepsResult.records) {
                    data.pasos += record.count.toInt()
                }
                Log.d(TAG, "Steps: ${stepsResult.records.size} records, total=${data.pasos}")

                // NUTRICIÓN (FatSecret u otra app)
                val nutriResult = client.readRecords(
                    ReadRecordsRequest(NutritionRecord::class, timeRangeFilter = timeFilter)
                )
                var kcalFromEnergy = 0
                var kcalFromMacros = 0
                for (record in nutriResult.records) {
                    record.energy?.let {
                        val kcal = it.inKilocalories.toInt()
                        data.caloriasConsumidas += kcal
                        kcalFromEnergy += kcal
                    }
                    record.protein?.let { data.proteinaG += it.inGrams.toInt() }
                    record.totalCarbohydrate?.let { data.carbosG += it.inGrams.toInt() }
                    record.totalFat?.let { data.grasasG += it.inGrams.toInt() }

                    // Fallback: si energy es null pero hay macros → calcular kcal
                    if (record.energy == null) {
                        val p = record.protein?.inGrams?.toInt() ?: 0
                        val c = record.totalCarbohydrate?.inGrams?.toInt() ?: 0
                        val g = record.totalFat?.inGrams?.toInt() ?: 0
                        val kcal = (p * 4) + (c * 4) + (g * 9)
                        if (kcal > 0) {
                            data.caloriasConsumidas += kcal
                            kcalFromMacros += kcal
                        }
                    }
                }
                Log.d(TAG, "Nutrition: ${nutriResult.records.size} records, kcalEnergy=$kcalFromEnergy kcalMacros=$kcalFromMacros total=${data.caloriasConsumidas} P=${data.proteinaG} C=${data.carbosG} G=${data.grasasG}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading today data", e)
        }
        return data
    }

    /**
     * Lee datos de recuperación: peso corporal, sueño, FC reposo.
     * Para progresión y motor de cargas (Kiviniemi 2007).
     * Debe llamarse desde background thread.
     *
     * Cada tipo de dato (peso+grasa+agua / sueño / FC reposo) tiene su PROPIO
     * permiso de Health Connect, concedibles por separado en el diálogo del
     * sistema — el usuario puede aceptar Pasos+Peso+Sueño pero dejar sin
     * marcar FC reposo, por ejemplo. Antes las 5 lecturas compartían un único
     * try/catch: si UNA sola fallaba (permiso no concedido, o el tipo de
     * registro no existe porque ninguna app escribe ahí, p.ej.
     * BodyWaterMassRecord si la báscula no mide hidratación), la excepción
     * abortaba el bloque entero y se perdían TAMBIÉN las demás lecturas que
     * sí habrían funcionado — por eso sueño/FC/peso podían llegar vacíos a la
     * vez aunque solo uno de los permisos fallara. Ahora cada grupo tiene su
     * propio try/catch: un fallo aislado no tumba a los demás.
     */
    @JvmStatic
    fun readRecoveryData(context: Context, diasAtras: Int): RecoveryData {
        val data = RecoveryData()
        val client = HealthConnectClient.getOrCreate(context)
        val desde = Instant.now().minus(Duration.ofDays(diasAtras.toLong()))
        val ahora = Instant.now()
        val timeFilter = TimeRangeFilter.between(desde, ahora)

        // PESO CORPORAL + COMPOSICIÓN (grasa %, agua)
        // Grasa visceral NO existe en Health Connect — ni como registro ni
        // como campo de ninguno de los existentes — es propietario de
        // Xiaomi/Mi Fitness (confirmado en el SDK). Sigue siendo entrada
        // manual (ver hardware.md). Aquí se leen los 3 datos que Health
        // Connect SÍ expone de forma literal: peso, % grasa, y masa de agua
        // (convertida a % dividiendo por el peso del mismo día — conversión
        // de unidades entre dos medidas reales, no una estimación). Grasa/agua
        // son opcionales: si fallan (permiso no concedido, sin fuente de
        // datos), el peso se guarda igual, solo sin ese enriquecido.
        try {
            runBlocking {
                val pesoResult = client.readRecords(
                    ReadRecordsRequest(WeightRecord::class, timeRangeFilter = timeFilter)
                )
                val grasaPctPorDia = mutableMapOf<String, Double>()
                try {
                    val grasaResult = client.readRecords(
                        ReadRecordsRequest(BodyFatRecord::class, timeRangeFilter = timeFilter)
                    )
                    for (record in grasaResult.records) {
                        val fecha = record.time.atZone(ZoneId.systemDefault()).toLocalDate().toString()
                        grasaPctPorDia[fecha] = record.percentage.value
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Sin acceso a % grasa (BodyFatRecord): ${e.message}")
                }
                val aguaKgPorDia = mutableMapOf<String, Double>()
                try {
                    val aguaResult = client.readRecords(
                        ReadRecordsRequest(BodyWaterMassRecord::class, timeRangeFilter = timeFilter)
                    )
                    for (record in aguaResult.records) {
                        val fecha = record.time.atZone(ZoneId.systemDefault()).toLocalDate().toString()
                        aguaKgPorDia[fecha] = record.mass.inKilograms
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Sin acceso a masa de agua (BodyWaterMassRecord): ${e.message}")
                }

                for (record in pesoResult.records) {
                    val kg = record.weight.inKilograms
                    val fecha = record.time.atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    val grasaPct = grasaPctPorDia[fecha]
                    val aguaKg = aguaKgPorDia[fecha]
                    val hidratacionPct = if (aguaKg != null && kg > 0) (aguaKg / kg * 100.0) else null
                    data.pesosKg.add(PesoEntry(fecha, kg, grasaPct, hidratacionPct))
                }
                Log.d(TAG, "Weight: ${pesoResult.records.size} records, grasa: ${grasaPctPorDia.size}, agua: ${aguaKgPorDia.size} (over $diasAtras days)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sin acceso a peso (WeightRecord): ${e.javaClass.simpleName}: ${e.message}", e)
        }

        // SUEÑO
        // Health Connect NO expone el "Sleep Score" propietario de Zepp (no existe
        // ese campo en SleepSessionRecord, solo fases en bruto) — así que el score
        // que calculamos aquí es una ESTIMACIÓN a partir de datos crudos reales
        // (duración, profundo, REM, ligero), no el mismo número que calcula Zepp.
        // Ver el cálculo más abajo para la fórmula y sus fuentes.
        //
        // Zepp puede escribir la noche como varias SleepSessionRecord separadas
        // (p.ej. interrupciones), así que se agrupan por "noche" para sumar los minutos.
        try {
            runBlocking {
                val sleepResult = client.readRecords(
                    ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter = timeFilter)
                )

                class NocheAgg {
                    var deepMin = 0L
                    var remMin = 0L
                    var lightMin = 0L
                    var sleepingMin = 0L
                    var duracionTotalMin = 0L
                }

                val porNoche = mutableMapOf<String, NocheAgg>()
                for (record in sleepResult.records) {
                    val inicioLocal = record.startTime.atZone(ZoneId.systemDefault())
                    // Sesión iniciada de madrugada (00:00-11:59) → pertenece a la noche de esa fecha.
                    // Sesión iniciada por la tarde/noche (12:00-23:59) → pertenece a la mañana siguiente.
                    val fechaNoche = if (inicioLocal.hour < 12) inicioLocal.toLocalDate()
                                      else inicioLocal.toLocalDate().plusDays(1)
                    val key = fechaNoche.toString()
                    val agg = porNoche.getOrPut(key) { NocheAgg() }

                    agg.duracionTotalMin += Duration.between(record.startTime, record.endTime).toMinutes()

                    for (stage in record.stages) {
                        val minutos = Duration.between(stage.startTime, stage.endTime).toMinutes()
                        when (stage.stage) {
                            SleepSessionRecord.STAGE_TYPE_DEEP -> agg.deepMin += minutos
                            SleepSessionRecord.STAGE_TYPE_REM -> agg.remMin += minutos
                            SleepSessionRecord.STAGE_TYPE_LIGHT -> agg.lightMin += minutos
                            SleepSessionRecord.STAGE_TYPE_SLEEPING -> agg.sleepingMin += minutos
                            else -> {} // AWAKE / OUT_OF_BED / AWAKE_IN_BED / UNKNOWN — no cuentan como dormido
                        }
                    }
                }

                for ((fecha, agg) in porNoche.toSortedMap()) {
                    val totalDormidoMin = agg.deepMin + agg.remMin + agg.lightMin + agg.sleepingMin
                    val duracionFinalMin = if (totalDormidoMin > 0) totalDormidoMin.toInt()
                                            else agg.duracionTotalMin.toInt()

                    // Score ESTIMADO 0-100 (no es el de Zepp — HC no lo tiene) en base a
                    // 3 factores, cada uno ya acotado 0-100, pesos suman 1 (no puede pasar
                    // de 100 ni saturar siempre ahí, a diferencia de un intento anterior):
                    //   1) Duración vs objetivo de 7.5h (evidencia/sueno.md: 7-9h adulto,
                    //      se usa el extremo bajo para no penalizar noches normales) → 50%
                    //   2) Eficiencia: % del tiempo en cama que se pasó dormido           → 20%
                    //   3) Cercanía de % profundo/REM a rangos fisiológicos típicos       → 30%
                    //      (profundo ~13-23% del sueño, REM ~20-25% — Ohayon et al. 2004)
                    val score = if (totalDormidoMin > 0) {
                        val minutosObjetivo = 450.0 // 7.5h
                        val scoreDuracion = (totalDormidoMin / minutosObjetivo * 100.0).coerceIn(0.0, 100.0)

                        val scoreEficiencia = if (agg.duracionTotalMin > 0)
                            (totalDormidoMin.toDouble() / agg.duracionTotalMin * 100.0).coerceIn(0.0, 100.0)
                        else 100.0

                        val deepPct = agg.deepMin.toDouble() / totalDormidoMin * 100.0
                        val remPct = agg.remMin.toDouble() / totalDormidoMin * 100.0
                        // Pendiente de penalización más suave (3.0) que el intento anterior (4.0)
                        // — un desvío moderado de los rangos ideales no debería hundir el score.
                        val scoreDeep = (100.0 - (kotlin.math.abs(deepPct - 18.0) * 3.0)).coerceIn(0.0, 100.0)
                        val scoreRem = (100.0 - (kotlin.math.abs(remPct - 22.5) * 3.0)).coerceIn(0.0, 100.0)
                        val scoreFases = (scoreDeep + scoreRem) / 2.0

                        (scoreDuracion * 0.5 + scoreEficiencia * 0.2 + scoreFases * 0.3)
                            .let { Math.round(it).toInt() }.coerceIn(0, 100)
                    } else {
                        // Sin desglose de fases (dispositivo no las reportó) — fallback
                        // solo por duración total, sin inventar composición de fases.
                        val horas = agg.duracionTotalMin / 60.0
                        when {
                            horas >= 7.5 -> 90
                            horas >= 6.5 -> 78
                            horas >= 6.0 -> 68
                            horas >= 5.0 -> 55
                            else -> 40
                        }
                    }

                    data.suenos.add(SleepEntry(
                        fecha, duracionFinalMin, score,
                        agg.deepMin.toInt(), agg.remMin.toInt(), agg.lightMin.toInt() + agg.sleepingMin.toInt()
                    ))
                }
                Log.d(TAG, "Sleep: ${sleepResult.records.size} sesiones agrupadas en ${porNoche.size} noches")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sin acceso a sueño (SleepSessionRecord): ${e.javaClass.simpleName}: ${e.message}", e)
        }

        // FC REPOSO — literal, tal cual la calcula Zepp/Amazfit.
        // RestingHeartRateRecord es un dato propio del dispositivo (no HeartRateRecord
        // continuo): no se estima ni se deriva nada aquí. Si un día no está, se omite
        // (mejor "sin dato" que un número inventado).
        try {
            runBlocking {
                val restingResult = client.readRecords(
                    ReadRecordsRequest(RestingHeartRateRecord::class, timeRangeFilter = timeFilter)
                )
                val fcPorDia = mutableMapOf<String, Int>()
                for (record in restingResult.records) {
                    val fecha = record.time.atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    fcPorDia[fecha] = record.beatsPerMinute.toInt()
                }
                Log.d(TAG, "RestingHeartRate: ${restingResult.records.size} records, ${fcPorDia.size} días")

                for ((fecha, bpm) in fcPorDia.toSortedMap()) {
                    data.fcReposo.add(HrEntry(fecha, bpm))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sin acceso a FC reposo (RestingHeartRateRecord): ${e.javaClass.simpleName}: ${e.message}", e)
        }
        return data
    }

    // ── Data classes ──

    /** Datos del día (pasos + nutrición) */
    class HealthData {
        @JvmField var pasos: Int = 0
        @JvmField var caloriasConsumidas: Int = 0
        @JvmField var proteinaG: Int = 0
        @JvmField var carbosG: Int = 0
        @JvmField var grasasG: Int = 0
    }

    /** Datos de recuperación (peso, sueño, FC) para progresión y motor */
    class RecoveryData {
        @JvmField var pesosKg: MutableList<PesoEntry> = mutableListOf()
        @JvmField var suenos: MutableList<SleepEntry> = mutableListOf()
        @JvmField var fcReposo: MutableList<HrEntry> = mutableListOf()
    }

    class PesoEntry(
        @JvmField val fecha: String,
        @JvmField val kg: Double,
        @JvmField val grasaPct: Double? = null,
        @JvmField val hidratacionPct: Double? = null
    )
    class SleepEntry(
        @JvmField val fecha: String,
        @JvmField val duracionMin: Int,
        /** Score ESTIMADO 0-100 (no el de Zepp — Health Connect no lo tiene). */
        @JvmField val score: Int,
        @JvmField val deepMin: Int = 0,
        @JvmField val remMin: Int = 0,
        @JvmField val lightMin: Int = 0
    )
    class HrEntry(@JvmField val fecha: String, @JvmField val bpm: Int)
}
