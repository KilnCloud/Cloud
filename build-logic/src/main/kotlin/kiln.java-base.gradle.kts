// Shared Java conventions for every KilnCloud module (AGENTS.md §2, §3, §15).
// Do not apply directly; use `kiln.java-cloud` or `kiln.java-compat`.
import at.kilncloud.buildlogic.KilnJavaExtension

plugins {
    `java`
}

val kilnJava = extensions.create<KilnJavaExtension>("kilnJava")

java {
    toolchain {
        // JDK 25 builds everything; the target level is controlled by --release below.
        languageVersion = JavaLanguageVersion.of(25)
    }
    modularity.inferModulePath = true
    withJavadocJar()
    withSourcesJar()
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
    options.encoding = "UTF-8"
    options.release.set(kilnJava.release)
}

tasks.withType<Javadoc>().configureEach {
    isFailOnError = true
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:all", "-quiet")
        // Javadoc must run without warnings (AGENTS.md §14); doclint findings are only warnings.
        addBooleanOption("Werror", true)
    }
}

tasks.test {
    useJUnitPlatform()
}

// Reproducible archives: no timestamps, stable order (supply-chain hygiene, AGENTS.md §7.7).
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
