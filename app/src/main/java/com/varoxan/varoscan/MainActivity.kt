package com.varoxan.varoscan

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.varoxan.varoscan.ble.BleScanner
import com.varoxan.varoscan.core.DiagnosticsEngine
import com.varoxan.varoscan.core.ModuleSnapshot
import com.varoxan.varoscan.core.Severity
import com.varoxan.varoscan.modules.NetworkModule
import com.varoxan.varoscan.modules.PhoneModule
import com.varoxan.varoscan.modules.SensorModule
import com.varoxan.varoscan.modules.UsbModule
import com.varoxan.varoscan.report.ReportJson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native Offline-UI for real Android diagnostics. No network request is made.
 * This version inventories external devices; device-specific error extraction
 * requires explicit protocol adapters in later versions.
 */
class MainActivity : Activity() {
    private val background = Color.rgb(10, 16, 25)
    private val panel = Color.rgb(20, 32, 45)
    private val border = Color.rgb(41, 65, 80)
    private val primary = Color.rgb(91, 226, 235)
    private val muted = Color.rgb(161, 182, 195)
    private val white = Color.rgb(240, 250, 255)
    private val orange = Color.rgb(255, 193, 99)

    private lateinit var scanner: BleScanner
    private lateinit var summary: TextView
    private lateinit var findingsPanel: LinearLayout
    private lateinit var bluetoothPanel: LinearLayout
    private lateinit var modulesPanel: LinearLayout
    private var snapshots: List<ModuleSnapshot> = emptyList()
    private val engine = DiagnosticsEngine(
        listOf(PhoneModule(), UsbModule(), SensorModule(), NetworkModule())
    )

