// Protobuf and gRPC code generation for modules that define wire contracts (kiln-protocol).
// Generated code is compiled and shipped, but it is neither formatted, analysed, documented nor
// counted for coverage: it is not ours to fix (AGENTS.md §13). Hand-written code in the same module
// stays fully covered by every gate of `kiln.quality`.
import at.kilncloud.buildlogic.GeneratedClassPatterns
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.protobuf

plugins {
    id("kiln.quality")
    id("com.google.protobuf")
}

val libs = the<VersionCatalogsExtension>().named("libs")
val protobufVersion = libs.findVersion("protobuf").get().requiredVersion
val grpcVersion = libs.findVersion("grpc").get().requiredVersion

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:$protobufVersion" }
    plugins { id("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion" } }
    generateProtoTasks { all().configureEach { plugins { id("grpc") } } }
}

val generatedRoot = layout.buildDirectory.dir("generated/sources/proto/main")

// Class-file patterns of everything that was generated; computed when a task runs, after the
// code generation, from the generated sources themselves.
val generatedClassPatterns: Provider<List<String>> =
    provider { GeneratedClassPatterns.excludePatterns(generatedRoot.get().asFile.resolve("java")) +
        GeneratedClassPatterns.excludePatterns(generatedRoot.get().asFile.resolve("grpc")) }

// Javadoc and the format check only cover hand-written sources.
tasks.withType<Javadoc>().configureEach {
    source = fileTree(layout.projectDirectory.dir("src/main/java"))
}

tasks.jacocoTestCoverageVerification {
    classDirectories.setFrom(
        files(provider { sourceSets.main.get().output.classesDirs.map { dir -> fileTree(dir) { exclude(generatedClassPatterns.get()) } } }))
}

tasks.jacocoTestReport {
    classDirectories.setFrom(
        files(provider { sourceSets.main.get().output.classesDirs.map { dir -> fileTree(dir) { exclude(generatedClassPatterns.get()) } } }))
}

// SpotBugs analyses hand-written classes only; its input is computed after compilation.
tasks.named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsMain") {
    // The filtered class set hides the link to compilation, so declare it explicitly.
    dependsOn(tasks.named("classes"))
    classes = files(provider { sourceSets.main.get().output.classesDirs.map { dir -> fileTree(dir) { exclude(generatedClassPatterns.get()) } } })
}
