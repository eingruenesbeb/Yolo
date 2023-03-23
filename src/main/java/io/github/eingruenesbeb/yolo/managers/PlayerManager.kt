/*
 * This is program is a plugin for Minecraft Servers called "Yolo".
 * Copyright (c) 2023  eingruenesbeb
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *   You can reach the original author via e-Mail: agreenbeb@gmail.com
 */
package io.github.eingruenesbeb.yolo.managers

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import io.github.eingruenesbeb.yolo.Yolo
import io.github.eingruenesbeb.yolo.utilities.PlayerInventoryPersistentDataType
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.jetbrains.annotations.Contract
import java.util.*

// TODO: Switch from saving the `YoloPlayer.isDead` and `YoloPlayer.isToRevive` to the player specific PDC to another
//  file in the plugin's data folder. Then update the constructor to read the data for these values from there. This
//  ensures, that these values can be manipulated, even if the player is offline.
class PlayerManager private constructor() {
    private class YoloPlayer    // The player already stores the coordinates of the last death, so there's no need there to store these.
        (val offlinePlayer: OfflinePlayer) {
        private val yolo = JavaPlugin.getPlugin(Yolo::class.java)
        private val reviveInventoryKey = NamespacedKey(yolo, "reviveInventory")
        private val isDeadKey = NamespacedKey(yolo, "isDead")
        private val isToReviveKey = NamespacedKey(yolo, "isToRevive")
        var isDead = false
        var isToRevive = false
        fun setIsToReviveOnDead(toTrue: Boolean) {
            if (isDead && toTrue) {
                isToRevive = true
                // If this is set to true, the player has been unbanned and will be revived upon the next join.
            } else if (!toTrue) {
                // Disabling this flag should always go through, because it is generally safe, as nothing will happen.
                isDead = false
            }
        }

        fun saveStatus() {
            yolo.getLogger().info("Saving player status...")
            with(offlinePlayer.player) {
                this ?: return yolo.getLogger().warning(yolo.pluginResourceBundle.getString("player.status.notOnline"))
                yolo.getLogger().info("Really doing it!")
                this.persistentDataContainer.set(isDeadKey, PersistentDataType.BYTE, this@YoloPlayer.isDead.let { if (it) 1.toByte() else 0.toByte() })
                this.persistentDataContainer.set(isToReviveKey, PersistentDataType.BYTE, isToRevive.let { if (it) 1.toByte() else 0.toByte() })
            }
        }

        fun saveReviveInventory() {
            with(offlinePlayer.player) {
                this ?: return yolo.getLogger().warning(yolo.pluginResourceBundle.getString("player.saveInventory.offline"))
                val inventoryToSave = this.inventory
                this.persistentDataContainer.set(
                    reviveInventoryKey,
                    PlayerInventoryPersistentDataType(),
                    inventoryToSave
                )
            }
        }

        private fun restoreReviveInventory() {
            with(offlinePlayer.player) {
                this ?: return yolo.getLogger().warning(yolo.pluginResourceBundle.getString("player.revive.notOnline"))
                val restoredInventory = this.persistentDataContainer.get(
                        reviveInventoryKey,
                        PlayerInventoryPersistentDataType()
                ) ?: Bukkit.createInventory(null, InventoryType.PLAYER)
                this.inventory.contents = restoredInventory.contents
            }
        }

        fun revivePlayer() {
            with(offlinePlayer.player) {
                this ?: return yolo.getLogger().warning(yolo.pluginResourceBundle.getString("player.revive.notOnline"))
                if (this@YoloPlayer.isDead && isToRevive) {
                    this@YoloPlayer.isDead = false
                    this.gameMode = GameMode.SURVIVAL
                    restoreReviveInventory()
                    if (this.lastDeathLocation != null) {
                        // The terrain may have changed, while the player was gone, so it has to be checked for safety.
                        safeTeleport(this, this.lastDeathLocation!!)
                    } else {
                        // Shouldn't happen, as the player is supposed to have died at least once. But just to be safe...
                        yolo.getLogger().warning(
                            yolo.pluginResourceBundle.getString("player.revive.noLastDeath")
                                .replace("%player_name%", this.name)
                        )
                    }
                    this@YoloPlayer.isDead = false
                    isToRevive = false
                }
            }
        }

        private fun safeTeleport(player: Player, targetLocation: Location) {
            // Despite the check, the location may still be dangerous.
            val effectsOnTeleport = listOf(
                PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30, 6),
                PotionEffect(PotionEffectType.INVISIBILITY, 30, 1)
            )
            if (checkTeleportSafety(targetLocation)) {
                player.addPotionEffects(effectsOnTeleport)
                player.teleport(targetLocation)
            } else if (checkTeleportSafety(targetLocation.toHighestLocation())) {
                player.addPotionEffects(effectsOnTeleport)
                player.teleport(targetLocation.toHighestLocation())
            } else {
                // Give up
                JavaPlugin.getPlugin(Yolo::class.java).logger.info(
                    JavaPlugin.getPlugin(
                        Yolo::class.java
                    ).pluginResourceBundle.getString("player.revive.unsafeTeleport")
                )
            }
        }

