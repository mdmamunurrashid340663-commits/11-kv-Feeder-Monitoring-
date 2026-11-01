package com.example.data.model

import com.squareup.moshi.JsonClass

enum class FaultType(
    val code: String,
    val titleBn: String,
    val titleEn: String,
    val descriptionBn: String,
    val isSevere: Boolean
) {
    NONE(
        code = "NORMAL",
        titleBn = "স্বাভাবিক অবস্থা",
        titleEn = "Normal Health",
        descriptionBn = "লাইনের সমস্ত ফেজ ভোল্টেজ এবং লোড কারেন্ট নিরাপদ সীমার মধ্যে আছে।",
        isSevere = false
    ),
    OVERCURRENT_50(
        code = "50",
        titleBn = "ইনস্ট্যান্টেনিয়াস ওভারকারেন্ট (ANSI 50)",
        titleEn = "Instantaneous Overcurrent",
        descriptionBn = "লাইনের লোড কারেন্ট নির্ধারিত সীমা অতিক্রম করেছে। ভ্যাকুয়াম ব্রেকার ট্রিপ করেছে।",
        isSevere = true
    ),
    OVERCURRENT_51(
        code = "51",
        titleBn = "আইডিএমটি ওভারকারেন্ট (ANSI 51)",
        titleEn = "IDMT Timed Overcurrent",
        descriptionBn = "অতিরিক্ত লোড দীর্ঘক্ষণ চলায় ইনভার্স টাইম কার্ভ অনুযায়ী লাইন ট্রিপ হয়েছে।",
        isSevere = true
    ),
    EARTH_FAULT_50N(
        code = "50N",
        titleBn = "আর্থ ফল্ট কারেন্ট (ANSI 50N/51N)",
        titleEn = "Earth Fault Detection",
        descriptionBn = "লাইনের তার মাটিতে স্পর্শ বা ইনসুলেটর লিকেজে আর্থ ফল্ট কারেন্ট ধরা পড়েছে।",
        isSevere = true
    ),
    SHORT_CIRCUIT_PHASE(
        code = "50-2P",
        titleBn = "ফেজ-টু-ফেজ শর্ট সার্কিট",
        titleEn = "Phase-to-Phase Short Circuit",
        descriptionBn = "দুটি ফেজের মধ্যে সরাসরি শর্ট সার্কিটে ক্ষতিকর উচ্চমাত্রার ফল্ট কারেন্ট তৈরি হয়েছে।",
        isSevere = true
    ),
    UNDER_VOLTAGE_27(
        code = "27",
        titleBn = "আন্ডার-ভোল্টেজ ফল্ট (ANSI 27)",
        titleEn = "Under-Voltage Warning",
        descriptionBn = "বাস ভোল্টেজ ৯.৫ কেভির নিচে নেমে গেছে।",
        isSevere = false
    ),
    OVER_VOLTAGE_59(
        code = "59",
        titleBn = "ওভার-ভোল্টেজ ফল্ট (ANSI 59)",
        titleEn = "Over-Voltage Warning",
        descriptionBn = "বাস ভোল্টেজ ১২.২ কেভির উপরে উঠে গেছে।",
        isSevere = false
    )
}

