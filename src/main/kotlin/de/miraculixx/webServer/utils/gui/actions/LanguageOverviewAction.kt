package de.miraculixx.webServer.utils.gui.actions

import de.miraculixx.kpaper.extensions.bukkit.dispatchCommand
import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.items.customModel
import de.miraculixx.webServer.utils.*
import de.miraculixx.webServer.utils.gui.items.LanguageOverview
import de.miraculixx.webServer.utils.gui.logic.GUIEvent
import de.miraculixx.webServer.utils.gui.logic.InventoryUtils.get
import de.miraculixx.webServer.utils.gui.logic.data.CustomInventory
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent

class LanguageOverviewAction: GUIEvent {
    override val run: (InventoryClickEvent, CustomInventory) -> Unit = event@{ it: InventoryClickEvent, inv: CustomInventory ->
        it.isCancelled = true
        val item = it.currentItem ?: return@event
        val player = it.whoClicked as? Player ?: return@event
        val meta = item.itemMeta ?: return@event
        val provider = inv.itemProvider as LanguageOverview

        when (meta.customModel) {
            1 -> {
                player.click()
                provider.lang = "german"
                inv.update()
            }
            2 -> {
                player.click()
                provider.lang = "english"
                inv.update()
            }

            100 -> {
                when (it.click) {
                    ClickType.LEFT -> {
                        val prefix = meta.persistentDataContainer.get(provider.prefixKey)
                        val finalPrefix = if (prefix.isNullOrBlank()) "{\"text\":\"\"}" else prefix
                        val message = meta.persistentDataContainer.get(provider.messageKey) ?: "<red>error"
                        player.sendMessage(gson.deserialize(finalPrefix) + mm.deserialize(message))
                        player.click()
                    }

                    ClickType.RIGHT -> {
                        val functionName = meta.persistentDataContainer.get(provider.functionKey) ?: "none"
                        console.dispatchCommand("function $functionName")
                    }

                    else -> player.soundStone()
                }
            }
        }
    }
}