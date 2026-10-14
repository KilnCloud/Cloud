package at.kilncloud.buildlogic

import org.gradle.api.provider.Property

/**
 * Per-project switches of the KilnCloud Java conventions.
 *
 * Exposed as the `kilnJava` extension by `kiln.java-base`.
 */
abstract class KilnJavaExtension {
    /**
     * Bytecode release (`javac --release`) of the project.
     *
     * Cloud modules use 25; the public API, bridges and feature modules use 21 (AGENTS.md §2).
     * Set by `kiln.java-cloud` or `kiln.java-compat`, never left unset.
     */
    abstract val release: Property<Int>
}
