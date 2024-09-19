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

@file:UseSerializers(NullableLocationKSerializer::class, MiniMessageKSerializer::class)

package io.github.eingruenesbeb.yolo.managers

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import io.github.eingruenesbeb.yolo.TextReplacements
import io.github.eingruenesbeb.yolo.Yolo
import io.github.eingruenesbeb.yolo.events.*
import io.github.eingruenesbeb.yolo.events.deathBan.PostDeathBanEvent
import io.github.eingruenesbeb.yolo.events.deathBan.PostDeathBanEventAsync
import io.github.eingruenesbeb.yolo.events.deathBan.PreDeathBanEvent
import io.github.eingruenesbeb.yolo.events.deathBan.PreDeathBanEventAsync
import io.github.eingruenesbeb.yolo.events.revive.YoloPlayerRevivedEvent
import io.github.eingruenesbeb.yolo.localizedMessageWithStackTrace
import io.github.eingruenesbeb.yolo.managers.spicord.DiscordMessageType
import io.github.eingruenesbeb.yolo.player.DeathBanResult
import io.github.eingruenesbeb.yolo.player.YoloPlayer
import io.github.eingruenesbeb.yolo.player.YoloPlayerData
import io.github.eingruenesbeb.yolo.serialize.LegacyPlayerDataMapKSerializer
import io.github.eingruenesbeb.yolo.serialize.MiniMessageKSerializer
import io.github.eingruenesbeb.yolo.serialize.NullableLocationKSerializer
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This object is responsible for managing player data and actions related to death and revival.
 * It includes a nested class [YoloPlayer], which contains the player's UUID and their [YoloPlayerData] data.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class PlayerManager {
    @VisibleForTesting
    internal val playerRegistry = PlayerRegistry()

    private val yolo
        get() = Yolo.pluginInstance!!

    init {
        yolo.logger.info { Yolo.pluginResourceBundle.getString("player.load.start") }  // TODO: remove

        val oldDataFile = File(yolo.dataFolder.absolutePath.plus("/data/yolo_player_data.json"))
        yolo.logger.info { "Looking for legacy data at \"$oldDataFile\"" }
        if (oldDataFile.exists() && oldDataFile.isFile) runCatching {
            convertLegacyData()
        }.onFailure {
            yolo.logger.severe {
                Yolo.pluginResourceBundle.getString("player.load.legacyConversion.fail")
            }
            throw it  // Throw to disable plugin
        }.onSuccess {
            yolo.logger.info {
                Yolo.pluginResourceBundle.getString("player.load.legacyConversion.success")
            }
        }

        yolo.logger.info { "Looking for player-data at \"${yolo.dataFolder.absolutePath.plus("/player_data")}\"" }  // TODO: remove
        val dataFilesWithUUID = runCatching {
            File(yolo.dataFolder.absolutePath.plus("/player_data")).listFiles { _, fileName ->
                // Data is stored in CBOR format.
                return@listFiles runCatching inner@ {
                    UUID.fromString(fileName.substringBeforeLast('.'))
                    return@inner fileName.substringAfterLast('.') == "cbor"
                }.getOrElse { false }
            }?.associateBy {
                UUID.fromString(it.nameWithoutExtension)
            } ?: throw NullPointerException("An IOException occurred, whilst trying to read data directory, or the directory is missing.")
        }.getOrElse {
            yolo.logger.severe { Yolo.pluginResourceBundle.getString("player.load.corruptedDir") }
            throw it
        }

        dataFilesWithUUID.forEach { (uuid, dataFile) ->
            runCatching {
                playerRegistry[uuid] = YoloPlayer(Cbor.decodeFromByteArray(dataFile.readBytes()))
            }.onFailure {
                yolo.logger.severe { Yolo.pluginResourceBundle.getString("player.load.corruptedFile").replace("", uuid.toString()) }
                throw it
            }
        }

        SingularPlayerAutoSave.enable()
        yolo.logger.info { Yolo.pluginResourceBundle.getString("player.load.success") }
    }

    /**
     * Provides a list of the names from every player, who is dead and isn't yet set to be revived.
     * Useful for command tab-completions.
     *
     * @return A list of every revivable player.
     */
    fun provideRevivable(): List<String> {
        val listOfRevivable = ArrayList<String>()
        playerRegistry.forEach { (uuid: UUID, yoloPlayer: YoloPlayer) ->
            val nameRetrieved = Bukkit.getOfflinePlayer(uuid).name
            if ( nameRetrieved != null && yoloPlayer.yoloPlayerData.isDead && !yoloPlayer.yoloPlayerData.isToRevive) {
                listOfRevivable.add(nameRetrieved)
            }
        }
        return listOfRevivable
    }

    /**
     * Provides a list of the names from every player, who has been revived.
     */
    fun provideRevived(): List<String> = playerRegistry.filter {
            it.value.yoloPlayerData.revives.isNotEmpty()
        }.map {
            Bukkit.getOfflinePlayer(it.key).name
        }.filterNotNull()

    internal fun undoRevive(targetName: String): Boolean {
        val offlinePlayer = Bukkit.getOfflinePlayer(targetName)
        val targetFromRegistry = playerRegistry[offlinePlayer.uniqueId]

        if (targetFromRegistry.yoloPlayerData.isToRevive) {
            targetFromRegistry.yoloPlayerData.isToRevive = false
            targetFromRegistry.yoloPlayerData.isDead = true
            return true
        } else if (targetFromRegistry.yoloPlayerData.revives.any { it.successful }) {
            if (offlinePlayer.player != null) {
                targetFromRegistry.undoLastReviveActive()
            } else {
                targetFromRegistry.yoloPlayerData.isUndoRevive = true  // Player isn't online. Undo revive later.
            }
            return true
        } else return false
    }

    /**
     * This method is used for setting a revivable player (meaning that the player is dead and isn't already queued for
     * revival) up to be revived upon the next join.
     * The revival process is setting the player's game mode to [GameMode.SURVIVAL], teleporting them to the location of
     * their death, if it's safe, and finally restoring their inventory.
     *
     * @param targetName The name of the player.
     * @param reviveOnJoin Whether to set `isToRevive` to true.
     * @param setIsRestoreInventory Whether the inventory should be restored (Default: `true`)
     * @param setIsTeleportToDeathPos Whether to teleport the player to their last death location (Default: `true`)
     *
     * @throws IllegalStateException When the player has not been set to be revived.
     */
    @Throws(IllegalStateException::class)
    internal fun setReviveOnUser(
        targetName: String,
        reviveOnJoin: Boolean,
        setIsRestoreInventory: Boolean = true,
        setIsTeleportToDeathPos: Boolean = true
    ) {
        val target = playerRegistry[Bukkit.getOfflinePlayer(targetName).uniqueId]
        val successful = target.setIsToReviveOnDead(reviveOnJoin)
        if (!successful) throw IllegalStateException("Player has not been set to be revived!")
        target.yoloPlayerData.isRestoreInventory = setIsRestoreInventory
        target.yoloPlayerData.isTeleportToDeathPos = setIsTeleportToDeathPos
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
        val targetLocation = playerRegistry[Bukkit.getOfflinePlayer(targetName).uniqueId].yoloPlayerData.latestDeathPos
        targetLocation ?: return false
        targetLocation.chunk.load()
        // No need to check for safety, as the teleported player is probably an admin.
        return toTeleport.teleport(targetLocation)
    }

    /**
     * Executes everything that needs to be, when a player has died.
     *
     * @param  deathEvent The death event.
     */
    internal fun actionsOnDeath(deathEvent: PlayerDeathEvent) {
        if (deathEvent.player.hasPermission("yolo.exempt") || yolo.isFunctionalityEnabled) return

        val playerFromRegistry = playerRegistry[deathEvent.player.uniqueId]
        val stringReplacementMap: HashMap<String, String?> = TextReplacements.provideStringDefaults(deathEvent, TextReplacements.ALL)
        val componentReplacementMap: HashMap<String, Component?> = TextReplacements.provideComponentDefaults(deathEvent, TextReplacements.ALL)
        var dynamicBanMessage = Yolo.pluginInstance!!.banMessage
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

        playerFromRegistry.yoloPlayerData.isDead = true
        playerFromRegistry.yoloPlayerData.latestDeathPos = deathEvent.player.location
        playerFromRegistry.yoloPlayerData.banMessage = dynamicBanMessage ?: Component.text("[<red>Yolo</red>] » You have died and therefore can no longer play on this hardcore server. :(")

        // Emit pre-death-ban events.
        val preEvent = PreDeathBanEvent(
            playerFromRegistry.yoloPlayerData.copy(),
            DeathBanResult(true, deathEvent.player.location, playerFromRegistry.yoloPlayerData.banMessage),
            deathEvent
        )
        preEvent.callEvent()

        with(preEvent.targetResult) {
            playerFromRegistry.yoloPlayerData.isDead = this.successful
            playerFromRegistry.yoloPlayerData.latestDeathPos = this.latestDeathPos
            playerFromRegistry.yoloPlayerData.banMessage = this.banMessage
        }

        object : BukkitRunnable() {
            override fun run() {
                PreDeathBanEventAsync(
                    playerFromRegistry.yoloPlayerData.copy(),
                    preEvent.targetResult,
                    preEvent.originalTargetResult,
                    deathEvent
                ).callEvent()
            }
        }.runTaskAsynchronously(yolo)

        playerFromRegistry.saveReviveInventory()

        // This may be necessary if the player was banned by this plugin directly.
        deathEvent.player.inventory.contents = arrayOf()

        val postEventAsync = object : BukkitRunnable() {
            override fun run() {
                PostDeathBanEventAsync(
                    playerFromRegistry.yoloPlayerData,
                    preEvent.targetResult,
                    deathEvent
                ).callEvent()
            }
        }

        // Finally, ban the player from the pseudo-ban server. (deferred)
        object : BukkitRunnable() {
            override fun run() {
                if (playerFromRegistry.yoloPlayerData.isDead) {
                    pseudoBanPlayer(deathEvent.player.uniqueId, null)

                    // It's about sending a message.
                    if (yolo.spicordManager?.isSpicordBotAvailable == true) {
                        yolo.spicordManager?.trySend(DiscordMessageType.DEATH, stringReplacementMap)
                    }
                    yolo.chatManager.trySend(Bukkit.getServer(), ChatManager.ChatMessageType.DEATH, componentReplacementMap)
                }

                postEventAsync.runTaskAsynchronously(yolo)
                PostDeathBanEvent(
                    playerFromRegistry.yoloPlayerData,
                    preEvent.targetResult,
                    deathEvent
                ).callEvent()
            }
        }.runTaskLater(yolo, 1)
    }

    /**
     * Is to be called when the plugin is disabled. Ensures that every important bit of data is saved.
     */
    internal fun saveAllPlayerData() {
        yolo.logger.info { Yolo.pluginResourceBundle.getString("player.saveData.start") }
        SingularPlayerAutoSave.cancelAndDisable()
        playerRegistry.values.forEach { savePlayerData(it.yoloPlayerData) }
        yolo.logger.info { Yolo.pluginResourceBundle.getString("player.saveData.done") }
    }

    internal fun onPlayerJoin(event: PlayerJoinEvent) {
        val playerInRegistry = playerRegistry[event.player.uniqueId]  // Automatically registers player.

        if (playerInRegistry.yoloPlayerData.isUndoRevive) {
            playerInRegistry.undoLastReviveActive()
            pseudoBanPlayer(playerInRegistry.yoloPlayerData.uuid, null)
        }

        // Players may have been set to be revived, after they have respawned, when they have respawned during the
        // plugin's death-ban functionality being disabled.
        if (!event.player.isDead && playerInRegistry.yoloPlayerData.isToRevive) {
            playerInRegistry.revivePlayer()
        }

        playerInRegistry.yoloPlayerData.ghostState.reinstateTicker()
    }

    internal fun onPlayerPreLoginAsync(event: AsyncPlayerPreLoginEvent) {
        // Don't kick players if the death-ban functionality is disabled.
        if (!Yolo.pluginInstance!!.isFunctionalityEnabled) return

        // Pseudo-ban players, if they are dead:
        val target = playerRegistry[event.uniqueId]
        if (target.yoloPlayerData.isDead && !target.yoloPlayerData.isToRevive) pseudoBanPlayer(event.uniqueId, event)
    }

    internal fun onPlayerQuit(event: PlayerQuitEvent) {
        playerRegistry[event.player.uniqueId].yoloPlayerData.ghostState.stopTicker()
    }

    internal fun onPlayerRespawned(event: PlayerPostRespawnEvent) {
        // Players may be revived, even if death-ban functionality is disabled.
        Bukkit.getScheduler().runTaskLater(
            Yolo.pluginInstance!!,
            Runnable {
                val playerInRegistry = playerRegistry[event.player.uniqueId]
                if (playerInRegistry.yoloPlayerData.isToRevive) {
                    playerRegistry[event.player.uniqueId].revivePlayer()
                }
            },
            1
        )
    }

    internal fun onPlayerAttack(event: EntityDamageByEntityEvent) {
        if (event.damager is Player) {
            event.isCancelled = playerRegistry[event.damager.uniqueId].yoloPlayerData.ghostState.enabled
            if (!event.isCancelled) return
            playerRegistry[event.damager.uniqueId].yoloPlayerData.ghostState.remove()
            if (event.entity is EnderCrystal) event.entity.remove()
        }
    }

    internal fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        val pvpItemMaterials = listOf(
            Material.FLINT_AND_STEEL,
            Material.BOW,
            Material.CROSSBOW,
            Material.SPLASH_POTION,
            Material.LINGERING_POTION,
            Material.LAVA_BUCKET,
            Material.FIREWORK_ROCKET,
            Material.FIRE_CHARGE,
            Material.FISHING_ROD
        )

        if (!event.hasItem()) return
        if (event.item!!.type in pvpItemMaterials) {
            event.isCancelled = playerRegistry[event.player.uniqueId].yoloPlayerData.ghostState.enabled
            if (event.useItemInHand() != Event.Result.DENY) return
            playerRegistry[event.player.uniqueId].yoloPlayerData.ghostState.remove()
            event.player.setCooldown(event.material, 20)
        }
    }

    internal fun onYoloPlayerRevived(event: YoloPlayerRevivedEvent) {
        if (!event.finalResult.successful) pseudoBanPlayer(event.offlinePlayer.uniqueId, null)
    }

    private fun savePlayerData(yoloPlayerData: YoloPlayerData) {
        runCatching {
            File(
                yolo.dataFolder.absolutePath.plus("/player_data"),
                "${yoloPlayerData.uuid}.cbor"
            ).writeBytes(Cbor.encodeToByteArray(yoloPlayerData))
        }.onFailure {
            yolo.logger.severe {
                Yolo.pluginResourceBundle.getString("player.saveData.failure")
                    .replace("%uuid%", "$yoloPlayerData")
                    .replace("%error%", it.localizedMessageWithStackTrace())
            }
        }
    }

    private fun convertLegacyData() {
        val oldDataPath = yolo.dataFolder.absolutePath.plus("/data/yolo_player_data.json")
        val oldData = Json.decodeFromStream(LegacyPlayerDataMapKSerializer, File(oldDataPath).inputStream())
        oldData.forEach { (uuid, yoloPlayer) ->
            playerRegistry[uuid] = yoloPlayer
        }
        saveAllPlayerData()

        File(oldDataPath).delete()
        if (File(oldDataPath).parentFile.listFiles()?.isEmpty() == true) File(oldDataPath).parentFile.deleteRecursively()
    }

    /**
     * Pseudo-ban players, if they're dead and aren't queued to be revived.
     *
     *
     * Only "Pseudo"-ban, because the player isn't officially put on the ban-list and thus is only banned as long as the
     * plugin is enabled.
     *
     * @param playerUUID The uuid of the player
     * @param preLoginEvent The corresponding event, if this method is called upon a player attempting to join.
     */
    private fun pseudoBanPlayer(playerUUID: UUID, preLoginEvent: AsyncPlayerPreLoginEvent?) {
        playerRegistry[playerUUID].let {
            val message = it.yoloPlayerData.banMessage
            preLoginEvent?.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message).also {
                yolo.logger.info { Yolo.pluginResourceBundle.getString("player.deathBan.onJoin").replace("%uuid%", "$playerUUID") }
            } ?: Bukkit.getServer().getPlayer(playerUUID)?.kick(message, PlayerKickEvent.Cause.BANNED).also {
                yolo.logger.info { Yolo.pluginResourceBundle.getString("player.deathBan.kick").replace("%uuid%, ", "$playerUUID") }
            }
        }
    }

    private object SingularPlayerAutoSave : BukkitRunnable() {
        private val queue = ConcurrentLinkedQueue<YoloPlayerData>()
        private val isRunning = AtomicBoolean(false)
        private val isEnabled = AtomicBoolean(false)
        private var task: BukkitTask? = null

        init {
            task = runTaskAsynchronously(Yolo.pluginInstance!!)
        }

        override fun run() {
            isRunning.set(true)
            while (!isCancelled && isEnabled.get()) {
                queue.poll()?.let { Yolo.pluginInstance!!.playerManager.savePlayerData(it) }
            }
            isRunning.set(false)
        }

        fun requestSave(data: YoloPlayerData) {
            if (isEnabled.get()) queue.offer(data)
        }

        fun enable() {
            isEnabled.set(true)
        }

        fun cancelAndDisable() {
            task?.cancel()
            while (isRunning.get()) { /* Wait until no data is written by this runnable to avoid concurrent writes. */ }
            isEnabled.set(false)
        }
    }

    @VisibleForTesting
    internal class PlayerRegistry : HashMap<UUID, YoloPlayer>() {
        private fun readResolve(): Any = this

        override fun get(key: UUID): YoloPlayer {
            return super.get(key) ?: YoloPlayer(YoloPlayerData(key, null)).also {
                this[key] = it
            }
        }

        override fun put(key: UUID, value: YoloPlayer): YoloPlayer? {
            val previousValue = super.put(key, value)
            SingularPlayerAutoSave.requestSave(value.yoloPlayerData)
            return previousValue
        }

        override fun putAll(from: Map<out UUID, YoloPlayer>) {
            super.putAll(from)
            from.values.forEach { SingularPlayerAutoSave.requestSave(it.yoloPlayerData) }
        }
    }
}
