package at.kilncloud.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.kilncloud.common.validation.ServerName;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests {@link SecureConfig} on an in-memory Unix file system. */
final class SecureConfigTest {
    /** Plain configuration used by the tests. */
    record Server(String name, int port) {}

    /** Configuration with a validated value type. */
    record Named(ServerName server) {}

    /** Configuration with a primitive and a list. */
    record Limits(int max, List<String> hosts) {}

    /** Configuration with a nested object. */
    record Nested(Server server) {}

    /** Abstract type that tries to select the class from the document. */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    interface Plugin {}

    /** Holder of a polymorphic field. */
    record Holder(Plugin plugin) {}

    private FileSystem unix;
    private Path dir;

    @BeforeEach
    void setUp() throws IOException {
        unix = Jimfs.newFileSystem(Configuration.unix().toBuilder()
                .setAttributeViews("basic", "posix")
                .build());
        dir = Files.createDirectories(unix.getPath("/etc/kiln"));
    }

    @AfterEach
    void tearDown() throws IOException {
        unix.close();
    }

    private Path file(String json) throws IOException {
        return Files.writeString(dir.resolve("config.json"), json);
    }

    @Test
    @DisplayName("read liest eine gültige Konfiguration in den Zieltyp")
    void read_parsesValidDocument() throws IOException {
        Server server = SecureConfig.read(file("{\"name\":\"lobby\",\"port\":25565}"), Server.class);
        assertEquals(new Server("lobby", 25565), server);
    }

    @Test
    @DisplayName("read lehnt unbekannte Felder ab und nennt nur den Feldnamen")
    void read_rejectsUnknownField() throws IOException {
        Path config = file("{\"name\":\"lobby\",\"port\":1,\"admin\":true}");
        ConfigException failure = assertThrows(ConfigException.class, () -> SecureConfig.read(config, Server.class));
        assertEquals("config.invalid:admin", failure.getMessage());
    }

    @Test
    @DisplayName("read lehnt doppelte Schlüssel ab")
    void read_rejectsDuplicateKeys() throws IOException {
        Path config = file("{\"name\":\"a\",\"name\":\"b\",\"port\":1}");
        assertThrows(ConfigException.class, () -> SecureConfig.read(config, Server.class));
    }

    @Test
    @DisplayName("read lehnt Inhalt nach dem Dokument ab")
    void read_rejectsTrailingContent() throws IOException {
        Path config = file("{\"name\":\"a\",\"port\":1} {\"name\":\"b\",\"port\":2}");
        assertThrows(ConfigException.class, () -> SecureConfig.read(config, Server.class));
    }

    @Test
    @DisplayName("read wandelt Zeichenketten nicht stillschweigend in Zahlen um")
    void read_rejectsScalarCoercion() throws IOException {
        Path config = file("{\"name\":\"a\",\"port\":\"25565\"}");
        ConfigException failure = assertThrows(ConfigException.class, () -> SecureConfig.read(config, Server.class));
        assertEquals("config.invalid:port", failure.getMessage());
    }

    @Test
    @DisplayName("read lehnt null für ein primitives Feld ab")
    void read_rejectsNullForPrimitive() throws IOException {
        Path config = file("{\"name\":\"a\",\"port\":null}");
        assertThrows(ConfigException.class, () -> SecureConfig.read(config, Server.class));
    }

    @Test
    @DisplayName("read gibt den abgelehnten Wert nicht in der Fehlermeldung preis")
    void read_doesNotLeakRejectedValue() throws IOException {
        Path config = file("{\"name\":\"a\",\"port\":\"hunter2-geheim\"}");
        ConfigException failure = assertThrows(ConfigException.class, () -> SecureConfig.read(config, Server.class));
        assertFalse(String.valueOf(failure.getMessage()).contains("hunter2"));
        assertNull(failure.getCause());
    }

    @Test
    @DisplayName("read meldet den Pfad in Listen mit Index")
    void read_reportsIndexInPath() throws IOException {
        Path config = file("{\"max\":1,\"hosts\":[\"a\",5,\"c\"]}");
        ConfigException failure = assertThrows(ConfigException.class, () -> SecureConfig.read(config, Limits.class));
        assertEquals("config.invalid:hosts[1]", failure.getMessage());
    }

    @Test
    @DisplayName("read meldet den Pfad verschachtelter Felder mit Punkt")
    void read_reportsNestedFieldPath() throws IOException {
        Path config = file("{\"server\":{\"name\":\"a\",\"port\":\"x\"}}");
        ConfigException failure = assertThrows(ConfigException.class, () -> SecureConfig.read(config, Nested.class));
        assertEquals("config.invalid:server.port", failure.getMessage());
    }

