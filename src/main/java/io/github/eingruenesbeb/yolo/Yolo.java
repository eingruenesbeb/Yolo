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

/**
 * A Bukkit plugin that adds features related to player deaths and Discord integration.
 * This is the main class of that plugin.
 */
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

    /**
     * Accessor for {@link Yolo#message_channel_id}
     * @return The id of the channel to send the death notifications to.
     */
    public String getMessage_channel_id() {
        return message_channel_id;
    }

    /**
     * Accessor for {@link Yolo#spicordBot}
     * @return (Spicord version of) The bot, that is used for sending the message, if provided.
     * @see DiscordBot
     */
    public DiscordBot getSpicordBot() {
        return spicordBot;
    }

    /**
     * Accessor for {@link Yolo#pluginResourceBundle}
     * @return The resource bundle used for localizing the plugin's messages.
     */
    public ResourceBundle getPluginResourceBundle() {
        return pluginResourceBundle;
    }

    /**
     * Accessor for {@link Yolo#useAB}
     * @return Whether the plugin can use AdvancedBan for banning players. Should only be true, if AdvancedBan is loaded.
     */
    public boolean isUseAB() {
        return useAB;
    }

    /**
     * Accessor for {@link Yolo#deathMessageTemplate}
     * @return The template configured in this plugin's data-folder as a String. Can be used to generate an embed from,
     * whilst replacing supported variables in it. (Currently only supports {@code %player_name%}.)
     */
    public String getDeathMessageTemplate() {
        return deathMessageTemplate;
    }

    /**
     * Accessor for {@link Yolo#addon}
     * @return The addon, that this plugin provides for Spicord to register.
     * @see SimpleAddon
     */
    public SpicordPlugin getAddon() {
        return addon;
    }

    /**
     * Accessor for {@link Yolo#spicordBotAvailable}
     * @return If the bot is available for sending a message.
     */
    public boolean isSpicordBotAvailable() {
        return spicordBotAvailable;
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

    /**
     * Private method for regenerating any missing file, if it doesn't exist.
     * @throws IOException Thrown, when the desired path is unavailable or another thing blocks writing to that
     * location/file.
     */
    private void regenerateMissingFiles() throws IOException {
        // Guarantee the existence of the data folder.
        //noinspection ResultOfMethodCallIgnored
        getDataFolder().mkdirs();

        // (No content checks, no subdir) config.yml:
        saveDefaultConfig();

        // (No content checks, no subdir) death_message.json:
        saveResource("death_message.json", false);
    }

    /**
     * Private method reading the configured message from the data directory.
     * @return The contents of the configured message, as a String.
     * @throws IOException If the file couldn't be read.
     */
    private String setDeathMessageTemplate() throws IOException {
        File file = new File(getDataFolder().getPath() + "/death_message.json");
        return Files.readString(file.toPath());
    }

    /**
     * Private method to register this plugin as an addon for Spicord and to set a few important fields for Spicord
     * support.
     */
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
