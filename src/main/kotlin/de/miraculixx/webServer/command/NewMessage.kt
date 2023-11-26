package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.bukkit.addCopy
import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.extensions.worlds
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.kpaper.localization.msgString
import de.miraculixx.webServer.interfaces.Module
import de.miraculixx.webServer.interfaces.Reloadable
import de.miraculixx.webServer.utils.*
import de.miraculixx.webServer.utils.SettingsManager.messageFolder
import de.miraculixx.webServer.utils.SettingsManager.messageLanguages
import de.miraculixx.webServer.utils.SettingsManager.scoreboard
import de.miraculixx.webServer.utils.SettingsManager.settingsFolder
import de.miraculixx.webServer.utils.extensions.command
import de.miraculixx.webServer.utils.extensions.unregister
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

class NewMessage : Reloadable, Module {
    private lateinit var dataPackFolder: File
    private lateinit var packMetaFile: File
    private lateinit var functionFolder: File
    private val mm = MiniMessage.miniMessage()
    private val chatClearer = "{\"text\":\"\\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n \\n\"}"
    private val headerFile = File(settingsFolder, "header/messages.txt")
    private var header = SettingsManager.saveReadFile(headerFile, "header/${headerFile.name}")
    private val headerRedirectFile = File(settingsFolder, "header/messagesRedirect.txt")
    private var headerRedirect = SettingsManager.saveReadFile(headerFile, "header/${headerRedirectFile.name}")

    private val command = command("message") {
        withPermission("buildertools.message")

        literalArgument("new") {
            textArgument("prefix") {
                textArgument("message") {
                    argument(StringArgument("lang").replaceSuggestions(ArgumentSuggestions.stringCollection { messageLanguages })) {
                        textArgument("name") {
                            entitySelectorArgumentManyEntities("target") {
                                anyExecutor { sender, args ->
                                    val prefix = args[0] as String
                                    val message = args[1] as String
                                    val lang = args[2] as String
                                    val name = args[3] as String
                                    val targets = args.getRaw(4) ?: "@a"

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
                val globals = getFileNamesInFolder(File(functionFolder, "chat/functions"))
                val languages = messageLanguages
                languages.forEach { lang ->
                    val messages = getFileNamesInFolder(File(functionFolder, "$lang/functions"))
                    messages.toMutableSet().apply { removeAll(globals) }.forEach { name ->
                        sender.sendMessage(prefix + msg("command.message.missingGlobal", listOf(lang, name)))
                        File(functionFolder, "chat/functions/$name.mcfunction").apply { parentFile.mkdirs() }.writeGlobal(name)
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
                        val file = File(functionFolder, "$lang/functions/${name.removeSuffix(".json")}.json")
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

        literalArgument("symbols") {
            anyExecutor { sender, _ ->
                sender.sendMessage(prefix + msg("command.message.symbols"))
                val clickToCopy = cmp(msgString("common.copyLore"), cMark)
                sender.sendMessage(
                    cmp("1 tick -> ① ($msgCopy)").addCopy("①").addHover(clickToCopy) +
                            cmp("\n5 ticks -> ⑤ ($msgCopy)").addCopy("⑤").addHover(clickToCopy) +
                            cmp("\n20 ticks -> ⑳ ($msgCopy)").addCopy("⑳").addHover(clickToCopy)
                )
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
        val targetFile = File(functionFolder, "${data.lang}/functions/${data.functionName}.mcfunction")
        val globalFile = File(functionFolder, "chat/functions/${data.functionName}.mcfunction")
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
                        "scoreboard players add $namespace $scoreboard 1\n" +
                        "execute if score $namespace $scoreboard matches ..${iterations} run schedule function ${data.lang}:${data.functionName} 1t replace\n" +
                        "execute if score $namespace $scoreboard matches ${iterations + 1}.. run scoreboard players set $namespace $scoreboard 0"
            )
        }
        targetFile.writeText(final)
        globalFile.writeGlobal(data.functionName)
        if (!packMetaFile.exists()) packMetaFile.writeBytes(SettingsManager.packMeta)
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

                val command = "execute if score ${functionInfo.scoreName} $scoreboard matches ${functionInfo.currentTick} run tellraw ${functionInfo.target}"
                val current = gson.serialize(mm.deserialize(text))
                append("\n$command [$chatClearer,$prefix,$current]")

                functionInfo.currentTick++
            }
        } to functionInfo.currentTick
    }

    override fun reload() {
        dataPackFolder = File("${worlds.first().name}/datapacks/$messageFolder")
        packMetaFile = File(dataPackFolder, "pack.mcmeta")
        functionFolder = File(dataPackFolder, "data")
        header = SettingsManager.saveReadFile(headerFile, "header/${headerFile.name}")
        headerRedirect = SettingsManager.saveReadFile(headerFile, "header/${headerRedirectFile.name}")
    }

    override fun disable() {
        command.unregister()
    }

    override fun enable() {
        command.register()
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