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

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Bukkit plugin that adds features related to player deaths and Discord integration.
 * This is the main class of that plugin.
 */
@SuppressWarnings("unused")
public final class Yolo extends JavaPlugin {
    private final ResourceBundle pluginResourceBundle = ResourceBundle.getBundle("i18n");
    private SpicordManager spicordManager;
    private boolean useAB;
    private String deathMessageTemplate;
    private Logger logger;
    private String message_channel_id;

    /**
     * Accessor for {@link Yolo#spicordManager}
     * @return The SpicordManager for the plugin.
     *
     * @see SpicordManager
     */
    public SpicordManager getSpicordManager() {
        return spicordManager;
    }
    /**
     * Accessor for {@link Yolo#message_channel_id}
     * @return The id of the channel to send the death notifications to.
     */
    public String getMessage_channel_id() {
        return message_channel_id;
    }

    /**
     * Accessor for {@link Yolo#pluginResourceBundle}
     * @return The resource bundle used for localizing the plugin's messages.
     */
    public ResourceBundle getPluginResourceBundle() {
        return pluginResourceBundle;
    }

    /**
     * Accessor for {@link Yolo#useAB}
     * @return Whether the plugin can use AdvancedBan for banning players. Should only be true, if AdvancedBan is loaded.
     */
    public boolean isUseAB() {
        return useAB;
    }

    /**
     * Accessor for {@link Yolo#deathMessageTemplate}
     * @return The template configured in this plugin's data-folder as a String. Can be used to generate an embed from,
     * whilst replacing supported variables in it. (Currently only supports {@code %player_name%}.)
     */
    public String getDeathMessageTemplate() {
        return deathMessageTemplate;
    }

    @Override
    public void onEnable() {
        try {
            regenerateMissingFiles();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, pluginResourceBundle.getString("loading.data_IOException").replace("%error%", e.toString()));
        }
        FileConfiguration config = getConfig();
        useAB = Bukkit.getPluginManager().isPluginEnabled("AdvancedBan");
        try {
            deathMessageTemplate = setDeathMessageTemplate();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, pluginResourceBundle.getString("loading.embed.IOException").replace("%error%", e.toString()));
        }
        message_channel_id = config.getString("message_channel_id");
        if (Bukkit.getPluginManager().isPluginEnabled("Spicord")) {
            spicordManager = new SpicordManager();
            spicordManager.loadSpicord();
        }
        getServer().getPluginManager().registerEvents(new YoloEventListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    /**
     * Private method for regenerating any missing file, if it doesn't exist.
     * @throws IOException Thrown, when the desired path is unavailable or another thing blocks writing to that
     * location/file.
     */
    private void regenerateMissingFiles() throws IOException {
        // Guarantee the existence of the data folder.
        //noinspection ResultOfMethodCallIgnored
        getDataFolder().mkdirs();

        // (No content checks, no subdir) config.yml:
        saveDefaultConfig();

        // (No content checks, no subdir) death_message.json:
        saveResource("death_message.json", false);
    }

    /**
     * Private method reading the configured message from the data directory.
     * @return The contents of the configured message, as a String.
     * @throws IOException If the file couldn't be read.
     */
    private String setDeathMessageTemplate() throws IOException {
        File file = new File(getDataFolder().getPath() + "/death_message.json");
        return Files.readString(file.toPath());
    }
}
