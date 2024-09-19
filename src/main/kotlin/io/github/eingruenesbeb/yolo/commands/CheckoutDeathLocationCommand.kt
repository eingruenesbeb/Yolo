/*
 * This program is a plugin for Minecraft Servers called "Yolo".
 * Copyright (C) 2023  eingruenesbeb
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

package io.github.eingruenesbeb.yolo.commands

import io.github.eingruenesbeb.yolo.Yolo
import io.github.eingruenesbeb.yolo.managers.ChatManager
import io.github.eingruenesbeb.yolo.managers.PlayerManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

/**
 * Represents the executor for the command `checkout_death_location`. On tab-completion, it supplies all revivable
 * players. When executed by a player, the command teleports the command sender to the death location of the target, if
 * available.
 *
 * @see TabExecutor
 * @see PlayerManager.teleportToRevivable
 */
internal class CheckoutDeathLocationCommand : TabExecutor {

    /**
     * Provides a list of revivable players as auto-complete options.
     *
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     *
     * @return The list of revivable players.
     *
     * @see [PlayerManager.provideRevivable]
     */
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): List<String> {
        return Yolo.pluginInstance!!.playerManager.provideRevivable()
    }

    /**
     * Teleports the executor if they are a player, to the location of the death from the target-revivable player.
     *
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     *
     * @return `true`, if successful, `false` otherwise
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) {
            Yolo.pluginInstance!!.chatManager.trySend(sender, ChatManager.ChatMessageType.PLAYER_ONLY_COMMAND, null)
            return false
        }

        return Yolo.pluginInstance!!.playerManager.teleportToRevivable(sender, args?.get(0) ?: "")
    }
}
