package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hardware.Esp32HardwareGuide
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.GridCardNavy
import com.example.ui.theme.StatusNormalGreen
import com.example.ui.viewmodel.GridViewModel

@Composable
fun Esp32GuideScreen(
    viewModel: GridViewModel,
    modifier: Modifier = Modifier
) {
    val isBengali by viewModel.isBengali.collectAsState()
    val context = LocalContext.current

    var selectedSection by remember { mutableStateOf(0) }
    // 0: Step by Step Guide, 1: Hardware & Pinout, 2: Arduino C++ Code

    val tabs = if (isBengali) {
        listOf("১. ধাপে ধাপে গাইড", "২. পিনআউট ও কম্পোনেন্ট", "৩. ESP32 সি++ কোড")
    } else {
        listOf("1. Step-by-Step Guide", "2. Hardware Pinout", "3. Arduino C++ Code")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF132338)),
                border = BorderStroke(1.dp, Color(0xFF1F3D60))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "ESP32 IoT Hub",
                            tint = ElectricCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isBengali) "১১ কেভি মেগাওয়াট ও ফল্ট মনিটরিং গাইড" else "11kV Grid MW & Fault Telemetry Guide",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isBengali)
                            "ESP32 দিয়ে ১১ কেভি লাইনের মেগাওয়াট পরিমাপ এবং ক্ষতিকর ফল্ট কারেন্ট শনাক্ত করার সম্পূর্ণ কারিগরি ডায়াগ্রাম, ফর্মুলা ও তৈরি সোর্স কোড।"
                        else
                            "Complete technical engineering documentation, formulas, and production-ready ESP32 Arduino C++ firmware for 11kV substation monitoring.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // Section Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    FilterChip(
                        selected = selectedSection == index,
                        onClick = { selectedSection = index },
                        label = { Text(title, style = MaterialTheme.typography.bodySmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricCyan,
                            containerColor = GridCardNavy,
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedSection == index,
                            borderColor = Color(0xFF263D5C),
                            selectedBorderColor = ElectricCyan
                        )
                    )
                }
            }
        }

        when (selectedSection) {
            0 -> {
                // Step-by-Step Practical Engineering Guide in Bengali / English
                items(Esp32HardwareGuide.STEPS_BN) { step ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GridCardNavy),
                        border = BorderStroke(1.dp, Color(0xFF263D5C))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ElectricGold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = step.description,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            1 -> {
                // Hardware Pinout and Components
                item {
                    Text(
                        text = if (isBengali) "প্রয়োজনীয় যন্ত্রাংশ তালিকা (Bill of Materials)" else "Hardware Components",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                items(Esp32HardwareGuide.HARDWARE_COMPONENTS) { comp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = GridCardNavy)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = comp.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Text(
                                    text = comp.specs,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isBengali) "ESP32 পিন কানেকশন ম্যাপ (Pinout Table)" else "ESP32 Pinout Mapping",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GridCardNavy)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            PinRow("GPIO 34 (ADC1)", "VR (Phase R Voltage Sensor)", "ZMPT101B Module 1")
                            PinRow("GPIO 35 (ADC1)", "VY (Phase Y Voltage Sensor)", "ZMPT101B Module 2")
                            PinRow("GPIO 32 (ADC1)", "VB (Phase B Voltage Sensor)", "ZMPT101B Module 3")
                            PinRow("GPIO 33 (ADC1)", "IR (Phase R Current)", "CT 200/5A + Burden 0.33Ω")
                            PinRow("GPIO 25 (ADC2)", "IY (Phase Y Current)", "CT 200/5A + Burden 0.33Ω")
                            PinRow("GPIO 26 (ADC2)", "IB (Phase B Current)", "CT 200/5A + Burden 0.33Ω")
                            PinRow("GPIO 27 (ADC2)", "IN (Earth/Neutral Current)", "Core-Balance CT (CBCT)")
                            PinRow("GPIO 4 (DO)", "VCB Trip Relay Output", "5V Relay -> VCB Trip Coil")
                            PinRow("GPIO 2 (DO)", "Status / Fault Warning LED", "Onboard Blue LED")
                        }
                    }
                }
            }

            2 -> {
                // Arduino C++ Code
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GridCardNavy)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = "Code",
                                        tint = StatusNormalGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "esp32_11kv_grid_monitor.ino",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("ESP32 Arduino Code", Esp32HardwareGuide.COMPLETE_ARDUINO_CODE)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, if (isBengali) "কোড কপি করা হয়েছে!" else "Arduino Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("copy_arduino_code_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = Color(0xFF00222D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBengali) "কোড কপি করুন" else "Copy Code",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF00222D)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF090E14), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = Esp32HardwareGuide.COMPLETE_ARDUINO_CODE,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF98C379),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinRow(pin: String, function: String, device: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = pin,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = ElectricCyan,
            modifier = Modifier.width(110.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = function,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Text(
                text = device,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
