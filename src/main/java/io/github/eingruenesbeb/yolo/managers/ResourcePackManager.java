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

package io.github.eingruenesbeb.yolo.managers;

import io.github.eingruenesbeb.yolo.Yolo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

/**
 * This is a utility class, that manages setting up a resource pack, as it is described in the plugin's config. It
 * provides asynchronous validation of the provided resource-pack and a fallback, if no custom resource pack should
 * be used or the custom resource pack couldn't be validated. It also provides a method to apply the resource pack
 * to a player.
 */
public final class ResourcePackManager {
    private static final ResourcePackManager SINGLETON = new ResourcePackManager();

    /**
     * Gets the singleton instance of this manager.
     *
     * @return The singleton instance of this manager.
     */
    public static ResourcePackManager getInstance() {
        return SINGLETON;
    }

    /**
     * This method is used, to update the singleton instance of this manager, based on the current config file.
     */
    public static void reload() {
        SINGLETON.reloadInstance();
    }

    private String packURL;
    private String packSha1;
    private final String defaultPackURL = "https://drive.google.com/uc?export=download&id=1UWoiOGFlt2QIyQPVKAv5flLTNeNiI439";
    private final String defaultPackSha1 = "cc17ee284417acd83536af878dabecab7ca7f3d1";
    private boolean force;

    private final Yolo yolo;

    /**
     * Constructs a new ResourcePackManager sets all important fields from the config or fallback and asynchronously
     * calls for validation of the pack.
     * @see ResourcePackManager#validatePackAsync(String, String)
     */
    private ResourcePackManager() {
        yolo = Yolo.getPlugin(Yolo.class);
        FileConfiguration config = yolo.getConfig();
        if (config.getBoolean("resource-pack.custom.use")) {
            packURL = config.getString("resource-pack.custom.url", defaultPackURL);
            packSha1 = config.getString("resource-pack.custom.sha1", defaultPackSha1);
        } else {
            packURL = defaultPackURL;
            packSha1 = defaultPackSha1;
        }
        force = config.getBoolean("resource-pack.force", true);

        validatePackAsync(packURL, packSha1).whenComplete((isValid, throwable) -> {
            if (!isValid) {
                yolo.getLogger().warning(yolo.getPluginResourceBundle().getString("loading.resourcePack.invalid"));
                packURL = defaultPackURL;
                packSha1 = defaultPackSha1;
            }
        });
    }

    /**
     * Sends a request to the given player, to load the configured resource-pack.
     * @param player The player to apply this resource-pack to.
     */
    public void applyPack(final @NotNull Player player) {
        TextComponent textComponent = Component.text("You are in hardcore mode. Please accept this ressource pack to reflecting that.");
        player.setResourcePack(packURL, packSha1, force, textComponent);
    }

    private void reloadInstance() {
        FileConfiguration config = yolo.getConfig();
        if (config.getBoolean("resource-pack.custom.use")) {
            packURL = config.getString("resource-pack.custom.url", defaultPackURL);
            packSha1 = config.getString("resource-pack.custom.sha1", defaultPackSha1);
        } else {
            packURL = defaultPackURL;
            packSha1 = defaultPackSha1;
        }
        force = config.getBoolean("resource-pack.force", true);

        validatePackAsync(packURL, packSha1).whenComplete((isValid, throwable) -> {
            if (!isValid) {
                yolo.getLogger().warning(yolo.getPluginResourceBundle().getString("loading.resourcePack.invalid"));
                packURL = defaultPackURL;
                packSha1 = defaultPackSha1;
            }
        });
    }

    /**
     * Helper method to convert a byte array to a hexadecimal string.
     */
    private static @NotNull String bytesToHex(final byte @NotNull [] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Asynchronously downloads the pack from the given URL and checks its SHA1 hash value.
     *
     * @param url      the URL of the file to download
     * @param expected the expected SHA1 hash value of the file
     * @return A completable future, that, once it is completed, returns a boolean, that indicates, whether the pack
     * can be downloaded, and it's checksum matches the one provided.
     */
    @Contract("_, _ -> new")
    private @NotNull CompletableFuture<Boolean> validatePackAsync(final String url, final String expected) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Download the file
                InputStream inputStream = new URL(url).openStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final int checksum_length = 4069;
                byte[] buffer = new byte[checksum_length];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                byte[] fileBytes = outputStream.toByteArray();
                inputStream.close();
                outputStream.close();

                // Compute the SHA1 hash value of the downloaded file
                MessageDigest digest = MessageDigest.getInstance("SHA1");
                byte[] hash = digest.digest(fileBytes);
                String actual = bytesToHex(hash);

                // Compare the computed hash value with the expected one
                return actual.equalsIgnoreCase(expected);
            } catch (IOException | NoSuchAlgorithmException e) {
                return false;
            }
        });
    }
}
