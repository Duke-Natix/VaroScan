package com.varoxan.varoscan.modules

import android.content.Context
import android.hardware.usb.UsbManager
import com.varoxan.varoscan.core.*
import java.util.Locale

class UsbModule : DiagnosticModule {
    override val id = "usb"
    override val title = "USB-C · Angeschlossene Geräte"
    override fun collect(context: Context): ModuleSnapshot {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = manager.deviceList.values.sortedBy { it.deviceId }
        val values = mutableListOf(
            Measurement("Erkannte USB-Geräte", "${devices.size}", "UsbManager.deviceList"))
        devices.forEachIndexed { index, device ->
            val name = try { device.productName ?: device.deviceName }
                catch (_: SecurityException) { device.deviceName }
            val vendor = String.format(Locale.ROOT, "%04X", device.vendorId)
            val product = String.format(Locale.ROOT, "%04X", device.productId)
            values += Measurement("USB ${index + 1}: $name",
                "VID:PID $vendor:$product · Klasse ${device.deviceClass} · " +
                "Interfaces ${device.interfaceCount} · Zugriff " +
                if (manager.hasPermission(device)) "erteilt" else "nicht erteilt",
                "Android USB Host API")
            for (indexInterface in 0 until device.interfaceCount.coerceAtMost(16)) {
                val intf = device.getInterface(indexInterface)
                values += Measurement("Interface ${intf.id}",
                    "Klasse ${intf.interfaceClass} · Subklasse ${intf.interfaceSubclass} · " +
                    "Protokoll ${intf.interfaceProtocol} · Endpunkte ${intf.endpointCount}",
                    "USB-Deskriptor")
            }
        }
        return ModuleSnapshot(id, title, values, note =
            "Die USB-Deskriptoren identifizieren ein Gerät, enthalten aber nicht dessen Fehlerprotokolle. Dafür werden spezielle Adapter benötigt.")
    }
}