    private val usbPermissionAction = "com.varoxan.varoscan.USB_PERMISSION"
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == usbPermissionAction) {
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                Toast.makeText(context,
                    if (granted) "USB-Zugriff erlaubt" else "USB-Zugriff nicht erteilt",
                    Toast.LENGTH_SHORT).show()
                refreshDiagnostics()
            }
        }
    }

    private fun dp(value: Int): Int = (resources.displayMetrics.density * value + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background

        val filter = IntentFilter(usbPermissionAction)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("DEPRECATION")
            registerReceiver(usbReceiver, filter)
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(this@MainActivity.background)
            isFillViewport = true
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
        }
        scroll.addView(column)
        setContentView(scroll)

        column.addView(label("VAROSCAN  /  DIAGNOSTICS", 13, primary, true))
        column.addView(label("Dein mobiles Diagnosezentrum", 26, white, true)
            .apply { setPadding(0, dp(8), 0, dp(2)) })
        column.addView(label("Echte Messwerte. Klare Datenquellen. Keine simulierten Fehler.", 14, muted))

        summary = label("Initialisiere Diagnose ...", 14, muted).also { column.addView(it, space(14)) }

        val firstRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        firstRow.addView(action("Neu analysieren") { refreshDiagnostics() }, weighted())
        firstRow.addView(action("BLE suchen") { startBluetoothScan() }, weighted())
        column.addView(firstRow)

        val secondRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        secondRow.addView(action("USB freigeben") { requestUsbPermission() }, weighted())
        secondRow.addView(action("JSON exportieren") { exportReport() }, weighted())
        column.addView(secondRow)

        column.addView(label("DIAGNOSEHINWEISE", 13, primary, true), space(20))
        findingsPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(findingsPanel)

        column.addView(label("BLUETOOTH LOW ENERGY", 13, primary, true), space(18))
        bluetoothPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(bluetoothPanel)

        column.addView(label("DATENQUELLEN & MESSWERTE", 13, primary, true), space(18))
        modulesPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(modulesPanel)
        column.addView(label(
            "VaroScan v0.1.0 · Offline-first · Ein USB-/Bluetooth-Inventar ist keine vollständige Geräteprüfung. " +
            "Proprietäre Diagnosedaten erfordern spezielle Adapter.", 12, muted
        ), space(14))

        scanner = BleScanner(this) { runOnUiThread { renderBle() } }
        refreshDiagnostics()
    }

    private fun weighted() = LinearLayout.LayoutParams(0, dp(54), 1f).apply {
        setMargins(dp(3), dp(4), dp(3), dp(4))
    }
    private fun space(top: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    private fun label(value: String, sp: Int, textColor: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = sp.toFloat()
            setTextColor(textColor)
            if (bold) setTypeface(null, Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1.0f)
        }

    private fun action(name: String, onClick: () -> Unit) =
        Button(this).apply {
            text = name
            isAllCaps = false
            textSize = 12f
            setTextColor(this@MainActivity.background)
            backgroundTintList = ColorStateList.valueOf(primary)
            setOnClickListener { onClick() }
        }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(14), dp(15), dp(14))
        background = GradientDrawable().apply {
            setColor(panel)
            cornerRadius = dp(15).toFloat()
            setStroke(dp(1), border)
        }
    }

    private fun appendCard(target: LinearLayout, card: LinearLayout) {
        target.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) })
    }

    private fun refreshDiagnostics() {
        snapshots = engine.collect(this)
        val warningCount = snapshots.sumOf { it.findings.count { f -> f.severity != Severity.INFO } }
        val count = snapshots.sumOf { it.measurements.size }
        val moment = SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(Date())
        summary.text = "$count Messwerte  ·  $warningCount Hinweise  ·  Stand $moment\n" +
            "Keine Warnung bedeutet NICHT, dass alle Geräte fehlerfrei sind."
        renderFindings()
        renderModules()
        if (::scanner.isInitialized) renderBle()
    }

    private fun renderFindings() {
        findingsPanel.removeAllViews()
        val findings = snapshots.flatMap { it.findings }
        if (findings.isEmpty()) {
            val item = card()
            item.addView(label("Keine auffälligen Werte nach den derzeitigen Regeln", 15, white, true))
            item.addView(label(
                "Der Funktionsumfang ist begrenzt. Nicht getestete Komponenten werden nicht als gesund bewertet.",
                12, muted
            ), space(7))
            appendCard(findingsPanel, item)
            return
        }
        findings.forEach { f ->
            val item = card()
            val color = if (f.severity == Severity.CRITICAL) Color.RED else orange
            item.addView(label("${f.severity}: ${f.title}", 16, color, true))
            item.addView(label(f.explanation, 13, white), space(6))
            item.addView(label("BELEG: ${f.evidence}", 12, muted), space(6))
            item.addView(label("EMPFEHLUNG: ${f.recommendation}", 12, primary), space(6))
            appendCard(findingsPanel, item)
        }
    }

    private fun renderModules() {
        modulesPanel.removeAllViews()
        snapshots.forEach { snapshot ->
            val item = card()
            item.addView(label(snapshot.title, 18, white, true))
            if (snapshot.measurements.isEmpty()) {
                item.addView(label("Keine auslesbaren Werte", 13, orange), space(8))
            }
            snapshot.measurements.forEach { m ->
                val line = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, dp(10), 0, 0)
                }
                line.addView(label(m.label, 12, muted))
                line.addView(label(m.value, 14, white, true))
                line.addView(label("Quelle: ${m.source}", 10, muted))
                item.addView(line)
            }
            if (snapshot.note.isNotBlank())
                item.addView(label(snapshot.note, 12, muted), space(12))
            appendCard(modulesPanel, item)
        }
    }

    private fun renderBle() {
        if (!::scanner.isInitialized) return
        bluetoothPanel.removeAllViews()
        val item = card()
        item.addView(label(scanner.state, 14, white, true))
        val found = scanner.devices.values.toList().sortedByDescending { it.rssi }
        item.addView(label("${found.size} Geräte in diesem Scan", 12, muted), space(4))
        found.forEach { device ->
            item.addView(label("${device.label}  ·  ${device.rssi} dBm", 14, white), space(9))
            item.addView(label(device.key, 11, muted))
        }
        item.addView(label(
            "BLE-Discovery zeigt lediglich ausgesendete Daten. Nicht erkannte Geräte können trotzdem in Reichweite sein.",
            12, muted
        ), space(12))
        appendCard(bluetoothPanel, item)
    }

    private fun startBluetoothScan() {
        val perms = scanner.permissions()
        if (scanner.hasPermissions()) scanner.start()
        else requestPermissions(perms, 3001)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
        permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 3001) {
            if (scanner.hasPermissions()) scanner.start()
            else Toast.makeText(this, "Für den BLE-Scan werden Bluetooth-Rechte benötigt.",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun requestUsbPermission() {
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val device = manager.deviceList.values.firstOrNull { !manager.hasPermission(it) }
        if (device == null) {
            Toast.makeText(this,
                if (manager.deviceList.isEmpty()) "Kein USB-Host-Gerät angeschlossen"
                else "Alle gefundenen USB-Geräte sind bereits freigegeben",
                Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(usbPermissionAction).setPackage(packageName)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val pending = PendingIntent.getBroadcast(this, 0, intent, flags)
        try { manager.requestPermission(device, pending) }
        catch (ex: Exception) {
            Toast.makeText(this, "USB-Freigabe nicht möglich: ${ex.javaClass.simpleName}",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun exportReport() {
        if (snapshots.isEmpty()) refreshDiagnostics()
        val name = "VaroScan-" +
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".json"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json")
            .putExtra(Intent.EXTRA_TITLE, name)
        @Suppress("DEPRECATION")
        startActivityForResult(intent, 4001)
    }

    @Deprecated("Uses the platform document picker for a self-contained Activity implementation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 4001 || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            val json = ReportJson.create(snapshots, scanner.devices.values.toList(), scanner.state)
            val output = contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Kein Schreibzugriff")
            output.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            Toast.makeText(this, "Diagnosebericht gespeichert", Toast.LENGTH_LONG).show()
        } catch (ex: Exception) {
            Toast.makeText(this, "Export fehlgeschlagen: ${ex.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        if (::scanner.isInitialized) scanner.stop()
        unregisterReceiver(usbReceiver)
        super.onDestroy()
    }
}
