package at.kilncloud.buildlogic

import java.io.File

/**
 * Derives class-file exclusion patterns from generated Java sources.
 *
 * Generated code (Protobuf, gRPC, jOOQ) is not ours to test or analyse (AGENTS.md §13), but it
 * ends up in the same class directory as hand-written code. Instead of guessing packages, the
 * patterns are computed from the generated source files themselves, so a hand-written class in the
 * same package is never excluded by accident.
 */
object GeneratedClassPatterns {
    /**
     * Lists Ant-style class-file patterns for every generated source file below [generatedRoot].
     *
     * A source `at/x/Foo.java` yields `at/x/Foo.class` and `at/x/Foo$*.class` (nested classes).
     *
     * @param generatedRoot directory with generated Java sources; a missing directory yields no patterns
     * @return the sorted patterns, empty if nothing was generated
     */
    fun excludePatterns(generatedRoot: File): List<String> {
        if (!generatedRoot.isDirectory) return emptyList()
        return generatedRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .map { it.relativeTo(generatedRoot).invariantSeparatorsPath.removeSuffix(".java") }
            .flatMap { sequenceOf("$it.class", "$it\$*.class") }
            .sorted()
            .toList()
    }
}
