plugins {
    `kotlin-dsl`
}

dependencies {
    // Plugin artifacts that the precompiled convention scripts apply.
    implementation(libs.errorprone.gradle.plugin)
    implementation(libs.protobuf.gradle.plugin)
    implementation(libs.spotbugs.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)

    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // Functional tests resolve dependencies like the real build: from Maven Central, verified against
    // the pinned hashes in gradle/verification-metadata.xml. The Gradle user home is only a cache.
    systemProperty("kiln.gradleUserHome", gradle.gradleUserHomeDir.absolutePath)
    systemProperty("kiln.versionCatalog", rootDir.resolve("../gradle/libs.versions.toml").canonicalPath)
    systemProperty("kiln.verificationMetadata", rootDir.resolve("../gradle/verification-metadata.xml").canonicalPath)
}
