@file:Suppress("unused")

package de.miraculixx.webServer.command

import de.miraculixx.kpaper.chat.sendMessage
import de.miraculixx.kpaper.extensions.broadcast
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import org.bukkit.Bukkit
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class ReloadDataPackCommand {
    private val command = commandTree("reload-dp") {
        withPermission("maptools.reload-dp")
        withAliases("datapack-reload", "dp-reload")
        anyExecutor { commandSender, _ ->
            broadcast(msg("command.reload.reload", listOf(commandSender.name)))
            val timeStarted = Instant.now().toEpochMilli()
            Bukkit.reloadData()
            val timeFinished = Instant.now().toEpochMilli()
            val timeUsed = (timeFinished - timeStarted).milliseconds
            commandSender.sendMessage(prefix + msg("command.reload.reloaded", listOf(timeUsed.toString())))
            commandSender.sendMessage(prefix + msg("command.reload.warning"))
        }
    }
}