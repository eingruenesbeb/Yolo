/*
 * This program is a plugin for Minecraft Servers called "Yolo".
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
package io.github.eingruenesbeb.yolo.managers

import io.github.eingruenesbeb.yolo.Yolo
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.Contract
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.CompletableFuture

/**
 * This is a utility object that manages to set up a resource pack, as it is described in the plugin's config.
 * It provides asynchronous validation of the provided resource-pack and a fallback if no custom resource pack should be
 * used, or the custom resource pack couldn't be validated.
 * It also provides a method to apply the resource pack to a player.
 */
internal object ResourcePackManager : ReloadableManager {
    private const val DEFAULT_PACK_URL = "https://download.mc-packs.net/pack/cc17ee284417acd83536af878dabecab7ca7f3d1.zip"
    private const val DEFAULT_PACK_SHA1 = "cc17ee284417acd83536af878dabecab7ca7f3d1"
    private val yolo: Yolo = JavaPlugin.getPlugin(Yolo::class.java)
    private var packURL = DEFAULT_PACK_URL
    private var packSha1  = DEFAULT_PACK_URL
    private var force = true

    /**
     * Constructs a new ResourcePackManager sets all important fields from the config or fallback and asynchronously
     * calls for validation of the pack. For details see `ResourcePackManager#validatePackAsync`.
     */
    init {
        reload()
    }

    /**
     * Sends a request to the given player, to load the configured resource-pack.
     * @param player The player to apply this resource-pack to.
     */
    fun applyPack(player: Player) {
        val textComponent =
            Component.text("You are in hardcore mode. Please accept this ressource pack to reflecting that.")
        player.setResourcePack(packURL, packSha1, force, textComponent)
    }

    override fun reload() {
        val config = yolo.config
        if (config.getBoolean("resource-pack.custom.use")) {
            config.getString("resource-pack.custom.url")?.let { packURL = it }
            config.getString("resource-pack.custom.sha1")?.let { packSha1 = it }
        }

        force = config.getBoolean("resource-pack.force", true)
        validatePackAsync(packURL, packSha1).whenComplete { isValid: Boolean?, _: Throwable? ->
            if (!isValid!!) {
                yolo.logger.warning { Yolo.pluginResourceBundle.getString("loading.resourcePack.invalid") }
                packURL = DEFAULT_PACK_URL
                packSha1 = DEFAULT_PACK_SHA1
            }
        }
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
    private fun validatePackAsync(url: String, expected: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                // Download the file
                val inputStream = url.let { URI(it).toURL().openStream() }
                val outputStream = ByteArrayOutputStream()
                val checksumLength = 4069
                val buffer = ByteArray(checksumLength)
                var bytesRead: Int
                if (inputStream != null) {
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
                val fileBytes = outputStream.toByteArray()
                inputStream?.close()
                outputStream.close()

                // Compute the SHA1 hash value of the downloaded file
                val digest = MessageDigest.getInstance("SHA1")
                val hash = digest.digest(fileBytes)
                val actual = bytesToHex(hash)

                // Compare the computed hash value with the expected one
                return@supplyAsync actual.equals(expected, ignoreCase = true)
            } catch (e: IOException) {
                return@supplyAsync false
            } catch (e: NoSuchAlgorithmException) {
                return@supplyAsync false
            }
        }
    }
    
    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
