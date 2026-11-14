package com.varoxan.varoscan.core

import android.content.Context

enum class Severity { INFO, WARNING, CRITICAL }

data class Measurement(val label: String, val value: String, val source: String)
data class Finding(
    val severity: Severity,
    val title: String,
    val explanation: String,
    val evidence: String,
    val recommendation: String
)
data class ModuleSnapshot(
    val id: String,
    val title: String,
    val measurements: List<Measurement>,
    val findings: List<Finding> = emptyList(),
    val note: String = "",
    val collectedAt: Long = System.currentTimeMillis()
)
interface DiagnosticModule {
    val id: String
    val title: String
    fun collect(context: Context): ModuleSnapshot
}
class DiagnosticsEngine(private val modules: List<DiagnosticModule>) {
    fun collect(context: Context): List<ModuleSnapshot> = modules.map { module ->
        try { module.collect(context) }
        catch (ex: Exception) {
            ModuleSnapshot(module.id, module.title, emptyList(),
                note = "Nicht auslesbar: ${ex.javaClass.simpleName}; keine Diagnose abgeleitet.")
        }
    }
}
