package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PhaseMeasurement
import com.example.ui.theme.GridCardNavy
import com.example.ui.theme.PhaseBlue
import com.example.ui.theme.PhaseRed
import com.example.ui.theme.PhaseYellow
import com.example.ui.theme.StatusFaultRed
import com.example.ui.theme.StatusNormalGreen

@Composable
fun PhaseCard(
    phase: PhaseMeasurement,
    isBengali: Boolean,
    modifier: Modifier = Modifier
) {
    val phaseColor = when (phase.phase) {
        "R" -> PhaseRed
        "Y" -> PhaseYellow
        else -> PhaseBlue
    }

    val phaseName = when (phase.phase) {
        "R" -> if (isBengali) "ফেজ-আর (Phase R)" else "Phase R"
        "Y" -> if (isBengali) "ফেজ-ওয়াই (Phase Y)" else "Phase Y"
        else -> if (isBengali) "ফেজ-বি (Phase B)" else "Phase B"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("phase_card_${phase.phase}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = GridCardNavy),
        border = BorderStroke(
            width = if (phase.isTripped) 2.dp else 1.dp,
            color = if (phase.isTripped) StatusFaultRed else Color(0xFF263D5C)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Phase badge, Name, and Status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(phaseColor.copy(alpha = 0.2f), CircleShape)
                            .border(1.5.dp, phaseColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = phase.phase,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = phaseColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = phaseName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF1F5F9)
                    )
                }

                if (phase.isTripped) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(StatusFaultRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Fault Trip",
                            tint = StatusFaultRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBengali) "ট্রিপড!" else "TRIPPED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = StatusFaultRed
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(StatusNormalGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(StatusNormalGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBengali) "সচল" else "LIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = StatusNormalGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Measurement Values Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Voltage Column
                Column {
                    Text(
                        text = if (isBengali) "ভোল্টেজ (V_L-N)" else "Voltage",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${String.format("%.2f", phase.voltageKv)} kV",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFE2E8F0)
                    )
                }

                // Current Column
                Column {
                    Text(
                        text = if (isBengali) "কারেন্ট (Load I)" else "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${String.format("%.1f", phase.currentA)} A",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = phaseColor
                    )
                }

                // Power Column
                Column {
                    Text(
                        text = if (isBengali) "পাওয়ার (Active P)" else "Power",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${String.format("%.2f", phase.activePowerMw)} MW",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFFB703)
                    )
                }
            }
        }
    }
}
