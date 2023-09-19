package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.geometry.minus
import de.miraculixx.webServer.events.BlockPlaceEvent
import de.miraculixx.webServer.utils.cmp
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.BlockDisplay
import java.io.File
import kotlin.math.max
import kotlin.math.min

class OriginToolCommand {
    private val folder = File("world/datapacks/iot-general/data/logic/functions")

    private val command = commandTree("place-mode") {
        literalArgument("tag") {
            stringArgument("tag") {
                playerExecutor { player, args ->
                    val mode = BlockPlaceEvent.playerModes[player.uniqueId] ?: BlockPlaceEvent.Mode()
                    mode.tag = args[0] as String
                    BlockPlaceEvent.playerModes[player.uniqueId] = mode
                    player.sendMessage(prefix + cmp("Place tagger set to ${mode.tag}"))
                }
            }
        }

        literalArgument("resizer") {
            doubleArgument("size") {
                playerExecutor { player, args ->
                    val mode = BlockPlaceEvent.playerModes[player.uniqueId] ?: BlockPlaceEvent.Mode()
                    mode.resizer = !mode.resizer
                    mode.resizerNumber = args[0] as Double
                    BlockPlaceEvent.playerModes[player.uniqueId] = mode
                    player.sendMessage(prefix + cmp("Place resizer is now ${mode.resizer} (${mode.resizerNumber})"))
                }
            }
        }
    }

    private val command2 = commandTree("origin") {
        locationArgument("from", LocationType.BLOCK_POSITION) {
            locationArgument("to", LocationType.BLOCK_POSITION) {
                locationArgument("origin", LocationType.BLOCK_POSITION) {
                    textArgument("file-name") {
                        integerArgument("time") {
                            doubleArgument("scale") {
                                stringArgument("tag") {
                                    playerExecutor { player, args ->
                                        val from = args[0] as Location
                                        val to = args[1] as Location
                                        val origin = args[2] as Location
                                        val fileName = args[3] as String
                                        val time = args[4] as Int
                                        val scale = args[5] as Double
                                        val tag = args[6] as String

                                        val fromX = min(from.blockX, to.blockX)
                                        val toX = max(from.blockX, to.blockX)
                                        val fromY = min(from.blockY, to.blockY)
                                        val toY = max(from.blockY, to.blockY)
                                        val fromZ = min(from.blockZ, to.blockZ)
                                        val toZ = max(from.blockZ, to.blockZ)

                                        val world = player.world

                                        val affectedBlocks = mutableListOf<Block>()
                                        val functionString = buildString {
                                            (fromX..toX).forEach x@{ x ->
                                                (fromY..toY).forEach y@{ y ->
                                                    (fromZ..toZ).forEach z@{ z ->
                                                        val block = world.getBlockAt(x, y, z)
                                                        if (block.type.isAir) return@z
                                                        val offset = block.location - origin
                                                        val bd = world.spawn(block.location, BlockDisplay::class.java)
                                                        bd.block = block.blockData
                                                        bd.addScoreboardTag(tag)
                                                        affectedBlocks.add(block)
                                                        append(
                                                            "\ndata merge entity ${bd.uniqueId} {start_interpolation:0,interpolation_duration:$time,transformation:{" +
                                                                    "scale:[${scale}f,${scale}f,${scale}f]," +
                                                                    "translation:[${offset.blockX * (scale - 1)}f,${offset.blockY * (scale - 1) - scale + 1}f,${offset.blockZ * (scale - 1)}f]}}"
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        affectedBlocks.forEach { b -> b.type = Material.AIR }

                                        val file = File(folder, "$fileName.mcfunction")
                                        val reverseFile = File(folder, "$fileName-reverse.mcfunction")
                                        file.writeText(functionString)
                                        reverseFile.writeText("execute as @e[tag=$tag] run data merge entity @s {start_interpolation:0,interpolation_duration:$time,transformation:{scale:[1f,1f,1f],translation:[0f,0f,0f]}}")
                                        Bukkit.reloadData()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}