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
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}
