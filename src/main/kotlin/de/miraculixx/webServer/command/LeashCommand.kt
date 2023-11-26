@file:Suppress("unused")

package de.miraculixx.webServer.command

import de.miraculixx.kpaper.event.register
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.webServer.events.LeashEvent
import de.miraculixx.webServer.interfaces.Module
import de.miraculixx.webServer.utils.extensions.command
import de.miraculixx.webServer.utils.extensions.unregister
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.LeashHitch
import org.bukkit.entity.Parrot
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class LeashCommand : Module {
    private val leashEvent = LeashEvent()

    private val command = command("leash") {
        withPermission("buildertools.leash")

        locationArgument("pos1", LocationType.BLOCK_POSITION) {
            locationArgument("pos2", LocationType.BLOCK_POSITION) {
                playerExecutor { player, args ->
                    val pos1 = args[0] as Location
                    val pos2 = args[1] as Location

                    val parrot = pos1.world.spawnEntity(pos1.clone().add(.5, 0.3, .3), EntityType.PARROT) as Parrot
                    parrot.setLeashHolder(player)
                    parrot.setAI(false)
                    parrot.setGravity(false)
                    parrot.isSilent = true
                    parrot.isSitting = true
                    parrot.isPersistent = true
                    parrot.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, -1, 1, false, false))

                    // Starting point
                    if (pos1.getNearbyEntitiesByType(LeashHitch::class.java, 1.0).isEmpty()) {
                        pos1.world.spawnEntity(pos1, EntityType.LEASH_HITCH) as LeashHitch
                        spawnInteraction(pos1)
                    }

                    // Ending point
                    val targetEntities = pos2.getNearbyEntitiesByType(LeashHitch::class.java, 1.0)
                    val target = if (targetEntities.isEmpty()) {
                        spawnInteraction(pos2)
                        pos2.world.spawnEntity(pos2, EntityType.LEASH_HITCH) as LeashHitch
                    } else targetEntities.first()
                    parrot.setLeashHolder(target)
                    player.sendMessage(prefix + msg("command.leash.spawn"))
                }
            }
        }
    }

    private fun spawnInteraction(location: Location) {
        val interaction = location.world.spawnEntity(location.add(0.5, 0.3, 0.5), EntityType.INTERACTION) as Interaction
        interaction.interactionHeight = 0.6f
        interaction.interactionWidth = 0.45f
    }

    override fun disable() {
        leashEvent.onFenceClick.unregister()
        command.unregister()
    }

    override fun enable() {
        leashEvent.onFenceClick.register()
        command.register()
    }
}