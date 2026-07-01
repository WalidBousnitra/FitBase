package com.fitbase.data.health

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Kotlin bridge for Health Connect SDK (Kotlin-first API).
 * Called from Java via HealthConnectReader.
 */
object HealthConnectBridge {

    private const val TAG = "HealthConnectBridge"

    private val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class)
    )

    @JvmStatic
    fun isAvailable(context: Context): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata")
            status == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
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
            false
        }
    }

    /**
     * Reads today's steps and nutrition from Health Connect (blocking).
     * Must be called from a background thread.
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
                // Steps
                val stepsRequest = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = timeFilter
                )
                val stepsResult = client.readRecords(stepsRequest)
                for (record in stepsResult.records) {
                    data.pasos += record.count.toInt()
                }
                Log.d(TAG, "Steps records=${stepsResult.records.size} total=${data.pasos}")

                // Nutrition
                val nutriRequest = ReadRecordsRequest(
                    recordType = NutritionRecord::class,
                    timeRangeFilter = timeFilter
                )
                val nutriResult = client.readRecords(nutriRequest)
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
                Log.d(
                    TAG,
                    "Nutrition records=${nutriResult.records.size} kcalEnergy=$kcalFromEnergy kcalMacros=$kcalFromMacros totalKcal=${data.caloriasConsumidas} p=${data.proteinaG} c=${data.carbosG} g=${data.grasasG}"
                )
            }
        } catch (e: Exception) {
            // HC not available or no permissions — data stays at 0
            Log.e(TAG, "Error leyendo datos HC", e)
        }
        return data
    }

    /** Simple data holder accessible from Java */
    class HealthData {
        @JvmField var pasos: Int = 0
        @JvmField var caloriasConsumidas: Int = 0
        @JvmField var proteinaG: Int = 0
        @JvmField var carbosG: Int = 0
        @JvmField var grasasG: Int = 0
    }
}
