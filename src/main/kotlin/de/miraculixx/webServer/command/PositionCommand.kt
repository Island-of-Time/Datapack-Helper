package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.webServer.interfaces.DataHolder
import de.miraculixx.webServer.interfaces.Reloadable
import de.miraculixx.webServer.utils.*
import de.miraculixx.webServer.utils.SettingsManager.settingsFolder
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

class WarpCommand : Reloadable, DataHolder {
    private val positions: MutableMap<String, LiteLocation>
    private val file = File(settingsFolder, "positions.json")

    init {
        positions = if (file.exists()) {
            Json.decodeFromString(file.readText().ifBlank { "{}" })
        } else mutableMapOf()
    }

    @Suppress("unused")
    val command = commandTree("position") {
        withPermission("mutils.position")

        withAliases("pos", "location", "loc")
        literalArgument("tp") {
            argument(StringArgument("name").replaceSuggestions(ArgumentSuggestions.stringsWithTooltips { getPositionNames() })) {
                playerExecutor { player, args ->
                    val name = args[0] as String
                    val position = positions[name]
                    if (position == null) {
                        player.sendMessage(prefix + msg("command.position.noPosition", listOf(name)))
                        return@playerExecutor
                    }
                    player.teleport(position.toLocation())
                    player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)
                    player.sendMessage(prefix + msg("command.position.teleported", listOf(name, position.toString())))
                }
            }
        }
        literalArgument("new") {
            withPermission("mutils.position.manage")
            stringArgument("name") {
                playerExecutor { player, args ->
                    val name = args[0] as String
                    val location = player.location
                    val liteLoc = LiteLocation(location.blockX, location.blockY, location.blockZ, location.world.name)
                    positions[name] = liteLoc
                    player.sendMessage(prefix + msg("command.position.created", listOf(name, liteLoc.toString())))
                }
            }
        }
        literalArgument("remove") {
            withPermission("mutils.position.manage")
            argument(StringArgument("name").replaceSuggestions(ArgumentSuggestions.stringsWithTooltips { getPositionNames() })) {
                anyExecutor { commandSender, args ->
                    val name = args[0] as String
                    if (positions.remove(name) == null) {
                        commandSender.sendMessage(prefix + msg("command.position.noPosition", listOf(name)))
                        return@anyExecutor
                    }
                    commandSender.sendMessage(prefix + msg("command.position.deleted"))
                }
            }
        }
        literalArgument("reset") {
            withPermission("mutils.position.manage")
            anyExecutor { commandSender, _ ->
                positions.clear()
                commandSender.sendMessage(prefix + msg("command.position.resetted"))
            }
        }
    }

    private fun getPositionNames(): Array<IStringTooltip> {
        return positions.map { StringTooltip.ofString(it.key, it.value.toString()) }.toTypedArray()
    }

    override fun save() {
        if (!file.exists()) file.parentFile.mkdirs()
        file.writeText(json.encodeToString(positions))
    }

    override fun load() {
        positions.clear()
        if (file.exists()) {
            val content = file.readText().ifBlank { "{}" }
            try {
                json.decodeFromString<Map<String, LiteLocation>>(content).forEach { (t, u) -> positions[t] = u }
            } catch (e: Exception) {
                console.sendMessage(prefix + cmp("Failed to load positions!", cError))
                console.sendMessage(prefix + cmp("Reason: ${e.message ?: "Unknown"}", cError))
            }
        }
    }

    override fun reload() {
        load()
    }
}

@Serializable
data class LiteLocation(val x: Int, val y: Int, val z: Int, val world: String) {
    override fun toString() = "$x $y $z ($world)"
    fun toLocation() = Location(Bukkit.getWorld(world), x.toDouble(), y.toDouble(), z.toDouble())
}