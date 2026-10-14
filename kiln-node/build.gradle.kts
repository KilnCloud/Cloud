plugins {
    application
    id("kiln.java-cloud")
    id("kiln.quality")
}

application {
    mainModule = "at.kilncloud.node"
    mainClass = "at.kilncloud.node.internal.KilnNode"
}

dependencies {
    implementation(project(":kiln-api"))
    implementation(project(":kiln-common"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}
