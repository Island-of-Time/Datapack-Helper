@file:Suppress("UNCHECKED_CAST")

package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.bukkit.addCommand
import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.extensions.kotlin.round
import de.miraculixx.kpaper.extensions.worlds
import de.miraculixx.kpaper.runnables.sync
import de.miraculixx.kpaper.runnables.task
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.webServer.utils.cMark
import de.miraculixx.webServer.utils.cmp
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.kotlindsl.*
import dev.jorel.commandapi.wrappers.CommandResult
import dev.jorel.commandapi.wrappers.Rotation
import kotlinx.coroutines.*
import net.kyori.adventure.audience.Audience
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.joml.Vector3d
import java.io.File
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.time.Duration.Companion.milliseconds

class PathingCommand {
    private val creators: MutableMap<UUID, PathingData> = mutableMapOf()
    private val dataPackFolder = File("world/datapacks/iot-general/data/animation/functions")

    /**
     * - Control Points
     *    >  Speed
     * - Pause (ticks)
     * - Execute
     * - Remove last
     */

    val command = commandTree("pathing") {
        literalArgument("new") {
            entitySelectorArgumentManyEntities("target") {
                playerExecutor { player, args ->
                    val target = args[0] as List<Entity>
                    creators[player.uniqueId] = PathingData(target, mutableListOf())
                    player.sendMessage(prefix + cmp("New Pathing script started!"))
                    target.forEach { e -> e.isGlowing = true }
                    taskRunLater(10) { target.forEach { e -> e.isGlowing = false } }
                }
            }
        }

        literalArgument("add-point") {
            locationArgument("point") {
                rotationArgument("view") {
                    doubleArgument("speed") {
                        playerExecutor { player, args ->
                            val location = args[0] as Location
                            val speed = args[2] as Double
                            val view = args[1] as Rotation
                            location.apply {
                                yaw = view.yaw
                                pitch = view.pitch
                            }
                            val data = creators[player.uniqueId] ?: return@playerExecutor
                            player.sendMessage(prefix + cmp("Add new control point"))

                            //Play the current step
                            val targets = data.target
                            val previous = data.actions.lastOrNull { it.type == PathingType.CONTROL_POINT }
                            if (previous == null) data.target.forEach { e -> e.teleport(location) }
                            else {
                                val animationData = animateFromTo(previous.location!!, location, speed)
                                player.animateDirectly(animationData, targets)
                            }
                            data.actions.add(PathingAction(PathingType.CONTROL_POINT, location, speed))
                        }
                    }
                }
            }
        }

        literalArgument("add-delay") {
            integerArgument("delay") {
                playerExecutor { player, args ->
                    val delay = args[0] as Int
                    creators[player.uniqueId]?.actions?.add(PathingAction(PathingType.DELAY, time = delay.toDouble())) ?: return@playerExecutor
                    player.sendMessage(prefix + cmp("Add $delay ticks delay"))
                }
            }
        }

        literalArgument("add-script") {
            commandArgument("command") {
                playerExecutor { player, args ->
                    val command = args[0] as CommandResult
                    val finalCommand = buildString {
                        append(command.command.name)
                        command.args.forEach { append(" $it") }
                    }
                    creators[player.uniqueId]?.actions?.add(PathingAction(PathingType.RUN_SCRIPT, script = finalCommand)) ?: return@playerExecutor
                    player.sendMessage(prefix + cmp("Added new command ") + cmp(finalCommand, cMark).addCommand("/$finalCommand"))
                }
            }
        }

        literalArgument("remove-last") {
            playerExecutor { player, _ ->
                creators[player.uniqueId]?.actions?.removeLastOrNull() ?: return@playerExecutor
                player.sendMessage(prefix + cmp("Removed last action"))
            }
        }

        literalArgument("finish") {
            textArgument("target") {
                playerExecutor { player, args ->
                    val data = creators[player.uniqueId] ?: return@playerExecutor
                    val target = args[0] as String
                    CoroutineScope(Dispatchers.Default).launch {
                        var lastPoint: Location? = null
                        data.actions.forEach { action ->
                            val delay = when (action.type) {
                                PathingType.CONTROL_POINT -> {
                                    val animationData = animateFromTo(lastPoint ?: action.location!!, action.location!!, action.time!!)
                                    Audience.empty().animateDirectly(animationData, data.target)
                                    lastPoint = action.location
                                    animationData.first.size + 1
                                }

                                PathingType.DELAY -> action.time!!.toInt()
                                PathingType.RUN_SCRIPT -> {
                                    sync { Bukkit.dispatchCommand(console, "execute as $target at @s run ${action.script}") }
                                    1
                                }
                            }

                            delay((50L * delay).milliseconds)
                        }
                    }
                }
            }
        }

        literalArgument("print") {
            textArgument("target") {
                textArgument("name") {
                    playerExecutor { player, args ->
                        val name = args[1] as String
                        val file = File(dataPackFolder, "$name.mcfunction")
                        if (!file.parentFile.exists()) file.parentFile.mkdirs()
                        val target = args[0] as String
                        val data = creators[player.uniqueId]?.actions ?: return@playerExecutor

                        val uuid = UUID.randomUUID()
                        val content = buildString {
                            append(
                                "# Animation Generator (by Miraculixx & To_Binio)\n" +
                                        "#\n" +
                                        "# Settings Input\n" +
                                        "# - Control Points: ${data.filter { it.type == PathingType.CONTROL_POINT }.size}\n" +
                                        "# - Scripts: ${data.filter { it.type == PathingType.RUN_SCRIPT }.size}\n" +
                                        "# - Target: $target\n"
                            )

                            var counter = 1
                            var lastPoint: Location? = null
                            data.forEach { action ->
                                when (action.type) {
                                    PathingType.CONTROL_POINT -> {
                                        val current = action.location ?: return@forEach
                                        val lastCurrent = lastPoint ?: current
                                        val animationData = animateFromTo(lastCurrent, current, action.time!!)
                                        append("\nexecute if score $uuid text-ticker matches $counter as $target at @s run tp @s ${lastCurrent.x} ${lastCurrent.y} ${lastCurrent.z} ${lastCurrent.yaw.round(3)} ${lastCurrent.pitch.round(3)}")
                                        val relativeMovement = animationData.first.firstOrNull()
                                        val counterTo = counter + animationData.first.size
                                        if (relativeMovement != null)
                                            append("\nexecute if score $uuid text-ticker matches $counter..$counterTo as $target at @s run tp @s ~${relativeMovement.x} ~${relativeMovement.y} ~${relativeMovement.z} ${relativeMovement.yaw} ~")
                                        counter = counterTo
                                        val finalLoc = animationData.second
                                        append("\nexecute if score $uuid text-ticker matches $counter as $target at @s run tp @s ${finalLoc.x} ${finalLoc.y} ${finalLoc.z} ${finalLoc.yaw} ${finalLoc.pitch}")
                                        lastPoint = current
                                        counter++
                                    }

                                    PathingType.DELAY -> counter += action.time?.toInt() ?: 0

                                    PathingType.RUN_SCRIPT -> {
                                        append("\nexecute if score $uuid text-ticker matches $counter as $target at @s run ${action.script}")
                                        counter++
                                    }
                                }
                            }

                            append(
                                "\n\n# Looping & Reset\n" +
                                        "scoreboard players add $uuid text-ticker 1\n" +
                                        "execute if score $uuid text-ticker matches ..$counter run schedule function animation:$name 1t replace\n" +
                                        "execute if score $uuid text-ticker matches ${counter + 1}.. run scoreboard players set $uuid text-ticker 0"
                            )
                        }

                        file.writeText(content)
                    }
                }
            }
        }
    }

