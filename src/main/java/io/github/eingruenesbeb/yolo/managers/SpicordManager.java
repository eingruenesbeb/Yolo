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

package io.github.eingruenesbeb.yolo.managers;

import io.github.eingruenesbeb.yolo.Yolo;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spicord.SpicordLoader;
import org.spicord.api.addon.SimpleAddon;
import org.spicord.bot.DiscordBot;
import org.spicord.embed.Embed;
import org.spicord.embed.EmbedParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Objects;

/**
 * This singleton is instantiated, when Spicord is also loaded as a Plugin. It manages every aspect regarding functionality
 * for sending Discord messages.
 */
public class SpicordManager {
    private record RawDiscordEmbed(String rawString, boolean enabled) {
        // The replacements could be provided by TextReplacements#provideDefaults
        Embed returnSpicordEmbed(@Nullable HashMap<String, String> replacements) {
            String toParse = rawString;
            if (replacements != null) {
                for (String toReplace : replacements.keySet()) {
                    toParse = toParse.replace(toReplace, replacements.get(toReplace));
                }
            }
            // The parsed object being null shouldn't happen, but oh well...
            return EmbedParser.parse(toParse);
        }
    }

    /**
     * Provides the types of messages, which can be sent to Discord.
     */
    public enum DiscordMessageType {
        /**
         * Defined in the plugins data-folder under "./discord/death_message.json"
         */
        DEATH,
        /**
         * Defined in the plugins data-folder under "./discord/totem_use_message.json"
         */
        TOTEM;

        /**
         * Used for setting the enabled field for the {@link RawDiscordEmbed} record, that is stored in
         * {@link #rawDiscordEmbedEnumMap}, from the plugin's config.
         *
         * @return The key, under which the enabled value for the message is stored in the config.
         */
        @Contract(pure = true)
        private @NotNull String getEnabledKey() {
            switch (this) {
                case DEATH -> {
                    return "announce.death.discord";
                }
                case TOTEM -> {
                    return "announce.totem.discord";
                }
                default -> {
                    return "spicord.send";
                }
            }
        }

        /**
         * Describes the path for retrieving the String contents of the file. Either used to get the file containing the
         * message from the plugin's data-folder, or it's embedded resources.
         *
         * @return The path identifying the message-content's file.
         */
        @Contract(pure = true)
        private @Nullable String getResourceName() {
            switch (this) {
                case DEATH -> {
                    return "discord/death_message.json";
                }
                case TOTEM -> {
                    return "discord/totem_use_message.json";
                }
                default -> {
                    return null;
                }
            }
        }
    }

    private static final SpicordManager SINGLETON = new SpicordManager();
    private final Yolo yolo = Yolo.getPlugin(Yolo.class);
    private boolean spicordBotAvailable;
    private DiscordBot spicordBot;
    private String messageChannelId;
    private final EnumMap<DiscordMessageType, RawDiscordEmbed> rawDiscordEmbedEnumMap = new EnumMap<>(DiscordMessageType.class);

    /**
     * Gets the singleton instance of this Manager.
     * @return The singleton instance
     */
    public static SpicordManager getInstance() {
        return SINGLETON;
    }

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

    private SpicordManager() {
        loadSpicord();
    }

    private void updateMessageTemplates() throws IOException {
        for (DiscordMessageType discordMessageType : DiscordMessageType.values()) {
            String resourceName = discordMessageType.getResourceName();
            if (resourceName == null) {
                continue;
            }
            Path resourcePath = yolo.getDataFolder().toPath().resolve(resourceName);
            String contents;
            try {
                contents = Files.readString(resourcePath);
            } catch (IOException e) {
                // Default to the embedded resource.
                // Embedded resources should not throw an NPE.
                contents = new String(Objects.requireNonNull(yolo.getResource(resourceName)).readAllBytes());
            }
            RawDiscordEmbed rawDiscordEmbed = new RawDiscordEmbed(contents, yolo.getConfig().getBoolean(discordMessageType.getEnabledKey()));
            rawDiscordEmbedEnumMap.put(discordMessageType, rawDiscordEmbed);
        }
    }


    /**
     * Tries to send a message to Discord created from the configured template, using Spicord and JDA.
     *
     * @param discordMessageType The Type of the message to be sent, as defined by {@link DiscordMessageType}.
     * @param replacements The replacements to perform. (Placeholders typically look like "%example%".)
     */
    public void trySend(@NotNull DiscordMessageType discordMessageType, @Nullable HashMap<String, String> replacements) {
        if (!spicordBotAvailable) return;
        RawDiscordEmbed rawDiscordEmbed = rawDiscordEmbedEnumMap.get(discordMessageType);
        if (!rawDiscordEmbed.enabled) return;
        DiscordBot bot = yolo.getSpicordManager().getSpicordBot();
        MessageEmbed toSend = rawDiscordEmbed.returnSpicordEmbed(replacements).toJdaEmbed();
        if (toSend.isSendable()) {
            try {
                MessageCreateAction messageCreateAction = Objects.requireNonNull(bot.getJda().getTextChannelById(messageChannelId)).sendMessage(MessageCreateData.fromEmbeds(toSend));
                messageCreateAction.submit().whenComplete((message, throwable) -> {
                    // Handle potential errors
                    if (throwable != null) yolo.getLogger().severe(yolo.getPluginResourceBundle().getString("spicord.sending.failed").replace("%error%", throwable.toString()));
                });
            } catch (NullPointerException e) {
                yolo.getLogger().severe(yolo.getPluginResourceBundle().getString("spicord.sending.nullChannel"));
            }
        }
    }

    /**
     * Private method to register this plugin as an addon for Spicord and to set a few important fields for Spicord
     * support.
     */
    private void loadSpicord() {
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
            updateMessageTemplates();
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
        SpicordLoader.addStartupListener(spicord -> spicord.getAddonManager().registerAddon(new SimpleAddon("Yolo-Spicord", "yolo", "eingruenesbeb", "v0.5.0") {
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
