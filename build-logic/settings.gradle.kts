// Included build that provides the KilnCloud convention plugins (AGENTS.md §3).
rootProject.name = "build-logic"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        // Single source of truth for versions: the same catalog the product modules use.
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}
