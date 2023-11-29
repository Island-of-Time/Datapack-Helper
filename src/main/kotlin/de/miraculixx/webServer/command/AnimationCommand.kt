@file:Suppress("UNCHECKED_CAST")

package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.bukkit.addCommand
import de.miraculixx.kpaper.extensions.worlds
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.kpaper.runnables.task
import de.miraculixx.webServer.CatmullRomSpline
import de.miraculixx.webServer.interfaces.Module
import de.miraculixx.webServer.interfaces.Reloadable
import de.miraculixx.webServer.utils.SettingsManager
import de.miraculixx.webServer.utils.SettingsManager.animationFolder
import de.miraculixx.webServer.utils.SettingsManager.saveReadFile
import de.miraculixx.webServer.utils.SettingsManager.scoreboard
import de.miraculixx.webServer.utils.SettingsManager.settingsFolder
import de.miraculixx.webServer.utils.extensions.command
import de.miraculixx.webServer.utils.extensions.unregister
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.kotlindsl.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.awt.geom.Point2D
import java.io.File
import java.util.*

class AnimationCommand : Reloadable, Module {
    private lateinit var dataPackFolder: File
    private lateinit var packMetaFile: File
    private lateinit var functionFolder: File
    private val creators: MutableMap<UUID, CreatorData> = mutableMapOf()
    private val headerFile = File(settingsFolder, "header/animation.txt")
    private var header = saveReadFile(headerFile, "header/animation.txt")

    @Suppress("unused")
    private val command = command("animation") {
        withPermission("maptools.animation")

        literalArgument("new") {
            playerExecutor { player, _ ->
                creators[player.uniqueId] = CreatorData(mutableListOf(), mutableListOf())
                player.sendMessage(prefix + msg("command.animation.new"))
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
                player.sendMessage(prefix + msg("command.animation.addPoint"))
            }
        }

        literalArgument("remove-last") {
            playerExecutor { player, _ ->
                val creator = creators[player.uniqueId] ?: player.noEditor() ?: return@playerExecutor
                if (creator.points.isEmpty()) {
                    player.sendMessage(prefix + msg("command.animation.removeFailed"))
                    return@playerExecutor
                }
                creator.points.removeLastOrNull()
                player.sendMessage(prefix + msg("command.animation.remove", listOf(creator.points.size.toString())))
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
                            task(period = 1, howOften = pointsView.size.toLong(), endCallback = {
                                player.sendMessage(prefix + msg("command.animation.renderFinish"))
                            }) {
                                val point = data.interpolation.getOrNull(it.counterUp!!.toInt()) ?: return@task
                                targets.forEach { e -> e.teleport(point) }
                            }

                            player.sendMessage(prefix + msg("command.animation.render"))
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
                        val file = File(functionFolder, "$name.mcfunction")
                        if (!file.parentFile.exists()) file.parentFile.mkdirs()
                        val target = args.getRaw(0) ?: "@s"
                        val data = creators[player.uniqueId] ?: player.noEditor() ?: return@playerExecutor

                        val content = buildString {
                            append(header.replace("<points>", data.points.size.toString()).replace("<target>", target))

                            val scoreID = UUID.randomUUID()
                            var ticker = 1
                            data.interpolation.forEach { pos ->
                                //execute if score c1e27c23-8bcd-40fa-84f3-96dd918e9e6c text-ticker matches <x> run teleport <target> <location>
                                append(
                                    "\nexecute if score $scoreID $scoreboard matches $ticker run teleport $target " +
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
                                        "scoreboard players add $scoreID $scoreboard 1\n" +
                                        "execute if score $scoreID $scoreboard matches ..$ticker run schedule function animation:$name 1t replace\n" +
                                        "execute if score $scoreID $scoreboard matches ${ticker + 1}.. run scoreboard players set $scoreID $scoreboard 0"
                            )
                        }
                        file.writeText(content)
                        if (!packMetaFile.exists()) packMetaFile.writeBytes(SettingsManager.packMeta)

                        Bukkit.reloadData()
                        player.sendMessage(prefix + msg("command.animation.finish").addCommand("/function animation:$name"))
                    }
                }
            }
        }
    }

    private fun Player.noEditor(): CreatorData? {
        sendMessage(prefix + msg("command.animation.notStarted"))
        return null
    }

    // Overrides
    override fun reload() {
        header = saveReadFile(headerFile, "header/animation.txt")
        dataPackFolder = File("${worlds.first().name}/datapacks/${animationFolder}")
        packMetaFile = File(dataPackFolder, "pack.mcmeta")
        functionFolder = File(dataPackFolder, "data/animation/functions")
    }

    override fun disable() {
        command.unregister()
    }

    override fun enable() {
        command.register()
    }

    private data class CreatorData(val points: MutableList<Location>, var interpolation: List<Location>)

    // Number extensions
    private fun Float.validate() = takeIf { it.isFinite() } ?: 0f
    private fun Double.validate() = takeIf { it.isFinite() } ?: 0.0
}