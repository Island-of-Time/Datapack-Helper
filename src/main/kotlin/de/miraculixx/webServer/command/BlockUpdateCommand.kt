package de.miraculixx.webServer.command

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.register
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.webServer.utils.cmp
import de.miraculixx.webServer.utils.plus
import de.miraculixx.webServer.utils.prefix
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import org.bukkit.event.block.BlockPhysicsEvent

class BlockUpdateCommand {
    private var blockUpdates = true
        set(value) {
            if (blockUpdates == value) return
            if (blockUpdates) listener.unregister()
            else listener.register()
            field = value
        }

    private val command = commandTree("block-update") {
        literalArgument("on") {
            anyExecutor { sender, _ ->
                blockUpdates = true
                sender.sendMessage(prefix + cmp("Block updates are active now!"))
            }
        }
        literalArgument("off") {
            anyExecutor { sender, _ ->
                blockUpdates = false
                sender.sendMessage(prefix + cmp("Block updates are paused now! Be aware, crashes may appear"))
            }
        }
    }


    private val listener = listen<BlockPhysicsEvent>(register = false) {
        it.isCancelled = true
    }
}