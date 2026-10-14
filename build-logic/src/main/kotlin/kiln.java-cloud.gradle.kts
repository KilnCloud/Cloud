// Conventions for Cloud modules (master, node, launcher, ...): bytecode release 25.
import at.kilncloud.buildlogic.KilnJavaExtension

plugins {
    id("kiln.java-base")
}

the<KilnJavaExtension>().release.set(25)

// gRPC, Protobuf and similar libraries ship no module descriptor and are used as automatic modules.
// javac warns about every `requires` of such a module ("requires-automatic"), which -Werror turns into
// an error. The names of the libraries in use are stable (they declare Automatic-Module-Name), so this
// one lint category is switched off for Cloud modules; every other -Xlint warning stays an error.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-requires-automatic")
}
