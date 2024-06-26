/*
 * This program is a plugin for Minecraft Servers called "Yolo".
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
package io.github.eingruenesbeb.yolo

import io.github.eingruenesbeb.yolo.events.YoloPlayerEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.Statistic
import org.bukkit.event.Event
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerEvent
import java.util.*

/**
 * The `TextReplacements` enum provides a set of named text replacements that can be used to generate dynamic
 * text in a message. These replacements can be used in chat messages, command responses, or any other
 * text-based content that needs to be customized for a specific player or situation.
 *
 *
 * Constant names should be pretty self-explanatory. If not, you can check their individual documentation.
 *
 *
 *
 * Here are some examples:
 *
 *
 *  * %player_name% has used %totem_uses% so far.
 *  * %player_name% is on hardcore mode!
 *
 *
 *
 * This enum also provides a helper method, [.provideDefaults], that takes a player object and a list of
 * TextReplacements and returns a hashmap containing the default values for the specified replacements. This can be
 * used to quickly generate replacement values for a given player without needing to manually retrieve their name
 * and totem usage.
 *
 */
enum class TextReplacements {
    PLUGIN_VERSION, PLAYER_NAME, TOTEM_USES,

    /**
     * Provides either a [TranslatableComponent] via the [provideComponentDefaults] method or an attempt at a translated
     * string version via [provideStringDefaults]
     */
    DEATH_MESSAGE,

    /**
     * A stand-in for all possible replacements. May not be used for actual replacements, but rather in conjunction with
     * [provideStringDefaults] or [provideComponentDefaults].
     */
    ALL;

