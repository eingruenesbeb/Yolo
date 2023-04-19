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

@file:UseSerializers(LocationSerializer::class)

package io.github.eingruenesbeb.yolo.managers

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import io.github.eingruenesbeb.yolo.TeleportationUtils.safeTeleport
import io.github.eingruenesbeb.yolo.TextReplacements
import io.github.eingruenesbeb.yolo.Yolo
import io.github.eingruenesbeb.yolo.managers.PlayerManager.PlayerStatus
import io.github.eingruenesbeb.yolo.managers.PlayerManager.YoloPlayer
import io.github.eingruenesbeb.yolo.serialize.ItemStackArrayPersistentDataType
import io.github.eingruenesbeb.yolo.serialize.LocationSerializer
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * This class is responsible for managing player data and actions related to death and revival.
 * It includes a nested class [YoloPlayer], which contains the player's UUID and their [PlayerStatus] data.
 *
 * @constructor Creates a new instance of `PlayerManager`. This is a private constructor since this class is a singleton.
 */
@OptIn(ExperimentalSerializationApi::class)
class PlayerManager private constructor() {
    @Serializable
    private data class YoloPlayerData(val data: Map<String, PlayerStatus>)
    @Serializable
    private data class PlayerStatus(
        var latestDeathPos: Location?,
        var isDead: Boolean = false,
        var isToRevive: Boolean = false,
        var isTeleportToDeathPos: Boolean = true,
        var isRestoreInventory: Boolean = true
    )

    private class YoloPlayer(
        val uuid: UUID,
        var playerStatus: PlayerStatus = PlayerStatus(null)
    ) {
        private val yolo = JavaPlugin.getPlugin(Yolo::class.java)
        private val reviveInventoryKey = NamespacedKey(yolo, "reviveInventory")

        fun setIsToReviveOnDead(toTrue: Boolean) {
            if (playerStatus.isDead && toTrue) {
                playerStatus.isToRevive = true
                // If this is set to true, the player has been unbanned and will be revived upon the next join.
            } else if (!toTrue) {
                // Disabling this flag should always go through, because it is generally safe, as nothing will happen.
                playerStatus.isDead = false
            }
        }

        fun saveReviveInventory() {
            with(Bukkit.getPlayer(uuid)) {
                this ?: return yolo.getLogger().warning(yolo.pluginResourceBundle.getString("player.saveInventory.offline"))
                val inventoryToSave = this.inventory
                this.persistentDataContainer.remove(reviveInventoryKey)
                this.persistentDataContainer.set(
                    reviveInventoryKey,
                    ItemStackArrayPersistentDataType(),
                    inventoryToSave.contents
                )
            }
        }

        private fun restoreReviveInventory() {
            with(Bukkit.getPlayer(uuid)) {
                this ?: return yolo.getLogger().warning(yolo.pluginResourceBundle.getString("player.revive.notOnline"))
                val restoredItemStacks = this.persistentDataContainer.get(
                        reviveInventoryKey,
                        ItemStackArrayPersistentDataType()
                ) as Array<ItemStack?>
                this.inventory.contents = restoredItemStacks
            }
        }

        fun revivePlayer() {
            with(Bukkit.getPlayer(uuid)) {
                this ?: return yolo.getLogger().warning(yolo.pluginResourceBundle.getString("player.revive.notOnline"))
                if (playerStatus.isDead && playerStatus.isToRevive) {
                    yolo.getLogger().info(yolo.pluginResourceBundle.getString("player.revive.attempt").replace("%player_name%", this.name))
                    this.gameMode = GameMode.SURVIVAL
                    if (playerStatus.isRestoreInventory) restoreReviveInventory()
                    if (playerStatus.isTeleportToDeathPos) playerStatus.latestDeathPos.runCatching {
                        if (!safeTeleport(this@with, this!!)) throw Exception("Player couldn't be teleported!")
                    }.onFailure {
                        yolo.getLogger().warning(yolo.pluginResourceBundle.getString("player.revive.invalidTeleport"))
                    }
                    playerStatus.isDead = false
                    playerStatus.isToRevive = false
                }
            }
        }
    }

