package com.varoxan.varoscan.modules

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.varoxan.varoscan.core.*
import java.util.Locale

class PhoneModule : DiagnosticModule {
    override val id = "phone"
    override val title = "Smartphone · System"

    override fun collect(context: Context): ModuleSnapshot {
        val measurements = mutableListOf(
            Measurement("Modell", "${Build.MANUFACTURER} ${Build.MODEL}", "Android Build"),
            Measurement("Android / API", "${Build.VERSION.RELEASE} / ${Build.VERSION.SDK_INT}", "Android Build"),
            Measurement("CPU-Architektur", Build.SUPPORTED_ABIS.joinToString(), "Android Build"),
            Measurement("Logische CPU-Kerne", Runtime.getRuntime().availableProcessors().toString(), "Java Runtime")
        )
        val findings = mutableListOf<Finding>()
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (battery != null) {
            val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                measurements += Measurement("Akkuladung",
                    "%.0f %%".format(Locale.GERMANY, level.toDouble() * 100.0 / scale),
                    "ACTION_BATTERY_CHANGED")
            }
            val temp = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            if (temp != Int.MIN_VALUE) {
                val celsius = temp / 10.0
                measurements += Measurement("Akku-Temperatur",
                    "%.1f °C".format(Locale.GERMANY, celsius), "ACTION_BATTERY_CHANGED")
                Rules.batteryTemperature(celsius)?.let { findings += it }
            }
            val millivolts = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            if (millivolts > 0)
                measurements += Measurement("Akku-Spannung", "$" + millivolts + " mV", "ACTION_BATTERY_CHANGED")
            val status = when (battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Wird geladen"
                BatteryManager.BATTERY_STATUS_FULL -> "Voll"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Entlädt"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Lädt nicht"
                else -> "Nicht verfügbar"
            }
            measurements += Measurement("Ladestatus", status, "ACTION_BATTERY_CHANGED")
        }
        val memory = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
        measurements += Measurement("RAM (gesamt / verfügbar)",
            "${formatBytes(memory.totalMem)} / ${formatBytes(memory.availMem)}", "ActivityManager")
        val statFs = StatFs(Environment.getDataDirectory().absolutePath)
        measurements += Measurement("Interner Speicher (frei / gesamt)",
            "${formatBytes(statFs.availableBytes)} / ${formatBytes(statFs.totalBytes)}", "StatFs /data")
        Rules.lowStorage(statFs.availableBytes, statFs.totalBytes)?.let { findings += it }
        return ModuleSnapshot(id, title, measurements, findings,
            "Android schränkt den Zugriff auf andere Apps, Systemprotokolle und Firmware ohne zusätzliche Autorisierung ein.")
    }
}
internal fun formatBytes(bytes: Long): String =
    "%.2f GiB".format(Locale.GERMANY, bytes / 1_073_741_824.0)
