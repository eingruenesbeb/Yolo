/*
 * This program is a plugin for Minecraft Servers called "Yolo".
 * Copyright (C) 2023-2023  eingruenesbeb
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
package io.github.eingruenesbeb.yolo.managers.spicord

import io.github.eingruenesbeb.yolo.Yolo
import io.github.eingruenesbeb.yolo.managers.ReloadableManager
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.bukkit.plugin.java.JavaPlugin
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
 * This object manages every aspect regarding functionality for sending Discord messages via Spicord.
 */
internal object SpicordManager : ReloadableManager {
    /**
     * Used to store the raw string data and enabled status of a discord embed. All potential [RawDiscordEmbed]s are
     * stored within the private value [rawDiscordEmbedEnumMap] for look-ups and don't have any other apparent use
     * cases.
     */
    @JvmRecord
    data class RawDiscordEmbed(val rawString: String, val enabled: Boolean) {
        /**
         * Converts the [rawString] into an [Embed] from Spicord, whilst applying potential replacements, which may be
         * provided by [io.github.eingruenesbeb.yolo.TextReplacements.provideStringDefaults]-.
         */
        fun returnSpicordEmbed(replacements: HashMap<String, String?>?): Embed {
            var toParse = rawString
            replacements?.keys?.forEach { replacementKey ->
                replacements[replacementKey]?.let { toParse = toParse.replace(replacementKey, it) }
            }
            return EmbedParser.parse(toParse)
        }
    }

    /**
     * If the bot is available for sending a message.
     */
    var isSpicordBotAvailable = false
        private set

    /**
     * (Spicord version of) The bot, that is used for sending the message, if provided.
     *
     * @see DiscordBot
     */
    var spicordBot: DiscordBot? = null
        private set

    private val yolo = JavaPlugin.getPlugin(Yolo::class.java)
    private val rawDiscordEmbedEnumMap = EnumMap<DiscordMessageType, RawDiscordEmbed>(DiscordMessageType::class.java)
    private var messageChannelId: String? = null

    init {
        loadSpicord()
    }

    /**
     * Tries to send a message to Discord created from the configured template, using Spicord and JDA.
     *
     * @param discordMessageType The Type of the message to be sent, as defined by [DiscordMessageType].
     * @param replacements The replacements to perform. (Placeholders typically look like "%example%".)
     */
    fun trySend(discordMessageType: DiscordMessageType, replacements: HashMap<String, String?>?) {
        if (!isSpicordBotAvailable) return
        if (!yolo.config.getBoolean("spicord.send")) return
        val rawDiscordEmbed = rawDiscordEmbedEnumMap[discordMessageType]
        if (!rawDiscordEmbed!!.enabled) return
        val toSend = rawDiscordEmbed.returnSpicordEmbed(replacements).toJdaEmbed()
        if (toSend.isSendable) {
            try {
                val textChannel = spicordBot!!.jda.getGuildChannelById(messageChannelId!!) as? TextChannel
                val messageCreateAction = textChannel!!.sendMessage(MessageCreateData.fromEmbeds(toSend))
                messageCreateAction.submit().whenComplete { _: Message?, throwable: Throwable? ->
                    // Handle potential errors
                    if (throwable != null) yolo.logger.severe {
                        Yolo.pluginResourceBundle.getString("spicord.sending.failed")
                            .replace("%error%", throwable.toString())
                    }
                }
            } catch (e: NullPointerException) {
                yolo.logger.severe {Yolo.pluginResourceBundle.getString("spicord.sending.nullChannel") }
            }
        }
    }

    override fun reload() {
        isSpicordBotAvailable = false

        // Get and validate the channel-id:
        messageChannelId = yolo.config.getString("spicord.message_channel_id")
        if (messageChannelId?.matches("[0-9]+".toRegex()) != true) {
            yolo.logger.warning { Yolo.pluginResourceBundle.getString("loading.spicord.invalidId") }
            return
        }

        // Load the message templates:
        try {
            updateMessageTemplates()
        } catch (e: IOException) {
            // Shouldn't happen
            yolo.logger.severe {
                Yolo.pluginResourceBundle
                    .getString("loading.spicord.failedMessageTemplate")
                    .replace("%error%", e.toString())
            }
            e.printStackTrace()
            return
        }
        isSpicordBotAvailable = spicordBot!!.isReady
    }

    @Throws(IOException::class)
    private fun updateMessageTemplates() {
        rawDiscordEmbedEnumMap.clear()
        for (discordMessageType in DiscordMessageType.entries) {
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

    /**
     * Private method to register this plugin as an addon for Spicord and to set a few important fields for Spicord
     * support.
     */
    private fun loadSpicord() {
        isSpicordBotAvailable = false

        // Get and validate the channel-id:
        messageChannelId = yolo.config.getString("spicord.message_channel_id")
        if (messageChannelId?.matches("[0-9]+".toRegex()) != true) {
            yolo.logger.warning { Yolo.pluginResourceBundle.getString("loading.spicord.invalidId") }
            return
        }

        // Load the message templates:
        try {
            updateMessageTemplates()
        } catch (e: IOException) {
            // Shouldn't happen
            yolo.logger.severe {
                Yolo.pluginResourceBundle
                    .getString("loading.spicord.failedMessageTemplate")
                    .replace("%error%", e.toString())
            }
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
                        yolo.logger.warning { Yolo.pluginResourceBundle.getString("loading.spicord.bot_unavailable") }
                    } else if (yolo.config.getBoolean("spicord.send")) {
                        yolo.logger.info { Yolo.pluginResourceBundle.getString("loading.spicord.bot_available") }
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
}
