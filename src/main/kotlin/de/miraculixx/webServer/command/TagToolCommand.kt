package de.miraculixx.webServer.command

import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.items.itemStack
import de.miraculixx.kpaper.items.meta
import de.miraculixx.kpaper.items.name
import de.miraculixx.webServer.events.ToolEvent.key
import de.miraculixx.webServer.events.ToolEvent.key2
import de.miraculixx.webServer.events.ToolEvent.key3
import de.miraculixx.webServer.utils.cError
import de.miraculixx.webServer.utils.cmp
import dev.jorel.commandapi.kotlindsl.*
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.persistence.PersistentDataType
import kotlin.jvm.optionals.getOrNull

class TagToolCommand {
    val command = commandTree("tag-tool") {
        withPermission("buildertools.tag-tool")

        entityTypeArgument("filter", true) {
            doubleArgument("radius", 0.1, 10.0, true) {
                greedyStringArgument("tags") {
                    playerExecutor { player, args ->
                        val filter = args.getOptional(0).getOrNull() as? EntityType
                        val range = args.getOptional(1).getOrNull() as? Double
                        val fullTags = args[2] as String
                        val tags = fullTags.split(' ')
                        val item = itemStack(Material.ARROW) {
                            meta {
                                name = cmp("Tag Tool - ${tags.first()} ${if (tags.size > 1) "[${tags.size - 1}...]" else ""} ${filter?.name ?: ""} ${range ?: ""}", cError)
                                customModel = 100
                                persistentDataContainer.set(key, PersistentDataType.STRING, fullTags)
                                filter?.name?.let { persistentDataContainer.set(key2, PersistentDataType.STRING, it) }
                                range?.let { persistentDataContainer.set(key3, PersistentDataType.DOUBLE, it) }
                            }
                        }
                        player.inventory.addItem(item)
                    }
                }
            }
        }
    }
}