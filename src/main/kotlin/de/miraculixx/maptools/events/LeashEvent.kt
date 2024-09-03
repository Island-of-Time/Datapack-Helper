package de.miraculixx.maptools.events

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.runnables.taskRunLater
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.LeashHitch
import org.bukkit.entity.Parrot
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityUnleashEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType


object LeashEvent {
    private val leashedPlayer: MutableSet<Player> = mutableSetOf()

    val onFenceClick = listen<PlayerInteractEvent>(register = false) {
        val player = it.player
        val block = it.clickedBlock ?: return@listen
        val type = block.type
        if (!Tag.FENCES.isTagged(type)) return@listen

        val loc = block.location
        taskRunLater(1) {
            val nearby = loc.getNearbyEntities(1.0, 1.0, 1.0).filter { e -> e.location.block == loc.block }.map { e -> e.type }
            if (nearby.contains(EntityType.LEASH_KNOT)) {
                if (nearby.contains(EntityType.INTERACTION)) return@taskRunLater
                spawnInteraction(loc)
            }
        }

        if (leashedPlayer.contains(player)) {
            leashedPlayer.remove(player)
            return@listen
        }
        val item = it.item ?: return@listen
        if (item.type != Material.LEAD) return@listen

        it.isCancelled = true

        leashedPlayer.add(player)
        spawnParrot(loc).setLeashHolder(player)
        spawnKnot(loc)
    }

    val onDisLeash = listen<EntityUnleashEvent> {
        val entity = it.entity
        if (entity is Parrot) {
            entity.location.getNearbyEntities(0.4, 0.4, 0.4).forEach { e ->
                if (e is Interaction || e is LeashHitch) e.remove()
            }
            entity.remove()
        }
    }

    fun spawnParrot(location: Location): Parrot {
        val parrot = location.world.spawnEntity(location.clone().add(.5, 0.3, .3), EntityType.PARROT) as Parrot
        parrot.setAI(false)
        parrot.setGravity(false)
        parrot.isSilent = true
        parrot.isSitting = true
        parrot.isPersistent = true
        parrot.isInvulnerable = true
        parrot.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, -1, 1, false, false))
        return parrot
    }

    private fun spawnInteraction(location: Location) {
        val interaction = location.world.spawnEntity(location.clone().add(0.5, 0.3, 0.5), EntityType.INTERACTION) as Interaction
        interaction.interactionHeight = 0.6f
        interaction.interactionWidth = 0.45f
    }

    fun spawnKnot(location: Location): LeashHitch {
        // Spawn interaction if not already present
        if (location.getNearbyEntitiesByType(Interaction::class.java, 0.3).none { it.location.block == location.block }) {
            spawnInteraction(location)
        }

        // Spawn knot if not already present
        return location.getNearbyEntitiesByType(LeashHitch::class.java, 0.3).firstOrNull() ?: location.world.spawnEntity(location, EntityType.LEASH_KNOT) as LeashHitch
    }
}
