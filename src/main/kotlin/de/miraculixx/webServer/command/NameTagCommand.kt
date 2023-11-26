package de.miraculixx.webServer.command

import com.google.common.primitives.UnsignedInteger
import de.miraculixx.kpaper.extensions.bukkit.addCommand
import de.miraculixx.kpaper.extensions.bukkit.addCopy
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.kpaper.localization.msgString
import de.miraculixx.webServer.interfaces.Module
import de.miraculixx.webServer.utils.*
import de.miraculixx.webServer.utils.SettingsManager.texturePackFolder
import de.miraculixx.webServer.utils.extensions.command
import de.miraculixx.webServer.utils.extensions.unregister
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.textArgument
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.kyori.adventure.audience.Audience
import java.io.File
import kotlin.jvm.optionals.getOrNull

class NameTagCommand : Module {
    private val command = command("nametag") {
        withPermission("buildertools.nametag")

        textArgument("content") {
            textArgument("name") {
                textArgument("main-color") {
                    textArgument("shadow-color") {
                        textArgument("char-shadow-color") {
                            textArgument("char-color", true) {
                                anyExecutor { sender, args ->
                                    val content = args[0] as String
                                    val name = args[1] as String
                                    val mainColor = args[2] as String
                                    val shadowColor = args[3] as String
                                    val charShadowColor = args[4] as String
                                    val charColor = args.getOptional(5).getOrNull() as? String ?: "ffffffff"

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

                                    sender.sendMessage(prefix + cmp("Successfully created a new nametag ") + cmp(name, cMark))
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

    private fun Int.to3Digits(): String {
        return when (this) {
            in 0..9 -> "00$this"
            in 10..99 -> "0$this"
            else -> "$this"
        }
    }

    private fun String.toColor(sender: Audience): Int? {
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
}