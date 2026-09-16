import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

plugins {
    id("java")
    id("net.fabricmc.fabric-loom") version ("1.17.20") apply (false)
}

val MINECRAFT_VERSION by extra { "26.3" }
val NEOFORGE_VERSION by extra { "26.3.0.1-beta" }
val FABRIC_LOADER_VERSION by extra { "0.19.5" }
val FABRIC_API_VERSION by extra { "0.160.5+26.3" } // testmod only

val MAVEN_GROUP by extra { "me.flashyreese.mods" }
val ARCHIVE_NAME by extra { "greenlight" }
val MOD_VERSION by extra { "0.1.0" }

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    group = MAVEN_GROUP
    version = createVersionString()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

subprojects {
    val modVersion = createVersionString()

    apply(plugin = "maven-publish")

    repositories {
        maven("https://maven.parchmentmc.org/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://libraries.minecraft.net")
    }

    base {
        archivesName = "$ARCHIVE_NAME-${project.name}"
    }

    java.toolchain.languageVersion = JavaLanguageVersion.of(25)

    version = modVersion
    group = "me.flashyreese.mods"

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    tasks.withType<GenerateModuleMetadata>().configureEach {
        enabled = false
    }

    extensions.configure<PublishingExtension>("publishing") {
        repositories {
            maven {
                name = "FlashyReeseSnapshots"
                url = uri("https://maven.flashyreese.me/snapshots")
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
            maven {
                name = "FlashyReeseReleases"
                url = uri("https://maven.flashyreese.me/releases")
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        onlyIf {
            when (repository.name) {
                "FlashyReeseSnapshots" -> project.version.toString().contains("snapshot", ignoreCase = true)
                "FlashyReeseReleases" -> !project.version.toString().contains("snapshot", ignoreCase = true)
                else -> true
            }
        }
    }
}

fun createVersionString(): String {
    val builder = StringBuilder()

    val isReleaseBuild = project.hasProperty("build.release")
    val buildId = System.getenv("GITHUB_RUN_NUMBER")

    if (isReleaseBuild) {
        builder.append(MOD_VERSION)
    } else {
        builder.append(MOD_VERSION.split('-')[0])
        builder.append("-snapshot")
    }

    builder.append("+mc").append(MINECRAFT_VERSION)

    if (!isReleaseBuild) {
        if (buildId != null) {
            builder.append("-build.$buildId")
        } else {
            builder.append("-local")
        }
    }

    return builder.toString()
}
