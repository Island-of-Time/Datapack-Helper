import dex.plugins.outlet.v2.util.ReleaseType
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("io.papermc.paperweight.userdev") version "1.7.+"
    id("xyz.jpenilla.run-paper") version "2.3.0"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("com.modrinth.minotaur") version "2.+"
    id("io.github.dexman545.outlet") version "1.6.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = properties["group"] as String
version = properties["version"] as String
description = properties["description"] as String

val gameVersion by properties
val foliaSupport = properties["foliaSupport"] as String == "true"
val projectName = properties["name"] as String

repositories {
    mavenCentral()
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweight.paperDevBundle("${gameVersion}-R0.1-SNAPSHOT")

    // Kotlin libraries
    library(kotlin("stdlib"))
    library("org.jetbrains.kotlinx:kotlinx-serialization-json:1.+")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.+")

    // Minecraft libraries
    library("de.miraculixx:kpaper:1.+")
    implementation("dev.jorel:commandapi-bukkit-shade-mojang-mapped:9.5.3")
    implementation("dev.jorel:commandapi-bukkit-kotlin:9.5.3")

    library("commons-codec:commons-codec:1.15")
    library("org.zeroturnaround:zt-zip:1.15")

    compileOnly("de.miraculixx:mweb:1.1.0")
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "21"
    }
    shadowJar {
        dependencies {
            include {
                it.moduleGroup == "de.miraculixx" || it.moduleGroup == "dev.jorel"
            }
        }
        relocate("dev.jorel.commandapi", "de.miraculixx.mchallenge.commandapi")
    }
}

bukkit {
    main = "$group.${projectName.lowercase()}.${projectName}"
    apiVersion = "1.16"
    foliaSupported = foliaSupport

    // Optionals
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    depend = listOf("MUtils-Web")
    softDepend = listOf()
}

modrinth {
    token.set(properties["modrinthToken"] as String)
    projectId.set(properties["modrinthProjectId"] as? String ?: projectName)
    versionNumber.set(version as String)
    versionName.set("$projectName - $version")
    versionType.set("release") // Can also be `beta` or `alpha`
    uploadFile.set(tasks.jar)
    outlet.mcVersionRange = properties["supportedVersions"] as String
    outlet.allowedReleaseTypes = setOf(ReleaseType.RELEASE)
    gameVersions.addAll(outlet.mcVersions())
    loaders.addAll(buildList {
        add("paper")
        add("purpur")
        if (foliaSupport) add("folia")
    })
    dependencies {
        // The scope can be `required`, `optional`, `incompatible`, or `embedded`
        // The type can either be `project` or `version`
        required.project("mweb")
    }

    // Project sync
    syncBodyFrom = rootProject.file("README.md").readText()
}