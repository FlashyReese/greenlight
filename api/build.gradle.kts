import net.fabricmc.loom.task.AbstractRemapJarTask

plugins {
    id("java-library")
    id("idea")
    id("net.fabricmc.fabric-loom-remap") version "1.17.13"
}

val MINECRAFT_VERSION: String by rootProject.extra

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$MINECRAFT_VERSION")
    mappings(loom.officialMojangMappings())

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

base {
    archivesName = "greenlight-api"
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
