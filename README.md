# KilnCloud

KilnCloud ist ein modulares, quelloffenes Cloudsystem für Minecraft-Netzwerke. Das Projekt
befindet sich im Meilenstein **M0 – Fundament** und ist noch nicht für den produktiven Einsatz
geeignet.

## Aktueller Umfang

- `build-logic`: Convention Plugins und Qualitätswerkzeuge ([README](build-logic/README.md))
- `kiln-api`: öffentliche, Java-21-kompatible API-Basis
- `kiln-common`: gekapselte gemeinsame Implementierungsbasis
- `kiln-master`: minimaler, nebenwirkungsfreier Master-Einstiegspunkt
- `kiln-node`: minimaler, nebenwirkungsfreier Node-Einstiegspunkt
- `kiln-protocol`: Wire-Verträge (Protobuf/gRPC) zwischen Master, Nodes und Bridges

Alle weiteren Module aus der Zielarchitektur werden in der in `AGENTS.md` festgelegten
Meilensteinreihenfolge ergänzt.

## Voraussetzungen

- JDK 25
- Der mitgelieferte Gradle Wrapper; keine lokale Gradle-Installation erforderlich

## Bauen und prüfen

Unter Windows:

```powershell
.\gradlew.bat build
```

Unter Linux oder macOS:

```bash
./gradlew build
```

`./gradlew spotlessApply` formatiert den Code; `./gradlew build` führt Tests, Coverage-Gates, Error Prone/NullAway,
SpotBugs/FindSecBugs und Spotless aus.

Einzelne Module lassen sich beispielsweise mit `./gradlew :kiln-api:build` bauen. Master und
Node können während M0 mit `./gradlew :kiln-master:run` beziehungsweise
`./gradlew :kiln-node:run` gestartet werden; beide beenden sich absichtlich sofort.

## Projektstatus und Mitarbeit

- Verbindlicher Implementierungsstand: [`docs/STATUS.md`](docs/STATUS.md)
- Session-Verlauf: [`docs/progress/`](docs/progress/)
- Architektur- und Sicherheitsregeln: [`AGENTS.md`](AGENTS.md)

Vor Beiträgen muss `AGENTS.md` vollständig gelesen werden. Sicherheit, Tests und
Nachvollziehbarkeit haben Vorrang vor Umsetzungsgeschwindigkeit.
