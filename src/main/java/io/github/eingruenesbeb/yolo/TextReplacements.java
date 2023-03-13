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

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Objects;

/**
 * The {@code TextReplacements} enum provides a set of named text replacements that can be used to generate dynamic
 * text in a message. These replacements can be used in chat messages, command responses, or any other
 * text-based content that needs to be customized for a specific player or situation.
 * <p>
 *     Constant names should be pretty self explanatory. If not, you can check their individual documentation.
 * </p>
 * <p>
 *     Here are some examples:
 * </p>
 * <ul>
 *     <li>%player_name% has used %totem_uses% so far.</li>
 *     <li>%player_name% is on hardcore mode!</li>
 * </ul>
 * <p>
 *     This enum also provides a helper method, {@link #provideDefaults}, that takes a player object and a list of
 *     TextReplacements and returns a hashmap containing the default values for the specified replacements. This can be
 *     used to quickly generate replacement values for a given player without needing to manually retrieve their name
 *     and totem usage.
 * </p>
 */
public enum TextReplacements {
    PLAYER_NAME,
    TOTEM_USES,
    /**
     * A stand-in for all possible replacements. May not be used for actual replacements, but rather in conjunction with
     * {@link #provideDefaults(Player, TextReplacements...)}.
     */
    ALL;

    /**
     * Turns the constant name into it's corresponding text-placeholder. (Example: {@link #PLAYER_NAME} →
     * {@code %player_name%})
     *
     * @return The text-placeholder representation of the constant.
     */
    @Override
    public @NotNull String toString() {
        return "%" + name().toLowerCase() + "%";
    }

    /**
     * Returns a {@code HashMap<String, String>} version of the provided {@code EnumMap<TextReplacements, String>}.
     * The keys in the resulting map are the text-placeholder versions of the corresponding {@code TextReplacements}
     * constants, and the values are the values from the original enum map.
     *
     * @param replacementsEnumMap the enum map to be converted to a {@code HashMap<String, String>}, used for actually
     *                            applying the replacements.
     * @return a {@code HashMap<String, String>} version of the provided {@code EnumMap<TextReplacements, String>}
     *         or {@code null} if the input map is null
     */
    public static @Nullable HashMap<String, String> fromEnumMap(EnumMap<TextReplacements, String> replacementsEnumMap) {
        if (replacementsEnumMap == null) return null;
        HashMap<String, String> toReturn = new HashMap<>();
        replacementsEnumMap.forEach((replacementEnum, s) -> toReturn.put(replacementEnum.toString(), s));
        return toReturn;
    }

    /**
     * Provides a prefilled {@code HashMap} of specified replacements, given the necessary other parameters. The
     * returned map can be directly used, as it is a text-placeholder (String) keyed version.
     *
     * @param player        The player for whom the replacements should be generated. If null, player-based replacements
     *                      will not be included in the returned map.
     * @param replacements  The replacements that should be included in the returned map. If null, an empty map will be returned.
     *                      If the {@link #ALL} constant is included in this array, it will include all predefined replacements,
     *                      but should not be used in combination with others.
     * @return A prefilled map of replacements.
     *
     * @implNote If any necessary reference parameter is not given, replacements referencing it will not be present in the final map.
     *           For example, if the player parameter is null, player-based replacements will not be included in the returned map.
     *           If the replacements array is null, an empty map will be returned.
     */
    public static @NotNull HashMap<String, String> provideDefaults(@Nullable Player player, @Nullable TextReplacements... replacements) {
        HashMap<String, String> toReturn = new HashMap<>();
        for (TextReplacements replacement : replacements) {
            if (replacement != null) {
                switch (Objects.requireNonNull(replacement)) {
                    case ALL -> {
                        if (player != null) {
                            toReturn.put(PLAYER_NAME.toString(), player.getName());
                            toReturn.put(TOTEM_USES.toString(), String.valueOf(player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING)));
                        }
                        return toReturn;
                    }
                    case PLAYER_NAME -> {
                        if (player != null) {
                            toReturn.put(PLAYER_NAME.toString(), player.getName());
                        }
                    }
                    case TOTEM_USES -> {
                        if (player != null) {
                            toReturn.put(TOTEM_USES.toString(), String.valueOf(player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING)));
                        }
                    }
                }
            }
        }

        return toReturn;
    }
}
