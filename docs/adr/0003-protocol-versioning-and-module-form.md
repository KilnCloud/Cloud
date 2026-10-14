# ADR 0003: Protokollversionierung und Bauform von `kiln-protocol`

Status: akzeptiert (2026-10-14)

## Kontext

Master, Nodes und Bridges tauschen Nachrichten über gRPC aus. Nodes und Master werden von
Betreibern unabhängig voneinander aktualisiert, also müssen unterschiedliche Builds miteinander
sprechen oder sich sauber ablehnen. Das Modul wird von Cloud-Modulen (Java 25, JPMS) und von
Bridges (Java 21, ohne JPMS) genutzt.

## Entscheidungen

1. **Versionsfeld im Handshake.** Jede Verbindung beginnt mit `Register`; beide Seiten melden ihre
   `ProtocolVersion` (major, minor). Der Server lehnt eine abweichende Major-Version mit
   `REGISTER_STATUS_VERSION_INCOMPATIBLE` ab. Major `0` gilt nie als kompatibel, denn so sieht ein
   fehlendes Feld aus. Minor-Versionen sind kompatibel (nur zusätzliche, optionale Felder).
2. **`reserved`-Regeln.** Feldnummern werden nie wiederverwendet oder geändert. Entfernte Felder
   werden mit Namen als `reserved` eingetragen. Ein Test (`WireContractTest`) schreibt die
   Feldnummern und Enum-Werte fest, damit eine versehentliche Änderung den Build bricht.
3. **Inkompatible Änderungen** erhöhen die Major-Version und liegen in einem neuen Paket (`v2`).
4. **Release 21 (`kiln.java-compat`).** Bridges laufen auf Minecraft 1.21.1 und damit auf Java 21.
5. **Kein `module-info.java`.** gRPC und Protobuf sind automatische Module, der generierte Code
   braucht zusätzlich Guava, und `-Xlint:all -Werror` verbietet `requires transitive` auf
   automatische Module. Stattdessen setzt das JAR `Automatic-Module-Name: at.kilncloud.protocol`,
   sodass Cloud-Module das Modul stabil per `requires` einbinden können.
6. **Generierter Code** wird weder formatiert, analysiert, dokumentiert noch für Coverage
   gezählt (AGENTS.md §13). Die Ausnahmen leitet `kiln.protobuf` aus den generierten Quellen ab,
   handgeschriebener Code im Modul bleibt voll abgedeckt.
7. **Geheimnisse in Nachrichten.** Der Join-Token ist als `debug_redact` markiert, aber Protobuf-Javas
   `toString()` beachtet das nicht. Nachrichten dürfen deshalb nur über
   `ProtocolLogging.safeString` geloggt werden.

## Folgen

- Alte Nodes werden von neuen Mastern erkannt und höflich abgelehnt statt unvorhersehbar zu scheitern.
- Neue Hashes für `protoc` und das gRPC-Plugin gibt es pro Betriebssystem; alle unterstützten
  Plattformen (Linux, macOS, Windows) stehen in `gradle/verification-metadata.xml`.
- Eine spätere Sperre gegen `toString()` auf Nachrichten (Regel oder Lint) bleibt eine Folgeaufgabe.
