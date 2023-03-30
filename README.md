# Yolo - A hardcore plugin

---

[![Discord](https://img.shields.io/discord/1085932576197316629?color=7289da&label=Support&logo=discord&style=for-the-badge)](https://discord.gg/zMqkeRseME)
[![GitHub all releases](https://img.shields.io/github/downloads/eingruenesbeb/Yolo/total?logo=github&style=for-the-badge)](https://github.com/eingruenesbeb/Yolo/releases/)
[![Modrinth downloads](https://img.shields.io/modrinth/dt/ExWUwvY3?logo=modrinth&style=for-the-badge)](https://modrinth.com/plugin/yolo)
[![JitPack](https://img.shields.io/jitpack/version/io.github.eingruenesbeb/Yolo?color=green&style=for-the-badge)](https://jitpack.io/#eingruenesbeb/Yolo/v0.5.0)
[![GitHub](https://img.shields.io/github/license/eingruenesbeb/Yolo?style=for-the-badge)](https://github.com/eingruenesbeb/Yolo/blob/master/COPYING)

---

## 1. What does this plugin do?

You only live once... the name says it all.

This plugin is meant for hardcore survival servers, that implements, but also expands on the vanilla hardcore
mechanics for a player's death on a server.\
Upon death players usually get banned off of a server, if (and only if) the setting for hardcore was enabled in
`server.properties` before world generation. That is not the case, if this setting was set to true retroactively. And
here is, where this plugin comes into play, by implementing this behaviour regardless of when hardcore was enabled. 
Additionally, you can exempt players from this rule and send a customized message to a Discord-server upon occurrence of
a (non-exempt) player-death. The plugin can also force a resource-pack of your choice, with the default one replacing
the normal health-bar with hardcore-hearts, on any non-exempt player. (*You can check out the resource-pack
[here](https://drive.google.com/file/d/1UWoiOGFlt2QIyQPVKAv5flLTNeNiI439/view?usp=share_link).*)

## 2. Configuration

### 2.1 `config.yml`

There are currently four configuration sections.\
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
  [this method](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-) (
  you
  can also obtain the channel-id by itself, by right-clicking the channel on the channel-selection-sidebar).

In the `announce` section you can individually toggle messages:

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

The option `enable-on-non-hc` lets you enable this plugin's functionality even on a non-hardcore server, while the last
option `easy-disable` lets you disable the death-ban functionality, if it's `true` (previously dead players can join, if
this is enabled). It may take a while, before banned players can join, if this option is true. Yet again acceptable 
values (for both) are `true` and `false`.

### 2.2 `chat_messages.properties`

The messages defined in this file, are chat messages, that can be customized. There should not be a need to add more.
You can also use the MiniMessage format, to make them *special*. How you can use this format, you can read
[here](https://docs.advntr.dev/minimessage/format.html).

### 2.3 `discord/[...].json`

In these files, you can customize the embeds, that are sent by the plugin. You can use
[this tool](https://leovoel.github.io/embed-visualizer/), to visualize them and generate the json file.\
If for example you'd like to customize the discord message, that gets send upon a player's death, you'd need to modify
`plugins/yolo/discord/death_message.json` to something like this:

```json
{
  "embed": {
    "title": "%player_name% has died!",
    "description": "Press \"F\" to pay respect.",
    "color": 16521991,
    "author": {
      "name": "author name",
      "url": "https://discordapp.com",
      "icon_url": "https://cdn.modrinth.com/data/ExWUwvY3/a8ab710896279f5c3ed9c377d408a10587f5509d.png"
    },
    "image": {
      "url": "https://media.tenor.com/bMbIAroA0PkAAAAi/rip-rest.gif"
    }
  }
}

```

### 2.4 `ban_message.txt`
This is, where you can customize the ban message, players see, when they are dead.

## 3. Permissions

One permission is `yolo.exempt`, which excludes any players having it from the plugin's effects. There are also two
other permissions, that relate to commands. These are described in the section about [commands](#4-commands).\
For a general guide to permissions on Bukkit and its derivatives, please refer to
[this page](https://bukkit.fandom.com/wiki/Permissions.yml). *(I'd recommend using a permission manager like
[LuckPerms](https://luckperms.net/) though.)*

## 4. Commands

There are currently two commands available:

- `/yolo-reload`: This command is useful for reloading all configuration dependent features of this plugin, without
  having to restart the whole server. It requires the `yolo.reload` permission.
- `/revive [revivable player] <restore inventory> <safe teleport to death location>`: This command lets you revive a
player who has died, attempting to restore their inventory and teleporting them to their death location (if safe). It
requires the `yolo.revive` permission. You can also disable restoring their inventory and the teleportation, by setting
the arguments to `false`. (If you only want to disable the teleport, you need to explicitly set the inventory restoring
to `true`.)
- `/checkout_death_location` This command lets you teleport to a dead player's death location. This is useful for
checking for traps and duplicate items. It requires the `yolo.revive` permission.

## 5. Further considerations

- If hardcore is enabled in `server.properties`, the resource pack is always forced upon players by default. (
  configurable since: 0.4.0)
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

- **This plugin was made for PaperMC-1.19.4.** Other versions and platforms haven't been tested *yet*, but this is
  planned
  to be completed until the full release. Issues for incompatibility can
  be [submitted](https://github.com/eingruenesbeb/Yolo/issues/new/choose), but will not be resolved, if
  this would mean dropping support for other compatible versions.
- Reviving a player with inventory restoring enabled, can lead to item duplication, as players still drop their items.
You may want to check out the teleport location with `/checkout_death_location` first, in order to check for duplicates.
- This is a pre-release version, so bugs or glitches may be encountered more often.

## 6. Planned features

- Commands to undo a player's revive
- GUI for the revive system (planned for 2.0)
- Full support for PlaceholderAPI (planned for 2.0)
- Death location marker on Dynmap, Bluemap and co. (planned for in 2.0)
- Have a suggestion? Submit it [here](https://github.com/eingruenesbeb/Yolo/issues/new/choose)!

---

*From Version: 0.6.0*
