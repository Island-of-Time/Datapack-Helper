package de.miraculixx.maptools

import de.miraculixx.kpaper.extensions.pluginManager
import de.miraculixx.kpaper.main.KPaper
import de.miraculixx.kpaper.main.KPaperConfiguration
import de.miraculixx.mweb.api.MWebAPI
import de.miraculixx.maptools.command.MapToolsCommand
import de.miraculixx.maptools.command.ReloadDataPackCommand
import de.miraculixx.maptools.command.SyncCommand
import de.miraculixx.maptools.events.ToolEvent
import de.miraculixx.maptools.utils.SettingsManager
import de.miraculixx.maptools.utils.consoleSender
import de.miraculixx.maptools.utils.prefix
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig

class MapTools : KPaper() {
    companion object {
        lateinit var INSTANCE: KPaper
        lateinit var mWebAPI: MWebAPI
        var mWebLoaded = false
    }

    override fun load() {
        INSTANCE = this
        consoleSender = server.consoleSender
        CommandAPI.onLoad(CommandAPIBukkitConfig(this).silentLogs(true))
        KPaperConfiguration.Text.prefix = prefix
    }

    override fun startup() {
        if (pluginManager.isPluginEnabled("MUtils-Web")) {
            mWebLoaded = true
            mWebAPI = MWebAPI.INSTANCE ?: throw ClassNotFoundException("Failed to load MWeb API while it's loaded. Did you reloaded your server?")
        }
        CommandAPI.onEnable()

        // Core Modules
        ReloadDataPackCommand()
        MapToolsCommand()
        ToolEvent

        // All Modules
        SettingsManager
        SyncCommand.load()
    }

    override fun shutdown() {
        SettingsManager.save()
        CommandAPI.onDisable()
    }
}
