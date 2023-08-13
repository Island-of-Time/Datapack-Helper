@file:Suppress("UNCHECKED_CAST")

package de.miraculixx.webServer.command

import de.miraculixx.webServer.utils.cmp
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Interaction

class HitBoxCommand {
    val command = commandTree("interaction") {
        locationArgument("block", LocationType.BLOCK_POSITION) {
            stringArgument("tag") {
                anyExecutor { sender, args ->
                    val loc = args[0] as Location
                    val tag = args[1] as String
                    val e = loc.world.spawn(loc.subtract(-.5,.005,-.5), Interaction::class.java)
                    e.scoreboardTags.add(tag)
                    e.interactionWidth = 1.01f
                    e.interactionHeight = 1.01f
                    sender.sendMessage(prefix + cmp("Interaction spawned around the block"))
                }
            }
        }
        entitySelectorArgumentManyEntities("entity") {
            stringArgument("tag") {
                anyExecutor { sender, args ->
                    val entities = args[0] as List<Entity>
                    val tag = args[1] as String
                    entities.forEach { e ->
                        val interaction = e.world.spawn(e.location.subtract(.0,.01,.0), Interaction::class.java)
                        interaction.interactionWidth = e.width.toFloat() + 0.02f
                        interaction.interactionHeight = e.height.toFloat() + 0.02f
                        interaction.scoreboardTags.add(tag)
                    }
                    sender.sendMessage(prefix + cmp("Interaction spawned around ${entities.size} ${if (entities.size > 1) "entities" else "entity"}"))
                }
            }
        }
    }
}