package de.miraculixx.webServer.events

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.extensions.broadcast
import de.miraculixx.kpaper.extensions.geometry.add
import de.miraculixx.kpaper.extensions.onlinePlayers
import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.runnables.task
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.webServer.Main
import de.miraculixx.webServer.utils.cmp
import de.miraculixx.webServer.utils.gui.logic.InventoryUtils.get
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import de.miraculixx.webServer.utils.soundEnable
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent
import org.bukkit.*
import org.bukkit.block.data.BlockData
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Marker
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerChangedMainHandEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import org.joml.Vector3d
import java.util.*
import kotlin.collections.set
import kotlin.math.cos
import kotlin.math.sin

class ConvertorEvent {
    private val key = NamespacedKey(Main.INSTANCE, "webserver.command-item")
    private val key2 = NamespacedKey(Main.INSTANCE, "webserver.command-item-tag")
    private val deletors: MutableMap<UUID, MutableMap<BlockDisplay, BlockData>> = mutableMapOf()

    private val cooldown: MutableSet<Player> = mutableSetOf()
    private val onClick = listen<PlayerInteractEvent> {
        if (it.hand == EquipmentSlot.OFF_HAND) return@listen
        if (it.isAsynchronous) return@listen
        val item = it.item ?: return@listen
        val meta = item.itemMeta ?: return@listen
        if (meta.customModel != 100) return@listen
        val player = it.player

        when (item.type) {
            Material.SHEARS -> {
                val block = it.clickedBlock ?: return@listen
                if (cooldown.contains(player)) return@listen
                val dataContainer = item.itemMeta?.persistentDataContainer ?: return@listen
                val tag = dataContainer.get(key2, PersistentDataType.STRING) ?: return@listen
                val scale = dataContainer.get(key, PersistentDataType.FLOAT) ?: return@listen
                it.isCancelled = true
                player.performCommand("convertblock ${block.x} ${block.y} ${block.z} $tag $scale")

            }

            Material.SLIME_BALL -> {
                it.isCancelled = true
                if (cooldown.contains(player)) return@listen
                val deletor = deletors.getOrPut(player.uniqueId) { mutableMapOf() }
                if (player.isSneaking) {
                    deletor.forEach { bd -> bd.key.block = bd.value }
                    deletor.clear()
                    return@listen
                }

                if (it.action.isLeftClick) { //Delete
                    deletor.forEach { bd -> bd.key.remove() }
                    player.sendMessage(prefix + cmp("Killed ${deletor.size} Blocks"))
                    deletor.clear()
                } else if (it.action.isRightClick) {
                    broadcast(cmp("Start Ray"))
                    val start = player.location.add(0.0, 1.5, 0.0)
                    val distance = item.itemMeta?.persistentDataContainer?.get(key2, PersistentDataType.INTEGER) ?: return@listen
                    raycast(start, start.yaw - 90, start.pitch, distance) { loc ->
                        loc.world.spawnParticle(Particle.COMPOSTER, loc, 1, .0, .0, .0, 0.1)
                        val vec = Vector(loc.x, loc.y, loc.z)
                        loc.getNearbyEntitiesByType(BlockDisplay::class.java, 3.0).forEach { bd ->
                            if (deletor.contains(bd)) return@forEach
                            val bdLoc = bd.location
                            val transformation = bd.transformation
                            val translation = transformation.translation
                            val scale = transformation.scale
                            val sourceVec = Vector(bdLoc.x + translation.x, bdLoc.y + translation.y, bdLoc.z + translation.z)
                            val boundingBox = BoundingBox.of(
                                sourceVec,
                                sourceVec.clone().add(Vector(scale.x, scale.y, scale.z))
                            )
                            if (!boundingBox.contains(vec)) return@forEach
                            deletor[bd] = bd.block.clone()
                            bd.block = Bukkit.createBlockData(Material.YELLOW_CONCRETE)
                        }
                    }
                    player.sendMessage(prefix + cmp("Stop Ray"))
                }
            }

            Material.SPECTRAL_ARROW -> {
                val block = it.clickedBlock ?: return@listen
                val tag = meta.persistentDataContainer.get(key, PersistentDataType.STRING)
                it.isCancelled = true
                if (it.action == Action.RIGHT_CLICK_BLOCK) {
                    val marker = block.world.spawn(block.location.subtract(-.5, -.5, -.5), Marker::class.java)
                    marker.scoreboardTags.add(tag)
                    player.soundEnable()
                } else {
                    block.location.getNearbyEntitiesByType(Marker::class.java, 1.0).forEach { e ->
                        if (e.scoreboardTags.contains(tag)) {
                            e.remove()
                            player.sendMessage(prefix + cmp("Removed a marker"))
                        }
                    }
                }
            }

            else -> Unit
        }

        cooldown.add(player)
        taskRunLater(1) { cooldown.remove(player) }
    }

    private val task = task(true, 0, 10) {
        onlinePlayers.forEach { p ->
            val item = p.inventory.itemInMainHand

            when (item.type) {
                Material.RECOVERY_COMPASS -> {
                    if (item.itemMeta?.customModel != 100) return@forEach
                    val radius = item.itemMeta?.persistentDataContainer?.get(key, PersistentDataType.INTEGER)
                    val nearbyFull = p.location.getNearbyEntitiesByType(Marker::class.java, radius?.toDouble() ?: 3.0).toMutableSet()
                    val nearbyHalf = p.location.getNearbyEntitiesByType(Marker::class.java, radius?.toDouble()?.div(2) ?: 1.5).toSet()
                    nearbyFull.removeAll(nearbyHalf)
                    nearbyFull.forEach { e -> p.spawnParticle(Particle.VILLAGER_HAPPY, e.location, 2, 0.4, 0.4, 0.4, 0.1) }
                    nearbyHalf.forEach { e -> p.spawnParticle(Particle.VILLAGER_HAPPY, e.location, 2, 0.2, 0.2, 0.2, 0.1) }
                }

                Material.PINK_STAINED_GLASS_PANE -> {
                    val source = p.location
                    val display = Bukkit.createBlockData(Material.GLASS_PANE)
                    (-5..5).forEach { x ->
                        (-5..5).forEach { y ->
                            (-5..5).forEach { z ->
                                val b = source.world.getBlockAt(source.clone().add(x, y, z))
                                if (b.type == Material.PINK_STAINED_GLASS_PANE)
                                    p.spawnParticle(Particle.BLOCK_MARKER, b.location.add(.5,.5,.5), 1, display)
                            }
                        }
                    }
                }

                else -> Unit
            }
        }
    }

    private fun raycast(from: Location, yaw: Float, pitch: Float, distance: Int, search: (Location) -> Unit) {
        val direction = yawPitchToDirection(-yaw, pitch).normalize().mul(-0.1)
        val vector = Vector(direction.x, direction.y, direction.z)
        repeat(distance * 10) {
            from.add(vector)
            search.invoke(from)
        }
    }

    private fun yawPitchToDirection(yaw: Float, pitch: Float): Vector3d {
        val yyy = Math.toRadians(yaw.toDouble())
        val ppp = Math.toRadians(pitch.toDouble())
        val xzLen = cos(ppp)
        val x = xzLen * cos(yyy)
        val y = sin(ppp)
        val z = xzLen * sin(-yyy)
        return Vector3d(x, y, z)
    }
}