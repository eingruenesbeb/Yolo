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

package io.github.eingruenesbeb.yolo.events.deathBan

import io.github.eingruenesbeb.yolo.events.YoloPlayerEvent
import io.github.eingruenesbeb.yolo.events.revive.YoloPlayerRevivedEventAsync
import io.github.eingruenesbeb.yolo.managers.PlayerManager
import io.github.eingruenesbeb.yolo.managers.PlayerManager.PlayerStatus
import io.github.eingruenesbeb.yolo.managers.PlayerManager.ReviveResult
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.PlayerDeathEvent
import java.util.*

/**
 * This event is emitted after a player has been death-banned by the plugin.
 * This is mostly just useful for post-processing and information purposes.
 * It is a synchronous version of [PostDeathBanEventAsync].
 * **For actions that are not needed to be executed synchronously, consider the alternative.**
 *
 *
 * It implements [YoloPlayerEvent], meaning, that it provides relevant information about the player and access to the
 * corresponding [org.bukkit.OfflinePlayer] and [org.bukkit.entity.Player].
 *
 * @property yoloPlayerInformation The information regarding this player including their current status
 * @property result The result of the death-ban.
 *
 * @see YoloPlayerRevivedEventAsync
 * @see PlayerStatus
 * @see ReviveResult
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class PostDeathBanEvent internal constructor(
    override val yoloPlayerInformation: Pair<UUID, PlayerStatus>,
    val result: PlayerManager.DeathBanResult,
    val associatedPlayerDeathEvent: PlayerDeathEvent
): YoloPlayerEvent, Event() {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlerList
    }

    override fun getHandlers(): HandlerList = handlerList
}
