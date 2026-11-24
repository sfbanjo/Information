# INFORMATION
a Paper plugin to show player's information

This is the first public release of my Information plugin.
It is a Paper/Purpur plugin that displays main player information.
It is permission-based: players with permission can check their own information, while admins can view information for any player (online or offline) who has joined the server.
Every field supports placeholders.
### Dependency: PlaceholderAPI

## **Permissions**

| Permission | Description |
| :--- | :--- |
| information.self | Allow to execute command /playerinfo and show self statistics |
| information.playerinfo | Allow admins to execute command /playerinfo on all players, online and offline |
| information.admin | Allow to execute the command /information reload |


## **Available placeholders**

| Placeholder | Description |
| :--- | :--- |
| `%information_status%` | ONLINE/OFFLINE status |
| `%information_uuid%` | UUID of player |
| `%information_ip%` | Last IP of player |
| `%information_previous_ip%` | Previous IP of player (if different) |
| `%information_op%` | Is player OP? |
| `%information_banned%` | Ban status |
| `%information_first_join%` | First join date |
| `%information_last_seen%` | Last seen date/time |
| `%information_playtime%` | Total server playtime |
| `%information_world%` | Name of the world |
| `%information_dimension%` | Dimension |
| `%information_x%` | X |
| `%information_y%` | Y |
| `%information_z%` | Z |
| `%information_last_death%` | Last death point |
| `%information_spawn%` | Spawn/bed position |
| `%information_health%` | Health |
| `%information_food%` | Hunger |
| `%information_level%` | XP level |
| `%information_ping%` | Latency ms |
| `%information_gamemode%` | Last/actual gamemode |
| `%information_mob_kills%` | Total mob kills |
| `%information_pvp_wins%` | PVP wins against player |
| `%information_pvp_losses%` | PVP losses against player |
| `%information_effects%` | Potion effects |

