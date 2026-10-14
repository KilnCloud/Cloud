plugins {
    `java-library`
    id("kiln.java-cloud")
    id("kiln.quality")
}

description = "KilnCloud setup: certificate authority, mTLS identities and join tokens"

dependencies {
    // NodeName appears in public signatures, so the module requires it transitively.
    api(project(":kiln-common"))
    // The JDK has no public API for creating X.509 certificates (ADR 0004).
    implementation(libs.bouncycastle.pkix)
    implementation(libs.bouncycastle.provider)

    // In-memory file systems make permission and symlink tests identical on every OS.
    testImplementation(libs.jimfs)
}
