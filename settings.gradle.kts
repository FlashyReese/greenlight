rootProject.name = "greenlight"

pluginManagement {
    repositories {
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.neoforged.net/releases/") }
        maven { url = uri("https://maven.minecraftforge.net/") }
        gradlePluginPortal()
    }
}

include("common")
include("api")
include("fabric")
include("neoforge")
include("forge")
include("testmod")
