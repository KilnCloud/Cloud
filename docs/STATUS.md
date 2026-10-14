# KilnCloud – Projektstatus

Stand: 2026-10-14

## Aktueller Meilenstein

**M0 – Fundament, etwa 25 %**

Der Gradle-Multi-Modul-Build besitzt die ersten vier korrekt benannten Module. Java-Ziele,
JPMS-Kapselung und minimale ausführbare Artefakte sind eingerichtet. Die verpflichtenden
Convention Plugins, die übrigen Qualitätswerkzeuge und CI fehlen noch. SpotBugs mit FindSecBugs,
strikte Dependency Verification und die Gradle-Wrapper-Prüfsumme sind als erste verpflichtende
Sicherheits-Gates aktiv; M0 ist dennoch nicht funktionsfähig abgeschlossen.

## Modulübersicht

| Modul | Status | Fertigstellung | Funktioniert | Fehlt | Zuletzt geändert | Agent |
|---|---|---:|---|---|---|---|
| `build-logic` | nicht begonnen | 0 % | – | Convention Plugins und sämtliche Qualitätswerkzeuge | – | – |
| `kiln-api` | Grundgerüst | 5 % | Java-21-JAR, JPMS-Export, API-Anker, Javadoc-/Sources-JAR | Fachliche Verträge, JSpecify, japicmp, API-Doku und Tests | 2026-10-14 | Codex |
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
| Codex | Gradle-Grundgerüst und Modulstruktur vervollständigen | Projektstruktur | `codex/feat/gradle-foundation` | Implementiert; Abschlussprüfungen und Security-Review laufen |

## Bekannte Probleme und Risiken

- M0 erzwingt noch keine JaCoCo-, Error-Prone-, NullAway-, Spotless- oder PIT-Prüfungen. Ein grüner
  Build ist daher noch kein vollständiger Qualitätsnachweis. SpotBugs und FindSecBugs sind aktiv.
- Gradle Dependency Verification läuft strikt mit SHA-256-Metadaten; PGP-Signaturprüfung ist noch
  nicht aktiviert.
- Eine CI-Pipeline fehlt.
- `docs/reference/FontConverter.java` fehlt im Repository. Die spätere M7-Implementierung darf
  ohne diese Wahrheitsquelle nicht begonnen werden.
- `gradlew clean` kann lokal scheitern, solange ein fremder Windows-Prozess
  `build/reports/problems` geöffnet hält. Der normale Build ist davon nicht betroffen.
- Gradle 9.6.0 meldet unter Java 25 einen Native-Access-Hinweis aus seiner eingebetteten
  `native-platform`-Bibliothek. Projektcode verursacht diese Warnung nicht.

## Nächste Schritte

1. Security-Fixes in einem unabhängigen Nachreview bestätigen und das Grundgerüst übergeben.
2. Convention Plugins und verpflichtende Qualitätswerkzeuge in `build-logic` einrichten.
3. CI-Pipeline für vollständige Builds und Sicherheitsprüfungen ergänzen.
4. `kiln-common` mit validierten Werttypen und vollständigen Tests beginnen.
5. Fehlende Referenz `docs/reference/FontConverter.java` vor M7 bereitstellen.
