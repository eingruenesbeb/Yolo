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
 * You can reach the original autor via e-Mail: agreenbeb@gmail.com
 */

package io.github.eingruenesbeb.yolo;

import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.spicord.SpicordLoader;
import org.spicord.api.addon.SimpleAddon;
import org.spicord.bot.DiscordBot;
import org.spicord.embed.EmbedParser;

import java.util.Objects;
import java.util.logging.Level;

/**
 * This class is instantiated, when Spicord is also loaded as a Plugin. It manages every aspect regarding functionality
 * for sending Discord messages.
 */
public class SpicordManager {
    private final Yolo yolo = Yolo.getPlugin(Yolo.class);
    private boolean spicordBotAvailable = false;
    private DiscordBot spicordBot;

    /**
     * Accessor for {@link SpicordManager#spicordBotAvailable}
     * @return If the bot is available for sending a message.
     */
    public boolean isSpicordBotAvailable() {
        return spicordBotAvailable;
    }

    /**
     * Accessor for {@link SpicordManager#spicordBot}
     *
     * @return (Spicord version of) The bot, that is used for sending the message, if provided.
     * @see DiscordBot
     */
    public DiscordBot getSpicordBot() {
        return spicordBot;
    }

    /**
     * Tries to send a message to Discord created from the configured template, using Spicord and JDA.
     * @param player The player. This is used, to replace the variable {@code %player_name%} in the template. This is
     *               currently used in the event-listener.
     * @see YoloEventListener#onPlayerDeath(PlayerDeathEvent)
     */
    public void trySend(@NotNull Player player) {
        if (!spicordBotAvailable) return;
        Yolo yoloPluginInstance = Yolo.getPlugin(Yolo.class);
        DiscordBot bot = yoloPluginInstance.getSpicordManager().getSpicordBot();
        String embedFromTemplate;
        try {
            embedFromTemplate = yoloPluginInstance.getDeathMessageTemplate().replace("%player_name%", player.getName());
        } catch (NullPointerException npe){
            embedFromTemplate = Yolo.getPlugin(Yolo.class).getPluginResourceBundle().getString("sending.no_death_message");
        }
        net.dv8tion.jda.api.entities.MessageEmbed embed = EmbedParser.parse(embedFromTemplate).toJdaEmbed();
        if (embed.isSendable()) {
            try {
                MessageCreateAction messageCreateAction = Objects.requireNonNull(bot.getJda().getTextChannelById(yoloPluginInstance.getMessage_channel_id())).sendMessage(MessageCreateData.fromEmbeds(embed));
                messageCreateAction.submit().whenComplete((message, throwable) -> {
                    // Handle potential errors
                    if (throwable != null) yoloPluginInstance.getLogger().log(Level.SEVERE, yoloPluginInstance.getPluginResourceBundle().getString("sending.failed").replace("%error%", throwable.toString()));
                });
            } catch (NullPointerException e) {
                yoloPluginInstance.getLogger().log(Level.WARNING, yoloPluginInstance.getPluginResourceBundle().getString("sending.null_channel"));
            }
        }
    }

    /**
     * Package private method to register this plugin as an addon for Spicord and to set a few important fields for Spicord
     * support.
     */
    void loadSpicord() {
        SpicordLoader.addStartupListener(spicord -> spicord.getAddonManager().registerAddon(new SimpleAddon("Yolo-Spicord", "yolo-deaths", "eingruenesbeb", "v0.3.0") {
            @Override
            public void onLoad(DiscordBot bot) {
                spicordBotAvailable = bot == null;
            }

            @Override
            public void onReady(DiscordBot bot) {
                spicordBot = bot;
                spicordBotAvailable = bot != null;
                if (!spicordBotAvailable) {
                    yolo.getLogger().warning(yolo.getPluginResourceBundle().getString("loading.spicord.bot_unavailable"));
                } else {
                    yolo.getLogger().info(yolo.getPluginResourceBundle().getString("loading.spicord.bot_available"));
                }
            }

            @Override
            public void onShutdown(DiscordBot bot) {
                spicordBotAvailable = false;
            }

            @Override
            public void onDisable() {
                spicordBotAvailable = false;
            }
        }));
    }
}