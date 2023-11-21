@file:Suppress("UNCHECKED_CAST")

package de.miraculixx.webServer.command

import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.items.itemStack
import de.miraculixx.kpaper.items.meta
import de.miraculixx.kpaper.items.name
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.kpaper.localization.msgString
import de.miraculixx.webServer.events.ToolEvent.key
import de.miraculixx.webServer.utils.*
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.entity.Interaction
import org.bukkit.inventory.ItemFlag
import org.bukkit.persistence.PersistentDataType
import kotlin.jvm.optionals.getOrNull

class HitBoxCommand {
    val command = commandTree("interaction") {
        literalArgument("spawn") {
            withPermission("buildertools.interaction")

            locationArgument("block", LocationType.BLOCK_POSITION) {
                stringArgument("tag", optional = true) {
                    floatArgument("size", optional = true) {
                        anyExecutor { sender, args ->
                            val loc = args[0] as Location
                            val tag = args.getOptional(1).getOrNull() as? String
                            val size = args.getOptional(2).getOrNull() as? Float
                            val sizeD = size?.toDouble()
                            val e = loc.world.spawn(loc.add(.5, (sizeD ?: 0.01) / 2, .5), Interaction::class.java)
                            tag?.let { e.scoreboardTags.add(it) }
                            e.interactionWidth = size ?: 1.01f
                            e.interactionHeight = size ?: 1.01f
                            sender.sendMessage(prefix + msg("command.hitbox.spawnBlock"))
                        }
                    }
                }
            }
            entitySelectorArgumentManyEntities("entity") {
                stringArgument("tag", true) {
                    floatArgument("additional-space", optional = true) {
                        anyExecutor { sender, args ->
                            val entities = args[0] as List<Entity>
                            val tag = args.getOptional(1).getOrNull() as? String
                            val space = args.getOptional(2).getOrNull() as? Float ?: 0.02f
                            entities.forEach { e ->
                                val interaction = e.world.spawn(e.location.subtract(.0, .01, .0), Interaction::class.java)
                                interaction.interactionWidth = e.width.toFloat() + space
                                interaction.interactionHeight = e.height.toFloat() + space
                                tag?.let { interaction.scoreboardTags.add(it) }
                            }
                            when (entities.size) {
                                0 -> sender.sendMessage(prefix + msg("command.hitbox.spawn"))
                                1 -> sender.sendMessage(prefix + msg("command.hitbox.spawnEntity", listOf(entities.first().name)))
                                else -> sender.sendMessage(prefix + msg("command.hitbox.spawnEntities", listOf(entities.size.toString())))
                            }
                        }
                    }
                }
            }
        }

        literalArgument("tool") {
            withPermission("buildertools.interaction-tool")

            stringArgument("tag") {
                playerExecutor { player, args ->
                    val tag = args[0] as String
                    val item = itemStack(Material.SHULKER_SHELL) {
                        meta {
                            name = cmp("Hitbox - $tag")
                            lore(listOf(
                                emptyComponent(),
                                msgClickRight + cmp(msgString("tool.hitbox.right")),
                                msgClickLeft + cmp(msgString("tool.hitbox.left"))
                            ))
                            customModel = 100
                            persistentDataContainer.set(key, PersistentDataType.STRING, tag)
                            addEnchant(Enchantment.MENDING, 1, true)
                            addItemFlags(ItemFlag.HIDE_ENCHANTS)
                        }
                    }
                    player.inventory.addItem(item)
                    player.sendMessage(prefix + msg("command.hitbox.tool"))
                }
            }
        }
    }
}