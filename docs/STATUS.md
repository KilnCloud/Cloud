# KilnCloud – Projektstatus

Stand: 2026-10-14

## Aktueller Meilenstein

**M0 – Fundament, etwa 45 %**

Der Gradle-Multi-Modul-Build besitzt die ersten vier korrekt benannten Module. Java-Ziele,
JPMS-Kapselung und minimale ausführbare Artefakte sind eingerichtet. Die Convention Plugins in
`build-logic` erzwingen Spotless, Error Prone mit NullAway, JaCoCo (90 %), SpotBugs mit FindSecBugs,
Javadoc ohne Warnungen und strikte Dependency Verification (SHA-256). PIT, japicmp und die
PGP-Prüfung fehlen noch; M0 ist daher nicht funktionsfähig abgeschlossen.

## Modulübersicht

| Modul | Status | Fertigstellung | Funktioniert | Fehlt | Zuletzt geändert | Agent |
|---|---|---:|---|---|---|---|
| `build-logic` | in Arbeit | 60 % | Convention Plugins (Java 25/21, Quality, Publishing), Spotless, Error Prone + NullAway, JaCoCo 90 %, SpotBugs, Funktionstests | PIT, japicmp, Signierung, PGP-Prüfung | 2026-10-14 | Claude |
| `kiln-api` | Grundgerüst | 5 % | Java-21-JAR, JPMS-Export, API-Anker mit Test, Javadoc-/Sources-JAR, Maven-Publikation (ohne Ziel) | Fachliche Verträge, japicmp, API-Doku | 2026-10-14 | Codex |
| `kiln-protocol` | nicht begonnen | 0 % | – | Protobuf-Verträge und Build | – | – |
| `kiln-common` | Grundgerüst | 3 % | Gekapseltes Java-25-JPMS-Modul | Validierung, Crypto, Config, JSpecify und Tests | 2026-10-14 | Codex |
| `kiln-storage` | nicht begonnen | 0 % | – | Datei- und Datenbank-Backends, Migrationen und Tests | – | – |
| `kiln-master` | Grundgerüst | 2 % | Ausführbares Java-25-Modul und Einstiegspunkt-Test | Gesamte M1+-Funktionalität | 2026-10-14 | Codex |
| `kiln-node` | Grundgerüst | 2 % | Ausführbares Java-25-Modul und Einstiegspunkt-Test | Gesamte M1+-Funktionalität | 2026-10-14 | Codex |
| `kiln-memory` | nicht begonnen | 0 % | – | Budgets, cgroups v2 und Überbuchungsschutz | – | – |
| `kiln-launcher` | nicht begonnen | 0 % | – | Signaturprüfung und Prozessstart | – | – |
| `kiln-terminal` | nicht begonnen | 0 % | – | Server und CLI-Client | – | – |
| `kiln-setup` | nicht begonnen | 0 % | – | Easy Setup, Zertifikate, Join-Tokens und Datenbank | – | – |
| `kiln-versions` | nicht begonnen | 0 % | – | Generator, Schema und `download.json` | – | – |
| `kiln-i18n` | nicht begonnen | 0 % | – | Sprachdateien, Locale und Fallbacks | – | – |
| `kiln-bridge-paper` | nicht begonnen | 0 % | – | Paper-Bridge | – | – |
| `kiln-bridge-velocity` | nicht begonnen | 0 % | – | Velocity-Bridge und Proxy-Transfer | – | – |
| `kiln-bridge-bungee` | nicht begonnen | 0 % | – | BungeeCord-Bridge | – | – |
| `kiln-module-npc` | nicht begonnen | 0 % | – | NPC-Modul | – | – |
| `kiln-module-signs` | nicht begonnen | 0 % | – | Schilder-Modul | – | – |
| `kiln-module-tablist` | nicht begonnen | 0 % | – | Tablist-Modul | – | – |
| `kiln-module-motd` | nicht begonnen | 0 % | – | MOTD-Modul | – | – |
| `kiln-module-maintenance` | nicht begonnen | 0 % | – | Wartungsmodul | – | – |
| `kiln-module-font` | nicht begonnen | 0 % | – | FontConverter-Modul und Referenzübernahme | – | – |
| `kiln-web` | nicht begonnen | 0 % | – | Gesamtes Webinterface | – | – |

## Gerade in Arbeit

| Agent | Roadmap-Eintrag | Bereich | Branch | Stand |
|---|---|---|---|---|
| Claude | CI-Pipeline (GitHub Actions) mit strikter Dependency Verification | CI | `claude/feat/ci-pipeline` | Workflow und Cache-Unabhängigkeit umgesetzt; Lauf auf GitHub steht aus |

## Bekannte Probleme und Risiken

- PIT (Mutationstests) und japicmp werden noch nicht erzwungen; ein grüner Build ist kein
  vollständiger Qualitätsnachweis. `@NullMarked` je Paket wird nur über NullAway-`AnnotatedPackages` abgedeckt.
- Gradle Dependency Verification läuft strikt mit SHA-256-Metadaten; PGP-Signaturprüfung ist noch
  nicht aktiviert.
- Die CI-Pipeline (`.github/workflows/build.yml`) ist angelegt, ihr erster Lauf auf GitHub ist noch nicht bestätigt.
- `docs/reference/FontConverter.java` fehlt im Repository. Die spätere M7-Implementierung darf
  ohne diese Wahrheitsquelle nicht begonnen werden.
- `gradlew clean` kann lokal scheitern, solange ein fremder Windows-Prozess
  `build/reports/problems` geöffnet hält. Der normale Build ist davon nicht betroffen.
- Gradle 9.6.0 meldet unter Java 25 einen Native-Access-Hinweis aus seiner eingebetteten
  `native-platform`-Bibliothek. Projektcode verursacht diese Warnung nicht.

## Nächste Schritte

1. Ersten GitHub-Actions-Lauf prüfen und die Pipeline als Pflicht-Check für `main` einrichten (Maintainer).
2. `kiln-common` mit validierten Werttypen und vollständigen Tests beginnen (Codex).
3. PIT und japicmp ergänzen; PGP-Signaturprüfung der Abhängigkeiten.
4. `@NullMarked`-Regel und Test-Bibliotheken (AssertJ, Mockito, Awaitility) in die Konventionen aufnehmen.
5. Fehlende Referenz `docs/reference/FontConverter.java` vor M7 bereitstellen.
