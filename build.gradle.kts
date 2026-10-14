plugins {
    base
}

group = "at.kilncloud"
version = "1.0-SNAPSHOT"

allprojects {
    group = rootProject.group
    version = rootProject.version
}

tasks.named("build") {
    dependsOn(subprojects.map { "${it.path}:build" })
}

tasks.named("clean") {
    dependsOn(subprojects.map { "${it.path}:clean" })
}
