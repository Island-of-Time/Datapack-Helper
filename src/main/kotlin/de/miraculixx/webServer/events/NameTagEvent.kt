package de.miraculixx.webServer.events

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.items.name
import de.miraculixx.webServer.utils.gui.logic.InventoryUtils.get
import de.miraculixx.webServer.utils.mm
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.block.Sign
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent

class NameTagEvent {
    private val realName = NamespacedKey("de.miraculixx.api", "realname")

    val onSign = listen<PlayerInteractEvent> {
        val item = it.item ?: return@listen
        val player = it.player
        if (item.type != Material.NAME_TAG || it.action != Action.RIGHT_CLICK_BLOCK) return@listen
        val block = it.clickedBlock ?: return@listen
        println(0)
        if (!Tag.ALL_SIGNS.isTagged(block.type)) return@listen
        val sign = block.state as Sign
        println(1)
        val name = item.itemMeta?.persistentDataContainer?.get(realName) ?: return@listen
        var row = 0
        name.split("\\n", ignoreCase = true).forEach { s ->
            println(s)
            if (row >= 4) return@listen
            sign.line(row, mm.deserialize(s))
            row++
        }
    }

    val onRename = listen<InventoryClickEvent> {
        if (it.inventory.type != InventoryType.ANVIL) return@listen
        println(it.slot)
        if (it.slot == 2) {
            val item = it.currentItem ?: return@listen
            item.editMeta { meta ->
                meta.name = mm.deserialize(item.itemMeta.displayName)
            }
        }
    }
}