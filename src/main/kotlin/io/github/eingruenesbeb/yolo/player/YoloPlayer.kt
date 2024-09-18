package io.github.eingruenesbeb.yolo.player

import io.github.eingruenesbeb.yolo.TeleportationUtils.safeTeleport
import io.github.eingruenesbeb.yolo.Yolo
import io.github.eingruenesbeb.yolo.events.revive.PreYoloPlayerReviveEvent
import io.github.eingruenesbeb.yolo.events.revive.PreYoloPlayerReviveEventAsync
import io.github.eingruenesbeb.yolo.events.revive.YoloPlayerRevivedEvent
import io.github.eingruenesbeb.yolo.events.revive.YoloPlayerRevivedEventAsync
import io.github.eingruenesbeb.yolo.serialize.ItemStackArrayPersistentDataType
import net.kyori.adventure.util.TriState
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

class YoloPlayer(
    val yoloPlayerData: YoloPlayerData
) {
    private val yolo = Yolo.pluginInstance!!
    private val reviveInventoryKey = NamespacedKey(yolo, "reviveInventory")

    fun setIsToReviveOnDead(toTrue: Boolean): Boolean {
        return if (yoloPlayerData.isDead && toTrue) {
            yoloPlayerData.isToRevive = true
            // If this is set to true, the player has been unbanned and will be revived upon the next join.
            true
        } else {
            // Disabling this flag should always go through, because it is generally safe, as nothing will happen.
            yoloPlayerData.isDead = false
            false
        }
    }

    fun saveReviveInventory() {
        with(Bukkit.getPlayer(yoloPlayerData.uuid)) {
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
        val originalOutcome = ReviveResult(yoloPlayerData.isToRevive, yoloPlayerData.isTeleportToDeathPos, yoloPlayerData.isRestoreInventory)

        val targetOutcome = dispatchPreReviveEvents(originalOutcome)

        var isSuccess = false
        var isTeleported = false
        var isWithInventory = false
        var teleportedFrom: Location? = null
        if (targetOutcome.successful) {
            with(Bukkit.getPlayer(yoloPlayerData.uuid)) {
                this ?: return yolo.logger.warning { Yolo.pluginResourceBundle.getString("player.revive.notOnline") }  // The player will still be revivable
                // if the process is stopped here.

                this.gameMode = GameMode.SURVIVAL

                if (targetOutcome.inventoryRestored) {
                    restoreReviveInventory()
                    isWithInventory = true
                }

                if (targetOutcome.teleported) yoloPlayerData.latestDeathPos.runCatching {
                    teleportedFrom = this@with.location
                    isTeleported = safeTeleport(this@with, this!!)
                    if (isTeleported) {
                        yoloPlayerData.ghostState.apply()  // Auto-removes after 600 ticks
                    }  // Effects get applied by the teleport function.
                    if (!isTeleported) yolo.logger.warning { Yolo.pluginResourceBundle.getString("player.revive.invalidTeleport") }
                }

                // If we got here, that means that the process was successful.
                // Therefore, set the death and revive status to false, as to avoid accidentally reviving a player
                // twice, without them having died in the meantime.
                yoloPlayerData.isDead = false
                yoloPlayerData.isToRevive = false
                isSuccess = true
            }
        } else {
            // Player was initially marked for revival, but another plugin prevented it. Reset the status from
            // before the revive was scheduled.
            yoloPlayerData.isDead = true
            yoloPlayerData.isToRevive = false
        }

        val finalResult = ReviveResult(isSuccess, isTeleported, isWithInventory, teleportedFrom)

        dispatchPostReviveEvents(finalResult)

        yoloPlayerData.revives.add(finalResult)
    }

    // Runs in case the player has already been revived.
    fun undoLastReviveActive() {
        val player = Bukkit.getPlayer(yoloPlayerData.uuid) ?: return yolo.logger.warning { Yolo.pluginResourceBundle.getString("player.revive.undo.notOnline") }
        if (player.permissionValue("yolo.exempt") == TriState.TRUE) return
        yoloPlayerData.isUndoRevive = false

        runCatching {
            with(yoloPlayerData.revives.last { it.successful }) {
                this.teleportedFrom?.let { player.teleport(it) }
                if (this.inventoryRestored) {
                    player.inventory.clear()
                }
            }
        }.onFailure {
            if (it is NoSuchElementException) return
        }

        yoloPlayerData.revives.removeAt(yoloPlayerData.revives.indexOfLast { it.successful })

        yoloPlayerData.isToRevive = false
        yoloPlayerData.isDead = true
    }

    private fun dispatchPreReviveEvents(originalOutcome: ReviveResult): ReviveResult {
        val preEvent = PreYoloPlayerReviveEvent(yoloPlayerData.copy(), originalOutcome)
        preEvent.callEvent()

        // The async variant of the event is only for observing:
        object : BukkitRunnable() {
            override fun run() {
                PreYoloPlayerReviveEventAsync(yoloPlayerData.copy(), originalOutcome.copy(), preEvent.targetOutcome.copy()).callEvent()
            }
        }.runTaskAsynchronously(yolo)

        return preEvent.targetOutcome
    }

    private fun dispatchPostReviveEvents(finalResult: ReviveResult) {
        object : BukkitRunnable() {
            override fun run() {
                YoloPlayerRevivedEventAsync(yoloPlayerData.copy(), finalResult).callEvent()
            }
        }.runTaskAsynchronously(yolo)

        YoloPlayerRevivedEvent(yoloPlayerData.copy(), finalResult).callEvent()
    }

    private fun restoreReviveInventory() {
        with(Bukkit.getPlayer(yoloPlayerData.uuid)) {
            this ?: return yolo.logger.warning { Yolo.pluginResourceBundle.getString("player.revive.notOnline") }
            val restoredItemStacks = this.persistentDataContainer.get(
                reviveInventoryKey,
                ItemStackArrayPersistentDataType()
            ) as Array<ItemStack?>
            this.inventory.contents = restoredItemStacks
        }
    }
}
