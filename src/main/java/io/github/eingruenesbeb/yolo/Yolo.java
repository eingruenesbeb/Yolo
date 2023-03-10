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

import io.github.eingruenesbeb.yolo.managers.ChatManager;
import io.github.eingruenesbeb.yolo.managers.ResourcePackManager;
import io.github.eingruenesbeb.yolo.managers.SpicordManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

// May be interesting to implement PAPI support in the future.

/**
 * A Bukkit plugin that adds features related to player deaths and Discord integration.
 * This is the main class of that plugin.
 */
@SuppressWarnings("unused")
public final class Yolo extends JavaPlugin {
    private final ResourceBundle pluginResourceBundle = ResourceBundle.getBundle("i18n");
    private SpicordManager spicordManager;
    private boolean useSpicord;
    private boolean useAB;
    private Logger logger;
    private ResourcePackManager resourcePackManager;



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
     * Accessor for {@link Yolo#pluginResourceBundle}
     * @return The resource bundle used for localizing the plugin's messages.
     */
    public ResourceBundle getPluginResourceBundle() {
        return pluginResourceBundle;
    }

    /**
     * Accessor for {@link #useSpicord}
     *
     * @return Whether Spicord is available for use.
     * @apiNote This boolean should ALWAYS be checked, if something is to be done with the {@link SpicordManager}.
     */
    public boolean isUseSpicord() {
        return useSpicord;
    }

    /**
     * Accessor for {@link Yolo#useAB}
     * @return Whether the plugin can use AdvancedBan for banning players. Should only be true, if AdvancedBan is loaded.
     */
    public boolean isUseAB() {
        return useAB;
    }

    /**
     * Accessor for {@link Yolo#resourcePackManager}
     * @return The plugin's resource pack manager.
     * @see ResourcePackManager
     */
    public ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }

    @Override
    public void onEnable() {
        regenerateMissingFiles();
        FileConfiguration config = getConfig();

        useAB = Bukkit.getPluginManager().isPluginEnabled("AdvancedBan");
        useSpicord = Bukkit.getPluginManager().isPluginEnabled("Spicord");
        if (Bukkit.getPluginManager().isPluginEnabled("Spicord")) {
            spicordManager = SpicordManager.getInstance();
        }
        resourcePackManager = ResourcePackManager.getInstance();
        //noinspection ResultOfMethodCallIgnored
        ChatManager.getInstance();
        getServer().getPluginManager().registerEvents(new YoloEventListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    /**
     * Private method for regenerating any missing file, if it doesn't exist.
     */
    private void regenerateMissingFiles() {
        // Guarantee the existence of the data folder.
        //noinspection ResultOfMethodCallIgnored
        getDataFolder().mkdirs();

        // (With content checks, no subdir) config.yml:
        saveDefaultConfig();
        validateConfigVersion();

        // (No content checks, in subdir) death_message.json:
        if (!new File(getDataFolder().getPath() + "/discord/death_message.json").exists()) {
            saveResource("discord/death_message.json", false);
        }
        // (No content checks, in subdir)
        if (!new File(getDataFolder().getPath() + "/discord/totem_use_message.json").exists()) {
            saveResource("discord/totem_use_message.json", false);
        }

        // (Deferred content checks, no subdir)
        if (!new File(getDataFolder().getPath() + "/chat_messages.properties").exists()) {
            saveResource("chat_messages.properties", false);
        }
    }

    private void validateConfigVersion() {
        FileConfiguration inFolder = getConfig();
        Configuration embedded = getConfig().getDefaults();
        int inFolderVer = getConfig().getInt("config_version", 0);
        assert embedded != null;
        int embeddedVer = embedded.getInt("config_version");
        if (inFolderVer != embeddedVer) {
            Set<String> inFolderKeys = inFolder.getKeys(true);
            Set<String> embeddedKeys = embedded.getKeys(true);

            // Remove redundant keys:
            for (String key : inFolderKeys) {
                if (embedded.get(key) == null) {
                    inFolder.set(key, null);
                }
            }
            // Add missing keys:
            for (String key : embeddedKeys) {
                if (!inFolder.contains(key, true)) {
                    inFolder.createSection(key);
                    inFolder.set(key, embedded.get(key));
                    inFolder.setComments(key, embedded.getComments(key));
                }
            }

            // Set to the newest config version
            inFolder.set("config_version", embeddedVer);

            try {
                inFolder.save(getDataFolder().getPath() + "/config.yml");
            } catch (IOException e) {
                getLogger().severe(pluginResourceBundle.getString("loading.configUpdateFail"));
            }
        }
    }
}
