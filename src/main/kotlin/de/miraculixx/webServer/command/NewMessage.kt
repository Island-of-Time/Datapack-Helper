package de.miraculixx.webServer.command

import de.miraculixx.webServer.utils.*
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.kotlindsl.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import java.io.File
import java.util.*

class NewMessage {
    private val datapackFile = File("world/datapacks/iot-chat/data")
    private val mm = MiniMessage.miniMessage()
    private val chatClearer = "{\"text\":\"\\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n\"}"

    val command = commandTree("message") {
        textArgument("prefix") {
            textArgument("message") {
                argument(StringArgument("lang").replaceSuggestions(ArgumentSuggestions.strings("german", "english"))) {
                    textArgument("name") {
                        textArgument("target") {
                            anyExecutor { sender, args ->
                                val prefix = args[0] as String
                                val message = args[1] as String
                                val lang = args[2] as String
                                val name = args[3] as String
                                val targets = args[4] as String

                                val functionData = FunctionInfo(lang, name, UUID.randomUUID().toString(), targets, prefix, 1)
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

    private fun Audience.calculateMessage(message: String, data: FunctionInfo) {
        val targetFile = File(datapackFile, "${data.lang}/functions/${data.functionName}.mcfunction")
        if (!targetFile.exists()) targetFile.parentFile.mkdirs()
        val namespace = data.scoreName
        val prefix = gson.serialize(mm.deserialize(data.prefix))

        val infoData = MessageOverview.FunctionInfo(message, data.prefix, data.target)
        val final = buildString {
            append(
                "#${Json.encodeToString(infoData)}\n" +
                        "#\n" +
                        "# Message Convertor (by Miraculixx & To_Binio)\n" +
                        "#\n" +
                        "# Settings Input\n" +
                        "# - Text: $message\n" +
                        "# - Prefix: ${data.prefix}\n" +
                        "# - Target: ${data.target}\n"
            )

            //Berechnung
            val body = calcFunctionBody(prefix, message, data)
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
        prefix: String,
        body: String,
        functionInfo: FunctionInfo
    ): Pair<String, Int> {
        return buildString {
            var text = ""
            var isTag = false

            body.forEach { char ->

                if (isTag) {
                    text += char
                    if (char == '>') isTag = false
                    return@forEach
                } else {
                    when (char) {
                        '<' -> {
                            text += char
                            isTag = true
                            return@forEach
                        }
                        '①' -> {
                            functionInfo.currentTick++
                            return@forEach
                        }
                        '⑤' -> {
                            functionInfo.currentTick += 5
                            return@forEach
                        }
                        '⑳' -> {
                            functionInfo.currentTick += 20
                            return@forEach
                        }
                    }
                }
                text += char

                val command = "execute if score ${functionInfo.scoreName} text-ticker matches ${functionInfo.currentTick} run tellraw ${functionInfo.target}"
                val current = gson.serialize(mm.deserialize(text))
                append("\n$command [$chatClearer,$prefix,$current]")

                functionInfo.currentTick++
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
        val scoreName: String,
        val target: String,
        val prefix: String,
        var currentTick: Int,
    )
}