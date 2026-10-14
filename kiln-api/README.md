# kiln-api

`kiln-api` ist die stabile öffentliche Java-Schnittstelle für Drittanbieter und alle
KilnCloud-Module. Das Modul bleibt frei von Implementierungsdetails und wird für die
Kompatibilität mit unterstützten Minecraft-Plattformen als Java-21-Bytecode gebaut.

## Status

**Grundgerüst – 5 %:** Der JPMS-Moduldeskriptor, ein dokumentierter API-Anker und die
Java-21-Buildkonfiguration sind vorhanden. Fachliche Verträge und Kompatibilitätsprüfungen
folgen in späteren Meilensteinen.

## Öffentliche Schnittstellen

- `at.kilncloud.api.KilnCloudApi`: leerer Wurzelvertrag für künftige, fachliche APIs
- JPMS exportiert ausschließlich `at.kilncloud.api`

## Abhängigkeiten

- Keine Abhängigkeit zu anderen KilnCloud-Modulen
- JUnit 5 ausschließlich für Tests

## Konfiguration

- Kompiliert mit einer Java-25-Toolchain und `--release 21`
- Erzeugt Binär-, Quellen- und Javadoc-JARs

## Offene Punkte

- Fachliche Schnittstellen und immutable Modelle ergänzen
- JSpecify-Null-Verträge einführen
- japicmp gegen die erste veröffentlichte API-Version konfigurieren
- Java- und Kotlin-Einstiegsdokumentation erstellen
