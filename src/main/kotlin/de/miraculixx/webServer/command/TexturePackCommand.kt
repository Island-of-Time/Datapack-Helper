package de.miraculixx.webServer.command

import de.miraculixx.webServer.events.TexturePackEvent.loadPlayer1
import de.miraculixx.webServer.events.TexturePackEvent.loadPlayer2
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor

class TexturePackCommand {
    val command = commandTree("texturepack") {
        literalArgument("player1") {
            playerExecutor { player, _ -> player.loadPlayer1() }
        }
        literalArgument("player2") {
            playerExecutor { player, _ -> player.loadPlayer2() }
        }
    }
}