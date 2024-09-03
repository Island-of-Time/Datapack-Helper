package de.miraculixx.maptools.utils.extensions

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.world.phys.Vec3

object Vec3dSerializer : KSerializer<Vec3> {
    override val descriptor = PrimitiveSerialDescriptor("vec3d", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Vec3 {
        val split = decoder.decodeString().split(":", limit = 3)
        return Vec3(split[0].toDouble(), split[1].toDouble(), split[2].toDouble())
    }

    override fun serialize(encoder: Encoder, value: Vec3) {
        encoder.encodeString("${value.x}:${value.y}:${value.z}")
    }
}