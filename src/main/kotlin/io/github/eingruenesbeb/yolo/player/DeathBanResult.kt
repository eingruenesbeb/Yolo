package io.github.eingruenesbeb.yolo.player

import net.kyori.adventure.text.Component
import org.bukkit.Location

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
