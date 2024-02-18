package de.miraculixx.maptools.events

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.event.register
import de.miraculixx.kpaper.event.unregister
import de.miraculixx.kpaper.extensions.bukkit.toLegacyString
import de.miraculixx.kpaper.items.name
import de.miraculixx.maptools.interfaces.Module
import de.miraculixx.maptools.utils.mm
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType

class TextInputEvent : Module {
    private val onSign = listen<SignChangeEvent>(register = false) {
        it.lines().forEachIndexed { line, component ->
            val raw = component.toLegacyString()
            val escaped = mm.escapeTags(raw)
            if (raw != escaped) it.line(line, mm.deserialize(raw))
        }
    }

    private val onRename = listen<InventoryClickEvent>(register = false) {
        if (it.inventory.type != InventoryType.ANVIL) return@listen
        if (it.slot == 2) {
            val item = it.currentItem ?: return@listen
            item.editMeta { meta ->
                meta.name = mm.deserialize(item.itemMeta.displayName)
            }
        }
    }

    override fun disable() {
        onSign.unregister()
        onRename.unregister()
    }

    override fun enable() {
        onSign.register()
        onRename.register()
    }
}