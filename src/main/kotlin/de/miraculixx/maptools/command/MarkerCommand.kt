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
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.persistence.PersistentDataType

class MarkerCommand : Module {
    private val command1 = command("marker-tool") {
        withPermission("maptools.marker-tool")

        stringArgument("tag") {
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