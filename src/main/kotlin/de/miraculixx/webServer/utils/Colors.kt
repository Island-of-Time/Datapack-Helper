package de.miraculixx.webServer.utils

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

val cHighlight: NamedTextColor = NamedTextColor.BLUE
val cBase: NamedTextColor = NamedTextColor.GRAY
val cError: NamedTextColor = NamedTextColor.RED
val cSuccess: NamedTextColor = NamedTextColor.GREEN
val cMark = TextColor.fromHexString("#6e94ff")!!
val cHide = TextColor.fromHexString("#1f2124")!!

val prefix = cmp("WebServer", cHighlight) + cmp(" >>", NamedTextColor.DARK_GRAY) + cmp(" ")
lateinit var consoleSender: Audience