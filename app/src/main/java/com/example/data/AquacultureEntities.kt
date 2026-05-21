package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "feed_logs")
data class FeedLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val feedType: String, // Starter, Grower, Finisher, etc.
    val quantityKg: Double,
    val costPerKg: Double = 1.8, // Standard price for feed valuation
    val pondName: String,
    val remarks: String = ""
) : Serializable

@Entity(tableName = "medicine_logs")
data class MedicineLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val medicineName: String, // Probiotics, Vitamins, Sanitizer, Minerals
    val dosage: String, // e.g., "500g", "10ml/kg"
    val purpose: String, // e.g., "Water quality", "Gut health"
    val quantityUsed: Double = 1.0,
    val unit: String = "kg",
    val pondName: String,
    val remarks: String = ""
) : Serializable

@Entity(tableName = "count_records")
data class CountRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val pondName: String,
    val daysOfCulture: Int, // DOC (Age in days)
    val averageBodyWeightGrams: Double, // ABW in grams
    val estimatedRemainingStock: Int, // Count of surviving seed/shrimp
    val seedValuationRatePerThousand: Double, // Valuation rate e.g., $15 per 1000 pcs or equivalent currency
    val nextCountDate: String, // YYYY-MM-DD (Remainder date)
    val remarks: String = ""
) : Serializable {
    val totalSeedValuation: Double
        get() = (estimatedRemainingStock * seedValuationRatePerThousand) / 1000.0
}

@Entity(tableName = "tray_check_alarms")
data class TrayCheckAlarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pondName: String,
    val trayNumber: Int,
    val scheduledTime: String, // "HH:mm" (e.g. "14:30")
    val epochScheduledTime: Long, // milliseconds
    val minutesOffset: Int = 90, // default time to check after feeding (e.g., 90 mins)
    val isCompleted: Boolean = false,
    val feedRemainingStatus: String = "Pending", // "Clean" (Normal), "Leftover" (Overfed), "Deficient" (Underfed), "Pending"
    val remarks: String = ""
) : Serializable {
    val isAlarmActive: Boolean
        get() = !isCompleted && System.currentTimeMillis() > epochScheduledTime
}

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val userId: String, // Google unique ID or phone number
    val userName: String,
    val email: String,
    val phone: String,
    val role: String, // "Owner", "Pond Manager", "Supervisor"
    val primaryPondName: String = "Pond A1",
    val loginMethod: String, // "Google" or "Mobile"
    val lastSyncTime: Long = System.currentTimeMillis(),
    val isCloudSyncEnabled: Boolean = true
) : Serializable

@Entity(tableName = "ponds")
data class Pond(
    @PrimaryKey val name: String,
    val dateCreated: String = "",
    val activeSeedStock: Int = 150000,
    val targetSurvivingPercentage: Double = 85.0,
    val areaInAcres: Double = 1.0
) : Serializable

@Entity(tableName = "ap_market_rates")
data class ApMarketRate(
    @PrimaryKey val count: Int, // Count of shrimp per Kg (e.g. 30, 40, 50, 60, 70, 80, 100)
    val ratePerKgInInr: Double, // Price in ₹
    val lastUpdatedDate: String // YYYY-MM-DD
) : Serializable
