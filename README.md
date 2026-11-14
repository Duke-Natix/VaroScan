# VaroScan · Universal Device Diagnostics

**VaroScan** ist eine native Android-App (Kotlin), die tatsächlich zugängliche Messwerte und Gerätedaten auswertet. Die erste Version arbeitet **lokal und ohne Cloud-Konto**.

## Bereits implementiert (v0.1.0)
- Android-Systemstatus: Geräte-/OS-Informationen, Akku-Ladung, -Temperatur, -Spannung und -Status, RAM und freier Speicher
- USB-Host-Inventar: angeschlossene USB-Geräte (Vendor ID, Product ID, Geräteklasse und Schnittstellen), optionale Android-Zugriffsanfrage
- Bluetooth LE: auf ausdrücklichen Klick zeitlich begrenzter Scan mit gefundenem Gerätenamen und Signalstärke
- Netzwerk: aktiver Transport, vom Betriebssystem bestätigte Konnektivität, lokale IP und DNS
- Sensorsystem: verfügbare Sensoren im Inventar
- Nachvollziehbare Diagnosehinweise mit Messwert-Evidenz und Handlungsempfehlung (z. B. hohe Akkutemperatur, wenig freier Speicher)
- Lokaler JSON-Export der Messungen und Findings

## Build: GitHub Actions (APK)
1. Öffne den Tab **Actions** und wähle den Workflow **Android Debug APK**.
2. Nach einem erfolgreichen Lauf: **Artifacts → VaroScan-debug-apk** herunterladen.
3. ZIP entpacken und `app-debug.apk` auf einem Android-Gerät installieren. Eine Debug-APK ist kein signiertes Store-Release.

Die Pipeline kann auch manuell per **Run workflow** gestartet werden. Prüfe die Build-Logs, bevor du die APK als funktionierend ansiehst.

## Lokal in Android Studio
1. Das Repository klonen und in Android Studio öffnen.
2. JDK 17 installieren/auswählen und Android SDK 35 bereitstellen.
3. Gradle-Sync durchführen und `app` auf einem realen Android-Gerät starten.
4. USB-Host-Unterstützung und USB-C-OTG sind für USB-Geräte notwendig; BLE verlangt die Android-Laufzeitberechtigung.

Android Gradle Plugin **8.9.2**, Kotlin **2.0.21**, Gradle **8.11.1**, Android API 35.

## Diagnose-Architektur

```text
MainActivity (native UI)
    │
    ├── DiagnosticsEngine
    │     ├── PhoneModule
    │     ├── UsbModule
    │     ├── SensorModule
    │     └── NetworkModule
    ├── BleScanner (asynchron, 12 Sekunden)
    ├── Rules (evidenzbasierte Warnungen)
    └── ReportJson (lokaler Export)
```

Jedes Modul implementiert `DiagnosticModule` und liefert `ModuleSnapshot` mit nachvollziehbaren Messwertquellen und optionalen Findings. Das Format kann später für OBD-II, PC-Agenten, serielle Geräte und IoT-Protokolle erweitert werden.

## Wichtige Grenzen
- **Keine erfundenen Messungen**: Fehlende Werte werden weggelassen oder als nicht verfügbar angezeigt.
- Ein Android-Handy kann weder automatisch jede Hardwarekomponente noch Firmwarefehler externer Geräte diagnostizieren. Dazu benötigt es offengelegte Geräteprotokolle, einen Adapter, einen autorisierten Agenten oder zusätzliche Hardware.
- USB-Deskriptoren geben eine Geräte-Identität, **keinen garantierten Gesundheitszustand**.
- Das Sensor-Inventar beweist nicht die Funktion aller Sensoren.
- BLE-Discovery liest *keine* privaten Geräteinformationen aus.
- Die Regeln erkennen Hinweise, **keine medizinische, elektrische oder mechanische Sicherheitsfreigabe**.
- Keine Hintergrundüberwachung, kein Hochladen von Diagnosedaten. Im JSON-Export können jedoch IP-Adressen und BLE-Gerätekennungen enthalten sein: vor dem Teilen prüfen!

## Roadmap
- v0.2: Live-Sensor-Graphen und Selbsttests, USB-Klassenadapter
- v0.3: BLE-GATT-Adapter mit gerätespezifischer Einwilligung, Verlauf
- v0.4: optionaler Windows/Linux-Agent für S.M.A.R.T. und Hardwarediagnose
- v0.5: OBD-II (ELM327) inkl. PIDs und DTCs
- Später: Diagnosebibliothek, dokumentierte Plugins, evidenzbasierte Ursachenanalyse

Siehe [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
