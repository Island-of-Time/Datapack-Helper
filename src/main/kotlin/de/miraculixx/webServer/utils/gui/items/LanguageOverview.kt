package de.miraculixx.webServer.utils.gui.items

import de.miraculixx.kpaper.extensions.bukkit.plainText
import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.items.itemStack
import de.miraculixx.kpaper.items.meta
import de.miraculixx.kpaper.items.name
import de.miraculixx.mweb.gui.logic.items.ItemProvider
import de.miraculixx.webServer.utils.gui.logic.items.skullTexture
import de.miraculixx.webServer.command.MessageOverview
import de.miraculixx.webServer.utils.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import java.io.File

class LanguageOverview : ItemProvider {
    private val datapackFile = File("world/datapacks/iot-chat/data")
    var lang = "german"
    private val seperator = cmp(" ≫ ")
    val prefixKey = NamespacedKey("de.miraculixx.api", "prefixmessage")
    val messageKey = NamespacedKey("de.miraculixx.api", "fullmessage")
    val functionKey = NamespacedKey("de.miraculixx.api", "functionname")

    override fun getItemList(from: Int, to: Int): List<ItemStack> {
        return buildList {
            val folder = File(datapackFile, "$lang/functions")
            println(folder.path)
            folder.listFiles()?.forEach { file ->
                println(" -> ${file.name}")
                val reader = file.bufferedReader()
                if (file.isDirectory) return@forEach
                val line = reader.readLine().substring(1)
                val functionData = try {
                    Json.decodeFromString<MessageOverview.FunctionInfo>(line)
                } catch (e: Exception) {
                    println(e.message)
                    MessageOverview.FunctionInfo("Error", "", "")
                }

                add(itemStack(Material.PAPER) {
                    meta {
                        customModel = 100
                        name = cmp(file.nameWithoutExtension, cHighlight)
                        lore(buildList {
                            add(Component.empty())
                            add(cmp("Selector: ", cHighlight) + cmp(functionData.target))
                            add(cmp("Prefix: ", cHighlight) + cmp(gson.deserialize(functionData.prefix.ifBlank { "{\"text\":\"\"}" }).plainText()))
                            val component = mm.stripTags(functionData.text)
                            var counter = 0
                            var bufferText = ""
                            component.split(' ').forEach { s ->
                                bufferText += "$s "
                                if (counter >= 7) {
                                    add(cmp(bufferText))
                                    bufferText = ""
                                    counter = 0
                                } else counter++
                            }
                            if (counter != 0) add(cmp(bufferText))
                            add(Component.empty())
                            add(cmp("Left Click", cHighlight) + seperator + cmp("Display Text"))
                            add(cmp("Right Click", cHighlight) + seperator + cmp("Play Animation"))
                        })
                        persistentDataContainer.set(prefixKey, PersistentDataType.STRING, functionData.prefix)
                        persistentDataContainer.set(messageKey, PersistentDataType.STRING, functionData.text)
                        persistentDataContainer.set(functionKey, PersistentDataType.STRING, "$lang:${file.nameWithoutExtension}")
                    }
                })
            }
        }
    }

    override fun getExtra(): List<ItemStack> {
        return listOf(
            itemStack(Material.PLAYER_HEAD) {
                meta {
                    name = cmp("German", cHighlight, true)
                    customModel = 1
                }
                itemMeta =
                    (itemMeta as SkullMeta).skullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWU3ODk5YjQ4MDY4NTg2OTdlMjgzZjA4NGQ5MTczZmU0ODc4ODY0NTM3NzQ2MjZiMjRiZDhjZmVjYzc3YjNmIn19fQ==")
            },
            itemStack(Material.PLAYER_HEAD) {
                meta {
                    name = cmp("English", cHighlight, true)
                    customModel = 1
                }
                itemMeta =
                    (itemMeta as SkullMeta).skullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODgzMWM3M2Y1NDY4ZTg4OGMzMDE5ZTI4NDdlNDQyZGZhYTg4ODk4ZDUwY2NmMDFmZDJmOTE0YWY1NDRkNTM2OCJ9fX0=")
            }
        )
    }
}