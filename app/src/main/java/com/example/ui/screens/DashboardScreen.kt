package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PhaseCard
import com.example.ui.components.PowerGauge
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.GridCardNavy
import com.example.ui.theme.PhaseNeutral
import com.example.ui.theme.StatusFaultRed
import com.example.ui.theme.StatusNormalGreen
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.viewmodel.ConnectionStatus
import com.example.ui.viewmodel.GridViewModel

@Composable
fun DashboardScreen(
    viewModel: GridViewModel,
    modifier: Modifier = Modifier
) {
    val gridState by viewModel.gridState.collectAsState()
    val connStatus by viewModel.connectionStatus.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()
    val selectedFeeder by viewModel.selectedFeederIndex.collectAsState()
    val espConfig by viewModel.espConfig.collectAsState()

    var showBreakerConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Connection & Source Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (statusColor, statusText) = when (connStatus) {
                    ConnectionStatus.CONNECTED -> StatusNormalGreen to if (isBengali) "ESP32 সংযুক্ত (${espConfig.ipAddress})" else "ESP32 Connected"
                    ConnectionStatus.CONNECTING -> ElectricGold to if (isBengali) "সংযোগ হচ্ছে..." else "Connecting..."
                    ConnectionStatus.ERROR -> StatusFaultRed to if (isBengali) "সংযোগ বিচ্ছিন্ন" else "Offline"
                    ConnectionStatus.SIMULATING -> ElectricCyan to if (isBengali) "১১ কেভি সিমুলেটর মোড" else "Substation Simulator Mode"
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = statusColor
                )
            }

            TextButton(
                onClick = { viewModel.startPolling() },
                modifier = Modifier.testTag("refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(18.dp),
                    tint = ElectricCyan
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isBengali) "রিফ্রেশ" else "Refresh",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Feeder Selection Horizontal Chips
        val feederList = if (isBengali) {
            listOf("ফিডার ০১: শিল্প এক্সপ্রেস", "ফিডার ০২: টাউন সেন্ট্রাল", "ফিডার ০৩: পল্লী বাণিজ্যিক")
        } else {
            listOf("Feeder 01: Industrial", "Feeder 02: Town Central", "Feeder 03: Rural Grid")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            feederList.forEachIndexed { index, name ->
                FilterChip(
                    selected = selectedFeeder == index,
                    onClick = { viewModel.selectFeeder(index) },
                    label = { Text(name, style = MaterialTheme.typography.bodySmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricGold.copy(alpha = 0.2f),
                        selectedLabelColor = ElectricGold,
                        containerColor = GridCardNavy,
                        labelColor = Color(0xFFCBD5E1)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFeeder == index,
                        borderColor = Color(0xFF263D5C),
                        selectedBorderColor = ElectricGold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Active Fault Alert Banner (if fault detected)
        AnimatedVisibility(visible = gridState.hasActiveFault) {
            val infiniteTransition = rememberInfiniteTransition(label = "alertTransition")
            val alertBorderColor by infiniteTransition.animateColor(
                initialValue = StatusFaultRed,
                targetValue = Color.Transparent,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alertBorder"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("fault_alert_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF330810)),
                border = BorderStroke(2.dp, alertBorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Fault Alert",
                            tint = StatusFaultRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBengali) "জরুরী: লাইনে ফল্ট ধরা পড়েছে!" else "CRITICAL: LINE FAULT DETECTED!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = StatusFaultRed
                            )
                            Text(
                                text = if (isBengali) gridState.activeFaultType.titleBn else gridState.activeFaultType.titleEn,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isBengali) gridState.activeFaultType.descriptionBn else "Automatic relay protection tripped the Vacuum Circuit Breaker to prevent substation transformer damage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD5DA)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBengali) "সর্বোচ্চ ফল্ট কারেন্ট: ${String.format("%.1f", gridState.faultCurrentPeakA)} A (${gridState.trippedPhase})"
                            else "Peak Fault Current: ${String.format("%.1f", gridState.faultCurrentPeakA)} A (${gridState.trippedPhase})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ElectricGold
                        )

                        Button(
                            onClick = { viewModel.resetFault() },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusNormalGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("clear_fault_button")
                        ) {
                            Text(
                                text = if (isBengali) "রিস্টোর" else "Restore",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF002214)
                            )
                        }
                    }
                }
            }
        }

        // Circular Active Power (MW) Gauge
        PowerGauge(
            currentMw = gridState.totalActivePowerMw,
            maxMw = 6.0,
            frequencyHz = gridState.systemFrequencyHz,
            powerFactor = gridState.powerFactorAvg,
            isBengali = isBengali
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Grid Secondary Metrics (MVA, MVAR, Line Voltage, Neutral Current)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Apparent Power (MVA)
            MetricSmallCard(
                title = if (isBengali) "মোট আপাত পাওয়ার" else "Apparent Power",
                value = "${String.format("%.2f", gridState.totalApparentPowerMva)} MVA",
                accentColor = ElectricCyan,
                modifier = Modifier.weight(1f)
            )

            // Reactive Power (MVAR)
            MetricSmallCard(
                title = if (isBengali) "রিঅ্যাক্টিভ পাওয়ার" else "Reactive Power",
                value = "${String.format("%.2f", gridState.totalReactivePowerMvar)} MVAR",
                accentColor = StatusWarningAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 11kV Phase-to-Phase Line Voltage
            val lineKv = (gridState.phaseR.voltageKv * 1.732)
            MetricSmallCard(
                title = if (isBengali) "লাইন ভোল্টেজ (V_L-L)" else "Line Voltage",
                value = "${String.format("%.2f", lineKv)} kV",
                accentColor = ElectricGold,
                modifier = Modifier.weight(1f)
            )

            // Neutral/Earth Current
            MetricSmallCard(
                title = if (isBengali) "আর্থ/নিউট্রাল কারেন্ট" else "Earth Current",
                value = "${String.format("%.1f", gridState.neutralCurrentA)} A",
                accentColor = if (gridState.neutralCurrentA > 20.0) StatusFaultRed else PhaseNeutral,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3-Phase Telemetry Section (R, Y, B)
        Text(
            text = if (isBengali) "৩-ফেজ লাইভ টেলিমেট্রি (Phase Telemetry)" else "3-Phase Telemetry",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFF1F5F9)
        )

        Spacer(modifier = Modifier.height(10.dp))

        PhaseCard(phase = gridState.phaseR, isBengali = isBengali)
        Spacer(modifier = Modifier.height(8.dp))
        PhaseCard(phase = gridState.phaseY, isBengali = isBengali)
        Spacer(modifier = Modifier.height(8.dp))
        PhaseCard(phase = gridState.phaseB, isBengali = isBengali)

        Spacer(modifier = Modifier.height(20.dp))

        // Substation Vacuum Circuit Breaker (VCB) Master Control
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("breaker_control_card"),
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
                            imageVector = Icons.Default.Power,
                            contentDescription = "Breaker",
                            tint = if (gridState.isBreakerClosed) StatusNormalGreen else StatusFaultRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isBengali) "ভ্যাকুয়াম সার্কিট ব্রেকার (VCB)" else "Vacuum Circuit Breaker (VCB)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (gridState.isBreakerClosed)
                                    (if (isBengali) "সার্কিট ব্রেকার ক্লোজড (লাইন সচল)" else "VCB CLOSED (Feeder Energized)")
                                else
                                    (if (isBengali) "সার্কিট ব্রেকার ওপেন/ট্রিপড (লাইন বন্ধ)" else "VCB TRIPPED (Feeder Isolated)"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (gridState.isBreakerClosed) StatusNormalGreen else StatusFaultRed
                            )
                        }
                    }

                    Button(
                        onClick = { showBreakerConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (gridState.isBreakerClosed) StatusFaultRed else StatusNormalGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("toggle_breaker_button")
                    ) {
                        Text(
                            text = if (gridState.isBreakerClosed)
                                (if (isBengali) "ট্রিপ করুন" else "Trip VCB")
                            else
                                (if (isBengali) "ক্লোজ করুন" else "Close VCB"),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Total Energy Accumulated
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF132030))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "Energy",
                        tint = ElectricGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBengali) "মোট ব্যবহৃত বিদ্যুৎ (Energy)" else "Total Energy Delivered",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
                Text(
                    text = "${String.format("%.1f", gridState.energyMwh)} MWh",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ElectricGold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Confirmation Dialog for Circuit Breaker Action
    if (showBreakerConfirmDialog) {
        val actionText = if (gridState.isBreakerClosed)
            (if (isBengali) "১১ কেভি ফিডার লাইন ট্রিপ (বিচ্ছিন্ন)" else "TRIP (De-energize)")
        else
            (if (isBengali) "ফিডার লাইন পুনরায় ক্লোজ (চালু)" else "CLOSE (Energize)")

        AlertDialog(
            onDismissRequest = { showBreakerConfirmDialog = false },
            title = {
                Text(
                    text = if (isBengali) "সার্কিট ব্রেকার নিশ্চিতকরণ" else "Breaker Command Confirmation",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBengali)
                        "আপনি কি নিশ্চিত যে ESP32 রিলে পিনের মাধ্যমে $actionText করতে চান?"
                    else
                        "Are you sure you want to $actionText the 11kV Feeder Breaker via ESP32 relay GPIO?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBreakerConfirmDialog = false
                        viewModel.toggleBreaker()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (gridState.isBreakerClosed) StatusFaultRed else StatusNormalGreen
                    )
                ) {
                    Text(if (isBengali) "হ্যাঁ, কমান্ড পাঠান" else "Confirm Command")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBreakerConfirmDialog = false }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun MetricSmallCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GridCardNavy),
        border = BorderStroke(1.dp, Color(0xFF263D5C))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
    }
}
