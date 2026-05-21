package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AquacultureDao {

    // --- Feed Logs ---
    @Query("SELECT * FROM feed_logs ORDER BY date DESC, id DESC")
    fun getAllFeedLogs(): Flow<List<FeedLog>>

    @Query("SELECT * FROM feed_logs WHERE date = :date ORDER BY id DESC")
    fun getFeedLogsByDate(date: String): Flow<List<FeedLog>>

    @Query("SELECT SUM(quantityKg) FROM feed_logs")
    fun getTotalFeedUsage(): Flow<Double?>

    @Query("SELECT * FROM feed_logs WHERE feedType = :feedType ORDER BY date DESC")
    fun getFeedLogsByType(feedType: String): Flow<List<FeedLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedLog(feedLog: FeedLog)

    @Delete
    suspend fun deleteFeedLog(feedLog: FeedLog)


    // --- Medicine Logs ---
    @Query("SELECT * FROM medicine_logs ORDER BY date DESC, id DESC")
    fun getAllMedicineLogs(): Flow<List<MedicineLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicineLog(medicineLog: MedicineLog)

    @Delete
    suspend fun deleteMedicineLog(medicineLog: MedicineLog)


    // --- Count Records ---
    @Query("SELECT * FROM count_records ORDER BY date DESC, daysOfCulture DESC")
    fun getAllCountRecords(): Flow<List<CountRecord>>

    @Query("SELECT * FROM count_records ORDER BY daysOfCulture ASC")
    fun getCountRecordsChronological(): Flow<List<CountRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountRecord(countRecord: CountRecord)

    @Delete
    suspend fun deleteCountRecord(countRecord: CountRecord)


    // --- Tray Check Alarms ---
    @Query("SELECT * FROM tray_check_alarms ORDER BY epochScheduledTime ASC")
    fun getAllTrayCheckAlarms(): Flow<List<TrayCheckAlarm>>

    @Query("SELECT * FROM tray_check_alarms WHERE isCompleted = 0 ORDER BY epochScheduledTime ASC")
    fun getActiveTrayCheckAlarms(): Flow<List<TrayCheckAlarm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrayCheckAlarm(trayCheckAlarm: TrayCheckAlarm)

    @Update
    suspend fun updateTrayCheckAlarm(trayCheckAlarm: TrayCheckAlarm)

    @Query("DELETE FROM tray_check_alarms WHERE id = :id")
    suspend fun deleteTrayCheckAlarmById(id: Int)


    // --- User Session Profile ---
    @Query("SELECT * FROM user_profiles LIMIT 1")
    fun getCurrentUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles LIMIT 1")
    suspend fun getCurrentUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(userProfile: UserProfile)

    @Query("DELETE FROM user_profiles")
    suspend fun clearUserProfiles()


    // --- Ponds ---
    @Query("SELECT * FROM ponds ORDER BY name ASC")
    fun getAllPonds(): Flow<List<Pond>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPond(pond: Pond)

    @Delete
    suspend fun deletePond(pond: Pond)


    // --- AP Market Rates ---
    @Query("SELECT * FROM ap_market_rates ORDER BY count ASC")
    fun getAllApMarketRates(): Flow<List<ApMarketRate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApMarketRate(rate: ApMarketRate)
}
