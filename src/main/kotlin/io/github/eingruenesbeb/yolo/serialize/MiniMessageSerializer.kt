/*
 * This is program is a plugin for Minecraft Servers called "Yolo".
 * Copyright (c) 2023  eingruenesbeb
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *   You can reach the original author via e-Mail: agreenbeb@gmail.com
 */

package io.github.eingruenesbeb.yolo.serialize

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

// Leave public, as other plugins may want to (de)serialize this ones data.

object MiniMessageSerializer : KSerializer<Component> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("MiniMessage") {
            element<String>("message")
        }

    override fun deserialize(decoder: Decoder): Component {
        val miniMessage = MiniMessage.miniMessage()
        var toReturn: Component? = null
        decoder.decodeStructure(descriptor) {
            var deserialized: Component? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> deserialized =
                        miniMessage.deserialize(this.decodeStringElement(descriptor, 0)).replaceText {
                            it.matchLiteral("\r")
                            it.replacement("")
                        }  // Convert from potential crlf to lf format, to avoid unrecognized character.

                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index $index")
                }
            }

            toReturn = deserialized
        }
        requireNotNull(toReturn)
        return toReturn as Component
    }

    override fun serialize(encoder: Encoder, value: Component) {
        encoder.encodeStructure(descriptor) {
            this.encodeStringElement(descriptor, 0, MiniMessage.miniMessage().serialize(value))
        }
    }
}
