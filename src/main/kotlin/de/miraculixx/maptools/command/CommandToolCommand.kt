package de.miraculixx.maptools.command

import de.miraculixx.kpaper.extensions.bukkit.getHandItem
import de.miraculixx.kpaper.localization.msg
import de.miraculixx.maptools.events.ToolEvent
import de.miraculixx.maptools.interfaces.Module
import de.miraculixx.maptools.utils.extensions.command
import de.miraculixx.maptools.utils.extensions.unregister
import de.miraculixx.maptools.utils.gui.logic.InventoryUtils.get
import de.miraculixx.maptools.utils.plus
import de.miraculixx.maptools.utils.prefix
import dev.jorel.commandapi.kotlindsl.commandArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType

class CommandToolCommand : Module {
    private val command = command("commandtool") {
        withPermission("maptools.commandtool")

        commandArgument("command") {
            playerExecutor { player, args ->
                val command = args.getRaw(0) ?: return@playerExecutor
                val item = player.getHandItem(EquipmentSlot.HAND)
                if (item == null) {
                    player.sendMessage(prefix + msg("command.commandTool.failApply"))
                    return@playerExecutor
                }

                item.editMeta {
                    it.persistentDataContainer.set(ToolEvent.keyCommand, PersistentDataType.STRING, command)
                }
                player.sendMessage(prefix + msg("command.commandTool.added", input = listOf(command)))
            }
        }

        literalArgument("info") {
            playerExecutor { player, _ ->
                val item = player.getHandItem(EquipmentSlot.HAND)
                val command = item?.itemMeta?.persistentDataContainer?.get(ToolEvent.keyCommand)
                if (command == null) {
                    player.sendMessage(prefix + msg("command.commandTool.failGet"))
                    return@playerExecutor
                }

                player.sendMessage(prefix + msg("command.commandTool.get", listOf(command)))
            }
        }

        literalArgument("clear") {
            playerExecutor { player, _ ->
                val item = player.getHandItem(EquipmentSlot.HAND)
                if (item == null) {
                    player.sendMessage(prefix + msg("command.commandTool.failRemove"))
                    return@playerExecutor
                }

                item.editMeta {
                    it.persistentDataContainer.remove(ToolEvent.keyCommand)
                }
                player.sendMessage(prefix + msg("command.commandTool.remove"))
            }
        }
    }

    override fun disable() {
        command.unregister()
    }

    override fun enable() {
        command.register()
    }
}