package de.miraculixx.webServer.command

import de.miraculixx.webServer.utils.gui.GUITypes
import de.miraculixx.webServer.utils.gui.actions.LanguageOverviewAction
import de.miraculixx.webServer.utils.gui.buildInventory
import de.miraculixx.webServer.utils.gui.items.LanguageOverview
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import kotlinx.serialization.Serializable
import java.io.File

class MessageOverview {
    private val datapackFile = File("world/datapacks/iot-chat/data")

    val command = commandTree("translations") {
        literalArgument("overview") {
            playerExecutor { player, args ->
                GUITypes.LOCALIZATION.buildInventory(player, "${player.uniqueId}", LanguageOverview(), LanguageOverviewAction())
            }
        }
        literalArgument("missing") {

        }
    }

    @Serializable
    data class FunctionInfo(
        val text: String,
        val prefix: String,
        val speed: Int,
        val target: String
    )
}