# ADR 0001: Convention Plugins in `build-logic`

Status: akzeptiert (2026-10-14)

## Kontext

Vier Module duplizierten ihre Build-Logik. AGENTS.md verlangt Convention Plugins sowie
Spotless, Error Prone/NullAway, JaCoCo und Publishing.

## Entscheidungen

1. **Included Build mit Kotlin-DSL-Precompiled-Script-Plugins** unter `build-logic/`. Versionen
   kommen aus dem gemeinsamen Katalog `gradle/libs.versions.toml`.
2. **Zwei Release-Plugins statt Parameter:** `kiln.java-cloud` (25) und `kiln.java-compat` (21).
   Ein Modul kann das Release nicht vergessen; die Wahl ist eine sichtbare Plugin-ID.
3. **NullAway über `AnnotatedPackages=at.kilncloud`** statt `@NullMarked` je Paket. Das wirkt
   sofort und lückenlos. Die Pflicht-Annotation aus §15 wird später als eigene Regel ergänzt.
   *Nachtrag:* Diese Regel ist jetzt aktiv (`verifyNullMarked`, Teil von `check`).
4. **Coverage-Gate mit Zusatzprüfung:** Gradle überspringt `jacocoTestCoverageVerification` ohne
   Ausführungsdaten. `requireCoverageData` lässt Module mit Produktionscode ohne Tests scheitern.
5. **Dependency Verification bleibt strikt (SHA-256).** Neue Hashes werden von Gradle erzeugt
   und danach gegen Maven Central bzw. das Plugin-Portal gegengeprüft, nie von Hand eingetragen.
6. **Publishing ohne Ziel:** `kiln.publishing` erzeugt nur POM und Publikation. Repositories,
   Zugangsdaten und Signierung gehören zu M6/M8.

## Folgen

- Neue Module brauchen nur zwei bis drei Plugin-IDs.
- Gates sind per Funktionstest nachweislich wirksam.
- Der Test-Lauf der Build-Logik ist Teil von `./gradlew build`.
