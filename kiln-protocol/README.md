# kiln-protocol

`kiln-protocol` definiert die Wire-Verträge zwischen Master, Nodes und Bridges als Protobuf und
gRPC. Es enthält die generierten Nachrichten und Stubs sowie kleine, getestete Helfer für
Versionsabgleich und sicheres Logging.

## Status

**Grundgerüst – 15 %:** Protobuf-/gRPC-Build, der erste Vertrag (`NodeControl`: `Register` mit
Protokollversion und Join-Token, `Heartbeat`) und die Versionsregel sind vorhanden und getestet.
Weitere Verträge (Serverstart/-stopp, Terminal-Strom, RAM-Meldungen) folgen mit den
Arbeitspaketen von M1.

## Öffentliche Schnittstellen

- `kiln/protocol/v1/node_control.proto` (Quelle) und das generierte Paket `at.kilncloud.protocol.v1`
- `at.kilncloud.protocol.ProtocolVersions`: aktuelle Version und Kompatibilitätsregel
- `at.kilncloud.protocol.ProtocolLogging`: einzige erlaubte Art, Nachrichten zu loggen

## Regeln für Änderungen am Protokoll (ADR 0003)

- Feldnummern und Enum-Werte nie ändern oder wiederverwenden; entfernte Felder als `reserved`.
- Inkompatible Änderungen erhöhen die Major-Version und liegen in einem neuen Paket.
- Nachrichten mit Secrets (Join-Token) nie mit `toString()` loggen, sondern mit
  `ProtocolLogging.safeString`.

## Abhängigkeiten

- gRPC (`grpc-api`, `grpc-protobuf`, `grpc-stub`) und Protobuf-Java, exponiert als `api`
- Build: Protobuf-Gradle-Plugin; `protoc` und das gRPC-Plugin kommen als gepinnte Artefakte
  (SHA-256 für Linux, macOS und Windows)

## Konfiguration

- Kompiliert über `kiln.java-compat` (Release 21, damit Bridges es nutzen können), `kiln.protobuf`
  und `kiln.quality`
- Kein `module-info.java`; stabiler `Automatic-Module-Name: at.kilncloud.protocol` (ADR 0003)

## Offene Punkte

- Verträge für Serverstart/-stopp, Terminal-Strom und Node-Kapazität
- Sperre gegen versehentliches `toString()` auf Nachrichten mit Secrets
