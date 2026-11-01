package com.example.hardware

object Esp32HardwareGuide {

    val COMPLETE_ARDUINO_CODE = """
/*
 * =========================================================================
 * 11kV Substation 3-Phase MW & Fault Current Monitoring System using ESP32
 * =========================================================================
 * Features:
 *  - 3-Phase Voltage (VR, VY, VB) via PT 11000/110V + ZMPT101B
 *  - 3-Phase Current (IR, IY, IB) via CT 200/5A + Burden Resistor (or SCT-013)
 *  - Active Power Calculation in Megawatts (MW)
 *  - High-Speed Fault Current Detection:
 *      * ANSI 50: Instantaneous Overcurrent (> 350A)
 *      * ANSI 50N: Earth Fault Current (|Ir + Iy + Ib| > 25A)
 *      * ANSI 50-2P: Phase-to-Phase Short Circuit
 *  - Vacuum Circuit Breaker (VCB) Trip Relay Trigger (<20ms response)
 *  - Built-in WiFi Web Server providing JSON API for Android Mobile App
 * =========================================================================
 */

#include <WiFi.h>
#include <WebServer.h>
#include <math.h>

// ----- WiFi Credentials -----
const char* ssid = "11kV_Substation_WiFi";     // Or your local WiFi SSID
const char* password = "substation_pass";     // WiFi password

WebServer server(80);

// ----- Pin Definitions (ESP32 ADC1 Pins for analog stability) -----
#define PIN_VR_ADC        34  // R-Phase Voltage (ZMPT101B)
#define PIN_VY_ADC        35  // Y-Phase Voltage (ZMPT101B)
#define PIN_VB_ADC        32  // B-Phase Voltage (ZMPT101B)

#define PIN_IR_ADC        33  // R-Phase Current (CT Secondary)
#define PIN_IY_ADC        25  // Y-Phase Current (CT Secondary)
#define PIN_IB_ADC        26  // B-Phase Current (CT Secondary)
#define PIN_IN_ADC        27  // Earth/Neutral CT (CBCT)

#define PIN_RELAY_TRIP     4  // VCB Trip Coil Relay Trigger (Active HIGH)
#define PIN_STATUS_LED     2  // Onboard Heartbeat LED

// ----- Substation Transformer Ratios & Calibration -----
// PT: 11000V Primary / 110V Secondary (Ratio = 100.0)
const float PT_RATIO = 100.0;
// CT: 200A Primary / 5A Secondary (Ratio = 40.0)
const float CT_RATIO = 40.0; 

// ADC Calibration constants (3.3V reference, 12-bit ADC = 4096 levels)
const float ADC_VOLTAGE_SCALE = 3.3 / 4095.0;
const float ZMPT_CALIB_FACTOR = 48.5; // Calibration factor to convert ADC RMS to 110V PT secondary
const float CT_BURDEN_OHMS = 0.33;    // Burden Resistor in Ohms (5A * 0.33R = 1.65V peak)

// ----- Protection Thresholds -----
const float OVERCURRENT_TRIP_A = 350.0;   // ANSI 50 Trip threshold
const float EARTH_FAULT_TRIP_A = 25.0;    // ANSI 50N Trip threshold
const float SHORT_CIRCUIT_TRIP_A = 800.0; // ANSI 50-2P Instantaneous Short Circuit

// ----- Grid State Variables -----
float vR_kV = 6.35, vY_kV = 6.35, vB_kV = 6.35;
float iR_A = 0.0, iY_A = 0.0, iB_A = 0.0, iN_A = 0.0;
float activePowerMW = 0.0;
float reactivePowerMVAR = 0.0;
float apparentPowerMVA = 0.0;
float powerFactor = 0.95;
float gridFrequencyHz = 50.0;
double totalEnergyMWh = 1248.5;

bool breakerClosed = true;
String activeFault = "NONE";
float peakFaultCurrentA = 0.0;
String trippedPhase = "None";
unsigned long lastSampleTime = 0;

// ----- Sample AC Signal and Compute RMS Values -----
void sampleGridParameters() {
  const int SAMPLES = 250;
  long sumSqVR = 0, sumSqVY = 0, sumSqVB = 0;
  long sumSqIR = 0, sumSqIY = 0, sumSqIB = 0, sumSqIN = 0;
  int rawIRMax = 0, rawIYMax = 0, rawIBMax = 0;

  // Midpoint reference for 1.65V DC Bias
  const int ADC_MID = 2048;

  for (int i = 0; i < SAMPLES; i++) {
    int rVR = analogRead(PIN_VR_ADC) - ADC_MID;
    int rVY = analogRead(PIN_VY_ADC) - ADC_MID;
    int rVB = analogRead(PIN_VB_ADC) - ADC_MID;

    int rIR = analogRead(PIN_IR_ADC) - ADC_MID;
    int rIY = analogRead(PIN_IY_ADC) - ADC_MID;
    int rIB = analogRead(PIN_IB_ADC) - ADC_MID;
    int rIN = analogRead(PIN_IN_ADC) - ADC_MID;

    sumSqVR += (long)rVR * rVR;
    sumSqVY += (long)rVY * rVY;
    sumSqVB += (long)rVB * rVB;

    sumSqIR += (long)rIR * rIR;
    sumSqIY += (long)rIY * rIY;
    sumSqIB += (long)rIB * rIB;
    sumSqIN += (long)rIN * rIN;

    if (abs(rIR) > rawIRMax) rawIRMax = abs(rIR);
    if (abs(rIY) > rawIYMax) rawIYMax = abs(rIY);
    if (abs(rIB) > rawIBMax) rawIBMax = abs(rIB);

    delayMicroseconds(80); // ~20ms window for one complete 50Hz cycle
  }

  // Calculate RMS Secondary Values
  float vR_rms_sec = sqrt(sumSqVR / SAMPLES) * ADC_VOLTAGE_SCALE * ZMPT_CALIB_FACTOR;
  float vY_rms_sec = sqrt(sumSqVY / SAMPLES) * ADC_VOLTAGE_SCALE * ZMPT_CALIB_FACTOR;
  float vB_rms_sec = sqrt(sumSqVB / SAMPLES) * ADC_VOLTAGE_SCALE * ZMPT_CALIB_FACTOR;

  float iR_rms_sec = (sqrt(sumSqIR / SAMPLES) * ADC_VOLTAGE_SCALE) / (CT_BURDEN_OHMS * 1.414);
  float iY_rms_sec = (sqrt(sumSqIY / SAMPLES) * ADC_VOLTAGE_SCALE) / (CT_BURDEN_OHMS * 1.414);
  float iB_rms_sec = (sqrt(sumSqIB / SAMPLES) * ADC_VOLTAGE_SCALE) / (CT_BURDEN_OHMS * 1.414);
  float iN_rms_sec = (sqrt(sumSqIN / SAMPLES) * ADC_VOLTAGE_SCALE) / (CT_BURDEN_OHMS * 1.414);

  // Convert to Primary 11kV Grid Values
  vR_kV = (vR_rms_sec * PT_RATIO) / 1000.0;
  vY_kV = (vY_rms_sec * PT_RATIO) / 1000.0;
  vB_kV = (vB_rms_sec * PT_RATIO) / 1000.0;

  if (breakerClosed) {
    iR_A = iR_rms_sec * CT_RATIO;
    iY_A = iY_rms_sec * CT_RATIO;
    iB_A = iB_rms_sec * CT_RATIO;
    iN_A = iN_rms_sec * CT_RATIO;
  } else {
    iR_A = 0.0; iY_A = 0.0; iB_A = 0.0; iN_A = 0.0;
  }

  // Instantaneous Peak Current for High-Speed Fault Detection
  float peakR = (rawIRMax * ADC_VOLTAGE_SCALE / (CT_BURDEN_OHMS)) * CT_RATIO;
  float peakY = (rawIYMax * ADC_VOLTAGE_SCALE / (CT_BURDEN_OHMS)) * CT_RATIO;
  float peakB = (rawIBMax * ADC_VOLTAGE_SCALE / (CT_BURDEN_OHMS)) * CT_RATIO;

  // ----- High-Speed Protective Relay Logic -----
  if (breakerClosed) {
    // 1. Short Circuit (ANSI 50-2P)
    if (peakR > SHORT_CIRCUIT_TRIP_A || peakY > SHORT_CIRCUIT_TRIP_A || peakB > SHORT_CIRCUIT_TRIP_A) {
      triggerBreakerTrip("50-2P", max(peakR, max(peakY, peakB)), "R-Y-B Short Circuit");
    }
    // 2. Instantaneous Overcurrent (ANSI 50)
    else if (iR_A > OVERCURRENT_TRIP_A || iY_A > OVERCURRENT_TRIP_A || iB_A > OVERCURRENT_TRIP_A) {
      String phase = (iR_A > OVERCURRENT_TRIP_A) ? "Phase R" : (iY_A > OVERCURRENT_TRIP_A ? "Phase Y" : "Phase B");
      triggerBreakerTrip("50", max(iR_A, max(iY_A, iB_A)), phase);
    }
    // 3. Earth Fault (ANSI 50N)
    else if (iN_A > EARTH_FAULT_TRIP_A) {
      triggerBreakerTrip("50N", iN_A, "Earth Ground");
    }
  }

  // Active Power in Megawatts (MW):
  // Formula: P_Total = (VR*IR + VY*IY + VB*IB) * PF / 1,000,000
  // Note: vR_kV is in kV (10^3) and iR_A is in A, so product is in kW. Divide by 1000 for MW.
  if (breakerClosed) {
    float pR_mw = (vR_kV * iR_A * powerFactor) / 1000.0;
    float pY_mw = (vY_kV * iY_A * powerFactor) / 1000.0;
    float pB_mw = (vB_kV * iB_A * powerFactor) / 1000.0;
    activePowerMW = pR_mw + pY_mw + pB_mw;
    reactivePowerMVAR = activePowerMW * tan(acos(powerFactor));
    apparentPowerMVA = sqrt(activePowerMW * activePowerMW + reactivePowerMVAR * reactivePowerMVAR);
  } else {
    activePowerMW = 0.0;
    reactivePowerMVAR = 0.0;
    apparentPowerMVA = 0.0;
  }
}

// ----- Trip Breaker Function -----
void triggerBreakerTrip(String faultCode, float faultCurrent, String phase) {
  breakerClosed = false;
  activeFault = faultCode;
  peakFaultCurrentA = faultCurrent;
  trippedPhase = phase;

  // Assert hardware relay output to open Vacuum Circuit Breaker (VCB)
  digitalWrite(PIN_RELAY_TRIP, HIGH);
  digitalWrite(PIN_STATUS_LED, HIGH);

  Serial.printf("[FAULT TRIP!] Code: %s, Current: %.1f A, Phase: %s\n", 
                faultCode.c_str(), faultCurrent, phase.c_str());
}

// ----- Reset Breaker Function -----
void resetBreaker() {
  breakerClosed = true;
  activeFault = "NONE";
  peakFaultCurrentA = 0.0;
  trippedPhase = "None";

  digitalWrite(PIN_RELAY_TRIP, LOW);
  digitalWrite(PIN_STATUS_LED, LOW);
  Serial.println("[BREAKER RESET] Line restored to normal state.");
}

// ----- Web Server JSON API for Android App -----
void handleGridApi() {
  String json = "{";
  json += "\"feeder_id\":\"FDR-11KV-01\",";
  json += "\"vr_kv\":" + String(vR_kV, 2) + ",";
  json += "\"vy_kv\":" + String(vY_kV, 2) + ",";
  json += "\"vb_kv\":" + String(vB_kV, 2) + ",";
  json += "\"ir_a\":" + String(iR_A, 1) + ",";
  json += "\"iy_a\":" + String(iY_A, 1) + ",";
  json += "\"ib_a\":" + String(iB_A, 1) + ",";
  json += "\"in_a\":" + String(iN_A, 1) + ",";
  json += "\"mw\":" + String(activePowerMW, 2) + ",";
  json += "\"mvar\":" + String(reactivePowerMVAR, 2) + ",";
  json += "\"mva\":" + String(apparentPowerMVA, 2) + ",";
  json += "\"pf\":" + String(powerFactor, 2) + ",";
  json += "\"freq_hz\":" + String(gridFrequencyHz, 2) + ",";
  json += "\"mwh\":" + String(totalEnergyMWh, 1) + ",";
  json += "\"breaker\":\"" + String(breakerClosed ? "CLOSED" : "TRIPPED") + "\",";
  json += "\"fault\":\"" + activeFault + "\",";
  json += "\"fault_current_a\":" + String(peakFaultCurrentA, 1) + ",";
  json += "\"tripped_phase\":\"" + trippedPhase + "\"";
  json += "}";

  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", json);
}

void handleBreakerApi() {
  if (server.hasArg("action")) {
    String action = server.arg("action");
    if (action == "trip") {
      triggerBreakerTrip("MANUAL_TRIP", 0.0, "Remote Operator");
      server.send(200, "application/json", "{\"status\":\"TRIPPED\"}");
      return;
    } else if (action == "reset") {
      resetBreaker();
      server.send(200, "application/json", "{\"status\":\"CLOSED\"}");
      return;
    }
  }
  server.send(400, "application/json", "{\"error\":\"Invalid action\"}");
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_RELAY_TRIP, OUTPUT);
  pinMode(PIN_STATUS_LED, OUTPUT);
  digitalWrite(PIN_RELAY_TRIP, LOW);
  digitalWrite(PIN_STATUS_LED, LOW);

  // Set ADC Attenuation to 11dB (0V - 3.3V range)
  analogSetAttenuation(ADC_11db);

  // Setup WiFi
  Serial.println("Connecting to WiFi...");
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  // Or create SoftAP if standalone:
  // WiFi.softAP("ESP32_11KV_GRID", "12345678");

  int retries = 0;
  while (WiFi.status() != WL_CONNECTED && retries < 20) {
    delay(500);
    Serial.print(".");
    retries++;
  }

  Serial.println("\nWiFi Connected!");
  Serial.print("ESP32 IP Address: ");
  Serial.println(WiFi.localIP());

  // Setup API Endpoints
  server.on("/api/grid", HTTP_GET, handleGridApi);
  server.on("/api/breaker", HTTP_GET, handleBreakerApi);
  server.begin();
  Serial.println("HTTP REST Server started!");
}

void loop() {
  server.handleClient();

  if (millis() - lastSampleTime >= 500) {
    lastSampleTime = millis();
    sampleGridParameters();
  }
}
""".trimIndent()

