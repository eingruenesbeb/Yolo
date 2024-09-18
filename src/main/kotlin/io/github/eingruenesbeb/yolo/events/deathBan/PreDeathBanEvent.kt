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

package io.github.eingruenesbeb.yolo.events.deathBan

import io.github.eingruenesbeb.yolo.Yolo
import io.github.eingruenesbeb.yolo.events.YoloPlayerEvent
import io.github.eingruenesbeb.yolo.events.revive.PreYoloPlayerReviveEventAsync
import io.github.eingruenesbeb.yolo.player.DeathBanResult
import io.github.eingruenesbeb.yolo.player.YoloPlayerData
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.plugin.Plugin

/**
 * This event is emitted when a player is about to be death-banned.
 * It is a synchronous variant of [PreDeathBanEventAsync], that can change the outcome of the process by using
 * [changeOutcome].
 *
 *
 * It implements [YoloPlayerEvent], meaning, that it provides relevant information about the player and access to the
 * corresponding [org.bukkit.OfflinePlayer] and [org.bukkit.entity.Player].
 *
 * @property yoloPlayerInformation The information regarding this player including their current status
 * @property originalTargetResult The originally intended outcome of the event
 * @property targetResult The currently targeted outcome of the event.
 *
 * @see PreYoloPlayerReviveEventAsync
 * @see YoloPlayerEvent
 * @see DeathBanResult
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class PreDeathBanEvent internal constructor(
    override val yoloPlayerInformation: YoloPlayerData,
    val originalTargetResult: DeathBanResult,
    val associatedPlayerDeathEvent: PlayerDeathEvent
): YoloPlayerEvent, Event() {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlerList
    }

    var targetResult = originalTargetResult
        private set

    override fun getHandlers(): HandlerList = handlerList

    /**
     * Changes the outcome of the process depending on the new parameters given.
     *
     * @param newTargetResult The new desired outcome. Note that a player will not be considered as dead by the plugin
     * if the result of the ban is a failure.
     * @param by what plugin this change is coming from. (← Your plugin's main class here)
     * @param isSilent Whether to mute the console message, stating the changes and source of the changes. `false` by
     * default
     * @param reason Optionally a reason, why the change was made. `null` by default
     *
     * @see DeathBanResult
     */
    fun changeOutcome(newTargetResult: DeathBanResult, by: Plugin, isSilent: Boolean = false, reason: String? = null) {
        targetResult = newTargetResult
        if (isSilent) return
        Yolo.pluginInstance!!.logger.run {
            this.info {
                Yolo.pluginResourceBundle.getString("player.revive.outcome.changed")
                    .replace("%original_outcome%", originalTargetResult.toString())
                    .replace("%new_outcome%", targetResult.toString())
                    .replace("%plugin%", by.name)
            }
            reason?.let {
                this.info {
                    Yolo.pluginResourceBundle.getString("player.revive.outcome.reason")
                        .replace("%reason%", it)
                }
            }
        }
    }
}