@JsonClass(generateAdapter = true)
data class PhaseMeasurement(
    val phase: String, // "R", "Y", "B"
    val voltageKv: Double,
    val currentA: Double,
    val activePowerMw: Double,
    val reactivePowerMvar: Double,
    val powerFactor: Double,
    val isTripped: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GridFeederState(
    val feederId: String = "FDR-01",
    val feederName: String = "11kV Feeder 01 (Town)",
    val feederNameBn: String = "১১ কেভি ফিডার-০১ (টাউন)",
    val totalActivePowerMw: Double = 2.45,
    val totalReactivePowerMvar: Double = 0.82,
    val totalApparentPowerMva: Double = 2.58,
    val systemFrequencyHz: Double = 50.02,
    val powerFactorAvg: Double = 0.95,
    val energyMwh: Double = 845.2,
    val phaseR: PhaseMeasurement = PhaseMeasurement("R", 6.35, 132.0, 0.82, 0.27, 0.95),
    val phaseY: PhaseMeasurement = PhaseMeasurement("Y", 6.34, 130.5, 0.81, 0.27, 0.95),
    val phaseB: PhaseMeasurement = PhaseMeasurement("B", 6.36, 134.2, 0.82, 0.28, 0.95),
    val neutralCurrentA: Double = 3.2,
    val isBreakerClosed: Boolean = true,
    val hasActiveFault: Boolean = false,
    val activeFaultType: FaultType = FaultType.NONE,
    val faultCurrentPeakA: Double = 0.0,
    val trippedPhase: String = "None",
    val faultRecordedCause: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SubstationState(
    val substationName: String = "33/11 kV Grid Substation",
    val substationNameBn: String = "৩৩/১১ কেভি আউটডোর গ্রিড সাবস্টেশন",
    val busVoltage33kv: Double = 33.2,
    val busVoltage11kv: Double = 11.0,
    val powerTransformerMva: Double = 10.0, // 10/14 MVA Power Transformer
    val transformerLoadPercent: Double = 58.0,
    val feeder1: GridFeederState = GridFeederState(
        feederId = "FDR-01",
        feederName = "11kV Feeder 01 (Town / Express)",
        feederNameBn = "১১ কেভি ফিডার-০১ (টাউন / এক্সপ্রেস)",
        totalActivePowerMw = 2.85,
        phaseR = PhaseMeasurement("R", 6.35, 155.0, 0.95, 0.31, 0.95),
        phaseY = PhaseMeasurement("Y", 6.35, 152.0, 0.94, 0.31, 0.95),
        phaseB = PhaseMeasurement("B", 6.35, 156.0, 0.96, 0.32, 0.95)
    ),
    val feeder2: GridFeederState = GridFeederState(
        feederId = "FDR-02",
        feederName = "11kV Feeder 02 (Rural / Industrial)",
        feederNameBn = "১১ কেভি ফিডার-০২ (পল্লী / ইন্ডাস্ট্রিয়াল)",
        totalActivePowerMw = 2.15,
        phaseR = PhaseMeasurement("R", 6.35, 118.0, 0.72, 0.24, 0.95),
        phaseY = PhaseMeasurement("Y", 6.34, 115.0, 0.71, 0.23, 0.95),
        phaseB = PhaseMeasurement("B", 6.36, 119.0, 0.72, 0.24, 0.95)
    ),
    val isFirebaseSynced: Boolean = true,
    val firebaseDbUrl: String = "https://substation-33-11kv-default-rtdb.firebaseio.com",
    val lastFirebaseSyncTime: Long = System.currentTimeMillis()
) {
    val totalSubstationMw: Double
        get() = feeder1.totalActivePowerMw + feeder2.totalActivePowerMw

    val totalSubstationMva: Double
        get() = feeder1.totalApparentPowerMva + feeder2.totalApparentPowerMva

    val hasAnyFault: Boolean
        get() = feeder1.hasActiveFault || feeder2.hasActiveFault
}

data class Esp32Config(
    val ipAddress: String = "192.168.4.1",
    val port: Int = 80,
    val isSimulationMode: Boolean = true,
    val useFirebaseSync: Boolean = true,
    val firebaseProjectId: String = "substation-33-11kv",
    val firebaseDbUrl: String = "https://substation-33-11kv-default-rtdb.firebaseio.com",
    val firebaseAuthSecret: String = "substation_api_key_2026",
    val ctPrimaryA: Double = 200.0,
    val ctSecondaryA: Double = 5.0,
    val ptPrimaryV: Double = 11000.0,
    val ptSecondaryV: Double = 110.0,
    val overcurrentThresholdA: Double = 350.0,
    val earthFaultThresholdA: Double = 25.0
) {
    val ctRatio: Double get() = if (ctSecondaryA > 0) ctPrimaryA / ctSecondaryA else 40.0
    val ptRatio: Double get() = if (ptSecondaryV > 0) ptPrimaryV / ptSecondaryV else 100.0
}
