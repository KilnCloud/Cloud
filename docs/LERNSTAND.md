# Aktuelle Stufe

L1 – Gradle Multi-Modul, Projektstruktur und Version Catalog

# Verstanden

- Subprojekte werden in `settings.gradle.kts` registriert und erscheinen danach im Gradle-Projektbaum.
- `settings.gradle.kts` legt die Struktur des Builds fest; die `build.gradle.kts`-Dateien konfigurieren das Build-Verhalten der einzelnen Projekte.
- `gradlew help` konfiguriert den Build und erkennt Konfigurationsfehler, kompiliert aber keinen Java-Code.
- Bibliotheksaliase aus `gradle/libs.versions.toml` werden in Kotlin DSL über `libs...` referenziert.
- Eine BOM wird als `platform(...)` eingebunden und liefert abgestimmte Versions-Constraints statt ausführbaren Codes.

# Noch unsicher

- Abhängigkeitsrichtung zwischen `api`, `common`, `master` und `node`

# Aktuelle Aufgabe

Die Testabhängigkeiten der übrigen Module auf den Version Catalog umstellen und Java 25 als Toolchain festlegen.

# Verlauf

- 2026-10-14: Lernprojekt gestartet; Gradle-Projekt mit Kotlin DSL, Java-Plugin, JUnit und Gradle Wrapper 9.6.0 angelegt. L1 begonnen.
- 2026-10-14: Die Subprojekte `api`, `common`, `master` und `node` angelegt und registriert; `gradlew projects` erfolgreich ausgeführt.
- 2026-10-14: JUnit 5.14.3 im Version Catalog definiert, über typsichere Aliase eingebunden und den `testRuntimeClasspath` erfolgreich aufgelöst.
