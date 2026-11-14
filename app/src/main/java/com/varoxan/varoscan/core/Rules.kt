package com.varoxan.varoscan.core

import java.util.Locale

object Rules {
    fun batteryTemperature(celsius: Double?): Finding? {
        if (celsius == null || celsius < 45.0) return null
        return Finding(
            Severity.WARNING,
            "Akku ungewöhnlich warm",
            "Die gemeldete Akkutemperatur ist erhöht. Das allein beweist keinen Defekt.",
            "Akku-Temperatur: %.1f °C (Hinweisschwelle ≥ 45 °C)".format(Locale.US, celsius),
            "Belastung und Laden reduzieren. Bei starker Hitze abkühlen lassen; ggf. Service kontaktieren."
        )
    }
    fun lowStorage(freeBytes: Long, totalBytes: Long): Finding? {
        if (totalBytes <= 0 || freeBytes < 0) return null
        if (freeBytes >= 1_073_741_824L && freeBytes.toDouble() / totalBytes >= 0.10) return null
        return Finding(
            Severity.WARNING,
            "Wenig freier interner Speicher",
            "Zu wenig Speicherplatz kann Updates und Apps beeinträchtigen. Kein Hardwaredefekt nachgewiesen.",
            "Frei: ${freeBytes / 1_048_576} MiB / ${totalBytes / 1_048_576} MiB",
            "Nicht mehr benötigte Dateien sichern oder löschen und erneut prüfen."
        )
    }
}