    companion object {
        /**
         * Similar to [provideStringDefaults], but rather than strings this returns [Component] equivalents. These can
         * be used for (almost) everything text related in Minecraft.
         *
         * @param event        An optional event, that can provide context-based replacements. If null, context-based
         * replacements will not be included in the returned map.
         * @param replacements  The replacements that should be included in the returned map. If null, an empty map will be returned.
         * If the [ALL] constant is included in this array, it will include all predefined replacements,
         * but should not be used in combination with others.
         * @return A prefilled map of replacements.
         *
         * @implNote If any necessary reference parameter is not given, replacements referencing it will not be present in the final map.
         * For example, if the player parameter is null, player-based replacements will not be included in the returned map.
         * If the `replacements` array is null, an empty map will be returned.
         */
        fun provideComponentDefaults(event: Event?, vararg replacements: TextReplacements?): HashMap<String, Component?> {
            val toReturn = HashMap<String, Component?>()
            for (replacement in replacements) {
                if (replacement != null) {
                    when (replacement) {
                        ALL -> {
                            if (event is PlayerEvent) {
                                toReturn[PLAYER_NAME.toString()] = event.player.displayName()
                                toReturn[TOTEM_USES.toString()] =
                                    Component.text(event.player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING))
                            }
                            if (event is YoloPlayerEvent) {
                                toReturn[PLAYER_NAME.toString()] =
                                    event.player?.displayName() ?:
                                    event.offlinePlayer.playerProfile.name?.let { Component.text(it) } ?:
                                    Component.text(event.offlinePlayer.uniqueId.toString())
                                toReturn[TOTEM_USES.toString()] =
                                    event.player?.let { Component.text(it.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING)) }
                            }
                            if (event is PlayerDeathEvent) {
                                event.deathMessage()?.let { toReturn[DEATH_MESSAGE.toString()] = it}
                                toReturn[PLAYER_NAME.toString()] = event.player.displayName()
                                toReturn[TOTEM_USES.toString()] =
                                    Component.text(event.player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING))
                            }

                            toReturn[PLUGIN_VERSION.toString()] = Component.text(Yolo.VERSION)

                            return toReturn
                        }

                        PLAYER_NAME -> {
                            if (event is PlayerEvent) {
                                toReturn[PLAYER_NAME.toString()] = event.player.displayName()
                            }
                            if (event is YoloPlayerEvent) {
                                toReturn[PLAYER_NAME.toString()] =
                                    event.player?.displayName() ?:
                                    event.offlinePlayer.playerProfile.name?.let { Component.text(it) } ?:
                                    Component.text(event.offlinePlayer.uniqueId.toString())
                            }
                            if (event is PlayerDeathEvent) {
                                toReturn[PLAYER_NAME.toString()] = event.player.displayName()
                            }
                        }

                        TOTEM_USES -> {
                            if (event is PlayerEvent) {
                                toReturn[TOTEM_USES.toString()] =
                                    Component.text(event.player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING))
                            }
                            if (event is YoloPlayerEvent) {
                                toReturn[TOTEM_USES.toString()] =
                                    event.player?.let { Component.text(it.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING)) }
                            }
                            if (event is PlayerDeathEvent) {
                                toReturn[TOTEM_USES.toString()] =
                                    Component.text(event.player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING))
                            }
                        }

                        DEATH_MESSAGE -> {
                            if (event is PlayerDeathEvent) {
                                event.deathMessage()?.let { toReturn[DEATH_MESSAGE.toString()] = it}
                            }
                        }

                        PLUGIN_VERSION -> {
                            toReturn[PLUGIN_VERSION.toString()] = Component.text(Yolo.VERSION)
                        }
                    }
                }
            }
            return toReturn
        }

        /**
         * Provides a prefilled `HashMap` of specified replacements, given the necessary other parameters. The
         * returned map can be directly used, as it is a text-placeholder (String) keyed version.
         *
         * @param event        An optional event, that can provide context-based replacements. If null, context-based
         * replacements will not be included in the returned map.
         * @param replacements  The replacements that should be included in the returned map. If null, an empty map will be returned.
         * If the [ALL] constant is included in this array, it will include all predefined replacements,
         * but should not be used in combination with others.
         * @return A prefilled map of replacements.
         *
         * @implNote If any necessary reference parameter is not given, replacements referencing it will not be present in the final map.
         * For example, if the player parameter is null, player-based replacements will not be included in the returned map.
         * If the `replacements` array is null, an empty map will be returned.
         */
        fun provideStringDefaults(event: Event?, vararg replacements: TextReplacements?): HashMap<String, String?> {
            val toReturn = HashMap<String, String?>()
            for (replacement in replacements) {
                if (replacement != null) {
                    when (replacement) {
                        ALL -> {
                            if (event is PlayerEvent) {
                                toReturn[PLAYER_NAME.toString()] = event.player.name
                                toReturn[TOTEM_USES.toString()] =
                                    event.player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING).toString()
                            }

                            if (event is YoloPlayerEvent) {
                                toReturn[PLAYER_NAME.toString()] = event.offlinePlayer.playerProfile.name
                                toReturn[TOTEM_USES.toString()] =
                                    event.player?.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING)?.toString()
                                        ?: event.offlinePlayer.playerProfile.name
                            }

                            if (event is PlayerDeathEvent) {
                                // Apparently this event is not a PlayerEvent (WHY?!)
                                toReturn[PLAYER_NAME.toString()] = event.player.name
                                toReturn[TOTEM_USES.toString()] =
                                    event.player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING).toString()
                                toReturn[DEATH_MESSAGE.toString()] = event.deathMessage()
                                    ?.let { LegacyComponentSerializer.legacySection().serialize(it) }
                            }

                            toReturn[PLUGIN_VERSION.toString()] = Yolo.VERSION

                            return toReturn
                        }

                        PLAYER_NAME -> {
                            if (event is PlayerEvent) {
                                toReturn[PLAYER_NAME.toString()] = event.player.name
                            }
                            if (event is YoloPlayerEvent) {
                                toReturn[PLAYER_NAME.toString()] = event.offlinePlayer.playerProfile.name
                            }
                            if (event is PlayerDeathEvent) {
                                toReturn[PLAYER_NAME.toString()] = event.player.name
                            }
                        }

                        TOTEM_USES -> {
                            if (event is PlayerEvent) {
                                toReturn[TOTEM_USES.toString()] =
                                    event.player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING).toString()
                            }
                            if (event is YoloPlayerEvent) {
                                toReturn[TOTEM_USES.toString()] =
                                    event.player?.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING).toString()
                            }
                            if (event is PlayerDeathEvent) {
                                toReturn[TOTEM_USES.toString()] =
                                    event.player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING).toString()
                            }
                        }

                        DEATH_MESSAGE -> {
                            if (event is PlayerDeathEvent) {
                                toReturn[DEATH_MESSAGE.toString()] = event.deathMessage()
                                    ?.let { LegacyComponentSerializer.legacySection().serialize(it) }
                            }
                        }

                        PLUGIN_VERSION -> {
                            toReturn[PLUGIN_VERSION.toString()] = Yolo.VERSION
                        }
                    }
                }
            }
            return toReturn
        }
    }

    /**
     * Turns the constant name into it's corresponding text-placeholder. (Example: [PLAYER_NAME] →
     * `%player_name%`)
     *
     * @return The text-placeholder representation of the constant.
     */
    override fun toString(): String {
        return "%" + name.lowercase(Locale.getDefault()) + "%"
    }
}
