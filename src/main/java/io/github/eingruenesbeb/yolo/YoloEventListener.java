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

import io.github.eingruenesbeb.yolo.managers.ChatManager;
import io.github.eingruenesbeb.yolo.managers.SpicordManager;
import me.leoko.advancedban.bukkit.BukkitMethods;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * The event listener for this plugin.
 * @see Listener
 * @see org.bukkit.event.Event
 */
public class YoloEventListener implements Listener {
    private final Yolo yoloPluginInstance = Yolo.getPlugin(Yolo.class);
    private final boolean isFunctionalityEnabled = Bukkit.isHardcore() || yoloPluginInstance.getConfig().getBoolean("enable_on_non_hc", false);

    /**
     * This is the main "attraction" of this plugin, that is triggered everytime a player dies. If the player isn't
     * exempt and the server is in hardcore mode, the plugin bans the player and, if enabled, sends a custom message to
     * the configured discord text channel.
     * @param event The {@link PlayerDeathEvent} passed to the listener.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
         Player player = event.getPlayer();
         String reason = yoloPluginInstance.getPluginResourceBundle().getString("player.ban.death");
         if (!player.hasPermission("yolo.exempt") && isFunctionalityEnabled) {
             HashMap<String, String> replacementMap = TextReplacements.provideDefaults(player, TextReplacements.ALL);

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

             // It's about sending a message.
             if (yoloPluginInstance.isUseSpicord()) {
                 if (SpicordManager.getInstance().isSpicordBotAvailable()) {
                     SpicordManager.getInstance().trySend(SpicordManager.DiscordMessageType.DEATH, replacementMap);
                 }
             }
             ChatManager.getInstance().trySend(ChatManager.ChatMessageType.DEATH, replacementMap);
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
        if (!player.hasPermission("yolo.exempt") && isFunctionalityEnabled) {
            yoloPluginInstance.getResourcePackManager().applyPack(player);
        }
    }

    /**
     * Currently used for providing the capability to send a message upon totem use.
     * @param event The {@link EntityResurrectEvent} passed to the listener.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityResurrected(EntityResurrectEvent event) {
        if (event.getEntity().getType() != EntityType.PLAYER) return;
        if (!Bukkit.isHardcore()) return;
        Player player = (Player) event.getEntity();
        if (player.hasPermission("yolo.exempt")) return;

        HashMap<String, String> replacementMap = TextReplacements.provideDefaults(player, TextReplacements.PLAYER_NAME);
        replacementMap.put(TextReplacements.TOTEM_USES.toString(), String.valueOf(player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING) + 1));

        if (yoloPluginInstance.isUseSpicord()) {
            SpicordManager.getInstance().trySend(SpicordManager.DiscordMessageType.TOTEM, replacementMap);
        }
        ChatManager.getInstance().trySend(ChatManager.ChatMessageType.TOTEM, replacementMap);
    }
}
