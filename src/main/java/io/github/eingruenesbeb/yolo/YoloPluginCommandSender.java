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
