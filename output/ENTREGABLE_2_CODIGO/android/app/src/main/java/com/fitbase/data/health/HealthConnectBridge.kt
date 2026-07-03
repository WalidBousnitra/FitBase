package com.fitbase.data.health

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.NutritionRecord
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
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
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
     */
    @JvmStatic
    fun readRecoveryData(context: Context, diasAtras: Int): RecoveryData {
        val data = RecoveryData()
        try {
            val client = HealthConnectClient.getOrCreate(context)
            val desde = Instant.now().minus(Duration.ofDays(diasAtras.toLong()))
            val ahora = Instant.now()
            val timeFilter = TimeRangeFilter.between(desde, ahora)

            runBlocking {
                // PESO CORPORAL
                val pesoResult = client.readRecords(
                    ReadRecordsRequest(WeightRecord::class, timeRangeFilter = timeFilter)
                )
                for (record in pesoResult.records) {
                    val kg = record.weight.inKilograms
                    val fecha = record.time.atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    data.pesosKg.add(PesoEntry(fecha, kg))
                }
                Log.d(TAG, "Weight: ${pesoResult.records.size} records over $diasAtras days")

                // SUEÑO
                val sleepResult = client.readRecords(
                    ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter = timeFilter)
                )
                for (record in sleepResult.records) {
                    val fecha = record.startTime.atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    val duracionMin = Duration.between(record.startTime, record.endTime).toMinutes().toInt()
                    // Calcular sleep score heurístico (basado en duración)
                    // 8h=100, 7h=85, 6h=65, 5h=45, <5h=30
                    val horas = duracionMin / 60.0
                    val score = when {
                        horas >= 8.0 -> 100
                        horas >= 7.0 -> 85
                        horas >= 6.5 -> 75
                        horas >= 6.0 -> 65
                        horas >= 5.0 -> 45
                        else -> 30
                    }
                    data.suenos.add(SleepEntry(fecha, duracionMin, score))
                }
                Log.d(TAG, "Sleep: ${sleepResult.records.size} sessions over $diasAtras days")

                // FC REPOSO (mínimo nocturno de HeartRate samples)
                val hrResult = client.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = timeFilter)
                )
                // Agrupar samples por día, tomar mínimo como "FC reposo"
                val hrPorDia = mutableMapOf<String, MutableList<Long>>()
                for (record in hrResult.records) {
                    for (sample in record.samples) {
                        val fecha = sample.time.atZone(ZoneId.systemDefault()).toLocalDate().toString()
                        val hora = sample.time.atZone(ZoneId.systemDefault()).hour
                        // Solo samples nocturnos (00:00-06:00) para FC reposo
                        if (hora in 0..6) {
                            hrPorDia.getOrPut(fecha) { mutableListOf() }.add(sample.beatsPerMinute)
                        }
                    }
                }
                for ((fecha, bpms) in hrPorDia) {
                    val minBpm = bpms.minOrNull()?.toInt() ?: 0
                    if (minBpm > 30) { // Filtrar artefactos
                        data.fcReposo.add(HrEntry(fecha, minBpm))
                    }
                }
                Log.d(TAG, "HR: ${hrResult.records.size} records, ${data.fcReposo.size} days with resting HR")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading recovery data: ${e.javaClass.simpleName}: ${e.message}", e)
            // Si es SecurityException = no hay permisos. Los datos quedan vacíos.
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

    class PesoEntry(@JvmField val fecha: String, @JvmField val kg: Double)
    class SleepEntry(@JvmField val fecha: String, @JvmField val duracionMin: Int, @JvmField val score: Int)
    class HrEntry(@JvmField val fecha: String, @JvmField val bpm: Int)
}
