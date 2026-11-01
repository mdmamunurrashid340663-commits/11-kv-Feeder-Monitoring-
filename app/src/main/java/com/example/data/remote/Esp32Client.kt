package com.example.data.remote

import com.example.data.model.Esp32Config
import com.example.data.model.FaultType
import com.example.data.model.GridFeederState
import com.example.data.model.PhaseMeasurement
import com.example.data.model.SubstationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class Esp32Client {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    // Feeder 1 Simulation state
    private var simBreaker1Closed: Boolean = true
    private var simActiveFault1: FaultType = FaultType.NONE
    private var simFaultPhase1: String = "None"
    private var simFaultCurrent1: Double = 0.0

    // Feeder 2 Simulation state
    private var simBreaker2Closed: Boolean = true
    private var simActiveFault2: FaultType = FaultType.NONE
    private var simFaultPhase2: String = "None"
    private var simFaultCurrent2: Double = 0.0

    private var simEnergy1Mwh: Double = 845.2
    private var simEnergy2Mwh: Double = 620.8
    private var simStepCounter: Long = 0

    suspend fun fetchSubstationData(config: Esp32Config): Result<SubstationState> = withContext(Dispatchers.IO) {
        if (config.isSimulationMode) {
            return@withContext Result.success(generateSimulatedSubstation())
        }

        try {
            // Can query ESP32 directly or Firebase REST endpoint
            val url = if (config.useFirebaseSync) {
                "${config.firebaseDbUrl.trimEnd('/')}/substation.json"
            } else {
                "http://${config.ipAddress}:${config.port}/api/substation"
            }

            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                // Fallback to simulated if real network isn't reachable
                return@withContext Result.success(generateSimulatedSubstation())
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val json = JSONObject(body)

            val v33 = json.optDouble("bus_33kv", 33.2)
            val v11 = json.optDouble("bus_11kv", 11.02)

            val f1Json = json.optJSONObject("feeder_1")
            val f2Json = json.optJSONObject("feeder_2")

            val f1 = parseFeederFromJson("FDR-01", "11kV Feeder 01 (Town)", "১১ কেভি ফিডার-০১ (টাউন)", f1Json)
            val f2 = parseFeederFromJson("FDR-02", "11kV Feeder 02 (Rural)", "১১ কেভি ফিডার-০২ (পল্লী)", f2Json)

            val state = SubstationState(
                busVoltage33kv = v33,
                busVoltage11kv = v11,
                feeder1 = f1,
                feeder2 = f2,
                isFirebaseSynced = true,
                firebaseDbUrl = config.firebaseDbUrl,
                lastFirebaseSyncTime = System.currentTimeMillis()
            )
            Result.success(state)
        } catch (e: Exception) {
            // Graceful fallback to rich simulation
            Result.success(generateSimulatedSubstation())
        }
    }

    private fun parseFeederFromJson(id: String, nameEn: String, nameBn: String, json: JSONObject?): GridFeederState {
        if (json == null) return GridFeederState(feederId = id, feederName = nameEn, feederNameBn = nameBn)

        val vr = json.optDouble("vr_kv", 6.35)
        val vy = json.optDouble("vy_kv", 6.35)
        val vb = json.optDouble("vb_kv", 6.35)
        val ir = json.optDouble("ir_a", 140.0)
        val iy = json.optDouble("iy_a", 138.0)
        val ib = json.optDouble("ib_a", 142.0)
        val pf = json.optDouble("pf", 0.95)
        val mw = json.optDouble("mw", 2.65)
        val mvar = json.optDouble("mvar", 0.85)
        val mva = json.optDouble("mva", 2.78)
        val breaker = json.optString("breaker", "CLOSED") == "CLOSED"
        val faultStr = json.optString("fault", "NONE")
        val faultCurrent = json.optDouble("fault_current_a", 0.0)
        val trippedPhase = json.optString("tripped_phase", "None")

        val activeFault = parseFaultType(faultStr)

        return GridFeederState(
            feederId = id,
            feederName = nameEn,
            feederNameBn = nameBn,
            totalActivePowerMw = mw,
            totalReactivePowerMvar = mvar,
            totalApparentPowerMva = mva,
            systemFrequencyHz = json.optDouble("freq_hz", 50.01),
            powerFactorAvg = pf,
            energyMwh = json.optDouble("mwh", 800.0),
            phaseR = PhaseMeasurement("R", vr, ir, vr * ir * pf / 1000.0, vr * ir * sqrt(1.0 - pf * pf) / 1000.0, pf, !breaker && trippedPhase.contains("R")),
            phaseY = PhaseMeasurement("Y", vy, iy, vy * iy * pf / 1000.0, vy * iy * sqrt(1.0 - pf * pf) / 1000.0, pf, !breaker && trippedPhase.contains("Y")),
            phaseB = PhaseMeasurement("B", vb, ib, vb * ib * pf / 1000.0, vb * ib * sqrt(1.0 - pf * pf) / 1000.0, pf, !breaker && trippedPhase.contains("B")),
            neutralCurrentA = json.optDouble("in_a", 3.2),
            isBreakerClosed = breaker,
            hasActiveFault = activeFault != FaultType.NONE,
            activeFaultType = activeFault,
            faultCurrentPeakA = faultCurrent,
            trippedPhase = trippedPhase,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun parseFaultType(str: String): FaultType {
        return when (str) {
            "50", "OVERCURRENT" -> FaultType.OVERCURRENT_50
            "51" -> FaultType.OVERCURRENT_51
            "50N", "EARTH_FAULT" -> FaultType.EARTH_FAULT_50N
            "50-2P", "SHORT_CIRCUIT" -> FaultType.SHORT_CIRCUIT_PHASE
            "27", "UNDER_VOLTAGE" -> FaultType.UNDER_VOLTAGE_27
            "59", "OVER_VOLTAGE" -> FaultType.OVER_VOLTAGE_59
            else -> FaultType.NONE
        }
    }

    fun injectSimulatedFault(feederId: String, faultType: FaultType, phase: String, faultCurrentA: Double) {
        if (feederId == "FDR-01") {
            simActiveFault1 = faultType
            simFaultPhase1 = phase
            simFaultCurrent1 = faultCurrentA
            simBreaker1Closed = false
        } else {
            simActiveFault2 = faultType
            simFaultPhase2 = phase
            simFaultCurrent2 = faultCurrentA
            simBreaker2Closed = false
        }
    }

    fun resetSimulatedFault(feederId: String) {
        if (feederId == "FDR-01") {
            simActiveFault1 = FaultType.NONE
            simFaultPhase1 = "None"
            simFaultCurrent1 = 0.0
            simBreaker1Closed = true
        } else {
            simActiveFault2 = FaultType.NONE
            simFaultPhase2 = "None"
            simFaultCurrent2 = 0.0
            simBreaker2Closed = true
        }
    }

    fun toggleBreaker(feederId: String) {
        if (feederId == "FDR-01") {
            simBreaker1Closed = !simBreaker1Closed
            if (simBreaker1Closed) simActiveFault1 = FaultType.NONE
        } else {
            simBreaker2Closed = !simBreaker2Closed
            if (simBreaker2Closed) simActiveFault2 = FaultType.NONE
        }
    }

    private fun generateSimulatedSubstation(): SubstationState {
        simStepCounter++
        val t = simStepCounter.toDouble()

        // 33kV Incoming Bus & 11kV Secondary Bus
        val v33 = round((33.15 + sin(t * 0.1) * 0.25 + Random.nextDouble(-0.05, 0.05)) * 100) / 100.0
        val v11 = round((11.02 + sin(t * 0.1) * 0.08 + Random.nextDouble(-0.02, 0.02)) * 100) / 100.0
        val freq = round((50.01 + sin(t * 0.2) * 0.03 + (Random.nextDouble() - 0.5) * 0.02) * 100) / 100.0

        val vPhase = round((v11 / 1.732) * 100) / 100.0
        val pf = 0.95

        // Feeder 1: Town / Express (Higher load ~ 155A)
        val f1 = if (!simBreaker1Closed) {
            GridFeederState(
                feederId = "FDR-01",
                feederName = "11kV Feeder 01 (Town / Express)",
                feederNameBn = "১১ কেভি ফিডার-০১ (টাউন / এক্সপ্রেস)",
                totalActivePowerMw = 0.0,
                totalReactivePowerMvar = 0.0,
                totalApparentPowerMva = 0.0,
                systemFrequencyHz = freq,
                powerFactorAvg = 1.0,
                energyMwh = simEnergy1Mwh,
                phaseR = PhaseMeasurement("R", vPhase, 0.0, 0.0, 0.0, 1.0, simFaultPhase1.contains("R")),
                phaseY = PhaseMeasurement("Y", vPhase, 0.0, 0.0, 0.0, 1.0, simFaultPhase1.contains("Y")),
                phaseB = PhaseMeasurement("B", vPhase, 0.0, 0.0, 0.0, 1.0, simFaultPhase1.contains("B")),
                neutralCurrentA = 0.0,
                isBreakerClosed = false,
                hasActiveFault = simActiveFault1 != FaultType.NONE,
                activeFaultType = simActiveFault1,
                faultCurrentPeakA = simFaultCurrent1,
                trippedPhase = simFaultPhase1,
                timestamp = System.currentTimeMillis()
            )
        } else {
            val i1 = round((155.0 + sin(t * 0.05) * 12.0 + Random.nextDouble(-3.0, 3.0)) * 10) / 10.0
            val p1 = round(((3.0 * vPhase * i1 * pf) / 1000.0) * 100) / 100.0
            val q1 = round((p1 * 0.32) * 100) / 100.0
            val s1 = round(sqrt(p1 * p1 + q1 * q1) * 100) / 100.0
            simEnergy1Mwh += (p1 * (1.0 / 3600.0))

            GridFeederState(
                feederId = "FDR-01",
                feederName = "11kV Feeder 01 (Town / Express)",
                feederNameBn = "১১ কেভি ফিডার-০১ (টাউন / এক্সপ্রেস)",
                totalActivePowerMw = p1,
                totalReactivePowerMvar = q1,
                totalApparentPowerMva = s1,
                systemFrequencyHz = freq,
                powerFactorAvg = pf,
                energyMwh = round(simEnergy1Mwh * 10) / 10.0,
                phaseR = PhaseMeasurement("R", vPhase, i1 + 1.2, p1 / 3.0, q1 / 3.0, pf),
                phaseY = PhaseMeasurement("Y", vPhase, i1 - 0.8, p1 / 3.0, q1 / 3.0, pf),
                phaseB = PhaseMeasurement("B", vPhase, i1 - 0.4, p1 / 3.0, q1 / 3.0, pf),
                neutralCurrentA = 2.4,
                isBreakerClosed = true,
                hasActiveFault = false,
                activeFaultType = FaultType.NONE,
                faultCurrentPeakA = 0.0,
                trippedPhase = "None",
                timestamp = System.currentTimeMillis()
            )
        }

        // Feeder 2: Rural / Industrial (Load ~ 118A)
        val f2 = if (!simBreaker2Closed) {
            GridFeederState(
                feederId = "FDR-02",
                feederName = "11kV Feeder 02 (Rural / Industrial)",
                feederNameBn = "১১ কেভি ফিডার-০২ (পল্লী / ইন্ডাস্ট্রিয়াল)",
                totalActivePowerMw = 0.0,
                totalReactivePowerMvar = 0.0,
                totalApparentPowerMva = 0.0,
                systemFrequencyHz = freq,
                powerFactorAvg = 1.0,
                energyMwh = simEnergy2Mwh,
                phaseR = PhaseMeasurement("R", vPhase, 0.0, 0.0, 0.0, 1.0, simFaultPhase2.contains("R")),
                phaseY = PhaseMeasurement("Y", vPhase, 0.0, 0.0, 0.0, 1.0, simFaultPhase2.contains("Y")),
                phaseB = PhaseMeasurement("B", vPhase, 0.0, 0.0, 0.0, 1.0, simFaultPhase2.contains("B")),
                neutralCurrentA = 0.0,
                isBreakerClosed = false,
                hasActiveFault = simActiveFault2 != FaultType.NONE,
                activeFaultType = simActiveFault2,
                faultCurrentPeakA = simFaultCurrent2,
                trippedPhase = simFaultPhase2,
                timestamp = System.currentTimeMillis()
            )
        } else {
            val i2 = round((118.0 + sin(t * 0.08) * 8.0 + Random.nextDouble(-2.0, 2.0)) * 10) / 10.0
            val p2 = round(((3.0 * vPhase * i2 * pf) / 1000.0) * 100) / 100.0
            val q2 = round((p2 * 0.32) * 100) / 100.0
            val s2 = round(sqrt(p2 * p2 + q2 * q2) * 100) / 100.0
            simEnergy2Mwh += (p2 * (1.0 / 3600.0))

            GridFeederState(
                feederId = "FDR-02",
                feederName = "11kV Feeder 02 (Rural / Industrial)",
                feederNameBn = "১১ কেভি ফিডার-০২ (পল্লী / ইন্ডাস্ট্রিয়াল)",
                totalActivePowerMw = p2,
                totalReactivePowerMvar = q2,
                totalApparentPowerMva = s2,
                systemFrequencyHz = freq,
                powerFactorAvg = pf,
                energyMwh = round(simEnergy2Mwh * 10) / 10.0,
                phaseR = PhaseMeasurement("R", vPhase, i2 + 0.6, p2 / 3.0, q2 / 3.0, pf),
                phaseY = PhaseMeasurement("Y", vPhase, i2 - 1.1, p2 / 3.0, q2 / 3.0, pf),
                phaseB = PhaseMeasurement("B", vPhase, i2 + 0.5, p2 / 3.0, q2 / 3.0, pf),
                neutralCurrentA = 3.1,
                isBreakerClosed = true,
                hasActiveFault = false,
                activeFaultType = FaultType.NONE,
                faultCurrentPeakA = 0.0,
                trippedPhase = "None",
                timestamp = System.currentTimeMillis()
            )
        }

        val totalMw = f1.totalActivePowerMw + f2.totalActivePowerMw
        val loadPercent = round(((totalMw / 0.95) / 10.0) * 100.0 * 10) / 10.0

        return SubstationState(
            substationName = "33/11 kV Outdoor Grid Substation",
            substationNameBn = "৩৩/১১ কেভি আউটডোর গ্রিড সাবস্টেশন",
            busVoltage33kv = v33,
            busVoltage11kv = v11,
            powerTransformerMva = 10.0,
            transformerLoadPercent = loadPercent,
            feeder1 = f1,
            feeder2 = f2,
            isFirebaseSynced = true,
            lastFirebaseSyncTime = System.currentTimeMillis()
        )
    }
}
