package at.kilncloud.master.audit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Append-only audit log in JSON Lines format (AGENTS.md §7.3).
 *
 * <p>The file is opened for appending only, never truncated or rewritten, and this class offers no
 * way to change or delete an entry. Each entry is one line; all text is escaped so that a hostile
 * value cannot start a new line and forge another entry (log injection). The file must not be
 * accessible to group or others (permissions {@code 600}); an existing file with wider permissions is
 * refused instead of silently used. Every entry is flushed to disk before {@link
 * #append(AuditEvent)} returns, so a recorded action survives a crash.
 *
 * <p>Thread-safe: appends are serialized. Blocking file I/O runs on the calling thread.
 */
public final class AuditLog implements AutoCloseable {
    private static final Set<PosixFilePermission> OWNER_ONLY = PosixFilePermissions.fromString("rw-------");

    private final FileChannel channel;
    private final ReentrantLock lock = new ReentrantLock();

    private AuditLog(FileChannel channel) {
        this.channel = channel;
    }

    /**
     * Opens or creates the audit log.
     *
     * @param file the log file; the last path component must not be a symbolic link
     * @param requireOwnerOnly {@code true} on POSIX systems, where the file is created with {@code 600}
     *     and an existing file with wider permissions is refused; {@code false} only for development
     *     on systems without POSIX permissions, where the caller must warn the operator
     * @return the open log
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws UnsupportedOperationException if {@code requireOwnerOnly} is set but the file system has
     *     no POSIX permissions
     * @throws IOException if the file cannot be opened or its permissions are too wide
     */
    public static AuditLog open(Path file, boolean requireOwnerOnly) throws IOException {
        Objects.requireNonNull(file, "audit.file.null");
        if (requireOwnerOnly
                && !file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            throw new UnsupportedOperationException("audit.permissions.unsupported");
        }
        FileChannel channel;
        if (requireOwnerOnly) {
            channel = FileChannel.open(
                    file,
                    Set.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND, LinkOption.NOFOLLOW_LINKS),
                    PosixFilePermissions.asFileAttribute(OWNER_ONLY));
            Set<PosixFilePermission> actual = Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS);
            if (!OWNER_ONLY.containsAll(actual)) {
                channel.close();
                throw new IOException("audit.permissions.too-open");
            }
        } else {
            channel = FileChannel.open(
                    file, Set.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND, LinkOption.NOFOLLOW_LINKS));
        }
        return new AuditLog(channel);
    }

    /**
     * Appends one entry and flushes it to disk.
     *
     * @param event the entry to record
     * @throws NullPointerException if {@code event} is {@code null}
     * @throws IOException if the entry cannot be written; callers must then refuse the action, because
     *     an action that cannot be audited must not happen (fail closed)
     */
    public void append(AuditEvent event) throws IOException {
        Objects.requireNonNull(event, "audit.event.null");
        String line = "{\"time\":" + quote(event.time().toString())
                + ",\"actor\":" + quote(event.actor())
                + ",\"action\":" + quote(event.action())
                + ",\"target\":" + quote(event.target())
                + ",\"outcome\":" + quote(event.outcome())
                + "}\n";
        ByteBuffer buffer = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8));
        // A private lock (not "this") keeps a line whole even if callers synchronize on the log.
        lock.lock();
        try {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(false);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Closes the log file.
     *
     * @throws IOException if closing fails
     */
    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            channel.close();
        } finally {
            lock.unlock();
        }
    }

    /** Escapes a value as a JSON string; control characters and line separators never survive. */
    static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                default -> {
                    if (c < 0x20 || c == 0x7f || c == 0x2028 || c == 0x2029) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
