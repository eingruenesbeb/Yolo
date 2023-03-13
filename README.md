# Yolo - A hardcore plugin

---

[![](https://jitpack.io/v/eingruenesbeb/Yolo.svg)](https://jitpack.io/#eingruenesbeb/Yolo)

---

## 1. What does this plugin do?

You only live once... the name says it all.

This plugin is meant for hardcore survival servers, that implements, but also expands on the vanilla hardcore 
mechanics for a player's death on a server.\
Upon death players usually get banned off of a server, if (and only if) the setting for hardcore was enabled in 
`server.properties` before world generation. That is not the case, if this setting was set to true retroactively. And 
here is, where this plugin comes into play, by implementing this behaviour regardless of when hardcore was enabled. If 
you also have [AdvancedBan](https://www.spigotmc.org/resources/advancedban.8695/) installed, this plugin will 
automatically use this for banning players. 
Additionally, you can exempt players from this rule and send a customized message to a Discord-server upon occurrence of 
a (non-exempt) player-death. The plugin will also force a resource-pack, replacing the normal health-bar with 
hardcore-hearts, on any non-exempt player. (*You can check out the resource-pack 
[here](https://drive.google.com/file/d/1UWoiOGFlt2QIyQPVKAv5flLTNeNiI439/view?usp=share_link).*)

## 2. Configuration:

### 2.1 `config.yml`

There are currently two configuration sections.\
The first one is for managing the resource-pack sent to non-exempt players:
```yaml
# You can specify a custom resource pack and turn off forcing the pack onto non-exempt players.
resource-pack:
  force: true
  custom:
    use: false
    url: ""
    sha1: ""
```
- The `force` option lets you control, whether the player *has* to accept the pack. (Allowed values: `true`, `false`)
- `custom` lets you define a custom resource-pack, other than the default one.
  - `use`: Set to `true` to use the custom pack, otherwise set it to `false`
  - `url`: The download-link to the custom pack.
  - `sha1`: The sha1-checksum of the file. You can use 
  [this tool](https://emn178.github.io/online-tools/sha1_checksum.html) to obtain it.

The next section is for configuring the Spicord integration:
```yaml
spicord:
    send: false
    message_channel_id: ""
```
- `send`: Whether to enable the Spicord-Integration. (Allowed values: `true`, `false`)
- `message_channel_id`: The id of the channel to send the message to. You can obtain it using 
[this method](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-) (you
can also obtain the channel-id by itself, by right-clicking the channel on the channel-selection-sidebar).

Lastly, you can individually disable certain messages:
```yaml
announce:
  death:
    discord: false
    chat: false
  totem:
    discord: false
    chat: false
```
This should be pretty self-explanatory. The listed messages are announcements, that would be sent on their respective 
occasions and targets. The allowed values are `true` or `false`.

### 2.2 `chat_messages.properties`

The messages defined in this file, are chat messages, that can be customized. There should not be a need to add more. 
You can also use the MiniMessage format, to make them *special*. How you can use this format, you can read 
[here](https://docs.advntr.dev/minimessage/format.html).

### 2.3 `discord/[...].json`
In these files, you can customize the embeds, that are sent by the plugin. You can use 
[this tool](https://leovoel.github.io/embed-visualizer/), to visualize them and generate the json file.

## 3. Permissions:

The one and currently only permission is `yolo.exempt`, which excludes any players having it from the plugin's effects.
For a general guide to permissions on Bukkit, please refer to 
[this page](https://bukkit.fandom.com/wiki/Permissions.yml). *(I'd recommend using a permission manager like 
[LuckPerms](https://luckperms.net/) though.)*

## 4. Further considerations:

- If hardcore is enabled in `server.properties`, the resource pack is always forced upon players by default. (configurable since: 0.4.0)
- If you use AdvancedBan, you can customize the message-layout, by creating one called "Hardcore_death". Please refer 
to [the Spigot page of AdvancedBan](https://www.spigotmc.org/resources/advancedban.8695/#Configuration) to find out, 
how to do that.
- In order to use Spicord, you have to configure it correctly. In order to register this plugin as an addon for a bot, 
you have to add it like this:
```toml
### ↑ Irrelevant stuff above ↑ ###
[[bots]]
  name = "" # Name for the bot here.
  enabled = true
  token = "" # Your bot's token here
  command_support = true # Irrelevant for this plugin's Spicord addon.
  command_prefix = "-"
  addons = [
    # Potentially other addons.
    "yolo"
  ]
### ↓ Other irrelevant stuff below ↓ ###
```
- **This plugin was made for PaperMC-1.19.3.** Other versions and platforms haven't been tested *yet*, but this is planned 
to be completed until the full release. Issues for incompatibility can be [submitted](https://github.com/eingruenesbeb/Yolo/issues/new/choose), but will not be resolved, if 
this would mean dropping support for other compatible versions.
- This is a pre-release version, so bugs or glitches may be encountered more often.

## 5. Planned features:
- Config option to enable on servers, that aren't explicitly set to hardcore
- `/revive` command to undo a player-ban by this plugin
- `/reload` command to reload this plugin on its own
- Support for more ban-manager-plugins
- Have a suggestion? Submit it [here](https://github.com/eingruenesbeb/Yolo/issues/new/choose)!

---
*From Version: 0.5.0*
