# build-logic

`build-logic` ist ein Gradle-Included-Build mit den Convention Plugins aller KilnCloud-Module.
Compiler-, Test-, Coverage-, Formatierungs-, Analyse- und Publishing-Regeln sind hier einmal
definiert und werden von den Modulen nur noch per Plugin-ID angewendet.

## Status

**in Arbeit – 60 %.** Java-, Qualitäts- und Publishing-Konventionen sind aktiv und durch
Funktionstests abgesichert. CI, PIT, japicmp, Signaturen und PGP-Prüfung fehlen noch.

## Öffentliche Schnittstellen (Plugin-IDs)

| Plugin | Zweck |
|---|---|
| `kiln.java-cloud` | Cloud-Module: `--release 25`, JDK-25-Toolchain |
| `kiln.java-compat` | `kiln-api`, Bridges, Feature-Module: `--release 21` |
| `kiln.quality` | Spotless (Palantir), Error Prone + NullAway, `@NullMarked`-Pflicht, SpotBugs + FindSecBugs, JaCoCo ≥ 90 % |
| `kiln.protobuf` | Protobuf-/gRPC-Generierung; generierter Code ist von Format, Analyse, Javadoc und Coverage ausgenommen (ADR 0003) |
| `kiln.publishing` | `maven-publish` mit POM (Apache-2.0, SCM); kein Ziel, keine Zugangsdaten |
| `kiln.java-base` | intern; wird von den obigen Plugins mitgebracht |

Ein Modul wendet genau eines von `kiln.java-cloud`/`kiln.java-compat` plus `kiln.quality` an.

## Was die Konventionen erzwingen

- `-Xlint:all -Werror`; Javadoc mit `-Xdoclint:all` und `-Werror`
- Reproduzierbare Archive (keine Zeitstempel, feste Reihenfolge)
- NullAway als Fehler für `at.kilncloud`; zusätzlich verlangt `verifyNullMarked` (Teil von `check`) in jedem Paket mit Code eine `package-info.java` mit `@NullMarked` (JSpecify)
- JaCoCo: mindestens 90 % Line und 90 % Branch pro Modul. Produktionscode ohne jeden Test
  lässt den Build fehlschlagen (`requireCoverageData`), Gradle würde die Prüfung sonst überspringen
- SpotBugs wird nur übersprungen, wenn ein Modul ausschließlich `module-info`/`package-info` enthält
- Repositories sind zentral in `settings.gradle.kts`; Projekt-Repositories sind verboten

## Abhängigkeiten

Plugin-Artefakte und Werkzeugversionen stehen in `gradle/libs.versions.toml`. Jede Version ist
in `gradle/verification-metadata.xml` per SHA-256 gebunden.

## Tests

`./gradlew :build-logic:test` startet TestKit-Funktionstests: Jedes Gate wird gegen ein
Wegwerf-Projekt mit absichtlicher Verletzung geprüft und muss fehlschlagen. Die Fixtures nutzen
denselben Versionskatalog und dieselbe strikte Dependency Verification wie der echte Build; Artefakte
kommen von Maven Central und müssen den gepinnten SHA-256-Werten entsprechen. Ein Aufruf mit leerem
Gradle-User-Home funktioniert daher ohne vorherigen Modul-Build.

## Offene Punkte

- PIT, japicmp, Awaitility/Mockito/Testcontainers-Konventionen (Backlog)
- Signierte Artefakte (M8) und PGP-Signaturprüfung der Abhängigkeiten
- Kotlin-Formatierung der Build-Skripte
