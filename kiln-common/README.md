# kiln-common

`kiln-common` enthält sicherheitsrelevante, gemeinsam genutzte Hilfsfunktionen für
KilnCloud-Prozesse. Das Modul exportiert keine Pakete und ist nicht Teil der Public API.

## Status

**Grundgerüst – 3 %:** JPMS-Kapselung, Java-25-Build und die Abhängigkeit zur Public API
sind eingerichtet. Es existieren noch keine Utilities.

## Öffentliche Schnittstellen

Keine. Alle zukünftigen Implementierungen liegen in nicht exportierten `internal`-Paketen.

## Abhängigkeiten

- `kiln-api`
- JUnit 5 ausschließlich für Tests

## Konfiguration

- Kompiliert und erzeugt Bytecode mit Java 25 über `kiln.java-cloud` und `kiln.quality` (siehe `build-logic`)
- Erzeugt Binär-, Quellen- und Javadoc-JARs

## Offene Punkte

- Validierte Werttypen und sichere Konfigurationsbasis implementieren
- Crypto- und Dateisystem-Hilfen nur zusammen mit dedizierten Sicherheitstests ergänzen
- JSpecify und die M0-Qualitätswerkzeuge aktivieren
