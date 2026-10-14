# Plan M1 – Kern lokal (Entwurf zur Bestätigung)

Stand: 2026-10-14 · Autor: Claude · Status: **Entwurf, wartet auf Bestätigung des Maintainers**
(AGENTS.md §5.3: Bei größeren Aufgaben zuerst Plan, dann Bestätigung.)

M1 beginnt erst, wenn M0 funktionsfähig ist (§4.5). Voraussetzung ist der Abschluss von
`kiln-common` und der Backlog-Punkte, die `kiln-common` betreffen. Dieser Plan ändert keinen Code.

## Ziel (laut AGENTS.md)

`kiln-master` und `kiln-node` laufen auf einer Maschine, sprechen per mTLS (TLS 1.3) über gRPC
miteinander, `kiln-memory` verwaltet das RAM-Budget, der Node startet und überwacht
Serverprozesse, und das zentrale Terminal ist als CLI nutzbar.

**Nicht Teil von M1:** Templates und `download.json` (M2), Proxys (M3), Datenbank und
Easy-Setup (M4), Webinterface (M5). Zertifikate werden in M1 von einem kleinen, klar
abgegrenzten Bootstrap-Werkzeug erzeugt, das später vom Setup-Manager (M4) abgelöst wird.

## Arbeitspakete

Die Reihenfolge ist die Abhängigkeitsreihenfolge; Pakete mit gleicher Nummer sind parallel möglich,
weil sie in getrennten Modulen liegen.

| Nr. | Paket | Modul | Abhängig von | Kurzbeschreibung |
|---|---|---|---|---|
| 1a | Protokollverträge | `kiln-protocol` | M0 | `.proto`-Dateien (Node-Registrierung, Heartbeat, Serverstart/-stopp, Terminal-Strom), Protobuf-Build, Versionierungsregel für das Protokoll |
| 1b | Validierte Werttypen und Krypto-Utilities | `kiln-common` | M0 | Server-/Node-Namen, Pfad-Sicherheit, Token-Erzeugung, konstante Vergleiche (läuft bereits bei Codex) |
| 2a | RAM-Manager | `kiln-memory` | 1b | Budgets pro Node, Überbuchungsschutz, cgroups v2 unter Linux, reine Buchhaltung ohne cgroups auf anderen Systemen |
| 2b | Zertifikats-Bootstrap | `kiln-common` oder eigenes Hilfsmodul | 1b | CA, Master- und Node-Zertifikate, Rotation, einmalige kurzlebige Join-Tokens |
| 3a | Master-Kern | `kiln-master` | 1a, 2b | gRPC-Server mit mTLS, Node-Registry, Join-Token-Prüfung, Heartbeat-Überwachung, Audit-Log (append-only, Datei) |
| 3b | Node-Kern | `kiln-node` | 1a, 2a, 2b | gRPC-Client mit mTLS, Registrierung, Heartbeat, RAM-Meldung |
| 4 | Prozessstart | `kiln-node` | 3b | `ProcessBuilder` nur mit Argumentliste, eigener unprivilegierter Nutzer, Ressourcenlimits, Start/Stopp/Neustart, Logstrom |
| 5 | Terminal (CLI) | `kiln-terminal` | 3a, 4 | zentrales Terminal: Befehle über den Master, RBAC-fähige Berechtigungsprüfung (Rollenmodell als Platzhalter), Ratenbegrenzung, Audit |
| 6 | Integrationstests | `kiln-master`, `kiln-node` | 3a, 3b, 4 | Master ↔ Node in Testcontainern bzw. lokal, Prozessstart mit Fake-Server, Ausfall- und Fehlerpfade |

Vorschlag zur Aufgabenteilung (getrennte Module, keine Kollision):

- **Codex:** 1b (`kiln-common`), danach 2a (`kiln-memory`).
- **Claude:** 1a (`kiln-protocol`), danach 2b und 3a (`kiln-master`).
- 3b und 4 (`kiln-node`) nach 1a, an wen auch immer zuerst frei wird.
- `kiln-protocol`, `libs.versions.toml`, `settings.gradle.kts` und `verification-metadata.xml` sind
  gemeinsame Dateien (§6): kleine, eigene Commits und Nennung im Session-Eintrag.

## Neue Abhängigkeiten (jeweils mit Begründung, CVE-Prüfung, Hashes aus offiziellen Quellen)

- gRPC-Java und Protobuf (festgelegt in AGENTS.md §2) samt Netty-Transport. Das Protobuf-Gradle-Plugin
  braucht eigene Verification-Einträge.
- Für Zertifikate und TLS: bevorzugt JDK-Bordmittel; BouncyCastle nur, wenn die Zertifikatserzeugung
  ohne sie nicht sauber möglich ist (eigene Entscheidung als ADR).
- Logback und SLF4J (festgelegt) für Logging mit Maskierung von Tokens.

## Sicherheitsanforderungen (aus AGENTS.md §7)

- TLS 1.3 only, Client-Zertifikate Pflicht, Zertifikatsprüfung ohne Ausnahmen.
- Join-Tokens einmalig, kurzlebig, nur gehasht gespeichert, Vergleich in konstanter Zeit,
  `SecureRandom`.
- Prozesse nur über Argumentlisten; Pfade aus Eingaben mit `normalize()`/`toRealPath()` und
  `startsWith`; Gameserver nie als root.
- Secrets nie in Logs oder `toString()`; Klassen mit Secrets sind keine Records.
- Audit-Log für Registrierung, Start/Stopp, Terminal-Befehle; append-only.

## Entscheidungen, die vor Beginn getroffen werden müssen (ADR)

1. **Protokollversionierung:** Wie wird Rückwärtskompatibilität zwischen Master und Node geregelt
   (Feldnummern, `reserved`, Versionsfeld im Handshake)?
2. **Zertifikatserzeugung:** JDK-Bordmittel oder BouncyCastle?
3. **Plattformumfang M1:** Läuft M1 nur unter Linux (cgroups v2, eigener Systemnutzer), oder muss
   die lokale Entwicklung unter Windows funktionieren (RAM-Buchhaltung ohne cgroups,
   Prozessstart ohne Nutzerwechsel mit deutlicher Warnung)? Empfehlung: Linux als Zielplattform,
   Windows nur für Entwicklung mit abgeschalteten Durchsetzungsfunktionen und Warnung.
4. **Audit-Log-Speicher in M1:** Datei (append-only) bis zum Storage-Modul in M4?
5. **Rollenmodell für das Terminal:** Platzhalter-Rollen in M1, vollständiges RBAC in M5?

## Definition of Done für M1

- Master und Node starten lokal, registrieren sich per mTLS mit Join-Token und halten den Heartbeat.
- Der Node startet, überwacht und stoppt einen Testprozess innerhalb des RAM-Budgets; Überbuchung
  wird abgelehnt.
- Das CLI-Terminal führt Befehle über den Master aus; jede Aktion steht im Audit-Log.
- Alle Gates aus AGENTS.md §17 sind grün (Build, JaCoCo 90 %, SpotBugs, Security-Review),
  Integrationstests laufen in der CI.
- `docs/STATUS.md`, Modul-READMEs und ADRs sind aktuell.
