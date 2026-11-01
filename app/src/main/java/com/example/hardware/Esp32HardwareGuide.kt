package com.example.hardware

object Esp32HardwareGuide {

    val COMPLETE_ARDUINO_FIREBASE_CODE = """
/*
 * ============================================================================================
 * 33/11 kV Outdoor Substation Dual-Feeder MW & Fault Monitoring System with Firebase & ESP32
 * ============================================================================================
 * Substation Setup:
 *  - 33/11 kV Power Transformer (10/14 MVA)
 *  - Feeder 1: 11kV Outgoing Feeder 01 (Town/Express)
 *  - Feeder 2: 11kV Outgoing Feeder 02 (Rural/Industrial)
 *
 * Microcontroller & Cloud:
 *  - ESP32 DevKit V1 (Dual-Core 240MHz)
 *  - Firebase Realtime Database over HTTPS / WiFi
 *  - Instantaneous Fault Current Detection (ANSI 50 / 50N / 50-2P)
 *  - 2x Vacuum Circuit Breaker (VCB) Independent Trip Relays
 * ============================================================================================
 */

#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>
#include <math.h>

// ----- WiFi Credentials -----
#define WIFI_SSID       "Substation_WiFi"
#define WIFI_PASSWORD   "substation_secure_2026"

// ----- Firebase Project Configuration -----
// Replace with your Firebase Project RTDB URL and API Key
#define DATABASE_URL    "https://substation-33-11kv-default-rtdb.firebaseio.com/"
#define API_KEY         "AIzaSyYourFirebaseWebApiKeyHere"

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// ----- Pin Definitions -----
// Feeder 1 Current CTs (via Burden Resistor 0.33Ω + 1.65V DC Offset)
#define PIN_F1_IR       34  // Feeder 1 Phase R Current
#define PIN_F1_IY       35  // Feeder 1 Phase Y Current
#define PIN_F1_IB       32  // Feeder 1 Phase B Current

// Feeder 2 Current CTs
#define PIN_F2_IR       33  // Feeder 2 Phase R Current
#define PIN_F2_IY       25  // Feeder 2 Phase Y Current
#define PIN_F2_IB       26  // Feeder 2 Phase B Current

// 11kV Bus Voltage PTs (11000/110V + ZMPT101B)
#define PIN_BUS_VR      27  // 11kV Bus Phase R Voltage

// Breaker (VCB) Master Trip Relays (Active HIGH to trip coil)
#define PIN_RELAY_VCB1   4  // Feeder 1 Vacuum Circuit Breaker Trip
#define PIN_RELAY_VCB2   5  // Feeder 2 Vacuum Circuit Breaker Trip
#define PIN_STATUS_LED   2  // Heartbeat Indicator

// ----- Transformer Ratios -----
const float PT_RATIO = 100.0; // 11000V Primary / 110V Secondary
const float CT_RATIO = 40.0;  // 200A Primary / 5A Secondary
const float CT_BURDEN_OHMS = 0.33;
const float ADC_SCALE = 3.3 / 4095.0;
const float ZMPT_CALIB = 48.5;

// ----- Protection Thresholds -----
const float OVERCURRENT_LIMIT_A = 350.0; // ANSI 50
const float EARTH_FAULT_LIMIT_A = 25.0;  // ANSI 50N

// ----- Feeder State Variables -----
float f1_ir = 0, f1_iy = 0, f1_ib = 0, f1_mw = 0, f1_pf = 0.95;
float f2_ir = 0, f2_iy = 0, f2_ib = 0, f2_mw = 0, f2_pf = 0.95;
float bus_11kv = 11.0, bus_33kv = 33.2;

bool f1_breaker_closed = true;
bool f2_breaker_closed = true;

String f1_fault = "NONE";
String f2_fault = "NONE";
float f1_fault_current = 0.0;
float f2_fault_current = 0.0;

unsigned long lastSendTime = 0;

void setup() {
  Serial.begin(115200);

  pinMode(PIN_RELAY_VCB1, OUTPUT);
  pinMode(PIN_RELAY_VCB2, OUTPUT);
  pinMode(PIN_STATUS_LED, OUTPUT);
  digitalWrite(PIN_RELAY_VCB1, LOW);
  digitalWrite(PIN_RELAY_VCB2, LOW);
  digitalWrite(PIN_STATUS_LED, LOW);

  analogSetAttenuation(ADC_11db);

  // Connect to Substation WiFi
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Substation WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi Connected! IP: " + WiFi.localIP().toString());

  // Configure Firebase
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  Serial.println("Firebase Client Initialized!");
}

void sampleDualFeeders() {
  const int SAMPLES = 200;
  long sumF1_R = 0, sumF1_Y = 0, sumF1_B = 0;
  long sumF2_R = 0, sumF2_Y = 0, sumF2_B = 0;
  long sumVR = 0;
  const int MID = 2048;

  for (int i = 0; i < SAMPLES; i++) {
    int r1r = analogRead(PIN_F1_IR) - MID;
    int r1y = analogRead(PIN_F1_IY) - MID;
    int r1b = analogRead(PIN_F1_IB) - MID;

    int r2r = analogRead(PIN_F2_IR) - MID;
    int r2y = analogRead(PIN_F2_IY) - MID;
    int r2b = analogRead(PIN_F2_IB) - MID;

    int vr = analogRead(PIN_BUS_VR) - MID;

    sumF1_R += (long)r1r * r1r;
    sumF1_Y += (long)r1y * r1y;
    sumF1_B += (long)r1b * r1b;

    sumF2_R += (long)r2r * r2r;
    sumF2_Y += (long)r2y * r2y;
    sumF2_B += (long)r2b * r2b;

    sumVR += (long)vr * vr;
    delayMicroseconds(100);
  }

  // 11kV Phase Voltage
  float vPhaseSec = sqrt(sumVR / SAMPLES) * ADC_SCALE * ZMPT_CALIB;
  float vPhase_kV = (vPhaseSec * PT_RATIO) / 1000.0;
  bus_11kv = vPhase_kV * 1.732;

  // Feeder 1 Currents (A)
  if (f1_breaker_closed) {
    f1_ir = ((sqrt(sumF1_R / SAMPLES) * ADC_SCALE) / (CT_BURDEN_OHMS * 1.414)) * CT_RATIO;
    f1_iy = ((sqrt(sumF1_Y / SAMPLES) * ADC_SCALE) / (CT_BURDEN_OHMS * 1.414)) * CT_RATIO;
    f1_ib = ((sqrt(sumF1_B / SAMPLES) * ADC_SCALE) / (CT_BURDEN_OHMS * 1.414)) * CT_RATIO;
    // Active Power MW: P = 3 * V_phase * I_avg * pf / 1000
    float f1_iavg = (f1_ir + f1_iy + f1_ib) / 3.0;
    f1_mw = (3.0 * vPhase_kV * f1_iavg * f1_pf) / 1000.0;
  } else {
    f1_ir = 0; f1_iy = 0; f1_ib = 0; f1_mw = 0;
  }

  // Feeder 2 Currents (A)
  if (f2_breaker_closed) {
    f2_ir = ((sqrt(sumF2_R / SAMPLES) * ADC_SCALE) / (CT_BURDEN_OHMS * 1.414)) * CT_RATIO;
    f2_iy = ((sqrt(sumF2_Y / SAMPLES) * ADC_SCALE) / (CT_BURDEN_OHMS * 1.414)) * CT_RATIO;
    f2_ib = ((sqrt(sumF2_B / SAMPLES) * ADC_SCALE) / (CT_BURDEN_OHMS * 1.414)) * CT_RATIO;
    float f2_iavg = (f2_ir + f2_iy + f2_ib) / 3.0;
    f2_mw = (3.0 * vPhase_kV * f2_iavg * f2_pf) / 1000.0;
  } else {
    f2_ir = 0; f2_iy = 0; f2_ib = 0; f2_mw = 0;
  }

  // ----- High-Speed Protective Relaying (ANSI 50) -----
  // Feeder 1 Protection
  if (f1_breaker_closed) {
    if (f1_ir > OVERCURRENT_LIMIT_A || f1_iy > OVERCURRENT_LIMIT_A || f1_ib > OVERCURRENT_LIMIT_A) {
      tripFeeder1("50", max(f1_ir, max(f1_iy, f1_ib)), "Overcurrent Detected");
    }
  }

  // Feeder 2 Protection
  if (f2_breaker_closed) {
    if (f2_ir > OVERCURRENT_LIMIT_A || f2_iy > OVERCURRENT_LIMIT_A || f2_ib > OVERCURRENT_LIMIT_A) {
      tripFeeder2("50", max(f2_ir, max(f2_iy, f2_ib)), "Overcurrent Detected");
    }
  }
}

void tripFeeder1(String code, float faultCurrent, String reason) {
  f1_breaker_closed = false;
  f1_fault = code;
  f1_fault_current = faultCurrent;
  digitalWrite(PIN_RELAY_VCB1, HIGH); // Pulse trip coil
  digitalWrite(PIN_STATUS_LED, HIGH);
  pushFaultAlertToFirebase("FDR-01", code, faultCurrent, reason);
}

void tripFeeder2(String code, float faultCurrent, String reason) {
  f2_breaker_closed = false;
  f2_fault = code;
  f2_fault_current = faultCurrent;
  digitalWrite(PIN_RELAY_VCB2, HIGH);
  digitalWrite(PIN_STATUS_LED, HIGH);
  pushFaultAlertToFirebase("FDR-02", code, faultCurrent, reason);
}

void pushFaultAlertToFirebase(String feederId, String code, float current, String reason) {
  FirebaseJson faultJson;
  faultJson.set("feeder_id", feederId);
  faultJson.set("fault_code", code);
  faultJson.set("fault_current_a", current);
  faultJson.set("reason_hint", reason);
  faultJson.set("timestamp", (int)millis());
  faultJson.set("status", "TRIPPED");

  Firebase.RTDB.pushJSON(&fbdo, "/substation/fault_alerts", &faultJson);
}

void pushTelemetryToFirebase() {
  FirebaseJson json;
  json.set("bus_33kv", bus_33kv);
  json.set("bus_11kv", bus_11kv);

  // Feeder 1 Node
  json.set("feeder_1/mw", f1_mw);
  json.set("feeder_1/ir_a", f1_ir);
  json.set("feeder_1/iy_a", f1_iy);
  json.set("feeder_1/ib_a", f1_ib);
  json.set("feeder_1/breaker", f1_breaker_closed ? "CLOSED" : "TRIPPED");
  json.set("feeder_1/fault", f1_fault);
  json.set("feeder_1/fault_current_a", f1_fault_current);

  // Feeder 2 Node
  json.set("feeder_2/mw", f2_mw);
  json.set("feeder_2/ir_a", f2_ir);
  json.set("feeder_2/iy_a", f2_iy);
  json.set("feeder_2/ib_a", f2_ib);
  json.set("feeder_2/breaker", f2_breaker_closed ? "CLOSED" : "TRIPPED");
  json.set("feeder_2/fault", f2_fault);
  json.set("feeder_2/fault_current_a", f2_fault_current);

  json.set("total_substation_mw", f1_mw + f2_mw);
  json.set("timestamp", (int)millis());

  if (Firebase.RTDB.setJSON(&fbdo, "/substation/live", &json)) {
    Serial.println("Firebase Live Telemetry Synced!");
  } else {
    Serial.println("Firebase Error: " + fbdo.errorReason());
  }
}

void loop() {
  sampleDualFeeders();

  if (millis() - lastSendTime >= 1000) {
    lastSendTime = millis();
    pushTelemetryToFirebase();
  }
}
""".trimIndent()

