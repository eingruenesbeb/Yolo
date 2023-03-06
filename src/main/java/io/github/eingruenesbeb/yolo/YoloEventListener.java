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

import me.leoko.advancedban.bukkit.BukkitMethods;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

/**
 * The event listener for this plugin.
 * @see Listener
 * @see org.bukkit.event.Event
 */
public class YoloEventListener implements Listener {
    private final Yolo yoloPluginInstance = Yolo.getPlugin(Yolo.class);

    /**
     * This is the main "attraction" of this plugin, that is triggered everytime a player dies. If the player isn't
     * exempt and the server is in hardcore mode, the plugin bans the player and, if enabled, sends a custom message to
     * the configured discord text channel.
     * @param event The {@link PlayerDeathEvent} passed to the listener.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
         Player player = event.getPlayer();
         String reason = yoloPluginInstance.getPluginResourceBundle().getString("player.ban.death");
         if (!player.hasPermission("yolo.exempt") && Bukkit.getServer().isHardcore()) {
             if (yoloPluginInstance.isUseAB()) {
                 BukkitMethods abMethods = new BukkitMethods();
                 abMethods.loadFiles();
                 boolean layoutConfigured = abMethods.getLayouts().contains("Message.Hardcore_death");
                 if (layoutConfigured) {
                     Bukkit.dispatchCommand(YoloPluginCommandSender.PLUGIN_COMMAND_SENDER, String.format("ban -s %s @Hardcore_death", player.getName()));
                 } else {
                     Bukkit.dispatchCommand(YoloPluginCommandSender.PLUGIN_COMMAND_SENDER, String.format("ban -s %s %s", player.getName(), reason));
                 }
             } else {
                 player.banPlayerFull(reason);
             }
             if (yoloPluginInstance.getSpicordManager().isSpicordBotAvailable() && yoloPluginInstance.getConfig().getBoolean("send")) {
                 yoloPluginInstance.getSpicordManager().trySend(player);
             }
         }
    }

    /**
     * Send all players, that are not exempt a forced resource-pack, to reflect, that they are (essentially) in hardcore-
     * mode.
     * @param event The {@link PlayerJoinEvent} passed to the listener.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("yolo.exempt") && Bukkit.isHardcore()) {
            final TextComponent textComponent = (TextComponent) Component.text("You are in hardcore mode. Please accept this ressource pack to reflecting that.").replaceText(builder -> {
                builder.matchLiteral("You are in hardcore mode. Please accept this ressource pack to reflecting that.");
                switch (player.locale().getLanguage()) {
                    case "lol_US":
                        builder.replacement("U R IN YOLO MODE. PLZ ACCEPT DIS RESOURCE PACKZ 2 REFLECTIN DAT.");
                    case "de_DE":
                        builder.replacement("Du bist im Hardcoremodus. Bitte akzeptiere dieses Resourcenpacket um dies akkurat darzustellen.");
                    default:
                        builder.replacement("You are in hardcore mode. Please accept this ressource pack to reflecting that.");
                }
                builder.once();
            });

            player.setResourcePack("https://drive.google.com/uc?export=download&id=1UWoiOGFlt2QIyQPVKAv5flLTNeNiI439",
                getResourcePackHash(),
                textComponent,
                true
            );
        }
    }

    /**
     * A private method, used for retrieving the byte array version of this plugin's resource-pack's hash.
     * @return The byte array version of this plugin's resource-pack's hash
     */
    private byte @NotNull [] getResourcePackHash() {
        assert "cc17ee284417acd83536af878dabecab7ca7f3d1".matches("[0-f]{40}");
        byte[] result = new byte["cc17ee284417acd83536af878dabecab7ca7f3d1".length() / 2];
        for (int i = 0; i < result.length; i++) {
            int index = i * 2;
            int val = Integer.parseInt("cc17ee284417acd83536af878dabecab7ca7f3d1".substring(index, index + 2), 16);
            result[i] = (byte) val;
        }

        return result;
    }
}