    @Test
    @DisplayName("read prüft validierte Werttypen über deren Konstruktor")
    void read_runsValidatingConstructors() throws IOException {
        assertEquals(
                new ServerName("lobby-1"),
                SecureConfig.read(file("{\"server\":{\"value\":\"lobby-1\"}}"), Named.class)
                        .server());
        Path bad = file("{\"server\":{\"value\":\"../etc\"}}");
        assertThrows(ConfigException.class, () -> SecureConfig.read(bad, Named.class));
    }

    @Test
    @DisplayName("read verweigert Typisierung über Klassennamen")
    void read_rejectsClassNameTyping() throws IOException {
        Path config = file("{\"plugin\":[\"java.lang.ProcessBuilder\",{}]}");
        assertThrows(ConfigException.class, () -> SecureConfig.read(config, Holder.class));
    }

    @Test
    @DisplayName("read meldet kaputtes JSON ohne Feldpfad")
    void read_reportsMalformedJson() throws IOException {
        Path config = file("{\"name\": ");
        ConfigException failure = assertThrows(ConfigException.class, () -> SecureConfig.read(config, Server.class));
        assertTrue(String.valueOf(failure.getMessage()).startsWith("config.invalid"));
    }

    @Test
    @DisplayName("read meldet ein JSON-null als leere Konfiguration")
    void read_rejectsJsonNull() throws IOException {
        Path config = file("null");
        ConfigException failure = assertThrows(ConfigException.class, () -> SecureConfig.read(config, Server.class));
        assertEquals("config.empty", failure.getMessage());
    }

    @Test
    @DisplayName("read meldet ein Wurzelfeld-Problem mit dem Pfad $")
    void read_rootLevelProblemUsesDollarPath() throws IOException {
        Path config = file("[1,2]");
        ConfigException failure = assertThrows(ConfigException.class, () -> SecureConfig.read(config, Server.class));
        assertEquals("config.invalid:$", failure.getMessage());
    }

    @Test
    @DisplayName("read begrenzt die Dateigröße")
    void read_enforcesSizeLimit() throws IOException {
        Path config = file("{\"name\":\"lobby\",\"port\":1}");
        assertThrows(IOException.class, () -> SecureConfig.read(config, Server.class, 5));
    }

    @Test
    @DisplayName("read meldet eine fehlende Datei")
    void read_missingFileFails() {
        assertThrows(NoSuchFileException.class, () -> SecureConfig.read(dir.resolve("missing.json"), Server.class));
    }

    @Test
    @DisplayName("read folgt keinem Symlink")
    void read_rejectsSymbolicLink() throws IOException {
        Path real = file("{\"name\":\"a\",\"port\":1}");
        Path link = Files.createSymbolicLink(dir.resolve("link.json"), real);
        assertThrows(IOException.class, () -> SecureConfig.read(link, Server.class));
    }

    @Test
    @DisplayName("read lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void read_rejectsNull() throws IOException {
        Path config = file("{}");
        assertThrows(NullPointerException.class, () -> SecureConfig.read(null, Server.class));
        assertThrows(NullPointerException.class, () -> SecureConfig.read(config, null));
    }

    @Test
    @DisplayName("write speichert lesbares JSON mit Zeilenumbruch, das read wieder einliest")
    void write_roundTrips() throws IOException {
        Path target = dir.resolve("out.json");
        SecureConfig.write(target, new Server("lobby", 25565), false);
        assertTrue(Files.readString(target).endsWith("\n"));
        assertEquals(new Server("lobby", 25565), SecureConfig.read(target, Server.class));
    }

    @Test
    @DisplayName("write legt Dateien mit Secrets mit Rechten 600 an")
    void write_secretUsesPermission600() throws IOException {
        Path target = dir.resolve("secret.json");
        SecureConfig.write(target, new Server("lobby", 1), true);
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(target)));
    }

    @Test
    @DisplayName("write meldet nicht serialisierbare Werte neutral")
    void write_rejectsUnserializableValue() {
        Object unserializable = new Object();
        ConfigException failure = assertThrows(
                ConfigException.class, () -> SecureConfig.write(dir.resolve("x.json"), unserializable, false));
        assertEquals("config.unwritable", failure.getMessage());
        assertFalse(Files.exists(dir.resolve("x.json")));
    }

    @Test
    @DisplayName("write lehnt null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void write_rejectsNull() {
        assertThrows(NullPointerException.class, () -> SecureConfig.write(null, new Object(), false));
        assertThrows(NullPointerException.class, () -> SecureConfig.write(dir.resolve("x"), null, false));
    }
}
