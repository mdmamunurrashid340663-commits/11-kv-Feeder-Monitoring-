package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.StatusFaultRed
import com.example.ui.theme.StatusNormalGreen
import com.example.ui.theme.StatusWarningAmber
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PowerGauge(
    currentMw: Double,
    maxMw: Double = 6.0,
    frequencyHz: Double,
    powerFactor: Double,
    isBengali: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedMw by animateFloatAsState(
        targetValue = currentMw.toFloat().coerceIn(0f, maxMw.toFloat()),
        animationSpec = tween(durationMillis = 600),
        label = "mwGauge"
    )

    val progress = (animatedMw / maxMw.toFloat()).coerceIn(0f, 1f)
    val startAngle = 140f
    val sweepAngle = 260f
    val currentSweep = sweepAngle * progress

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val strokeWidth = 16.dp.toPx()
            val arcSize = size.width - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background Track
            drawArc(
                color = Color(0xFF1B2A3D),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Dynamic Gradient Arc
            val gaugeBrush = Brush.sweepGradient(
                colors = listOf(
                    StatusNormalGreen,
                    ElectricGold,
                    StatusWarningAmber,
                    StatusFaultRed
                )
            )

            if (currentSweep > 0f) {
                drawArc(
                    brush = gaugeBrush,
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Draw tick marks
            val center = Offset(size.width / 2, size.height / 2)
            val radius = arcSize / 2
            val tickCount = 6
            for (i in 0..tickCount) {
                val tickFraction = i / tickCount.toFloat()
                val angleDeg = startAngle + sweepAngle * tickFraction
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val innerR = radius - strokeWidth * 0.9f
                val outerR = radius + strokeWidth * 0.1f

                val start = Offset(
                    x = center.x + innerR * cos(angleRad).toFloat(),
                    y = center.y + innerR * sin(angleRad).toFloat()
                )
                val end = Offset(
                    x = center.x + outerR * cos(angleRad).toFloat(),
                    y = center.y + outerR * sin(angleRad).toFloat()
                )

                drawCircle(
                    color = Color(0xFF64748B),
                    radius = 2.dp.toPx(),
                    center = end
                )
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = if (isBengali) "মোট পাওয়ার" else "TOTAL ACTIVE POWER",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )
            Text(
                text = String.format("%.2f", currentMw),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp
                ),
                color = ElectricGold
            )
            Text(
                text = if (isBengali) "মেগাওয়াট (MW)" else "Megawatts (MW)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ElectricCyan
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Sub metrics row: Frequency & Power Factor
            Text(
                text = "${String.format("%.2f", frequencyHz)} Hz  •  PF ${String.format("%.2f", powerFactor)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE2E8F0)
            )
        }
    }
}
