plugins {
    application
    id("kiln.java-cloud")
    id("kiln.quality")
}

application {
    mainModule = "at.kilncloud.master"
    mainClass = "at.kilncloud.master.internal.KilnMaster"
}

dependencies {
    implementation(project(":kiln-api"))
    implementation(project(":kiln-common"))
    implementation(project(":kiln-protocol"))
    implementation(project(":kiln-setup"))
    // Strict JSON configuration (SecureConfig) reads the master config record reflectively.
    implementation(libs.jackson.databind)

    // In-memory file systems make permission and symlink tests identical on every OS.
    testImplementation(libs.jimfs)
    // Netty is bundled and relocated, which avoids a separate Netty dependency tree.
    implementation(libs.grpc.netty.shaded)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}
