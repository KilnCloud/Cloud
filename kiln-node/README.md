# kiln-node

`kiln-node` wird Gameserver-Prozesse starten, überwachen und an den Master anbinden. Im
aktuellen M0-Grundgerüst ist nur ein sicherer, nebenwirkungsfreier Prozesseinstieg vorhanden.

## Status

**Grundgerüst – 2 %:** Das gekapselte JPMS-Modul, ein ausführbares Artefakt und ein Test des
Einstiegspunkts sind vorhanden. Prozess- und Netzwerkfunktionen beginnen erst in M1.

## Öffentliche Schnittstellen

Keine. Kommunikation mit anderen Modulen erfolgt künftig ausschließlich über `kiln-api`
und `kiln-protocol`.

## Abhängigkeiten

- `kiln-api`
- `kiln-common`
- JUnit 5 ausschließlich für Tests

## Konfiguration

- Kompiliert und erzeugt Bytecode mit Java 25
- Einstiegspunkt: `at.kilncloud.node.internal.KilnNode`
- Der M0-Einstieg greift nicht auf Netzwerk, Dateien oder Prozesse zu

## Offene Punkte

- M1-Lifecycle und sichere Konfigurationsvalidierung
- mTLS-gRPC-Anbindung an den Master
- Prozessstart ausschließlich über `ProcessBuilder` mit Argumentliste
- JDK-Auswahl und cgroups-v2-Ressourcensteuerung in den vorgesehenen Meilensteinen
