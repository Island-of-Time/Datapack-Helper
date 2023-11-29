package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.bukkit.dispatchCommand
import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.items.itemStack
import de.miraculixx.kpaper.items.meta
import de.miraculixx.kpaper.items.name
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.kpaper.localization.msgString
import de.miraculixx.webServer.events.ToolEvent.key
import de.miraculixx.webServer.events.ToolEvent.key2
import de.miraculixx.webServer.events.ToolEvent.key3
import de.miraculixx.webServer.interfaces.Module
import de.miraculixx.webServer.utils.*
import de.miraculixx.webServer.utils.extensions.command
import de.miraculixx.webServer.utils.extensions.unregister
import dev.jorel.commandapi.kotlindsl.*
import dev.jorel.commandapi.wrappers.CommandResult
import dev.jorel.commandapi.wrappers.Rotation
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.persistence.PersistentDataType
import java.util.*
import kotlin.jvm.optionals.getOrNull

val multiToolData: MutableMap<UUID, MultiToolCommand.MultiToolData> = mutableMapOf()
val multiToolSelection: MutableMap<UUID, MutableMap<Entity, Any>> = mutableMapOf()

class MultiToolCommand : Module {
    private val command = command("multitool") {
        withPermission("maptools.multitool")

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
                                    name = cmp("MultiTool - $entity $radius $vector", cError)
                                    lore(listOf(
                                        emptyComponent(),
                                        msgClickLeft + cmp(msgString("tool.multi.left")),
                                        msgShiftClickLeft + cmp(msgString("tool.multi.leftSneak")),
                                        Component.keybind("key.swapOffhand").color(cHighlight) + cmp(" ≫ ") + cmp(msgString("tool.multi.offhand")),
                                        emptyComponent(),
                                        cmp(msgString("tool.multi.infoMode")),
                                        cmp("• " + msgString("tool.multi.infoMove"), cHighlight, underlined = true),
                                        cmp("  " + msgString("tool.multi.moveRight")),
                                        cmp("  " + msgString("tool.multi.moveRightSneak")),
                                        emptyComponent(),
                                        cmp("• " + msgString("tool.multi.infoRotate"), cHighlight, underlined = true),
                                        cmp("  " + msgString("tool.multi.rotateRight")),
                                        cmp("  " + msgString("tool.multi.rotateRightSneak")),
                                    ))
                                    persistentDataContainer.set(key, PersistentDataType.STRING, entity.name)
                                    persistentDataContainer.set(key2, PersistentDataType.FLOAT, radius)
                                    persistentDataContainer.set(key3, PersistentDataType.FLOAT, vector)
                                    customModel = 100
                                }
                            }
                            sender.inventory.addItem(item)
                            sender.sendMessage(prefix + msg("command.multi.tool"))
                        }
                    }
                }
            }
        }

        literalArgument("mode") {
            literalArgument("rotate") {
                playerExecutor { player, _ ->
                    multiToolData.getOrPut(player.uniqueId) { MultiToolData() }.mode = MultiToolMode.ROTATE
                    player.sendMessage(prefix + msg("command.multi.modeRotate"))
                }
            }
            literalArgument("move") {
                playerExecutor { player, _ ->
                    multiToolData.getOrPut(player.uniqueId) { MultiToolData() }.mode = MultiToolMode.MOVE
                    player.sendMessage(prefix + msg("command.multi.modeMove"))
                }
            }
        }

        literalArgument("move-by") {
            locationArgument("vector") {
                playerExecutor { player, args ->
                    val vector = (args[0] as Location).toVector()
                    multiToolSelection[player.uniqueId]?.forEach { (e, _) -> e.teleport(e.location.add(vector)) }
                    player.sendMessage(prefix + msg("command.multi.moved", listOf(vector.toString())))
                }
            }
        }

        literalArgument("rotate-by") {
            rotationArgument("rotation") {
                playerExecutor { player, args ->
                    val rotation = args[0] as Rotation
                    multiToolSelection[player.uniqueId]?.forEach { (e, _) ->
                        e.teleport(e.location.apply {
                            yaw += rotation.normalizedYaw
                            pitch += rotation.normalizedPitch
                        })
                    }
                    player.sendMessage(prefix + msg("command.multi.rotated", listOf(rotation.toString())))
                }
            }
        }

        literalArgument("range") {
            integerArgument("range", 1) {
                playerExecutor { player, args ->
                    val range = args[0] as Int
                    multiToolData.getOrPut(player.uniqueId) { MultiToolData() }.range = range
                    player.sendMessage(prefix + msg("command.multi.rangeChange", listOf(range.toString())))
                }
            }
        }

        literalArgument("execute-as") {
            withPermission("maptools.multitool-execute")

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
                    player.sendMessage(prefix + msg("command.multi.executed", listOf(entities.size.toString())))
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

    data class MultiToolData(
        var mode: MultiToolMode = MultiToolMode.MOVE,
        var range: Int = 3,
        var confirm: Boolean = false
    )

    enum class MultiToolMode {
        MOVE, ROTATE
    }
}