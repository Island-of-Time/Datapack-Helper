package de.miraculixx.maptools.command

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.register
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.maptools.interfaces.Module
import de.miraculixx.maptools.utils.extensions.command
import de.miraculixx.maptools.utils.extensions.unregister
import de.miraculixx.maptools.utils.plus
import de.miraculixx.maptools.utils.prefix
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument
import org.bukkit.event.block.BlockPhysicsEvent

class BlockUpdateCommand : Module {
    private var blockUpdates = true

    private val command = command("block-update") {
        withPermission("maptools.block-update")

        literalArgument("toggle") {
            anyExecutor { sender, _ ->
                if (blockUpdates) {
                    listener.register()
                    sender.sendMessage(prefix + msg("command.blockUpdates.disable"))
                } else {
                    listener.unregister()
                    sender.sendMessage(prefix + msg("command.blockUpdates.enable"))
                }
                blockUpdates = !blockUpdates
            }
        }
    }

    private val listener = listen<BlockPhysicsEvent>(register = false) {
        it.isCancelled = true
    }

    override fun disable() {
        command.unregister()
    }

    override fun enable() {
        command.register()
    }
}