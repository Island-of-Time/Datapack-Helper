package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.bukkit.dispatchCommand
import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.items.itemStack
import de.miraculixx.kpaper.items.meta
import de.miraculixx.kpaper.items.name
import de.miraculixx.webServer.events.ToolEvent.key
import de.miraculixx.webServer.events.ToolEvent.key2
import de.miraculixx.webServer.events.ToolEvent.key3
import de.miraculixx.webServer.utils.*
import dev.jorel.commandapi.kotlindsl.*
import dev.jorel.commandapi.wrappers.CommandResult
import dev.jorel.commandapi.wrappers.Rotation
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.persistence.PersistentDataType
import java.util.*
import kotlin.jvm.optionals.getOrNull

val multiToolData: MutableMap<UUID, MultiToolCommand.MultiToolData> = mutableMapOf()
val multiToolSelection: MutableMap<UUID, MutableMap<Entity, Any>> = mutableMapOf()

class MultiToolCommand {
    val command = commandTree("multitool") {
        withPermission("buildertools.multitool")

        literalArgument("get") {
            entityTypeArgument("type") {
                floatArgument("radius", 0f, 10f, optional = true) {
                    floatArgument("vector", optional = true) {
                        playerExecutor { sender, args ->
                            val entity = args[0] as EntityType
                            val radius = args.getOptional(1).getOrNull() as Float? ?: 1.0f
                            val vector = args.getOptional(2).getOrNull() as Float? ?: 0.1f
                            val item = itemStack(Material.FEATHER) {
                                meta {
                                    name = cmp("Multi Tool - $entity $radius $vector", cError)
                                    persistentDataContainer.set(key, PersistentDataType.STRING, entity.name)
                                    persistentDataContainer.set(key2, PersistentDataType.FLOAT, radius)
                                    persistentDataContainer.set(key3, PersistentDataType.FLOAT, vector)
                                    customModel = 100
                                }
                            }
                            sender.inventory.addItem(item)
                        }
                    }
                }
            }
        }

        literalArgument("mode") {
            literalArgument("rotate") {
                playerExecutor { player, _ ->
                    multiToolData.getOrPut(player.uniqueId) { MultiToolData() }.mode = MultiToolMode.ROTATE
                    player.sendMessage(prefix + cmp("Multi tool mode changed to ") + cmp("rotate", cMark))
                }
            }
            literalArgument("move") {
                playerExecutor { player, _ ->
                    multiToolData.getOrPut(player.uniqueId) { MultiToolData() }.mode = MultiToolMode.MOVE
                    player.sendMessage(prefix + cmp("Multi tool mode changed to ") + cmp("move", cMark))
                }
            }
        }

        literalArgument("move-by") {
            locationArgument("vector") {
                playerExecutor { player, args ->
                    val vector = (args[0] as Location).toVector()
                    multiToolSelection[player.uniqueId]?.forEach { (e, _) -> e.teleportAsync(e.location.add(vector)) }
                    player.sendMessage(prefix + cmp("Selection moved by ") + cmp(vector.toString(), cMark))
                }
            }
        }

        literalArgument("rotate-by") {
            rotationArgument("rotation") {
                playerExecutor { player, args ->
                    val rotation = args[0] as Rotation
                    multiToolSelection[player.uniqueId]?.forEach { (e, _) ->
                        e.teleportAsync(e.location.apply {
                            yaw += rotation.normalizedYaw
                            pitch += rotation.normalizedPitch
                        })
                    }
                    player.sendMessage(prefix + cmp("Selection rotated by ") + cmp(rotation.toString(), cMark) + cmp(" (clockwise)"))
                }
            }
        }

        literalArgument("range") {
            integerArgument("range", 1) {
                playerExecutor { player, args ->
                    val range = args[0] as Int
                    multiToolData.getOrPut(player.uniqueId) { MultiToolData() }.range = range
                    player.sendMessage(prefix + cmp("Changed personal range to ") + cmp(range.toString(), cMark))
                }
            }
        }

        literalArgument("execute-as") {
            withPermission("buildertools.multitool-execute")

            commandArgument("command") {
                playerExecutor { player, args ->
                    val command = args[0] as CommandResult
                    val finalCommand = buildString {
                        append(command.command.name)
                        command.args.forEach { append(" $it") }
                    }
                    val entities = multiToolSelection[player.uniqueId] ?: return@playerExecutor
                    entities.forEach { (e, _) ->
                        console.dispatchCommand("execute as ${e.uniqueId} at @s run $finalCommand")
                    }
                    player.sendMessage(prefix + cmp("Command executed as ${entities.size} entities"))
                }
            }
        }
    }

    data class MultiToolData(
        var mode: MultiToolMode = MultiToolMode.MOVE,
        var range: Int = 3,
        var confirm: Boolean = false
    )

    enum class MultiToolMode {
        MOVE, ROTATE
    }
}