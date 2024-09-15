/*
 * This program is a plugin for Minecraft Servers called "Yolo".
 * Copyright (C) 2023-2023  eingruenesbeb
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * You can reach the original author via e-Mail: agreenbeb@gmail.com
 */
package io.github.eingruenesbeb.yolo.serialize

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import java.util.*


object UUIDKSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("MiniMessage") {
            element<Long>("leastSignificant")
            element<Long>("mostSignificant")
        }

    override fun deserialize(decoder: Decoder): UUID {
        return decoder.decodeStructure(descriptor) {
            var leastSignificantBits: Long? = null
            var mostSignificantBits: Long? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> leastSignificantBits = decodeLongElement(descriptor, index)
                    1 -> mostSignificantBits = decodeLongElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index $index")
                }
            }

            requireNotNull(mostSignificantBits)
            requireNotNull(leastSignificantBits)

            UUID(mostSignificantBits, leastSignificantBits)
        }
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeStructure(descriptor) {
            this.encodeLongElement(descriptor, 0, value.leastSignificantBits)
            this.encodeLongElement(descriptor, 1, value.mostSignificantBits)
        }
    }
}
