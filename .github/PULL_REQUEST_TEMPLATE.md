## Was und warum

<!-- Kurz: Was ändert dieser PR, und warum? Verweis auf den Notion-Eintrag. -->

## Betroffene Module und gemeinsame Dateien

<!-- Modulnamen. Gemeinsame Dateien (build-logic, Katalog, settings, verification-metadata, .proto, kiln-api, AGENTS.md) ausdrücklich nennen. -->

## Definition of Done (AGENTS.md §17)

- [ ] `./gradlew build spotbugsMain spotbugsTest --dependency-verification strict` ist grün (am besten mit leerem Gradle-Home)
- [ ] Jede neue oder geänderte Methode hat einen eigenen Test und Javadoc
- [ ] Neue Texte liegen in Deutsch und Englisch vor
- [ ] Neue Abhängigkeiten sind begründet; Hashes stammen aus offiziellen Quellen und wurden gegengeprüft
- [ ] Security-Review durchgeführt, Befunde der Stufe „mittel“ oder höher sind behoben
- [ ] `docs/STATUS.md`, Modul-READMEs und Session-Eintrag in `docs/progress/` sind aktualisiert
- [ ] Bei nutzersichtbaren Änderungen: `CHANGELOG.md` (Unreleased) ergänzt
- [ ] Notion-Eintrag steht auf `Review`, Folgeaufgaben sind als Backlog angelegt

## Hinweise zum Mergen

- Bei gestapelten PRs vor dem Merge des unteren PRs das Ziel des oberen auf `main` umstellen.
- Bitte „Create a merge commit“ verwenden, nicht „Squash“.
