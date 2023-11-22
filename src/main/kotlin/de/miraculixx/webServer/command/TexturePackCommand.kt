package de.miraculixx.webServer.command

import de.miraculixx.kpaper.localization.msg
import de.miraculixx.webServer.events.TexturePackEvent
import de.miraculixx.webServer.utils.SettingsManager.groupFolders
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.jvm.optionals.getOrNull

class TexturePackCommand {
    val command = commandTree("texturepack") {
        withAliases("resourcepack")
        stringArgument("group", optional = true) {
            replaceSuggestions(ArgumentSuggestions.stringCollection { groupFolders })
            playerExecutor { player, args ->
                val group = args.getOptional(0).getOrNull() as? String ?: groupFolders.firstOrNull() ?: "unknown"
                CoroutineScope(Dispatchers.Default).launch {
                    TexturePackEvent.loadTP(group, player, groupFolders)
                }
                player.sendMessage(prefix + msg("command.resourcepack.sent", listOf(group)))
            }
        }
    }
}