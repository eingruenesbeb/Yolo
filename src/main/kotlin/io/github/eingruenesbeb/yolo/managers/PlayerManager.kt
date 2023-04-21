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

@file:UseSerializers(LocationSerializer::class, MiniMessageSerializer::class)

package io.github.eingruenesbeb.yolo.managers

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import io.github.eingruenesbeb.yolo.TeleportationUtils.safeTeleport
import io.github.eingruenesbeb.yolo.TextReplacements
import io.github.eingruenesbeb.yolo.Yolo
import io.github.eingruenesbeb.yolo.events.PreYoloPlayerReviveEvent
import io.github.eingruenesbeb.yolo.events.PreYoloPlayerReviveEventAsync
import io.github.eingruenesbeb.yolo.events.YoloPlayerRevivedEvent
import io.github.eingruenesbeb.yolo.events.YoloPlayerRevivedEventAsync
import io.github.eingruenesbeb.yolo.managers.PlayerManager.PlayerStatus
import io.github.eingruenesbeb.yolo.managers.PlayerManager.YoloPlayer
import io.github.eingruenesbeb.yolo.serialize.ItemStackArrayPersistentDataType
import io.github.eingruenesbeb.yolo.serialize.LocationSerializer
import io.github.eingruenesbeb.yolo.serialize.MiniMessageSerializer
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
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
object PlayerManager {
    @Serializable
    data class PlayerStatus(
        var latestDeathPos: Location?,
        var isDead: Boolean = false,
        var isToRevive: Boolean = false,
        var isTeleportToDeathPos: Boolean = true,
        var isRestoreInventory: Boolean = true,
        var banMessage: Component = Component.text("")
    )

    /**
     * Represents the result of a revival attempt. If the attempt lies in the future or in the past, is
     * context-dependent.
     *
     * @property successful Whether the attempt should be or was successful.
     * @property teleported Whether the player should be or was teleported to their last death location.
     * @property inventoryRestored Whether the inventory should be or was restored.
     */
    data class ReviveResult(
        val successful: Boolean = false,
        val teleported: Boolean = false,
        val inventoryRestored: Boolean = false
    ) {
        override fun toString(): String {
            return "[Successful: $successful, Teleport: $teleported, Restored inventory: $inventoryRestored]"
        }
    }

    internal class PlayerManagerEvents : Listener {
        @EventHandler(ignoreCancelled = true)
        fun onPlayerPreLoginAsync(event: AsyncPlayerPreLoginEvent) {
            // Don't kick players, if the death-ban functionality is disabled.
            if (!JavaPlugin.getPlugin(Yolo::class.java).isFunctionalityEnabled) return

            // Pseudo-ban players, if they are dead:
            playerRegistry[event.uniqueId]?.let {
                if (it.playerStatus.isDead && !it.playerStatus.isToRevive) pseudoBanPlayer(event.uniqueId, event)
            }
        }

        @EventHandler(ignoreCancelled = true)
        fun onPlayerJoin(event: PlayerJoinEvent) {
            playerRegistry.putIfAbsent(event.player.uniqueId, YoloPlayer(event.player.uniqueId))

            // Players may have been set to be revived, after they have respawned, when they have respawned during the
            // plugin's death ban functionality being disabled.
            if (!event.player.isDead) { playerRegistry[event.player.uniqueId]!!.revivePlayer() }
        }

        @EventHandler(ignoreCancelled = true)
        fun onPlayerPostRespawn(event: PlayerPostRespawnEvent) {
            // Players may be revived, even if death-ban functionality is disabled.
            Bukkit.getScheduler().runTaskLater(
                JavaPlugin.getPlugin(Yolo::class.java),
                Runnable { playerRegistry[event.player.uniqueId]?.revivePlayer() },
                1
            )
        }
    }

