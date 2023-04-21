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

import io.github.eingruenesbeb.yolo.commands.CommandRegistrar
import io.github.eingruenesbeb.yolo.managers.ChatManager
import io.github.eingruenesbeb.yolo.managers.PlayerManager
import io.github.eingruenesbeb.yolo.managers.ResourcePackManager
import io.github.eingruenesbeb.yolo.managers.spicord.safeSpicordManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.io.IOException
import java.util.*

// May be interesting to implement PAPI support in the future.
/**
 * A Bukkit plugin that adds features related to player deaths and Discord integration.
 * This is the main class of that plugin.
 */
class Yolo : JavaPlugin() {
    companion object {
        // Meta:
        const val version = "v0.7.0"

        /**
         * This is the [ResourceBundle] to use for translating cli messages.
         */
        val pluginResourceBundle: ResourceBundle = ResourceBundle.getBundle("i18n")
    }

    /**
     * Indicates, whether the functionality of this plugin should be enabled.
     */
    var isFunctionalityEnabled = false
        private set

    /**
     * Contains the configured ban-message, to be shown to players, banned by this plugin, without any replacements
     * preformed.
     */
    internal var banMessage: Component? = null
        private set

    override fun onEnable() {
        regenerateMissingFiles()

        var rawBanMessage: String
        val banMessageFile = File(dataFolder.path.plus("/ban_message.txt"))
        runCatching {
            if (!banMessageFile.exists()) banMessageFile.createNewFile()
            rawBanMessage = banMessageFile.readText().replace("\r", "")
            banMessage = MiniMessage.miniMessage().deserialize(rawBanMessage)
        }.onFailure {
            rawBanMessage = getResource("ban_message.txt")!!.bufferedReader().readText().replace("\r", "")
            banMessage = MiniMessage.miniMessage().deserialize(rawBanMessage)
        }

        isFunctionalityEnabled = (Bukkit.isHardcore() || config.getBoolean("enable_on_non_hc", false)) && !config.getBoolean("easy_disable", false)

        // Initialize managers:
        safeSpicordManager()
        ChatManager
        PlayerManager
        ResourcePackManager

        server.pluginManager.registerEvents(YoloEventListener(), this)
        server.pluginManager.registerEvents(PlayerManager.PlayerManagerEvents(), this)
        val commandRegistrar = CommandRegistrar()
        commandRegistrar.registerCommands()

        // Setup automatic and asynchronous saving of player data, to make it less prone to data loss, after an
        // unexpected crashes.
        object : BukkitRunnable() {
            override fun run() {
                PlayerManager.savePlayerData()
            }
        }.runTaskTimerAsynchronously(this, 6000, 6000)  // Every 5 minutes at 20TPS
    }

    override fun onDisable() {
        // Plugin shutdown logic
        PlayerManager.savePlayerData()
    }

    /**
     * Reloads all configurable parts of this plugin.
     */
    internal fun globalReload() {
        regenerateMissingFiles()
        reloadConfig()
        isFunctionalityEnabled = (Bukkit.isHardcore() || config.getBoolean("enable_on_non_hc", false)) && !config.getBoolean("easy_disable", false)

        // May do some fancy shenanigans later, by reloading classes, that implement `ReloadableManager`, later. For
        // now, that's just not worth it, with only three classes, that need that.
        ResourcePackManager.reload()
        ChatManager.reload()
        safeSpicordManager()?.reload()

        var rawBanMessage: String
        val banMessageFile = File(dataFolder.path.plus("/ban_message.txt"))
        runCatching {
            if (!banMessageFile.exists()) banMessageFile.createNewFile()
            rawBanMessage = banMessageFile.readText().replace("\r", "")
            banMessage = MiniMessage.miniMessage().deserialize(rawBanMessage)
        }.onFailure {
            rawBanMessage = getResource("ban_message.txt")!!.bufferedReader().readText().replace("\r", "")
            banMessage = MiniMessage.miniMessage().deserialize(rawBanMessage)
        }
    }

    /**
     * Private method for regenerating any missing file, if it doesn't exist.
     */
    private fun regenerateMissingFiles() {
        // Guarantee the existence of the data folder.
        dataFolder.mkdirs()

        // (With content checks, no subdir) config.yml:
        saveDefaultConfig()
        validateConfigVersion()

        // (No content checks, in subdir) death_message.json:
        if (!File(dataFolder.path + "/discord/death_message.json").exists()) {
            saveResource("discord/death_message.json", false)
        }
        // (No content checks, in subdir)
        if (!File(dataFolder.path + "/discord/totem_use_message.json").exists()) {
            saveResource("discord/totem_use_message.json", false)
        }

        // (No content checks, in subdir)
        if (!File(dataFolder.path + "/discord/player_revive.json").exists()) {
            saveResource("discord/player_revive.json", false)
        }

        // (Deferred content checks, no subdir)
        if (!File(dataFolder.path + "/chat_messages.properties").exists()) {
            saveResource("chat_messages.properties", false)
        }

        // (No content checks, no subdir)
        if(!File(dataFolder.path + "/ban_message.txt").exists()) {
            saveResource("ban_message.txt", false)
        }
    }

    private fun validateConfigVersion() {
        val inFolder = config
        val embedded = config.defaults
        val inFolderVer = config.getInt("config_version", 0)
        val embeddedVer = embedded!!.getInt("config_version")
        if (inFolderVer != embeddedVer) {
            val inFolderKeys = inFolder.getKeys(true)
            val embeddedKeys = embedded.getKeys(true)

            // Remove redundant keys:
            for (key in inFolderKeys) {
                if (embedded[key] == null) {
                    inFolder[key] = null
                }
            }
            // Add missing keys:
            for (key in embeddedKeys) {
                if (!inFolder.contains(key, true)) {
                    inFolder.createSection(key)
                    inFolder[key] = embedded[key]
                    inFolder.setComments(key, embedded.getComments(key))
                }
            }

            // Set to the newest config version
            inFolder["config_version"] = embeddedVer
            try {
                inFolder.save(dataFolder.path + "/config.yml")
            } catch (e: IOException) {
                getLogger().severe(pluginResourceBundle.getString("loading.configUpdateFail"))
            }
        }
    }
}
