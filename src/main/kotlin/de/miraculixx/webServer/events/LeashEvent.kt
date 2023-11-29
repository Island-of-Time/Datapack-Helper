package de.miraculixx.webServer.events

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.webServer.utils.getNearbyEntities
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.Parrot
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class LeashEvent {
    val onFenceClick = listen<PlayerInteractEvent>(register = false) {
        val player = it.player
        val block = it.clickedBlock ?: return@listen
        val type = block.type
        if (!Tag.FENCES.isTagged(type)) return@listen

        val loc = block.location
        taskRunLater(1) {
            val nearby = loc.getNearbyEntities(1.0, 1.0, 1.0).map { e -> e.type }
            if (nearby.contains(EntityType.LEASH_HITCH)) {
                if (nearby.contains(EntityType.INTERACTION)) return@taskRunLater
                val interaction = loc.world!!.spawnEntity(loc.add(0.5, 0.3, 0.5), EntityType.INTERACTION) as Interaction
                interaction.interactionHeight = 0.6f
                interaction.interactionWidth = 0.45f
            }
        }

        val item = it.item ?: return@listen
        if (item.type != Material.LEAD) return@listen

        it.isCancelled = true

        val parrot = loc.world!!.spawnEntity(loc.add(.5, 0.3, .3), EntityType.PARROT) as Parrot
        parrot.setLeashHolder(player)
        parrot.setAI(false)
        parrot.setGravity(false)
        parrot.isSilent = true
        parrot.isSitting = true
        parrot.isPersistent = true
        parrot.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, -1, 1, false, false))
    }
}