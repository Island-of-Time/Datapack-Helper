package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.kotlin.enumOf
import de.miraculixx.kpaper.extensions.worlds
import de.miraculixx.kpaper.items.*
import de.miraculixx.webServer.utils.cError
import de.miraculixx.webServer.utils.cmp
import dev.jorel.commandapi.kotlindsl.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Boat
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Marker
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta
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

    fun click(player: Player, meta: ItemMeta) {
        if (!player.hasPermission("buildertools.tag-tool")) return

        val pdc = meta.persistentDataContainer
        val tags = pdc.get(key, PersistentDataType.STRING)?.split(' ') ?: return
        val filter = pdc.get(key2, PersistentDataType.STRING)?.let { t -> enumOf<EntityType>(t) }
        val range = pdc.get(key3, PersistentDataType.DOUBLE) ?: 0.5
        when (val e = filter?.entityClass) { // Return entities with hitboxes
            is ArmorStand -> if (!e.isMarker) return
            is LivingEntity, is Boat, is Interaction -> return
        }

        //raycast
        val loc = Location(worlds[0], .0,.0,.0)
        //rayvast

        val targets = if (filter) loc.getNearbyEntitiesByType(filter.entityClass, range)
    }
}