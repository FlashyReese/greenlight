plugins {
    id("java")
    id("idea")
    id("net.fabricmc.fabric-loom-remap") version ("1.17.13")
}

val MINECRAFT_VERSION: String by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra
val FABRIC_API_VERSION: String by rootProject.extra
val ARCHIVE_NAME: String by rootProject.extra

base {
    archivesName.set("$ARCHIVE_NAME-testmod")
}

dependencies {
    minecraft("com.mojang:minecraft:${MINECRAFT_VERSION}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")

    fun fabricModule(name: String) = modImplementation(fabricApi.module(name, FABRIC_API_VERSION))
    fabricModule("fabric-api-base")
    fabricModule("fabric-networking-api-v1")
    fabricModule("fabric-resource-loader-v0")

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
            displayName.set("Testmod Client")
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

    processResources {
        from(project(":api").sourceSets.main.get().resources)
        from(project(":common").sourceSets.main.get().resources)
        inputs.property("version", modVersion)
        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to modVersion))
        }
    }
}
