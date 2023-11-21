package de.miraculixx.webServer.command

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.register
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import org.bukkit.event.block.BlockPhysicsEvent

class BlockUpdateCommand {
    private var blockUpdates = true

    private val command = commandTree("block-update") {
        literalArgument("toggle") {
            anyExecutor { sender, _ ->
                if (blockUpdates) {
                    listener.register()
                    sender.sendMessage(prefix + msg("command.blockUpdates.off"))
                } else {
                    listener.unregister()
                    sender.sendMessage(prefix + msg("command.blockUpdates.on"))
                }
                blockUpdates = !blockUpdates
            }
        }
    }

    private val listener = listen<BlockPhysicsEvent>(register = false) {
        it.isCancelled = true
    }
}