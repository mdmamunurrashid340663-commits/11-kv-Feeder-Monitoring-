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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
    val gridState by viewModel.gridState.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()
    val faultHistory by viewModel.faultHistory.collectAsState()

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
                            text = if (isBengali) "প্রটেক্টিভ রিলে ও ফল্ট ডিটেকশন স্ট্যাটাস" else "Protective Relays & Fault Detection",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    RelayIndicatorRow("ANSI 50", if (isBengali) "ইনস্ট্যান্টেনিয়াস ওভারকারেন্ট" else "Instantaneous Overcurrent", "I > 350 A", gridState.activeFaultType == FaultType.OVERCURRENT_50)
                    Spacer(modifier = Modifier.height(8.dp))
                    RelayIndicatorRow("ANSI 51", if (isBengali) "আইডিএমটি টাইম ওভারকারেন্ট" else "IDMT Timed Overcurrent", "I > 250 A (t=0.5s)", gridState.activeFaultType == FaultType.OVERCURRENT_51)
                    Spacer(modifier = Modifier.height(8.dp))
                    RelayIndicatorRow("ANSI 50N", if (isBengali) "আর্থ ফল্ট প্রোটেকশন" else "Earth Fault Current", "In > 25 A", gridState.activeFaultType == FaultType.EARTH_FAULT_50N)
                    Spacer(modifier = Modifier.height(8.dp))
                    RelayIndicatorRow("ANSI 50-2P", if (isBengali) "ফেজ-টু-ফেজ শর্ট সার্কিট" else "Phase-Phase Short Circuit", "I > 800 A", gridState.activeFaultType == FaultType.SHORT_CIRCUIT_PHASE)
                }
            }
        }

        // Interactive Substation Fault Injector (Test Panel)
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
                                text = if (isBengali) "ফল্ট টেস্ট ও সিমুলেশন প্যানেল" else "Fault Simulation & Injection Test",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (isBengali) "মোবাইলে ফল্ট ডিটেকশন যাচাই করার জন্য নিচের বোতাম চাপুন" else "Tap below to simulate real fault current trips",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.injectFault(FaultType.OVERCURRENT_50, "Phase R", 420.5)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusFaultRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_inject_overcurrent")
                        ) {
                            Text(
                                text = if (isBengali) "ওভারকারেন্ট (420A)" else "Overcurrent (420A)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.injectFault(FaultType.EARTH_FAULT_50N, "Ground Neutral", 42.0)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusWarningAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_inject_earth_fault")
                        ) {
                            Text(
                                text = if (isBengali) "আর্থ ফল্ট (42A)" else "Earth Fault (42A)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1200)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.injectFault(FaultType.SHORT_CIRCUIT_PHASE, "Phase R-Y", 1250.0)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_inject_short_circuit")
                        ) {
                            Text(
                                text = if (isBengali) "শর্ট সার্কিট (1.25kA)" else "Short Circuit (1.25kA)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.resetFault() },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusNormalGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_reset_fault")
                        ) {
                            Text(
                                text = if (isBengali) "স্বাভাবিক রিস্টোর" else "Restore Normal",
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
                    text = if (isBengali) "ফল্ট ইভেন্ট রেকর্ড ও লগ (${faultHistory.size})" else "Fault Event History (${faultHistory.size})",
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

        // Empty state or Fault Logs List
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
                        Text(
                            text = if (isBengali) "১১ কেভি লাইনের সমস্ত ফিডার নিরাপদ ও স্বাভাবিকভাবে চলছে।" else "All 11kV distribution feeders are running within nominal limits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        } else {
            items(faultHistory, key = { it.id }) { fault ->
                FaultEventCard(
                    event = fault,
                    isBengali = isBengali,
                    onMarkCleared = { viewModel.markFaultCleared(fault.id) }
                )
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

@Composable
fun FaultEventCard(
    event: FaultEventEntity,
    isBengali: Boolean,
    onMarkCleared: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault())
    val dateStr = sdf.format(Date(event.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GridCardNavy),
        border = BorderStroke(1.dp, if (event.isCleared) Color(0xFF263D5C) else StatusFaultRed)
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
                        text = event.faultTitle,
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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isBengali) "পিক ফল্ট কারেন্ট" else "Fault Peak",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${String.format("%.1f", event.faultCurrentA)} A",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricGold
                    )
                }

                Column {
                    Text(
                        text = if (isBengali) "আক্রান্ত ফেজ" else "Tripped Phase",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = event.phase,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Column {
                    Text(
                        text = if (isBengali) "ফিডার লাইন" else "Feeder",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = event.feederId,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricCyan
                    )
                }
            }

            if (!event.isCleared) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onMarkCleared,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isBengali) "সমাধান সম্পন্ন হিসেবে চিহ্নিত করুন" else "Mark Cleared",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
