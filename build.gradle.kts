import java.net.URI

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}


group = "de.miraculixx"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/central")
}

dependencies {
    compileOnly("org.spigotmc:spigot:1.20.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")
    compileOnly("org.zeroturnaround:zt-zip:1.15")

    compileOnly("dev.jorel:commandapi-bukkit-shade:9.2.0")
    compileOnly("dev.jorel:commandapi-bukkit-kotlin:9.2.0")

    compileOnly("commons-codec:commons-codec:1.15")
    compileOnly("de.miraculixx:kpaper:1.1.1")
    compileOnly("de.miraculixx:mweb:1.1.0")

    val adventureVersion = "4.13.1"
    compileOnly("net.kyori:adventure-api:$adventureVersion")
    compileOnly("net.kyori:adventure-text-minimessage:$adventureVersion")
    compileOnly("net.kyori:adventure-text-serializer-plain:$adventureVersion")
    compileOnly("net.kyori:adventure-text-serializer-gson:$adventureVersion")
    compileOnly("net.kyori:adventure-text-serializer-legacy:$adventureVersion")

    implementation("de.miraculixx:kpaper:1.1.1")
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
}