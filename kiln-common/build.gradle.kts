plugins {
    `java-library`
    id("kiln.java-cloud")
    id("kiln.quality")
}

dependencies {
    implementation(project(":kiln-api"))
    // JSON for configuration files; strictly configured in SecureConfig, never exposed in signatures.
    implementation(libs.jackson.databind)

    // In-memory Unix and Windows file systems make permission and symlink tests identical on every OS.
    testImplementation(libs.jimfs)
}
