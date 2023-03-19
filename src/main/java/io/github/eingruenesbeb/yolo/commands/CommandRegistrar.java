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

package io.github.eingruenesbeb.yolo.commands;

import io.github.eingruenesbeb.yolo.Yolo;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class CommandRegistrar {
    private enum Commands {
        RELOAD,
        REVIVE;

        @Override
        public @NotNull String toString() {
            if (this == RELOAD) return "yolo-reload";
            return name().toLowerCase();
        }

        @Nullable CommandExecutor getCommandInstance() {
            switch (this) {
                case RELOAD -> {
                    return new ReloadCommand();
                }
                case REVIVE -> {
                    return new ReviveCommand();
                }
            }
            return null;
        }
    }

    public void registerCommands() {
        for (Commands command : Commands.values()) {
            PluginCommand pluginCommand = Yolo.getPlugin(Yolo.class).getCommand(command.toString());
            if (pluginCommand == null) {
                Yolo.getPlugin(Yolo.class).getLogger().info(String.format("Ahhhhhh!\nSincerely: %s", command));
                continue;
            }
            pluginCommand.setExecutor(command.getCommandInstance());

            if (Arrays.stream(command.getClass().getInterfaces()).anyMatch(i -> i == TabExecutor.class)) {
                pluginCommand.setTabCompleter((TabExecutor) command.getCommandInstance());
            }
        }
    }
}
