package io.github.eingruenesbeb.yolo.commands

import io.github.eingruenesbeb.yolo.Yolo
import io.github.eingruenesbeb.yolo.managers.PlayerManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

class UndoReviveCommand : TabExecutor {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): List<String> {
        return PlayerManager.provideRevived()
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): Boolean {
        return args?.first()?.let { playerNameArg ->
            PlayerManager.undoRevive(playerNameArg).also {
                if (it) sender.sendMessage(MiniMessage.miniMessage().deserialize(Yolo.pluginResourceBundle.getString("player.revive.undo.success")))
                else sender.sendMessage(MiniMessage.miniMessage().deserialize(Yolo.pluginResourceBundle.getString("player.revive.undo.fail")))
            }
        } ?: false
    }
}
