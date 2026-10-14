/**
 * Defines shared infrastructure utilities for KilnCloud processes.
 */
module at.kilncloud.common {
    requires at.kilncloud.api;
    requires com.fasterxml.jackson.databind;
    requires static org.jspecify;

    exports at.kilncloud.common.config;
    exports at.kilncloud.common.filesystem;
    exports at.kilncloud.common.security;
    exports at.kilncloud.common.validation;
}
