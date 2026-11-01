package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FaultEventEntity
import com.example.data.model.FaultType
import com.example.ui.components.FaultCauseDialog
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.GridCardNavy
import com.example.ui.theme.StatusFaultRed
import com.example.ui.theme.StatusNormalGreen
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.viewmodel.GridViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FaultProtectionScreen(
    viewModel: GridViewModel,
    modifier: Modifier = Modifier
) {
    val substationState by viewModel.substationState.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()
    val faultHistory by viewModel.faultHistory.collectAsState()
    val activeDialogFault by viewModel.activeDialogFault.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Protection Relays Status Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("protection_status_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GridCardNavy),
                border = BorderStroke(1.dp, Color(0xFF263D5C))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Protection Relay",
                            tint = ElectricGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBengali) "২টি ফিডারের রিলে ও ফল্ট মনিটরিং" else "Dual-Feeder Protection Relays",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    RelayIndicatorRow("ANSI 50", if (isBengali) "ইনস্ট্যান্টেনিয়াস ওভারকারেন্ট" else "Instantaneous Overcurrent", "I > 350 A", substationState.hasAnyFault)
                    Spacer(modifier = Modifier.height(8.dp))
                    RelayIndicatorRow("ANSI 50N", if (isBengali) "আর্থ ফল্ট প্রোটেকশন" else "Earth Fault Current", "In > 25 A", false)
                    Spacer(modifier = Modifier.height(8.dp))
                    RelayIndicatorRow("ANSI 50-2P", if (isBengali) "ফেজ-টু-ফেজ শর্ট সার্কিট" else "Phase Short Circuit", "I > 800 A", false)
                }
            }
        }

        // Substation Fault Simulator (Feeder 1 & Feeder 2)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fault_simulator_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF132032)),
                border = BorderStroke(1.dp, Color(0xFF1E3A5F))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Test Faults",
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isBengali) "সাবস্টেশন ফল্ট টেস্ট প্যানেল" else "Substation Fault Injection Test",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (isBengali) "ফিডার ১ বা ২ এ ফল্ট ঘটিয়ে অ্যাপের বক্সটিতে কারণ লিখে টেস্ট করুন" else "Inject faults on Feeder 1 or 2 to test the cause logging box",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isBengali) "ফিডার ০১ (টাউন লাইন) টেস্ট:" else "Feeder 01 Tests:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ElectricGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.injectFault("FDR-01", FaultType.OVERCURRENT_50, "Phase R", 435.0) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusFaultRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_f1_overcurrent")
                        ) {
                            Text(
                                text = if (isBengali) "ফিডার ১: ৪৩৫A ওভারকারেন্ট" else "F1: 435A Overcurrent",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.resetFault("FDR-01") },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusNormalGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.7f).testTag("btn_f1_reset")
                        ) {
                            Text(
                                text = if (isBengali) "রিস্টোর ১" else "Reset F1",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF002214)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isBengali) "ফিডার ০২ (পল্লী লাইন) টেস্ট:" else "Feeder 02 Tests:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.injectFault("FDR-02", FaultType.EARTH_FAULT_50N, "Phase Y-Ground", 38.5) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusWarningAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_f2_earth_fault")
                        ) {
                            Text(
                                text = if (isBengali) "ফিডার ২: ৩৮A আর্থ ফল্ট" else "F2: 38A Earth Fault",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1200)
                            )
                        }

                        Button(
                            onClick = { viewModel.resetFault("FDR-02") },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusNormalGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.7f).testTag("btn_f2_reset")
                        ) {
                            Text(
                                text = if (isBengali) "রিস্টোর ২" else "Reset F2",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF002214)
                            )
                        }
                    }
                }
            }
        }

        // Fault History Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBengali) "ফল্ট হিস্টোরি ও ইঞ্জিনিয়ার্স কারণ লগ (${faultHistory.size})" else "Fault History & Cause Logs (${faultHistory.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFF1F5F9)
                )

                if (faultHistory.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearFaultHistory() },
                        modifier = Modifier.testTag("clear_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Fault Logs List showing User-recorded Causes!
        if (faultHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GridCardNavy)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Safe",
                            tint = StatusNormalGreen,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isBengali) "কোনো ফল্ট রেকর্ড নেই" else "No Fault Events Recorded",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            items(faultHistory, key = { it.id }) { fault ->
                FaultEventCardWithCause(
                    event = fault,
                    isBengali = isBengali,
                    onOpenCauseBox = { viewModel.openFaultCauseDialog(fault) }
                )
            }
        }
    }

    // Interactive Fault Cause Dialog
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
fun FaultEventCardWithCause(
    event: FaultEventEntity,
    isBengali: Boolean,
    onOpenCauseBox: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault())
    val dateStr = sdf.format(Date(event.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GridCardNavy),
        border = BorderStroke(1.dp, if (event.faultCause.isEmpty()) StatusFaultRed else Color(0xFF263D5C))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(StatusFaultRed.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = event.faultCode,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = StatusFaultRed
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.feederName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isBengali) "পিক কারেন্ট: ${String.format("%.1f", event.faultCurrentA)} A (${event.phase})" else "Peak: ${String.format("%.1f", event.faultCurrentA)} A (${event.phase})",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = ElectricGold
                )
                Text(
                    text = event.faultTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Engineer's Recorded Cause Box
            if (event.faultCause.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F1B2B), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = "Cause",
                                tint = StatusNormalGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${event.rootCauseCategory}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = StatusNormalGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "কারণ: ${event.faultCause}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF1F5F9)
                        )

                        if (event.actionTaken.isNotEmpty()) {
                            Text(
                                text = "গৃহীত ব্যবস্থা: ${event.actionTaken} (${event.engineerName})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Button: Open Cause Input Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onOpenCauseBox,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (event.faultCause.isEmpty()) ElectricGold else Color(0xFF22364F)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Edit",
                        tint = if (event.faultCause.isEmpty()) Color(0xFF1E1200) else ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (event.faultCause.isEmpty())
                            (if (isBengali) "ফল্টের কারণ লিখুন" else "Enter Fault Cause")
                        else
                            (if (isBengali) "কারণ এডিট করুন" else "Edit Cause"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (event.faultCause.isEmpty()) Color(0xFF1E1200) else ElectricCyan
                    )
                }
            }
        }
    }
}

@Composable
fun RelayIndicatorRow(
    code: String,
    title: String,
    setting: String,
    isTripped: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (isTripped) StatusFaultRed else StatusNormalGreen, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "$code • $title",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                Text(
                    text = "Threshold: $setting",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Text(
            text = if (isTripped) "TRIP!" else "ARMED",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isTripped) StatusFaultRed else StatusNormalGreen
        )
    }
}
