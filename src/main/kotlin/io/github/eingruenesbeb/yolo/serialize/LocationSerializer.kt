/*
 * This is program is a plugin for Minecraft Servers called "Yolo".
 * Copyright (C) 2023  eingruenesbeb
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
import org.bukkit.Bukkit
import org.bukkit.Location

object LocationSerializer : KSerializer<Location?> {
    // The location will look like this, when serialized to json:
    // {world:}

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("Location") {
            element<String>("world")
            element<Double>("x")
            element<Double>("y")
            element<Double>("z")
            element<Float>("pitch")
            element<Float>("yaw")
        }

    override fun deserialize(decoder: Decoder): Location? {
        var toReturn: Location? = null

        decoder.decodeStructure(descriptor) {
            var worldName: String? = null
            var x: Double? = null
            var y: Double? = null
            var z: Double? = null
            var pitch: Float? = null
            var yaw: Float? = null

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> worldName = this.decodeStringElement(descriptor, 0)
                    1 -> x = this.decodeDoubleElement(descriptor, 1)
                    2 -> y = this.decodeDoubleElement(descriptor, 2)
                    3 -> z = this.decodeDoubleElement(descriptor, 3)
                    4 -> pitch = this.decodeFloatElement(descriptor, 4)
                    5 -> yaw = this.decodeFloatElement(descriptor, 5)

                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index $index")
                }
            }

            toReturn = try {
                Location(Bukkit.getWorld(worldName!!), x!!, y!!, z!!, yaw!!, pitch!!)
            } catch (_: NullPointerException) {
                null
            }
        }

        return toReturn
    }

    override fun serialize(encoder: Encoder, value: Location?) {
        value ?:return
        val map = value.serialize().toMap()
        encoder.encodeStructure(descriptor) {
            this.encodeStringElement(descriptor, 0, map["world"] as String)
            this.encodeDoubleElement(descriptor, 1, map["x"] as Double)
            this.encodeDoubleElement(descriptor, 2, map["y"] as Double)
            this.encodeDoubleElement(descriptor, 3, map["z"] as Double)
            this.encodeFloatElement(descriptor, 4, map["pitch"] as Float)
            this.encodeFloatElement(descriptor, 5, map["yaw"] as Float)
        }
    }
}