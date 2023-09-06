@file:Suppress("unused")

package de.miraculixx.webServer.command

import de.miraculixx.kpaper.extensions.broadcast
import de.miraculixx.webServer.utils.cSuccess
import de.miraculixx.webServer.utils.cmp
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import org.bukkit.Bukkit

class ReloadDataPackCommand {
    private val command = commandTree("reload-dp") {
        withAliases("datapack-reload", "dp-reload")
        anyExecutor { commandSender, _ ->
            broadcast(cmp("${commandSender.name} >> Reload all data packs..."))
            Bukkit.reloadData()
            commandSender.sendMessage(prefix + cmp("Successfully reloaded all data packs!", cSuccess))
            commandSender.sendMessage(prefix + cmp("(errors might appeared, check console for more information)"))
        }
    }
}