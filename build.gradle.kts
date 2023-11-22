import java.net.URI

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    id("io.papermc.paperweight.userdev") version "1.5.10"
    id("xyz.jpenilla.run-paper") version "2.2.2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}


group = "de.miraculixx"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        url = URI("https://repo.codemc.io/repository/maven-snapshots")
        name = "codemc-snapshots"
    }
    maven { url = URI("https://s01.oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    paperweight.paperDevBundle("1.20.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")
    compileOnly("org.zeroturnaround:zt-zip:1.15")

    compileOnly("dev.jorel:commandapi-bukkit-shade:9.2.0")
    compileOnly("dev.jorel:commandapi-bukkit-kotlin:9.2.0")

    compileOnly("commons-codec:commons-codec:1.15")
    compileOnly("de.miraculixx:kpaper:1.1.1")
    compileOnly("de.miraculixx:mweb:1.1.0")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
}