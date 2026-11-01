package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.FaultEventEntity
import com.example.data.model.Esp32Config
import com.example.data.model.FaultType
import com.example.data.model.GridFeederState
import com.example.data.model.SubstationState
import com.example.data.remote.Esp32Client
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    ERROR,
    SIMULATING
}

class GridViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val espClient = Esp32Client()

    private val _substationState = MutableStateFlow(SubstationState())
    val substationState: StateFlow<SubstationState> = _substationState.asStateFlow()

    private val _espConfig = MutableStateFlow(Esp32Config())
    val espConfig: StateFlow<Esp32Config> = _espConfig.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.SIMULATING)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedFeederTab = MutableStateFlow(0) // 0: Feeder 1, 1: Feeder 2, 2: 33kV Substation Overview
    val selectedFeederTab: StateFlow<Int> = _selectedFeederTab.asStateFlow()

    private val _isBengali = MutableStateFlow(true)
    val isBengali: StateFlow<Boolean> = _isBengali.asStateFlow()

    private val _activeDialogFault = MutableStateFlow<FaultEventEntity?>(null)
    val activeDialogFault: StateFlow<FaultEventEntity?> = _activeDialogFault.asStateFlow()

    val faultHistory: StateFlow<List<FaultEventEntity>> = db.faultEventDao()
        .getAllFaults()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var pollingJob: Job? = null
    private var lastLoggedFaultTime1: Long = 0L
    private var lastLoggedFaultTime2: Long = 0L

    init {
        startPolling()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                val config = _espConfig.value
                val result = espClient.fetchSubstationData(config)

                result.onSuccess { state ->
                    _substationState.value = state
                    _connectionStatus.value = if (config.isSimulationMode) ConnectionStatus.SIMULATING else ConnectionStatus.CONNECTED
                    _errorMessage.value = null

                    // Monitor Feeder 1 Faults
                    if (state.feeder1.hasActiveFault && state.feeder1.timestamp - lastLoggedFaultTime1 > 4000L) {
                        lastLoggedFaultTime1 = state.feeder1.timestamp
                        triggerVibration()
                        logFault(state.feeder1)
                    }

                    // Monitor Feeder 2 Faults
                    if (state.feeder2.hasActiveFault && state.feeder2.timestamp - lastLoggedFaultTime2 > 4000L) {
                        lastLoggedFaultTime2 = state.feeder2.timestamp
                        triggerVibration()
                        logFault(state.feeder2)
                    }
                }.onFailure { ex ->
                    if (!config.isSimulationMode) {
                        _connectionStatus.value = ConnectionStatus.ERROR
                        _errorMessage.value = ex.message ?: "Connection error"
                    }
                }

                delay(1000L)
            }
        }
    }

    private fun logFault(feeder: GridFeederState) {
        viewModelScope.launch {
            val entity = FaultEventEntity(
                timestamp = feeder.timestamp,
                feederId = feeder.feederId,
                feederName = feeder.feederName,
                faultCode = feeder.activeFaultType.code,
                faultTitle = if (_isBengali.value) feeder.activeFaultType.titleBn else feeder.activeFaultType.titleEn,
                faultCurrentA = feeder.faultCurrentPeakA,
                phase = feeder.trippedPhase,
                systemVoltageKv = feeder.phaseR.voltageKv,
                activePowerAtTripMw = feeder.totalActivePowerMw,
                isCleared = false,
                faultCause = "",
                rootCauseCategory = "গাছের ডাল স্পর্শ (Tree Branch)",
                actionTaken = "লাইন পেট্রোলিং প্রক্রিয়াধীন",
                engineerName = "Duty Engineer"
            )
            val newId = db.faultEventDao().insertFault(entity)
            // Automatically prompt the duty engineer to record the fault cause!
            _activeDialogFault.value = entity.copy(id = newId)
        }
    }

    private fun triggerVibration() {
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 150, 350), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(500)
            }
        } catch (_: Exception) {}
    }

    fun openFaultCauseDialog(fault: FaultEventEntity) {
        _activeDialogFault.value = fault
    }

    fun dismissFaultCauseDialog() {
        _activeDialogFault.value = null
    }

    fun saveFaultCause(faultId: Long, cause: String, category: String, action: String, engineer: String) {
        viewModelScope.launch {
            db.faultEventDao().updateFaultCause(faultId, cause, category, action, engineer)
            _activeDialogFault.value = null
        }
    }

    fun selectFeederTab(index: Int) {
        _selectedFeederTab.value = index
    }

    fun toggleLanguage() {
        _isBengali.value = !_isBengali.value
    }

    fun updateConfig(newConfig: Esp32Config) {
        _espConfig.value = newConfig
        _connectionStatus.value = if (newConfig.isSimulationMode) ConnectionStatus.SIMULATING else ConnectionStatus.CONNECTING
        startPolling()
    }

    fun toggleBreaker(feederId: String) {
        espClient.toggleBreaker(feederId)
    }

    fun injectFault(feederId: String, faultType: FaultType, phase: String, faultCurrentA: Double) {
        espClient.injectSimulatedFault(feederId, faultType, phase, faultCurrentA)
    }

    fun resetFault(feederId: String) {
        espClient.resetSimulatedFault(feederId)
    }

    fun clearFaultHistory() {
        viewModelScope.launch {
            db.faultEventDao().clearAll()
        }
    }

    fun markFaultCleared(id: Long) {
        viewModelScope.launch {
            db.faultEventDao().markFaultCleared(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
