# Session: @NullMarked-Pflicht als Build-Regel

## Aufgabe und Notion-Eintrag

- Aufgabe: `@NullMarked-Pflicht je Paket als Build-Regel` (M0, build-logic)
- Agent: Claude · Branch `claude/feat/nullmarked-rule` (Worktree `Cloud-claude`)
- Notion: <https://app.notion.com/p/3f9c33850d5681e383b1e68eba0fc371>

## Erledigt

- `NullMarkedPackages` (Kotlin, `build-logic`): findet Pakete mit Code ohne `package-info.java`
  mit `@NullMarked` (JSpecify-Import erforderlich, Kommentare zählen nicht). 8 Unit-Tests.
- Task `verifyNullMarked` in `kiln.quality`, Teil von `check`; zwei Funktionstests (Verstoß schlägt
  fehl, `check` enthält die Prüfung); das saubere Fixture enthält jetzt eine markierte `package-info`.
- `kiln-api`, `kiln-master` und `kiln-node`: Pakete mit `@NullMarked` markiert und
  `requires static org.jspecify;` in den Moduldeskriptoren. `kiln-common` hatte das schon (Codex).
- ADR 0001 (Nachtrag), `build-logic/README.md` und `docs/STATUS.md` aktualisiert.
- Notion-Status von Codex' beiden gemergten Einträgen (Gradle-Grundgerüst, kiln-common) auf
  `Erledigt` gesetzt (auf Wunsch des Maintainers).

## Prüfung

`./gradlew build spotbugsMain spotbugsTest --dependency-verification strict` grün
(Gradle-User-Home leer, wie in der CI). build-logic-Tests: 29, 0 Fehler.

## Offen

- PIT, japicmp, PGP (bewusst zurückgestellt), Test-Bibliotheken: siehe Notion-Backlog.
- Windows hält Build-Verzeichnisse gelegentlich gesperrt; `clean` kann deshalb scheitern, die eigenen
  `build/`-Ordner löschen und ohne `clean` neu bauen hilft.

## Nächster sinnvoller Schritt

M0 abschließen (Maintainer-Bestätigung), dann M1-Plan bestätigen lassen.
