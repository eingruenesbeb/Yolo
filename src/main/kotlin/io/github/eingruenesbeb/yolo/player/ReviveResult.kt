@file:UseSerializers(NullableLocationKSerializer::class)

package io.github.eingruenesbeb.yolo.player

import io.github.eingruenesbeb.yolo.serialize.NullableLocationKSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.bukkit.Location

/**
 * Represents the result of a revival attempt.
 * If the attempt lies in the future or in the past, it is context-dependent.
 *
 * @property successful Whether the attempt should be or was successful.
 * @property teleported Whether the player should be or was teleported to their last death location.
 * @property inventoryRestored Whether the inventory should be or was restored.
 */
@Serializable
data class ReviveResult(
    val successful: Boolean = false,
    val teleported: Boolean = false,
    val inventoryRestored: Boolean = false,
    internal val teleportedFrom: Location? = null
) {
    override fun toString(): String {
        return "[Successful: $successful, Teleport: $teleported, Restored inventory: $inventoryRestored]"
    }
}
