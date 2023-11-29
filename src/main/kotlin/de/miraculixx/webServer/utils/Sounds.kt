package de.miraculixx.webServer.utils

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.entity.Player


fun Audience.click() {
    playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.7f, 1f))
}

fun Audience.soundError() {
    playSound(Sound.sound(Key.key("entity.enderman.teleport"), Sound.Source.MASTER, 1f, 1.1f))
}

fun Player.soundEnable() {
    playSound(this, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f)
}

fun Audience.soundDisable() {
    playSound(Sound.sound(Key.key("block.note_block.bass"), Sound.Source.MASTER, 1f, 0.4f))
}

fun Audience.soundDelete() {
    playSound(Sound.sound(Key.key("block.respawn_anchor.deplete"), Sound.Source.MASTER, 1f, 1.2f))
}

fun Audience.soundStone() {
    playSound(Sound.sound(Key.key("block.stone.hit"), Sound.Source.MASTER, 1f, 1f))
}

fun Audience.soundUp() {
    playSound(Sound.sound(Key.key("block.note_block.chime"), Sound.Source.MASTER, 1f, 1.2f))
}

fun Audience.soundDown() {
    playSound(Sound.sound(Key.key("block.note_block.chime"), Sound.Source.MASTER, 1f, 0.5f))
}

fun Player.soundPling() {
    playSound(this, org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.5f)
}