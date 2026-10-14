plugins {
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    modularity.inferModulePath = true
    withJavadocJar()
    withSourcesJar()
}

application {
    mainModule = "at.kilncloud.node"
    mainClass = "at.kilncloud.node.internal.KilnNode"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":kiln-api"))
    implementation(project(":kiln-common"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
    options.encoding = "UTF-8"
    options.release = 25
}

tasks.withType<Javadoc>().configureEach {
    isFailOnError = true
    options.encoding = "UTF-8"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}
