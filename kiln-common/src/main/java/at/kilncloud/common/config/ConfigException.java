package at.kilncloud.common.config;

import java.io.IOException;

/**
 * Signals that a configuration file could not be read, parsed, validated or written.
 *
 * <p>The message is a neutral key such as {@code config.invalid}. It never contains file content,
 * so that a rejected secret cannot leak through logs or error pages. It carries no cause for the
 * same reason: parser messages can quote the rejected value.
 */
public final class ConfigException extends IOException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a neutral message key.
     *
     * @param messageKey a stable key such as {@code config.invalid}; never file content
     */
    ConfigException(String messageKey) {
        super(messageKey);
    }
}
