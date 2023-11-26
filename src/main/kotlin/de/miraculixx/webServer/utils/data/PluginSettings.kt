package de.miraculixx.webServer.utils.data

import de.miraculixx.webServer.utils.tooling
import kotlinx.serialization.Serializable

@Serializable
data class PluginSettings(
    var language: String = "en_US",
    var mainModules: MutableMap<Modules, Boolean> = buildMap { Modules.entries.filter { !it.isSupporter && !it.isExperimental }.forEach { put(it, true) } }.toMutableMap(),
    var supporterModules: MutableMap<Modules, Boolean> = buildMap { Modules.entries.filter { it.isSupporter }.forEach { put(it, tooling) } }.toMutableMap(),
    var experimentalModules: MutableMap<Modules, Boolean> = buildMap { Modules.entries.filter { it.isExperimental }.forEach { put(it, false) } }.toMutableMap()
)
