# Sicherheitsrichtlinie / Security Policy

KilnCloud läuft auf Servern von Betreibern und steuert dort Prozesse, Dateien, Datenbanken und
Netzwerk. Eine Schwachstelle betrifft deshalb nicht nur dieses Projekt, sondern alle Betreiber
und ihre Spieler. Wir nehmen Meldungen ernst.

*KilnCloud runs on operators' servers and controls processes, files, databases and networking.
A vulnerability affects every operator and their players, so we take reports seriously.*

## Schwachstellen melden / Reporting a vulnerability

**Bitte melde Sicherheitslücken nicht über öffentliche Issues, Pull Requests oder Diskussionen.**
*Please do not report security issues in public issues, pull requests or discussions.*

Nutze stattdessen das private Melden von GitHub:
*Use GitHub's private vulnerability reporting instead:*

<https://github.com/KilnCloud/Cloud/security/advisories/new>

Hilfreich in einer Meldung / *Helpful in a report*:

- betroffene Komponente und Version bzw. Commit / *affected component and version or commit*
- Schritte zur Reproduktion oder ein Proof of Concept / *steps to reproduce or a proof of concept*
- erwartete Auswirkung (z. B. Codeausführung, Rechteausweitung, Datenabfluss) / *expected impact*
- Hinweise auf mögliche Gegenmaßnahmen / *suggested mitigations, if any*

Bitte keine echten Zugangsdaten, Tokens oder personenbezogenen Daten in die Meldung packen.
*Please do not include real credentials, tokens or personal data in a report.*

## Ablauf / What to expect

Wir streben an, eine Meldung innerhalb von sieben Tagen zu bestätigen und gemeinsam mit dir einen
Zeitplan für Korrektur und Veröffentlichung abzustimmen. Das sind Zielwerte, keine Zusagen; das
Projekt wird derzeit von wenigen Personen betreut. Wir veröffentlichen eine Korrektur zuerst und
nennen die Meldenden auf Wunsch in der Danksagung.

*We aim to acknowledge a report within seven days and to agree a fix and disclosure timeline with
you. These are targets, not guarantees. We publish a fix first and credit reporters on request.*

## Unterstützte Versionen / Supported versions

KilnCloud befindet sich in der Entwicklung (Meilenstein M1 von M8) und hat noch keine
veröffentlichte Version. Sicherheitskorrekturen erfolgen auf dem Branch `main`. Sobald es Releases
gibt, wird diese Tabelle die unterstützten Versionen auflisten.

*KilnCloud is under development and has no published release yet. Fixes land on `main`. Once
releases exist, this section will list the supported versions.*

## Geltungsbereich / Scope

Im Geltungsbereich sind alle Module dieses Repositories, insbesondere Master, Node, Launcher,
Terminal, Bridges, Webinterface und die Public API sowie die Build- und Release-Kette
(Abhängigkeiten, GitHub Actions, Dependency Verification).

Nicht im Geltungsbereich: Schwachstellen in Minecraft, Paper, Velocity, BungeeCord oder anderen
Drittsoftware-Produkten (bitte direkt dort melden) sowie Fehlkonfigurationen auf Betreiberseite,
etwa ein öffentlich erreichbarer Backend-Server ohne Proxy-Forwarding.

## Sicherheitsgrundsätze des Projekts

Die verbindlichen Regeln stehen in [`AGENTS.md`](AGENTS.md), Abschnitt 7: deny by default, Least
Privilege, mTLS zwischen Master und Node, gehashte Tokens, sichere Eingabevalidierung und eine
strikte Prüfung aller Abhängigkeiten per SHA-256.
