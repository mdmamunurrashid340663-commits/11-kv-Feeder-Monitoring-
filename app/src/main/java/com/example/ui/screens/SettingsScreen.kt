package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Esp32Config
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.GridCardNavy
import com.example.ui.theme.StatusNormalGreen
import com.example.ui.viewmodel.GridViewModel

@Composable
fun SettingsScreen(
    viewModel: GridViewModel,
    modifier: Modifier = Modifier
) {
    val currentConfig by viewModel.espConfig.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()
    val context = LocalContext.current

    var ipAddress by remember(currentConfig) { mutableStateOf(currentConfig.ipAddress) }
    var port by remember(currentConfig) { mutableStateOf(currentConfig.port.toString()) }
    var isSimMode by remember(currentConfig) { mutableStateOf(currentConfig.isSimulationMode) }

    var ctPrimary by remember(currentConfig) { mutableStateOf(currentConfig.ctPrimaryA.toString()) }
    var ctSecondary by remember(currentConfig) { mutableStateOf(currentConfig.ctSecondaryA.toString()) }

    var ptPrimary by remember(currentConfig) { mutableStateOf(currentConfig.ptPrimaryV.toString()) }
    var ptSecondary by remember(currentConfig) { mutableStateOf(currentConfig.ptSecondaryV.toString()) }

    var overcurrentThreshold by remember(currentConfig) { mutableStateOf(currentConfig.overcurrentThresholdA.toString()) }
    var earthFaultThreshold by remember(currentConfig) { mutableStateOf(currentConfig.earthFaultThresholdA.toString()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = ElectricGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBengali) "ESP32 ও গ্রিড প্যারামিটার কনফিগারেশন" else "ESP32 & Grid Configuration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        // ESP32 WiFi & Connection Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GridCardNavy),
                border = BorderStroke(1.dp, Color(0xFF263D5C))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "WiFi",
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBengali) "ESP32 নেটওয়ার্ক ও মোড" else "ESP32 Network & Mode",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Simulation Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBengali) "১১ কেভি সিমুলেটর মোড (Simulator)" else "Substation Simulator Mode",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                            Text(
                                text = if (isBengali)
                                    "হার্ডওয়্যার ছাড়া রিয়েল-টাইম টেস্ট করতে অন রাখুন।"
                                else
                                    "Enable to simulate realistic 11kV grid load without physical hardware.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Switch(
                            checked = isSimMode,
                            onCheckedChange = { isSimMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ElectricCyan,
                                checkedTrackColor = ElectricCyan.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("switch_sim_mode")
                        )
                    }

                    if (!isSimMode) {
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { ipAddress = it },
                            label = { Text("ESP32 IP Address") },
                            placeholder = { Text("192.168.4.1") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_esp_ip"),
                            colors = outlinedColors()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Port") },
                            placeholder = { Text("80") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_esp_port"),
                            colors = outlinedColors()
                        )
                    }
                }
            }
        }

        // CT & PT Transformer Ratios
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GridCardNavy),
                border = BorderStroke(1.dp, Color(0xFF263D5C))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "CT PT Ratios",
                            tint = ElectricGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBengali) "ট্রান্সফরমার রেশিও ক্যালিব্রেশন (CT & PT)" else "CT & PT Transformer Ratios",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ctPrimary,
                            onValueChange = { ctPrimary = it },
                            label = { Text("CT Primary (A)") },
                            modifier = Modifier.weight(1f),
                            colors = outlinedColors()
                        )
                        OutlinedTextField(
                            value = ctSecondary,
                            onValueChange = { ctSecondary = it },
                            label = { Text("CT Sec (A)") },
                            modifier = Modifier.weight(1f),
                            colors = outlinedColors()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ptPrimary,
                            onValueChange = { ptPrimary = it },
                            label = { Text("PT Primary (V)") },
                            modifier = Modifier.weight(1f),
                            colors = outlinedColors()
                        )
                        OutlinedTextField(
                            value = ptSecondary,
                            onValueChange = { ptSecondary = it },
                            label = { Text("PT Sec (V)") },
                            modifier = Modifier.weight(1f),
                            colors = outlinedColors()
                        )
                    }
                }
            }
        }

        // Protective Relay Trip Thresholds
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GridCardNavy),
                border = BorderStroke(1.dp, Color(0xFF263D5C))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isBengali) "রিলে ট্রিপ থ্রেশহোল্ড (Relay Trip Limits)" else "Protection Relay Limits",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = overcurrentThreshold,
                        onValueChange = { overcurrentThreshold = it },
                        label = { Text(if (isBengali) "ওভারকারেন্ট ট্রিপ সীমা (ANSI 50 Amps)" else "Overcurrent Limit (A)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = earthFaultThreshold,
                        onValueChange = { earthFaultThreshold = it },
                        label = { Text(if (isBengali) "আর্থ ফল্ট ট্রিপ সীমা (ANSI 50N Amps)" else "Earth Fault Limit (A)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                }
            }
        }

        // Language & UI Preferences
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GridCardNavy),
                border = BorderStroke(1.dp, Color(0xFF263D5C))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = StatusNormalGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBengali) "ভাষা পরিবর্তন (Language)" else "Language (বাংলা / English)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = { viewModel.toggleLanguage() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22364F)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("toggle_language_btn")
                        ) {
                            Text(
                                text = if (isBengali) "বাংলা (BN)" else "English (EN)",
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    val newConfig = Esp32Config(
                        ipAddress = ipAddress,
                        port = port.toIntOrNull() ?: 80,
                        isSimulationMode = isSimMode,
                        ctPrimaryA = ctPrimary.toDoubleOrNull() ?: 200.0,
                        ctSecondaryA = ctSecondary.toDoubleOrNull() ?: 5.0,
                        ptPrimaryV = ptPrimary.toDoubleOrNull() ?: 11000.0,
                        ptSecondaryV = ptSecondary.toDoubleOrNull() ?: 110.0,
                        overcurrentThresholdA = overcurrentThreshold.toDoubleOrNull() ?: 350.0,
                        earthFaultThresholdA = earthFaultThreshold.toDoubleOrNull() ?: 25.0
                    )
                    viewModel.updateConfig(newConfig)
                    Toast.makeText(context, if (isBengali) "কনফিগারেশন সফলভাবে সংরক্ষিত হয়েছে!" else "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_settings_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricGold),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save",
                    tint = Color(0xFF1E1200)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBengali) "সেটিংস সংরক্ষণ করুন" else "Save Configuration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E1200)
                )
            }
        }
    }
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ElectricCyan,
    unfocusedBorderColor = Color(0xFF263D5C),
    focusedLabelColor = ElectricCyan,
    unfocusedLabelColor = Color(0xFF94A3B8),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color(0xFFE2E8F0)
)