    /**
     * A specialized [Listener] for this class. Handles [PlayerJoinEvent] and [PlayerPostRespawnEvent].
     */
    class PlayerManagerEvents : Listener {
        @EventHandler(ignoreCancelled = true)
        fun onPlayerPreLoginAsync(event: AsyncPlayerPreLoginEvent) {
            // Don't kick players, if the death-ban functionality is disabled.
            if (!JavaPlugin.getPlugin(Yolo::class.java).isFunctionalityEnabled) return

            // Pseudo-ban players, if they are dead:
            instance.playerRegistry[event.uniqueId]?.let { yoloPlayer ->
                if (yoloPlayer.playerStatus.isDead && !yoloPlayer.playerStatus.isToRevive) {
                    val componentReplacementMap: HashMap<String?, Component?> =
                        TextReplacements.provideComponentDefaults(event, TextReplacements.ALL)

                    var dynamicBanMessage = JavaPlugin.getPlugin(Yolo::class.java).banMessage
                    componentReplacementMap.forEach { replacement ->
                        replacement.key?.let {  key ->
                            replacement.value?.let {  value ->
                                dynamicBanMessage = dynamicBanMessage?.replaceText {
                                    it.matchLiteral(key)
                                    it.replacement(value)
                                }
                            }
                        }
                    }

                    // The ban message component is guaranteed to be set after the plugin has loaded.
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, dynamicBanMessage ?: JavaPlugin.getPlugin(Yolo::class.java).banMessage!!)
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        fun onPlayerJoin(event: PlayerJoinEvent) {
            instance.playerRegistry.putIfAbsent(event.player.uniqueId, YoloPlayer(event.player.uniqueId))

            // Players may have been set to be revived, after they have respawned, when they have respawned during the
            // plugin's death ban functionality being disabled.
            if (!event.player.isDead) { instance.playerRegistry[event.player.uniqueId]!!.revivePlayer() }
        }

        @EventHandler(ignoreCancelled = true)
        fun onPlayerPostRespawn(event: PlayerPostRespawnEvent) {
            // Players may be revived, even if death-ban functionality is disabled.
            Bukkit.getScheduler().runTaskLater(
                JavaPlugin.getPlugin(Yolo::class.java),
                Runnable { instance.playerRegistry[event.player.uniqueId]?.revivePlayer() },
                1
            )
        }
    }

    private val playerRegistry = HashMap<UUID, YoloPlayer>()
    private val yolo = JavaPlugin.getPlugin(Yolo::class.java)

    init {
        // The registration of all previously online players is paramount to providing and modifying them in case they
        // get revived. This step is fine during the initial load, but NOT TO BE REPEATED DURING A RELOAD of the plugin.
        val allPlayers = Bukkit.getOfflinePlayers()
        val dataFile = File(yolo.dataFolder.path.plus("/data/yolo_player_data.json"))
        var userData = YoloPlayerData(mapOf())

        dataFile.runCatching {
            userData = Json.decodeFromStream(this.inputStream())
        }.recoverCatching {
            if (it is SerializationException || it is IllegalArgumentException) {
                yolo.getLogger().severe(yolo.pluginResourceBundle.getString("player.load.corrupted"))
                throw Exception()
            }
            File(dataFile.parent.toString()).mkdirs()
            dataFile.createNewFile()
            userData = Json.decodeFromString(dataFile.readText())
        }.onFailure {
            yolo.getLogger().severe(yolo.pluginResourceBundle.getString("player.load.fail"))
        }

        Arrays.stream(allPlayers).forEach { offlinePlayer: OfflinePlayer ->
            val recoveredStatus = userData.data[offlinePlayer.uniqueId.toString()]
            playerRegistry[offlinePlayer.uniqueId] = recoveredStatus?.let { YoloPlayer(offlinePlayer.uniqueId, it) } ?: YoloPlayer(offlinePlayer.uniqueId)
        }
        // (There's also probably no need to reload this manager, as nothing is config dependent.)
    }

    /**
     * This method is used for setting a revivable (meaning that the player is dead and isn't already queued for
     * revival) up to be revived upon the next join.
     * The revival process is setting the player's gamemode to [GameMode.SURVIVAL], teleporting them to the location of
     * their death, if it's safe, and finally restoring their inventory.
     *
     * @param targetName The name of the player.
     * @param reviveOnJoin Whether to set `isToRevive` to true.
     * @param setIsRestoreInventory Whether the inventory should be restored (Default: `true`)
     * @param setIsTeleportToDeathPos Whether to teleport the player to their last death location (Default: `true`)
     *
     * @throws IllegalArgumentException If the player is not in the [playerRegistry], this exception is thrown.
     */
    @Throws(IllegalArgumentException::class)
    fun setReviveOnUser(targetName: String,
                        reviveOnJoin: Boolean,
                        setIsRestoreInventory: Boolean = true,
                        setIsTeleportToDeathPos: Boolean = true
    ) {
        val target = playerRegistry[Bukkit.getOfflinePlayer(targetName).uniqueId] ?: throw IllegalArgumentException("Player isn't in the registry!")
        target.setIsToReviveOnDead(reviveOnJoin)
        target.playerStatus.isRestoreInventory = setIsRestoreInventory
        target.playerStatus.isTeleportToDeathPos = setIsTeleportToDeathPos
    }

    /**
     * Provides a list of the names of every player, that is dead and isn't already set to be revived. Useful for
     * command tab-completions.
     *
     * @return A list of every revivable player.
     */
    fun provideRevivable(): List<String> {
        val listOfRevivable = ArrayList<String>()
        playerRegistry.forEach { (uuid: UUID?, yoloPlayer: YoloPlayer) ->
            val nameRetrieved = Bukkit.getOfflinePlayer(uuid).name
            if ( nameRetrieved != null && yoloPlayer.playerStatus.isDead && !yoloPlayer.playerStatus.isToRevive) {
                listOfRevivable.add(nameRetrieved)
            }
        }
        return listOfRevivable
    }

    /**
     * Teleports the player [toTeleport] to the location of the specified revivable player.
     *
     * @param toTeleport The player to teleport.
     * @param targetName The name of the player, that the death location belongs to.
     *
     * @return Whether the action was a success
     */
    fun teleportToRevivable(toTeleport: Player, targetName: String): Boolean {
        if (targetName == "") return false
        val targetLocation = instance.playerRegistry[Bukkit.getOfflinePlayer(targetName).uniqueId]?.playerStatus?.latestDeathPos
        targetLocation ?: return false
        targetLocation.chunk.load()
        return toTeleport.teleport(targetLocation)
    }

    /**
     * Executes everything, that needs to be, when a player has died.
     *
     * @param player The player, that has died
     */
    internal fun actionsOnDeath(player: Player) {
        val playerFromRegistry = playerRegistry[player.uniqueId]
        playerFromRegistry!!.playerStatus.isDead = true
        playerFromRegistry.saveReviveInventory()
        playerFromRegistry.playerStatus.latestDeathPos = player.location

        // This may be necessary, if the player was banned by this plugin directly. (See the comment in
        // YoloEventListener#onPlayerDeath)
        player.inventory.contents = arrayOf()
    }

    /**
     * Is to be called, when the plugin is disabled. Ensures, that every important bit of data is saved.
     */
    internal fun savePlayerData() {
        yolo.getLogger().info(yolo.pluginResourceBundle.getString("player.saveData.start"))
        val toSerializeMap = mutableMapOf<String, PlayerStatus>()
        playerRegistry.forEach { (uuid, yoloPlayer) ->
            toSerializeMap[uuid.toString()] = yoloPlayer.playerStatus
        }
        val toSerialize = YoloPlayerData(toSerializeMap.toMap())
        val dataFile = File(yolo.dataFolder.path.plus("/data/yolo_player_data.json"))
        dataFile.runCatching {
            val os = Files.newOutputStream(this.toPath(), StandardOpenOption.TRUNCATE_EXISTING)
            Json.encodeToStream(toSerialize, os)
            os.close()
        }.recoverCatching {
            if (it is FileNotFoundException) {
                File(dataFile.parent.toString()).mkdirs()
                dataFile.createNewFile()
                val os = Files.newOutputStream(dataFile.toPath(), StandardOpenOption.TRUNCATE_EXISTING)
                Json.encodeToStream(toSerialize, os)
                os.close()
            } else throw Exception()
        }.onFailure {
            yolo.getLogger().severe(yolo.pluginResourceBundle.getString("player.saveData.failure"))
        }.onSuccess {
            yolo.getLogger().info(yolo.pluginResourceBundle.getString("player.saveData.success"))
        }
    }

    companion object {
        val instance = PlayerManager()
    }
}
