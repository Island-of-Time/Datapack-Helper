package de.miraculixx.maptools.utils.extensions

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.core.Vec3i

object Vec3iSerializer: KSerializer<Vec3i> {
    override val descriptor = PrimitiveSerialDescriptor("vec3i", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Vec3i {
        val split = decoder.decodeString().split(":", limit = 3)
        return Vec3i(split[0].toInt(), split[1].toInt(), split[2].toInt())
    }

    override fun serialize(encoder: Encoder, value: Vec3i) {
        encoder.encodeString("${value.x}:${value.y}:${value.z}")
    }
}