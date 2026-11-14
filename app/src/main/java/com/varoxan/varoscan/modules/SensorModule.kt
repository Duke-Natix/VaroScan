package com.varoxan.varoscan.modules

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.varoxan.varoscan.core.*

class SensorModule : DiagnosticModule {
    override val id = "sensors"
    override val title = "Sensoren · Hardware-Inventar"
    override fun collect(context: Context): ModuleSnapshot {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = manager.getSensorList(Sensor.TYPE_ALL)
        val values = mutableListOf(
            Measurement("Vom System gemeldete Sensoren", "${sensors.size}", "SensorManager"))
        sensors.take(40).forEachIndexed { index, sensor ->
            values += Measurement("Sensor ${index + 1}",
                "${sensor.name} · ${sensor.vendor} · Typ ${sensor.type}", "SensorManager")
        }
        return ModuleSnapshot(id, title, values,
            note = "Inventar, kein Sensor-Funktionstest. Live-Messungen und Kalibrierung folgen in einer späteren Version.")
    }
}
