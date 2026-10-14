plugins {
    `kotlin-dsl`
}

dependencies {
    // Plugin artifacts that the precompiled convention scripts apply.
    implementation(libs.spotbugs.gradle.plugin)
}
