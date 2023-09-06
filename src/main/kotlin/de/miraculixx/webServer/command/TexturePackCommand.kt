package de.miraculixx.webServer.command

import de.miraculixx.webServer.events.TexturePackEvent
import de.miraculixx.webServer.settings
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument

class TexturePackCommand {
    val command = commandTree("texturepack") {
        stringArgument("group") {
            replaceSuggestions(ArgumentSuggestions.stringCollection { settings.groupFolders })
            playerExecutor { player, args ->
                println(settings.groupFolders.toString())
                val group = args[0] as String
                TexturePackEvent.loadTP(group, player, settings.groupFolders)
            }
        }
    }
}