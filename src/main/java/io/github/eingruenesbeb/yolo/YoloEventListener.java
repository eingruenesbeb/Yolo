package io.github.eingruenesbeb.yolo;

import me.leoko.advancedban.bukkit.BukkitMethods;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Locale;

public class YoloEventListener implements Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
         Player player = event.getPlayer();
         String reason = Yolo.PLUGIN_RESOURCE_BUNDLE.getString("player.ban.death");
         if (!player.hasPermission("yolo.exempt") && Bukkit.getServer().isHardcore()) {
             if (Yolo.useAB) {
                 BukkitMethods abMethods = new BukkitMethods();
                 abMethods.loadFiles();
                 boolean layoutConfigured = abMethods.getLayouts().contains("Message.Hardcore_death");
                 if (layoutConfigured) {
                     Bukkit.dispatchCommand(YoloPluginCommandSender.PLUGIN_COMMAND_SENDER, String.format("ban -s %s @Hardcore_death", player.getName()));
                 } else {
                     Bukkit.dispatchCommand(YoloPluginCommandSender.PLUGIN_COMMAND_SENDER, String.format("ban -s %s %s", player.getName(), reason));
                 }
             } else {
                 player.banPlayerFull(reason);
             }
         }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("yolo.exempt") && Bukkit.isHardcore()) {
            final TextComponent textComponent = (TextComponent) Component.text("You are in hardcore mode. Please accept this ressource pack to reflecting that.").replaceText(builder -> {
                builder.matchLiteral("You are in hardcore mode. Please accept this ressource pack to reflecting that.");
                switch (player.locale().getLanguage()) {
                    case "lol_us":
                        builder.replacement("U R IN YOLO MODE. PLZ ACCEPT DIS RESOURCE PACKZ 2 REFLECTIN DAT.");
                    case "de_de":
                        builder.replacement("Du bist im Hardcoremodus. Bitte akzeptiere dieses Resourcenpacket um dies akkurat darzustellen.");
                    default:
                        builder.replacement("You are in hardcore mode. Please accept this ressource pack to reflecting that.");
                }
                builder.once();
            });

            player.setResourcePack("https://drive.google.com/uc?export=download&id=1UWoiOGFlt2QIyQPVKAv5flLTNeNiI439",
                hashFromString("cc17ee284417acd83536af878dabecab7ca7f3d1"),
                textComponent,
                true
            );
        }
    }

    private byte @NotNull [] hashFromString(@NotNull String input) {
        assert input.matches("[0-f]{40}");
        byte[] result = new byte[input.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int index = i * 2;
            int val = Integer.parseInt(input.substring(index, index + 2), 16);
            result[i] = (byte) val;
        }

        return result;
    }
}
