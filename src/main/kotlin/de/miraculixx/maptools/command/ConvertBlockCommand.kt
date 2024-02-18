package de.miraculixx.maptools.command

import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.items.itemStack
import de.miraculixx.kpaper.items.meta
import de.miraculixx.kpaper.items.name
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.kpaper.localization.msgString
import de.miraculixx.maptools.events.ToolEvent.key
import de.miraculixx.maptools.events.ToolEvent.key2
import de.miraculixx.maptools.events.ToolEvent.key3
import de.miraculixx.maptools.interfaces.Module
import de.miraculixx.maptools.utils.*
import de.miraculixx.maptools.utils.extensions.command
import de.miraculixx.maptools.utils.extensions.unregister
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemFlag
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.jvm.optionals.getOrNull

class ConvertBlockCommand: Module {
    private val command1 = command("blockify") {
        withPermission("maptools.blockify")

        locationArgument("pos", LocationType.BLOCK_POSITION) {
            stringArgument("tag") {
                floatArgument("scale") {
                    locationArgument("origin", LocationType.BLOCK_POSITION, optional = true) {
                        playerExecutor { player, args ->
                            val pos = args[0] as Location
                            val tag = args[1] as String
                            val scale = args[2] as Float
                            val origin = args.getOptional(3).getOrNull() as? Location

                            val offset = (1.0 - scale) / 2
                            if (origin != null) {
                                val bd = pos.world.spawnEntity(origin.add(offset + 0.5, offset + 0.5, offset + 0.5), EntityType.BLOCK_DISPLAY) as BlockDisplay
                                bd.block = pos.block.blockData
                                bd.scoreboardTags.add(tag)
                                val center = Vector3f((pos.x - origin.x).toFloat(), (pos.y - origin.y).toFloat(), (pos.z - origin.z).toFloat())
                                bd.transformation = Transformation(center, Quaternionf(0f, 0f, 0f, 1f), Vector3f(scale, scale, scale), Quaternionf(0f, 0f, 0f, 1f))

                            } else {
                                val bd = pos.world.spawnEntity(pos.clone().add(offset, offset, offset), EntityType.BLOCK_DISPLAY) as BlockDisplay
                                bd.block = pos.block.blockData
                                bd.scoreboardTags.add(tag)
                                bd.transformation = Transformation(Vector3f(0f, 0f, 0f), Quaternionf(0f, 0f, 0f, 1f), Vector3f(scale, scale, scale), Quaternionf(0f, 0f, 0f, 1f))
                            }

                            val type = pos.block.type.translationKey()
                            pos.block.type = Material.AIR
                            player.sendMessage(prefix + msg("command.blockConverter.convert", listOf(type)))
                        }
                    }
                }
            }
        }
    }

    private val command2 = command("blockify-tool") {
        withPermission("maptools.blockify-tool")

        floatArgument("scale") {
            stringArgument("tag") {
                locationArgument("origin", LocationType.BLOCK_POSITION, optional = true) {
                    playerExecutor { player, args ->
                        val origin = args.getOptional(2).getOrNull() as? Location
                        val text = origin?.let { "${it.blockX}:${it.blockY}:${it.blockZ}" }
                        val item = itemStack(Material.SHEARS) {
                            meta {
                                val scale = args[0] as Float
                                val tag = args[1] as String
                                name = cmp("Blockify: $scale - $tag ${text?.let { "- $it" } ?: ""}", cError)
                                lore(
                                    listOf(
                                        emptyComponent(),
                                        msgClick + cmp(msgString("tool.blockConverter.click"))
                                    )
                                )
                                persistentDataContainer.set(key, PersistentDataType.FLOAT, scale)
                                persistentDataContainer.set(key2, PersistentDataType.STRING, tag)
                                if (text != null) persistentDataContainer.set(key3, PersistentDataType.STRING, text)
                                customModel = 100
                                addEnchant(Enchantment.MENDING, 1, true)
                                addItemFlags(ItemFlag.HIDE_ENCHANTS)
                            }
                        }
                        player.inventory.addItem(item)
                        player.sendMessage(prefix + msg("command.blockConverter.tool"))
                    }
                }
            }
        }
    }

    override fun disable() {
        command1.unregister()
        command2.unregister()
    }

    override fun enable() {
        command1.register()
        command2.register()
    }
}