    private class YoloPlayer(
        private val uuid: UUID,
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
                this ?: return yolo.getLogger().warning(Yolo.pluginResourceBundle.getString("player.saveInventory.offline"))
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
                this ?: return yolo.getLogger().warning(Yolo.pluginResourceBundle.getString("player.revive.notOnline"))
                val restoredItemStacks = this.persistentDataContainer.get(
                    reviveInventoryKey,
                    ItemStackArrayPersistentDataType()
                ) as Array<ItemStack?>
                this.inventory.contents = restoredItemStacks
            }
        }

        fun revivePlayer() {
            yolo.getLogger().info(Yolo.pluginResourceBundle.getString("player.revive.attempt").replace("%player_uuid%", uuid.toString()))
            // Dispatch pre-revive Events:
            val originalOutcome = ReviveResult(playerStatus.isToRevive, playerStatus.isTeleportToDeathPos, playerStatus.isRestoreInventory)
            // Let the synchronous event take precedence, as it could modify the target result (and the asynchronous one
            // not).
            val preEvent = PreYoloPlayerReviveEvent(uuid to playerStatus.copy(), originalOutcome)
            preEvent.callEvent()
            // The event should have an influence on the revive-process, but not on the originally set values.
            val tempStatus: PlayerStatus
            with(preEvent.targetOutcome) {
                tempStatus = playerStatus.copy(
                    isToRevive = this.successful,
                    isTeleportToDeathPos = this.teleported,
                    isRestoreInventory = this.inventoryRestored
                )
            }
            // The async variant of the event is only for reactionary measures:
            object : BukkitRunnable() {
                override fun run() {
                    PreYoloPlayerReviveEventAsync(uuid to playerStatus.copy(), originalOutcome, preEvent.targetOutcome).callEvent()
                }
            }.runTaskAsynchronously(yolo)

            var isSuccess = false
            var isTeleported = false
            var isWithInventory = false
            with(Bukkit.getPlayer(uuid)) {
                this ?: return yolo.getLogger().warning(Yolo.pluginResourceBundle.getString("player.revive.notOnline"))  // The player will still be revivable, if the process is stopped here.
                if (playerStatus.isDead && tempStatus.isToRevive) {
                    this.gameMode = GameMode.SURVIVAL
                    if (tempStatus.isRestoreInventory) restoreReviveInventory().also { isWithInventory = true }
                    if (tempStatus.isTeleportToDeathPos) playerStatus.latestDeathPos.runCatching {
                        isTeleported = safeTeleport(this@with, this!!)
                        if (!isTeleported) throw Exception("Player couldn't be teleported!")
                    }.onFailure {
                        yolo.getLogger().warning(Yolo.pluginResourceBundle.getString("player.revive.invalidTeleport"))
                    }

                    // If we got here, that means, that the process was successful. Therefore, set the death and revive
                    // status to false, as to avoid accidentally reviving a player twice, without them having died in
                    // the meantime.
                    playerStatus.isDead = false
                    playerStatus.isToRevive = false
                    isSuccess = true
                }
            }
            val finalResult = ReviveResult(isSuccess, isTeleported, isWithInventory)

            // Dispatch post revive events:
            object : BukkitRunnable() {
                override fun run() {
                    YoloPlayerRevivedEventAsync(uuid to playerStatus.copy(), finalResult).callEvent()
                }
            }.runTaskAsynchronously(yolo)
            YoloPlayerRevivedEvent(uuid to playerStatus.copy(), finalResult).callEvent()
        }
    }

    @Serializable
    private data class YoloPlayerData(val data: Map<String, PlayerStatus>)

    /**
     * A specialized [Listener] for this class. Handles [PlayerJoinEvent] and [PlayerPostRespawnEvent].
     */

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
                yolo.getLogger().severe(Yolo.pluginResourceBundle.getString("player.load.corrupted"))
                throw Exception()
            }
            File(dataFile.parent.toString()).mkdirs()
            dataFile.createNewFile()
            userData = Json.decodeFromString(dataFile.readText())
        }.onFailure {
            yolo.getLogger().severe(Yolo.pluginResourceBundle.getString("player.load.fail"))
        }

        Arrays.stream(allPlayers).forEach { offlinePlayer: OfflinePlayer ->
            val recoveredStatus = userData.data[offlinePlayer.uniqueId.toString()]
            playerRegistry[offlinePlayer.uniqueId] = recoveredStatus?.let { YoloPlayer(offlinePlayer.uniqueId, it) } ?: YoloPlayer(offlinePlayer.uniqueId)
        }
        // (There's also probably no need to reload this manager, as nothing is config dependent.)
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
     * This method is used for setting a revivable (meaning that the player is dead and isn't already queued for
     * revival) up to be revived upon the next join.
     * The revival process is setting the player's game mode to [GameMode.SURVIVAL], teleporting them to the location of
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
    internal fun setReviveOnUser(
        targetName: String,
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
     * Teleports the player [toTeleport] to the location of the specified revivable player.
     *
     * @param toTeleport The player to teleport.
     * @param targetName The name of the player, that the death location belongs to.
     *
     * @return Whether the action was a success
     */
    internal fun teleportToRevivable(toTeleport: Player, targetName: String): Boolean {
        if (targetName == "") return false
        val targetLocation = playerRegistry[Bukkit.getOfflinePlayer(targetName).uniqueId]?.playerStatus?.latestDeathPos
        targetLocation ?: return false
        targetLocation.chunk.load()
        // No need to check for safety, as the teleported player is probably an admin.
        return toTeleport.teleport(targetLocation)
    }

    /**
     * Executes everything, that needs to be, when a player has died.
     *
     * @param  deathEvent The death event.
     */
    internal fun actionsOnDeath(deathEvent: PlayerDeathEvent) {
        val playerFromRegistry = playerRegistry[deathEvent.player.uniqueId] ?: YoloPlayer(deathEvent.player.uniqueId).also {
            playerRegistry[deathEvent.player.uniqueId] = it
        }
        playerFromRegistry.playerStatus.isDead = true
        playerFromRegistry.saveReviveInventory()
        playerFromRegistry.playerStatus.latestDeathPos = deathEvent.player.location

        val componentReplacementMap: HashMap<String, Component?> = TextReplacements.provideComponentDefaults(deathEvent, TextReplacements.ALL)

        var dynamicBanMessage = JavaPlugin.getPlugin(Yolo::class.java).banMessage
        componentReplacementMap.forEach { replacement ->
            replacement.key.let { key ->
                replacement.value?.let {  value ->
                    dynamicBanMessage = dynamicBanMessage?.replaceText {
                        it.matchLiteral(key)
                        it.replacement(value)
                    }
                }
            }
        }

        playerFromRegistry.playerStatus.banMessage = dynamicBanMessage ?: Component.text("[<red>Yolo</red>] » You have died and therefore can no longer play on this hardcore server. :(")

        // This may be necessary, if the player was banned by this plugin directly. (See the comment in
        // YoloEventListener#onPlayerDeath)
        deathEvent.player.inventory.contents = arrayOf()
    }

    /**
     * Pseudo-ban players, if they are dead and aren't queued to be revived.
     *
     *
     * Only "Pseudo-"ban, because the player isn't officially put on the ban-list and thus is only banned as long as the
     * plugin is enabled.
     *
     * @param playerUUID The uuid of the player
     * @param preLoginEvent The corresponding event, if this method is called upon a player attempting to join.
     */
    internal fun pseudoBanPlayer(playerUUID: UUID, preLoginEvent: AsyncPlayerPreLoginEvent?) {
        playerRegistry[playerUUID]?.let {
            val message = it.playerStatus.banMessage
            preLoginEvent?.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message) ?: Bukkit.getServer().getPlayer(playerUUID)?.kick(message, PlayerKickEvent.Cause.BANNED)
        }
    }

    /**
     * Is to be called, when the plugin is disabled. Ensures, that every important bit of data is saved.
     */
    internal fun savePlayerData() {
        yolo.getLogger().info(Yolo.pluginResourceBundle.getString("player.saveData.start"))
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
            yolo.getLogger().severe(Yolo.pluginResourceBundle.getString("player.saveData.failure"))
        }.onSuccess {
            yolo.getLogger().info(Yolo.pluginResourceBundle.getString("player.saveData.success"))
        }
    }
}
