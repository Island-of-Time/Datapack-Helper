package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.console
import de.miraculixx.webServer.settings
import de.miraculixx.webServer.utils.*
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.kotlindsl.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import java.io.File
import java.util.*

class NewMessage {
    private val datapackFile
        get() = File("world/datapacks/${settings.messageFolder}/data")
    private val mm = MiniMessage.miniMessage()
    private val chatClearer = "{\"text\":\"\\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n\"}"

    val command = commandTree("message") {
        withPermission("buildertools.message")

        literalArgument("new") {
            textArgument("prefix") {
                textArgument("message") {
                    argument(StringArgument("lang").replaceSuggestions(ArgumentSuggestions.stringCollection { settings.messageLanguages })) {
                        textArgument("name") {
                            textArgument("target") {
                                anyExecutor { sender, args ->
                                    val prefix = args[0] as String
                                    val message = args[1] as String
                                    val lang = args[2] as String
                                    val name = args[3] as String
                                    val targets = args[4] as String

                                    val functionData = FunctionInfo(lang, name, UUID.randomUUID().toString(), targets, prefix, 1)
                                    calculateMessage(message, functionData)
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


        literalArgument("validate") {
            anyExecutor { sender, _ ->
                val globals = getFileNamesInFolder(File(datapackFile, "chat/functions"))
                val languages = settings.messageLanguages
                languages.forEach { lang ->
                    val messages = getFileNamesInFolder(File(datapackFile, "$lang/functions"))
                    messages.toMutableSet().apply { removeAll(globals) }.forEach { name ->
                        sender.sendMessage(prefix + cmp("Missing global for $lang ") + cmp(name, cMark) + cmp("! Creating it..."))
                        File(datapackFile, "chat/functions/$name.mcfunction").apply { parentFile.mkdirs() }.writeGlobal(name)
                    }

                    globals.toMutableSet().apply { removeAll(messages) }.forEach { name ->
                        sender.sendMessage(prefix + cmp("Missing $lang translation of ") + cmp(name, cMark))
                    }
                }
                sender.sendMessage(prefix + cmp("Validation complete!", cSuccess))
                Bukkit.reloadData()
            }
        }

        literalArgument("conversation") {
            textArgument("file") {
                argument(StringArgument("lang").replaceSuggestions(ArgumentSuggestions.stringCollection { settings.messageLanguages })) {
                    anyExecutor { sender, args ->
                        val name = args[0] as String
                        val lang = args[1] as String
                        val file = File(datapackFile, "$lang/functions/${name.removeSuffix(".json")}.json")
                        if (!file.exists()) {
                            sender.sendMessage(prefix + cmp("The requested file does not exist!", cError))
                            return@anyExecutor
                        }
                        val rawJson = file.readText()
                        val convData = try {
                            json.decodeFromString<Conversation>(rawJson)
                        } catch (e: Exception) {
                            sender.sendMessage(prefix + cmp("Failed to build conversation:", cError))
                            sender.sendMessage(prefix + cmp(e.message ?: "Unknown", cError))
                            console.sendMessage(cmp(e.stackTraceToString(), cError))
                            return@anyExecutor
                        }
                        val folderPath = name.substringBeforeLast('/')
                        var prev = ""
                        convData.content.forEachIndexed { index, entry ->
                            val functionData = FunctionInfo(lang, "$folderPath/${index + 1}", UUID.randomUUID().toString(), convData.target, "$prev<br><br>${entry.prefix}", 1)
                            calculateMessage(entry.text, functionData)
                            prev += "<br><br>${entry.prefix}${entry.text}"
                        }
                        Bukkit.reloadData()
                        sender.sendMessage(prefix + cmp("Conversation successfully build to $folderPath (${convData.content.size} messages)", cSuccess))
                    }
                }
            }
        }
    }

    private fun getFileNamesInFolder(folder: File?): Set<String> {
        return buildSet {
            folder?.takeIf { it.isDirectory }?.listFiles()?.forEach { file ->
                if (file.isDirectory) addAll(getFileNamesInFolder(file))
                else add(file.path.substringAfter("functions/").removeSuffix(".mcfunction"))
            }
        }
    }

    private fun calculateMessage(message: String, data: FunctionInfo) {
        val targetFile = File(datapackFile, "${data.lang}/functions/${data.functionName}.mcfunction")
        val globalFile = File(datapackFile, "chat/functions/${data.functionName}.mcfunction")
        if (!targetFile.exists()) targetFile.parentFile.mkdirs()
        if (!globalFile.exists()) globalFile.parentFile.mkdirs()
        val namespace = data.scoreName
        val prefix = gson.serialize(mm.deserialize(data.prefix.replace("①", "").replace("⑤", "").replace("⑳", "")))

        val final = buildString {
            append(
                "# Message Animator (by Miraculixx & To_Binio)\n" +
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
        targetFile.writeText(final)
        globalFile.writeGlobal(data.functionName)
    }

    private fun File.writeGlobal(name: String) {
        writeText(
            "# Message Convertor (by Miraculixx & To_Binio)\n" +
                    "#\n" +
                    "# Language Redirection\n" +
                    "# - Target: ../../<lang>/functions/${name}\n\n" +
                    "execute if score language state matches 1 run function german:${name}\n" +
                    "execute if score language state matches 2 run function english:${name}"
        )
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
    private data class FunctionInfo(
        val lang: String,
        val functionName: String,
        val scoreName: String,
        val target: String,
        val prefix: String,
        var currentTick: Int,
    )

    @Serializable
    private data class Conversation(
        val target: String,
        val content: List<ConversationText>
    )

    @Serializable
    private data class ConversationText(
        val prefix: String,
        val text: String
    )
}