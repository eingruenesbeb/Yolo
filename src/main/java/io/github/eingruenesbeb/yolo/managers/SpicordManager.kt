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
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.Contract
import org.spicord.Spicord
import org.spicord.SpicordLoader
import org.spicord.api.addon.SimpleAddon
import org.spicord.bot.DiscordBot
import org.spicord.embed.Embed
import org.spicord.embed.EmbedParser
import java.io.IOException
import java.nio.file.Files
import java.util.*

/**
 * This singleton is instantiated, when Spicord is also loaded as a Plugin. It manages every aspect regarding functionality
 * for sending Discord messages.
 */
class SpicordManager private constructor() {
    @JvmRecord
    private data class RawDiscordEmbed(val rawString: String, val enabled: Boolean) {
        // The replacements could be provided by TextReplacements#provideDefaults
        fun returnSpicordEmbed(replacements: HashMap<String?, String?>?): Embed {
            var toParse = rawString
            if (replacements != null) {
                for (toReplace in replacements.keys) {
                    toParse = toParse.replace(toReplace!!, replacements[toReplace]!!)
                }
            }
            // The parsed object being null shouldn't happen, but oh well...
            return EmbedParser.parse(toParse)
        }
    }

    /**
     * Provides the types of messages, which can be sent to Discord.
     */
    enum class DiscordMessageType {
        /**
         * Defined in the plugins data-folder under "./discord/death_message.json".
         */
        DEATH,

        /**
         * Defined in the plugins data-folder under "./discord/totem_use_message.json".
         */
        TOTEM;

        @get:Contract(pure = true)
        val enabledKey: String
            /**
             * Used for setting the enabled field for the [RawDiscordEmbed] record, that is stored in
             * [.rawDiscordEmbedEnumMap], from the plugin's config.
             *
             * @return The key, under which the enabled value for the message is stored in the config.
             */
            get() = when (this) {
                DEATH -> {
                    "announce.death.discord"
                }

                TOTEM -> {
                    "announce.totem.discord"
                }
            }

        @get:Contract(pure = true)
        val resourceName: String
            /**
             * Describes the path for retrieving the String contents of the file. Either used to get the file containing the
             * message from the plugin's data-folder, or it's embedded resources.
             *
             * @return The path identifying the message-content's file.
             */
            get() = when (this) {
                DEATH -> {
                    "discord/death_message.json"
                }

                TOTEM -> {
                    "discord/totem_use_message.json"
                }
            }
    }

    private val yolo = JavaPlugin.getPlugin(Yolo::class.java)

    /**
     * This is an accessor for [isSpicordBotAvailable].
     * @return If the bot is available for sending a message.
     */
    var isSpicordBotAvailable = false
        private set

    /**
     * This is an accessor for [SpicordManager.spicordBot].
     *
     * @return (Spicord version of) The bot, that is used for sending the message, if provided.
     * @see DiscordBot
     */
    var spicordBot: DiscordBot? = null
        private set
    private var messageChannelId: String? = null
    private val rawDiscordEmbedEnumMap = EnumMap<DiscordMessageType, RawDiscordEmbed>(
        DiscordMessageType::class.java
    )

    init {
        loadSpicord()
    }

    /**
     * Tries to send a message to Discord created from the configured template, using Spicord and JDA.
     *
     * @param discordMessageType The Type of the message to be sent, as defined by [DiscordMessageType].
     * @param replacements The replacements to perform. (Placeholders typically look like "%example%".)
     */
    fun trySend(discordMessageType: DiscordMessageType, replacements: HashMap<String?, String?>?) {
        if (!isSpicordBotAvailable) return
        if (!yolo.config.getBoolean("spicord.send")) return
        val rawDiscordEmbed = rawDiscordEmbedEnumMap[discordMessageType]
        if (!rawDiscordEmbed!!.enabled) return
        val bot = instance.spicordBot
        val toSend = rawDiscordEmbed.returnSpicordEmbed(replacements).toJdaEmbed()
        if (toSend.isSendable) {
            try {
                val messageCreateAction = Objects.requireNonNull(
                    bot!!.jda.getTextChannelById(
                        messageChannelId!!
                    )
                )?.sendMessage(MessageCreateData.fromEmbeds(toSend))
                messageCreateAction?.submit()?.whenComplete { _: Message?, throwable: Throwable? ->
                    // Handle potential errors
                    if (throwable != null) yolo.getLogger().severe(
                        yolo.pluginResourceBundle.getString("spicord.sending.failed")
                            .replace("%error%", throwable.toString())
                    )
                }
            } catch (e: NullPointerException) {
                yolo.getLogger().severe(yolo.pluginResourceBundle.getString("spicord.sending.nullChannel"))
            }
        }
    }

