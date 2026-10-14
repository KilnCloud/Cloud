// Static analysis and quality gates for every KilnCloud module (AGENTS.md §7.8, §13, §15, §17).
// None of these gates may be weakened or switched off to get a green build (AGENTS.md §18).
import at.kilncloud.buildlogic.NullMarkedPackages
import at.kilncloud.buildlogic.ProductionBytecode
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("kiln.java-base")
    id("com.diffplug.spotless")
    id("com.github.spotbugs")
    id("net.ltgt.errorprone")
    id("jacoco")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    add("spotbugsPlugins", libs.findLibrary("findsecbugs-plugin").get())
    add("errorprone", libs.findLibrary("errorprone-core").get())
    add("errorprone", libs.findLibrary("nullaway").get())
    // Nullness annotations (@NullMarked, @Nullable) for modules; not a runtime dependency.
    add("compileOnly", libs.findLibrary("jspecify").get())
    // Libraries such as gRPC reference these annotations; without them javac warns (-Werror).
    add("compileOnly", libs.findLibrary("errorprone-annotations").get())
}

// --- Formatting: Palantir Java Format via Spotless (AGENTS.md §15) ---
spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat(libs.findVersion("palantir-java-format").get().requiredVersion)
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// --- Error Prone + NullAway (AGENTS.md §15) ---
tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        // Generated code (protobuf, jOOQ) is not ours to fix.
        excludedPaths = ".*/build/generated/.*"
        // Packages are treated as @NullMarked; NullAway enforces the null contract as an error.
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "at.kilncloud")
        option("NullAway:JSpecifyMode", "true")
    }
}

// --- @NullMarked on every package with code (AGENTS.md §15) ---
val verifyNullMarked =
    tasks.register("verifyNullMarked") {
        group = "verification"
        description = "Fails if a package with production code is not marked @NullMarked."
        val sourceRoot = layout.projectDirectory.dir("src/main/java")
        inputs.dir(sourceRoot).optional()
        doLast {
            val violations = NullMarkedPackages.violations(sourceRoot.asFile)
            if (violations.isNotEmpty()) {
                throw GradleException(
                    "Packages without @NullMarked (add it to package-info.java):\n" +
                        violations.joinToString("\n") { "  - $it" })
            }
        }
    }

tasks.check {
    dependsOn(verifyNullMarked)
}

// --- SpotBugs + FindSecBugs ---
// SpotBugs cannot analyse a source set without production bytecode (module-info/package-info
// only, e.g. a module that has no code yet). Skip explicitly instead of failing, and only then.
tasks.named("spotbugsMain") {
    onlyIf("module contains production bytecode beyond module and package metadata") {
        ProductionBytecode.existsIn(layout.buildDirectory.dir("classes/java/main").get().asFile)
    }
}

// --- JaCoCo: at least 90 % line and branch coverage per module (AGENTS.md §13) ---
jacoco {
    toolVersion = libs.findVersion("jacoco").get().requiredVersion
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.90".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

// Gradle silently skips the coverage verification when no execution data exists. That would let a
// module with production code but no tests pass the gate, so fail explicitly in that case.
val requireCoverageData =
    tasks.register("requireCoverageData") {
        group = "verification"
        description = "Fails if production code exists but the tests produced no coverage data."
        dependsOn(tasks.test)
        val classesDir = layout.buildDirectory.dir("classes/java/main")
        val execFile = layout.buildDirectory.file("jacoco/test.exec")
        doLast {
            if (ProductionBytecode.existsIn(classesDir.get().asFile) &&
                !execFile.get().asFile.isFile
            ) {
                throw GradleException(
                    "Production code exists but no coverage data was recorded: add tests (AGENTS.md §13).")
            }
        }
    }

tasks.jacocoTestCoverageVerification {
    dependsOn(requireCoverageData)
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
