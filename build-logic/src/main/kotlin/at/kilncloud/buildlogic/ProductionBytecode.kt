package at.kilncloud.buildlogic

import java.io.File

/**
 * Decides whether a compiled class directory contains real production code.
 *
 * `module-info.class` and `package-info.class` carry no logic. A module that only has these
 * cannot be analysed by SpotBugs and cannot produce coverage data, but a module that has any
 * other class must be analysed and tested.
 */
object ProductionBytecode {
    private val metadataClasses = setOf("module-info.class", "package-info.class")

    /**
     * Checks a compiled class directory for production bytecode.
     *
     * @param classesDir directory with compiled classes, e.g. `build/classes/java/main`
     * @return `true` if at least one class file other than module/package metadata exists;
     *   `false` if the directory is missing, empty or contains only metadata
     */
    fun existsIn(classesDir: File): Boolean =
        classesDir.isDirectory &&
            classesDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .any { it.name !in metadataClasses }
}
