package de.miraculixx.webServer.command

import de.miraculixx.webServer.events.TexturePackEvent
import de.miraculixx.webServer.utils.SettingsManager.groupFolders
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import kotlin.jvm.optionals.getOrNull

class TexturePackCommand {
    val command = commandTree("texturepack") {
        withAliases("resourcepack")
        stringArgument("group", optional = true) {
            replaceSuggestions(ArgumentSuggestions.stringCollection { groupFolders })
            playerExecutor { player, args ->
                println(groupFolders.toString())
                val group = args.getOptional(0).getOrNull() as? String ?: groupFolders.firstOrNull() ?: "unknown"
                TexturePackEvent.loadTP(group, player, groupFolders)
            }
        }
    }
}