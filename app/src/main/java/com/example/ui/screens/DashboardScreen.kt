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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EditNote
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.data.local.FaultEventEntity
import com.example.data.model.GridFeederState
import com.example.ui.components.FaultCauseDialog
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
    val substationState by viewModel.substationState.collectAsState()
    val connStatus by viewModel.connectionStatus.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()
    val selectedTab by viewModel.selectedFeederTab.collectAsState()
    val espConfig by viewModel.espConfig.collectAsState()
    val activeDialogFault by viewModel.activeDialogFault.collectAsState()

    var showBreakerConfirmDialog by remember { mutableStateOf(false) }

    val activeFeeder: GridFeederState = if (selectedTab == 0) substationState.feeder1 else substationState.feeder2

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Substation & Firebase Status Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Firebase",
                    tint = StatusNormalGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBengali) "ফায়ারবেস ক্লাউড সিঙ্ক: অ্যাক্টিভ" else "Firebase Cloud: Synchronized",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = StatusNormalGreen
                )
            }

            TextButton(
                onClick = { viewModel.startPolling() },
                modifier = Modifier.testTag("refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(16.dp),
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

        Spacer(modifier = Modifier.height(6.dp))

        // 33/11kV Substation Overview Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF132338)),
            border = BorderStroke(1.dp, Color(0xFF1F3D60))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isBengali) "৩৩/১১ কেভি আউটডোর গ্রিড সাবস্টেশন" else "33/11 kV Outdoor Grid Substation",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = if (isBengali) "পাওয়ার ট্রান্সফরমার: ১০/১৪ MVA (${substationState.transformerLoadPercent}% লোড)" else "Power Transformer: 10/14 MVA (${substationState.transformerLoadPercent}% loaded)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isBengali) "মোট সাবস্টেশন লোড" else "Total Substation Load",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricGold
                        )
                        Text(
                            text = "${String.format("%.2f", substationState.totalSubstationMw)} MW",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ElectricGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bus Voltages Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "৩৩ কেভি বাস: ${String.format("%.1f", substationState.busVoltage33kv)} kV",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                    Text(
                        text = "১১ কেভি বাস: ${String.format("%.2f", substationState.busVoltage11kv)} kV",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                    Text(
                        text = "ফ্রিকোয়েন্সি: 50.0 Hz",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dual Feeder Selection Tab
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = GridCardNavy,
            contentColor = ElectricGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ElectricGold
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { viewModel.selectFeederTab(0) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (substationState.feeder1.hasActiveFault) {
                            Box(modifier = Modifier.size(8.dp).background(StatusFaultRed, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isBengali) "ফিডার ০১: টাউন (${String.format("%.1f", substationState.feeder1.totalActivePowerMw)} MW)" else "Feeder 01: Town (${String.format("%.1f", substationState.feeder1.totalActivePowerMw)} MW)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { viewModel.selectFeederTab(1) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (substationState.feeder2.hasActiveFault) {
                            Box(modifier = Modifier.size(8.dp).background(StatusFaultRed, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isBengali) "ফিডার ০২: পল্লী (${String.format("%.1f", substationState.feeder2.totalActivePowerMw)} MW)" else "Feeder 02: Rural (${String.format("%.1f", substationState.feeder2.totalActivePowerMw)} MW)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Active Fault Alert Banner (for the active feeder)
        AnimatedVisibility(visible = activeFeeder.hasActiveFault) {
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
                                text = if (isBengali) "জরুরী: ${activeFeeder.feederNameBn}-এ ফল্ট ঘটেছে!" else "CRITICAL: FAULT ON ${activeFeeder.feederName}!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = StatusFaultRed
                            )
                            Text(
                                text = if (isBengali) activeFeeder.activeFaultType.titleBn else activeFeeder.activeFaultType.titleEn,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isBengali)
                            "পিক ফল্ট কারেন্ট: ${String.format("%.1f", activeFeeder.faultCurrentPeakA)} A (${activeFeeder.trippedPhase}) • ব্রেকার ট্রিপড"
                        else
                            "Peak Fault Current: ${String.format("%.1f", activeFeeder.faultCurrentPeakA)} A (${activeFeeder.trippedPhase}) • VCB Tripped",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = ElectricGold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons: "ফল্টের কারণ লিখুন" (Requested Box) and "রিস্টোর"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // The button that opens the Fault Cause Input Box!
                        Button(
                            onClick = {
                                val tempFault = FaultEventEntity(
                                    feederId = activeFeeder.feederId,
                                    feederName = activeFeeder.feederName,
                                    faultCode = activeFeeder.activeFaultType.code,
                                    faultTitle = if (isBengali) activeFeeder.activeFaultType.titleBn else activeFeeder.activeFaultType.titleEn,
                                    faultCurrentA = activeFeeder.faultCurrentPeakA,
                                    phase = activeFeeder.trippedPhase,
                                    systemVoltageKv = activeFeeder.phaseR.voltageKv,
                                    activePowerAtTripMw = activeFeeder.totalActivePowerMw
                                )
                                viewModel.openFaultCauseDialog(tempFault)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("btn_write_fault_cause")
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Edit Cause",
                                tint = Color(0xFF1E1200),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBengali) "ফল্টের কারণ লিখুন" else "Enter Fault Cause",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1E1200)
                            )
                        }

                        Button(
                            onClick = { viewModel.resetFault(activeFeeder.feederId) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusNormalGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_restore_feeder")
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

        // Circular Active Power (MW) Gauge for Selected Feeder
        PowerGauge(
            currentMw = activeFeeder.totalActivePowerMw,
            maxMw = 5.0,
            frequencyHz = activeFeeder.systemFrequencyHz,
            powerFactor = activeFeeder.powerFactorAvg,
            isBengali = isBengali
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Feeder Metrics (MVA, MVAR, Line Voltage, Neutral Current)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricSmallCard(
                title = if (isBengali) "আপাত পাওয়ার" else "Apparent Power",
                value = "${String.format("%.2f", activeFeeder.totalApparentPowerMva)} MVA",
                accentColor = ElectricCyan,
                modifier = Modifier.weight(1f)
            )

            MetricSmallCard(
                title = if (isBengali) "রিঅ্যাক্টিভ পাওয়ার" else "Reactive Power",
                value = "${String.format("%.2f", activeFeeder.totalReactivePowerMvar)} MVAR",
                accentColor = StatusWarningAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val lineKv = (activeFeeder.phaseR.voltageKv * 1.732)
            MetricSmallCard(
                title = if (isBengali) "লাইন ভোল্টেজ (V_L-L)" else "Line Voltage",
                value = "${String.format("%.2f", lineKv)} kV",
                accentColor = ElectricGold,
                modifier = Modifier.weight(1f)
            )

            MetricSmallCard(
                title = if (isBengali) "আর্থ/নিউট্রাল কারেন্ট" else "Earth Current",
                value = "${String.format("%.1f", activeFeeder.neutralCurrentA)} A",
                accentColor = if (activeFeeder.neutralCurrentA > 20.0) StatusFaultRed else PhaseNeutral,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3-Phase Telemetry Section (R, Y, B) for Active Feeder
        Text(
            text = if (isBengali) "${activeFeeder.feederNameBn} ৩-ফেজ কারেন্ট ও ভোল্টেজ" else "${activeFeeder.feederName} Phase Telemetry",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFF1F5F9)
        )

        Spacer(modifier = Modifier.height(10.dp))

        PhaseCard(phase = activeFeeder.phaseR, isBengali = isBengali)
        Spacer(modifier = Modifier.height(8.dp))
        PhaseCard(phase = activeFeeder.phaseY, isBengali = isBengali)
        Spacer(modifier = Modifier.height(8.dp))
        PhaseCard(phase = activeFeeder.phaseB, isBengali = isBengali)

        Spacer(modifier = Modifier.height(20.dp))

        // Vacuum Circuit Breaker (VCB) Control for Active Feeder
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
                            tint = if (activeFeeder.isBreakerClosed) StatusNormalGreen else StatusFaultRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${activeFeeder.feederName} VCB",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (activeFeeder.isBreakerClosed)
                                    (if (isBengali) "সার্কিট ব্রেকার ক্লোজড (সচল)" else "VCB CLOSED (Energized)")
                                else
                                    (if (isBengali) "সার্কিট ব্রেকার ট্রিপড (বন্ধ)" else "VCB TRIPPED (Isolated)"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (activeFeeder.isBreakerClosed) StatusNormalGreen else StatusFaultRed
                            )
                        }
                    }

                    Button(
                        onClick = { showBreakerConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeFeeder.isBreakerClosed) StatusFaultRed else StatusNormalGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("toggle_breaker_button")
                    ) {
                        Text(
                            text = if (activeFeeder.isBreakerClosed)
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

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Breaker Toggle Confirmation Dialog
    if (showBreakerConfirmDialog) {
        val actionText = if (activeFeeder.isBreakerClosed)
            (if (isBengali) "${activeFeeder.feederNameBn} লাইন ট্রিপ (বিচ্ছিন্ন)" else "TRIP ${activeFeeder.feederName}")
        else
            (if (isBengali) "${activeFeeder.feederNameBn} লাইন পুনরায় চালু (ক্লোজ)" else "CLOSE ${activeFeeder.feederName}")

        AlertDialog(
            onDismissRequest = { showBreakerConfirmDialog = false },
            title = {
                Text(
                    text = if (isBengali) "ব্রেকার কমান্ড নিশ্চিতকরণ" else "Confirm VCB Command",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBengali)
                        "আপনি কি নিশ্চিত যে ESP32 রিলে আউটপুটের মাধ্যমে $actionText করতে চান?"
                    else
                        "Are you sure you want to $actionText via the ESP32 relay trigger?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBreakerConfirmDialog = false
                        viewModel.toggleBreaker(activeFeeder.feederId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeFeeder.isBreakerClosed) StatusFaultRed else StatusNormalGreen
                    )
                ) {
                    Text(if (isBengali) "হ্যাঁ, কমান্ড পাঠান" else "Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBreakerConfirmDialog = false }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // The Interactive Fault Cause Dialog (Shown when duty engineer records cause)
    activeDialogFault?.let { fault ->
        FaultCauseDialog(
            faultEvent = fault,
            isBengali = isBengali,
            onDismiss = { viewModel.dismissFaultCauseDialog() },
            onSaveCause = { cause, category, action, engineer ->
                viewModel.saveFaultCause(fault.id, cause, category, action, engineer)
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
