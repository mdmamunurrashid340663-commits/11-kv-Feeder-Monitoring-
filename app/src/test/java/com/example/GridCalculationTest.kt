package com.example

import com.example.data.model.Esp32Config
import com.example.data.model.FaultType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class GridCalculationTest {

    @Test
    fun testThreePhaseMegawattPowerCalculation() {
        // Line-to-Line Voltage: 11.0 kV
        // Average Phase Current: 185.0 A
        // Power Factor: 0.95
        val vLineKv = 11.0
        val currentA = 185.0
        val powerFactor = 0.95

        // Formula: P (MW) = (sqrt(3) * V_LL (kV) * I (A) * PF) / 1000
        val activePowerMw = (sqrt(3.0) * vLineKv * currentA * powerFactor) / 1000.0

        // Expected approx 3.348 MW
        assertTrue("Calculated active power should be approximately 3.35 MW", activePowerMw in 3.30..3.40)
    }

    @Test
    fun testCtAndPtRatioCalculations() {
        val config = Esp32Config(
            ctPrimaryA = 200.0,
            ctSecondaryA = 5.0,
            ptPrimaryV = 11000.0,
            ptSecondaryV = 110.0
        )

        assertEquals(40.0, config.ctRatio, 0.001)
        assertEquals(100.0, config.ptRatio, 0.001)
    }

    @Test
    fun testFaultTypeClassifications() {
        assertTrue(FaultType.OVERCURRENT_50.isSevere)
        assertTrue(FaultType.EARTH_FAULT_50N.isSevere)
        assertTrue(FaultType.SHORT_CIRCUIT_PHASE.isSevere)
        assertEquals("50", FaultType.OVERCURRENT_50.code)
        assertEquals("50N", FaultType.EARTH_FAULT_50N.code)
    }
}
