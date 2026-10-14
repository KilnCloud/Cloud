// Conventions for Cloud modules (master, node, launcher, ...): bytecode release 25.
plugins {
    id("kiln.java-base")
}

the<KilnJavaExtension>().release.set(25)
