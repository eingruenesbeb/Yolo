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
import io.github.eingruenesbeb.yolo.managers.PlayerManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

/**
 * This is the command executor, registered for the command `revive`. On tab completion, it provides a list of revivable
 * players. If successfully executed, the targeted player will be unbanned and revived upon their next join.
 *
 * @see TabExecutor
 * @see PlayerManager.setReviveOnUser
 * @see PlayerManager.provideRevivable
 */
internal class ReviveCommand : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        try {
            val restoreInventory = try {
                args[1].toBooleanStrict()
            } catch (e: IndexOutOfBoundsException) {
                true
            }
            val teleportToDeathPos = try {
                args[2].toBooleanStrict()
            } catch (e: IndexOutOfBoundsException) {
                true
            }
            PlayerManager.setReviveOnUser(args[0], true, restoreInventory, teleportToDeathPos)
            sender.sendPlainMessage(Yolo.pluginResourceBundle.getString("player.setRevive").replace("%player_name%", args[0]))
        } catch (e: IllegalArgumentException) {
            return false
        } catch (e: IndexOutOfBoundsException) {
            return false
        } catch (e: IllegalStateException) {
            return false
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String> {
        return PlayerManager.provideRevivable()
    }
}
