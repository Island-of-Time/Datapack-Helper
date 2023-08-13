package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.bukkit.plus
import de.miraculixx.kpaper.items.itemStack
import de.miraculixx.kpaper.items.meta
import de.miraculixx.webServer.utils.addHover
import de.miraculixx.webServer.utils.cmp
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.integerArgument
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Material
import org.bukkit.inventory.meta.BookMeta
import java.io.File

class QuestBookCommand {
    val datapackFile = File("world/datapacks/iot-general/data/logic/functions/todo-book")
    val gson = GsonComponentSerializer.gson()

    val command = commandTree("quest-book") {
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
                            append("{display:{Name:'{\"translate\":\"custom.inv.quest-book\"}'},title:'',author:'',generation:3,pages:[")
                            when (it) {
                                in 0..4 -> append("'${getPageContent(0, it, chapter)}'")
                                in 5..11 -> append(",'${getPageContent(5, it, chapter)}'")
                            }
                            append("]} 1")
                        }.replace("\\n","\\\\n")
                        file.writeText("execute as @a at @s run playsound minecraft:custom.new-note master @s ~ ~ ~ 0.4 1\n" +
                                "title @a times 20 60 30\n" +
                                "title @a subtitle {\"translate\":\"custom.quest.title.new\"}\n" +
                                "title @a title \" \"\n" +
                                "item replace entity @a hotbar.8 with minecraft:written_book$content\n")
                    }
                }
            }
        }
    }

    private fun getPageContent(from: Int, to: Int, chapter: Int): String {
        val finished = Style.style(NamedTextColor.BLACK, TextDecoration.STRIKETHROUGH)
        val current = Style.style(NamedTextColor.BLACK)
        var finalComponent = cmp("      ", strikethrough = true) + cmp(" Quest Book ", TextColor.fromHexString("#F0F028")!!) + cmp("      ", strikethrough = true)
        (from..to).forEach { state ->
            val char = if (state == to) '☐' else '☒'
            finalComponent += (cmp("\n\n$char ", TextColor.fromHexString("#4F4F4F")!!) + Component.translatable("custom.quest.$chapter.$state.n", if (state == to) current else finished))
                .addHover(Component.translatable("custom.quest.$chapter.$state.l", Style.style(NamedTextColor.WHITE)))
        }
        return gson.serialize(finalComponent)
    }
}