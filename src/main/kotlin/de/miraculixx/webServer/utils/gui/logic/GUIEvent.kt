package de.miraculixx.webServer.utils.gui.logic

import de.miraculixx.webServer.utils.gui.logic.data.CustomInventory
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent

interface GUIEvent {
    val run: (InventoryClickEvent, CustomInventory) -> Unit
    val close: ((InventoryCloseEvent, CustomInventory) -> Unit)?
        get() = null
}