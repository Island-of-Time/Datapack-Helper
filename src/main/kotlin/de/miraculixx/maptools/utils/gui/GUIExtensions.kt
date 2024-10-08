package de.miraculixx.maptools.utils.gui

import de.miraculixx.maptools.utils.gui.logic.data.InventoryManager
import de.miraculixx.maptools.utils.gui.logic.items.ItemProvider
import de.miraculixx.maptools.utils.gui.logic.GUIEvent
import org.bukkit.entity.Player

fun GUITypes.buildInventory(player: Player, id: String, itemProvider: ItemProvider?, clickAction: GUIEvent) {
    InventoryManager.get(id)?.open(player) ?: when (this) {
        GUITypes.LOCALIZATION -> InventoryManager.storageBuilder(id) {
            this.title = this@buildInventory.title
            this.player = player
            this.filterable = false
            this.scrollable = true
            this.headers = itemProvider?.getExtra() ?: emptyList()
            this.itemProvider = itemProvider
            this.clickAction = clickAction.run
        }
    }
}