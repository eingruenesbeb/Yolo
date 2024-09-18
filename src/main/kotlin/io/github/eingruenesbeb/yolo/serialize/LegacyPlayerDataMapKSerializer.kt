@file:UseSerializers(NullableLocationKSerializer::class, MiniMessageKSerializer::class)

package io.github.eingruenesbeb.yolo.serialize

import io.github.eingruenesbeb.yolo.player.GhostState
import io.github.eingruenesbeb.yolo.player.ReviveResult
import io.github.eingruenesbeb.yolo.player.YoloPlayer
import io.github.eingruenesbeb.yolo.player.YoloPlayerData
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.mapSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import net.kyori.adventure.text.Component
import org.bukkit.Location
import java.util.*

object LegacyPlayerDataMapKSerializer : KSerializer<Map<UUID, YoloPlayer>> {
    @Serializable
    private data class LegacyPlayerData(
        var latestDeathPos: Location? = null,
        var isDead: Boolean = false,
        var isToRevive: Boolean = false,
        var isTeleportToDeathPos: Boolean = true,
        var isRestoreInventory: Boolean = true,
        var banMessage: Component = Component.text(""),
        var isUndoRevive: Boolean = false,
        val ghostState: LegacyGhostState,
        val revives: MutableList<ReviveResult> = mutableListOf()
    ) {
        fun toNewFormat(uuid: UUID): YoloPlayerData = YoloPlayerData(
            uuid,
            latestDeathPos,
            isDead,
            isToRevive,
            isTeleportToDeathPos,
            isRestoreInventory,
            banMessage,
            isUndoRevive,
            GhostState(uuid, ghostState.enabled, ghostState.ticksLeft.toInt()),
            revives
        )
    }

    @Serializable private data class LegacyGhostState(var enabled: Boolean = false, val ticksLeft: Long = 600L)

    // Descriptor for serializing a map of UUID to YoloPlayerData
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("LegacyPlayerData") {
        element("data", mapSerialDescriptor(String.serializer().descriptor, LegacyPlayerData.serializer().descriptor))
    }

    // Deserialize the data from the structure { "data": { UUID as String : YoloPlayerData } }
    override fun deserialize(decoder: Decoder): Map<UUID, YoloPlayer> {
        return decoder.decodeStructure(descriptor) {
            val map = mutableMapOf<UUID, YoloPlayer>()

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> {
                        val decodedMap = decodeSerializableElement(
                            descriptor,
                            0,
                            MapSerializer(String.serializer(), LegacyPlayerData.serializer())
                        )
                        // Convert the map keys from String to UUID and construct YoloPlayer instances
                        decodedMap.forEach { (uuidString, legacyPlayerData) ->
                            val uuid = UUID.fromString(uuidString)
                            map[uuid] = YoloPlayer(legacyPlayerData.toNewFormat(uuid))
                        }
                    }
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index $index")
                }
            }

            map
        }
    }

    // Serialize the data to the structure { "data": { UUID as String : YoloPlayerData } }
    override fun serialize(encoder: Encoder, value: Map<UUID, YoloPlayer>) {
        throw NotImplementedError("Use the new format!")
    }
}
