@file:Suppress("unused")

package de.miraculixx.maptools.command

import de.miraculixx.kpaper.event.register
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.maptools.events.LeashEvent
import de.miraculixx.maptools.interfaces.Module
import de.miraculixx.maptools.utils.extensions.command
import de.miraculixx.maptools.utils.extensions.unregister
import de.miraculixx.maptools.utils.plus
import de.miraculixx.maptools.utils.prefix
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import org.bukkit.Location

class LeashCommand : Module {
    private val leashEvent = LeashEvent

    private val command = command("leash") {
        withAliases("knot")
        withPermission("maptools.leash")

        locationArgument("pos1", LocationType.BLOCK_POSITION) {
            locationArgument("pos2", LocationType.BLOCK_POSITION) {
                playerExecutor { player, args ->
                    val pos1 = args[0] as Location
                    val pos2 = args[1] as Location

                    // Starting point
                    val parrot = LeashEvent.spawnParrot(pos1)
                    LeashEvent.spawnKnot(pos1)

                    // Ending point
                    val knot = LeashEvent.spawnKnot(pos2)
                    parrot.setLeashHolder(knot)
                    player.sendMessage(prefix + msg("command.leash.spawn"))
                }
            }
        }
    }

    override fun disable() {
        leashEvent.onFenceClick.unregister()
        leashEvent.onDisLeash.unregister()
        command.unregister()
    }

    override fun enable() {
        leashEvent.onFenceClick.register()
        leashEvent.onDisLeash.register()
        command.register()
    }
}