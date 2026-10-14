plugins {
    base
    alias(libs.plugins.spotbugs) apply false
}

group = "at.kilncloud"
version = "1.0-SNAPSHOT"

val findsecbugsPlugin = libs.findsecbugs.plugin

allprojects {
    group = rootProject.group
    version = rootProject.version
}

subprojects {
    apply(plugin = "com.github.spotbugs")

    dependencies {
        add("spotbugsPlugins", findsecbugsPlugin)
    }
}

tasks.named("build") {
    dependsOn(subprojects.map { "${it.path}:build" })
}

tasks.named("clean") {
    dependsOn(subprojects.map { "${it.path}:clean" })
}

tasks.register("spotbugsMain") {
    group = "verification"
    description = "Runs SpotBugs with FindSecBugs for every production source set."
    dependsOn(subprojects.map { "${it.path}:spotbugsMain" })
}

tasks.register("spotbugsTest") {
    group = "verification"
    description = "Runs SpotBugs with FindSecBugs for every test source set."
    dependsOn(subprojects.map { "${it.path}:spotbugsTest" })
}
