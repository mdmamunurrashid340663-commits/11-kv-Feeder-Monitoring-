package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fault_events")
data class FaultEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val feederId: String,
    val feederName: String,
    val faultCode: String,
    val faultTitle: String,
    val faultCurrentA: Double,
    val phase: String,
    val systemVoltageKv: Double,
    val activePowerAtTripMw: Double,
    val isCleared: Boolean = false
)
