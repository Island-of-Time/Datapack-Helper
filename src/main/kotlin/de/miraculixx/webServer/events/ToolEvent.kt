@file:Suppress("unused")

package de.miraculixx.webServer.events

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.extensions.geometry.add
import de.miraculixx.kpaper.extensions.kotlin.enumOf
import de.miraculixx.kpaper.extensions.onlinePlayers
import de.miraculixx.kpaper.extensions.worlds
import de.miraculixx.kpaper.items.customModel
import de.miraculixx.kpaper.runnables.task
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.webServer.Main
import de.miraculixx.webServer.command.MultiToolCommand
import de.miraculixx.webServer.command.multiToolData
import de.miraculixx.webServer.command.multiToolSelection
import de.miraculixx.webServer.settings
import de.miraculixx.webServer.utils.*
import de.miraculixx.webServer.utils.gui.logic.InventoryUtils.get
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Fence
import org.bukkit.block.data.type.Slab
import org.bukkit.block.data.type.Stairs
import org.bukkit.block.data.type.Wall
import org.bukkit.entity.*
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import org.joml.Vector3d
import kotlin.collections.set
import kotlin.math.cos
import kotlin.math.sin

object ToolEvent {
    val key = NamespacedKey(Main.INSTANCE, "webserver.command-item1")
    val key2 = NamespacedKey(Main.INSTANCE, "webserver.command-item2")
    val key3 = NamespacedKey(Main.INSTANCE, "webserver.command-item3")

