package de.miraculixx.webServer.command

import de.miraculixx.webServer.Main
import de.miraculixx.webServer.utils.*
import dev.jorel.commandapi.IStringTooltip
import dev.jorel.commandapi.StringTooltip
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.kotlindsl.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import java.io.File

class WarpCommand {
    private val positions: MutableMap<String, LiteLocation>
    private val file = File(Main.settingsManager.settingsFolder, "positions.json")

    init {
        positions = if (file.exists()) {
            Json.decodeFromString(file.readText().ifBlank { "{}" })
        } else mutableMapOf()
    }

    @Suppress("unused")
    val command = commandTree("position") {
        withAliases("pos", "location", "loc")
        literalArgument("tp") {
            argument(StringArgument("name").replaceSuggestions(ArgumentSuggestions.stringsWithTooltips { getPositionNames() })) {
                playerExecutor { player, args ->
                    val name = args[0] as String
                    val position = positions[name] ?: return@playerExecutor
                    player.teleport(position.toLocation())
                    player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)
                    player.sendMessage(prefix + cmp("Zu Position $name teleportiert! ($position)"))
                }
            }
        }
        literalArgument("new") {
            stringArgument("name") {
                playerExecutor { player, args ->
                    val name = args[0] as String
                    val location = player.location
                    val liteLoc = LiteLocation(location.blockX, location.blockY, location.blockZ, location.world.name)
                    positions[name] = liteLoc
                    player.sendMessage(prefix + cmp("Warp $name bei $liteLoc erstellt!", cSuccess))
                }
            }
        }
        literalArgument("remove") {
            argument(StringArgument("name").replaceSuggestions(ArgumentSuggestions.stringsWithTooltips { getPositionNames() })) {
                anyExecutor { commandSender, args ->
                    val name = args[0] as String
                    positions.remove(name)
                    commandSender.sendMessage(prefix + cmp("Warp $name wurde gelöscht!", cError))
                }
            }
        }
        literalArgument("reset") {
            anyExecutor { commandSender, _ ->
                positions.clear()
                commandSender.sendMessage(prefix + cmp("Alle Warps gelöscht!", cError))
            }
        }.withPermission("mutils.position.reset")
    }

    private fun getPositionNames(): Array<IStringTooltip> {
        return positions.map { StringTooltip.ofString(it.key, it.value.toString()) }.toTypedArray()
    }

    fun saveFile() {
        if (!file.exists()) file.parentFile.mkdirs()
        file.writeText(json.encodeToString(positions))
    }
}

@Serializable
data class LiteLocation(val x: Int, val y: Int, val z: Int, val world: String) {
    override fun toString() = "$x $y $z ($world)"
    fun toLocation() = Location(Bukkit.getWorld(world), x.toDouble(), y.toDouble(), z.toDouble())
}