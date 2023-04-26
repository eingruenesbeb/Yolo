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

import io.github.eingruenesbeb.yolo.events.YoloPlayerEvent
import io.github.eingruenesbeb.yolo.events.revive.PreYoloPlayerReviveEvent
import io.github.eingruenesbeb.yolo.managers.PlayerManager
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.PlayerDeathEvent
import java.util.*

/**
 * This event is emitted when a player is about to be revived.
 * It is an asynchronous variant of [PreDeathBanEvent] and can be used to monitor the status of the player,
 * **but not for interfering with the process**.
 *
 *
 * It implements [YoloPlayerEvent], meaning, that it provides relevant information about the player and access to the
 * corresponding [org.bukkit.OfflinePlayer] and [org.bukkit.entity.Player].
 *
 * @property yoloPlayerInformation The information regarding this player including their current status
 * @property originalTargetResult The originally intended outcome of the event.
 * @property targetResult The currently targeted outcome of the event.
 *
 * @see PreYoloPlayerReviveEvent
 * @see YoloPlayerEvent
 * @see PlayerManager.DeathBanResult
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class PreDeathBanEventAsync internal constructor(
    override val yoloPlayerInformation: Pair<UUID, PlayerManager.PlayerStatus>,
    val targetResult: PlayerManager.DeathBanResult,
    val originalTargetResult: PlayerManager.DeathBanResult,
    val associatedPlayerDeathEvent: PlayerDeathEvent
): YoloPlayerEvent, Event(true) {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlerList
    }

    override fun getHandlers(): HandlerList = handlerList
}