    private val cooldown: MutableSet<Player> = mutableSetOf()
    private val onClick = listen<PlayerInteractEvent> {
        if (it.hand == EquipmentSlot.OFF_HAND) return@listen
        if (it.isAsynchronous) return@listen
        val item = it.item ?: return@listen
        val meta = item.itemMeta ?: return@listen
        if (meta.customModel != 100) return@listen
        val player = it.player

        when (item.type) {
            // Convertor Tool
            Material.SHEARS -> {
                if (!player.hasPermission("buildertools.blockify-tool")) return@listen
                val block = it.clickedBlock ?: return@listen
                if (cooldown.contains(player)) return@listen
                val dataContainer = item.itemMeta?.persistentDataContainer ?: return@listen
                val tag = dataContainer.get(key2, PersistentDataType.STRING) ?: return@listen
                val scale = dataContainer.get(key, PersistentDataType.FLOAT) ?: return@listen
                val originText = dataContainer.get(key3, PersistentDataType.STRING)?.split(':')
                val originExtension = originText?.let { c -> " ${c[0]} ${c[1]} ${c[2]}" } ?: ""
                it.isCancelled = true
                player.performCommand("blockify ${block.x} ${block.y} ${block.z} $tag $scale$originExtension")
            }

            // Marker Tool
            Material.SPECTRAL_ARROW -> {
                if (!player.hasPermission("buildertools.marker-tool")) return@listen
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
                            player.sendMessage(prefix + cmp("Removed a marker at targeted position"))
                        }
                    }
                }
            }

            // Multitool
            Material.FEATHER -> {
                if (!player.hasPermission("buildertools.multitool")) return@listen
                it.isCancelled = true
                val multiData = multiToolSelection.getOrPut(player.uniqueId) { mutableMapOf() }
                val multiSettings = multiToolData.getOrPut(player.uniqueId) { MultiToolCommand.MultiToolData() }
                if (it.action.isRightClick) {
                    when (multiSettings.mode) {
                        MultiToolCommand.MultiToolMode.MOVE -> {
                            val loc = player.location
                            val pitch = loc.pitch
                            val yaw = ((loc.yaw + 180) % 360) - 180
                            val facing = when {
                                pitch <= -65 -> BlockFace.UP
                                pitch >= 65 -> BlockFace.DOWN
                                yaw in -45f..45f -> BlockFace.SOUTH
                                yaw in 45.1f..135f -> BlockFace.WEST
                                yaw in 135.1f..180f || yaw in -180f..-135.1f -> BlockFace.NORTH
                                yaw in -135f..-45.1f -> BlockFace.EAST
                                else -> BlockFace.SELF
                            }
                            val moveVector = meta.persistentDataContainer.get(key3, PersistentDataType.FLOAT) ?: 0.1f
                            val vector = facing.direction.multiply(if (player.isSneaking) moveVector * 2f else moveVector)
                            multiData.forEach { (e, _) -> e.teleportAsync(e.location.add(vector)) }
                        }

                        MultiToolCommand.MultiToolMode.ROTATE -> {
                            val moveVector = (meta.persistentDataContainer.get(key3, PersistentDataType.FLOAT)?.times(10f)) ?: 1f
                            multiData.forEach { (e, _) ->
                                e.teleportAsync(e.location.apply {
                                    if (player.isSneaking) pitch = rotateLocking(pitch, moveVector)
                                    else yaw = rotateLocking(yaw, moveVector)
                                })
                            }
                        }
                    }

                } else {
                    featherHitter(player, multiData, meta, multiSettings)
                }
            }

            // Interaction Tool
            Material.SHULKER_SHELL -> {
                if (!player.hasPermission("buildertools.interaction-tool")) return@listen
                val block = it.clickedBlock ?: return@listen
                val tag = meta.persistentDataContainer.get(key)
                it.isCancelled = true
                if (it.action.isRightClick) {
                    val hitbox = block.world.spawn(block.location.subtract(-.5, .005, -.5), Interaction::class.java)
                    hitbox.scoreboardTags.add(tag)
                    hitbox.interactionWidth = 1.01f
                    hitbox.interactionHeight = 1.01f
                    player.soundEnable()
                }
            }

            // Tag Tool
            Material.ARROW -> {
                if (!player.hasPermission("buildertools.tag-tool")) return@listen

                val pdc = meta.persistentDataContainer
                val tags = pdc.get(key, PersistentDataType.STRING)?.split(' ') ?: return@listen
                val filter = pdc.get(key2, PersistentDataType.STRING)?.let { t -> enumOf<EntityType>(t)  }
                val range = pdc.get(key3, PersistentDataType.DOUBLE) ?: 0.5

                val start = player.location.add(0.0, 1.5, 0.0)
                raycast(start, start.yaw, start.pitch, 6) { loc ->
                    val targets = if (filter != null) loc.getNearbyEntitiesByType(filter.entityClass, range)
                        else loc.getNearbyEntities(range, range, range)
                    targets
                }

            }

            else -> Unit
        }

        cooldown.add(player)
        taskRunLater(1) { cooldown.remove(player) }
    }

    private val onSwap = listen<PlayerSwapHandItemsEvent> {
        val item = it.offHandItem ?: return@listen
        val meta = item.itemMeta ?: return@listen
        if (meta.customModel != 100) return@listen
        val player = it.player

        when (item.type) {
            Material.FEATHER -> {
                if (!player.hasPermission("buildertools.multitool")) return@listen
                it.isCancelled = true
                val multiData = multiToolSelection[player.uniqueId] ?: return@listen
                multiData.forEach { (e, data) ->
                    when (e) {
                        is LivingEntity -> e.isGlowing = false
                        is BlockDisplay -> {
                            when (e.block.material) {
                                Material.YELLOW_CONCRETE, Material.BAMBOO_MOSAIC_SLAB, Material.END_STONE_BRICK_WALL,
                                Material.BAMBOO_MOSAIC_STAIRS, Material.BAMBOO_FENCE -> e.block = data as BlockData

                                else -> Unit
                            }
                        }
                    }
                }
                multiData.clear()
            }

            else -> Unit
        }
    }

    private val onEntityClick = listen<PlayerInteractAtEntityEvent> {
        if (it.hand == EquipmentSlot.OFF_HAND) return@listen
        if (it.isAsynchronous) return@listen
        val player = it.player
        val item = player.inventory.itemInMainHand
        val meta = item.itemMeta ?: return@listen
        val entity = it.rightClicked

        when (item.type) {
            Material.SHULKER_SHELL -> {
                if (!player.hasPermission("buildertools.interaction-tool")) return@listen
                val type = entity.type
                if (type == EntityType.INTERACTION) return@listen
                val tag = meta.persistentDataContainer.get(key)
                val interaction = entity.world.spawn(entity.location.subtract(.0, .01, .0), Interaction::class.java)
                interaction.interactionWidth = entity.width.toFloat() + 0.02f
                interaction.interactionHeight = entity.height.toFloat() + 0.02f
                interaction.scoreboardTags.add(tag)
            }

            else -> Unit
        }
    }

    private val onHitEntity = listen<EntityDamageByEntityEvent> {
        val player = it.damager as? Player ?: return@listen
        val item = player.inventory.itemInMainHand
        val meta = item.itemMeta ?: return@listen

        when (item.type) {
            Material.FEATHER -> {
                if (!player.hasPermission("buildertools.multitool")) return@listen
                val multiData = multiToolSelection.getOrPut(player.uniqueId) { mutableMapOf() }
                val multiSettings = multiToolData.getOrPut(player.uniqueId) { MultiToolCommand.MultiToolData() }

                featherHitter(player, multiData, meta, multiSettings)
            }

            else -> Unit
        }
    }

    private val task = task(true, 0, 10) {
        onlinePlayers.forEach { p ->
            val item = p.inventory.itemInMainHand

            when (item.type) {
                Material.RECOVERY_COMPASS -> {
                    if (!p.hasPermission("buildertools.marker-finder")) return@forEach

                    if (item.itemMeta?.customModel != 100) return@forEach
                    val radius = item.itemMeta?.persistentDataContainer?.get(key, PersistentDataType.INTEGER)
                    val nearbyFull = p.location.getNearbyEntitiesByType(Marker::class.java, radius?.toDouble() ?: 3.0).toMutableSet()
                    val nearbyHalf = p.location.getNearbyEntitiesByType(Marker::class.java, radius?.toDouble()?.div(2) ?: 1.5).toSet()
                    nearbyFull.removeAll(nearbyHalf)
                    nearbyFull.forEach { e -> p.spawnParticle(Particle.VILLAGER_HAPPY, e.location, 2, 0.4, 0.4, 0.4, 0.1) }
                    nearbyHalf.forEach { e -> p.spawnParticle(Particle.VILLAGER_HAPPY, e.location, 2, 0.2, 0.2, 0.2, 0.1) }
                }

                Material.PINK_STAINED_GLASS_PANE -> {
                    if (!settings.highlightPinkGlass) return@forEach

                    val source = p.location
                    val display = Bukkit.createBlockData(Material.GLASS_PANE)
                    (-5..5).forEach { x ->
                        (-5..5).forEach { y ->
                            (-5..5).forEach { z ->
                                val b = source.world.getBlockAt(source.clone().add(x, y, z))
                                if (b.type == Material.PINK_STAINED_GLASS_PANE)
                                    p.spawnParticle(Particle.BLOCK_MARKER, b.location.add(.5, .5, .5), 1, display)
                            }
                        }
                    }
                }

                Material.FEATHER -> {
                    if (!p.hasPermission("buildertools.multitool")) return@forEach

                    if (item.itemMeta?.customModel != 100) return@forEach
                    val multiData = multiToolSelection[p.uniqueId] ?: return@forEach
                    multiData.forEach { (e, _) ->
                        if (e !is LivingEntity && e !is BlockDisplay) {
                            p.spawnParticle(Particle.VILLAGER_HAPPY, e.location, 2, 0.1, 0.1, 0.1, 0.1)
                        }
                    }
                }

                else -> Unit
            }
        }
    }

    private fun featherHitter(player: Player, multiData: MutableMap<Entity, Any>, meta: ItemMeta, multiSettings: MultiToolCommand.MultiToolData) {
        if (player.isSneaking) {
            //Delete Selection
            if (!multiSettings.confirm) {
                player.sendMessage(prefix + cmp("Confirm deleting ${multiData.size} entities by clicking again (5s)", cError))
                multiSettings.confirm = true
                taskRunLater(5 * 20) { multiSettings.confirm = false }
                return
            }
            player.sendMessage(prefix + cmp("Removed ${multiData.size} entities!"))
            multiData.forEach { (e, _) -> e.remove() }
            multiData.clear()
        } else {
            //Select
            val typeString = meta.persistentDataContainer.get(key) ?: return
            val radius = meta.persistentDataContainer.get(key2, PersistentDataType.FLOAT) ?: return
            val type = enumOf<EntityType>(typeString) ?: return
            val start = player.location.add(0.0, 1.5, 0.0)
            if (type == EntityType.BLOCK_DISPLAY) {
                raycast(start, start.yaw - 90, start.pitch, multiSettings.range) { loc ->
                    loc.world.spawnParticle(Particle.COMPOSTER, loc, 1, .0, .0, .0, 0.1)
                    val vec = Vector(loc.x, loc.y, loc.z)
                    loc.getNearbyEntitiesByType(BlockDisplay::class.java, 3.0).forEach { bd ->
                        if (multiData.contains(bd)) return@forEach
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
                        multiData[bd] = bd.block.clone()
                        val material = bd.block.material
                        val highlightData = when {
                            Tag.STAIRS.isTagged(material) -> {
                                val sData = bd.block as Stairs
                                (Bukkit.createBlockData(settings.highlightStairs) as Stairs).apply {
                                    facing = sData.facing
                                    shape = sData.shape
                                    half = sData.half
                                    isWaterlogged = sData.isWaterlogged
                                }
                            }

                            Tag.WALLS.isTagged(material) -> {
                                val sData = bd.block as Wall
                                (Bukkit.createBlockData(settings.highlightWalls) as Wall).apply {
                                    isUp = sData.isUp
                                    listOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH).forEach { face ->
                                        setHeight(face, sData.getHeight(face))
                                    }
                                    isWaterlogged = sData.isWaterlogged
                                }
                            }

                            Tag.FENCES.isTagged(material) -> {
                                val sData = bd.block as Fence
                                (Bukkit.createBlockData(settings.highlightFence) as Fence).apply {
                                    listOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH).forEach { face ->
                                        setFace(face, sData.hasFace(face))
                                    }
                                    isWaterlogged = sData.isWaterlogged
                                }
                            }

                            Tag.SLABS.isTagged(material) -> {
                                val sData = bd.block as Slab
                                (Bukkit.createBlockData(settings.highlightSlabs) as Slab).apply {
                                    setType(sData.type)
                                    isWaterlogged = sData.isWaterlogged
                                }
                            }

                            else -> Bukkit.createBlockData(settings.highlightGlobal)
                        }
                        bd.block = highlightData
                    }
                }
            } else {
                raycast(start, start.yaw - 90, start.pitch, multiSettings.range) { loc ->
                    loc.world.spawnParticle(Particle.COMPOSTER, loc, 1, .0, .0, .0, 0.1)
                    loc.getNearbyEntitiesByType(type.entityClass, radius.toDouble()).forEach { e ->
                        if (e is LivingEntity) e.isGlowing = true
                        multiData[e] = false
                    }
                }
            }
        }
    }

    private fun rotateLocking(source: Float, vec: Float): Float {
        val target = source + vec
        return when {
            source == 0f || source == 90f || source == -90f || source == 180f || source == -180f -> target //unlock after first snap
            target in -1f..1f -> 0f
            target in -91f..-89f -> -90f
            target in 89f..91f -> 90f
            target in 179f..180f || target in -180f..-179f -> 180f
            else -> target
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