plugins {
    id("java")
    id("idea")
    id("net.fabricmc.fabric-loom") version ("1.17.20")
}

val MINECRAFT_VERSION: String by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra
val ARCHIVE_NAME: String by rootProject.extra

base {
    archivesName.set("$ARCHIVE_NAME-fabric")
}

dependencies {
    minecraft("com.mojang:minecraft:${MINECRAFT_VERSION}")
    compileOnly("net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")
    runtimeOnly("net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")
    testCompileOnly("net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")

    compileOnly(project(":api"))
    compileOnly(project(":common"))
}

tasks.test {
    failOnNoDiscoveredTests = false
}

loom {
    runs {
        named("client") {
            client()
            displayName.set("Fabric Client")
            generateRunConfig.set(true)
            runDirectory.set(layout.projectDirectory.dir("run"))
        }
    }
}

val modVersion = project.version.toString()

tasks {
    withType<JavaCompile> {
        source(project(":api").sourceSets.main.get().allSource)
        source(project(":common").sourceSets.main.get().allSource)
    }

    javadoc {
        source(project(":api").sourceSets.main.get().allJava)
        source(project(":common").sourceSets.main.get().allJava)
    }

    processResources {
        from(project(":api").sourceSets.main.get().resources)
        from(project(":common").sourceSets.main.get().resources)

        inputs.property("version", modVersion)

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to modVersion))
        }
    }

    jar {
        from(rootDir.resolve("LICENSE"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
}
