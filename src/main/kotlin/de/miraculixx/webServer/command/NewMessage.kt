package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.kpaper.localization.msgString
import de.miraculixx.webServer.interfaces.Reloadable
import de.miraculixx.webServer.utils.*
import de.miraculixx.webServer.utils.SettingsManager.messageFolder
import de.miraculixx.webServer.utils.SettingsManager.messageLanguages
import de.miraculixx.webServer.utils.SettingsManager.settingsFolder
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

class NewMessage : Reloadable {
    private var datapackFile = File("world/datapacks/${messageFolder}/data")
    private val mm = MiniMessage.miniMessage()
    private val chatClearer = "{\"text\":\"\\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n\"}"
    private val headerFile = File(settingsFolder, "header/messages.txt")
    private var header = SettingsManager.saveReadFile(headerFile, "header/${headerFile.name}")
    private val headerRedirectFile = File(settingsFolder, "header/messagesRedirect.txt")
    private var headerRedirect = SettingsManager.saveReadFile(headerFile, "header/${headerRedirectFile.name}")

    val command = commandTree("message") {
        withPermission("buildertools.message")

        literalArgument("new") {
            textArgument("prefix") {
                textArgument("message") {
                    argument(StringArgument("lang").replaceSuggestions(ArgumentSuggestions.stringCollection { messageLanguages })) {
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

                                    Bukkit.reloadData()
                                    sender.sendMessage(msg("command.message.finalMessage", listOf(message)))
                                    sender.sendMessage(msg("command.message.clickToPlay").clickEvent(ClickEvent.runCommand("/function $lang:$name")))
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
                val languages = messageLanguages
                languages.forEach { lang ->
                    val messages = getFileNamesInFolder(File(datapackFile, "$lang/functions"))
                    messages.toMutableSet().apply { removeAll(globals) }.forEach { name ->
                        sender.sendMessage(prefix + msg("command.message.missingGlobal", listOf(lang, name)))
                        File(datapackFile, "chat/functions/$name.mcfunction").apply { parentFile.mkdirs() }.writeGlobal(name)
                    }

                    globals.toMutableSet().apply { removeAll(messages) }.forEach { name ->
                        sender.sendMessage(prefix + msg("command.message.missingTranslation", listOf(name)))
                    }
                }
                Bukkit.reloadData()
                sender.sendMessage(prefix + msg("command.message.validated"))
            }
        }

        literalArgument("conversation") {
            textArgument("file") {
                argument(StringArgument("lang").replaceSuggestions(ArgumentSuggestions.stringCollection { messageLanguages })) {
                    anyExecutor { sender, args ->
                        val name = args[0] as String
                        val lang = args[1] as String
                        val file = File(datapackFile, "$lang/functions/${name.removeSuffix(".json")}.json")
                        if (!file.exists()) {
                            sender.sendMessage(prefix + cmp(msgString("common.fileNotFound"), cError))
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
                        sender.sendMessage(prefix + msg("command.message.built", listOf(folderPath, convData.content.size.toString())))
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
            append(header.replace("<message>", message).replace("<prefix>", data.prefix).replace("<target>", data.target))

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
            buildString {
                append(headerRedirect.replace("<name>", name) + "\n")
                messageLanguages.forEachIndexed { index, lang ->
                    append("\nexecute if score language state matches ${index + 1} run function $lang:${name}")
                }
            }
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

    override fun reload() {
        datapackFile = File("world/datapacks/${messageFolder}/data")
        header = SettingsManager.saveReadFile(headerFile, "header/${headerFile.name}")
        headerRedirect = SettingsManager.saveReadFile(headerFile, "header/${headerRedirectFile.name}")
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