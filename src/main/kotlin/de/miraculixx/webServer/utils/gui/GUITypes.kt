package de.miraculixx.webServer.utils.gui

import de.miraculixx.webServer.utils.cHighlight
import de.miraculixx.webServer.utils.cmp
import de.miraculixx.webServer.utils.plus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

enum class GUITypes(val title: Component) {
    LOCALIZATION(cmp("• ", NamedTextColor.DARK_GRAY) + cmp("Localization", cHighlight))
}