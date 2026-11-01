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

    private val _gridState = MutableStateFlow(GridFeederState())
    val gridState: StateFlow<GridFeederState> = _gridState.asStateFlow()

    private val _espConfig = MutableStateFlow(Esp32Config())
    val espConfig: StateFlow<Esp32Config> = _espConfig.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.SIMULATING)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedFeederIndex = MutableStateFlow(0)
    val selectedFeederIndex: StateFlow<Int> = _selectedFeederIndex.asStateFlow()

    private val _isBengali = MutableStateFlow(true)
    val isBengali: StateFlow<Boolean> = _isBengali.asStateFlow()

    val faultHistory: StateFlow<List<FaultEventEntity>> = db.faultEventDao()
        .getAllFaults()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var pollingJob: Job? = null
    private var lastLoggedFaultTimestamp: Long = 0L

    init {
        startPolling()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                val config = _espConfig.value
                val result = espClient.fetchGridData(config, _selectedFeederIndex.value)

                result.onSuccess { state ->
                    _gridState.value = state
                    _connectionStatus.value = if (config.isSimulationMode) ConnectionStatus.SIMULATING else ConnectionStatus.CONNECTED
                    _errorMessage.value = null

                    // If a fault is detected, trigger haptic and save to Room DB
                    if (state.hasActiveFault && state.timestamp - lastLoggedFaultTimestamp > 3000L) {
                        lastLoggedFaultTimestamp = state.timestamp
                        triggerVibration()
                        logFaultToDb(state)
                    }
                }.onFailure { ex ->
                    if (!config.isSimulationMode) {
                        _connectionStatus.value = ConnectionStatus.ERROR
                        _errorMessage.value = ex.message ?: "ESP32 connection failed"
                    }
                }

                delay(1000L)
            }
        }
    }

    private fun logFaultToDb(state: GridFeederState) {
        viewModelScope.launch {
            val entity = FaultEventEntity(
                timestamp = state.timestamp,
                feederId = state.feederId,
                feederName = state.feederName,
                faultCode = state.activeFaultType.code,
                faultTitle = if (_isBengali.value) state.activeFaultType.titleBn else state.activeFaultType.titleEn,
                faultCurrentA = state.faultCurrentPeakA,
                phase = state.trippedPhase,
                systemVoltageKv = state.phaseR.voltageKv,
                activePowerAtTripMw = state.totalActivePowerMw,
                isCleared = false
            )
            db.faultEventDao().insertFault(entity)
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
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 300), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(400)
            }
        } catch (_: Exception) {}
    }

    fun selectFeeder(index: Int) {
        _selectedFeederIndex.value = index
    }

    fun toggleLanguage() {
        _isBengali.value = !_isBengali.value
    }

    fun updateConfig(newConfig: Esp32Config) {
        _espConfig.value = newConfig
        _connectionStatus.value = if (newConfig.isSimulationMode) ConnectionStatus.SIMULATING else ConnectionStatus.CONNECTING
        startPolling()
    }

    fun toggleBreaker() {
        viewModelScope.launch {
            val isClosed = _gridState.value.isBreakerClosed
            val action = if (isClosed) "trip" else "reset"
            espClient.sendBreakerCommand(_espConfig.value, action)
        }
    }

    fun injectFault(faultType: FaultType, phase: String, faultCurrentA: Double) {
        espClient.injectSimulatedFault(faultType, phase, faultCurrentA)
    }

    fun resetFault() {
        espClient.resetSimulatedFault()
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
