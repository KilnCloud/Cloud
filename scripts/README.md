# scripts

Setup- und Betriebsskripte für KilnCloud (AGENTS.md §3, §8). Jedes Skript ist unabhängig von der
Java-Codebasis und wird mit ShellCheck geprüft und mit bats getestet.

## Aufbau

| Pfad | Inhalt |
|---|---|
| `scripts/setup-database.sh` | Datenbank-Setup für MariaDB/MySQL und PostgreSQL (AGENTS.md §8.2) |
| `scripts/tests/` | bats-Tests, eine Datei pro Skript, z. B. `scripts/tests/setup-database.bats` |

Die Skripte erzeugen zur Laufzeit Dateien wie `config/database.json`; diese werden nie committet
(`.gitignore`).

## Regeln für Skripte

- Kopfkommentar mit Zweck, Voraussetzungen und Aufruf; Kommentare an jedem nicht offensichtlichen Schritt.
- `#!/usr/bin/env bash` und `set -euo pipefail`; idempotent (vor dem Überschreiben fragen).
- Geheimnisse (Passwörter) nie als Argument, in der Prozessliste, in der Shell-History oder in Logs:
  Übergabe per stdin oder Umgebungsvariable des Kindprozesses.
- Dateien mit Geheimnissen mit Rechten `600`.
- Zeilenenden sind LF (`.gitattributes`); ausführbar markieren mit
  `git update-index --chmod=+x scripts/<skript>.sh`.
- Prüfung vor jedem Commit: `shellcheck scripts/*.sh` ohne Befunde, `bats scripts/tests`.
