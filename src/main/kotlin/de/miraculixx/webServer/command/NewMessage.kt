package de.miraculixx.webServer.command

import de.miraculixx.webServer.utils.cError
import de.miraculixx.webServer.utils.cmp
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.kotlindsl.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import java.io.File
import java.util.*

class NewMessage {
    private val datapackFile = File("world/datapacks/iot-chat/data")
    private val mm = MiniMessage.miniMessage()
    private val mGson = GsonComponentSerializer.gson()
    private val chatClearer = "{\"text\":\"\\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n\"}"

    val command = commandTree("message") {
        textArgument("prefix") {
            textArgument("message") {
                argument(StringArgument("lang").replaceSuggestions(ArgumentSuggestions.strings("german", "english"))) {
                    textArgument("name") {
                        integerArgument("speed", 1, 10) {
                            textArgument("target") {
                                anyExecutor { sender, args ->
                                    "\"\uE001 \",{\"text\":\"Was willst du \",\"color\":\"white\"},{\"text\":\"Fremder\",\"color\":\"#F0F018\"}"
                                    val prefix = args[0] as String
                                    val message = args[1] as String
                                    val lang = args[2] as String
                                    val name = args[3] as String
                                    val speed = args[4] as Int
                                    val targets = args[5] as String

                                    val functionData = FunctionInfo(lang, name, speed, UUID.randomUUID().toString(), targets, prefix, 1, "")
                                    sender.calculateMessage(message, functionData)
                                    sender.sendMessage(cmp("Final Message: ") + mm.deserialize(message))
                                    sender.sendMessage(cmp("(Click here to play animation)").clickEvent(ClickEvent.runCommand("/function $lang:$name")))
                                    Bukkit.reloadData()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Audience.calculateMessage(message: String, data: FunctionInfo) {
        val targetFile = File(datapackFile, "${data.lang}/functions/${data.functionName}.mcfunction")
        if (!targetFile.exists()) targetFile.parentFile.mkdirs()
        val namespace = data.scoreName
        val component = mm.deserialize(message)
        val string = mGson.serialize(component)
        val textObj = try {
            Json.decodeFromString<TextStyling>(string)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            sendMessage(prefix + cmp("An error occurred while handling your request! (More in console)", cError))
            return
        }
        val infoData = MessageOverview.FunctionInfo(message, data.prefix, data.charsPerTick, data.target)

        val final = buildString {
            append(
                "#${Json.encodeToString(infoData)}\n" +
                        "#\n" +
                        "# Message Convertor (by Miraculixx & To_Binio)\n" +
                        "#\n" +
                        "# Settings Input\n" +
                        "# - Text: $message\n" +
                        "# - Prefix: ${data.prefix}\n" +
                        "# - Chars per Tick: ${data.charsPerTick}\n" +
                        "# - Target: ${data.target}\n"
            )

            //Berechnung
            val body = calcFunctionBody(textObj, textObj, data)
            val iterations = body.second
            append(body.first)
            append(
                "\n\n# Looping & Reset\n" +
                        "scoreboard players add $namespace text-ticker 1\n" +
                        "execute if score $namespace text-ticker matches ..${iterations} run schedule function ${data.lang}:${data.functionName} 1t replace\n" +
                        "execute if score $namespace text-ticker matches ${iterations + 1}.. run scoreboard players set $namespace text-ticker 0"
            )
        }
        println(targetFile.absolutePath)
        targetFile.writeText(final)
    }

    private fun calcFunctionBody(
        styling: TextStyling,
        preStyling: TextStyling,
        functionInfo: FunctionInfo
    ): Pair<String, Int> {
        return buildString {
            styling.text?.let { input ->
                val split = input.chunked(functionInfo.charsPerTick)
                var text = ""
                val styleTag = buildString style@{
                    styling.bold?.let { this@style.append(",\"bold\":$it") } ?: preStyling.bold?.let { this@style.append(",\"bold\":$it") }
                    styling.italic?.let { this@style.append(",\"italic\":$it") } ?: preStyling.italic?.let { this@style.append(",\"italic\":$it") }
                    styling.underlined?.let { this@style.append(",\"underlined\":$it") } ?: preStyling.underlined?.let { this@style.append(",\"underlined\":$it") }
                    styling.strikethrough?.let { this@style.append(",\"strikethrough\":$it") } ?: preStyling.strikethrough?.let { this@style.append(",\"strikethrough\":$it") }
                    styling.obfuscated?.let { this@style.append(",\"obfuscated\":$it") } ?: preStyling.obfuscated?.let { this@style.append(",\"obfuscated\":$it") }
                    styling.color?.let { this@style.append(",\"color\":\"$it\"") } ?: preStyling.color?.let { this@style.append(",\"color\":\"$it\"") }
                }
                val prefix = if (functionInfo.prefix.isBlank()) ",\"\","
                else if (functionInfo.prefix.startsWith('{')) functionInfo.prefix.let { ",$it," }
                else functionInfo.prefix.let { ",\"$it\"," }

                split.forEach { sequence ->
                    if (sequence == "⛔") {
                        functionInfo.currentTick++
                        return@forEach
                    }
                    val currentPart = "{\"text\":\"$text$sequence\"$styleTag}"
                    val command = "execute if score ${functionInfo.scoreName} text-ticker matches ${functionInfo.currentTick} run tellraw ${functionInfo.target}"
                    val previousPart = if (functionInfo.previousPart.isNotBlank()) "${functionInfo.previousPart}," else ""
                    append("\n$command [$chatClearer$prefix$previousPart$currentPart]")

                    functionInfo.currentTick++
                    text += sequence
                }
                val fullTag = "{\"text\":\"$text\"$styleTag}"
                functionInfo.previousPart += if (functionInfo.previousPart.isBlank()) fullTag else ",$fullTag"
            }

            styling.extra?.let {
                it.forEach { extra ->
                    append(calcFunctionBody(extra, styling, functionInfo).first)
                }
            }
        } to functionInfo.currentTick
    }

    @Serializable
    private data class TextStyling(
        val extra: List<TextStyling>? = null,
        val color: String? = null,
        val bold: Boolean? = null,
        val italic: Boolean? = null,
        val underlined: Boolean? = null,
        val strikethrough: Boolean? = null,
        val obfuscated: Boolean? = null,
        val text: String? = null
    )

    @Serializable
    private data class FunctionInfo(
        val lang: String,
        val functionName: String,
        val charsPerTick: Int,
        val scoreName: String,
        val target: String,
        val prefix: String,
        var currentTick: Int,
        var previousPart: String
    )
}