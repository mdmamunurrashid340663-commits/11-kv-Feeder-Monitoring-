package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGold
import com.example.ui.theme.GridCardNavy
import com.example.ui.theme.StatusFaultRed
import com.example.ui.theme.StatusNormalGreen

@Composable
fun FaultCauseDialog(
    faultEvent: FaultEventEntity,
    isBengali: Boolean,
    onDismiss: () -> Unit,
    onSaveCause: (cause: String, category: String, action: String, engineer: String) -> Unit
) {
    val quickCauses = if (isBengali) {
        listOf(
            "গাছের ডাল স্পর্শ (Tree Branch)",
            "বজ্রপাত / লাইটেনিং (Lightning)",
            "ইনসুলেটর ফ্ল্যাশওভার (Insulator)",
            "জাম্পার কাট (Jumper Cut)",
            "অতিরিক্ত লোড (Overload)",
            "কেবল পাংচার (Cable Fault)",
            "পাখি বা প্রাণী শর্ট সার্কিট (Bird Fault)"
        )
    } else {
        listOf(
            "Tree Branch Contact",
            "Lightning Surge",
            "Insulator Flashover",
            "Jumper Disconnect",
            "High Overload",
            "Underground Cable Puncture",
            "Bird/Animal Short"
        )
    }

    var selectedCategory by remember { mutableStateOf(faultEvent.rootCauseCategory.ifEmpty { quickCauses[0] }) }
    var faultCauseText by remember { mutableStateOf(faultEvent.faultCause) }
    var actionTakenText by remember { mutableStateOf(faultEvent.actionTaken.ifEmpty { if (isBengali) "লাইন পেট্রোলিং সম্পন্ন ও ব্রেকার রিস্টোর করা হয়েছে" else "Patrolling completed and breaker restored" }) }
    var engineerNameText by remember { mutableStateOf(faultEvent.engineerName.ifEmpty { "Duty Engineer" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Edit Cause",
                    tint = ElectricGold,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isBengali) "ফল্টের কারণ ও লগ এন্ট্রি" else "Fault Root Cause Logging",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "${faultEvent.feederName} (${faultEvent.faultCode})",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = if (isBengali) "১. ফল্টের প্রধান ক্যাটাগরি বেছে নিন:" else "1. Select Root Cause Category:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Horizontal chips for quick root causes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickCauses.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricGold.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricGold,
                                containerColor = GridCardNavy,
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // The text box requested by the user: "কি কারণে ফল্ট হয়েছে তা লিখে দেয়ার জন্য অ্যাপস এ একটা বক্স থাকবে"
                Text(
                    text = if (isBengali) "২. কি কারণে ফল্ট হয়েছে বিস্তারিত লিখুন:" else "2. Enter Detailed Cause Description:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = faultCauseText,
                    onValueChange = { faultCauseText = it },
                    placeholder = {
                        Text(
                            text = if (isBengali)
                                "যেমন: পোল নং ৪৫ এর কাছে প্রবল বাতাসে গাছের শুকনো ডাল ১১ কেভি তারের ওপর ভেঙে পড়ায় ফেজ-আর এ ৪২০ অ্যাম্পিয়ার ফল্ট কারেন্ট প্রবাহিত হয়ে ভিওসিবি ট্রিপ করে..."
                            else
                                "e.g. Tree branch fell across Phase R conductors near Pole #45 during high winds causing 420A overcurrent trip...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_fault_cause_box"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricGold,
                        unfocusedBorderColor = Color(0xFF263D5C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action Taken
                Text(
                    text = if (isBengali) "৩. গৃহীত পদক্ষেপ (Action Taken):" else "3. Action Taken / Remedy:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = actionTakenText,
                    onValueChange = { actionTakenText = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_action_taken"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = Color(0xFF263D5C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Engineer Name
                OutlinedTextField(
                    value = engineerNameText,
                    onValueChange = { engineerNameText = it },
                    label = { Text(if (isBengali) "ডিউটি ইঞ্জিনিয়ার / ইনস্পেক্টর" else "Duty Engineer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = Color(0xFF263D5C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFE2E8F0)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveCause(faultCauseText, selectedCategory, actionTakenText, engineerNameText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusNormalGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_cause_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Save",
                    tint = Color(0xFF002214),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBengali) "সংরক্ষণ ও ফায়ারবেস সিঙ্ক" else "Save & Sync to Firebase",
                    color = Color(0xFF002214),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isBengali) "বাতিল" else "Cancel")
            }
        }
    )
}
