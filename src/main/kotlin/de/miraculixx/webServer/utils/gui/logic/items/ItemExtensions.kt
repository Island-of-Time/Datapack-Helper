package de.miraculixx.webServer.utils.gui.logic.items

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import de.miraculixx.webServer.utils.*
import org.bukkit.inventory.meta.SkullMeta
import java.lang.reflect.Field
import java.util.*

fun SkullMeta.skullTexture(base64: String, uuid: UUID = UUID.randomUUID()): SkullMeta {
    val profile = GameProfile(uuid, "")
    profile.properties.put("textures", Property("textures", base64))
    val profileField: Field?
    try {
        profileField = javaClass.getDeclaredField("profile")
        profileField.isAccessible = true
        profileField[this] = profile
    } catch (e: Exception) {
        e.printStackTrace()
        consoleSender.sendMessage(prefix + cmp("Head Builder failed to apply Base64 Code to Skull!", cError))
        consoleSender.sendMessage(prefix + cmp("Code: ", cError) + cmp(base64))
    }
    return this
}