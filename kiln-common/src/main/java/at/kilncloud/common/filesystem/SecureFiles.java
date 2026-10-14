package at.kilncloud.common.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;

/**
 * Reads and writes files without time-of-check/time-of-use gaps that an attacker could exploit.
 *
 * <p>Reads open the file first and check afterwards, on the open handle, instead of checking a path
 * and opening it later. Writes go to a fresh temporary file in the target directory and replace the
 * target with one atomic rename, so readers never see partial content. Only the last path
 * component is protected against symbolic links; resolve untrusted relative paths with {@link
 * SecurePathResolver} first, because a link in a parent directory is still followed.
 */
public final class SecureFiles {
    /** Permissions {@code rw-------}, used for files that contain secrets (AGENTS.md §7.6). */
    private static final Set<PosixFilePermission> OWNER_ONLY = PosixFilePermissions.fromString("rw-------");

    /** Number of random bytes in a temporary file name; 128 bits make a guess infeasible. */
    private static final int TEMP_NAME_BYTES = 16;

    /** Cryptographically secure source for unpredictable temporary file names. */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Prevents construction of this stateless utility class. */
    private SecureFiles() {}

    /**
     * Reads a regular file completely, refusing symbolic links and files larger than a limit.
     *
     * <p>The file is opened with {@link LinkOption#NOFOLLOW_LINKS}; a symbolic link in the last path
     * component fails the open. The size is then taken from the open handle, so the file cannot be
     * swapped between check and read. This method blocks on file I/O on the calling thread.
     *
     * @param file the file to read; the last component must not be a symbolic link
     * @param maxBytes the largest accepted file size in bytes; must be positive
     * @return the file content, never more than {@code maxBytes} bytes
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws IllegalArgumentException if {@code maxBytes} is not positive
     * @throws IOException if the file cannot be opened, is a symbolic link, or exceeds the limit
     */
    public static byte[] readBounded(Path file, int maxBytes) throws IOException {
        Objects.requireNonNull(file, "filesystem.file.null");
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("filesystem.max-bytes.invalid");
        }
        try (SeekableByteChannel channel =
                Files.newByteChannel(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            long size = channel.size();
            if (size > maxBytes) {
                throw new IOException("filesystem.file.too-large");
            }
            // Read one byte more than the limit so that a file that grew after size() is detected.
            ByteBuffer buffer = ByteBuffer.allocate((int) Math.min((long) maxBytes + 1, size + 1));
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) {
                // keep reading until the buffer is full or the channel ends
            }
            if (buffer.position() > maxBytes) {
                throw new IOException("filesystem.file.too-large");
            }
            byte[] content = new byte[buffer.position()];
            buffer.flip().get(content);
            return content;
        }
    }

    /**
     * Writes a file atomically, optionally readable and writable by its owner only.
     *
     * <p>The content is written to a new temporary file next to the target (never to an existing
     * path), flushed to disk and then moved over the target with an atomic rename. A symbolic link at
     * the target path is replaced, not followed. On failure the temporary file is removed. This
     * method blocks on file I/O on the calling thread.
     *
     * @param target the file to create or replace; its parent directory must exist
     * @param content the bytes to write
     * @param ownerOnly {@code true} to create the file with permissions {@code 600}; this needs a
     *     POSIX file system
     * @throws NullPointerException if an argument is {@code null}
     * @throws UnsupportedOperationException if {@code ownerOnly} is set and the file system has no
     *     POSIX permissions, so secrets are never written with unknown access rights
     * @throws IOException if writing or the atomic move fails, including file systems that cannot
     *     move atomically
     */
    public static void writeAtomically(Path target, byte[] content, boolean ownerOnly) throws IOException {
        Objects.requireNonNull(target, "filesystem.target.null");
        Objects.requireNonNull(content, "filesystem.content.null");
        FileAttribute<?>[] attributes = ownerOnlyAttributes(target, ownerOnly);

        Path directory = target.toAbsolutePath().getParent();
        if (directory == null) {
            throw new IOException("filesystem.target.no-parent");
        }
        Path temporary = directory.resolve(".kiln-" + randomName() + ".tmp");
        boolean moved = false;
        try {
            // CREATE_NEW fails instead of following a pre-placed file or symlink. The content is
            // written through this very handle, so the temporary path is never opened a second time.
            try (SeekableByteChannel channel = Files.newByteChannel(
                    temporary, Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), attributes)) {
                ByteBuffer buffer = ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                if (channel instanceof FileChannel fileChannel) {
                    fileChannel.force(true);
                }
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            moved = true;
        } catch (AtomicMoveNotSupportedException failure) {
            throw new IOException("filesystem.move.not-atomic", failure);
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static String randomName() {
        byte[] random = new byte[TEMP_NAME_BYTES];
        RANDOM.nextBytes(random);
        return HexFormat.of().formatHex(random);
    }

    private static FileAttribute<?>[] ownerOnlyAttributes(Path target, boolean ownerOnly) {
        if (!ownerOnly) {
            return new FileAttribute<?>[0];
        }
        if (!target.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            throw new UnsupportedOperationException("filesystem.permissions.unsupported");
        }
        return new FileAttribute<?>[] {PosixFilePermissions.asFileAttribute(OWNER_ONLY)};
    }
}
