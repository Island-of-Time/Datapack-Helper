package de.miraculixx.webServer

import de.miraculixx.kpaper.extensions.pluginManager
import de.miraculixx.kpaper.main.KPaper
import de.miraculixx.mweb.api.MWebAPI
import de.miraculixx.webServer.command.*
import de.miraculixx.webServer.events.*
import de.miraculixx.webServer.utils.SettingsManager
import de.miraculixx.webServer.utils.consoleSender
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig

class Main : KPaper() {
    companion object {
        lateinit var INSTANCE: KPaper
        lateinit var settingsManager: SettingsManager
        lateinit var mWebAPI: MWebAPI
        var mWebLoaded = false
    }

    private lateinit var warpCommand: WarpCommand

    override fun load() {
        INSTANCE = this
        consoleSender = server.consoleSender
        settingsManager = SettingsManager()

        CommandAPI.onLoad(CommandAPIBukkitConfig(this).silentLogs(true))
        warpCommand = WarpCommand()
        LeashCommand()
        ConvertBlockCommand()
        NewMessage()
        TexturePackCommand()
        AnimationCommand()
        PathingCommand()
        ReloadDataPackCommand()
        MarkerCommand()
        MultiToolCommand()
        QuestBookCommand()
        HitBoxCommand()
        NameTagCommand()
    }

    override fun startup() {
        if (pluginManager.isPluginEnabled("MUtils-Web")) {
            mWebLoaded = true
            mWebAPI = MWebAPI.INSTANCE ?: throw ClassNotFoundException("Failed to load MWeb API while it's loaded. Did you reloaded your server?")
        }

        CommandAPI.onEnable()

        LeashEvent()
        ToolEvent
        NameTagEvent()
        CommandPreprocess()
        TexturePackEvent
    }

    override fun shutdown() {
        warpCommand.saveFile()
        CommandAPI.onDisable()
    }
}

val settings by lazy { Main.settingsManager }