package de.miraculixx.webServer.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import org.bukkit.Bukkit

class ReloadDataPackCommand {
    private val command = commandTree("reload-dp") {
        anyExecutor { commandSender, _ ->
            Bukkit.reloadData()
        }
    }
}