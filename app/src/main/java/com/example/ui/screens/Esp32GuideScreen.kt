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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.IntegrationInstructions
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
    // 0: 5-Step Project Roadmap, 1: Hardware Pinout, 2: Complete Arduino Firebase C++ Code

    val tabs = if (isBengali) {
        listOf("১. স্টেপ-বাই-স্টেপ প্রজেক্ট গাইড", "২. পিনআউট ও ওয়্যারিং", "৩. ESP32 + Firebase C++ কোড")
    } else {
        listOf("1. Step-by-Step Project Guide", "2. Pinout & Wiring", "3. ESP32 + Firebase Code")
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
                            contentDescription = "Project Guide",
                            tint = ElectricCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isBengali) "৩৩/১১ কেভি সাবস্টেশন IoT প্রজেক্ট গাইড" else "33/11kV Substation IoT Project Guide",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isBengali)
                            "ESP32 দিয়ে ২টি ১১ কেভি ফিডারের মেগাওয়াট লোড পরিমাপ, ফল্ট ডিটেকশন, ফায়ারবেস ক্লাউড সিঙ্ক এবং অ্যাপে কারণ লিখে সেভ করার সম্পূর্ণ প্রজেক্ট তৈরির ধাপসমূহ।"
                        else
                            "Complete step-by-step engineering implementation for dual-feeder MW power monitoring, fault detection, and Firebase sync with cause logging.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // Section Filter Chips
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
                // Step-by-Step Practical Engineering Guide
                items(Esp32HardwareGuide.STEP_BY_STEP_PROJECT_GUIDE) { step ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GridCardNavy),
                        border = BorderStroke(1.dp, Color(0xFF263D5C))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(ElectricGold.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = step.stepNumber,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ElectricGold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = step.summary,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = ElectricCyan
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = step.details,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            1 -> {
                // Hardware Pinout Table for 2 Feeders
                item {
                    Text(
                        text = if (isBengali) "ESP32 ডুয়াল ফিডার পিন কানেকশন ম্যাপ" else "ESP32 Dual-Feeder Pinout Table",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GridCardNavy)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            PinRow("GPIO 34 (ADC1)", "ফিডার ০১ Phase R কারেন্ট", "CT 200/5A + 0.33Ω Burden")
                            PinRow("GPIO 35 (ADC1)", "ফিডার ০১ Phase Y কারেন্ট", "CT 200/5A + 0.33Ω Burden")
                            PinRow("GPIO 32 (ADC1)", "ফিডার ০১ Phase B কারেন্ট", "CT 200/5A + 0.33Ω Burden")
                            PinRow("GPIO 33 (ADC1)", "ফিডার ০২ Phase R কারেন্ট", "CT 200/5A + 0.33Ω Burden")
                            PinRow("GPIO 25 (ADC2)", "ফিডার ০২ Phase Y কারেন্ট", "CT 200/5A + 0.33Ω Burden")
                            PinRow("GPIO 26 (ADC2)", "ফিডার ০২ Phase B কারেন্ট", "CT 200/5A + 0.33Ω Burden")
                            PinRow("GPIO 27 (ADC2)", "১১ কেভি বাস ভোল্টেজ (VR)", "PT 11000/110V + ZMPT101B")
                            PinRow("GPIO 4 (DO)", "ফিডার ০১ VCB ট্রিপ রিলে", "5V Relay -> VCB 1 Trip Coil")
                            PinRow("GPIO 5 (DO)", "ফিডার ০২ VCB ট্রিপ রিলে", "5V Relay -> VCB 2 Trip Coil")
                            PinRow("GPIO 2 (DO)", "ফল্ট সতর্কতা ইন্ডিকেটর", "Onboard Blue LED")
                        }
                    }
                }
            }

            2 -> {
                // Complete Arduino C++ Code with Firebase Realtime Database
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
                                        text = "esp32_substation_firebase.ino",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("ESP32 Firebase Arduino Code", Esp32HardwareGuide.COMPLETE_ARDUINO_FIREBASE_CODE)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, if (isBengali) "ফায়ারবেস কোড কপি করা হয়েছে!" else "Arduino Firebase Code copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("copy_firebase_arduino_code")
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
                                    text = Esp32HardwareGuide.COMPLETE_ARDUINO_FIREBASE_CODE,
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
            modifier = Modifier.width(120.dp)
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
