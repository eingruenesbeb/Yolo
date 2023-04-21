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
package io.github.eingruenesbeb.yolo.managers

import io.github.eingruenesbeb.yolo.Yolo
import io.github.eingruenesbeb.yolo.managers.ChatManager.RawChatMessage
import io.github.eingruenesbeb.yolo.managers.ChatManager.trySend
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.apache.commons.lang3.exception.ExceptionUtils
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.Contract
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * The ChatManager object manages chat messages for the Yolo plugin. Chat messages can be configured through a
 * properties file. Messages can be enabled or disabled in the config, and can contain placeholders that can be replaced
 * with values.
 *
 *
 * The class also provides a [trySend] method to send chat messages based on their configured key.
 *
 *
 *
 * This class uses the MiniMessage library to parse chat messages.
 *
 *
 * @see RawChatMessage
 */
internal object ChatManager : ReloadableManager {
    /**
     * A container class for raw chat messages and their enabled status.
     */
    private data class RawChatMessage(val rawString: String, val enabled: Boolean) {
        fun returnComponent(replacements: HashMap<String, Component?>?): Component {
            var toReturn = MiniMessage.miniMessage().deserialize(rawString)
            replacements?.forEach { replacement ->
                replacement.value?.let {  value ->
                    toReturn = toReturn.replaceText {
                        it.matchLiteral(replacement.key)
                        it.replacement(value)
                    }
                }
            }
            return toReturn
        }
    }

    /**
     * This enum represents the different types of chat messages that can be sent by the Yolo plugin.
     */
    enum class ChatMessageType {
        DEATH, TOTEM, PLAYER_ONLY_COMMAND, PLAYER_REVIVED;

        companion object {
            /**
             * Returns the chat message type corresponding to the specified properties key.
             * @param key The properties key to look up.
             * @return The corresponding chat message type, or null if the key does not match any defined message type.
             */
            @Contract(pure = true)
            fun fromPropertiesKey(key: String): ChatMessageType? {
                return when (key) {
                    "announce.totem" -> {
                        TOTEM
                    }

                    "announce.death" -> {
                        DEATH
                    }

                    "system.playerOnlyCommand" -> {
                        PLAYER_ONLY_COMMAND
                    }

                    "announce.revive" -> {
                        PLAYER_REVIVED
                    }

                    else -> {
                        null
                    }
                }
            }
        }

        @get:Contract(pure = true)
        val enabledKey: String?
            /**
             * Returns the key to get the enabled status from the config for this message type.
             *
             * @return The key in under which the enabled status for this message type is found, or null if the key is not
             * defined.
             */
            get() = when (this) {
                TOTEM, DEATH, PLAYER_REVIVED -> {
                    "$propertiesKey.chat"
                }

                else -> {
                    null
                }
            }

        @get:Contract(pure = true)
        private val propertiesKey: String
            get() = when (this) {
                TOTEM -> {
                    "announce.totem"
                }

                DEATH -> {
                    "announce.death"
                }

                PLAYER_REVIVED -> {
                    "announce.revive"
                }

                PLAYER_ONLY_COMMAND -> {
                    "system.playerOnlyCommand"
                }
            }
    }

    private val yolo = JavaPlugin.getPlugin(Yolo::class.java)
    private val rawMessagesEnumMap = EnumMap<ChatMessageType, RawChatMessage>(
        ChatMessageType::class.java
    )

    init {
        initMessages()
    }

