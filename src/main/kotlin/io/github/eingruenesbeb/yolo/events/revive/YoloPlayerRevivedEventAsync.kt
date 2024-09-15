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

package io.github.eingruenesbeb.yolo.events.revive

import io.github.eingruenesbeb.yolo.events.YoloPlayerEvent
import io.github.eingruenesbeb.yolo.player.ReviveResult
import io.github.eingruenesbeb.yolo.player.YoloPlayerData
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event is emitted after a player has been revived by the plugin. This is mostly just useful for post-processing
 * and information purposes. It is an asynchronous version of [YoloPlayerRevivedEvent]. **For actions that need to
 * be executed synchronously, consider the alternative.**
 *
 *
 * It implements [YoloPlayerEvent], meaning, that it provides relevant information about the player and access to the
 * corresponding [org.bukkit.OfflinePlayer] and [org.bukkit.entity.Player].
 *
 * @property yoloPlayerInformation The information regarding this player including their current status
 * @property finalResult The result of the revive-attempt
 *
 * @see YoloPlayerRevivedEvent
 * @see YoloPlayerData
 * @see ReviveResult
 */
@Suppress("unused")
class YoloPlayerRevivedEventAsync internal constructor(override val yoloPlayerInformation: YoloPlayerData, val finalResult: ReviveResult): Event(true),
    YoloPlayerEvent {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlerList
    }

    override fun getHandlers(): HandlerList = handlerList
}
