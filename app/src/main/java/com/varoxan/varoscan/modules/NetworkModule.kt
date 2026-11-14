package com.varoxan.varoscan.modules

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.varoxan.varoscan.core.*

class NetworkModule : DiagnosticModule {
    override val id = "network"
    override val title = "Netzwerk · Verbindungsstatus"

    override fun collect(context: Context): ModuleSnapshot {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return ModuleSnapshot(id, title,
            listOf(Measurement("Aktive Verbindung", "Keine", "ConnectivityManager")),
            note = "Keine aktive Netzwerkverbindung. Das ist nicht automatisch ein Fehler.")
        val caps = cm.getNetworkCapabilities(network)
        val link = cm.getLinkProperties(network)
        val types = buildList {
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) add("WLAN")
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) add("Mobilfunk")
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) add("Ethernet")
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) add("VPN")
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) == true) add("Bluetooth")
        }
        val values = mutableListOf(
            Measurement("Verbindungstyp", types.joinToString().ifBlank { "Unbekannt" }, "NetworkCapabilities"),
            Measurement("Vom System validiert",
                if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) "Ja" else "Nein / unbekannt",
                "NetworkCapabilities"),
            Measurement("Getaktete Verbindung", if (cm.isActiveNetworkMetered) "Ja" else "Nein", "ConnectivityManager"))
        link?.let {
            val addresses = it.linkAddresses.mapNotNull { a -> a.address.hostAddress }
            if (addresses.isNotEmpty()) values += Measurement("Lokale IP-Adressen",
                addresses.joinToString(" | "), "LinkProperties")
            if (it.dnsServers.isNotEmpty()) values += Measurement("DNS-Server",
                it.dnsServers.mapNotNull { server -> server.hostAddress }.joinToString(" | "), "LinkProperties")
        }
        return ModuleSnapshot(id, title, values, note =
            "Lokale Android-Netzwerkdaten; keine fremden Geräte gescannt oder überwacht.")
    }
}
