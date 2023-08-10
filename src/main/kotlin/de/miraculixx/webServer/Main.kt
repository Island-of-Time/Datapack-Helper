package de.miraculixx.webServer

import de.miraculixx.kpaper.main.KPaper
import de.miraculixx.webServer.command.*
import de.miraculixx.webServer.events.*
import de.miraculixx.webServer.utils.consoleSender
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig

class Main : KPaper() {
    companion object {
        lateinit var INSTANCE: KPaper
    }

    private lateinit var warpCommand: WarpCommand

    override fun load() {
        INSTANCE = this
        consoleSender = server.consoleSender

        CommandAPI.onLoad(CommandAPIBukkitConfig(this).silentLogs(true))
        warpCommand = WarpCommand()
        LeashCommand()
        ConvertBlockCommand()
        NewMessage()
        MessageOverview()
        TexturePackCommand()
        AnimationCommand()
        PathingCommand()
        ReloadDataPackCommand()
        MarkerCommand()
        MultiToolCommand()
    }

    override fun startup() {
        CommandAPI.onEnable()

        LeashEvent()
        ConvertorEvent()
        NameTagEvent()
        CommandPreprocess()
        TexturePackEvent
    }

    override fun shutdown() {
        warpCommand.saveFile()
        CommandAPI.onDisable()
    }
}