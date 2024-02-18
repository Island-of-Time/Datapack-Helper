package de.miraculixx.maptools.events

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.register
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.extensions.bukkit.addCommand
import de.miraculixx.maptools.interfaces.Module
import de.miraculixx.maptools.utils.*
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import java.util.*

class DangerWarningEvent: Module {
    private val lastCommand: MutableMap<UUID, String> = mutableMapOf()

    private val event = listen<PlayerCommandPreprocessEvent>(register = false) {
        val message = it.message
        val player = it.player
        if (message.endsWith("kill @e") || message.endsWith("kill @a")) {
            it.isCancelled = true
            player.sendMessage(prefix + cmp("Killing all entities is probably not what you want", cError))
            player.sendMessage(prefix + cmp("Run this command as a server command if you really want to execute it", cError))
        }

        val split = message.substringAfter(" @", "").takeIf { s -> s.isNotBlank() } ?: return@listen
        val fullSelector = if (split.getOrNull(1) == '[') "@${split.substringBefore(']')}]" else "@${split.getOrNull(0)}"
        try {
            val targets = Bukkit.selectEntities(player, fullSelector)
            if (targets.size >= 10 && message != lastCommand[player.uniqueId]) {
                it.isCancelled = true
                player.sendMessage(
                    prefix + cmp("Warning! You selected ${targets.size} entities at once with ", cError) +
                            cmp("${fullSelector[0]}${fullSelector[1]}", cError, underlined = true).addHover(cmp(fullSelector)) + cmp("!")
                )
                player.sendMessage(prefix + cmp("Click ") + cmp("here", cMark).addHover(cmp(message)).addCommand(message) + cmp(" to execute it anyway"))
            }
        } catch (_: IllegalArgumentException) {}

        lastCommand[player.uniqueId] = message
    }

    override fun disable() {
        event.unregister()
    }

    override fun enable() {
        event.register()
    }
}