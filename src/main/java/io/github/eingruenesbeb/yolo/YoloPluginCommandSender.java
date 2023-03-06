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

package io.github.eingruenesbeb.yolo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * This class is a custom {@link CommandSender}, that triggers commands on behalf of this plugin.
 */
public class YoloPluginCommandSender extends PermissibleBase implements CommandSender {
    /**
     * A singleton instance of this command sender.
     */
    public static final YoloPluginCommandSender PLUGIN_COMMAND_SENDER = new YoloPluginCommandSender(null);

    private YoloPluginCommandSender(@Nullable ServerOperator opable) {
        super(opable);
    }

    @Override
    public void sendMessage(@NotNull String message) {
        Bukkit.getConsoleSender().sendMessage(message);
    }

    @Override
    public void sendMessage(@NotNull String... messages) {
        Bukkit.getConsoleSender().sendMessage(messages);
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String message) {
        Bukkit.getConsoleSender().sendMessage(sender, message);
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String... messages) {
        Bukkit.getConsoleSender().sendMessage(sender, messages);
    }

    @Override
    public @NotNull Server getServer() {
        return Bukkit.getServer();
    }

    @Override
    public @NotNull String getName() {
        return "[Plugin] Yolo";
    }

    @Override
    public @NotNull Spigot spigot() {
        return new Spigot();
    }

    @Override
    public @NotNull Component name() {
        return Component.text("Yolo plugin").color(NamedTextColor.LIGHT_PURPLE);
    }

    @Override
    public boolean isOp() {
        return true;
    }
}
