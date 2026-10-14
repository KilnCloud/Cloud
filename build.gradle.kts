plugins {
    base
}

group = "at.kilncloud"
version = "1.0-SNAPSHOT"

allprojects {
    group = rootProject.group
    version = rootProject.version
}

val buildLogic = gradle.includedBuild("build-logic")

tasks.named("build") {
    dependsOn(subprojects.map { "${it.path}:build" })
    dependsOn(buildLogic.task(":build"))
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
