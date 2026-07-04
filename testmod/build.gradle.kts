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
    fabricModule("fabric-client-gametest-api-v1")
    // The 1.21.10 client-gametest module still depends on the v0 resource loader at runtime.
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
        // Headless end-to-end test: boots a real client, stands up a dedicated server with the
        // sample required pack, connects, and asserts Greenlight sees the grant. Run under a
        // virtual display in CI (e.g. xvfb-run ./gradlew :testmod:runGametestClient).
        create("gametestClient") {
            client()
            displayName.set("Client Gametest")
            generateRunConfig.set(true)
            runDirectory.set(layout.projectDirectory.dir("run/gametest"))
            // Enables the client-gametest harness (its ENABLED gate is just the presence of this
            // property); the .modid filter then scopes the run to this mod's test.
            vmArg("-Dfabric.client.gametest")
            vmArg("-Dfabric.client.gametest.modid=greenlight-testmod")
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
