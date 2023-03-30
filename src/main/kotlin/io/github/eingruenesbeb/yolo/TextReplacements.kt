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
package io.github.eingruenesbeb.yolo

import org.bukkit.Material
import org.bukkit.Statistic
import org.bukkit.entity.Player
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
    PLAYER_NAME, TOTEM_USES,

    /**
     * A stand-in for all possible replacements. May not be used for actual replacements, but rather in conjunction with
     * [.provideDefaults].
     */
    ALL;

    /**
     * Turns the constant name into it's corresponding text-placeholder. (Example: [.PLAYER_NAME] →
     * `%player_name%`)
     *
     * @return The text-placeholder representation of the constant.
     */
    override fun toString(): String {
        return "%" + name.lowercase(Locale.getDefault()) + "%"
    }

    companion object {

        /**
         * Provides a prefilled `HashMap` of specified replacements, given the necessary other parameters. The
         * returned map can be directly used, as it is a text-placeholder (String) keyed version.
         *
         * @param player        The player for whom the replacements should be generated. If null, player-based replacements
         * will not be included in the returned map.
         * @param replacements  The replacements that should be included in the returned map. If null, an empty map will be returned.
         * If the [.ALL] constant is included in this array, it will include all predefined replacements,
         * but should not be used in combination with others.
         * @return A prefilled map of replacements.
         *
         * @implNote If any necessary reference parameter is not given, replacements referencing it will not be present in the final map.
         * For example, if the player parameter is null, player-based replacements will not be included in the returned map.
         * If the replacements array is null, an empty map will be returned.
         */
        fun provideDefaults(player: Player?, vararg replacements: TextReplacements?): HashMap<String?, String?> {
            val toReturn = HashMap<String?, String?>()
            for (replacement in replacements) {
                if (replacement != null) {
                    when (replacement) {
                        ALL -> {
                            if (player != null) {
                                toReturn[PLAYER_NAME.toString()] = player.name
                                toReturn[TOTEM_USES.toString()] =
                                    player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING).toString()
                            }
                            return toReturn
                        }

                        PLAYER_NAME -> {
                            if (player != null) {
                                toReturn[PLAYER_NAME.toString()] = player.name
                            }
                        }

                        TOTEM_USES -> {
                            if (player != null) {
                                toReturn[TOTEM_USES.toString()] =
                                    player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING).toString()
                            }
                        }
                    }
                }
            }
            return toReturn
        }
    }
}