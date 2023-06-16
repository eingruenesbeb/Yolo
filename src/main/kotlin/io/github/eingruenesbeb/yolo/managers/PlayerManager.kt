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
import io.github.eingruenesbeb.yolo.events.*
import io.github.eingruenesbeb.yolo.events.deathBan.PostDeathBanEvent
import io.github.eingruenesbeb.yolo.events.deathBan.PostDeathBanEventAsync
import io.github.eingruenesbeb.yolo.events.deathBan.PreDeathBanEvent
import io.github.eingruenesbeb.yolo.events.deathBan.PreDeathBanEventAsync
import io.github.eingruenesbeb.yolo.events.revive.PreYoloPlayerReviveEvent
import io.github.eingruenesbeb.yolo.events.revive.PreYoloPlayerReviveEventAsync
import io.github.eingruenesbeb.yolo.events.revive.YoloPlayerRevivedEvent
import io.github.eingruenesbeb.yolo.events.revive.YoloPlayerRevivedEventAsync
import io.github.eingruenesbeb.yolo.managers.PlayerManager.PlayerStatus
import io.github.eingruenesbeb.yolo.managers.PlayerManager.YoloPlayer
import io.github.eingruenesbeb.yolo.managers.spicord.DiscordMessageType
import io.github.eingruenesbeb.yolo.managers.spicord.safeSpicordManager
import io.github.eingruenesbeb.yolo.serialize.ItemStackArrayPersistentDataType
import io.github.eingruenesbeb.yolo.serialize.LocationSerializer
import io.github.eingruenesbeb.yolo.serialize.MiniMessageSerializer
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.annotations.NotNull
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * This object is responsible for managing player data and actions related to death and revival.
 * It includes a nested class [YoloPlayer], which contains the player's UUID and their [PlayerStatus] data.
 */
@OptIn(ExperimentalSerializationApi::class)
object PlayerManager {

    /**
     * This data class represents the status values, tracked by this plugin.
     *
     *
     * It can be used externally to get information about a player, that is the subject of a
     * [io.github.eingruenesbeb.yolo.events.YoloPlayerEvent].
     *
     * @property latestDeathPos The location of the last death.
     * @property isDead Whether the player is considered to be dead by the plugin.
     * @property isToRevive Whether the player should be revived by the plugin.
     * @property isTeleportToDeathPos Whether the player should be teleported to the [latestDeathPos] upon their revival.
     * @property isRestoreInventory Whether the inventory should be restored upon revival.
     * @property banMessage The ban message, that will be shown to the player upon being death-banned.
     */
    @Serializable
    data class PlayerStatus(
        var latestDeathPos: Location?,
        var isDead: Boolean = false,
        var isToRevive: Boolean = false,
        var isTeleportToDeathPos: Boolean = true,
        var isRestoreInventory: Boolean = true,
        var banMessage: Component = Component.text(""),
        internal val ghostState: GhostState = GhostState(false, 0)
    )

    /**
     * Represents the result of a death-ban.
     * If the attempt lies in the future or in the past, it is context-dependent.
     *
     * @property successful Whether the outcome was successful.
     * @property latestDeathPos The stored death-position
     * @property banMessage The message shown to the banned player
     */
    data class DeathBanResult(
        var successful: Boolean,
        var latestDeathPos: Location?,
        var banMessage: Component
    )