    /**
     * @param onTeleport Location Vector - Relative (yes or no)
     */
    private fun animateFromTo(from: Location, to: Location, speed: Double): Pair<List<Location>, Location> {
        val distanceLocation = Vector3d(to.x - from.x, to.y - from.y, to.z - from.z)
        val absoluteDistance = Vector3d(abs(distanceLocation.x), abs(distanceLocation.y), abs(distanceLocation.z))

        val alreadyMoved = Vector3d(0.0, 0.0, 0.0)
        val xPositive = distanceLocation.x >= 0
        val yPositive = distanceLocation.y >= 0
        val zPositive = distanceLocation.z >= 0
        val primary = when {
            absoluteDistance.x >= absoluteDistance.y && absoluteDistance.x >= absoluteDistance.z -> CoordinateAxis.X
            absoluteDistance.z >= absoluteDistance.y && absoluteDistance.z >= absoluteDistance.x -> CoordinateAxis.Z
            else -> CoordinateAxis.Y
        }
        val maxTime = when (primary) {
            CoordinateAxis.X -> absoluteDistance.x / speed
            CoordinateAxis.Y -> absoluteDistance.y / speed
            CoordinateAxis.Z -> absoluteDistance.z / speed
        }

        val yaw = atan2(distanceLocation.z, distanceLocation.x) * (180 / PI) - 90

        return buildList {
            repeat(maxTime.toInt()) {
                val moveX = if (primary == CoordinateAxis.X) (absoluteDistance.x - alreadyMoved.x).coerceAtMost(speed) else distanceLocation.x / maxTime
                val moveY = if (primary == CoordinateAxis.Y) (absoluteDistance.y - alreadyMoved.y).coerceAtMost(speed) else distanceLocation.y / maxTime
                val moveZ = if (primary == CoordinateAxis.Z) (absoluteDistance.z - alreadyMoved.z).coerceAtMost(speed) else distanceLocation.z / maxTime

                alreadyMoved.x += moveX
                alreadyMoved.y += moveY
                alreadyMoved.z += moveZ

                val relativeLoc = Location(
                    worlds[0],
                    if (xPositive || primary != CoordinateAxis.X) moveX else -moveX,
                    if (yPositive || primary != CoordinateAxis.Y) moveY else -moveY,
                    if (zPositive || primary != CoordinateAxis.Z) moveZ else -moveZ,
                    yaw.toFloat(), 0f
                )
                add(relativeLoc)
            }
        } to to

//        task(period = 1, howOften = maxTime.toLong(), endCallback = {
//            onTeleport.invoke(to, false)
//            sendMessage(prefix + cmp("Animation finished!"))
//        }) {
//
//            onTeleport.invoke(relativeLoc, true)
//        }
//        return maxTime.toInt() + 1
    }

    private fun Audience.animateDirectly(pair: Pair<List<Location>, Location>, target: List<Entity>) {
        pair.first.firstOrNull()?.let { sync { target.forEach { e -> e.tpRelative(it) } } }
        task(period = 1, howOften = pair.first.size.toLong(), endCallback = {
            sync { target.forEach { e -> e.teleport(pair.second) } }
            sendMessage(prefix + cmp("Animation finished!"))
        }) {
            val loc = pair.first.getOrNull(it.counterUp?.toInt()!!) ?: return@task
            sync { target.forEach { e -> e.tpRelative(loc) } }
        }
    }

    private fun Entity.tpRelative(vec: Location) {
        teleport(location.add(
            vec.x, vec.y, vec.z
        ).apply { yaw = vec.yaw })
    }

    private data class PathingData(val target: List<Entity>, val actions: MutableList<PathingAction>)
    private data class PathingAction(
        val type: PathingType,
        val location: Location? = null,
        val time: Double? = null,
        val script: String? = null
    )

    private enum class PathingType {
        CONTROL_POINT,
        DELAY,
        RUN_SCRIPT
    }

    private enum class CoordinateAxis {
        X, Y, Z
    }
}