plugins {
    `java-library`
    id("kiln.java-cloud")
    id("kiln.quality")
}

dependencies {
    implementation(project(":kiln-api"))
}