    /**
     * Represents the result of a revival attempt.
     * If the attempt lies in the future or in the past, it is context-dependent.
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

    /**
     * Represents an additional state, a player can be in, when teleported upon their revival. Whilst in this state,
     * a player can't be harmed and is invisible, but can't engage in either PvP or PvE, without being forced out of
     * this state. The last part is handled by the [PlayerManagerEvents] event-listener.
     *
     *
     * It Is visible because it's a part of a player's data, which is public. However, all its members are either
     * private or internal.
     *
     * @property enabled Whether the player is in this ghost-like state
     * @property ticksLeft How long the player will be in this state if it's not removed prematurely.
     * @property attachedPlayerID The [UUID] of the corresponding player. **MUSTN'T BE `null`!**
     */
    @Internal
    @Serializable
    data class GhostState internal constructor(
        internal var enabled: Boolean = false,
        private var ticksLeft: Long = 0,
        @Transient @NotNull private val attachedPlayerID: UUID? = null
    ) {
        private class Ticker(val state:GhostState) : BukkitRunnable() {
            var remainingTicks = state.ticksLeft
            override fun run() {
                // Cancel the task for offline players:
                if (Bukkit.getPlayer(state.attachedPlayerID!!) == null) {
                    stop()
                    return
                }

                if (!state.enabled) {
                    remainingTicks = 0
                    state.ticksLeft = 0
                }

                if (remainingTicks < 1) {
                    state.remove()
                    return
                }
                remainingTicks -= 1
                state.ticksLeft = remainingTicks
            }

            fun stop() {
                cancel()
                state.ticker = Ticker(state)  // Once cancelled, a BukkitRunnable cannot be reused.
            }
        }

        @Transient
        private var ticker = Ticker(this)

        internal fun apply() {
            val playerObject = Bukkit.getPlayer(attachedPlayerID!!) ?: return
            playerObject.isInvulnerable = true
            playerObject.isInvisible = true
            enabled = true
            if (ticksLeft < 1) ticker.remainingTicks = 600
            reinstateTicker()
        }

        internal fun remove() {
            val playerObject = Bukkit.getPlayer(attachedPlayerID!!) ?: return yolo.logger.warning {
                Yolo.pluginResourceBundle.getString(
                    "player.removeGhostState.notOnline"
                ).replace("%uuid%", attachedPlayerID.toString())
            }
            playerObject.isInvulnerable = false
            playerObject.isInvisible = false
            enabled = false
            ticker.stop()
        }

        internal fun reinstateTicker() {
            Bukkit.getPlayer(attachedPlayerID!!) ?: return
            runCatching{
                ticker.runTaskTimer(yolo, 0, 1)
            }.onFailure {
                if (it is IllegalStateException) ticker.stop()  // Ticker is still running.
                ticker.runTaskTimer(yolo, 0, 1)
            }
        }

        internal fun stopTicker() {
            ticker.runCatching {
                this.stop()
            }
        }
    }

    internal class PlayerManagerEvents : Listener {
        @EventHandler(ignoreCancelled = true)
        fun onPlayerPreLoginAsync(event: AsyncPlayerPreLoginEvent) {
            // Don't kick players if the death-ban functionality is disabled.
            if (!JavaPlugin.getPlugin(Yolo::class.java).isFunctionalityEnabled) return

            // Pseudo-ban players, if they are dead:
            PlayerRegistry[event.uniqueId].let {
                if (it.playerStatus.isDead && !it.playerStatus.isToRevive) pseudoBanPlayer(event.uniqueId, event)
            }
        }

        @EventHandler(ignoreCancelled = true)
        fun onPlayerJoin(event: PlayerJoinEvent) {
            val playerInRegistry = PlayerRegistry[event.player.uniqueId]

            // Players may have been set to be revived, after they have respawned, when they have respawned during the
            // plugin's death ban functionality being disabled.
            if (!event.player.isDead) { playerInRegistry.revivePlayer() }

            playerInRegistry.playerStatus.ghostState.reinstateTicker()
        }

        @EventHandler(ignoreCancelled = true)
        fun onPlayerQuit(event: PlayerQuitEvent) {
            PlayerRegistry[event.player.uniqueId].playerStatus.ghostState.stopTicker()
        }

        @EventHandler(ignoreCancelled = true)
        fun onPlayerPostRespawn(event: PlayerPostRespawnEvent) {
            // Players may be revived, even if death-ban functionality is disabled.
            Bukkit.getScheduler().runTaskLater(
                JavaPlugin.getPlugin(Yolo::class.java),
                Runnable { PlayerRegistry[event.player.uniqueId].revivePlayer() },
                1
            )
        }

        // Check potentially offensive actions by a recently revived player and force them out of their ghost-like
        // state.
        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
        fun onPlayerAttack(event: EntityDamageByEntityEvent) {  // For the plain old *BONK* on the head
            if (event.damager is Player) {
                event.isCancelled = PlayerRegistry[event.damager.uniqueId].playerStatus.ghostState.enabled
                if (!event.isCancelled) return
                PlayerRegistry[event.damager.uniqueId].playerStatus.ghostState.remove()
                if (event.entity is EnderCrystal) event.entity.remove()
            }
        }

