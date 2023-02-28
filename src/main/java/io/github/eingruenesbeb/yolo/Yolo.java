package io.github.eingruenesbeb.yolo;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ResourceBundle;

@SuppressWarnings("unused")
public final class Yolo extends JavaPlugin {
    public static final ResourceBundle PLUGIN_RESOURCE_BUNDLE = ResourceBundle.getBundle("i18n");
    public static boolean useAB = false;

    @Override
    public void onEnable() {
        useAB = Bukkit.getPluginManager().isPluginEnabled("AdvancedBan");
        getServer().getPluginManager().registerEvents(new YoloEventListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
