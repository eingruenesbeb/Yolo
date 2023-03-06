package io.github.eingruenesbeb.yolo;

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
public class ResourcePackManager {
    private String packURL;
    private final String packSha1;
    private final boolean force;
    private final Yolo yolo = Yolo.getPlugin(Yolo.class);

    /**
     * Constructs a new ResourcePackManager sets all important fields from the config or fallback and asynchronously
     * calls for validation of the pack.
     * @see ResourcePackManager#validatePackAsync(String, String)
     */
    public ResourcePackManager() {
        FileConfiguration config = yolo.getConfig();
        if (config.getBoolean("resource-pack.custom.use")) {
            packURL = config.getString("resource-pack.custom.url", "https://drive.google.com/uc?export=download&id=1UWoiOGFlt2QIyQPVKAv5flLTNeNiI439");
            packSha1 = config.getString("resource-pack.custom.sha1", "cc17ee284417acd83536af878dabecab7ca7f3d1");
        } else {
            packURL = "https://drive.google.com/uc?export=download&id=1UWoiOGFlt2QIyQPVKAv5flLTNeNiI439";
            packSha1 = "cc17ee284417acd83536af878dabecab7ca7f3d1";
        }
        force = config.getBoolean("resource-pack.force", true);

        validatePackAsync(packURL, packSha1).whenComplete((isValid, throwable) -> {
            if (!isValid) {
                yolo.getLogger().warning(yolo.getPluginResourceBundle().getString("loading.resourcePack.invalid"));
                packURL = "https://drive.google.com/uc?export=download&id=1UWoiOGFlt2QIyQPVKAv5flLTNeNiI439";
            }
        });
    }

    /**
     * Sends a request to the given player, to load the configured resource-pack.
     * @param player The player to apply this resource-pack to.
     */
    public void applyPack(@NotNull Player player) {
        TextComponent textComponent = Component.text("You are in hardcore mode. Please accept this ressource pack to reflecting that.");
        player.setResourcePack(packURL, packSha1, force, textComponent);
    }

    /**
     * Helper method to convert a byte array to a hexadecimal string.
     */
    private static @NotNull String bytesToHex(byte @NotNull [] bytes) {
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
    private @NotNull CompletableFuture<Boolean> validatePackAsync(String url, String expected) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Download the file
                InputStream inputStream = new URL(url).openStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
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
