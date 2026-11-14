# VaroScan – Architektur und technische Grenzen

## Ziel
Native Android-App, die benutzerautorisierte Datenquellen inventarisiert, konkrete Messwerte erfasst, Regeln ausführt und Berichte ohne Cloud-Abhängigkeit exportiert.

## Datenfluss
1. UI löst eine Diagnose aus (nicht dauerhaft im Hintergrund).
2. `DiagnosticsEngine` führt definierte `DiagnosticModule`-Adapter aus.
3. Jedes Modul liefert einen `ModuleSnapshot` mit `Measurement` (Label, Wert, Datenquelle), optionalen `Finding` (Schweregrad, Begründung, Evidenz, Handlungsempfehlung) und Zeitstempel.
4. `Rules` liefert nachvollziehbare Warnungen zu messbaren Schwellenwerten.
5. Die UI zeigt die Daten, `ReportJson` exportiert bei explizitem Nutzerwunsch.

## Aktuelle Adapter
| Modul | Android API | Erfasst | Nicht erfasst |
| --- | --- | --- | --- |
| Phone | Build, Battery Broadcast, ActivityManager, StatFs | Modell, OS, Akkustand/-temperatur, RAM, Speicher | Tiefe Firmwaredefekte, genereller Hardwarezustand |
| USB | UsbManager | Gerätekennung und Interface-Deskriptoren | Vendor-spezifische Fehlercodes ohne Adapter |
| Sensors | SensorManager | Inventar | Kalibrier- oder Funktionstests |
| Network | ConnectivityManager | Aktive Verbindung und Konfiguration | Fremde Netzwerke, geheime Zugangsdaten |
| Bluetooth | BluetoothLeScanner | Sichtbare BLE-Advertisements, RSSI | Nicht beworbene oder geschützte Daten |

## Geplante Plugin-Schnittstelle
```kotlin
interface DiagnosticModule {
    val id: String
    val title: String
    fun collect(context: Context): ModuleSnapshot
}
```

Ein späterer Plugin-Katalog sollte zusätzlich angeben:
- erforderliche Berechtigungen und explizite Zustimmung
- unterstützte Busse/Protokolle, Hersteller, Gerätekennungen und Firmwareversionen
- mögliche Nebenwirkungen (nur Lesen standardmäßig)
- genaue Evidenzquelle je Messwert
- Adapter-Version, Fehlermodi und automatisierte Tests

## Sicherheit und Datenschutz
- Offline-first; keine INTERNET-Permission.
- BLE-Suche nur nach Klick, automatisch nach 12 Sekunden beendet.
- USB-Zugriff nur per Android-Bestätigungsdialog; keine USB-Endpunkte werden beschrieben.
- Diagnose-Schlussfolgerungen sollen nie besser klingen als die Messgrundlage.
- Exportdaten können lokale IP-Adressen und BLE-Hardwarekennungen enthalten. Vor Weitergabe prüfen.

## Erweiterbarkeit
- Windows/Linux-Hardware-Agent für S.M.A.R.T., GPU/CPU und Treiberprotokolle.
- OBD-II ELM327 über BLE/klassisches Bluetooth, herstellerspezifische DTCs optional.
- USB CDC/serial, HID, ADB nur nach Zustimmung, BLE GATT, SNMP, mDNS und eigene IoT-Adapter.
- Ereignisbasierter Verlauf, Snapshot-Diffs, reproduzierbare Gerätetests.
- Optional später KI als Erklärschicht, nie als Ersatz für belegte Rohdaten.
