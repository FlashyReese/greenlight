rootProject.name = "greenlight"

pluginManagement {
    repositories {
        maven { url = uri("https://maven.fabricmc.net/") }
        gradlePluginPortal()
    }
}

include("common")
include("api")
include("fabric")
include("testmod")
