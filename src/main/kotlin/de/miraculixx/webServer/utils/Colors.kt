package de.miraculixx.webServer.utils

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.ConsoleCommandSender

val cHighlight: NamedTextColor = NamedTextColor.BLUE
val cBase: NamedTextColor = NamedTextColor.GRAY
val cError: NamedTextColor = NamedTextColor.RED
val cSuccess: NamedTextColor = NamedTextColor.GREEN
val cMark = TextColor.fromHexString("#6e94ff")!!
val cHide = TextColor.fromHexString("#1f2124")!!

val prefix = cmp("") + cmp("MapTools", cHighlight) + cmp(" >>", NamedTextColor.DARK_GRAY) + cmp(" ")
const val tooling = false
lateinit var consoleSender: ConsoleCommandSender