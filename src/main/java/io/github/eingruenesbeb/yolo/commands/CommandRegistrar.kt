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
package io.github.eingruenesbeb.yolo.commands

import io.github.eingruenesbeb.yolo.Yolo
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabExecutor
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * This class is responsible for registering all commands of the plugin. It provides a single public method:
 * [.registerCommands]
 */
class CommandRegistrar {
    private enum class Commands {
        RELOAD, REVIVE;

        // Currently not implemented
        override fun toString(): String {
            return if (this == RELOAD) "yolo-reload" else name.lowercase(
                Locale.getDefault()
            )
        }

        val commandInstance: CommandExecutor
            get() {
                return when (this) {
                    RELOAD -> {
                        ReloadCommand()
                    }

                    REVIVE -> {
                        ReviveCommand()
                    }
                }
            }
    }

    /**
     * Registers the commands for this plugin.
     */
    fun registerCommands() {
        for (command in Commands.values()) {
            val pluginCommand = JavaPlugin.getPlugin(Yolo::class.java).getCommand(command.toString())
                ?: continue
            pluginCommand.setExecutor(command.commandInstance)
            if (Arrays.stream(command.javaClass.interfaces).anyMatch { i: Class<*> -> i == TabExecutor::class.java }) {
                pluginCommand.tabCompleter = command.commandInstance as TabExecutor?
            }
        }
    }
}