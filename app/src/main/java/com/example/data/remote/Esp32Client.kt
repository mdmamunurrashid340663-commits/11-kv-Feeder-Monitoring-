package com.example.data.remote

import com.example.data.model.Esp32Config
import com.example.data.model.FaultType
import com.example.data.model.GridFeederState
import com.example.data.model.PhaseMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class Esp32Client {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    // Simulation state variables
    private var simBreakerClosed: Boolean = true
    private var simActiveFault: FaultType = FaultType.NONE
    private var simFaultPhase: String = "None"
    private var simFaultCurrent: Double = 0.0
    private var simEnergyMwh: Double = 1248.5
    private var simStepCounter: Long = 0

    suspend fun fetchGridData(config: Esp32Config, feederIndex: Int = 0): Result<GridFeederState> = withContext(Dispatchers.IO) {
        if (config.isSimulationMode) {
            return@withContext Result.success(generateSimulatedData(feederIndex, config))
        }

        try {
            val url = "http://${config.ipAddress}:${config.port}/api/grid"
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP Error: ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val json = JSONObject(body)

            val vr = json.optDouble("vr_kv", 6.35)
            val vy = json.optDouble("vy_kv", 6.35)
            val vb = json.optDouble("vb_kv", 6.35)
            val ir = json.optDouble("ir_a", 180.0)
            val iy = json.optDouble("iy_a", 180.0)
            val ib = json.optDouble("ib_a", 180.0)
            val pf = json.optDouble("pf", 0.95)
            val freq = json.optDouble("freq_hz", 50.00)
            val mw = json.optDouble("mw", 3.42)
            val mvar = json.optDouble("mvar", 1.12)
            val mva = json.optDouble("mva", 3.60)
            val breaker = json.optString("breaker", "CLOSED") == "CLOSED"
            val faultStr = json.optString("fault", "NONE")
            val faultCurrent = json.optDouble("fault_current_a", 0.0)
            val trippedPhase = json.optString("tripped_phase", "None")

            val activeFault = when (faultStr) {
                "50", "OVERCURRENT" -> FaultType.OVERCURRENT_50
                "51" -> FaultType.OVERCURRENT_51
                "50N", "EARTH_FAULT" -> FaultType.EARTH_FAULT_50N
                "50-2P", "SHORT_CIRCUIT" -> FaultType.SHORT_CIRCUIT_PHASE
                "27", "UNDER_VOLTAGE" -> FaultType.UNDER_VOLTAGE_27
                "59", "OVER_VOLTAGE" -> FaultType.OVER_VOLTAGE_59
                else -> FaultType.NONE
            }

            val state = GridFeederState(
                feederId = json.optString("feeder_id", "FDR-11KV-01"),
                feederName = "11kV Express Feeder",
                feederNameBn = "১১ কেভি এক্সপ্রেস ফিডার",
                totalActivePowerMw = mw,
                totalReactivePowerMvar = mvar,
                totalApparentPowerMva = mva,
                systemFrequencyHz = freq,
                powerFactorAvg = pf,
                energyMwh = json.optDouble("mwh", 1248.5),
                phaseR = PhaseMeasurement("R", vr, ir, vr * ir * pf / 1000.0, vr * ir * sqrt(1.0 - pf * pf) / 1000.0, pf, !breaker && trippedPhase.contains("R")),
                phaseY = PhaseMeasurement("Y", vy, iy, vy * iy * pf / 1000.0, vy * iy * sqrt(1.0 - pf * pf) / 1000.0, pf, !breaker && trippedPhase.contains("Y")),
                phaseB = PhaseMeasurement("B", vb, ib, vb * ib * pf / 1000.0, vb * ib * sqrt(1.0 - pf * pf) / 1000.0, pf, !breaker && trippedPhase.contains("B")),
                neutralCurrentA = json.optDouble("in_a", 3.8),
                isBreakerClosed = breaker,
                hasActiveFault = activeFault != FaultType.NONE,
                activeFaultType = activeFault,
                faultCurrentPeakA = faultCurrent,
                trippedPhase = trippedPhase,
                timestamp = System.currentTimeMillis()
            )
            Result.success(state)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendBreakerCommand(config: Esp32Config, action: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (config.isSimulationMode) {
            if (action == "trip") {
                simBreakerClosed = false
            } else if (action == "reset") {
                simBreakerClosed = true
                simActiveFault = FaultType.NONE
                simFaultCurrent = 0.0
                simFaultPhase = "None"
            }
            return@withContext Result.success(true)
        }

        try {
            val url = "http://${config.ipAddress}:${config.port}/api/breaker?action=$action"
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun injectSimulatedFault(faultType: FaultType, phase: String, faultCurrentA: Double) {
        simActiveFault = faultType
        simFaultPhase = phase
        simFaultCurrent = faultCurrentA
        simBreakerClosed = false // Trip the breaker
    }

    fun resetSimulatedFault() {
        simActiveFault = FaultType.NONE
        simFaultPhase = "None"
        simFaultCurrent = 0.0
        simBreakerClosed = true
    }

    private fun generateSimulatedData(feederIndex: Int, config: Esp32Config): GridFeederState {
        simStepCounter++

        val feeders = listOf(
            Pair("FDR-11KV-01", "11kV Industrial Express Feeder" to "১১ কেভি শিল্প এলাকা এক্সপ্রেস ফিডার"),
            Pair("FDR-11KV-02", "11kV Town Central Feeder" to "১১ কেভি টাউন সেন্ট্রাল ফিডার"),
            Pair("FDR-11KV-03", "11kV Rural Commercial Feeder" to "১১ কেভি পল্লী বাণিজ্যিক ফিডার")
        )
        val selected = feeders.getOrElse(feederIndex) { feeders[0] }
        val feederId = selected.first
        val nameEn = selected.second.first
        val nameBn = selected.second.second

        // If circuit breaker is open / tripped
        if (!simBreakerClosed) {
            return GridFeederState(
                feederId = feederId,
                feederName = nameEn,
                feederNameBn = nameBn,
                totalActivePowerMw = 0.0,
                totalReactivePowerMvar = 0.0,
                totalApparentPowerMva = 0.0,
                systemFrequencyHz = 50.00,
                powerFactorAvg = 1.0,
                energyMwh = simEnergyMwh,
                phaseR = PhaseMeasurement("R", 6.35, 0.0, 0.0, 0.0, 1.0, simFaultPhase.contains("R")),
                phaseY = PhaseMeasurement("Y", 6.35, 0.0, 0.0, 0.0, 1.0, simFaultPhase.contains("Y")),
                phaseB = PhaseMeasurement("B", 6.35, 0.0, 0.0, 0.0, 1.0, simFaultPhase.contains("B")),
                neutralCurrentA = 0.0,
                isBreakerClosed = false,
                hasActiveFault = simActiveFault != FaultType.NONE,
                activeFaultType = simActiveFault,
                faultCurrentPeakA = simFaultCurrent,
                trippedPhase = simFaultPhase,
                timestamp = System.currentTimeMillis()
            )
        }

        // Base 11kV Phase-to-Neutral is ~6351 Volts (6.35 kV)
        val timeSec = simStepCounter.toDouble()
        val freqJitter = (sin(timeSec * 0.2) * 0.03 + (Random.nextDouble() - 0.5) * 0.02)
        val freq = round((50.00 + freqJitter) * 100) / 100.0

        val vFluct = sin(timeSec * 0.1) * 0.08
        val vr = round((6.35 + vFluct + (Random.nextDouble() - 0.5) * 0.04) * 100) / 100.0
        val vy = round((6.34 + vFluct + (Random.nextDouble() - 0.5) * 0.04) * 100) / 100.0
        val vb = round((6.36 + vFluct + (Random.nextDouble() - 0.5) * 0.04) * 100) / 100.0

        // Line currents with natural fluctuation based on industrial load
        val baseLoadA = when (feederIndex) {
            0 -> 185.0
            1 -> 135.0
            else -> 95.0
        }
        val loadWave = sin(timeSec * 0.05) * 15.0
        val ir = round((baseLoadA + loadWave + Random.nextDouble(-3.0, 3.0)) * 10) / 10.0
        val iy = round((baseLoadA + loadWave + Random.nextDouble(-4.0, 2.0)) * 10) / 10.0
        val ib = round((baseLoadA + loadWave + Random.nextDouble(-2.0, 4.0)) * 10) / 10.0

        val pf = round((0.94 + sin(timeSec * 0.03) * 0.03) * 100) / 100.0

        // Three Phase Active Power in MW:
        // P = (Vr*Ir + Vy*Iy + Vb*Ib) * pf / 1000.0
        val pR = (vr * ir * pf) / 1000.0
        val pY = (vy * iy * pf) / 1000.0
        val pB = (vb * ib * pf) / 1000.0
        val totalMw = round((pR + pY + pB) * 100) / 100.0

        val qR = (vr * ir * sqrt(1.0 - pf * pf)) / 1000.0
        val qY = (vy * iy * sqrt(1.0 - pf * pf)) / 1000.0
        val qB = (vb * ib * sqrt(1.0 - pf * pf)) / 1000.0
        val totalMvar = round((qR + qY + qB) * 100) / 100.0

        val totalMva = round(sqrt(totalMw * totalMw + totalMvar * totalMvar) * 100) / 100.0

        // Neutral current (sum of unbalanced phase currents)
        val inA = round((kotlin.math.abs(ir - iy) * 0.3 + kotlin.math.abs(iy - ib) * 0.3 + 1.5) * 10) / 10.0

        simEnergyMwh += (totalMw * (1.0 / 3600.0))

        return GridFeederState(
            feederId = feederId,
            feederName = nameEn,
            feederNameBn = nameBn,
            totalActivePowerMw = totalMw,
            totalReactivePowerMvar = totalMvar,
            totalApparentPowerMva = totalMva,
            systemFrequencyHz = freq,
            powerFactorAvg = pf,
            energyMwh = round(simEnergyMwh * 100) / 100.0,
            phaseR = PhaseMeasurement("R", vr, ir, round(pR * 100) / 100.0, round(qR * 100) / 100.0, pf),
            phaseY = PhaseMeasurement("Y", vy, iy, round(pY * 100) / 100.0, round(qY * 100) / 100.0, pf),
            phaseB = PhaseMeasurement("B", vb, ib, round(pB * 100) / 100.0, round(qB * 100) / 100.0, pf),
            neutralCurrentA = inA,
            isBreakerClosed = true,
            hasActiveFault = false,
            activeFaultType = FaultType.NONE,
            faultCurrentPeakA = 0.0,
            trippedPhase = "None",
            timestamp = System.currentTimeMillis()
        )
    }
}