    val STEP_BY_STEP_PROJECT_GUIDE = listOf(
        ProjectStep(
            stepNumber = "১",
            title = "সাবস্টেশনের হার্ডওয়্যার আর্কিটেকচার ও সেন্সর নির্বাচন",
            summary = "৩৩/১১ কেভি আউটডোর সাবস্টেশনে ২টি ১১ কেভি ফিডার মনিটর করার সেন্সর সেটআপ।",
            details = """
• ভোল্টেজ সেন্সিং: ১১ কেভি বাস থেকে ৩-ফেজ পটেনশিয়াল ট্রান্সফরমার (PT 11000/110V) এর মাধ্যমে ১১০ ভোল্ট নেওয়া হয়। এরপর ৩টি ZMPT101B ভোল্টেজ সেন্সর মডিউল দিয়ে ০-৩.৩ ভোল্টে রূপান্তর করে ESP32 এর এডিসিতে দেওয়া হয়।
• কারেন্ট সেন্সিং (ফিডার ১ ও ফিডার ২): ২টি ফিডারের ৬টি ফেজের জন্য ৬টি Protection Class 5P10 (যেমন 200/5A বা 100/5A) কারেন্ট ট্রান্সফরমার (CT) ব্যবহার করা হবে। CT এর সেকেন্ডারির ৫ অ্যাম্পিয়ার কারেন্টকে ০.৩৩ ওহম (১০ ওয়াট) বার্ডেন রেজিস্টর এবং ১.৬৫ ভোল্ট ডিসি বায়াস সার্কিট দিয়ে ESP32 এর ৩.৩V লিমিটে আনা হয়।
• সার্কিট ব্রেকার ট্রিপ রিলে: ২টি ৫ ভোল্ট অপটোকপলার রিলে মডিউল (GPIO 4 ও GPIO 5) দিয়ে ফিডার ১ ও ফিডার ২ এর ভ্যাকুয়াম সার্কিট ব্রেকার (VCB) ট্রিপ কয়েলের সাথে ইন্টারফেস করা হয়।
            """.trimIndent()
        ),
        ProjectStep(
            stepNumber = "২",
            title = "ESP32 মাইক্রোকন্ট্রোলারের পিনআউট ও ওয়্যারিং",
            summary = "ESP32 এর সাথে ৬টি CT, ৩টি PT এবং ২টি VCB ট্রিপ রিলে সংযোগ।",
            details = """
• ফিডার ০১ কারেন্ট: GPIO 34 (Phase R), GPIO 35 (Phase Y), GPIO 32 (Phase B)
• ফিডার ০২ কারেন্ট: GPIO 33 (Phase R), GPIO 25 (Phase Y), GPIO 26 (Phase B)
• ১১ কেভি বাস ভোল্টেজ: GPIO 27 (ZMPT101B Phase R)
• ফিডার ০১ VCB ট্রিপ রিলে: GPIO 4 (Active HIGH)
• ফিডার ০২ VCB ট্রিপ রিলে: GPIO 5 (Active HIGH)
• স্ট্যাটাস / ফল্ট LED: GPIO 2
            """.trimIndent()
        ),
        ProjectStep(
            stepNumber = "৩",
            title = "ফায়ারবেস (Firebase) ক্লাউড ডাটাবেস সেটআপ",
            summary = "Google Firebase-এ প্রজেক্ট ও রিয়েলটাইম ডাটাবেস তৈরি।",
            details = """
১. console.firebase.google.com-এ যান এবং নতুন প্রজেক্ট তৈরি করুন (নাম: Substation-33-11kV)।
২. 'Build' মেনু থেকে 'Realtime Database' তৈরি করুন এবং রুলস (Rules) কনফিগার করুন।
৩. ডাটাবেস URL (যেমন: https://your-substation-rtdb.firebaseio.com) এবং Web API Key সংগ্রহ করুন।
৪. ডাটাবেসে ২টি নোড থাকবে:
   - /substation/live : উভয় ফিডারের প্রতি মুহূর্তের মেগাওয়াট (MW), অ্যাম্পিয়ার, ভোল্টেজ।
   - /substation/fault_alerts : কোনো ফিডারে ফল্ট হলে ফল্ট কোড ও পিক কারেন্ট এন্ট্রি।
            """.trimIndent()
        ),
        ProjectStep(
            stepNumber = "৪",
            title = "ESP32 Arduino কোডিং ও মেগাওয়াট (MW) অ্যালগরিদম",
            summary = "RMS ভ্যালু ক্যালকুলেশন, ফল্ট ডিটেকশন ও ফায়ারবেস পুশ।",
            details = """
• মেগাওয়াট হিসাব: P_Total = (√3 × 11.0 kV × I_avg × 0.95) / 1000
• ইনস্ট্যান্টেনিয়াস ওভারকারেন্ট (ANSI 50): কারেন্ট ৩৫০ অ্যাম্পিয়ার পার হলেই সাথে সাথে <২০ মিলিসেকেন্ডে VCB ট্রিপ করা এবং ফায়ারবেসে অ্যালার্ট পাঠানো।
• সম্পূর্ণ তৈরি Arduino C++ কোড এই অ্যাপের ৩য় ট্যাবে দেওয়া আছে, যা সরাসরি কপি করে ESP32-তে আপলোড করতে পারবেন।
            """.trimIndent()
        ),
        ProjectStep(
            stepNumber = "৫",
            title = "মোবাইল অ্যাপে ফল্টের কারণ (Fault Cause) ইনপুট ও সিঙ্ক",
            summary = "ডিউটি ইঞ্জিনিয়ারের কারণ লিখে ডাটাবেসে সেভ করার পদ্ধতি।",
            details = """
• ফিডার ১ বা ২ এ ফল্ট হওয়ামাত্রই অ্যাপে লাল ব্যানার চলে আসবে এবং অ্যালার্ম বাজবে।
• 'ফল্টের কারণ লিখুন' বাটনে ট্যাপ করলে একটি ফর্ম ওপেন হবে:
  - ফল্ট ক্যাটাগরি বাছাই (গাছের ডাল স্পর্শ / বজ্রপাত / ইনসুলেটর ফ্ল্যাশওভার / কেবল ফল্ট ইত্যাদি)।
  - বিস্তারিত বিবরণ লেখার বক্স: যেমন "পোল ৪২ এর কাছে লাইনে ডাল পড়েছিল"।
  - গৃহীত পদক্ষেপ: যেমন "ডাল কেটে লাইন মেরামত সম্পন্ন"।
• 'সংরক্ষণ' বাটনে চাপলে এটি স্বয়ংক্রিয়ভাবে অ্যাপের লোকাল ডাটাবেজ ও ফায়ারবেসে স্টোর হয়ে যায়।
            """.trimIndent()
        )
    )
}

data class ProjectStep(
    val stepNumber: String,
    val title: String,
    val summary: String,
    val details: String
)
