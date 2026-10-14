# kiln-master

`kiln-master` ist die zentrale Steuerung von KilnCloud. Der Master nimmt Nodes per gegenseitig
authentifiziertem TLS 1.3 und Join-Token auf, überwacht ihre Heartbeats und protokolliert
sicherheitsrelevante Aktionen in einem append-only Audit-Log.

## Status

**Grundgerüst – 20 %:** gRPC-Server mit mTLS, Node-Registrierung mit Zertifikats-Identität und
Join-Token, Heartbeat mit Replay-Schutz, Node-Registry, Audit-Log und strikte Konfiguration sind
umgesetzt und getestet, auch über echte mTLS-Verbindungen. Scheduling, Templates, Prozessverwaltung,
Terminal und REST/WebSocket-API folgen in späteren Paketen und Meilensteinen.

## Start

```
kiln-master <master.json>
```

Beispielkonfiguration (alle Felder außer `allowUnrestrictedFilePermissions` sind Pflicht;
Dateinamen sind relativ zum Verzeichnis der Konfigurationsdatei):

```json
{
  "listenHost": "0.0.0.0",
  "port": 25570,
  "serverCertificate": "pki/master.crt",
  "serverKey": "pki/master.key",
  "authorityCertificate": "pki/ca.crt",
  "auditLog": "logs/audit.log",
  "heartbeatTimeoutSeconds": 30
}
```

`allowUnrestrictedFilePermissions` ist standardmäßig `false` und gehört nur in Entwicklungsumgebungen
ohne POSIX-Dateirechte (Windows); der Start warnt dann.

## Öffentliche Schnittstellen

Der Master exportiert kein Java-Paket (alles ist intern). Seine Schnittstelle ist der Dienst
`kiln.protocol.v1.NodeControl` aus `kiln-protocol` über mTLS.

## Sicherheit

Siehe ADR 0005: Identität aus dem Zertifikat, Zertifikat plus einmaliges Join-Token bei der
Aufnahme, Audit vor jeder Entscheidung (fail closed), keine Nachrichten im Log, Replay-Schutz,
Größen- und Zeitbegrenzungen.

## Abhängigkeiten

- `kiln-api`, `kiln-common`, `kiln-protocol`, `kiln-setup`
- gRPC (`grpc-netty-shaded`) und Jackson (nur für die Konfiguration)

## Offene Punkte

- Persistenz des Enrollments und der Tokens (M4), Ratenbegrenzung für Registrierungen
- Ausstellen von Join-Tokens über das Terminal (Paket 5) bzw. den Setup-Manager (M4)
- Prozess-/Server-Verwaltung, Templates, REST/WebSocket-API
