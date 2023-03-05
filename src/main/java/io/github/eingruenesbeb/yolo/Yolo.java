package io.github.eingruenesbeb.yolo;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.spicord.SpicordLoader;
import org.spicord.SpicordPlugin;
import org.spicord.api.addon.SimpleAddon;
import org.spicord.bot.DiscordBot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public final class Yolo extends JavaPlugin {
    private final ResourceBundle pluginResourceBundle = ResourceBundle.getBundle("i18n");
    private boolean useAB;
    private String deathMessageTemplate;
    private SpicordPlugin addon;
    private boolean spicordBotAvailable = false;
    private Logger logger;
    private DiscordBot spicordBot;
    private String message_channel_id;

    public String getMessage_channel_id() {
        return message_channel_id;
    }

    public DiscordBot getSpicordBot() {
        return spicordBot;
    }

    public ResourceBundle getPluginResourceBundle() {
        return pluginResourceBundle;
    }

    public boolean isUseAB() {
        return useAB;
    }

    public String getDeathMessageTemplate() {
        return deathMessageTemplate;
    }

    public SpicordPlugin getAddon() {
        return addon;
    }

    public boolean isSpicordBotAvailable() {
        return spicordBotAvailable;
    }

    private void setSpicordBotAvailable(boolean spicordBotAvailable) {
        this.spicordBotAvailable = spicordBotAvailable;
    }

    @Override
    public void onEnable() {
        try {
            regenerateMissingFiles();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, pluginResourceBundle.getString("loading.data_IOException").replace("%error%", e.toString()));
        }
        FileConfiguration config = getConfig();
        useAB = Bukkit.getPluginManager().isPluginEnabled("AdvancedBan");
        try {
            deathMessageTemplate = setDeathMessageTemplate();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, pluginResourceBundle.getString("loading.embed.IOException").replace("%error%", e.toString()));
        }
        message_channel_id = config.getString("message_channel_id");
        if (Bukkit.getPluginManager().isPluginEnabled("Spicord")) loadSpicord();
        getServer().getPluginManager().registerEvents(new YoloEventListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void regenerateMissingFiles() throws IOException {
        // Guarantee the existence of the data folder.
        //noinspection ResultOfMethodCallIgnored
        getDataFolder().mkdirs();

        // (No content checks, no subdir) config.yml:
        saveDefaultConfig();

        // (No content checks, no subdir) death_message.json:
        saveResource("death_message.json", false);
    }

    private String setDeathMessageTemplate() throws IOException {
        File file = new File(getDataFolder().getPath() + "/death_message.json");
        return Files.readString(file.toPath());
    }

    private void loadSpicord() {
        SpicordLoader.addStartupListener(spicord -> spicord.getAddonManager().registerAddon(new SimpleAddon("Yolo-Spicord", "yolo-deaths", "eingruenesbeb", "v0.3.0") {
            @Override
            public void onLoad(DiscordBot bot) {
                spicordBotAvailable = bot == null;
            }

            @Override
            public void onReady(DiscordBot bot) {
                spicordBot = bot;
                spicordBotAvailable = bot != null;
                if (!spicordBotAvailable) {
                    getLogger().warning(pluginResourceBundle.getString("loading.spicord.bot_unavailable"));
                } else {
                    getLogger().info(pluginResourceBundle.getString("loading.spicord.bot_available"));
                }
            }

            @Override
            public void onShutdown(DiscordBot bot) {
                spicordBotAvailable = false;
            }

            @Override
            public void onDisable() {
                spicordBotAvailable = false;
            }
        }));
    }
}
