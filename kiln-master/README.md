# kiln-master

`kiln-master` wird die zentrale Steuerung für Registry, Scheduling sowie REST- und
WebSocket-Schnittstellen bereitstellen. Im aktuellen M0-Grundgerüst ist nur ein sicherer,
nebenwirkungsfreier Prozesseinstieg vorhanden.

## Status

**Grundgerüst – 2 %:** Das gekapselte JPMS-Modul, ein ausführbares Artefakt und ein Test des
Einstiegspunkts sind vorhanden. Netzwerk- und Steuerungsfunktionen beginnen erst in M1.

## Öffentliche Schnittstellen

Keine. Kommunikation mit anderen Modulen erfolgt künftig ausschließlich über `kiln-api`
und `kiln-protocol`.

## Abhängigkeiten

- `kiln-api`
- `kiln-common`
- JUnit 5 ausschließlich für Tests

## Konfiguration

- Kompiliert und erzeugt Bytecode mit Java 25
- Einstiegspunkt: `at.kilncloud.master.internal.KilnMaster`
- Der M0-Einstieg greift nicht auf Netzwerk, Dateien oder Prozesse zu

## Offene Punkte

- M1-Lifecycle und sichere Konfigurationsvalidierung
- mTLS-gRPC-Anbindung und Node-Registry
- Scheduling, REST/WebSocket-API und Audit-Log in ihren jeweiligen Meilensteinen
