package de.miraculixx.webServer.command

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.register
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.webServer.Main
import de.miraculixx.webServer.interfaces.Module
import de.miraculixx.webServer.utils.SettingsManager
import de.miraculixx.webServer.utils.SettingsManager.groupFolders
import de.miraculixx.webServer.utils.Zipping
import de.miraculixx.webServer.utils.extensions.command
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import java.io.File
import kotlin.jvm.optionals.getOrNull

class TexturePackCommand : Module {
    private val mWebAPI = if (Main.mWebLoaded) Main.mWebAPI else null

    private val command = command("texturepack") {
        withAliases("resourcepack")
        stringArgument("group", optional = true) {
            replaceSuggestions(ArgumentSuggestions.stringCollection { groupFolders })
            playerExecutor { player, args ->
                val group = args.getOptional(0).getOrNull() as? String ?: groupFolders.firstOrNull() ?: "unknown"
                CoroutineScope(Dispatchers.Default).launch {
                    loadTP(group, player, groupFolders)
                }
                player.sendMessage(prefix + msg("command.resourcepack.sent", listOf(group)))
            }
        }
    }

    private fun loadTP(secondFolderName: String, target: Player, groups: List<String>) {
        val tpFolder = File(SettingsManager.texturePackFolder)
        val mixFolder = File(tpFolder, ".mixed")
        val secondFolder = File(tpFolder, secondFolderName)
        if (!secondFolder.exists()) secondFolder.mkdir()
        if (!mixFolder.exists()) mixFolder.mkdir()
        val zipTarget = File(mixFolder, "${secondFolder.name}.zip")
        if (zipTarget.exists()) zipTarget.delete()
        val folders = tpFolder.listFiles()?.filter { file ->
            val name = file.name
            !name.startsWith('.') && !groups.contains(name)
        }?.toTypedArray() ?: emptyArray()
        Zipping.zipFolder(listOf(*folders, secondFolder), zipTarget)

        mWebAPI?.sendFileAsResourcePack(zipTarget.path, setOf(target.uniqueId), false)
    }

    private val playerJoin = listen<PlayerJoinEvent>(register = false) {
        if (mWebAPI == null) return@listen
        val player = it.player
        taskRunLater(20) {
            val groups = groupFolders
            player.scoreboardTags.forEach { tag ->
                if (!groups.contains(tag)) return@forEach
                CoroutineScope(Dispatchers.Default).launch {
                    loadTP(tag, player, groupFolders)
                }
                return@taskRunLater
            }
        }
    }

    override fun disable() {
        command.register()
        playerJoin.register()
    }

    override fun enable() {
        command.register()
        playerJoin.unregister()
    }
}