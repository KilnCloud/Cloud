pluginManagement {
    // Convention plugins (build-logic) are resolved from the included build, never from a registry.
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "KilnCloud"

include("kiln-api")
include("kiln-common")
include("kiln-master")
include("kiln-node")
include("kiln-protocol")
include("kiln-setup")
