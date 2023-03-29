/*
 * This is program is a plugin for Minecraft Servers called "Yolo".
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
package io.github.eingruenesbeb.yolo

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.permissions.PermissibleBase
import java.util.*

/**
 * This class is a custom [CommandSender], that triggers commands on behalf of this plugin.
 */
class YoloPluginCommandSender private constructor() : PermissibleBase(null), CommandSender {
    override fun sendMessage(message: String) {
        Bukkit.getConsoleSender().sendMessage(message)
    }

    override fun sendMessage(vararg messages: String) {
        Bukkit.getConsoleSender().sendMessage(*messages)
    }

    override fun sendMessage(sender: UUID?, message: String) {
        Bukkit.getConsoleSender().sendMessage(sender, message)
    }

    override fun sendMessage(sender: UUID?, vararg messages: String) {
        Bukkit.getConsoleSender().sendMessage(sender, *messages)
    }

    override fun getServer(): Server {
        return Bukkit.getServer()
    }

    override fun getName(): String {
        return "[Plugin] Yolo"
    }

    override fun spigot(): CommandSender.Spigot {
        return CommandSender.Spigot()
    }

    override fun name(): Component {
        return Component.text("Yolo plugin").color(NamedTextColor.LIGHT_PURPLE)
    }

    override fun isOp(): Boolean {
        return true
    }

    companion object {
        /**
         * A singleton instance of this command sender.
         */
        val PLUGIN_COMMAND_SENDER = YoloPluginCommandSender()
    }
}