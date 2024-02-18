package de.miraculixx.maptools.utils.gui

import de.miraculixx.maptools.utils.cHighlight
import de.miraculixx.maptools.utils.cmp
import de.miraculixx.maptools.utils.plus
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

enum class GUITypes(val title: Component) {
    LOCALIZATION(cmp("• ", NamedTextColor.DARK_GRAY) + cmp("Localization", cHighlight))
}