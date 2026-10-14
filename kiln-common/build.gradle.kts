plugins {
    `java-library`
    id("kiln.java-cloud")
    id("kiln.quality")
}

dependencies {
    implementation(project(":kiln-api"))

    // In-memory Unix and Windows file systems make permission and symlink tests identical on every OS.
    testImplementation(libs.jimfs)
}
