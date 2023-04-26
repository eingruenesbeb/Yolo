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

package io.github.eingruenesbeb.yolo

import io.github.eingruenesbeb.yolo.TeleportationUtils.safeTeleport
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.Contract

/**
 * Contains functions useful for teleporting players.\
 * Currently only used for survival-safe teleportation.
 *
 * @see safeTeleport
 */
internal object TeleportationUtils {
    /**
     * This function is useful for teleporting a player in survival or adventure mode to a "safe location".
     *
     * A teleport is considered safe, if the target location is:
     * - Not in a solid block
     * - Not in the void
     * If [advancedChecks] is true, a teleport is considered safe, if the target location additionally fulfills the
     * following conditions:
     * - The location is not directly above: Lava, fire, soul fire, campfire, soul campfire, magma blocks, cactus or
     * void.
     * - The location is not water, when the block above is solid or water as well.
     * - The surrounding blocks do not contain lava.
     * - The neighbouring blocks are not collidable, thus guaranteeing movement for at least one block and additionally
     * raising the odds of the location being escapable.
     *
     * If the location is determined to be unsafe and [tryHighestLoc] is true, the teleportation attempt is tried again
     * at the highest solid block above the original location, as determined by [Location.toHighestLocation].\
     * If the teleport is successful, the player will gain additional effects for 30 seconds to mitigate potentially
     * dangerous mobs and players.
     *
     * @param player The player to be teleported
     * @param targetLocation The location to teleport the player to
     * @param advancedChecks If additional checks as described above should be performed. (Default: `true`)
     * @param tryHighestLoc If the highest location above the target location should also be considered, if the
     * safety-check on the original location fails. (Default `true`)
     *
     * @return Whether the attempt was successful.
     */
    fun safeTeleport(player: Player, targetLocation: Location, advancedChecks: Boolean = true, tryHighestLoc: Boolean = true): Boolean {
        // Despite the check, the location may still be dangerous.

        return if (checkTeleportSafety(targetLocation, advancedChecks)) {
            targetLocation.chunk.load()
            player.teleport(targetLocation)
        } else if (tryHighestLoc && checkTeleportSafety(targetLocation.toHighestLocation(), advancedChecks)) {
            targetLocation.chunk.load()
            player.teleport(targetLocation.toHighestLocation())
        } else {
            // Give up
            JavaPlugin.getPlugin(Yolo::class.java).logger.info {
                Yolo.pluginResourceBundle
                    .getString("player.unsafeTeleport")
                    .replace("%player_name%", player.name)
            }
            false
        }
    }

    @Contract(pure = true)
    private fun checkTeleportSafety(teleportLocation: Location, advancedChecks: Boolean): Boolean {
        // Player may suffocate, when teleported into a solid block.
        if (teleportLocation.block.isSolid) return false

        // The location may be in the void. (Technically also checked for in the next check, but it's faster that
        // way.)
        if (teleportLocation.block.type == Material.VOID_AIR) return false

        if (!advancedChecks) return true

        // ↓ Advanced checks (Can be expensive) ↓
        // Or it may be above void or other dangerous blocks.
        val iterateLocation = teleportLocation.clone()
        val hazardousMaterials = listOf(
            Material.LAVA,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.MAGMA_BLOCK,
            Material.CACTUS,
            Material.VOID_AIR
        )
        // Negative 64 is the absolute lowest block in regular worlds, so it's safe to assume no blocks below.
        for (i in teleportLocation.blockY downTo -64) {
            iterateLocation.y = i.toDouble()
            // Player may drown.
            if (i == teleportLocation.blockY) {
                if (iterateLocation.block.type == Material.WATER &&
                    (
                            iterateLocation.block.getRelative(BlockFace.UP).isCollidable ||
                                    iterateLocation.block.getRelative(BlockFace.UP).type == Material.WATER
                            )
                ) return false
            } else {
                if (hazardousMaterials.contains(iterateLocation.block.type)) return false
                if (iterateLocation.block.type.isCollidable) break
                if (i == -64) return false
            }
        }

        // Check adjacent blocks for lava or if the target location completely blocked off.
        val minPoint = teleportLocation.clone().add(-1.0, -1.0, -1.0)
        val maxPoint = teleportLocation.clone().add(1.0, 1.0, 1.0)
        val blocksInRegion = getBlocksInCuboidRegion(minPoint, maxPoint)
        return when {
            blocksInRegion.any { it.type == Material.LAVA } -> false
            blocksInRegion.filter {
                it.location.toVector().subtract(teleportLocation.toVector()).length() != 1.toDouble()
            }.all { it.isCollidable } -> false
            // Final return value (always true, because all checks are negative)
            else -> true
        }
    }

    private fun getBlocksInCuboidRegion(minPoint: Location, maxPoint: Location): List<Block> {
        val blocks = mutableListOf<Block>()
        for (x in minPoint.blockX..maxPoint.blockX) {
            for (y in minPoint.blockY..maxPoint.blockY) {
                for (z in minPoint.blockZ..maxPoint.blockZ) {
                    val block = minPoint.world.getBlockAt(x, y, z)
                    blocks.add(block)
                }
            }
        }
        return blocks
    }
}