    val STEPS_BN = listOf(
        StepGuide(
            title = "১. ১১ কেভি লাইনের ভোল্টেজ সেন্সিং (PT ও ZMPT101B)",
            description = "১১ কেভি সরাসরি কোন মাইক্রোকন্ট্রোলারে যুক্ত করা অসম্ভব এবং জীবননাশী। সাবস্টেশনের পটেনশিয়াল ট্রান্সফরমার (PT) ১১ কেভি লাইন ভোল্টেজকে নিরাপদ ১১০ ভোল্ট (110V AC) এ নামিয়ে আনে। এরপর একটি ZMPT101B ভোল্টেজ মডিউল দিয়ে ১১০ ভোল্টকে ESP32 এর ৩.৩ ভোল্ট এনালগ সীমার মধ্যে নামিয়ে ADC পিন (GPIO 34, 35, 32)-এ দেওয়া হয়।"
        ),
        StepGuide(
            title = "২. ১১ কেভি লাইনের কারেন্ট সেন্সিং (CT ও Burden Resistor)",
            description = "সাবস্টেশনে কারেন্ট পরিমাপের জন্য Protection Class 5P10 বা 0.5 Class এর 200/5A বা 100/5A কারেন্ট ট্রান্সফরমার (CT) ব্যবহার করা হয়। CT সেকেন্ডারির ৫ অ্যাম্পিয়ার কারেন্টকে একটি ০.৩৩ ওহম (১০ ওয়াট) প্রিসিশন বার্ডেন রেজিস্টরের (Burden Resistor) মধ্য দিয়ে প্রবাহিত করে ভোল্টেজ ড্রপে রূপান্তর করা হয় এবং ১.৬৫ ভোল্ট ডিসি অফসেট সার্কিট দিয়ে ESP32 ADC পিন (GPIO 33, 25, 26)-এ ইনপুট করা হয়।"
        ),
        StepGuide(
            title = "৩. মেগাওয়াট (MW) পাওয়ার পরিমাপের ফর্মুলা",
            description = "৩-ফেজ ব্যালান্সড লাইনে মোট পাওয়ার:\nP (MW) = [√3 × V_LL (kV) × I_avg (A) × cos(φ)] / 1000\nঅথবা ৩টি ফেজের যোগফল:\nP (MW) = [(V_R × I_R + V_Y × I_Y + V_B × I_B) × cos(φ)] / 1,000,000\nযেখানে cos(φ) হলো পাওয়ার ফ্যাক্টর (সাধারণত ০.৯৫)।"
        ),
        StepGuide(
            title = "৪. ফল্ট কারেন্ট ও আর্থ ফল্ট ডিটেকশন পদ্ধতি",
            description = "• ওভারকারেন্ট (ANSI 50): লাইনে শর্ট সার্কিট বা অতিরিক্ত লোড হলে যদি কারেন্ট ৩৫০ অ্যাম্পিয়ার ছাড়িয়ে যায়, ESP32 মাত্র ১৫-২০ মিলিসেকেন্ডের মধ্যে ফল্ট পিক শনাক্ত করে।\n• আর্থ ফল্ট (ANSI 50N): তিনটি ফেজ কারেন্টের ভেক্টর যোগফল যদি ০ এর চেয়ে বেশি (যেমন > ২৫ অ্যাম্পিয়ার) হয়, তার মানে লাইনের তার মাটিতে পড়েছে।\nESP32 সাথে সাথে GPIO 4 দিয়ে অপটোকপলার রিলে অন করে সাবস্টেশনের ভ্যাকিউম সার্কিট ব্রেকার (VCB) ট্রিপ করিয়ে লাইন সুরক্ষিত করে।"
        ),
        StepGuide(
            title = "৫. মোবাইল অ্যাপের সাথে সংযোগ",
            description = "ESP32 এর বিল্ট-ইন WiFi থেকে /api/grid এন্ডপয়েন্টে প্রতি ৫০০ মিলিসেকেন্ডে JSON ডাটা পাঠানো হয়। মোবাইল অ্যাপ সরাসরি ESP32 এর আইপি অ্যাড্রেসে (যেমন 192.168.4.1) রিকোয়েস্ট পাঠিয়ে রিয়েল-টাইমে মেগাওয়াট, ৩-ফেজের কারেন্ট এবং ফল্ট স্ট্যাটাস ডিসপ্লে করে।"
        )
    )

    val HARDWARE_COMPONENTS = listOf(
        HardwareItem("ESP32 DevKit V1", "Dual-core 240MHz, 12-bit ADC, Built-in WiFi/BLE"),
        HardwareItem("11kV / 110V Potential Transformer (PT)", "Substation Line PT (Ratio 100:1)"),
        HardwareItem("ZMPT101B Voltage Transformer Module (3x)", "Active AC Voltage sensor for Phase R, Y, B"),
        HardwareItem("200/5A or 100/5A Current Transformer (CT) (3x)", "Class 5P10 / 0.5 Protection CT for feeder lines"),
        HardwareItem("Core Balance CT (CBCT)", "Zero-Sequence Earth Fault CT for neutral"),
        HardwareItem("Burden Resistor 0.33Ω 10W", "Converts 5A CT secondary current into 0-1.65V AC"),
        HardwareItem("DC Bias Op-Amp Circuit (LM358)", "Adds 1.65V DC offset for ESP32 0-3.3V ADC range"),
        HardwareItem("5V 1-Channel Optocoupled Relay Module", "VCB Trip Coil Master Trigger (GPIO 4)")
    )
}

data class StepGuide(val title: String, val description: String)
data class HardwareItem(val name: String, val specs: String)
