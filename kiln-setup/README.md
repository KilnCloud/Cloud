# kiln-setup

`kiln-setup` enthält die Einrichtungslogik von KilnCloud: die eigene Zertifizierungsstelle, die
mTLS-Identitäten von Master und Nodes sowie die einmaligen Join-Tokens. Der interaktive
Easy-Setup-Manager (M4) baut auf dieser Logik auf.

## Status

**Grundgerüst – 20 %:** CA, Zertifikatsausstellung, PEM-Dateien, TLS-1.3-Kontexte, Rotation und
Join-Tokens sind umgesetzt und getestet, einschließlich echter gegenseitig authentifizierter
Handshakes. Der interaktive Setup-Ablauf, Datenbank-Setup und persistente Token-Ablage folgen
in M4.

## Öffentliche Schnittstellen

- `at.kilncloud.setup.pki.CertificateAuthority`: erzeugt oder lädt die CA, stellt Client- und
  Server-Zertifikate aus
- `at.kilncloud.setup.pki.IssuedCertificate`: Zertifikat samt Schlüssel (kein Record, `toString()`
  zeigt keinen Schlüssel)
- `at.kilncloud.setup.pki.PkiFiles`: PEM lesen und schreiben, Schlüssel mit Rechten 600
- `at.kilncloud.setup.pki.TlsContexts`: TLS-1.3-Kontext, Server- und Client-Parameter
- `at.kilncloud.setup.pki.CertificateRotation`, `CertificateNames`: Rotationsprüfung, Node-Name aus
  einem geprüften Zertifikat
- `at.kilncloud.setup.token.JoinTokens`: einmalige, kurzlebige, an einen Node gebundene Tokens

## Abhängigkeiten

- `kiln-common` (validierte Namen, sichere Dateien, Token-Erzeugung), transitiv
- BouncyCastle (`bcprov`, `bcpkix`) nur zum Erzeugen von Zertifikaten, siehe ADR 0004
- Jimfs und JUnit nur für Tests

## Konfiguration

- Kompiliert über `kiln.java-cloud` und `kiln.quality` (Release 25, JPMS)
- Schlüssel sind `secp256r1`; Blätter laufen höchstens 366 Tage, Tokens höchstens 24 Stunden

## Offene Punkte

- Interaktiver Setup-Ablauf und Betreiber-Warnungen (M4)
- Persistente, gehashte Token-Ablage über das Storage-Modul (M4)
- Optionale Passphrase für Schlüsseldateien, Widerruf (CRL/OCSP) und automatische Rotation