        @Contract(pure = true)
        private fun checkTeleportSafety(teleportLocation: Location): Boolean {
            // Player may suffocate, when teleported into a solid block.
            if (teleportLocation.block.isSolid) return false

            // The location may be in the void.
            if (teleportLocation.block.type == Material.VOID_AIR) return false

            // Or it may be above void or other dangerous blocks below.
            val iterateLocation = teleportLocation.clone()
            val hazardousMaterials = ArrayList(
                listOf(
                    Material.LAVA,
                    Material.FIRE,
                    Material.SOUL_FIRE,
                    Material.CAMPFIRE,
                    Material.SOUL_CAMPFIRE,
                    Material.MAGMA_BLOCK,
                    Material.VOID_AIR
                )
            )
            for (i in iterateLocation.blockY downTo -65 + 1) {
                if (hazardousMaterials.contains(iterateLocation.block.type)) return false
                if (iterateLocation.block.type.isCollidable) break
                if (i == -64) return false
            }
            return true
        }
    }

    class PlayerManagerEvents : Listener {
        @EventHandler(ignoreCancelled = true)
        fun onPlayerJoin(event: PlayerJoinEvent) {
            instance.playerRegistry.putIfAbsent(event.player.uniqueId, YoloPlayer(event.player))
            instance.playerRegistry[event.player.uniqueId]!!.revivePlayer()
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
        fun onPlayerQuit(event: PlayerQuitEvent) {
            val playerFromRegistry = instance.playerRegistry[event.player.uniqueId]
            playerFromRegistry!!.saveStatus()
        }

        @EventHandler(ignoreCancelled = true)
        fun onPlayerPostRespawn(event: PlayerPostRespawnEvent) {
            val playerFromRegistry = instance.playerRegistry[event.player.uniqueId]
            playerFromRegistry!!.revivePlayer()
        }
    }

    private val playerRegistry = HashMap<UUID, YoloPlayer>()
    private val yolo = JavaPlugin.getPlugin(Yolo::class.java)

    init {
        // The registration of all previously online players is paramount to providing and modifying them in case they
        // get revived. This step is fine during the initial load, but NOT TO BE REPEATED DURING A RELOAD of the plugin.
        val allPlayers = Bukkit.getOfflinePlayers()
        Arrays.stream(allPlayers).forEach { offlinePlayer: OfflinePlayer ->
            playerRegistry[offlinePlayer.uniqueId] = YoloPlayer(offlinePlayer)
        }
        // (There's also probably no need to reload this manager, as nothing is config dependent.)
    }

    fun setReviveOnUser(targetUUID: UUID, reviveOnJoin: Boolean) {
        val target = playerRegistry[targetUUID]
        // There shouldn't be a NPE, because the player is guaranteed to have joined the server at least once.
        val targetName = Objects.requireNonNull(target!!.offlinePlayer.name)
        target.setIsToReviveOnDead(reviveOnJoin)
        if (reviveOnJoin) {
            Bukkit.getBanList(BanList.Type.NAME).pardon(targetName!!)
        } else if (!Bukkit.getBanList(BanList.Type.NAME).isBanned(targetName!!)) {
            target.offlinePlayer.banPlayer(yolo.pluginResourceBundle.getString("player.ban.death"))
        }
    }

    fun provideRevivable(): List<String> {
        val listOfRevivable = ArrayList<String>()
        playerRegistry.forEach { (_: UUID?, yoloPlayer: YoloPlayer) ->
            if (yoloPlayer.offlinePlayer.name != null && yoloPlayer.isDead && !yoloPlayer.isToRevive) {
                listOfRevivable.add(yoloPlayer.offlinePlayer.name!!)
            }
        }
        return listOfRevivable
    }

    fun actionsOnDeath(player: Player) {
        val playerFromRegistry = playerRegistry[player.uniqueId]
        playerFromRegistry!!.isDead = true
        playerFromRegistry.saveReviveInventory()

        // This may be necessary, if the player was banned by this plugin directly. (See the comment in
        // YoloEventListener#onPlayerDeath())
        player.inventory.contents = arrayOf()
    }

    companion object {
        val instance = PlayerManager()
    }
}