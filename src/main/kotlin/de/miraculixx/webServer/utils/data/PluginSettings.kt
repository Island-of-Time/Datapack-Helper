package de.miraculixx.webServer.utils.data

data class PluginSettings(
    var language: String = "en_US",
    var mainModules: Set<Modules> = emptySet(),
    var supporterModules: Set<Modules> = emptySet(),
    var experimentalModules: Set<Modules> = emptySet(),
)
