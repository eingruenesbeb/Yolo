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

import io.github.eingruenesbeb.yolo.managers.ChatManager
import io.github.eingruenesbeb.yolo.managers.PlayerManager
import io.github.eingruenesbeb.yolo.managers.SpicordManager
import io.github.eingruenesbeb.yolo.managers.SpicordManager.DiscordMessageType
import me.leoko.advancedban.bukkit.BukkitMethods
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Statistic
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * The event listener for this plugin.
 * @see Listener
 *
 * @see org.bukkit.event.Event
 */
class YoloEventListener : Listener {
    private val yoloPluginInstance = JavaPlugin.getPlugin(Yolo::class.java)

    /**
     * This is the main "attraction" of this plugin, that is triggered everytime a player dies. If the player isn't
     * exempt and the server is in hardcore mode, the plugin bans the player and, if enabled, sends a custom message to
     * the configured discord text channel.
     * @param event The [PlayerDeathEvent] passed to the listener.
     */
    @EventHandler(ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        var banMessage = ""
        val banMessageFile = File(yoloPluginInstance.dataFolder.path.plus("/ban_message.txt"))
        runCatching {
            if (!banMessageFile.exists()) banMessageFile.createNewFile()
            banMessage = banMessageFile.readText()
        }.onFailure {
            banMessage = yoloPluginInstance.getResource("ban_message.txt")!!.bufferedReader().readText()
        }

        if (!player.hasPermission("yolo.exempt") && yoloPluginInstance.isFunctionalityEnabled) {
            PlayerManager.instance.actionsOnDeath(player)
            val replacementMap: HashMap<String?, String?> =
                TextReplacements.provideDefaults(player, TextReplacements.ALL)
            if (yoloPluginInstance.isUseAB) {
                val abMethods = BukkitMethods()
                abMethods.loadFiles()
                val layoutConfigured = abMethods.layouts.contains("Message.Hardcore_death")
                if (layoutConfigured) {
                    Bukkit.dispatchCommand(
                        YoloPluginCommandSender.PLUGIN_COMMAND_SENDER,
                        String.format("ban -s %s @Hardcore_death", player.name)
                    )
                } else {
                    Bukkit.dispatchCommand(
                        YoloPluginCommandSender.PLUGIN_COMMAND_SENDER,
                        String.format("ban -s %s %s", player.name, banMessage)
                    )
                }
                // Here players will not retain their inventory.
            } else {
                player.banPlayer(banMessage)
                // Players will retain their inventory but still drop it (essentially duping it) after death, when
                // banned the instant they die (somehow). Therefore, it has to be removed explicitly.
            }

            // It's about sending a message.
            if (yoloPluginInstance.isUseSpicord) {
                if (SpicordManager.instance.isSpicordBotAvailable) {
                    SpicordManager.instance.trySend(DiscordMessageType.DEATH, replacementMap)
                }
            }
            ChatManager.instance.trySend(Bukkit.getServer(), ChatManager.ChatMessageType.DEATH, replacementMap)
        }
    }

    /**
     * Send all players, that are not exempt a forced resource-pack, to reflect, that they are (essentially) in hardcore-
     * mode.
     * @param event The [PlayerJoinEvent] passed to the listener.
     */
    @EventHandler(ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!player.hasPermission("yolo.exempt") && yoloPluginInstance.isFunctionalityEnabled) {
            yoloPluginInstance.resourcePackManager?.applyPack(player)
        }
    }

    /**
     * Currently used for providing the capability to send a message upon totem use.
     * @param event The [EntityResurrectEvent] passed to the listener.
     */
    @EventHandler(ignoreCancelled = true)
    fun onEntityResurrected(event: EntityResurrectEvent) {
        if (event.entity.type != EntityType.PLAYER) return
        if (event.entity.hasPermission("yolo.exempt") || !yoloPluginInstance.isFunctionalityEnabled) return
        val player = event.entity as Player
        if (player.hasPermission("yolo.exempt")) return
        val replacementMap: HashMap<String?, String?> =
            TextReplacements.provideDefaults(player, TextReplacements.PLAYER_NAME)
        replacementMap[TextReplacements.TOTEM_USES.toString()] =
            (player.getStatistic(Statistic.USE_ITEM, Material.TOTEM_OF_UNDYING) + 1).toString()
        if (yoloPluginInstance.isUseSpicord) {
            SpicordManager.instance.trySend(DiscordMessageType.TOTEM, replacementMap)
        }
        ChatManager.instance.trySend(Bukkit.getServer(), ChatManager.ChatMessageType.TOTEM, replacementMap)
    }
}
