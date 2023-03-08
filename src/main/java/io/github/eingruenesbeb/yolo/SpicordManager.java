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

import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.spicord.SpicordLoader;
import org.spicord.api.addon.SimpleAddon;
import org.spicord.bot.DiscordBot;
import org.spicord.embed.EmbedParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * This class is instantiated, when Spicord is also loaded as a Plugin. It manages every aspect regarding functionality
 * for sending Discord messages.
 */
public class SpicordManager {
    private final Yolo yolo = Yolo.getPlugin(Yolo.class);
    private boolean spicordBotAvailable;
    private DiscordBot spicordBot;
    private String messageChannelId;
    private String deathMessageTemplate;

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

    private void updateDeathMessageTemplate() throws IOException {
        try {
            deathMessageTemplate = Files.readString(Path.of(yolo.getDataFolder().getPath() + "/death_message.json"));
        } catch (IOException e) {
            // Shouldn't be happening, unless the file was deleted, after the plugin was loaded.
            // Default to the embedded message-config.
            // NPE shouldn't occur on embedded resources.
            deathMessageTemplate = new String(Objects.requireNonNull(yolo.getResource("death_message.json")).readAllBytes());
        }
    }

    /**
     * Tries to send a message to Discord created from the configured template, using Spicord and JDA.
     * @param player The player. This is used, to replace the variable {@code %player_name%} in the template. This is
     *               currently used in the event-listener.
     * @see YoloEventListener#onPlayerDeath(PlayerDeathEvent)
     */
    public void trySend(@NotNull Player player) {
        if (!spicordBotAvailable) return;
        DiscordBot bot = yolo.getSpicordManager().getSpicordBot();
        String embedFromTemplate;
        try {
            embedFromTemplate = deathMessageTemplate.replace("%player_name%", player.getName());
        } catch (NullPointerException npe){
            embedFromTemplate = Yolo.getPlugin(Yolo.class).getPluginResourceBundle().getString("sending.no_death_message");
        }
        net.dv8tion.jda.api.entities.MessageEmbed embed = EmbedParser.parse(embedFromTemplate).toJdaEmbed();
        if (embed.isSendable()) {
            try {
                MessageCreateAction messageCreateAction = Objects.requireNonNull(bot.getJda().getTextChannelById(messageChannelId)).sendMessage(MessageCreateData.fromEmbeds(embed));
                messageCreateAction.submit().whenComplete((message, throwable) -> {
                    // Handle potential errors
                    if (throwable != null) yolo.getLogger().severe(yolo.getPluginResourceBundle().getString("sending.failed").replace("%error%", throwable.toString()));
                });
            } catch (NullPointerException e) {
                yolo.getLogger().severe(yolo.getPluginResourceBundle().getString("sending.null_channel"));
            }
        }
    }

    /**
     * Package private method to register this plugin as an addon for Spicord and to set a few important fields for Spicord
     * support.
     */
    void loadSpicord() {
        spicordBotAvailable = false;

        if (!yolo.getConfig().getBoolean("spicord.send")) return;

        // Get and validate the channel-id:
        messageChannelId = yolo.getConfig().getString("spicord.message_channel_id");
        boolean validId = false;
        if (messageChannelId != null) {
            if (messageChannelId.matches("[0-9]+")) {
                validId = true;
            }
        }
        if (!validId) {
            yolo.getLogger().warning(yolo.getPluginResourceBundle().getString("loading.spicord.invalidId"));
            return;
        }

        // Load the message template:
        try {
            updateDeathMessageTemplate();
        } catch (IOException e) {
            // Shouldn't happen
            yolo.getLogger().severe(
                    yolo.getPluginResourceBundle()
                            .getString("loading.spicord.failedMessageTemplate")
                            .replace("%error%", e.toString())
            );
            e.printStackTrace();
            return;
        }

        // Up until this point, the field spicordBotAvailable should be false;
        // Provide the spicord loader an addon for use with this plugin.
        SpicordLoader.addStartupListener(spicord -> spicord.getAddonManager().registerAddon(new SimpleAddon("Yolo-Spicord", "yolo-deaths", "eingruenesbeb", "v0.5.0") {
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
