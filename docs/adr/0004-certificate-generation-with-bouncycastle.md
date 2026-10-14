# ADR 0004: Zertifikatserzeugung mit BouncyCastle, Schlüsselwahl und Laufzeiten

Status: akzeptiert (2026-10-14)

## Kontext

Master und Nodes sprechen per mTLS (TLS 1.3) miteinander (AGENTS.md §7.2). Der Setup-Manager muss
dafür eine eigene Zertifizierungsstelle (CA) betreiben und Zertifikate für Master und Nodes
ausstellen und rotieren. Der Maintainer hat entschieden: JDK-Bordmittel, BouncyCastle nur wenn nötig.

## Entscheidung

1. **BouncyCastle (`bcprov-jdk18on`, `bcpkix-jdk18on`, 1.86) wird verwendet**, weil das JDK keine
   öffentliche API zum Erzeugen von X.509-Zertifikaten besitzt. Geprüft und verworfen:
   - `sun.security.x509` (interne API, nicht stabil, braucht `--add-exports`),
   - `keytool` als Kindprozess (Schlüsselpasswörter in der Prozessliste, widerspricht §7.6; schwer
     testbar).
   BouncyCastle wird nur zur Erzeugung verwendet. TLS selbst, Prüfung und Parsen laufen über das JDK
   (JSSE, `CertificateFactory`, `KeyFactory`).
2. **Schlüssel:** Elliptische Kurven `secp256r1` mit `SHA256withECDSA`. TLS 1.3 unterstützt das
   verpflichtend, Schlüssel sind klein und schnell.
3. **Zertifikate:** Die CA hat `basicConstraints CA:true, pathlen 0` und darf nur Blätter signieren.
   Blätter haben `CA:false`, `digitalSignature` und genau einen Zweck: Client-Zertifikate für Nodes
   (`clientAuth`), Server-Zertifikat für den Master (`serverAuth`, mit DNS- und IP-Alternativnamen).
   Seriennummern sind zufällig (159 Bit).
4. **Laufzeiten:** Blätter höchstens 366 Tage und nie länger als die CA; CA frei wählbar (Empfehlung
   fünf Jahre). Rotation rechtzeitig vor Ablauf über `CertificateRotation.isRotationDue`.
5. **Vertrauen:** Beide Seiten vertrauen ausschließlich der KilnCloud-CA, nie dem Trust-Store des
   Betriebssystems. Der Server verlangt ein Client-Zertifikat, der Client prüft den Hostnamen.
6. **Join-Tokens:** 32 Zufallsbytes, einmalig, höchstens 24 Stunden gültig, an einen Node-Namen
   gebunden, nur als SHA-256 gespeichert (ein Salz ist bei 256 Bit Zufall nicht nötig), Vergleich in
   konstanter Zeit gegen alle Einträge. Die Ablage im Speicher gilt für M1; die Persistenz kommt mit
   dem Storage-Modul (M4).
7. **Schlüssel auf der Platte:** PEM (PKCS#8) mit Dateirechten `600`, atomar geschrieben. Auf
   Systemen ohne POSIX-Rechte schlägt das Schreiben fehl, außer die Einschränkung wird ausdrücklich
   abgeschaltet (nur Entwicklung unter Windows, mit Warnung). Verschlüsselung der Schlüssel mit einer
   Passphrase ist eine spätere Option.

## Folgen

- Eine zusätzliche, sicherheitskritische Abhängigkeit mit eigenen Hashes; sie wird regelmäßig auf
  Sicherheitsmeldungen geprüft.
- `kiln-setup` (neues Modul) enthält die PKI- und Token-Kernlogik; der interaktive
  Easy-Setup-Manager folgt in M4 und nutzt dieselbe Logik.
- Die Tests führen echte gegenseitig authentifizierte TLS-1.3-Handshakes über Loopback aus, auch die
  Fehlerfälle (fremde CA, abgelaufen, falscher Host, falsche Verwendung).
