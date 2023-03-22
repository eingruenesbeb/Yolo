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

package io.github.eingruenesbeb.yolo.managers;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.github.eingruenesbeb.yolo.Yolo;
import io.github.eingruenesbeb.yolo.utilities.BooleanPersistentDataType;
import org.apache.commons.lang3.SerializationUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

public class PlayerManager {
    private static class YoloPlayer {

        private final Yolo yolo = Yolo.getPlugin(Yolo.class);
        private final NamespacedKey reviveInventoryKey = new NamespacedKey(yolo, "reviveInventory");
        private final NamespacedKey isDeadKey = new NamespacedKey(yolo, "isDead");
        private final NamespacedKey isToReviveKey = new NamespacedKey(yolo, "isToRevive");
        private final OfflinePlayer offlinePlayer;
        private boolean isDead = false;
        private boolean isToRevive = false;
        private @Nullable Player onlinePlayer;
        // The player already stores the coordinates of the last death, so there's no need there to store these.

        public YoloPlayer(OfflinePlayer player) {
            this.offlinePlayer = player;
        }

        public void setIsToReviveOnDead(boolean toTrue) {
            if (this.isDead && toTrue) {
                this.isToRevive = true;
                // If this is set to true, the player has been unbanned and will be revived upon the next join.
            } else if (!toTrue) {
                // Disabling this flag should always go through, because it is generally safe, as nothing will happen.
                this.isDead = false;
            }
        }

        public void setPlayer() {
            onlinePlayer = offlinePlayer.getPlayer();
        }

        public void saveStatus() {
            assert onlinePlayer != null;
            onlinePlayer.getPersistentDataContainer().set(isDeadKey, new BooleanPersistentDataType(), isDead);
            onlinePlayer.getPersistentDataContainer().set(isToReviveKey, new BooleanPersistentDataType(), isToRevive);
        }

        private void saveReviveInventory() {
            if (onlinePlayer != null) {
                PlayerInventory inventoryToSave = onlinePlayer.getInventory();
                byte[] serializedInventory = SerializationUtils.serialize((Serializable) inventoryToSave);
                onlinePlayer.getPersistentDataContainer().set(reviveInventoryKey, PersistentDataType.BYTE_ARRAY, serializedInventory);
            } else {
                yolo.getLogger().warning(yolo.getPluginResourceBundle().getString("player.saveInventory.offline"));
            }
        }

        private void restoreReviveInventory() {
            try {
                assert onlinePlayer != null;
                final PlayerInventory fromPDC;
                fromPDC = SerializationUtils.deserialize(Objects.requireNonNull(onlinePlayer.getPersistentDataContainer().get(reviveInventoryKey, PersistentDataType.BYTE_ARRAY)));
                onlinePlayer.getInventory().setContents(fromPDC.getContents());
            } catch (NullPointerException npe) {
                yolo.getLogger().warning(yolo.getPluginResourceBundle().getString("player.revive.noReviveInventory").replace("%player_name%", onlinePlayer.getName()));
                onlinePlayer.getInventory().setContents(new ItemStack[] {});
            }
        }

        private void revivePlayer() {
            try {
                assert onlinePlayer != null;
            } catch (AssertionError e) {
                yolo.getLogger().warning(yolo.getPluginResourceBundle().getString("player.revive.notOnline").replace("%player_uuid%", offlinePlayer.getUniqueId().toString()));
                return;
            }

            if (isDead && isToRevive) {
                isDead = false;
                onlinePlayer.setGameMode(GameMode.SURVIVAL);
                restoreReviveInventory();
                if (onlinePlayer.getLastDeathLocation() != null) {
                    // The terrain may have changed, while the player was gone, so it has to be checked for safety.
                    safeTeleport(onlinePlayer, onlinePlayer.getLastDeathLocation());
                } else {
                    // Shouldn't happen, as the player is supposed to have died at least once. But just to be safe...
                    yolo.getLogger().warning(yolo.getPluginResourceBundle().getString("player.revive.noLastDeath").replace("%player_name%", onlinePlayer.getName()));
                }

                isDead = false;
                isToRevive = false;
            }
        }

        private void safeTeleport(@NotNull Player player, Location targetLocation) {
            // Despite the check, the location may still be dangerous.
            final List<PotionEffect> effectsOnTeleport = List.of(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30, 6), new PotionEffect(PotionEffectType.INVISIBILITY, 30, 1));

