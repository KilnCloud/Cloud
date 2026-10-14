package at.kilncloud.common.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Objects;

/** Resolves existing untrusted paths without allowing traversal outside an approved directory. */
public final class SecurePathResolver {
    /** Prevents construction of this stateless utility class. */
    private SecurePathResolver() {}

    /**
     * Resolves an existing relative path below an existing base directory.
     *
     * <p>The method first performs a normalized lexical containment check, then resolves symlinks
     * through {@link Path#toRealPath(java.nio.file.LinkOption...)} and checks containment again.
     * The returned path therefore names the real existing target at validation time. Callers that
     * operate in directories writable by an attacker must still prevent time-of-check/time-of-use
     * replacement while opening the returned path.
     *
     * <p>This method performs blocking filesystem access and runs on the calling thread.
     *
     * @param baseDirectory the existing directory that bounds resolution
     * @param untrustedRelativePath the untrusted relative path to resolve
     * @return the real path of the existing target below the real base directory
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalArgumentException if the untrusted path is absolute or escapes the base
     * @throws NotDirectoryException if the real base path is not a directory
     * @throws IOException if either path cannot be resolved to an existing real path
     */
    public static Path resolveExisting(Path baseDirectory, Path untrustedRelativePath) throws IOException {
        Objects.requireNonNull(baseDirectory, "filesystem.base.null");
        Objects.requireNonNull(untrustedRelativePath, "filesystem.path.null");

        if (untrustedRelativePath.isAbsolute()) {
            throw new IllegalArgumentException("filesystem.path.absolute");
        }

        Path realBase = baseDirectory.toRealPath();
        if (!Files.isDirectory(realBase)) {
            throw new NotDirectoryException("filesystem.base.not-directory");
        }

        Path normalizedCandidate = realBase.resolve(untrustedRelativePath).normalize();
        if (!normalizedCandidate.startsWith(realBase)) {
            throw new IllegalArgumentException("filesystem.path.outside-base");
        }

        Path realCandidate = normalizedCandidate.toRealPath();
        if (!realCandidate.startsWith(realBase)) {
            throw new IllegalArgumentException("filesystem.path.outside-base");
        }
        return realCandidate;
    }
}
