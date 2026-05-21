package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AquacultureViewModel(private val repository: AquacultureRepository) : ViewModel() {

    init {
        // Automatically seed sample data on first start to provide immediate visual analytics
        viewModelScope.launch {
            repository.seedSampleDataIfNeeded()
        }
    }

    // --- Navigation & UI Filter States ---
    private val _currentTab = MutableStateFlow("dashboard") // dashboard, feeds, meds, counts, alarms, profile
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _selectedPond = MutableStateFlow("Pond Delta-1")
    val selectedPond: StateFlow<String> = _selectedPond.asStateFlow()

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun selectPond(pond: String) {
        _selectedPond.value = pond
    }


    // --- User Profile Profile (Simulated Cloud-Sync) ---
    val currentUserProfile: StateFlow<UserProfile?> = repository.currentUserProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun loginWithGoogle(email: String, name: String) {
        viewModelScope.launch {
            val profile = UserProfile(
                userId = "google_user_${UUID.randomUUID().toString().take(6)}",
                userName = name.ifEmpty { "Owner Representative" },
                email = email.ifEmpty { "kpericharla2005@gmail.com" },
                phone = "+91 94401 12345",
                role = "Primary Owner",
                loginMethod = "Google Sign-In",
                isCloudSyncEnabled = true
            )
            repository.saveUserProfile(profile)
            _currentTab.value = "dashboard"
        }
    }

    fun loginWithMobile(phone: String, name: String, role: String) {
        viewModelScope.launch {
            val profile = UserProfile(
                userId = "mobile_user_${UUID.randomUUID().toString().take(6)}",
                userName = name.ifEmpty { "Pond Manager" },
                email = "shrimp_owner@example.com",
                phone = phone.ifEmpty { "+91 94405 98765" },
                role = role,
                loginMethod = "Mobile SMS Login",
                isCloudSyncEnabled = true
            )
            repository.saveUserProfile(profile)
            _currentTab.value = "dashboard"
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearUserProfile()
            _currentTab.value = "profile"
        }
    }

    fun toggleCloudSync() {
        viewModelScope.launch {
            val profile = repository.getCurrentUser()
            if (profile != null) {
                val updated = profile.copy(
                    isCloudSyncEnabled = !profile.isCloudSyncEnabled,
                    lastSyncTime = System.currentTimeMillis()
                )
                repository.saveUserProfile(updated)
            }
        }
    }


    // --- Pond State Flows & Operations ---
    val allPonds: StateFlow<List<Pond>> = repository.allPonds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addPond(name: String, areaInAcres: Double, seedStock: Int, survivalPct: Double) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.insertPond(
                Pond(
                    name = name,
                    dateCreated = dateStr,
                    areaInAcres = areaInAcres,
                    activeSeedStock = seedStock,
                    targetSurvivingPercentage = survivalPct
                )
            )
            _selectedPond.value = name
        }
    }

    fun deletePond(pond: Pond) {
        viewModelScope.launch {
            repository.deletePond(pond)
        }
    }


    // --- AP Market Rates State Flows & Operations ---
    val allApMarketRates: StateFlow<List<ApMarketRate>> = repository.allApMarketRates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateApMarketRate(count: Int, ratePerKg: Double) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.insertApMarketRate(
                ApMarketRate(
                    count = count,
                    ratePerKgInInr = ratePerKg,
                    lastUpdatedDate = dateStr
                )
            )
        }
    }


    // --- Feed Operations & Summaries ---
    val allFeedLogs: StateFlow<List<FeedLog>> = repository.allFeedLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val feedSummaryByPondAndType: StateFlow<Map<String, Double>> = repository.allFeedLogs
        .map { logs ->
            logs.groupBy { it.feedType }
                .mapValues { entry -> entry.value.sumOf { it.quantityKg } }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val feedSummaryByDate: StateFlow<Map<String, Double>> = repository.allFeedLogs
        .map { logs ->
            logs.groupBy { it.date }
                .mapValues { entry -> entry.value.sumOf { it.quantityKg } }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun addFeedLog(feedType: String, quantity: Double, cost: Double, pondName: String, remarks: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val newLog = FeedLog(
                date = dateStr,
                feedType = feedType,
                quantityKg = quantity,
                costPerKg = cost,
                pondName = pondName,
                remarks = remarks
            )
            repository.insertFeedLog(newLog)
        }
    }

    fun deleteFeedLog(log: FeedLog) {
        viewModelScope.launch {
            repository.deleteFeedLog(log)
        }
    }


    // --- Medicine Operations ---
    val allMedicineLogs: StateFlow<List<MedicineLog>> = repository.allMedicineLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addMedicineLog(medName: String, dosage: String, purpose: String, quantity: Double, unit: String, pondName: String, remarks: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val log = MedicineLog(
                date = dateStr,
                medicineName = medName,
                dosage = dosage,
                purpose = purpose,
                quantityUsed = quantity,
                unit = unit,
                pondName = pondName,
                remarks = remarks
            )
            repository.insertMedicineLog(log)
        }
    }

    fun deleteMedicineLog(log: MedicineLog) {
        viewModelScope.launch {
            repository.deleteMedicineLog(log)
        }
    }


    // --- Count, Seed and Valuation Operations ---
    val countRecordsChronological: StateFlow<List<CountRecord>> = repository.chronologicalCountRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val countRecordsLatest: StateFlow<List<CountRecord>> = repository.allCountRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val latestCountRecord: StateFlow<CountRecord?> = combine(
        repository.allCountRecords,
        _selectedPond
    ) { records, pond ->
        records.firstOrNull { it.pondName == pond }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun addCountRecord(pondName: String, doc: Int, abw: Double, remainingStock: Int, ratePerThousand: Double, nextCountDate: String, remarks: String) {
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val record = CountRecord(
                date = dateStr,
                pondName = pondName,
                daysOfCulture = doc,
                averageBodyWeightGrams = abw,
                estimatedRemainingStock = remainingStock,
                seedValuationRatePerThousand = ratePerThousand,
                nextCountDate = nextCountDate,
                remarks = remarks
            )
            repository.insertCountRecord(record)
        }
    }

    // --- AP Market Rate Valuation Support ---
    data class ApValuationResult(
        val isSeedStage: Boolean = true,
        val computedCount: Int = 0,
        val closestRateCount: Int = 0,
        val pricePerUnit: Double = 0.0,
        val biomassKg: Double = 0.0,
        val totalValueInr: Double = 0.0,
        val calculationRemark: String = "No data available"
    )

    fun calculateApValuation(record: CountRecord?, apRates: List<ApMarketRate>): ApValuationResult {
        if (record == null) return ApValuationResult()

        val abw = record.averageBodyWeightGrams
        val stockCount = record.estimatedRemainingStock

        // If ABW is super small (< 2.0g), we value by seed PL rates per thousand seeds
        if (abw < 2.0) {
            val totalInr = (stockCount * record.seedValuationRatePerThousand) / 1000.0
            return ApValuationResult(
                isSeedStage = true,
                computedCount = 0,
                closestRateCount = 0,
                pricePerUnit = record.seedValuationRatePerThousand,
                biomassKg = 0.0,
                totalValueInr = totalInr,
                calculationRemark = "PL Seed State: Total stock valued at ₹${record.seedValuationRatePerThousand} per 1,000"
            )
        }

        // Commercial crop size: computed commercial count per Kg = 1000 / abw
        val computedCount = (1000.0 / abw + 0.5).toInt()

        // Find closest matching Count rate from the AP market rates
        val matchingRate = apRates.minByOrNull { rate ->
            val diff = rate.count - computedCount
            if (diff >= 0) diff else -diff
        } ?: ApMarketRate(count = 100, ratePerKgInInr = 230.0, lastUpdatedDate = "")

        val biomassKg = (stockCount * abw) / 1000.0
        val totalInr = biomassKg * matchingRate.ratePerKgInInr

        return ApValuationResult(
            isSeedStage = false,
            computedCount = computedCount,
            closestRateCount = matchingRate.count,
            pricePerUnit = matchingRate.ratePerKgInInr,
            biomassKg = biomassKg,
            totalValueInr = totalInr,
            calculationRemark = "Shrimp Biomass: ${String.format(Locale.getDefault(), "%,.1f", biomassKg)} Kg x AP Market Price: ₹${matchingRate.ratePerKgInInr}/Kg (approx. ${matchingRate.count} count grade)"
        )
    }

    fun deleteCountRecord(record: CountRecord) {
        viewModelScope.launch {
            repository.deleteCountRecord(record)
        }
    }


    // --- Tray Alarm Operations & Alerts ---
    val allTrayAlarms: StateFlow<List<TrayCheckAlarm>> = repository.allTrayCheckAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeTrayAlarms: StateFlow<List<TrayCheckAlarm>> = repository.activeTrayCheckAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTrayCheckAlarm(pondName: String, trayNum: Int, scheduledTime: String, offsetMins: Int, remarks: String) {
        viewModelScope.launch {
            // Parse HH:mm to check epoch scheduled time
            val cal = Calendar.getInstance()
            val timeParts = scheduledTime.split(":")
            if (timeParts.size == 2) {
                cal.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                cal.set(Calendar.MINUTE, timeParts[1].toInt())
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                // If scheduled time is in the past, schedule for tomorrow
                if (cal.timeInMillis < System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val alarm = TrayCheckAlarm(
                pondName = pondName,
                trayNumber = trayNum,
                scheduledTime = scheduledTime,
                epochScheduledTime = cal.timeInMillis,
                minutesOffset = offsetMins,
                isCompleted = false,
                feedRemainingStatus = "Pending",
                remarks = remarks
            )
            repository.insertTrayCheckAlarm(alarm)
        }
    }

    fun completeTrayAlarm(alarm: TrayCheckAlarm, status: String, remarks: String) {
        viewModelScope.launch {
            val updated = alarm.copy(
                isCompleted = true,
                feedRemainingStatus = status,
                remarks = remarks
            )
            repository.updateTrayCheckAlarm(updated)
        }
    }

    fun deleteTrayAlarm(id: Int) {
        viewModelScope.launch {
            repository.deleteTrayCheckAlarmById(id)
        }
    }
}

// --- Factory for Creating ViewModels with Repository injection ---
class AquacultureViewModelFactory(private val repository: AquacultureRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AquacultureViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AquacultureViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
