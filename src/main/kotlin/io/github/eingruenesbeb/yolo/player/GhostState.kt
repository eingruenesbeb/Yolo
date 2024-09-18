@file:UseSerializers(UUIDKSerializer::class)

package io.github.eingruenesbeb.yolo.player

import io.github.eingruenesbeb.yolo.Yolo
import io.github.eingruenesbeb.yolo.managers.PlayerManager.PlayerManagerEvents
import io.github.eingruenesbeb.yolo.serialize.UUIDKSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.*

/**
 * Represents an additional state, a player can be in, when teleported upon their revival. Whilst in this state,
 * a player can't be harmed and is invisible, but can't engage in either PvP or PvE, without being forced out of
 * this state. The last part is handled by the [PlayerManagerEvents] event-listener.
 *
 *
 * It Is visible because it's a part of a player's data, which is public. However, all its members are either
 * private or internal.
 *
 * @property enabled Whether the player is in this ghost-like state
 * @property ticksLeft How long the player will be in this state if it's not removed prematurely.
 * @property attachedPlayerID The [UUID] of the corresponding player. **MUSTN'T BE `null`!**
 */
@Internal
@ConsistentCopyVisibility
@Serializable
data class GhostState(
    private val attachedPlayerID: UUID,
    internal var enabled: Boolean = false,
    private var ticksLeft: Int = 0
) {
    private val yolo
        get() = Yolo.pluginInstance!!

    private class Ticker(val state:GhostState) : BukkitRunnable() {
        var remainingTicks = state.ticksLeft
        override fun run() {
            // Cancel the task for offline players:
            if (Bukkit.getPlayer(state.attachedPlayerID) == null) {
                stop()
                return
            }

            if (!state.enabled) {
                remainingTicks = 0
                state.ticksLeft = 0
            }

            if (remainingTicks < 1) {
                state.remove()
                return
            }
            remainingTicks -= 1
            state.ticksLeft = remainingTicks
        }

        fun stop() {
            cancel()
            state.ticker = Ticker(state)  // Once cancelled, a BukkitRunnable cannot be reused.
        }
    }

    @Transient
    private var ticker = Ticker(this)

    internal fun apply() {
        val playerObject = Bukkit.getPlayer(attachedPlayerID) ?: return
        playerObject.isInvulnerable = true
        playerObject.isInvisible = true
        enabled = true
        if (ticksLeft < 1) {
            ticksLeft = 600
            ticker.remainingTicks = ticksLeft
        }
        reinstateTicker()
    }

    internal fun remove() {
        val playerObject = Bukkit.getPlayer(attachedPlayerID) ?: return yolo.logger.warning {
            Yolo.pluginResourceBundle.getString(
                "player.removeGhostState.notOnline"
            ).replace("%uuid%", attachedPlayerID.toString())
        }
        playerObject.isInvulnerable = false
        playerObject.isInvisible = false
        enabled = false
        ticker.stop()
    }

    internal fun reinstateTicker() {
        Bukkit.getPlayer(attachedPlayerID) ?: return
        runCatching{
            ticker.runTaskTimer(yolo, 0, 1)
        }.onFailure {
            if (it is IllegalStateException) ticker.stop()  // Ticker is still running.
            ticker.runTaskTimer(yolo, 0, 1)
        }
    }

    internal fun stopTicker() {
        ticker.runCatching {
            this.stop()
        }
    }
}
