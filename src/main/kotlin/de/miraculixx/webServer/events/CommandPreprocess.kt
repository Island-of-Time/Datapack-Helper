package de.miraculixx.webServer.events

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.extensions.bukkit.addCommand
import de.miraculixx.webServer.utils.*
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import java.util.UUID

class CommandPreprocess {
    private val lastCommand: MutableMap<UUID, String> = mutableMapOf()

    val event = listen<PlayerCommandPreprocessEvent> {
        val message = it.message
        val player = it.player
        if (message.endsWith("kill @e") || message.endsWith("kill @a")) {
            it.isCancelled = true
            player.sendMessage(prefix + cmp("Stay on the save side my jung boy"))
        }

        val firstSelector = message.substringAfter('@').substringBefore(']')
        if (firstSelector != message) {
            val fullSelector = "@$firstSelector]"
            try {
                val targets = Bukkit.selectEntities(player, fullSelector)
                if (targets.size >= 10 && message != lastCommand[player.uniqueId]) {
                    it.isCancelled = true
                    player.sendMessage(
                        prefix + cmp("Warning! You selected ${targets.size} entities at once with ", cError) +
                                cmp("${fullSelector[0]}${fullSelector[1]}", cError, underlined = true).addHover(cmp(fullSelector) + cmp("!"))
                    )
                    player.sendMessage(prefix + cmp("Click ") + cmp("here", cMark).addHover(cmp(message)).addCommand(message) + cmp(" to execute it anyway"))
                }
            } catch (_: IllegalArgumentException) {}
        }

        lastCommand[player.uniqueId] = message
    }
}