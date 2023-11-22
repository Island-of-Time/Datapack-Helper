package de.miraculixx.webServer.command

import de.miraculixx.webServer.utils.SettingsManager
import de.miraculixx.webServer.utils.cmp
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument

class MapToolsCommand {
    val command = commandTree("maptools") {
        withPermission("buildertools.maptools")

        literalArgument("config") {
            literalArgument("reload") {
                anyExecutor { sender, _ ->
                    SettingsManager.reload()
                    sender.sendMessage(prefix + cmp("Reloaded all configurations"))
                }
            }
            literalArgument("save") {
                anyExecutor { sender, _ ->
                    SettingsManager.save()
                    sender.sendMessage(prefix + cmp("Saved all persistent data to disk"))
                }
            }
        }

        literalArgument("language") {
            anyExecutor { sender, _ ->
                sender.sendMessage(prefix + cmp("Current language: $"))
            }
        }
    }
}