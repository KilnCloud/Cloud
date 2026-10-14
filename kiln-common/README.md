# kiln-common

`kiln-common` enthält sicherheitsrelevante, gemeinsam genutzte Hilfsfunktionen für
KilnCloud-Prozesse. Seine eng begrenzten Exporte sind interne Infrastrukturverträge zwischen
Cloud-Modulen und nicht Teil der öffentlichen Drittentwickler-API `kiln-api`.

## Status

**In Arbeit – 35 %:** JPMS-Kapselung und Java-25-Build sind eingerichtet. Validierte Namen,
sichere Auflösung existierender Pfade sowie sichere Token-Erzeugung und -Vergleiche sind
vollständig getestet.

## Öffentliche Schnittstellen

- `at.kilncloud.common.validation.ServerName`
- `at.kilncloud.common.validation.TemplateName`
- `at.kilncloud.common.filesystem.SecurePathResolver`
- `at.kilncloud.common.filesystem.SecureFiles` (Lesen ohne Symlink-Folgen mit Größenlimit, atomares Schreiben, optional Rechte 600)
- `at.kilncloud.common.config.SecureConfig` (strikter JSON-Reader und -Writer; unbekannte Felder, doppelte Schlüssel, Typ-Coercion und Klassennamen-Typisierung werden abgelehnt)
- `at.kilncloud.common.config.ConfigException` (neutrale Fehlerschlüssel, nie Dateiinhalt)
- `at.kilncloud.common.security.SecureTokens`

Das exportierte Paket ist nur für Cloud-Module bestimmt. Fachliche Kommunikation erfolgt
weiterhin ausschließlich über `kiln-api` und `kiln-protocol` (ADR 0002).

## Abhängigkeiten

- `kiln-api`
- Jackson Databind (intern, Konfigurations-JSON); Jimfs nur für Tests
- JUnit 5 ausschließlich für Tests

## Konfiguration

- Kompiliert und erzeugt Bytecode mit Java 25 über `kiln.java-cloud` und `kiln.quality` (siehe `build-logic`)
- Erzeugt Binär-, Quellen- und Javadoc-JARs
- Alle Pakete sind mit JSpecify `@NullMarked` markiert

## Offene Punkte

- Weitere gemeinsame Utilities nach Bedarf der M1-Module (z. B. Konfigurationstypen mit
  maskiertem `toString()` für Secrets)
- Module, die Konfigurationstypen laden, müssen deren Paket auf dem Modulpfad für `com.fasterxml.jackson.databind` öffnen
