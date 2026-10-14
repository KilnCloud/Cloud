# KilnCloud – Projektstatus

Stand: 2026-10-15

## Aktueller Meilenstein

**M0 – Fundament, funktionsfähig (PIT und japicmp zurückgestellt); M1 – Kern lokal, etwa 35 %**

Der Gradle-Multi-Modul-Build besitzt sechs Module (Api, Common, Protocol, Setup, Master, Node). Java-Ziele,
JPMS-Kapselung und minimale ausführbare Artefakte sind eingerichtet. Die Convention Plugins in
`build-logic` erzwingen Spotless, Error Prone mit NullAway, JaCoCo (90 %), SpotBugs mit FindSecBugs,
Javadoc ohne Warnungen und strikte Dependency Verification (SHA-256). PIT, japicmp und die
PGP-Prüfung fehlen noch (Backlog bzw. zurückgestellt). Die GitHub-Actions-CI läuft grün; das Repository ist öffentlich.
Von M1 sind `kiln-protocol` (1a), `kiln-setup` (2b) und der Master-Kern (3a) umgesetzt; es folgen der
Node-Kern (3b), `kiln-memory` (2a), der Prozessstart (4), das Terminal (5) und Integrationstests (6).

## Modulübersicht

| Modul | Status | Fertigstellung | Funktioniert | Fehlt | Zuletzt geändert | Agent |
|---|---|---:|---|---|---|---|
| `build-logic` | in Arbeit | 60 % | Convention Plugins (Java 25/21, Quality, Publishing), Spotless, Error Prone + NullAway, JaCoCo 90 %, SpotBugs, Funktionstests | PIT, japicmp, Signierung, PGP-Prüfung | 2026-10-14 | Claude |
| `kiln-api` | Grundgerüst | 5 % | Java-21-JAR, JPMS-Export, API-Anker mit Test, Javadoc-/Sources-JAR, Maven-Publikation (ohne Ziel) | Fachliche Verträge, japicmp, API-Doku | 2026-10-14 | Codex |
| `kiln-protocol` | Grundgerüst | 15 % | Protobuf-/gRPC-Build, Vertrag `NodeControl` (Register mit Version und Join-Token, Heartbeat), Versionsregel, sicheres Logging, Wire-Contract-Tests | Weitere Verträge (Serverstart/-stopp, Terminal, Kapazität) | 2026-10-14 | Claude |
| `kiln-common` | in Arbeit | 35 % | Validierte Server-/Templatenamen, sichere Auflösung existierender Pfade, sichere Token-Erzeugung und konstanter Vergleich; JPMS/JSpecify; 96 % Line-/95 % Branch-Coverage; TOCTOU-sicheres Lesen/Schreiben, strikte JSON-Konfiguration | Weitere gemeinsame Utilities nach Bedarf von M1 | 2026-10-14 | Codex, Claude |
| `kiln-storage` | nicht begonnen | 0 % | – | Datei- und Datenbank-Backends, Migrationen und Tests | – | – |
| `kiln-master` | Grundgerüst | 20 % | gRPC-Server mit mTLS, Node-Registrierung (Zertifikats-Identität + Join-Token), Heartbeat mit Replay-Schutz, Node-Registry, append-only Audit-Log, strikte Konfiguration; echte mTLS-Integrationstests | Scheduling, Templates, Prozessverwaltung, Terminal, REST/WS-API, Persistenz (M4) | 2026-10-14 | Claude |
| `kiln-node` | Grundgerüst | 2 % | Ausführbares Java-25-Modul und Einstiegspunkt-Test | Node-Kern (3b), Prozessstart (4), RAM-Anbindung | 2026-10-14 | Codex |
| `kiln-memory` | nicht begonnen | 0 % | – | Budgets, cgroups v2 und Überbuchungsschutz | – | – |
| `kiln-launcher` | nicht begonnen | 0 % | – | Signaturprüfung und Prozessstart | – | – |
| `kiln-terminal` | nicht begonnen | 0 % | – | Server und CLI-Client | – | – |
| `kiln-setup` | Grundgerüst | 20 % | CA, Client-/Server-Zertifikate (EC P-256), PEM-Dateien mit Rechten 600, TLS-1.3-Kontexte (mTLS), Rotationsprüfung, einmalige Join-Tokens; echte Handshake-Tests | Interaktiver Easy-Setup-Ablauf, Datenbank-Setup, persistente Token-Ablage (M4) | 2026-10-14 | Claude |
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
| `kiln-module-font` | nicht begonnen | 0 % | Referenz `docs/reference/FontConverter.java` (PR #16) | FontConverter-Modul (M7): Übernahme mit identischem Verhalten, Apostroph-Fix, Tests, Live-Vorschau | 2026-10-15 | – |
| `kiln-web` | nicht begonnen | 0 % | – | Gesamtes Webinterface | – | – |

## Gerade in Arbeit

| Agent | Roadmap-Eintrag | Bereich | Branch | Stand |
|---|---|---|---|---|
| Claude | kiln-node: Node-Kern (M1, Paket 3b) und Entwickler-Werkzeug für Zertifikate und Tokens | kiln-node, kiln-setup | – | In Planung |

## Bekannte Probleme und Risiken

- PIT (Mutationstests) und japicmp werden noch nicht erzwungen; ein grüner Build ist kein
  vollständiger Qualitätsnachweis.
- Gradle Dependency Verification läuft strikt mit SHA-256-Metadaten; PGP-Signaturprüfung ist noch
  nicht aktiviert.
- Die CI läuft grün; ein Branch-Schutz für `main` mit dem Pflicht-Check `build` ist noch nicht eingerichtet (Maintainer).
- `docs/reference/FontConverter.java` ist die Wahrheitsquelle für M7 (PR #16). Die Übernahme muss das
  Verhalten identisch halten und die bekannte Schwäche beheben: Ein Apostroph in einem unquotierten
  Tag-Argument (z. B. in einer URL) schaltet den Quote-Modus ein und lässt den Rest des Textes
  unkonvertiert.
- `gradlew clean` kann lokal scheitern, solange ein fremder Windows-Prozess
  `build/reports/problems` geöffnet hält. Der normale Build ist davon nicht betroffen.
- Gradle 9.6.0 meldet unter Java 25 einen Native-Access-Hinweis aus seiner eingebetteten
  `native-platform`-Bibliothek. Projektcode verursacht diese Warnung nicht.

## Nächste Schritte

1. Branch-Schutz für `main` mit Pflicht-Check `build`, Secret Scanning und CodeQL einrichten (Maintainer).
2. M1: Node-Kern (3b) mit einem Entwickler-Werkzeug für CA, Zertifikate und Join-Tokens, damit Master und
   Node lokal ausprobiert werden können.
3. M1: `kiln-memory` (2a), Prozessstart (4), Terminal (5) und Integrationstests (6).
4. PIT und japicmp ergänzen; Test-Bibliotheken (AssertJ, Mockito, Awaitility) aufnehmen, sobald ein Modul sie braucht.
5. Release-Workflow (Tag, SBOM, Prüfsummen, Provenance) nach dem Abschluss von M1.
