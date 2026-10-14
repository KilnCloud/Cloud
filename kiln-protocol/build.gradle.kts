plugins {
    `java-library`
    id("kiln.java-compat")
    id("kiln.protobuf")
}

description = "KilnCloud wire contracts between master, nodes and bridges"

dependencies {
    // Generated stubs and messages are part of the public surface of this module.
    api(libs.grpc.api)
    api(libs.grpc.protobuf)
    api(libs.grpc.stub)
    api(libs.protobuf.java)
}

// No module-info: gRPC and Protobuf are automatic modules, generated code needs more of them
// (Guava), and the bridges run without JPMS anyway (ADR 0003). A stable automatic module name keeps
// `requires at.kilncloud.protocol` working for the Cloud modules.
tasks.jar {
    manifest {
        attributes["Automatic-Module-Name"] = "at.kilncloud.protocol"
    }
}
