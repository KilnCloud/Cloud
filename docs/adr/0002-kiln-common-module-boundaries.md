# ADR 0002: Schmale Infrastruktur-Exporte aus `kiln-common`

Status: akzeptiert (2026-10-14)

## Kontext

`kiln-common` stellt Validierungs-, Dateisystem- und Sicherheitsprimitiven bereit, die mehrere
Cloud-Prozesse identisch verwenden müssen. Ohne JPMS-Exporte wären diese Primitiven für Master,
Node und spätere Cloud-Module nicht nutzbar. Gleichzeitig verbietet `AGENTS.md` direkte Zugriffe
auf Interna anderer Module und verlangt, dass fachliche Kommunikation ausschließlich über
`kiln-api` beziehungsweise `kiln-protocol` läuft.

## Entscheidung

1. `kiln-common` exportiert ausschließlich die Pakete `at.kilncloud.common.validation`,
   `at.kilncloud.common.filesystem` und `at.kilncloud.common.security`.
2. Diese Pakete enthalten zustandslose Infrastrukturprimitiven. Sie definieren weder fachliche
   Services noch Nachrichten, Events oder Prozessschnittstellen.
3. Alle Implementierungsdetails bleiben in nicht exportierten Paketen. Ein neuer Export benötigt
   eine erneute Architekturprüfung.
4. Die Exporte sind keine öffentliche Drittentwickler-API. Diese bleibt ausschließlich
   `kiln-api`; Kommunikation zwischen Prozessen bleibt ausschließlich `kiln-api` beziehungsweise
   `kiln-protocol` vorbehalten.
5. Jedes Paket ist mit JSpecify `@NullMarked` markiert. Utility-Klassen sind `final` und besitzen
   einen privaten Konstruktor.

## Folgen

- Cloud-Module können dieselbe sicherheitskritische Validierung verwenden, ohne Implementierungen
  zu duplizieren oder nicht exportierte Pakete zu öffnen.
- Die Modulgrenze bleibt explizit: Nur drei kleine Infrastrukturpakete sind sichtbar.
- Änderungen an diesen Typen müssen alle internen Verbraucher berücksichtigen, unterliegen aber
  nicht der SemVer-/japicmp-Garantie von `kiln-api`.

## Nachtrag (2026-10-14): viertes Paket `at.kilncloud.common.config`

Die Konfigurationsbasis `SecureConfig` (strikt konfigurierter JSON-Reader/-Writer) wird als
`at.kilncloud.common.config` exportiert, weil Master, Node und weitere Cloud-Module ihre
Konfigurationsdateien identisch und sicher laden müssen. Die Signaturen enthalten keine
Jackson-Typen; Jackson bleibt ein interner, nicht transitiver Abhängigkeitsdetail
(`requires com.fasterxml.jackson.databind` ohne `transitive`). Module, deren Konfigurationstypen
auf dem Modulpfad geladen werden, öffnen deren Paket für `com.fasterxml.jackson.databind`.
`SecureFiles` (TOCTOU-sicheres Lesen und atomares Schreiben) ergänzt das bestehende Paket
`at.kilncloud.common.filesystem`. Die übrigen Regeln dieses ADR gelten unverändert.
