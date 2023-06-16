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

package io.github.eingruenesbeb.yolo.events

import io.github.eingruenesbeb.yolo.managers.PlayerManager.PlayerStatus
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*

/**
 * Indicates that an event has a [yoloPlayerInformation] property, that is a [Pair] consisting of a player's [UUID] and
 * their associated [PlayerStatus].
 */
interface YoloPlayerEvent {
    /**
     * A [Pair] consisting of a player's [UUID] and their associated [PlayerStatus].
     */
    val yoloPlayerInformation: Pair<UUID, PlayerStatus>

    /**
     * The corresponding [Player], if they are online. Otherwise, this is `null`.
     *
     * @see Bukkit.getPlayer
     */
    val player: Player?
        get() = Bukkit.getPlayer(yoloPlayerInformation.first)

    /**
     * The corresponding [OfflinePlayer].
     *
     * @see Bukkit.getOfflinePlayer
     */
    val offlinePlayer: OfflinePlayer
        get() = Bukkit.getOfflinePlayer(yoloPlayerInformation.first)

    /**
     * Returns the [PlayerStatus] contained in [yoloPlayerInformation].
     */
    val status: PlayerStatus
        get() = yoloPlayerInformation.second
}
