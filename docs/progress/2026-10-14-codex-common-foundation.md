# Session: kiln-common Grundgerüst

## Aufgabe und Notion-Eintrag

- Aufgabe: `kiln-common Grundgerüst implementieren`
- Meilenstein: M0 – Fundament, Bereich `kiln-common`
- Agent: Codex
- Branch: `codex/feat/common-foundation`, Basis `origin/main` bei `6109548`
- Notion: <https://app.notion.com/p/3f9c33850d56817e904ee1856176cb0d>

## Erledigt

- `ServerName` und `TemplateName` als unveränderliche Records mit validierendem
  Kompakt-Konstruktor umgesetzt. Beide begrenzen die Länge vor einer linearen ASCII-Allowlist.
- `SecurePathResolver.resolveExisting` prüft relative Eingaben normalisiert und nach
  `toRealPath()` erneut mit `startsWith`, sodass Traversal und Symlink-Ausbrüche abgelehnt werden.
- `SecureTokens` erzeugt 32 bis 1024 Zufallsbytes ausschließlich mit `SecureRandom` und vergleicht
  Bytefolgen über `MessageDigest.isEqual`.
- Die Pakete `validation`, `filesystem` und `security` sind mit JSpecify `@NullMarked` markiert und
  als schmale Infrastrukturpakete per JPMS exportiert.
- ADR 0002 dokumentiert, warum diese Exporte keine fachliche Kommunikation und keine öffentliche
  Drittentwickler-API darstellen.
- `kiln-common/README.md` beschreibt Status, Schnittstellen, Abhängigkeiten und offene Punkte.
- 25 Tests decken Normal-, Grenz-, Fehler- und Sicherheitsfälle ab. Auf dem lokalen Windows-System
  liefen 24 erfolgreich; der Symlink-Integrationstest wurde nur wegen fehlendem Betriebssystemrecht
  per JUnit-Assumption übersprungen und läuft auf symlink-fähigen Systemen vollständig.

Betroffene Dateien liegen in `kiln-common/src/main`, `kiln-common/src/test`,
`kiln-common/README.md`, `docs/adr/0002-kiln-common-module-boundaries.md`, `docs/STATUS.md` und
diesem Session-Eintrag. Gemeinsame Build-Dateien wurden nicht geändert.

## Prüfungen

- Baseline vor Änderungen: `:kiln-common:build --dependency-verification strict` erfolgreich
- `:kiln-common:spotlessApply` ausgeführt
- `:kiln-common:build :kiln-common:spotbugsMain :kiln-common:spotbugsTest
  --dependency-verification strict --warning-mode all` erfolgreich
- JaCoCo: 96 % Line Coverage, 95 % Branch Coverage auf Modulebene
- Error Prone/NullAway: ohne Befund
- Javadoc mit Doclint/Werror: ohne Befund
- SpotBugs/FindSecBugs: ohne Befund
- Unabhängiges Security-Review: noch ausstehend

## Offen

- Der verpflichtende Security-Review in einem getrennten Codex-Thread steht noch aus.
- Eine sichere Konfigurationsbasis und TOCTOU-sichere konkrete Dateioperationen bleiben getrennte
  Folgearbeiten; der Resolver dokumentiert die Grenze seiner Zeitpunktprüfung ausdrücklich.
- Der Branch bleibt lokal und wird gemäß Maintainer-Vorgabe nicht gepusht.
- `CHANGELOG.md` wurde nicht geändert, weil die neuen Bausteine noch kein nutzersichtbares
  Verhalten bereitstellen.

## Entscheidungen

- [ADR 0002](../adr/0002-kiln-common-module-boundaries.md) begrenzt die JPMS-Exporte auf
  zustandslose Infrastrukturprimitiven. Fachliche Verträge bleiben in `kiln-api`/`kiln-protocol`.
- Namen erlauben 1 bis 64 Zeichen aus `[A-Za-z0-9_-]`, wobei das erste Zeichen alphanumerisch ist.
- Token unter 32 Byte werden abgelehnt; 1024 Byte sind die defensive Allokationsobergrenze.
- Negative Null-Tests unterdrücken NullAway nur an der jeweiligen Testmethode und begründen die
  absichtliche Verletzung des statischen Vertrags direkt im Code.

## Probleme und Erkenntnisse

- Ein von der IDE gesperrtes Javadoc-Verzeichnis im ursprünglichen Checkout wurde nach exakter
  Pfadprüfung durch Entfernen ausschließlich von `kiln-common/build` behoben.
- Claude wechselte den gemeinsam geöffneten Checkout während der Arbeit auf seinen Branch. Die
  eigenen Änderungen waren in einem geprüften Stash gesichert und wurden danach in einen eigenen
  Managed Worktree verschoben; Claudes Dateien und Branch wurden nicht verändert.
- Windows verweigert diesem Prozess das Erstellen symbolischer Links. Der Sicherheitstest bleibt
  aktiv und wird nur bei fehlender Plattformfähigkeit abgebrochen, nicht deaktiviert.

## Commits

- `1980ec9 feat(common): add validated resource names`
- `6a7c02e feat(common): add secure path and token utilities`

## Nächster sinnvoller Schritt

Nach Security-Review und Maintainer-Review folgen als getrennte M0-Aufgaben die sichere
Konfigurationsbasis und konkrete, gegen TOCTOU abgesicherte Dateioperationen.
