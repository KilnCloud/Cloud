# ADR 0005: Sicherheitsmodell des Master-Kerns

Status: akzeptiert (2026-10-14)

## Kontext

Der Master nimmt Nodes auf, überwacht sie und protokolliert sicherheitsrelevante Aktionen
(M1, Paket 3a). Er läuft auf Servern von Betreibern und ist von Nodes aus dem Netzwerk erreichbar.

## Entscheidungen

1. **Identität kommt aus dem Zertifikat, nicht aus der Nachricht.** Der Server verlangt mTLS 1.3 mit
   Client-Zertifikat. `PeerIdentity` liest den Node-Namen aus dem Common Name des verifizierten
   Zertifikats und stellt ihn dem Dienst bereit. Ein Aufruf ohne verifizierte Identität wird vor jedem
   Dienstcode mit `UNAUTHENTICATED` beendet. Der Name in der Nachricht muss mit dem im Zertifikat
   übereinstimmen, sonst `REGISTER_STATUS_NAME_REJECTED`.
2. **Zwei Faktoren bei der Aufnahme.** Ein neuer Node braucht Zertifikat **und** ein einmaliges,
   an seinen Namen gebundenes Join-Token. Ein gestohlenes Zertifikat allein genügt nicht, ein
   gestohlenes Token allein auch nicht. Ein aufgenommener Node darf sich ohne Token wieder verbinden.
   Das Entziehen der Aufnahme (`revoke`) beendet das.
3. **Fail closed beim Audit.** Jede Entscheidung über eine Registrierung wird ins Audit-Log
   geschrieben, bevor sie wirkt. Kann das Log nicht geschrieben werden, scheitert die Anfrage mit
   `INTERNAL`; der Zustand ändert sich nicht. Das Log ist append-only, hat Rechte `600`, escaped alle
   Werte (Log-Injection) und wird bei jedem Eintrag auf die Platte geschrieben.
4. **Keine Nachrichten im Log.** Anfragen enthalten das Join-Token und werden nie geloggt oder
   gedruckt. Fehler an den Betreiber sind neutrale Schlüssel (und Feldpfade), nie Werte.
5. **Replay-Schutz.** Heartbeat-Sequenznummern müssen je Verbindung streng steigen.
6. **Begrenzungen.** Nachrichten höchstens 64 KiB, Join-Token höchstens 256 Byte, Handshake-Timeout
   zehn Sekunden, höchstens 1000 offene Tokens, Anfragen auf virtuellen Threads.
7. **Standard ist sicher.** Die Konfiguration schaltet die Dateirechte-Einschränkung nur über das
   ausdrücklich benannte Feld `allowUnrestrictedFilePermissions` ab (Standard `false`), und der Start
   warnt dann.
8. **Transport: `grpc-netty-shaded`.** Netty ist gebündelt und umbenannt, sodass kein eigener
   Netty-Abhängigkeitsbaum nötig ist. Sicherheitsupdates von Netty kommen mit gRPC-Updates.
9. **Lint-Lockerung `requires-automatic`.** gRPC und Protobuf sind automatische Module. javac warnt
   bei jedem `requires` darauf, was `-Werror` zu einem Fehler macht. Diese eine Lint-Kategorie ist für
   Cloud-Module abgeschaltet (`kiln.java-cloud`); alle anderen Warnungen bleiben Fehler.

## Folgen

- Der Enrollment-Zustand liegt nur im Speicher; nach einem Neustart des Masters müssen Nodes sich mit
  neuen Join-Tokens neu aufnehmen lassen. Persistenz folgt mit dem Storage-Modul (M4).
- Join-Tokens kann der Betreiber in M1 noch nicht über eine Oberfläche ausstellen; das Terminal
  (Paket 5) und der Setup-Manager (M4) liefern das.
- Ratenbegrenzung für Registrierungsversuche fehlt noch und ist Folgearbeit.
