package de.miraculixx.maptools.command

import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.items.itemStack
import de.miraculixx.kpaper.items.meta
import de.miraculixx.kpaper.items.name
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.kpaper.localization.msgList
import de.miraculixx.kpaper.localization.msgString
import de.miraculixx.maptools.events.ToolEvent.key
import de.miraculixx.maptools.interfaces.Module
import de.miraculixx.maptools.utils.*
import de.miraculixx.maptools.utils.extensions.command
import de.miraculixx.maptools.utils.extensions.unregister
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Marker
import org.bukkit.inventory.ItemFlag
import org.bukkit.persistence.PersistentDataType

class MarkerCommand : Module {
    private val command1 = command("marker") {
        withPermission("maptools.marker-tool")

        literalArgument("tool") {
            textArgument("tag") {
                playerExecutor { player, args ->
                    val item = itemStack(Material.SPECTRAL_ARROW) {
                        meta {
                            val tag = args[0] as String
                            name = cmp("Block Marker - $tag", cError)
                            lore(
                                listOf(
                                    emptyComponent(),
                                    msgClickRight + cmp(msgString("tool.marker.right")),
                                    msgClickLeft + cmp(msgString("tool.marker.left")),
                                )
                            )
                            persistentDataContainer.set(key, PersistentDataType.STRING, tag)
                            customModel = 100
                            addEnchant(Enchantment.MENDING, 1, true)
                            addItemFlags(ItemFlag.HIDE_ENCHANTS)
                        }
                    }
                    player.inventory.addItem(item)
                    player.sendMessage(prefix + msg("command.marker.tool"))
                }
            }
        }

        literalArgument("spawn") {
            textArgument("tag") {
                locationArgument("location", LocationType.BLOCK_POSITION) {
                    anyExecutor { sender, args ->
                        val tags = (args[0] as String).split(',')
                        val location = args[1] as Location
                        val block = location.block
                        val marker = block.world.spawn(block.location.subtract(-.5, -.5, -.5), Marker::class.java)
                        marker.scoreboardTags.addAll(tags)
                        sender.soundEnable()
                        sender.sendMessage(prefix + msg("command.marker.spawn"))
                    }
                }
            }
        }
    }

    private val command2 = command("marker-finder") {
        withPermission("maptools.marker-finder")

        integerArgument("range") {
            playerExecutor { player, args ->
                val item = itemStack(Material.RECOVERY_COMPASS) {
                    meta {
                        val range = args[0] as Int
                        name = cmp("Marker Finder - $range", cError)
                        lore(msgList("tool.marker.finderInfo"))
                        persistentDataContainer.set(key, PersistentDataType.INTEGER, range)
                        customModel = 100
                        addEnchant(Enchantment.MENDING, 1, true)
                        addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    }
                }
                player.inventory.addItem(item)
                player.sendMessage(prefix + msg("command.marker.toolFinder"))
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