            if (checkTeleportSafety(targetLocation)) {
                player.addPotionEffects(effectsOnTeleport);
                player.teleport(targetLocation);
            } else if (checkTeleportSafety(targetLocation.toHighestLocation())) {
                player.addPotionEffects(effectsOnTeleport);
                player.teleport(targetLocation.toHighestLocation());
            } else {
                // Give up
                Yolo.getPlugin(Yolo.class).getLogger().info(Yolo.getPlugin(Yolo.class).getPluginResourceBundle().getString("player.revive.unsafeTeleport"));
            }
        }

        @Contract(pure = true)
        private boolean checkTeleportSafety(final @NotNull Location teleportLocation) {
            // Player may suffocate, when teleported into a solid block.
            if (teleportLocation.getBlock().isSolid()) return false;

            // The location may be in the void.
            if (teleportLocation.getBlock().getType() == Material.VOID_AIR) return false;

            // Or it may be above void or other dangerous blocks below.
            final Location iterateLocation = teleportLocation.clone();
            final ArrayList<Material> hazardousMaterials = new ArrayList<>(List.of(Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.MAGMA_BLOCK, Material.VOID_AIR));

            for (int i = iterateLocation.getBlockY(); i > -65; i--) {
                if (hazardousMaterials.contains(iterateLocation.getBlock().getType())) return false;
                if (iterateLocation.getBlock().getType().isCollidable()) break;
                if (i == -64) return false;
            }

            return true;
        }
    }

    private static class PlayerManagerEvents implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
            final YoloPlayer playerFromRegistry = getInstance().playerRegistry.get(event.getPlayer().getUniqueId());
            playerFromRegistry.setPlayer();
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
            final YoloPlayer playerFromRegistry = getInstance().playerRegistry.get(event.getPlayer().getUniqueId());
            playerFromRegistry.saveStatus();
            playerFromRegistry.onlinePlayer = null;
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerPostRespawn(final @NotNull PlayerPostRespawnEvent event) {
            final YoloPlayer playerFromRegistry = getInstance().playerRegistry.get(event.getPlayer().getUniqueId());
            playerFromRegistry.revivePlayer();
        }
    }

    private static final PlayerManager INSTANCE = new PlayerManager();

    public static PlayerManager getInstance() {
        return INSTANCE;
    }

    private final HashMap<UUID, YoloPlayer> playerRegistry = new HashMap<>();
    private final Yolo yolo = Yolo.getPlugin(Yolo.class);

    private PlayerManager() {
        yolo.getServer().getPluginManager().registerEvents(new PlayerManagerEvents(), yolo);

        // The registration of all previously online players is paramount to providing and modifying them in case they
        // get revived. This step is fine during the initial load, but NOT TO BE REPEATED DURING A RELOAD of the plugin.
        OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();
        Arrays.stream(allPlayers).forEach(offlinePlayer -> playerRegistry.put(offlinePlayer.getUniqueId(), new YoloPlayer(offlinePlayer)));
        // (There's also probably no need to reload this manager, as nothing is config dependent.)
    }

    public void setReviveOnUser(UUID targetUUID, boolean reviveOnJoin) {
        YoloPlayer target = playerRegistry.get(targetUUID);
        // There shouldn't be a NPE, because the player is guaranteed to have joined the server at least once.
        String targetName = Objects.requireNonNull(target.offlinePlayer.getName());

        target.setIsToReviveOnDead(reviveOnJoin);
        if (reviveOnJoin) {
            Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);
        } else if (!Bukkit.getBanList(BanList.Type.NAME).isBanned(targetName)) {
            target.offlinePlayer.banPlayer(yolo.getPluginResourceBundle().getString("player.ban.death"));
        }
    }

    public List<String> provideRevivable() {
        final ArrayList<String> listOfRevivable = new ArrayList<>();
        playerRegistry.forEach((uuid, yoloPlayer) -> {
            if (yoloPlayer.offlinePlayer.getName()  != null && yoloPlayer.isDead && !yoloPlayer.isToRevive) {
                listOfRevivable.add(yoloPlayer.offlinePlayer.getName());
            }
        });
        return listOfRevivable;
    }

    public void actionsOnDeath(final @NotNull Player player) {
        final YoloPlayer playerFromRegistry = playerRegistry.get(player.getUniqueId());

        playerFromRegistry.isDead = true;
        playerFromRegistry.saveReviveInventory();

        // This may be necessary, if the player was banned by this plugin directly. (See the comment in
        // YoloEventListener#onPlayerDeath())
        player.getInventory().setContents(new ItemStack[] {});
    }
}
