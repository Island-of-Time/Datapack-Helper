package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.bukkit.addCommand
import de.miraculixx.kpaper.extensions.bukkit.plus
import de.miraculixx.webServer.interfaces.Module
import de.miraculixx.webServer.utils.SettingsManager
import de.miraculixx.webServer.utils.addHover
import de.miraculixx.webServer.utils.cmp
import de.miraculixx.webServer.utils.extensions.command
import de.miraculixx.webServer.utils.extensions.unregister
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.integerArgument
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import org.jetbrains.annotations.ApiStatus.Experimental
import java.io.File

@Experimental
class QuestBookCommand : Module {
    // This module will get its own namespace if it ever should exit experimental
    private val datapackFile = File("world/datapacks/${SettingsManager.pathingFolder}/data/logic/functions/quest-book")
    private val gson = GsonComponentSerializer.gson()
    private val darkGray = TextColor.fromHexString("#4F4F4F")!!

    val command = command("quest-book") {
        integerArgument("chapter") {
            integerArgument("quests") {
                anyExecutor { sender, args ->
                    val chapter = args[0] as Int
                    val quests = args[1] as Int
                    val folder = File(datapackFile, "chapter$chapter")
                    if (!folder.exists()) folder.mkdirs()

                    repeat(quests) {
                        val file = File(folder, "${it + 1}.mcfunction")
                        val content = buildString {
                            append("{display:{Name:'{\"translate\":\"custom.inv.quest-book\",\"italic\":false}'},title:'',author:'',generation:3,pages:[")
                            append("'${getPageContent((it - 5).coerceAtLeast(0), it, chapter)}',")
                            append("'${getClueContent()}'")
                            append("]} 1")
                        }.replace("\\n", "\\\\n")
                        file.writeText(
                            "execute as @a at @s run playsound minecraft:custom.new-note master @s ~ ~ ~ 0.4 1\n" +
                                    "title @a times 20 60 30\n" +
                                    "title @a subtitle {\"translate\":\"custom.quest.title.new\"}\n" +
                                    "title @a title \" \"\n" +
                                    "item replace entity @a hotbar.8 with minecraft:written_book$content\n"
                        )
                    }

                    Bukkit.reloadData()
                    sender.sendMessage(prefix + cmp("Finished quest book setup!"))
                }
            }
        }
    }

    private fun getPageContent(from: Int, to: Int, chapter: Int): String {
        val finished = Style.style(NamedTextColor.BLACK, TextDecoration.STRIKETHROUGH)
        val current = Style.style(NamedTextColor.BLACK)
        var finalComponent = cmp("\uE040\n", NamedTextColor.WHITE)
        (from..to).forEach { state ->
            val char = if (state == to) '☐' else '☒'
            finalComponent += (cmp("\n\n$char ", darkGray) + Component.translatable("custom.quest.$chapter.$state.n", if (state == to) current else finished))
                .addHover(Component.translatable("custom.quest.$chapter.$state.l", Style.style(NamedTextColor.WHITE)))
        }
        return gson.serialize(finalComponent)
    }

    private fun getClueContent(): String {
        return gson.serialize(
            (cmp("\n\n    ") + Component.translatable("custom.quest.clue") + cmp("\n\n\n")).color(NamedTextColor.WHITE) +
                    (cmp("   \uE042 ", darkGray) + Component.translatable("custom.quest.clue.current.n", NamedTextColor.BLACK))
                        .addHover(Component.translatable("custom.quest.clue.current.l")).addCommand("/trigger rehint") +
                    (cmp("\n   + ", darkGray) + Component.translatable("custom.quest.clue.next.n", NamedTextColor.BLACK))
                        .addHover(Component.translatable("custom.quest.clue.next.l")).addCommand("/trigger hint")
        )
    }

    override fun disable() {
        command.unregister()
    }

    override fun enable() {
        command.register()
    }
}