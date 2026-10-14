# Session: Sicherheitsrichtlinie und GitHub-Vorlagen

## Aufgabe und Notion-Eintrag

- Aufgabe: `SECURITY.md, CODEOWNERS und PR-Vorlage anlegen` (Dokumentation, M0)
- Agent: Claude · Branch `claude/docs/security-policy`
- Notion: <https://app.notion.com/p/3f9c33850d5681f3b889cd52ac9f19f3>

## Erledigt

- `SECURITY.md` (zweisprachig): Meldeweg über das private Melden von GitHub, Ablauf als Zielwerte,
  Geltungsbereich, Verweis auf AGENTS.md §7. Es wurde bewusst keine E-Mail-Adresse erfunden.
- `.github/CODEOWNERS`: Maintainer für alles, ausdrücklich für gemeinsame und sicherheitskritische Dateien.
- `.github/PULL_REQUEST_TEMPLATE.md`: Definition of Done aus AGENTS.md §17 und Merge-Hinweise
  (Ziel gestapelter PRs umstellen, „Create a merge commit“).
- Repository-Einstellung „Automatically delete head branches“ aktiviert (auf Wunsch des
  Maintainers), damit gestapelte PRs nicht mehr in einen bereits gemergten Branch laufen.
- Notion-Backlog: Repository öffentlich schalten und Branch-Schutz, Dependabot für Actions,
  Release-Workflow.

## Offen

- Der Maintainer muss „Private vulnerability reporting“ in den Repository-Einstellungen aktivieren,
  sonst funktioniert der Meldelink in `SECURITY.md` nicht.
- Reaktionszeiten in `SECURITY.md` sind Zielwerte und sollten vom Maintainer bestätigt werden.

## Lernpunkt

PR #6 wurde in den Branch von #5 gemergt, nachdem #5 diesen Branch bereits nach `main` gebracht
hatte; der Inhalt kam erst über #7 nach `main`. Künftig Ziel gestapelter PRs vor dem Merge umstellen.
