// Conventions for Cloud modules (master, node, launcher, ...): bytecode release 25.
import at.kilncloud.buildlogic.KilnJavaExtension

plugins {
    id("kiln.java-base")
}

the<KilnJavaExtension>().release.set(25)