    /**
     * Sends the specified chat message with optional replacements, if the message is enabled and the specified key exists.
     *
     * @param targetAudience The [Audience], that should see the message.
     * @param messageType The [ChatMessageType] of the chat message to send.
     * @param replacements A mapping of strings to replace in the raw chat message.
     */
    fun trySend(targetAudience: Audience, messageType: ChatMessageType, replacements: HashMap<String, Component?>?) {
        val rawMessage = rawMessagesEnumMap[messageType]
        if (rawMessage == null) {
            try {
                throw IllegalArgumentException()
            } catch (e: IllegalArgumentException) {
                yolo.getLogger().severe(
                    Yolo.pluginResourceBundle.getString("chatManager.noMessage")
                        .replace("%trace%", ExceptionUtils.getStackTrace(e))
                )
            }
            return
        }

        // Finally send the message.
        val toSend = rawMessage.returnComponent(replacements)
        if (rawMessage.enabled) {
            targetAudience.sendMessage(toSend)
        }
    }

    override fun reload() {
        initMessages()
    }

    private fun initMessages() {
        rawMessagesEnumMap.clear()
        val embedded = Properties()
        try {
            embedded.load(yolo.getResource("chat_messages.properties"))
        } catch (e: IOException) {
            // This should never happen on an embedded resource.
        }
        val userConfiguredProperties = Properties()
        try {
            // Try to load the messages, as configured in the plugin's data folder.
            val userConfigured = Path.of(yolo.dataFolder.path + "/chat_messages.properties").toFile()
            userConfiguredProperties.load(Files.newBufferedReader(userConfigured.toPath(), StandardCharsets.ISO_8859_1))

            // Perform content/version check:
            if (embedded.getProperty("version") != userConfiguredProperties.getProperty("version", "0")) {
                val embeddedKeys = embedded.stringPropertyNames()
                val loadedKeys = userConfiguredProperties.stringPropertyNames()
                loadedKeys.iterator().forEachRemaining { key: String ->
                    // Remove redundant keys:
                    if (!embeddedKeys.contains(key)) {
                        userConfiguredProperties.remove(key)
                    }
                }
                embeddedKeys.iterator().forEachRemaining { key: String? ->
                    // Add missing keys:
                    userConfiguredProperties.putIfAbsent(key, embedded.getProperty(key))
                }

                // Update the file version:
                userConfiguredProperties.setProperty("version", embedded.getProperty("version"))

                // Finally save the new version of the userConfiguredProperties.
                userConfiguredProperties.store(
                    Files.newBufferedWriter(
                        userConfigured.toPath(),
                        StandardCharsets.ISO_8859_1
                    ), null
                )
                // This is a stupid solution, but this undoes the escaping of the '#' character.
                val unescaped =
                    Files.readString(userConfigured.toPath(), StandardCharsets.ISO_8859_1).replace("\\#", "#")
                Files.writeString(userConfigured.toPath(), unescaped, StandardCharsets.ISO_8859_1)
            }
            for (key in userConfiguredProperties.stringPropertyNames()) {
                // The config follows this pattern for chat message keys: "[name].chat".
                val enumRepresentation = ChatMessageType.fromPropertiesKey(key)
                if (enumRepresentation != null) {
                    val isEnabled = enumRepresentation.enabledKey?.let { yolo.config.getBoolean(it, true) } ?: true
                    rawMessagesEnumMap[enumRepresentation] =
                        RawChatMessage(userConfiguredProperties.getProperty(key), isEnabled)
                }
            }
        } catch (e: IOException) {
            // The file should already be present and readable because of the file check on the plugin being loaded.
            // But just in case...
            // No need for a content check here.
            yolo.getLogger().severe(Yolo.pluginResourceBundle.getString("chatManager.initFailedUserProvided"))
            yolo.saveResource("chat_messages.properties", true)
            for (key in embedded.stringPropertyNames()) {
                // The config follows this pattern for chat message keys: "[name].chat".
                val enumRepresentation = ChatMessageType.fromPropertiesKey(key)
                if (enumRepresentation != null) {
                    val isEnabled = enumRepresentation.enabledKey?.let { yolo.config.getBoolean(it, true) } ?: true
                    rawMessagesEnumMap[enumRepresentation] = RawChatMessage(embedded.getProperty(key), isEnabled)
                }
            }
        }
    }
}
