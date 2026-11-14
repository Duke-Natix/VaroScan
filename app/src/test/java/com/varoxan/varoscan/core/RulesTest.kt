package com.varoxan.varoscan.core

import org.junit.Assert.*
import org.junit.Test

class RulesTest {
    @Test fun batteryTemperatureThreshold() {
        assertNull(Rules.batteryTemperature(null))
        assertNull(Rules.batteryTemperature(44.9))
        assertEquals(Severity.WARNING, Rules.batteryTemperature(45.0)?.severity)
    }
    @Test fun lowStorageChecks() {
        assertNull(Rules.lowStorage(20_000_000_000L, 100_000_000_000L))
        assertEquals(Severity.WARNING, Rules.lowStorage(500_000_000L, 100_000_000_000L)?.severity)
        assertNull(Rules.lowStorage(5L, 0L))
    }
}
