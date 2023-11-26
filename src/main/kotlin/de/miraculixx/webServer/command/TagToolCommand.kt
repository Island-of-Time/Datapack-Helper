package de.miraculixx.webServer.command

import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.items.itemStack
import de.miraculixx.kpaper.items.meta
import de.miraculixx.kpaper.items.name
import de.miraculixx.kpaper.localization.msgString
import de.miraculixx.webServer.events.ToolEvent.key
import de.miraculixx.webServer.events.ToolEvent.key2
import de.miraculixx.webServer.events.ToolEvent.key3
import de.miraculixx.webServer.interfaces.Module
import de.miraculixx.webServer.utils.*
import de.miraculixx.webServer.utils.extensions.command
import de.miraculixx.webServer.utils.extensions.unregister
import dev.jorel.commandapi.kotlindsl.*
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.persistence.PersistentDataType
import kotlin.jvm.optionals.getOrNull

class TagToolCommand : Module {
    private val command = command("tag-tool") {
        withPermission("buildertools.tag-tool")
        textArgument("tags") {
            entityTypeArgument("filter", true) {
                doubleArgument("radius", 0.1, 10.0, true) {
                    playerExecutor { player, args ->
                        val filter = args.getOptional(1).getOrNull() as? EntityType
                        val range = args.getOptional(2).getOrNull() as? Double
                        val fullTags = args[0] as String
                        val tags = fullTags.split(' ')
                        val item = itemStack(Material.ARROW) {
                            meta {
                                name = cmp("Tag Tool - ${tags.first()} ${if (tags.size > 1) "[${tags.size - 1}...]" else ""} ${filter?.name ?: ""} ${range ?: ""}", cError)
                                lore(listOf(
                                    emptyComponent(),
                                    msgClickRight + cmp(msgString("tool.tag.right")),
                                    msgShiftClickRight + cmp(msgString("tool.tag.rightSneak")),
                                    msgClickLeft + cmp(msgString("tool.tag.left"))
                                ))
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

    override fun disable() {
        command.unregister()
    }

    override fun enable() {
        command.register()
    }
}