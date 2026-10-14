package at.kilncloud.buildlogic

import java.io.File

/**
 * Finds Java packages that are not marked `@NullMarked` (AGENTS.md §15).
 *
 * NullAway already treats `at.kilncloud` as annotated, but the annotation itself is the contract
 * that other tools and readers rely on, so every package that contains code must declare it.
 */
object NullMarkedPackages {
    private val blockComment = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
    private val lineComment = Regex("//[^\\n]*")
    private val annotationLine = Regex("^\\s*@NullMarked\\s*$", RegexOption.MULTILINE)
    private val importLine =
        Regex("^\\s*import\\s+org\\.jspecify\\.annotations\\.NullMarked\\s*;\\s*$", RegexOption.MULTILINE)
    private val metadataFiles = setOf("module-info.java", "package-info.java")

    /**
     * Lists packages below [sourceRoot] that contain code but are not marked `@NullMarked`.
     *
     * A package violates the rule if it has no `package-info.java`, or if that file does not
     * declare `@NullMarked` (imported from `org.jspecify.annotations`) outside of comments.
     * Directories that only contain `module-info.java` or `package-info.java` are ignored.
     *
     * @param sourceRoot source directory such as `src/main/java`; a missing directory has no violations
     * @return one sorted message per violating package, relative to [sourceRoot]; empty if all comply
     */
    fun violations(sourceRoot: File): List<String> {
        if (!sourceRoot.isDirectory) return emptyList()
        return sourceRoot
            .walkTopDown()
            .filter { it.isDirectory }
            .filter { dir ->
                dir.listFiles { f -> f.isFile && f.extension == "java" && f.name !in metadataFiles }
                    ?.isNotEmpty() == true
            }
            .mapNotNull { dir -> problem(sourceRoot, dir) }
            .sorted()
            .toList()
    }

    private fun problem(sourceRoot: File, dir: File): String? {
        val name = dir.relativeTo(sourceRoot).invariantSeparatorsPath
        val info = File(dir, "package-info.java")
        if (!info.isFile) return "$name: package-info.java is missing"
        val code = info.readText().replace(blockComment, "").replace(lineComment, "")
        val marked = annotationLine.containsMatchIn(code) && importLine.containsMatchIn(code)
        return if (marked) null else "$name: package-info.java does not declare @NullMarked"
    }
}
