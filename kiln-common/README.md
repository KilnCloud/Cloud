# kiln-common

`kiln-common` enthält sicherheitsrelevante, gemeinsam genutzte Hilfsfunktionen für
KilnCloud-Prozesse. Seine eng begrenzten Exporte sind interne Infrastrukturverträge zwischen
Cloud-Modulen und nicht Teil der öffentlichen Drittentwickler-API `kiln-api`.

## Status

**In Arbeit – 12 %:** JPMS-Kapselung und Java-25-Build sind eingerichtet. `ServerName` und
`TemplateName` erzwingen eine längenbegrenzte ASCII-Allowlist und sind vollständig getestet.

## Öffentliche Schnittstellen

- `at.kilncloud.common.validation.ServerName`
- `at.kilncloud.common.validation.TemplateName`

Das exportierte Paket ist nur für Cloud-Module bestimmt. Fachliche Kommunikation erfolgt
weiterhin ausschließlich über `kiln-api` und `kiln-protocol` (ADR 0002).

## Abhängigkeiten

- `kiln-api`
- JUnit 5 ausschließlich für Tests

## Konfiguration

- Kompiliert und erzeugt Bytecode mit Java 25 über `kiln.java-cloud` und `kiln.quality` (siehe `build-logic`)
- Erzeugt Binär-, Quellen- und Javadoc-JARs
- Alle Pakete sind mit JSpecify `@NullMarked` markiert

## Offene Punkte

- Sichere Pfadauflösung gegen Traversal und Symlink-Ausbruch ergänzen
- SecureRandom-Token und konstanten Bytevergleich ergänzen
- Sichere Konfigurationsbasis in einem getrennten Arbeitspaket planen