    @Throws(IOException::class)
    private fun updateMessageTemplates() {
        rawDiscordEmbedEnumMap.clear()
        for (discordMessageType in DiscordMessageType.values()) {
            val resourceName: String = discordMessageType.resourceName
            val resourcePath = yolo.dataFolder.toPath().resolve(resourceName)
            val contents: String = try {
                Files.readString(resourcePath)
            } catch (e: IOException) {
                // Default to the embedded resource.
                // Embedded resources should not throw an NPE.
                String(yolo.getResource(resourceName)!!.readAllBytes())
            }
            val rawDiscordEmbed = RawDiscordEmbed(contents, yolo.config.getBoolean(discordMessageType.enabledKey))
            rawDiscordEmbedEnumMap[discordMessageType] = rawDiscordEmbed
        }
    }

    private fun reloadInstance() {
        isSpicordBotAvailable = false


        // Get and validate the channel-id:
        messageChannelId = yolo.config.getString("spicord.message_channel_id")
        var validId = false
        if (messageChannelId != null) {
            if (messageChannelId!!.matches("[0-9]+".toRegex())) {
                validId = true
            }
        }
        if (!validId) {
            yolo.getLogger().warning(yolo.pluginResourceBundle.getString("loading.spicord.invalidId"))
            return
        }

        // Load the message templates:
        try {
            updateMessageTemplates()
        } catch (e: IOException) {
            // Shouldn't happen
            yolo.getLogger().severe(
                yolo.pluginResourceBundle
                    .getString("loading.spicord.failedMessageTemplate")
                    .replace("%error%", e.toString())
            )
            e.printStackTrace()
            return
        }
        isSpicordBotAvailable = spicordBot!!.isReady
    }

    /**
     * Private method to register this plugin as an addon for Spicord and to set a few important fields for Spicord
     * support.
     */
    private fun loadSpicord() {
        isSpicordBotAvailable = false

        // Get and validate the channel-id:
        messageChannelId = yolo.config.getString("spicord.message_channel_id")
        var validId = false
        if (messageChannelId != null) {
            if (messageChannelId!!.matches("[0-9]+".toRegex())) {
                validId = true
            }
        }
        if (!validId) {
            yolo.getLogger().warning(yolo.pluginResourceBundle.getString("loading.spicord.invalidId"))
            return
        }

        // Load the message templates:
        try {
            updateMessageTemplates()
        } catch (e: IOException) {
            // Shouldn't happen
            yolo.getLogger().severe(
                yolo.pluginResourceBundle
                    .getString("loading.spicord.failedMessageTemplate")
                    .replace("%error%", e.toString())
            )
            e.printStackTrace()
            return
        }

        // Up until this point, the field spicordBotAvailable should be false;
        // Provide the spicord loader an addon for use with this plugin.
        SpicordLoader.addStartupListener { spicord: Spicord ->
            spicord.addonManager.registerAddon(object : SimpleAddon("Yolo-Spicord", "yolo", "eingruenesbeb", "v0.5.0") {
                override fun onReady(bot: DiscordBot) {
                    spicordBot = bot
                    isSpicordBotAvailable = true
                    if (!isSpicordBotAvailable) {
                        yolo.getLogger().warning(yolo.pluginResourceBundle.getString("loading.spicord.bot_unavailable"))
                    } else if (yolo.config.getBoolean("spicord.send")) {
                        yolo.getLogger().info(yolo.pluginResourceBundle.getString("loading.spicord.bot_available"))
                    }
                }

                override fun onShutdown(bot: DiscordBot) {
                    isSpicordBotAvailable = false
                }

                override fun onDisable() {
                    isSpicordBotAvailable = false
                }
            })
        }
    }

    companion object {
        /**
         * Gets the singleton instance of this Manager.
         * @return The singleton instance
         */
        val instance = SpicordManager()

        /**
         * This method is used, to update the singleton instance of this manager, based on the current config file.
         */
        fun reload() {
            instance.reloadInstance()
        }
    }
}