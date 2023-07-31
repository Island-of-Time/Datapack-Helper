import java.net.URI

plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.10"
    id("io.papermc.paperweight.userdev") version "1.5.0"
    id("xyz.jpenilla.run-paper") version "1.1.0"
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
}

dependencies {
    paperweight.paperDevBundle("1.20.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")
    implementation("org.zeroturnaround:zt-zip:1.15")

    implementation("dev.jorel:commandapi-bukkit-shade:9.0.3")
    implementation("dev.jorel:commandapi-bukkit-kotlin:9.0.3")

    implementation("commons-codec:commons-codec:1.15")
    implementation("de.miraculixx:kpaper:1.1.0")
    implementation("net.wesjd:anvilgui:1.7.0-SNAPSHOT")
    compileOnly("de.miraculixx:mweb:1.1.0")
}

tasks {
    assemble {
        dependsOn(shadowJar)
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