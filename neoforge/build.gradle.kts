plugins {
    id("idea")
    id("net.neoforged.moddev") version "2.0.141"
    id("java-library")
}

val MINECRAFT_VERSION: String by rootProject.extra
val NEOFORGE_VERSION: String by rootProject.extra
val ARCHIVE_NAME: String by rootProject.extra

base {
    archivesName = "$ARCHIVE_NAME-neoforge"
}

tasks.jar {
    from(rootDir.resolve("LICENSE"))
}

neoForge {
    version = NEOFORGE_VERSION

    runs {
        create("client") {
            client()
            ideName = "NeoForge/Client"
        }
    }

    mods {
        create(project.name) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":common"))
}

tasks.named("compileTestJava").configure {
    enabled = false
}

val modVersion = project.version.toString()

// NeoGradle compiles the game, but we don't want to add our common code to the game's code
val notNeoTask: (Task) -> Boolean = { it: Task ->
    !it.name.startsWith("neo") && !it.name.startsWith("compileService")
}

tasks.withType<JavaCompile>().matching(notNeoTask).configureEach {
    source(project(":api").sourceSets.main.get().allSource)
    source(project(":common").sourceSets.main.get().allSource)
}

tasks.withType<Javadoc>().matching(notNeoTask).configureEach {
    source(project(":api").sourceSets.main.get().allJava)
    source(project(":common").sourceSets.main.get().allJava)
}

tasks.withType<ProcessResources>().matching(notNeoTask).configureEach {
    from(project(":api").sourceSets.main.get().resources)
    from(project(":common").sourceSets.main.get().resources)
    inputs.property("version", modVersion)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(mapOf("version" to modVersion))
    }
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
}
