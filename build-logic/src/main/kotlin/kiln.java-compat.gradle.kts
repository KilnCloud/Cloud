// Conventions for modules that run on Paper/Velocity/BungeeCord from Minecraft 1.21.1:
// kiln-api, bridges and feature modules. Bytecode release 21.
import at.kilncloud.buildlogic.KilnJavaExtension

plugins {
    id("kiln.java-base")
}

the<KilnJavaExtension>().release.set(21)
