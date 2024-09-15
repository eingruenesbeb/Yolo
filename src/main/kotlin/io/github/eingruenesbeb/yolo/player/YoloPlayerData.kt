@file:UseSerializers(NullableLocationKSerializer::class, UUIDKSerializer::class, MiniMessageKSerializer::class)

package io.github.eingruenesbeb.yolo.player

import io.github.eingruenesbeb.yolo.serialize.MiniMessageKSerializer
import io.github.eingruenesbeb.yolo.serialize.NullableLocationKSerializer
import io.github.eingruenesbeb.yolo.serialize.UUIDKSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.kyori.adventure.text.Component
import org.bukkit.Location
import java.util.*

/**
 * This data class represents the status values, tracked by this plugin.
 *
 *
 * It can be used externally to get information about a player, that is the subject of a
 * [io.github.eingruenesbeb.yolo.events.YoloPlayerEvent].
 *
 * @property latestDeathPos The location of the last death.
 * @property isDead Whether the player is considered to be dead by the plugin.
 * @property isToRevive Whether the player should be revived by the plugin.
 * @property isTeleportToDeathPos Whether the player should be teleported to the [latestDeathPos] upon their revival.
 * @property isRestoreInventory Whether the inventory should be restored upon revival.
 * @property banMessage The ban message, that will be shown to the player upon being death-banned.
 * @property isUndoRevive Whether the player had a successful revive, that should be reverted.
 */
@Serializable
data class YoloPlayerData(
    val uuid: UUID,
    var latestDeathPos: Location? = null,
    var isDead: Boolean = false,
    var isToRevive: Boolean = false,
    var isTeleportToDeathPos: Boolean = true,
    var isRestoreInventory: Boolean = true,
    var banMessage: Component = Component.text(""),
    var isUndoRevive: Boolean = false,
    internal val ghostState: GhostState = GhostState(false, 0, uuid),
    internal val revives: MutableList<ReviveResult> = mutableListOf()
)
