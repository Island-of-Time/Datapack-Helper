package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.bukkit.addCommand
import de.miraculixx.kpaper.runnables.task
import de.miraculixx.webServer.CatmullRomSpline
import de.miraculixx.webServer.settings
import de.miraculixx.webServer.utils.cError
import de.miraculixx.webServer.utils.cmp
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.kotlindsl.*
import net.kyori.adventure.audience.Audience
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.joml.Vector3d
import java.awt.geom.Point2D
import java.io.File
import java.util.*

class AnimationCommand {
    private val dataPackFolder
        get() = File("world/datapacks/${settings.animationFolder}/data/animation/functions")
    private val creators: MutableMap<UUID, CreatorData> = mutableMapOf()

    @Suppress("unused")
    private val mainCommand = commandTree("animation") {
        withPermission("buildertools.animation")

        literalArgument("new") {
            playerExecutor { player, _ ->
                creators[player.uniqueId] = CreatorData(mutableListOf(), mutableListOf())
                player.sendMessage(prefix + cmp("New animation creator started! Add new points with /animation add"))
            }
        }

        literalArgument("add") {
            playerExecutor { player, _ ->
                val newLoc = player.location.clone()
                newLoc.yaw = ((newLoc.yaw + 180) % 360) - 180
                newLoc.pitch = ((newLoc.pitch + 180) % 360) - 180
                val creator = creators[player.uniqueId] ?: player.noEditor() ?: return@playerExecutor
                val prevLoc = creator.points.lastOrNull()
                if (prevLoc != null) {
                    if (prevLoc.x == newLoc.x) newLoc.add(0.001, 0.0, 0.0)
                    if (prevLoc.y == newLoc.y) newLoc.add(0.0, 0.001, 0.0)
                    if (prevLoc.z == newLoc.z) newLoc.add(0.0, 0.0, 0.001)
                    if (prevLoc.yaw == newLoc.yaw) newLoc.apply { yaw += 0.001f }
                    if (prevLoc.pitch == newLoc.pitch) newLoc.apply { pitch += 0.001f }
                }
                creator.points.add(newLoc)
                player.sendMessage(prefix + cmp("New position added! Finish your animation with /animation finish"))
            }
        }

        literalArgument("remove-last") {
            playerExecutor { player, _ ->
                val creator = creators[player.uniqueId] ?: player.noEditor() ?: return@playerExecutor
                if (creator.points.isEmpty()) {
                    player.sendMessage(prefix + cmp("No points left to remove", cError))
                    return@playerExecutor
                }
                creator.points.removeLastOrNull()
                player.sendMessage(prefix + cmp("Removed last control point (${creator.points.size})"))
            }
        }

        literalArgument("render") {
            booleanArgument("close") {
                integerArgument("ticks") {
                    entitySelectorArgumentManyEntities("target") {
                        playerExecutor { player, args ->
                            val data = creators[player.uniqueId] ?: player.noEditor() ?: return@playerExecutor
                            val ticks = args[1] as Int
                            val locationsXZ = data.points.map { Point2D.Double(it.x, it.z) }
                            val splineXZ = CatmullRomSpline.create(locationsXZ, ticks, 0.5, args[0] as Boolean)
                            val pointsXZ = splineXZ.interpolatedPoints
                            val locationsView = data.points.map { Point2D.Double(it.yaw.toDouble(), it.pitch.toDouble()) }
                            val splineView = CatmullRomSpline.create(locationsView, ticks, 0.5, args[0] as Boolean)
                            val pointsView = splineView.interpolatedPoints
                            val locationsXY = data.points.map { Point2D.Double(it.x, it.y) }
                            val splineXY = CatmullRomSpline.create(locationsXY, ticks, 0.5, args[0] as Boolean)
                            val pointsXY = splineXY.interpolatedPoints

                            val world = player.world
                            data.interpolation = List(pointsXZ.size) { index ->
                                val xz = pointsXZ[index]
                                val xy = pointsXY[index]
                                val v = pointsView[index]
                                Location(world, xz.x.validate(), xy.y.validate(), xz.y.validate(), v.x.toFloat().validate(), v.y.toFloat().validate())
                            }

                            val targets = args[2] as List<Entity>
                            task(period = 1, howOften = pointsView.size.toLong()) {
                                val point = data.interpolation.getOrNull(it.counterUp!!.toInt()) ?: return@task
                                targets.forEach { e -> e.teleport(point) }
                            }
                        }
                    }
                }
            }
        }

        literalArgument("print") {
            entitySelectorArgumentManyEntities("target") {
                textArgument("name") {
                    playerExecutor { player, args ->
                        val name = args[1] as String
                        val file = File(dataPackFolder, "$name.mcfunction")
                        if (!file.parentFile.exists()) file.parentFile.mkdirs()
                        val target = args.getRaw(0) ?: "@s"
                        val data = creators[player.uniqueId] ?: player.noEditor() ?: return@playerExecutor

                        val content = buildString {
                            append(
                                "# Animation Generator (by Miraculixx & To_Binio)\n" +
                                        "#\n" +
                                        "# Settings Input\n" +
                                        "# - Control Points: ${data.points.size}\n" +
                                        "# - Target: $target\n"
                            )

                            val scoreID = UUID.randomUUID()
                            var ticker = 1
                            data.interpolation.forEach { pos ->
                                //execute if score c1e27c23-8bcd-40fa-84f3-96dd918e9e6c text-ticker matches <x> run teleport <target> <location>
                                append(
                                    "\nexecute if score $scoreID text-ticker matches $ticker run teleport $target " +
                                            "${pos.x.validate()} " +
                                            "${pos.y.validate()} " +
                                            "${pos.z.validate()} " +
                                            "${pos.yaw.validate()} " +
                                            "${pos.pitch.validate()}"
                                )
                                ticker++
                            }

                            append(
                                "\n\n# Looping & Reset\n" +
                                        "scoreboard players add $scoreID text-ticker 1\n" +
                                        "execute if score $scoreID text-ticker matches ..$ticker run schedule function animation:$name 1t replace\n" +
                                        "execute if score $scoreID text-ticker matches ${ticker + 1}.. run scoreboard players set $scoreID text-ticker 0"
                            )
                        }
                        file.writeText(content)

                        Bukkit.reloadData()
                        player.sendMessage(prefix + cmp("New animation created! Click here to play it").addCommand("/function animation:$name"))
                    }
                }
            }
        }
    }

    private fun Audience.noEditor(): CreatorData? {
        sendMessage(prefix + cmp("You don't have any animation creator! Create one via /animation new", cError))
        return null
    }

    private data class CreatorData(val points: MutableList<Location>, var interpolation: List<Location>)
}

private operator fun Vector3d.minus(other: Vector3d): Vector3d {
    return Vector3d(x - other.x, y - other.y, z - other.z)
}

private operator fun Vector3d.times(scalar: Double): Vector3d {
    return Vector3d(x * scalar, y * scalar, z * scalar)
}

fun Float.validate() = takeIf { it.isFinite() } ?: 0f
fun Double.validate() = takeIf { it.isFinite() } ?: 0.0