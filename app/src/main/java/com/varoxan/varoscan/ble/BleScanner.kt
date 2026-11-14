package com.varoxan.varoscan.ble

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper

/** Nur explizites Discovery; keine Verbindung oder Übertragung an Server. */
class BleScanner(private val context: Context, private val notifyUpdate: () -> Unit) {
    data class Record(val key: String, val label: String, val rssi: Int, val lastSeen: Long)
    val devices = linkedMapOf<String, Record>()
    var state = "Nicht gestartet"
        private set

    private val handler = Handler(Looper.getMainLooper())
    private val manager by lazy { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private var scanning = false
    private val timeout = Runnable { stop() }
    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val key = try { result.device.address }
                catch (_: SecurityException) { "unbekannt-${result.rssi}" }
            val label = result.scanRecord?.deviceName ?: "BLE-Gerät"
            devices[key] = Record(key, label, result.rssi, System.currentTimeMillis())
            notifyUpdate()
        }
        override fun onScanFailed(errorCode: Int) {
            scanning = false
            handler.removeCallbacks(timeout)
            state = "Scan fehlgeschlagen (Android-Code $errorCode)"
            notifyUpdate()
        }
    }

    fun permissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun hasPermissions() = permissions().all {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    fun start() {
        if (!hasPermissions()) { state = "Berechtigung fehlt"; notifyUpdate(); return }
        try {
            val adapter = manager.adapter
            if (adapter == null) { state = "Bluetooth nicht verfügbar"; notifyUpdate(); return }
            if (!adapter.isEnabled) { state = "Bluetooth ist ausgeschaltet"; notifyUpdate(); return }
            val scanner = adapter.bluetoothLeScanner
            if (scanner == null) { state = "BLE-Scanner nicht verfügbar"; notifyUpdate(); return }
            stop()
            devices.clear()
            scanner.startScan(null,
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                callback)
            scanning = true
            state = "Suche läuft (12 Sekunden)"
            handler.postDelayed(timeout, 12_000L)
            notifyUpdate()
        } catch (_: SecurityException) {
            state = "Berechtigung verweigert"
            notifyUpdate()
        } catch (ex: RuntimeException) {
            state = "Scan fehlgeschlagen: ${ex.javaClass.simpleName}"
            notifyUpdate()
        }
    }

    fun stop() {
        handler.removeCallbacks(timeout)
        if (scanning) {
            try { manager.adapter?.bluetoothLeScanner?.stopScan(callback) }
            catch (_: SecurityException) { }
            catch (_: RuntimeException) { }
            scanning = false
            state = "Abgeschlossen (${devices.size} Geräte entdeckt)"
            notifyUpdate()
        }
    }
}
