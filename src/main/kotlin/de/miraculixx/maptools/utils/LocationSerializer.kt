package de.miraculixx.maptools.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.bukkit.Location

val json = Json {
    prettyPrint = true
}

object LocationSerializer : KSerializer<Location> {
    override val descriptor = PrimitiveSerialDescriptor("Location", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Location {
        val dummy = Json.decodeFromString<DummyLocation>(decoder.decodeString())
        return Location(Bukkit.getWorld(dummy.world)!!, dummy.x, dummy.y, dummy.z, dummy.yaw, dummy.pitch)
    }

    override fun serialize(encoder: Encoder, value: Location) {
        encoder.encodeString(Json.encodeToString(DummyLocation(value.world.name, value.x, value.y, value.z, value.yaw, value.pitch)))
    }

    @Serializable
    data class DummyLocation(val world: String, val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float)
}