        @EventHandler(priority = EventPriority.LOW)
        fun onPlayerInteract(event: PlayerInteractEvent) {
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
                event.isCancelled = PlayerRegistry[event.player.uniqueId].playerStatus.ghostState.enabled
                if (event.useItemInHand() != Event.Result.DENY) return
                PlayerRegistry[event.player.uniqueId].playerStatus.ghostState.remove()
                event.player.setCooldown(event.material, 20)
            }
        }
    }

    private class YoloPlayer(
        private val uuid: UUID,
        var playerStatus: PlayerStatus = PlayerStatus(null, ghostState = GhostState(attachedPlayerID = uuid))
    ) {
        private val yolo = JavaPlugin.getPlugin(Yolo::class.java)
        private val reviveInventoryKey = NamespacedKey(yolo, "reviveInventory")

        fun setIsToReviveOnDead(toTrue: Boolean): Boolean {
            return if (playerStatus.isDead && toTrue) {
                playerStatus.isToRevive = true
                // If this is set to true, the player has been unbanned and will be revived upon the next join.
                true
            } else {
                // Disabling this flag should always go through, because it is generally safe, as nothing will happen.
                playerStatus.isDead = false
                false
            }
        }

        fun saveReviveInventory() {
            with(Bukkit.getPlayer(uuid)) {
                this ?: return yolo.logger.warning { Yolo.pluginResourceBundle.getString("player.saveInventory.offline") }
                val inventoryToSave = this.inventory
                this.persistentDataContainer.remove(reviveInventoryKey)
                this.persistentDataContainer.set(
                    reviveInventoryKey,
                    ItemStackArrayPersistentDataType(),
                    inventoryToSave.contents
                )
            }
        }

        fun revivePlayer() {
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
                if (this.successful) yolo.logger.info {
                    Yolo.pluginResourceBundle.getString("player.revive.attempt")
                        .replace("%player_uuid%", uuid.toString())
                }
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
                this ?: return yolo.logger.warning { Yolo.pluginResourceBundle.getString("player.revive.notOnline") }  // The player will still be revivable
                // if the process is stopped here.
                if (playerStatus.isDead && tempStatus.isToRevive) {
                    this.gameMode = GameMode.SURVIVAL
                    if (tempStatus.isRestoreInventory) restoreReviveInventory().also { isWithInventory = true }
                    if (tempStatus.isTeleportToDeathPos) playerStatus.latestDeathPos.runCatching {
                        isTeleported = safeTeleport(this@with, this!!)
                        if (isTeleported) {
                            playerStatus.ghostState.apply()  // Auto-removes after 600 ticks
                        }  // Effects get applied by the teleport function.
                        if (!isTeleported) throw Exception("Player couldn't be teleported!")
                    }.onFailure {
                        yolo.logger.warning { Yolo.pluginResourceBundle.getString("player.revive.invalidTeleport") }
                    }

                    // If we got here, that means that the process was successful.
                    // Therefore, set the death and revive status to false, as to avoid accidentally reviving a player
                    // twice, without them having died in the meantime.
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

        private fun restoreReviveInventory() {
            with(Bukkit.getPlayer(uuid)) {
                this ?: return yolo.logger.warning { Yolo.pluginResourceBundle.getString("player.revive.notOnline") }
                val restoredItemStacks = this.persistentDataContainer.get(
                    reviveInventoryKey,
                    ItemStackArrayPersistentDataType()
                ) as Array<ItemStack?>
                this.inventory.contents = restoredItemStacks
            }
        }
    }

    @Serializable
    private data class YoloPlayerData(val data: Map<String, PlayerStatus>)

    /**
     * A specialized [Listener] for this class. Handles [PlayerJoinEvent] and [PlayerPostRespawnEvent].
     */

    private object PlayerRegistry : HashMap<UUID, YoloPlayer>() {
        override fun get(key: UUID): YoloPlayer {
            return super.get(key) ?: YoloPlayer(key).also {
                this[key] = it
            }
        }
    }
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
                yolo.logger.severe { Yolo.pluginResourceBundle.getString("player.load.corrupted") }
                throw Exception()
            }
            File(dataFile.parent.toString()).mkdirs()
            dataFile.createNewFile()
            userData = Json.decodeFromString(dataFile.readText())
        }.onFailure {
            yolo.logger.severe { Yolo.pluginResourceBundle.getString("player.load.fail") }
        }

        Arrays.stream(allPlayers).forEach { offlinePlayer: OfflinePlayer ->
            val recoveredStatus = userData.data[offlinePlayer.uniqueId.toString()]
            PlayerRegistry[offlinePlayer.uniqueId] = recoveredStatus?.let {
                YoloPlayer(offlinePlayer.uniqueId, it.copy(ghostState = it.ghostState.copy(attachedPlayerID = offlinePlayer.uniqueId)))
            } ?: YoloPlayer(offlinePlayer.uniqueId)
        }
        // (There's also probably no need to reload this manager, as nothing is config dependent.)
    }

    /**
     * Provides a list of the names from every player, that is dead and isn't yet set to be revived.
     * Useful for command tab-completions.
     *
     * @return A list of every revivable player.
     */
    fun provideRevivable(): List<String> {
        val listOfRevivable = ArrayList<String>()
        PlayerRegistry.forEach { (uuid: UUID, yoloPlayer: YoloPlayer) ->
            val nameRetrieved = Bukkit.getOfflinePlayer(uuid).name
            if ( nameRetrieved != null && yoloPlayer.playerStatus.isDead && !yoloPlayer.playerStatus.isToRevive) {
                listOfRevivable.add(nameRetrieved)
            }
        }
        return listOfRevivable
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
        val target = PlayerRegistry[Bukkit.getOfflinePlayer(targetName).uniqueId]
        val successful = target.setIsToReviveOnDead(reviveOnJoin)
        if (!successful) throw IllegalStateException("Player has not been set to be revived!")
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
        val targetLocation = PlayerRegistry[Bukkit.getOfflinePlayer(targetName).uniqueId].playerStatus.latestDeathPos
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
        val playerFromRegistry = PlayerRegistry[deathEvent.player.uniqueId]
        val stringReplacementMap: HashMap<String, String?> = TextReplacements.provideStringDefaults(deathEvent, TextReplacements.ALL)
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

        playerFromRegistry.playerStatus.isDead = true
        playerFromRegistry.playerStatus.latestDeathPos = deathEvent.player.location
        playerFromRegistry.playerStatus.banMessage = dynamicBanMessage ?: Component.text("[<red>Yolo</red>] » You have died and therefore can no longer play on this hardcore server. :(")

        // Emit pre-death-ban events.
        val preEvent = PreDeathBanEvent(
            deathEvent.player.uniqueId to playerFromRegistry.playerStatus.copy(),
            DeathBanResult(true, deathEvent.player.location, playerFromRegistry.playerStatus.banMessage),
            deathEvent
        )
        preEvent.callEvent()

        with(preEvent.targetResult) {
            playerFromRegistry.playerStatus.isDead = this.successful
            playerFromRegistry.playerStatus.latestDeathPos = this.latestDeathPos
            playerFromRegistry.playerStatus.banMessage = this.banMessage
        }

        object : BukkitRunnable() {
            override fun run() {
                PreDeathBanEventAsync(
                    deathEvent.player.uniqueId to playerFromRegistry.playerStatus.copy(),
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
                    deathEvent.player.uniqueId to playerFromRegistry.playerStatus,
                    preEvent.targetResult,
                    deathEvent
                ).callEvent()
            }
        }

        // Finally, ban the player from the pseudo-ban server. (deferred)
        object : BukkitRunnable() {
            override fun run() {
                if (playerFromRegistry.playerStatus.isDead) {
                    pseudoBanPlayer(deathEvent.player.uniqueId, null)

                    // It's about sending a message.
                    if (safeSpicordManager()?.isSpicordBotAvailable == true) {
                        safeSpicordManager()?.trySend(DiscordMessageType.DEATH, stringReplacementMap)
                    }
                    ChatManager.trySend(Bukkit.getServer(), ChatManager.ChatMessageType.DEATH, componentReplacementMap)
                }

                postEventAsync.runTaskAsynchronously(yolo)
                PostDeathBanEvent(
                    deathEvent.player.uniqueId to playerFromRegistry.playerStatus,
                    preEvent.targetResult,
                    deathEvent
                ).callEvent()
            }
        }.runTaskLater(yolo, 1)
    }

    /**
     * Is to be called when the plugin is disabled. Ensures that every important bit of data is saved.
     */
    internal fun savePlayerData() {
        yolo.logger.info { Yolo.pluginResourceBundle.getString("player.saveData.start") }
        val toSerializeMap = mutableMapOf<String, PlayerStatus>()
        PlayerRegistry.forEach { (uuid, yoloPlayer) ->
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
            yolo.logger.severe { Yolo.pluginResourceBundle.getString("player.saveData.failure") }
        }.onSuccess {
            yolo.logger.info { Yolo.pluginResourceBundle.getString("player.saveData.success") }
        }
    }

    /**
     * Pseudo-ban players, if they're dead and aren't queued to be revived.
     *
     *
     * Only "Pseudo-"ban, because the player isn't officially put on the ban-list and thus is only banned as long as the
     * plugin is enabled.
     *
     * @param playerUUID The uuid of the player
     * @param preLoginEvent The corresponding event, if this method is called upon a player attempting to join.
     */
    private fun pseudoBanPlayer(playerUUID: UUID, preLoginEvent: AsyncPlayerPreLoginEvent?) {
        PlayerRegistry[playerUUID].let {
            val message = it.playerStatus.banMessage
            preLoginEvent?.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message) ?: Bukkit.getServer().getPlayer(playerUUID)?.kick(message, PlayerKickEvent.Cause.BANNED)
        }
    }
}
