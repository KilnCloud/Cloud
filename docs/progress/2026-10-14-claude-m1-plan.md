# Session: Entwurf M1-Plan

## Aufgabe

Während `kiln-common` bei Codex in Arbeit ist: M1 (Kern lokal) als Plan vorbereiten, ohne Code. M1 darf
laut AGENTS.md §4.5 erst beginnen, wenn M0 funktionsfähig ist.

## Erledigt

- `docs/plans/m1-kern-lokal.md`: Arbeitspakete mit Abhängigkeiten, Aufgabenteilung nach Modulen,
  neue Abhängigkeiten, Sicherheitsanforderungen, offene Entscheidungen (ADR) und Definition of Done.
- PGP-Prüfung der Abhängigkeiten geprüft und bewusst nicht aktiviert (Notion-Kommentar im Eintrag).

## Offen

- Bestätigung des Plans durch den Maintainer und Beantwortung der fünf Entscheidungsfragen.
- Notion: Für M1-Einträge fehlen die Bereichswerte `kiln-master`, `kiln-node`, `kiln-memory`,
  `kiln-protocol`, `kiln-terminal`; die Schema-Erweiterung braucht die Freigabe des Maintainers.

## Nächster sinnvoller Schritt

Plan bestätigen, danach `kiln-protocol` beginnen, sobald M0 abgeschlossen ist.
