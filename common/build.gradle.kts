import net.fabricmc.loom.task.AbstractRemapJarTask

plugins {
    id("java-library")
    id("idea")
    id("net.fabricmc.fabric-loom") version "1.17.11"
}

val MINECRAFT_VERSION: String by rootProject.extra
val FABRIC_LOADER_VERSION: String by rootProject.extra

repositories {
    mavenCentral() // JUnit
}

dependencies {
    minecraft("com.mojang:minecraft:$MINECRAFT_VERSION")
    compileOnly("net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")
    api(project(":api"))

    // Unit tests run headless against the named Minecraft jar loom puts on the classpath,
    // so they can reference Identifier, GsonHelper, etc. without launching the game.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.13.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

tasks.withType<AbstractRemapJarTask>().forEach {
    it.targetNamespace = "named"
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
}
