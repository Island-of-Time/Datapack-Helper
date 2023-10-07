package de.miraculixx.webServer.events

import de.miraculixx.kpaper.event.listen
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import org.bukkit.Material
import org.bukkit.entity.BlockDisplay
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import java.util.*

object BlockPlaceEvent {
    val playerModes: MutableMap<UUID, Mode> = mutableMapOf()

    private val onPlace = listen<BlockPlaceEvent> {
        val mode = playerModes[it.player.uniqueId] ?: return@listen
        val block = it.blockPlaced

        if (mode.resizer) {
            if (block.type == Material.BARRIER) return@listen
            val resizeFloat = mode.resizerNumber.toFloat()
            val resizeInt = mode.resizerNumber.toInt() - 1
            val bd = block.world.spawn(block.location, BlockDisplay::class.java)
            mode.tag?.let { tag -> bd.addScoreboardTag(tag) }
            bd.block = block.blockData
            bd.transformation = Transformation(Vector3f(0f, 0f, 0f), AxisAngle4f(0f, 0f, 0f, 0f), Vector3f(resizeFloat, resizeFloat, resizeFloat), AxisAngle4f(0f, 0f, 0f, 0f))

            val loc = block.location
            val world = loc.world
            (loc.blockX..loc.blockX + resizeInt).forEach { x ->
                (loc.blockY..loc.blockY + resizeInt).forEach { y ->
                    (loc.blockZ..loc.blockZ + resizeInt).forEach { z ->
                        world.getBlockAt(x, y, z).type = Material.BARRIER
                    }
                }
            }
        }
    }

    data class Mode(
        var tag: String? = null,
        var resizer: Boolean = false,
        var resizerNumber: Double = .0
    )
}