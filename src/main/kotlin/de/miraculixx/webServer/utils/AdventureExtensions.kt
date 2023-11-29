package de.miraculixx.webServer.utils

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

val mm = MiniMessage.miniMessage()
val gson = GsonComponentSerializer.gson()

fun Component.decorate(bold: Boolean? = null, italic: Boolean? = null, strikethrough: Boolean? = null, underlined: Boolean? = null): Component {
    var finalComponent = this
    if (bold != null) finalComponent = finalComponent.decoration(TextDecoration.BOLD, bold)
    if (italic != null) finalComponent = finalComponent.decoration(TextDecoration.ITALIC, italic)
    if (strikethrough != null) finalComponent = finalComponent.decoration(TextDecoration.STRIKETHROUGH, strikethrough)
    if (underlined != null) finalComponent = finalComponent.decoration(TextDecoration.UNDERLINED, underlined)
    return finalComponent
}

fun Component.lore(): Component {
    return decoration(TextDecoration.ITALIC, false)
}

fun emptyComponent(): Component {
    return Component.text(" ")
}

fun cmp(text: String, color: TextColor = cBase, bold: Boolean = false, italic: Boolean = false, strikethrough: Boolean = false, underlined: Boolean = false): Component {
    return Component.text(text).color(color)
        .decorations(
            mapOf(
                TextDecoration.BOLD to TextDecoration.State.byBoolean(bold),
                TextDecoration.ITALIC to TextDecoration.State.byBoolean(italic),
                TextDecoration.STRIKETHROUGH to TextDecoration.State.byBoolean(strikethrough),
                TextDecoration.UNDERLINED to TextDecoration.State.byBoolean(underlined)
            )
        )
}

fun Component.addHover(display: Component): Component {
    return hoverEvent(asHoverEvent().value(display))
}

operator fun Component.plus(other: Component): Component {
    return append(other)
}

fun Audience.title(main: Component, sub: Component, fadeIn: Duration = Duration.ZERO, stay: Duration = 5.seconds, fadeOut: Duration = Duration.ZERO) {
    showTitle(Title.title(main, sub, Title.Times.times(fadeIn.toJavaDuration(), stay.toJavaDuration(), fadeOut.toJavaDuration())))
}


// Spigot fixes, because their API sucks
val legacySerializer = LegacyComponentSerializer.builder()
    .character('§')
    .hexColors()
    .useUnusualXRepeatedCharacterHexFormat()
    .build()

fun Component.native() = legacySerializer.serialize(this)
fun List<Component>.native() = map { legacySerializer.serialize(it) }
fun ConsoleCommandSender.sendMessage(cmp: Component) {
    sendMessage(cmp.native())
}
fun Player.sendMessage(cmp: Component) {
    sendMessage(cmp.native())
}
fun ItemMeta.lore(list: List<Component>) {
    lore = list.native()
}
var ItemMeta.name: Component
    get() = Component.text(this.displayName)
    set(cmp) = this.setDisplayName(cmp.native())
fun String.append(string: String) = this + string
inline fun <reified T : Entity?> Location.getNearbyEntitiesByType(clazz: Class<out T>?, radius: Double): Collection<T>
    = world!!.getNearbyEntities(this, radius, radius, radius).filterIsInstance<T>()
fun Location.getNearbyEntities(x: Double, y: Double, z: Double): Collection<Entity>
    = world!!.getNearbyEntities(this, x, y, z)