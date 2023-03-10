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
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

/**
 * The ChatManager class manages chat messages for the Yolo plugin. Chat messages can be configured through a properties file.
 * Messages can be enabled or disabled in the config, and can contain placeholders that can be replaced with values.
 * <p>
 * This class is a singleton, and can be accessed using the {@link #getInstance()} method.
 * </p>
 * <p>
 * The class also provides a {@link #trySend(String, HashMap)} method to send chat messages based on their configured key.
 * </p>
 * <p>
 * This class uses the MiniMessage library to parse chat messages.
 * </p>
 *
 * @see RawChatMessage
 */
public class ChatManager {

    /**
     * A container class for raw chat messages and their enabled status.
     */
    private record RawChatMessage(String rawString, boolean enabled) {
        private static final MiniMessage MINI_MESSAGE_PARSER = MiniMessage.miniMessage();

        public @Nullable Component returnComponent(@Nullable HashMap<String, String> replacements) {
                String toParse = null;
                if (replacements != null) {
                    for (String toReplace : replacements.keySet()) {
                        toParse = rawString.replace(toReplace, replacements.get(toReplace));
                    }
                } else {
                    toParse = rawString;
                }
                if (toParse != null) {
                    return MINI_MESSAGE_PARSER.deserialize(toParse);
                } else return null;
        }
    }

    private final Yolo yolo = Yolo.getPlugin(Yolo.class);
    private final HashMap<String, RawChatMessage> rawMessages = new HashMap<>();
    private static final ChatManager singleton = new ChatManager();

    private ChatManager() {
        initMessages();
    }

    /**
     * Gets the single instance of the ChatManager class.
     *
     * @return The {@link ChatManager} singleton object.
     */
    public static ChatManager getInstance() {
        return singleton;
    }

    private void initMessages() {
        Properties embedded = new Properties();
        try {
            embedded.load(yolo.getResource("chat_messages.properties"));
        } catch (IOException e) {
            // This should never happen on an embedded resource.
        }
        Properties userConfiguredProperties = new Properties();

        try {
            // Try to load the messages, as configured in the plugin's data folder.
            File userConfigured = Path.of(yolo.getDataFolder().getPath() + "/chat_messages.properties").toFile();
            userConfiguredProperties.load(Files.newBufferedReader(userConfigured.toPath(), StandardCharsets.ISO_8859_1));

            // Perform content/version check:
            if (!embedded.getProperty("version").equals(userConfiguredProperties.getProperty("version", "0"))) {
                Set<String> embeddedKeys = embedded.stringPropertyNames();
                Set<String> loadedKeys = userConfiguredProperties.stringPropertyNames();

                loadedKeys.iterator().forEachRemaining(key -> {
                    // Remove redundant keys:
                    if (!embeddedKeys.contains(key)) {
                        userConfiguredProperties.remove(key);
                    }
                });

                embeddedKeys.iterator().forEachRemaining(key -> {
                    // Add missing keys:
                    userConfiguredProperties.putIfAbsent(key, embedded.getProperty(key));
                });

                // Update the file version:
                userConfiguredProperties.setProperty("version", embedded.getProperty("version"));

                // Finally save the new version of the userConfiguredProperties.
                userConfiguredProperties.store(Files.newBufferedWriter(userConfigured.toPath(), StandardCharsets.ISO_8859_1), null);
                // This is a stupid solution, but this undoes the escaping of the '#' character.
                String unescaped = Files.readString(userConfigured.toPath(), StandardCharsets.ISO_8859_1).replace("\\#", "#");
                Files.writeString(userConfigured.toPath(), unescaped, StandardCharsets.ISO_8859_1);
            }

            for (String key : (userConfiguredProperties.stringPropertyNames())) {
                // The config follows this pattern for chat message keys: "[name].chat".
                boolean isEnabled = yolo.getConfig().getBoolean(key + ".chat", true);
                rawMessages.put(key, new RawChatMessage(userConfiguredProperties.getProperty(key), isEnabled));
            }
        } catch (IOException e) {
            // The file should already be present and readable because of the file check on the plugin being loaded.
            // But just in case...
            // No need for a content check here.
            yolo.getLogger().severe(yolo.getPluginResourceBundle().getString("chatManager.initFailedUserProvided"));

            yolo.saveResource("chat_messages.properties", true);

            for (String key : (embedded.stringPropertyNames())) {
                // The config follows this pattern for chat message keys: "[name].chat".
                boolean isEnabled = yolo.getConfig().getBoolean(key + ".chat", true);
                rawMessages.put(key, new RawChatMessage(embedded.getProperty(key), isEnabled));
            }
        }
    }

    /**
     * Sends the specified chat message with optional replacements, if the message is enabled and the specified key exists.
     *
     * @param messageKey The key of the chat message to send.
     * @param replacements A mapping of strings to replace in the raw chat message.
     */
    public void trySend(String messageKey, @Nullable HashMap<String, String> replacements) {
        RawChatMessage rawMessage = rawMessages.get(messageKey);

        if (rawMessage == null) {
            try {
                throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                yolo.getLogger().severe(yolo.getPluginResourceBundle().getString("chatManager.noMessage")
                        .replace("%trace%", ExceptionUtils.getStackTrace(e))
                );
            }
            return;
        }

        // Finally send the message.
        Component toSend = rawMessage.returnComponent(replacements);
        if (rawMessage.enabled && toSend != null) {
            Bukkit.getServer().sendMessage(toSend);
        }
    }
}
