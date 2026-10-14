package at.kilncloud.common.config;

import at.kilncloud.common.filesystem.SecureFiles;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.type.LogicalType;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Loads and stores typed JSON configuration files with a strict parser (AGENTS.md §7.4).
 *
 * <p>Input is untrusted. The parser therefore rejects unknown fields, duplicate keys, trailing
 * content after the document, silent type coercion (for example {@code "5"} for a number) and
 * {@code null} for primitive fields. Polymorphic typing by class name is disabled: a type annotated
 * with {@code @JsonTypeInfo(use = Id.CLASS)} is refused instead of instantiating an arbitrary
 * class. Validating record types, such as the value types of {@code
 * at.kilncloud.common.validation}, are checked while parsing because their compact constructors
 * run.
 *
 * <p>Files are read with {@link SecureFiles#readBounded(Path, int)} and written with {@link
 * SecureFiles#writeAtomically(Path, byte[], boolean)}. When this module runs on the module path,
 * the module that owns the configuration type must open its package to {@code
 * com.fasterxml.jackson.databind}. Fields that hold secrets should be {@code char[]} or {@code
 * byte[]} in a class that masks {@code toString()}, not in a record (AGENTS.md §7.6).
 *
 * <p>Both methods block on file I/O on the calling thread and are safe to call concurrently.
 */
public final class SecureConfig {
    /** Default size limit for a configuration file: 1 MiB. */
    public static final int DEFAULT_MAX_BYTES = 1024 * 1024;

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            // Text fields must not silently accept numbers or booleans either.
            .withCoercionConfig(
                    LogicalType.Textual,
                    config -> config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                            .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                            .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
            // An empty validator allows no polymorphic subtype, which also blocks Id.CLASS typing.
            .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder().build())
            .build();

    /** Prevents construction of this stateless utility class. */
    private SecureConfig() {}

    /**
     * Reads a configuration file with the {@linkplain #DEFAULT_MAX_BYTES default size limit}.
     *
     * @param <T> the configuration type
     * @param file the file to read; the last path component must not be a symbolic link
     * @param type the configuration type to create
     * @return the parsed configuration, never {@code null}
     * @throws NullPointerException if an argument is {@code null}
     * @throws ConfigException if the content is invalid, empty or {@code null}
     * @throws IOException if the file cannot be read, is a symbolic link or is too large
     * @see #read(Path, Class, int)
     */
    public static <T> T read(Path file, Class<T> type) throws IOException {
        return read(file, type, DEFAULT_MAX_BYTES);
    }

    /**
     * Reads a configuration file into a typed object.
     *
     * <p>The exception message names only the failing field path, for example {@code
     * config.invalid:server.port}, never the rejected value.
     *
     * @param <T> the configuration type
     * @param file the file to read; the last path component must not be a symbolic link
     * @param type the configuration type to create
     * @param maxBytes the largest accepted file size in bytes; must be positive
     * @return the parsed configuration, never {@code null}
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code maxBytes} is not positive
     * @throws ConfigException if the content is invalid, empty or {@code null}
     * @throws IOException if the file cannot be read, is a symbolic link or is too large
     */
    public static <T> T read(Path file, Class<T> type, int maxBytes) throws IOException {
        Objects.requireNonNull(file, "config.file.null");
        Objects.requireNonNull(type, "config.type.null");
        byte[] content = SecureFiles.readBounded(file, maxBytes);
        T value;
        try {
            value = MAPPER.readValue(content, type);
        } catch (JsonMappingException failure) {
            // The cause is dropped on purpose: Jackson messages can quote the rejected value.
            throw new ConfigException("config.invalid:" + fieldPath(failure));
        } catch (IOException failure) {
            throw new ConfigException("config.invalid");
        }
        if (value == null) {
            throw new ConfigException("config.empty");
        }
        return value;
    }

    /**
     * Writes a configuration object as pretty-printed JSON, atomically.
     *
     * @param file the file to create or replace; its parent directory must exist
     * @param value the configuration to store
     * @param secret {@code true} if the file contains secrets, so it is created with permissions
     *     {@code 600}; this needs a POSIX file system
     * @throws NullPointerException if an argument is {@code null}
     * @throws UnsupportedOperationException if {@code secret} is set and the file system has no POSIX
     *     permissions
     * @throws ConfigException if the value cannot be serialized
     * @throws IOException if the file cannot be written
     */
    public static void write(Path file, Object value, boolean secret) throws IOException {
        Objects.requireNonNull(file, "config.file.null");
        Objects.requireNonNull(value, "config.value.null");
        byte[] json;
        try {
            json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);
        } catch (JsonProcessingException failure) {
            throw new ConfigException("config.unwritable");
        }
        byte[] withNewline = new byte[json.length + 1];
        System.arraycopy(json, 0, withNewline, 0, json.length);
        withNewline[json.length] = '\n';
        SecureFiles.writeAtomically(file, withNewline, secret);
    }

    private static String fieldPath(JsonMappingException failure) {
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference step : failure.getPath()) {
            if (step.getFieldName() != null) {
                if (path.length() > 0) {
                    path.append('.');
                }
                path.append(step.getFieldName());
            } else {
                // A path step is either a field or a list index, so this is the index case.
                path.append('[').append(step.getIndex()).append(']');
            }
        }
        return path.length() == 0 ? "$" : path.toString();
    }
}
