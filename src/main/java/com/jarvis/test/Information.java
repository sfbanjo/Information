package com.jarvis.test;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class Information extends JavaPlugin implements BasicCommand {

    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        this.playerDataManager = new PlayerDataManager(this);
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);

        // Inizializzazione sicura di PlaceholderAPI
        PlaceholderManager.registerExpansion(this);

        getLogger().info("Information successfully enabled on Paper 26.3!");

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("playerinfo", "Displays advanced information about a player", List.of("pinfo"), this);
            event.registrar().register("information", "Main commands for Information plugin", List.of("info"), new BasicCommand() {
                @Override
                public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
                    CommandSender sender = stack.getSender();
                    String adminPermission = getConfig().getString("settings.admin-permission", "information.admin");

                    if (sender instanceof Player && !sender.isOp() && !sender.hasPermission(adminPermission)) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.no-permission", "&cYou do not have permission!")));
                        return;
                    }

                    if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                        reloadConfig();
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.reload-success", "&aConfiguration reloaded!")));
                    } else {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.reload-usage", "&eCorrect usage: /information reload")));
                    }
                }

                @Override
                public Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
                    if (args.length == 1) return List.of("reload");
                    return List.of();
                }
            });
        });
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    @Override
    public void execute(@NotNull CommandSourceStack commandSourceStack, @NotNull String[] args) {
        CommandSender sender = commandSourceStack.getSender();

        String permSelf = getConfig().getString("settings.permission-self", "information.self");
        String permOthers = getConfig().getString("settings.permission-others", "information.playerinfo");

        String targetName;

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.usage", "&eUsage: /playerinfo <player>")));
                return;
            }
            targetName = sender.getName();
        } else {
            targetName = args[0];
        }

        boolean isSelfQuery = (sender instanceof Player && sender.getName().equalsIgnoreCase(targetName));

        // Controllo permessi granulari
        if (sender instanceof Player && !sender.isOp()) {
            if (isSelfQuery) {
                if (!sender.hasPermission(permSelf) && !sender.hasPermission(permOthers)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.no-permission", "&cYou do not have permission!")));
                    return;
                }
            } else {
                if (!sender.hasPermission(permOthers)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.no-permission-other", "&cYou cannot view other players!")));
                    return;
                }
            }
        }

        Player targetOnline = Bukkit.getPlayerExact(targetName);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        // --- PLAYER ONLINE ---
        if (targetOnline != null && targetOnline.isOnline()) {
            playerDataManager.savePlayerDataAsync(targetOnline);

            UUID uuid = targetOnline.getUniqueId();
            InetSocketAddress socketAddress = targetOnline.getAddress();
            String ipAddress = (socketAddress != null) ? socketAddress.getAddress().getHostAddress() : "Unknown";

            Location loc = targetOnline.getLocation();
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "Unknown";
            String dimension = loc.getWorld() != null ? loc.getWorld().getEnvironment().name() : "Unknown";

            Location deathLoc = targetOnline.getLastDeathLocation();
            String deathStr = (deathLoc != null && deathLoc.getWorld() != null) ? deathLoc.getWorld().getName() + " (X=" + (int)deathLoc.getX() + " Y=" + (int)deathLoc.getY() + " Z=" + (int)deathLoc.getZ() + ")" : "None";

            Location spawnLoc = targetOnline.getBedSpawnLocation();
            String spawnStr = (spawnLoc != null && spawnLoc.getWorld() != null) ? spawnLoc.getWorld().getName() + " (X=" + (int)spawnLoc.getX() + " Y=" + (int)spawnLoc.getY() + " Z=" + (int)spawnLoc.getZ() + ")" : "Not set";

            long totalSeconds = targetOnline.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
            String formattedPlaytime = (totalSeconds / 86400) + "d " + ((totalSeconds % 86400) / 3600) + "h " + ((totalSeconds % 3600) / 60) + "m";

            int mobKills = targetOnline.getStatistic(Statistic.MOB_KILLS);
            int deaths = targetOnline.getStatistic(Statistic.DEATHS);
            int pvpWins = targetOnline.getStatistic(Statistic.PLAYER_KILLS);

            FileConfiguration currentData = playerDataManager.getPlayerData(uuid);
            int pvpLosses = currentData != null ? currentData.getInt("pvp-losses", 0) : 0;

            Collection<PotionEffect> activeEffects = targetOnline.getActivePotionEffects();
            List<String> effectList = new ArrayList<>();
            for (PotionEffect effect : activeEffects) {
                effectList.add(effect.getType().getKey().getKey().replace('_', ' ') + " " + (effect.getAmplifier() + 1));
            }
            String effectsStr = effectList.isEmpty() ? "None" : String.join(", ", effectList);

            sendFormattedInfo(sender, targetOnline.getName(), getConfig().getString("messages.info.status-online", "&aONLINE"),
                    sdf.format(new Date()), uuid.toString(), ipAddress, targetOnline.isOp() ? "Yes" : "No", targetOnline.isBanned() ? "Yes" : "No",
                    sdf.format(new Date(targetOnline.getFirstPlayed())), formattedPlaytime, worldName, dimension,
                    String.valueOf((int)loc.getX()), String.valueOf((int)loc.getY()), String.valueOf((int)loc.getZ()),
                    deathStr, spawnStr, String.valueOf((int)targetOnline.getHealth()), String.valueOf(targetOnline.getFoodLevel()),
                    String.valueOf(targetOnline.getLevel()), String.valueOf(targetOnline.getPing()), targetOnline.getGameMode().name(),
                    String.valueOf(mobKills), String.valueOf(deaths), String.valueOf(pvpWins), String.valueOf(pvpLosses), effectsStr);
            return;
        }

        // --- PLAYER OFFLINE ---
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(targetName);
        UUID targetUUID = (offlinePlayer != null) ? offlinePlayer.getUniqueId() : null;
        FileConfiguration data = (targetUUID != null) ? playerDataManager.getPlayerData(targetUUID) : null;

        if (data == null) {
            String offlineMsg = getConfig().getString("messages.player-not-found", "&cThe player {player} has no recorded data.");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', offlineMsg.replace("{player}", targetName)));
            return;
        }

        long totalSeconds = data.getLong("playtime-seconds", 0);
        String formattedPlaytime = (totalSeconds / 86400) + "d " + ((totalSeconds % 86400) / 3600) + "h " + ((totalSeconds % 3600) / 60) + "m";
        String lastSeenStr = sdf.format(new Date(data.getLong("last-seen", System.currentTimeMillis())));

        sendFormattedInfo(sender, data.getString("name", targetName), getConfig().getString("messages.info.status-offline", "&cOFFLINE"),
                lastSeenStr, data.getString("uuid", "Unknown"), data.getString("ip", "Unknown"),
                data.getBoolean("op", false) ? "Yes" : "No", data.getBoolean("banned", false) ? "Yes" : "No",
                sdf.format(new Date(data.getLong("first-played", 0))), formattedPlaytime,
                data.getString("world", "Unknown"), data.getString("dimension", "Unknown"),
                String.valueOf(data.getInt("x", 0)), String.valueOf(data.getInt("y", 0)), String.valueOf(data.getInt("z", 0)),
                data.getString("last-death", "None"), data.getString("spawn-location", "Not set"),
                String.valueOf(data.getInt("health", 0)), String.valueOf(data.getInt("food", 0)),
                String.valueOf(data.getInt("xp-level", 0)), "N/A", data.getString("gamemode", "UNKNOWN"),
                String.valueOf(data.getInt("mob-kills", 0)), String.valueOf(data.getInt("deaths", 0)),
                String.valueOf(data.getInt("pvp-wins", 0)), String.valueOf(data.getInt("pvp-losses", 0)),
                data.getString("effects", "None"));
    }

    private void sendFormattedInfo(CommandSender sender, String name, String status, String lastSeen, String uuid, String ip,
                                   String op, String banned, String firstJoin, String playtime, String world, String dimension,
                                   String x, String y, String z, String death, String spawn, String health, String food,
                                   String level, String ping, String gamemode, String mobKills, String deaths, String pvpWins,
                                   String pvpLosses, String effects) {

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.header", "&a--- Information about {player} ---").replace("{player}", name)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.status", "&fStatus: {status}").replace("{status}", status)));
        if (status.contains("OFFLINE")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.last-seen", "&fLast Seen: &e{date}").replace("{date}", lastSeen)));
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.uuid", "&fUUID: &7{uuid}").replace("{uuid}", uuid)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.ip", "&fIP Address: &4{ip}").replace("{ip}", ip)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.op-status", "&fOP Status: &e{op}").replace("{op}", op)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.banned-status", "&fBanned: &c{banned}").replace("{banned}", banned)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.first-join", "&fFirst Join: &e{date}").replace("{date}", firstJoin)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.playtime", "&fTotal Playtime: &a{playtime}").replace("{playtime}", playtime)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.world", "&fWorld: &e{world}").replace("{world}", world)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.dimension", "&fDimension: &b{dimension}").replace("{dimension}", dimension)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.position", "&fPosition: X={x} Y={y} Z={z}").replace("{x}", x).replace("{y}", y).replace("{z}", z)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.last-death", "&fLast Death: &c{death}").replace("{death}", death)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.spawn-location", "&fSpawn Location: &e{spawn}").replace("{spawn}", spawn)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.health", "&fHealth: &c{health}/20").replace("{health}", health)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.food", "&fFood: &6{food}/20").replace("{food}", food)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.xp-level", "&fXP Level: &b{level}").replace("{level}", level)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.ping", "&fPing: &d{ping} ms").replace("{ping}", ping)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.gamemode", "&fGameMode: &9{gamemode}").replace("{gamemode}", gamemode)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.mob-kills", "&fMob Kills: &a{mob_kills}").replace("{mob_kills}", mobKills)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.deaths", "&fTotal Deaths: &c{deaths}").replace("{deaths}", deaths)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.pvp-wins", "&fPvP Wins: &e{pvp_wins}").replace("{pvp_wins}", pvpWins)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.pvp-losses", "&fPvP Losses: &4{pvp_losses}").replace("{pvp_losses}", pvpLosses)));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.info.effects", "&fActive Effects: &d{effects}").replace("{effects}", effects)));
    }

    @Override
    public Collection<String> suggest(@NotNull CommandSourceStack commandSourceStack, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return playerNames;
        }
        return List.of();
    }
}