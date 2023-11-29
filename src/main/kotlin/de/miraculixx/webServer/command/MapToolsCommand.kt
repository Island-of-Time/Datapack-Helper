package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.bukkit.addUrl
import de.miraculixx.kpaper.extensions.kotlin.enumOf
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.kpaper.localization.msgString
import de.miraculixx.webServer.Main
import de.miraculixx.webServer.utils.*
import de.miraculixx.webServer.utils.data.Modules
import dev.jorel.commandapi.StringTooltip
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.stringArgument
import org.bukkit.command.CommandSender

class MapToolsCommand {
    private val command = commandTree("maptools") {

        anyExecutor { sender, _ ->
            sender.sendMessage(prefix + cmp("Version: ") + cmp(Main.INSTANCE.description.version, cHighlight) + if (tooling) cmp(" (Supporter Version)", cMark) else cmp(" (free version)", cError))
            if (tooling) {
                sender.sendMessage(prefix + cmp("Thank you for supporting us! You can use every supporter module and change the content of any header file to apply your own style!"))
            } else {
                sender.sendMessage(prefix + cmp("In the free version you can not use any supporter module (Animation, NameTag, Resizer) and can not change the content of any header/pack file. " +
                        "I spend a lot time into creating this plugin, i'm thankful for every support ♥"))
                sender.sendMessage(prefix + cmp("Get Supporter Version", cMark, underlined = true).addUrl("https://mutils.net/maptools/supporter"))
            }
            sender.sendMessage(prefix + cmp("Support: ") + cmp("https://dc.mutils.net", cMark, underlined = true).addUrl("https://dc.mutils.net"))
        }

        literalArgument("modules") {
            withPermission("maptools.maptools")
            stringArgument("module") {
                replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsCollection { Modules.entries.map { StringTooltip.ofString(it.name, msgString("info.${it.name}.desc")) } })
                anyExecutor { sender, args ->
                    val module = getModule(sender, args) ?: return@anyExecutor
                    sender.sendMessage(prefix + msg("info.${module.name}.desc"))
                    sender.sendMessage(cmp("") + msg("info.${module.name}.guide"))
                }
                literalArgument("disable") {
                    anyExecutor { sender, args ->
                        val module = getModule(sender, args) ?: return@anyExecutor
                        if (module == Modules.CORE) {
                            sender.sendMessage(prefix + cmp("Core can not be disabled!", cError))
                            return@anyExecutor
                        }
                        val state = SettingsManager.getModuleState(module)
                        if (!state) {
                            sender.sendMessage(prefix + msg("command.main.alreadyOff", listOf(module.name)))
                            return@anyExecutor
                        }
                        SettingsManager.disableModule(module)
                        sender.sendMessage(prefix + msg("command.main.turnedOff", listOf(module.name)))
                    }
                }
                literalArgument("enable") {
                    anyExecutor { sender, args ->
                        val module = getModule(sender, args) ?: return@anyExecutor
                        val state = SettingsManager.getModuleState(module)
                        if (state) {
                            sender.sendMessage(prefix + msg("command.main.alreadyOn", listOf(module.name)))
                            return@anyExecutor
                        }
                        if (module.isSupporter && !tooling) {
                            sender.sendMessage(prefix + cmp("This module is only for supporter!", cError))
                            return@anyExecutor
                        }
                        SettingsManager.enableModule(module)
                        sender.sendMessage(prefix + msg("command.main.turnedOn", listOf(module.name)))
                    }
                }
            }
        }

        literalArgument("config") {
            withPermission("maptools.maptools")
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
            withPermission("maptools.maptools")
            anyExecutor { sender, _ ->
                sender.sendMessage(prefix + cmp("Current language: ${SettingsManager.pluginSettings.language}"))
            }
            stringArgument("lang") {
                replaceSuggestions(ArgumentSuggestions.stringCollection { SettingsManager.getLoadedLangs() })
                anyExecutor { sender, args ->
                    val lang = args[0] as String

                    if (SettingsManager.localization.setLanguage(lang)) {
                        SettingsManager.pluginSettings.language = lang
                        sender.sendMessage(prefix + cmp("New Language: ") + cmp(lang, cMark))
                    } else {
                        sender.sendMessage(prefix + cmp("Failed to apply language!", cError))
                    }
                }
            }
        }
    }

    private fun getModule(sender: CommandSender, args: CommandArguments): Modules? {
        val moduleS = args[0] as String
        val module = enumOf<Modules>(moduleS)
        return if (module == null) {
            sender.sendMessage(prefix + msg("command.main.noModule", listOf(moduleS)))
            null
        } else module
    }
}