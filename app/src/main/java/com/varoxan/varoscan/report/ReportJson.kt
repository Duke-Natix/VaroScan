package com.varoxan.varoscan.report

import android.os.Build
import com.varoxan.varoscan.ble.BleScanner
import com.varoxan.varoscan.core.ModuleSnapshot
import org.json.JSONArray
import org.json.JSONObject

object ReportJson {
    fun create(snapshots: List<ModuleSnapshot>, bleRecords: List<BleScanner.Record>, bleStatus: String): String {
        val root = JSONObject()
            .put("application", "VaroScan")
            .put("version", "0.1.0")
            .put("createdAtEpochMs", System.currentTimeMillis())
            .put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
            .put("notice", "Messwerte sind Beobachtungen, kein Beweis vollständiger Gerätegesundheit.")
        val modules = JSONArray()
        snapshots.forEach { snapshot ->
            val readings = JSONArray()
            snapshot.measurements.forEach { reading ->
                readings.put(JSONObject()
                    .put("label", reading.label)
                    .put("value", reading.value)
                    .put("source", reading.source))
            }
            val findings = JSONArray()
            snapshot.findings.forEach { finding ->
                findings.put(JSONObject()
                    .put("severity", finding.severity.name)
                    .put("title", finding.title)
                    .put("explanation", finding.explanation)
                    .put("evidence", finding.evidence)
                    .put("recommendation", finding.recommendation))
            }
            modules.put(JSONObject()
                .put("id", snapshot.id)
                .put("title", snapshot.title)
                .put("collectedAtEpochMs", snapshot.collectedAt)
                .put("note", snapshot.note)
                .put("measurements", readings)
                .put("findings", findings))
        }
        root.put("modules", modules)
        root.put("bluetoothScanStatus", bleStatus)
        val peripherals = JSONArray()
        bleRecords.forEach { record ->
            peripherals.put(JSONObject()
                .put("identifier", record.key)
                .put("name", record.label)
                .put("rssiDbm", record.rssi)
                .put("lastSeenEpochMs", record.lastSeen))
        }
        root.put("bleDevices", peripherals)
        return root.toString(2)
    }
}
