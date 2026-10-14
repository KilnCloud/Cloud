// Static analysis and quality gates for every KilnCloud module (AGENTS.md §7.8, §13, §15, §17).
plugins {
    id("kiln.java-base")
    id("com.github.spotbugs")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    add("spotbugsPlugins", libs.findLibrary("findsecbugs-plugin").get())
}

// SpotBugs cannot analyse a source set without production bytecode (module-info/package-info
// only, e.g. a module that has no code yet). Skip explicitly instead of failing, and only then.
tasks.named("spotbugsMain") {
    onlyIf("module contains production bytecode beyond module and package metadata") {
        fileTree(layout.buildDirectory.dir("classes/java/main")) {
                include("**/*.class")
                exclude("module-info.class", "**/package-info.class")
            }
            .files
            .isNotEmpty()
    }
}
