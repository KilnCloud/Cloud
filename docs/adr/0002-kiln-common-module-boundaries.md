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
