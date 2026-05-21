package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class AquacultureRepository(private val dao: AquacultureDao) {

    // --- Feed Queries ---
    val allFeedLogs: Flow<List<FeedLog>> = dao.getAllFeedLogs()
    val totalFeedUsage: Flow<Double?> = dao.getTotalFeedUsage()

    suspend fun insertFeedLog(feedLog: FeedLog) = dao.insertFeedLog(feedLog)
    suspend fun deleteFeedLog(feedLog: FeedLog) = dao.deleteFeedLog(feedLog)


    // --- Medicine Queries ---
    val allMedicineLogs: Flow<List<MedicineLog>> = dao.getAllMedicineLogs()

    suspend fun insertMedicineLog(medicineLog: MedicineLog) = dao.insertMedicineLog(medicineLog)
    suspend fun deleteMedicineLog(medicineLog: MedicineLog) = dao.deleteMedicineLog(medicineLog)


    // --- Count Records & Valuations ---
    val allCountRecords: Flow<List<CountRecord>> = dao.getAllCountRecords()
    val chronologicalCountRecords: Flow<List<CountRecord>> = dao.getCountRecordsChronological()

    suspend fun insertCountRecord(countRecord: CountRecord) = dao.insertCountRecord(countRecord)
    suspend fun deleteCountRecord(countRecord: CountRecord) = dao.deleteCountRecord(countRecord)


    // --- Tray Alarm Queries ---
    val allTrayCheckAlarms: Flow<List<TrayCheckAlarm>> = dao.getAllTrayCheckAlarms()
    val activeTrayCheckAlarms: Flow<List<TrayCheckAlarm>> = dao.getActiveTrayCheckAlarms()

    suspend fun insertTrayCheckAlarm(alarm: TrayCheckAlarm) = dao.insertTrayCheckAlarm(alarm)
    suspend fun updateTrayCheckAlarm(alarm: TrayCheckAlarm) = dao.updateTrayCheckAlarm(alarm)
    suspend fun deleteTrayCheckAlarmById(id: Int) = dao.deleteTrayCheckAlarmById(id)


    // --- User Profile Queries ---
    val currentUserProfile: Flow<UserProfile?> = dao.getCurrentUserProfileFlow()

    suspend fun getCurrentUser(): UserProfile? = dao.getCurrentUserProfile()
    suspend fun saveUserProfile(profile: UserProfile) = dao.saveUserProfile(profile)
    suspend fun clearUserProfile() = dao.clearUserProfiles()


    // --- Pond Queries ---
    val allPonds: Flow<List<Pond>> = dao.getAllPonds()
    suspend fun insertPond(pond: Pond) = dao.insertPond(pond)
    suspend fun deletePond(pond: Pond) = dao.deletePond(pond)


    // --- AP Market Rates ---
    val allApMarketRates: Flow<List<ApMarketRate>> = dao.getAllApMarketRates()
    suspend fun insertApMarketRate(rate: ApMarketRate) = dao.insertApMarketRate(rate)

    // --- Seeding Sample Data for Visual Analytics ---
    suspend fun seedSampleDataIfNeeded() {
        // Only seed if user profile is empty or no feed records are present
        val profile = dao.getCurrentUserProfile()
        if (profile == null) {
            // Seed initial ponds
            dao.insertPond(Pond(name = "Pond Delta-1", areaInAcres = 1.2, activeSeedStock = 180000, targetSurvivingPercentage = 88.0))
            dao.insertPond(Pond(name = "Pond Delta-2", areaInAcres = 1.5, activeSeedStock = 220000, targetSurvivingPercentage = 85.0))
            dao.insertPond(Pond(name = "Pond Delta-3", areaInAcres = 1.0, activeSeedStock = 150000, targetSurvivingPercentage = 90.0))

            // Seed initial AP Market Rates (Vannamei Price in INR per Kg)
            val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dao.insertApMarketRate(ApMarketRate(count = 100, ratePerKgInInr = 230.0, lastUpdatedDate = currentDateStr))
            dao.insertApMarketRate(ApMarketRate(count = 80, ratePerKgInInr = 270.0, lastUpdatedDate = currentDateStr))
            dao.insertApMarketRate(ApMarketRate(count = 70, ratePerKgInInr = 300.0, lastUpdatedDate = currentDateStr))
            dao.insertApMarketRate(ApMarketRate(count = 60, ratePerKgInInr = 340.0, lastUpdatedDate = currentDateStr))
            dao.insertApMarketRate(ApMarketRate(count = 50, ratePerKgInInr = 380.0, lastUpdatedDate = currentDateStr))
            dao.insertApMarketRate(ApMarketRate(count = 40, ratePerKgInInr = 450.0, lastUpdatedDate = currentDateStr))
            dao.insertApMarketRate(ApMarketRate(count = 30, ratePerKgInInr = 550.0, lastUpdatedDate = currentDateStr))

            // Seed default profile
            val defaultProfile = UserProfile(
                userId = "owner_google_shrimp99",
                userName = "Karthik Varma Pericharla",
                email = "kpericharla2005@gmail.com",
                phone = "+91 94405 12345",
                role = "Primary Owner",
                primaryPondName = "Pond Delta-1",
                loginMethod = "Google",
                isCloudSyncEnabled = true
            )
            dao.saveUserProfile(defaultProfile)

            // Seed feed logs (for feed tracking dashboards)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()

            // Generate past 7 days of feeds
            val feedTypes = listOf("Starter Pellet", "Grower Pellet", "Finisher Pellet")
            for (i in 6 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val dateStr = sdf.format(calendar.time)

                // 2 feeds per day
                dao.insertFeedLog(
                    FeedLog(
                        date = dateStr,
                        feedType = feedTypes[1], // Grower
                        quantityKg = 45.0 + (i * 2.5) + Random().nextInt(5),
                        costPerKg = 1.45,
                        pondName = "Pond Delta-1",
                        remarks = "Scheduled Feed Morning"
                    )
                )
                dao.insertFeedLog(
                    FeedLog(
                        date = dateStr,
                        feedType = feedTypes[1],
                        quantityKg = 50.0 + (i * 1.5) + Random().nextInt(8),
                        costPerKg = 1.45,
                        pondName = "Pond Delta-1",
                        remarks = "Scheduled Feed Evening"
                    )
                )
            }

            // Seed some past medicines
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -5)
            dao.insertMedicineLog(
                MedicineLog(
                    date = sdf.format(calendar.time),
                    medicineName = "Gut-Pro Probiotic",
                    dosage = "10g / kg of feed",
                    purpose = "Enhance digestion & gut health",
                    quantityUsed = 5.0,
                    unit = "Litre",
                    pondName = "Pond Delta-1",
                    remarks = "Applied in morning feeding session."
                )
            )

            calendar.add(Calendar.DAY_OF_YEAR, 3)
            dao.insertMedicineLog(
                MedicineLog(
                    date = sdf.format(calendar.time),
                    medicineName = "Min-Fortify Minerals",
                    dosage = "5kg / acre",
                    purpose = "Maintain salinity & shell moulting",
                    quantityUsed = 12.0,
                    unit = "kg",
                    pondName = "Pond Delta-1",
                    remarks = "Spread uniformly at sunset."
                )
            )

            // Seed count records for growth visualizer (DOC 10 to DOC 60)
            val seedCount = 180000 // Starting seed in pond
            val seedValuationRate = 18.0 // $18 per 1000 items

            val counts = listOf(
                CountRecord(
                    date = "2026-04-10",
                    pondName = "Pond Delta-1",
                    daysOfCulture = 10,
                    averageBodyWeightGrams = 2.1,
                    estimatedRemainingStock = 175000,
                    seedValuationRatePerThousand = 18.0,
                    nextCountDate = "2026-04-20"
                ),
                CountRecord(
                    date = "2026-04-20",
                    pondName = "Pond Delta-1",
                    daysOfCulture = 20,
                    averageBodyWeightGrams = 4.8,
                    estimatedRemainingStock = 170000,
                    seedValuationRatePerThousand = 20.0,
                    nextCountDate = "2026-04-30"
                ),
                CountRecord(
                    date = "2026-04-30",
                    pondName = "Pond Delta-1",
                    daysOfCulture = 30,
                    averageBodyWeightGrams = 8.5,
                    estimatedRemainingStock = 166000,
                    seedValuationRatePerThousand = 22.0,
                    nextCountDate = "2026-05-10"
                ),
                CountRecord(
                    date = "2026-05-10",
                    pondName = "Pond Delta-1",
                    daysOfCulture = 40,
                    averageBodyWeightGrams = 13.2,
                    estimatedRemainingStock = 162000,
                    seedValuationRatePerThousand = 25.0,
                    nextCountDate = "2026-05-20"
                ),
                CountRecord(
                    date = "2026-05-20",
                    pondName = "Pond Delta-1",
                    daysOfCulture = 50,
                    averageBodyWeightGrams = 18.4,
                    estimatedRemainingStock = 158000,
                    seedValuationRatePerThousand = 28.0,
                    nextCountDate = "2026-05-30"
                )
            )
            for (record in counts) {
                dao.insertCountRecord(record)
            }

            // Seed tray check alarms - one in future (reminder) and some past completed
            val currentTime = System.currentTimeMillis()
            dao.insertTrayCheckAlarm(
                TrayCheckAlarm(
                    pondName = "Pond Delta-1",
                    trayNumber = 1,
                    scheduledTime = "08:15",
                    epochScheduledTime = currentTime - 3600000L * 4, // 4 hours ago
                    isCompleted = true,
                    feedRemainingStatus = "Clean",
                    remarks = "Checked by supervisor, feed completely consumed."
                )
            )
            dao.insertTrayCheckAlarm(
                TrayCheckAlarm(
                    pondName = "Pond Delta-1",
                    trayNumber = 2,
                    scheduledTime = "12:30",
                    epochScheduledTime = currentTime + 3600000L * 2, // 2 hours in future
                    isCompleted = false,
                    feedRemainingStatus = "Pending",
                    remarks = "Requires feed consumption check 90 mins post-evening feeding."
                )
            )
            dao.insertTrayCheckAlarm(
                TrayCheckAlarm(
                    pondName = "Pond Delta-1",
                    trayNumber = 3,
                    scheduledTime = "18:00",
                    epochScheduledTime = currentTime + 3600000L * 4, // 4 hours in future
                    isCompleted = false,
                    feedRemainingStatus = "Pending",
                    remarks = "Check tray for night feeding wastage."
                )
            )
        }
    }
}
