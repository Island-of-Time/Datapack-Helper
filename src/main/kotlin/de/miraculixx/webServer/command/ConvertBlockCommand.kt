package de.miraculixx.webServer.command

import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.items.itemStack
import de.miraculixx.kpaper.items.meta
import de.miraculixx.kpaper.items.name
import de.miraculixx.webServer.Main
import de.miraculixx.webServer.utils.cError
import de.miraculixx.webServer.utils.cmp
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.EntityType
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f

class ConvertBlockCommand {
    val convertCommand = commandTree("convertblock") {
        locationArgument("pos", LocationType.BLOCK_POSITION) {
            stringArgument("tag") {
                floatArgument("scale") {
                    playerExecutor { player, args ->
                        val pos = args[0] as Location
                        val tag = args[1] as String
                        val scale = args[2] as Float
                        val offset = (1.0 - scale) / 2
                        val bd = pos.world.spawnEntity(pos.clone().add(offset, offset, offset), EntityType.BLOCK_DISPLAY) as BlockDisplay
                        bd.block = pos.block.blockData
                        bd.scoreboardTags.add(tag)
                        bd.transformation = Transformation(Vector3f(0f, 0f, 0f), Quaternionf(0f, 0f, 0f, 1f), Vector3f(scale, scale, scale), Quaternionf(0f, 0f, 0f, 1f))
                        pos.block.type = Material.AIR
                    }
                }
            }
        }
    }

    private val key = NamespacedKey(Main.INSTANCE, "webserver.command-item")
    private val key2 = NamespacedKey(Main.INSTANCE, "webserver.command-item-tag")
    val items = commandTree("convertor") {
        floatArgument("scale") {
            stringArgument("tag") {
                playerExecutor { player, args ->
                    val item = itemStack(Material.SHEARS) {
                        meta {
                            val scale = args[0] as Float
                            val tag = args[1] as String
                            name = cmp("Convertor: $scale - $tag", cError)
                            persistentDataContainer.set(key, PersistentDataType.FLOAT, scale)
                            persistentDataContainer.set(key2, PersistentDataType.STRING, tag)
                            customModel = 100
                        }
                    }
                    player.inventory.addItem(item)
                }
            }
        }
    }
}