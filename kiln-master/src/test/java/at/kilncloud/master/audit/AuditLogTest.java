package at.kilncloud.master.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests {@link AuditLog} and {@link AuditEvent} on an in-memory Unix file system. */
final class AuditLogTest {
    private static final Instant TIME = Instant.parse("2026-10-14T12:00:00Z");

    private FileSystem unix;
    private Path dir;

    @BeforeEach
    void setUp() throws IOException {
        unix = Jimfs.newFileSystem(Configuration.unix().toBuilder()
                .setAttributeViews("basic", "posix")
                .build());
        dir = Files.createDirectories(unix.getPath("/var/log/kiln"));
    }

    @AfterEach
    void tearDown() throws IOException {
        unix.close();
    }

    private static AuditEvent event(String actor, String outcome) {
        return new AuditEvent(TIME, actor, "node.register", "node-1", outcome);
    }

    @Test
    @DisplayName("append schreibt eine JSON-Zeile pro Ereignis")
    void append_writesOneJsonLinePerEvent() throws IOException {
        Path file = dir.resolve("audit.log");
        try (AuditLog log = AuditLog.open(file, true)) {
            log.append(event("node-1", "accepted"));
            log.append(event("node-2", "denied"));
        }
        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertEquals(
                "{\"time\":\"2026-10-14T12:00:00Z\",\"actor\":\"node-1\",\"action\":\"node.register\","
                        + "\"target\":\"node-1\",\"outcome\":\"accepted\"}",
                lines.get(0));
    }

    @Test
    @DisplayName("open legt die Datei mit Rechten 600 an")
    void open_createsFileWithOwnerOnlyPermissions() throws IOException {
        Path file = dir.resolve("audit.log");
        AuditLog.open(file, true).close();
        assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
    }

    @Test
    @DisplayName("open verweigert eine vorhandene Datei mit zu weiten Rechten")
    void open_rejectsExistingFileWithWidePermissions() throws IOException {
        Path file = Files.createFile(dir.resolve("audit.log"));
        Files.setPosixFilePermissions(
                file,
                java.util.EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.GROUP_READ));
        IOException failure = assertThrows(IOException.class, () -> AuditLog.open(file, true));
        assertEquals("audit.permissions.too-open", failure.getMessage());
    }

    @Test
    @DisplayName("open hängt an eine vorhandene Datei an, statt sie zu überschreiben")
    void open_appendsToExistingFile() throws IOException {
        Path file = dir.resolve("audit.log");
        try (AuditLog log = AuditLog.open(file, true)) {
            log.append(event("node-1", "first"));
        }
        try (AuditLog log = AuditLog.open(file, true)) {
            log.append(event("node-1", "second"));
        }
        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("first"));
        assertTrue(lines.get(1).contains("second"));
    }

    @Test
    @DisplayName("open folgt keinem Symlink")
    void open_rejectsSymbolicLink() throws IOException {
        Path real = Files.createFile(dir.resolve("real.log"));
        Path link = Files.createSymbolicLink(dir.resolve("audit.log"), real);
        assertThrows(IOException.class, () -> AuditLog.open(link, true));
    }

    @Test
    @DisplayName("open verlangt für Rechte 600 ein POSIX-Dateisystem")
    void open_ownerOnlyNeedsPosix() throws IOException {
        try (FileSystem windows = Jimfs.newFileSystem(Configuration.windows())) {
            Path file = Files.createDirectories(windows.getPath("C:\\logs")).resolve("audit.log");
            assertThrows(UnsupportedOperationException.class, () -> AuditLog.open(file, true));
            AuditLog.open(file, false).close();
            assertTrue(Files.exists(file));
        }
    }

    @Test
    @DisplayName("append verhindert Log-Injection durch Zeilenumbrüche und Anführungszeichen")
    void append_escapesInjectedLines() throws IOException {
        Path file = dir.resolve("audit.log");
        try (AuditLog log = AuditLog.open(file, true)) {
            log.append(event("evil\n{\"actor\":\"admin\"}", "x\"y\\z\r\u2028\u007f"));
        }
        List<String> lines = Files.readAllLines(file);
        assertEquals(1, lines.size(), "a newline in a value must not start a second entry");
        assertFalse(lines.get(0).contains("\r"));
        assertTrue(lines.get(0).contains("evil\\u000a{\\\"actor\\\":\\\"admin\\\"}"));
        assertTrue(lines.get(0).contains("\\u2028"));
        assertTrue(lines.get(0).contains("\\u007f"));
    }

    @Test
    @DisplayName("append nach close schlägt fehl, damit die Aktion verweigert werden kann")
    void append_afterCloseFails() throws IOException {
        AuditLog log = AuditLog.open(dir.resolve("audit.log"), true);
        log.close();
        assertThrows(IOException.class, () -> log.append(event("node-1", "accepted")));
    }

    @Test
    @DisplayName("quote maskiert Steuerzeichen und lässt normalen Text unverändert")
    void quote_escapesControlCharacters() {
        assertEquals("\"plain\"", AuditLog.quote("plain"));
        assertEquals("\"a\\u0000b\"", AuditLog.quote("a\u0000b"));
        assertEquals("\"ä\"", AuditLog.quote("ä"));
    }

    @Test
    @DisplayName("AuditEvent lehnt ungültige Werte ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void auditEvent_rejectsInvalidValues() {
        assertThrows(NullPointerException.class, () -> new AuditEvent(null, "a", "b", "c", "d"));
        assertThrows(NullPointerException.class, () -> new AuditEvent(TIME, null, "b", "c", "d"));
        assertThrows(IllegalArgumentException.class, () -> new AuditEvent(TIME, " ", "b", "c", "d"));
        assertThrows(IllegalArgumentException.class, () -> new AuditEvent(TIME, "a", "", "c", "d"));
        assertThrows(IllegalArgumentException.class, () -> new AuditEvent(TIME, "a", "b", "x".repeat(257), "d"));
        assertEquals("x".repeat(256), new AuditEvent(TIME, "a", "b", "x".repeat(256), "d").target());
    }

    @Test
    @DisplayName("open und append lehnen null ab")
    // This test intentionally violates the static null contract to verify the runtime boundary.
    @SuppressWarnings("NullAway")
    void methods_rejectNull() throws IOException {
        assertThrows(NullPointerException.class, () -> AuditLog.open(null, true));
        try (AuditLog log = AuditLog.open(dir.resolve("audit.log"), true)) {
            assertThrows(NullPointerException.class, () -> log.append(null));
        }
    }
}
