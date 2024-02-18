@file:Suppress("unused")

package de.miraculixx.maptools.command

import de.miraculixx.kpaper.extensions.broadcast
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.maptools.utils.plus
import de.miraculixx.maptools.utils.prefix
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