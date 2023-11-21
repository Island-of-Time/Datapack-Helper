@file:Suppress("unused")

package de.miraculixx.webServer.command

import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.items.itemStack
import de.miraculixx.kpaper.items.meta
import de.miraculixx.kpaper.items.name
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.kpaper.localization.msgString
import de.miraculixx.webServer.events.ToolEvent.key
import de.miraculixx.webServer.utils.*
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.persistence.PersistentDataType

class MarkerCommand {
    val block = commandTree("marker-tool") {
        withPermission("buildertools.marker-tool")

        stringArgument("tag") {
            playerExecutor { player, args ->
                val item = itemStack(Material.SPECTRAL_ARROW) {
                    meta {
                        val tag = args[0] as String
                        name = cmp("Block Marker - $tag", cError)
                        lore(listOf(
                            emptyComponent(),
                            msgClickRight + cmp(msgString("tool.marker.right")),
                            msgClickLeft + cmp(msgString("tool.marker.left")),
                        ))
                        persistentDataContainer.set(key, PersistentDataType.STRING, tag)
                        customModel = 100
                        addUnsafeEnchantment(Enchantment.MENDING, 1)
                        addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    }
                }
                player.inventory.addItem(item)
                player.sendMessage(prefix + msg("command.marker.tool"))
            }
        }
    }

    val finder = commandTree("marker-finder") {
        withPermission("buildertools.marker-finder")

        integerArgument("range") {
            playerExecutor { player, args ->
                val item = itemStack(Material.RECOVERY_COMPASS) {
                    meta {
                        val range = args[0] as Int
                        name = cmp("Marker Finder - $range", cError)
                        lore(listOf(
                            cmp("Highlight all nearby markers") + cmp("with green particles.")
                        ))
                        persistentDataContainer.set(key, PersistentDataType.INTEGER, range)
                        customModel = 100
                        addEnchant(Enchantment.MENDING, 1, true)
                        addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    }
                }
                player.inventory.addItem(item)
                player.sendMessage(prefix + cmp("Added marker finder to your inventory"))
            }
        }
    }
}