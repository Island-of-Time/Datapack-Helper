@file:Suppress("UNCHECKED_CAST")

package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.bukkit.addCommand
import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.extensions.geometry.toSimpleBlockString
import de.miraculixx.kpaper.extensions.kotlin.round
import de.miraculixx.kpaper.extensions.worlds
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.kpaper.localization.msgString
import de.miraculixx.kpaper.runnables.sync
import de.miraculixx.kpaper.runnables.task
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.webServer.interfaces.Reloadable
import de.miraculixx.webServer.utils.*
import de.miraculixx.webServer.utils.SettingsManager.pathingFolder
import dev.jorel.commandapi.kotlindsl.*
import dev.jorel.commandapi.wrappers.CommandResult
import dev.jorel.commandapi.wrappers.Rotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.kyori.adventure.audience.Audience
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.joml.Vector3d
import java.io.File
import java.text.DecimalFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.time.Duration.Companion.milliseconds

class PathingCommand : Reloadable {
    private val creators: MutableMap<UUID, PathingData> = mutableMapOf()
    private var dataPackFolder = File("world/datapacks/${pathingFolder}/data/animation/functions")
    private val format = DecimalFormat("#").apply { maximumFractionDigits = 4 }

    val command = commandTree("pathing") {
        withPermission("buildertools.pathing")

        literalArgument("new") {
            entitySelectorArgumentManyEntities("target") {
                playerExecutor { player, args ->
                    val target = args[0] as List<Entity>
                    creators[player.uniqueId] = PathingData(target, args.getRaw(0)!!, mutableListOf())
                    player.sendMessage(prefix + msg("command.pathing.new"))
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
                            val data = creators[player.uniqueId] ?: player.noEditor() ?: return@playerExecutor
                            player.sendMessage(prefix + msg("command.pathing.newControl", listOf(location.toSimpleBlockString())))

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
                    creators[player.uniqueId]?.actions?.add(PathingAction(PathingType.DELAY, time = delay.toDouble())) ?: player.noEditor() ?: return@playerExecutor
                    player.sendMessage(prefix + msg("command.pathing.newDelay", listOf(delay.toString())))
                }
            }
        }

        literalArgument("add-script") {
            withPermission("buildertools.multitool-execute")

            commandArgument("command") {
                playerExecutor { player, args ->
                    val command = args[0] as CommandResult
                    val finalCommand = buildString {
                        append(command.command.name)
                        command.args.forEach { append(" $it") }
                    }
                    creators[player.uniqueId]?.actions?.add(PathingAction(PathingType.RUN_SCRIPT, script = finalCommand)) ?: player.noEditor() ?: return@playerExecutor
                    player.sendMessage(prefix + msg("command.pathing.newCommand", listOf(finalCommand)).addCommand("/$finalCommand"))
                }
            }
        }

        literalArgument("add-repeat") {
            integerArgument("amount", 1) {
                commandArgument("command") {
                    playerExecutor { player, args ->
                        val amount = args[0] as Int
                        val command = args[1] as CommandResult
                        val finalCommand = buildString {
                            append(command.command.name)
                            command.args.forEach { append(" $it") }
                        }
                        creators[player.uniqueId]?.actions?.add(PathingAction(PathingType.REPEAT, script = finalCommand, time = amount.toDouble())) ?: player.noEditor() ?: return@playerExecutor
                        player.sendMessage(prefix + msg("command.pathing.newRepeating", listOf(finalCommand, amount.toString())))
                    }
                }
            }
        }

        literalArgument("remove-last") {
            playerExecutor { player, _ ->
                val creator = creators[player.uniqueId] ?: player.noEditor() ?: return@playerExecutor
                if (creator.actions.removeLastOrNull() == null) {
                    player.sendMessage(prefix + msg("command.pathing.noPoints"))
                    return@playerExecutor
                }
                player.sendMessage(prefix + msg("command.pathing.removeLast"))
            }
        }

        literalArgument("play") {
            playerExecutor { player, _ ->
                val data = creators[player.uniqueId] ?: return@playerExecutor
                CoroutineScope(Dispatchers.Default).launch {
                    var lastPoint: Location? = null
                    val first = data.actions.firstOrNull { it.type == PathingType.CONTROL_POINT }
                    first?.let { l -> sync { data.target.forEach { e -> e.teleport(l.location!!) } } }
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
                                sync { Bukkit.dispatchCommand(console, "execute as ${data.rawTarget} at @s run ${action.script}") }
                                1
                            }

                            PathingType.REPEAT -> {
                                val amount = action.time!!.toInt()
                                task(true, period = 1, howOften = amount.toLong()) {
                                    Bukkit.dispatchCommand(console, "execute as ${data.rawTarget} at @s run ${action.script}")
                                }
                                amount
                            }
                        }

                        delay((50L * delay).milliseconds)
                    }
                    player.sendMessage(prefix + msg("command.pathing.finished"))
                }
            }
        }

        literalArgument("print") {
            textArgument("name") {
                playerExecutor { player, args ->
                    val name = args[0] as String
                    val file = File(dataPackFolder, "$name.mcfunction")
                    if (!file.parentFile.exists()) file.parentFile.mkdirs()
                    val data = creators[player.uniqueId] ?: return@playerExecutor
                    File(dataPackFolder, "$name.json").writeText(json.encodeToString(PathingJsonData(data.rawTarget, data.actions)))

                    printToFunction(data.actions, data.rawTarget, file, name)
                    Bukkit.reloadData()
                    player.sendMessage(prefix + msg("command.pathing.printed", listOf(name)))
                    player.sendMessage(prefix + msg("command.pathing.clickToPlay").addCommand("/function animation:$name.mcfunction"))
                }
            }
        }

        literalArgument("reprint") {
            textArgument("name") {
                anyExecutor { sender, args ->
                    val name = args[0] as String
                    val file = File(dataPackFolder, "${name.removeSuffix(".json")}.json")
                    if (!file.exists()) {
                        sender.sendMessage(prefix + cmp(msgString("common.fileNotFound"), cError))
                        return@anyExecutor
                    }
                    val data = json.decodeFromString<PathingJsonData>(file.readText())

                    printToFunction(data.actions, data.target, File(dataPackFolder, "$name.mcfunction"), name)
                    Bukkit.reloadData()
                    sender.sendMessage(prefix + cmp("$name was reprinted to function"))
                }
            }
        }

        literalArgument("load") {
            textArgument("name") {
                playerExecutor { sender, args ->
                    val name = args[0] as String
                    val file = File(dataPackFolder, "$name.json")
                    if (!file.exists()) {
                        sender.sendMessage(prefix + cmp("The file $name.json does not exist!", cError))
                        return@playerExecutor
                    }
                    val data = json.decodeFromString<PathingJsonData>(file.readText())
                    val entities = Bukkit.selectEntities(sender, data.target)

                    creators[sender.uniqueId] = PathingData(entities, data.target, data.actions)
                    sender.sendMessage(prefix + cmp("Pathing $name loaded!"))
                }
            }
        }
    }

    private fun printToFunction(data: List<PathingAction>, target: String, file: File, name: String) {
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
                        append(
                            "\nexecute if score $uuid text-ticker matches $counter as $target at @s run tp @s ${lastCurrent.x.format()} ${lastCurrent.y.format()} ${lastCurrent.z.format()} " +
                                    "${lastCurrent.yaw.format()} ${lastCurrent.pitch.format()}"
                        )
                        val relativeMovement = animationData.first.firstOrNull()
                        val counterTo = counter + animationData.first.size
                        if (relativeMovement != null)
                            append("\nexecute if score $uuid text-ticker matches $counter..$counterTo as $target at @s run tp @s ~${relativeMovement.x.format()} ~${relativeMovement.y.format()} ~${relativeMovement.z.format()} ${relativeMovement.yaw.format()} ~")
                        counter = counterTo
                        val finalLoc = animationData.second
                        append("\nexecute if score $uuid text-ticker matches $counter as $target at @s run tp @s ${finalLoc.x.format()} ${finalLoc.y.format()} ${finalLoc.z.format()} ${finalLoc.yaw.format()} ${finalLoc.pitch.format()}")
                        lastPoint = current
                        counter++
                    }

                    PathingType.DELAY -> counter += action.time?.toInt() ?: 0

                    PathingType.RUN_SCRIPT -> {
                        append("\nexecute if score $uuid text-ticker matches $counter as $target at @s run ${action.script}")
                        counter++
                    }

                    PathingType.REPEAT -> {
                        val amount = action.time?.toInt() ?: 1
                        append("\nexecute if score $uuid text-ticker matches $counter..${counter + amount - 1} as $target at @s run ${action.script}")
                        counter += amount
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
                    (if (xPositive || primary != CoordinateAxis.X) moveX else -moveX).round(5),
                    (if (yPositive || primary != CoordinateAxis.Y) moveY else -moveY).round(5),
                    (if (zPositive || primary != CoordinateAxis.Z) moveZ else -moveZ).round(5),
                    yaw.toFloat().round(3), 0f
                )
                add(relativeLoc)
            }
        } to to
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

    private fun Player.noEditor(): PathingData? {
        sendMessage(prefix + cmp("You don't have any pathing creator! Create one via /pathing new", cError))
        return null
    }

    private fun Number.format() = format.format(this)

    override fun reload() {
        dataPackFolder = File("world/datapacks/${pathingFolder}/data/animation/functions")
    }

    @Serializable
    private data class PathingJsonData(val target: String, val actions: MutableList<PathingAction>)

    private data class PathingData(val target: List<Entity>, val rawTarget: String, val actions: MutableList<PathingAction>)

    @Serializable
    private data class PathingAction(
        val type: PathingType,
        val location: @Serializable(with = LocationSerializer::class) Location? = null,
        val time: Double? = null,
        val script: String? = null
    )

    private enum class PathingType {
        CONTROL_POINT,
        DELAY,
        RUN_SCRIPT,
        REPEAT
    }

    private enum class CoordinateAxis {
        X, Y, Z
    }
}