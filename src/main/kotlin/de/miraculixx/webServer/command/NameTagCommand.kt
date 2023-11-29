package de.miraculixx.webServer.command

import com.google.common.primitives.UnsignedInteger
import de.miraculixx.kpaper.chat.sendMessage
import de.miraculixx.kpaper.extensions.bukkit.addCommand
import de.miraculixx.kpaper.extensions.bukkit.addCopy
import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.kpaper.localization.msgString
import de.miraculixx.webServer.interfaces.DataHolder
import de.miraculixx.webServer.interfaces.Module
import de.miraculixx.webServer.utils.*
import de.miraculixx.webServer.utils.SettingsManager.settingsFolder
import de.miraculixx.webServer.utils.SettingsManager.texturePackFolder
import de.miraculixx.webServer.utils.extensions.command
import de.miraculixx.webServer.utils.extensions.unregister
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.textArgument
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.bukkit.command.CommandSender
import java.io.File
import kotlin.jvm.optionals.getOrNull

class NameTagCommand : Module, DataHolder {
    private val file = File(settingsFolder, "modules/nametag-inputs.json")
    private val lastUsedMain: MutableSet<String> = mutableSetOf()
    private val lastUsedShadow: MutableSet<String> = mutableSetOf()
    private val lastUsedCharShadow: MutableSet<String> = mutableSetOf()
    private val lastUsedChar: MutableSet<String> = mutableSetOf()

    private val command = command("nametag") {
        withPermission("maptools.nametag")

        textArgument("content") {
            textArgument("name") {
                textArgument("main-color") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection { lastUsedMain })
                    textArgument("shadow-color") {
                        replaceSuggestions(ArgumentSuggestions.stringCollection { lastUsedShadow })
                        textArgument("char-shadow-color") {
                            replaceSuggestions(ArgumentSuggestions.stringCollection { lastUsedCharShadow })
                            textArgument("char-color", true) {
                                replaceSuggestions(ArgumentSuggestions.stringCollection { lastUsedChar })
                                anyExecutor { sender, args ->
                                    val content = args[0] as String
                                    val name = args[1] as String
                                    val mainColor = args[2] as String
                                    val shadowColor = args[3] as String
                                    val charShadowColor = args[4] as String
                                    val charColor = args.getOptional(5).getOrNull() as? String ?: "ffffffff"

                                    lastUsedMain.push(mainColor)
                                    lastUsedShadow.push(shadowColor)
                                    lastUsedCharShadow.push(shadowColor)
                                    lastUsedChar.push(charColor)

                                    CustomNameTag.createNewNameTag(
                                        content,
                                        File("${texturePackFolder}/global/assets/minecraft/textures/font/nametags", "$name.png"),
                                        mainColor.toColor(sender) ?: return@anyExecutor,
                                        shadowColor.toColor(sender) ?: return@anyExecutor,
                                        charShadowColor.toColor(sender) ?: return@anyExecutor,
                                        charColor.toColor(sender) ?: return@anyExecutor
                                    )
                                    val fontFile = File("${texturePackFolder}/global/assets/minecraft/font/default.json")
                                    val font = json.decodeFromString<FontJson>(fontFile.takeIf { it.exists() }?.readText()?.ifBlank { "{}" } ?: "{}")
                                    val escaped = "\\uE${font.providers.size.plus(1).to3Digits()}"
                                    val unescaped = unescapeUnicode(escaped)
                                    font.providers.add(FontChar("bitmap", "minecraft:font/nametags/$name.png", 8, 8, listOf(unescaped)))
                                    fontFile.writeText(json.encodeToString(font))

                                    sender.sendMessage(prefix + msg("command.namtetag.create", listOf(name)))
                                    sender.sendMessage(prefix + cmp("-> $unescaped ($msgCopy)").addCopy(unescaped).addHover(cmp(msgString("common.copyLore"))))
                                    sender.sendMessage(prefix + msg("command.nametag.reloadRP").addCommand("/resourcepack"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun MutableSet<String>.push(string: String) {
        add(string)
        if (size >= 6) remove(first())
    }

    private fun Int.to3Digits(): String {
        return when (this) {
            in 0..9 -> "00$this"
            in 10..99 -> "0$this"
            else -> "$this"
        }
    }

    private fun String.toColor(sender: CommandSender): Int? {
        return try {
            val hex = if (this[0] == '#') substring(1, length) else this
            when (hex.length) {
                6 -> UnsignedInteger.valueOf("ff$hex", 16).toInt()
                8 -> UnsignedInteger.valueOf("${hex[6]}${hex[7]}${hex.substring(0, 6)}", 16).toInt()
                else -> throw IllegalArgumentException("Color must be 6 (#rrggbb) or 8 (#rrggbbaa) character long")
            }

        } catch (e: Exception) {
            sender.sendMessage(prefix + cmp("One or more colors aren't valid hex strings", cError))
            sender.sendMessage(prefix + cmp("(${e.message})", cError))
            e.printStackTrace()
            null
        }
    }

    private fun unescapeUnicode(input: String): String {
        val result = StringBuilder()
        val tokens = input.split("\\\\u".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in 1 until tokens.size) {
            val codePoint = tokens[i].toInt(16)
            result.append(codePoint.toChar())
        }
        return result.toString()
    }

    override fun load() {
        if (!file.exists()) return
        try {
            val inputs = json.decodeFromString<LastUsedInputs>(file.readText())
            lastUsedMain.clear()
            lastUsedMain.addAll(inputs.main)
            lastUsedChar.clear()
            lastUsedChar.addAll(inputs.char)
            lastUsedShadow.clear()
            lastUsedShadow.addAll(inputs.shadow)
            lastUsedCharShadow.clear()
            lastUsedCharShadow.addAll(inputs.charShadow)
        } catch (e: Exception) {
            console.sendMessage(prefix + cmp("Failed to read ${file.name}!", cError))
            console.sendMessage(prefix + cmp("Reason: ${e.message ?: "Unknown"}"))
        }
    }

    override fun save() {
        if (!file.exists()) file.parentFile.mkdirs()
        file.writeText(json.encodeToString(LastUsedInputs(lastUsedMain, lastUsedShadow, lastUsedChar, lastUsedCharShadow)))
    }

    override fun disable() {
        command.unregister()
    }

    override fun enable() {
        command.register()
    }

    @Serializable
    private data class FontJson(val providers: MutableList<FontChar>)

    @Serializable
    private data class FontChar(val type: String, val file: String, val ascent: Int, val height: Int, val chars: List<String>)

    @Serializable
    private data class LastUsedInputs(val main: Set<String>, val shadow: Set<String>, val char: Set<String>, val charShadow: Set<String>)
}