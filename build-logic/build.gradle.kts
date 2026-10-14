plugins {
    `kotlin-dsl`
}

dependencies {
    // Plugin artifacts that the precompiled convention scripts apply.
    implementation(libs.errorprone.gradle.plugin)
    implementation(libs.spotbugs.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)

    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // Functional tests resolve dependencies offline from the real Gradle user home, i.e. from the
    // artifacts that dependency verification already approved for this build.
    systemProperty("kiln.gradleUserHome", gradle.gradleUserHomeDir.absolutePath)
    systemProperty("kiln.versionCatalog", rootDir.resolve("../gradle/libs.versions.toml").canonicalPath)
}
