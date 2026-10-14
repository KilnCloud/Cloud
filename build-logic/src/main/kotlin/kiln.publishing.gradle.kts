// Maven publication of a module's Java component (AGENTS.md §10). Applied to published modules
// only (kiln-api first). No repository and no credentials are configured here: publishing targets
// and artifact signing are release concerns (M6/M8) and must never be hard-coded in the build.
plugins {
    id("kiln.java-base")
    id("maven-publish")
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = project.name
                description = provider { project.description ?: "KilnCloud module ${project.name}" }
                url = "https://github.com/KilnCloud/Cloud"
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                scm {
                    url = "https://github.com/KilnCloud/Cloud"
                    connection = "scm:git:https://github.com/KilnCloud/Cloud.git"
                }
            }
        }
    }
}
