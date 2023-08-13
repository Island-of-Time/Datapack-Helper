@file:Suppress("DuplicatedCode")

package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.bukkit.addCommand
import de.miraculixx.kpaper.extensions.bukkit.addCopy
import de.miraculixx.webServer.utils.*
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.textArgument
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.format.TextColor
import java.awt.Color
import java.io.File

class NameTagCommand {
    private val nameTagFolder = File("texturepack/global/assets/minecraft/textures/font/nametags")
    private val fontFile = File("texturepack/global/assets/minecraft/font/default.json")

    val command = commandTree("nametag") {
        textArgument("content") {
            textArgument("name") {
                textArgument("main-color") {
                    textArgument("shadow-color") {
                        textArgument("char-color") {
                            anyExecutor { sender, args ->
                                val content = args[0] as String
                                val name = args[1] as String
                                val mainColor = args[2] as String
                                val shadowColor = args[3] as String
                                val charShadowColor = args[4] as String

                                CustomNameTag.createNewNameTag(
                                    content,
                                    File(nameTagFolder, "$name.png"),
                                    mainColor.toColor(sender) ?: return@anyExecutor,
                                    shadowColor.toColor(sender) ?: return@anyExecutor,
                                    charShadowColor.toColor(sender) ?: return@anyExecutor
                                )
                                val font = json.decodeFromString<FontJson>(fontFile.readText())
                                val escaped = "\\uE${font.providers.size.plus(1).to3Digits()}"
                                val unescaped = unescapeUnicode(escaped)
                                font.providers.add(FontChar("bitmap", "minecraft:font/nametags/$name.png", 8, 8, listOf(unescaped)))
                                fontFile.writeText(json.encodeToString(font))
                                sender.sendMessage(prefix + cmp("Successfully created a new nametag ") + cmp(name, cMark))
                                sender.sendMessage(prefix + cmp("$unescaped (copy)").addCopy(unescaped).addHover(cmp("Click to copy")))
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
        val c = TextColor.fromCSSHexString(this)
        return if (c == null) {
            sender.sendMessage(prefix + cmp("One or more colors aren't valid hex strings", cError))
            null
        } else Color(c.red(), c.green(), c.blue(), 255).rgb
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

    @Serializable
    private data class FontJson(val providers: MutableList<FontChar>)
    @Serializable
    private data class FontChar(val type: String, val file: String, val ascent: Int, val height: Int, val chars: List<String>)
}