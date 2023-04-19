/*
 * This is program is a plugin for Minecraft Servers called "Yolo".
 * Copyright (c) 2023  eingruenesbeb
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *   You can reach the original author via e-Mail: agreenbeb@gmail.com
 */

package io.github.eingruenesbeb.yolo.managers.spicord

import io.github.eingruenesbeb.yolo.managers.spicord.SpicordManager.RawDiscordEmbed

import org.jetbrains.annotations.Contract

/**
 * Provides the types of messages, which can be sent to Discord.
 */
internal enum class DiscordMessageType {
    /**
     * Defined in the plugin's data-folder under "./discord/death_message.json".
     */
    DEATH,

    /**
     * Defined in the plugin's data-folder under "./discord/totem_use_message.json".
     */
    TOTEM,

    /**
     * Defined in the plugin's data-folder under "./discord/player_revive.json"
     */
    PLAYER_REVIVE;

    @get:Contract(pure = true)
    val enabledKey: String
        /**
         * Used for setting the enabled field for the [RawDiscordEmbed] record, that is stored in
         * [SpicordManager.rawDiscordEmbedEnumMap], from the plugin's config.
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

            PLAYER_REVIVE -> {
                "announce.revive.discord"
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

            PLAYER_REVIVE -> {
                "discord/player_revive.json"
            }
        }
}
