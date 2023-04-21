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

package io.github.eingruenesbeb.yolo.events

import io.github.eingruenesbeb.yolo.Yolo
import io.github.eingruenesbeb.yolo.managers.PlayerManager.PlayerStatus
import io.github.eingruenesbeb.yolo.managers.PlayerManager.ReviveResult
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * This event is emitted, when a player is about to be revived. It is a synchronous variant of
 * [PreYoloPlayerReviveEventAsync], that can change the outcome of the process by using [changeOutcome].
 *
 *
 * It implements [YoloPlayerEvent], meaning, that it provides relevant information about the player and access to the
 * corresponding [org.bukkit.OfflinePlayer] and [org.bukkit.entity.Player].
 *
 * @property yoloPlayerInformation The information regarding this player including their current status
 * @property originalTargetOutcome The originally intended outcome of the revive attempt
 * @property targetOutcome The currently targeted outcome of the revive attempt
 *
 * @see PreYoloPlayerReviveEventAsync
 * @see YoloPlayerEvent
 * @see ReviveResult
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class PreYoloPlayerReviveEvent internal constructor(override val yoloPlayerInformation: Pair<UUID, PlayerStatus>, val originalTargetOutcome: ReviveResult): Event(), YoloPlayerEvent {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlerList
    }

    /**
     * This is the current targeted outcome.\
     * It have been altered by other plugins.
     *
     * @see ReviveResult
     */
    var targetOutcome = originalTargetOutcome
        private set

    override fun getHandlers(): HandlerList = handlerList

    /**
     * Changes the outcome of the process depending on the new parameters given.
     *
     *
     * **Note, that this has no direct effect on the player's (Yolo internal) status and only on the revive attempt,
     * that emitted the event.** Should the attempt succeed however, the isDead and isToRevive flags are updated to
     * false. You can check the outcome via [YoloPlayerRevivedEvent] (or it's asynchronous variant) and react
     * accordingly.
     *
     * @param isToRevive Whether to revive the player at all
     * @param isTeleportToDeathPos Whether to teleport the player to the location of their last death
     * @param isRestoreInventory Whether to restore the inventory.
     * @param by What plugin this change is coming from. (← Your plugin's main class here)
     * @param isSilent Whether to mute the console message, stating the changes and source of the changes. `false` by
     * default
     * @param reason Optionally a reason, why the change was made. `null` by default
     */
    fun changeOutcome(isToRevive: Boolean, isTeleportToDeathPos: Boolean, isRestoreInventory: Boolean, by: Plugin, isSilent: Boolean = false, reason: String? = null) {
        targetOutcome = ReviveResult(isToRevive, isTeleportToDeathPos, isRestoreInventory)
        if (isSilent) return
        JavaPlugin.getPlugin(Yolo::class.java).getLogger().also { logger ->
            logger.info(
                Yolo.pluginResourceBundle.getString("player.revive.outcome.changed")
                    .replace("%original_outcome%", originalTargetOutcome.toString())
                    .replace("%new_outcome%", targetOutcome.toString())
                    .replace("%plugin%", by.name)
            )
            reason?.let {
                Yolo.pluginResourceBundle.getString("player.revive.outcome.reason")
                    .replace("%reason%", reason)
            }
        }
    }
}