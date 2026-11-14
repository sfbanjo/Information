package com.jarvis.test;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Information extends JavaPlugin implements BasicCommand {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

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
                        sendMsg(sender, getConfig().getString("messages.no-permission", "&cYou do not have permission!"));
                        return;
                    }

                    if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                        reloadConfig();
                        sendMsg(sender, getConfig().getString("messages.reload-success", "&aConfiguration reloaded!"));
                    } else {
                        sendMsg(sender, getConfig().getString("messages.reload-usage", "&eCorrect usage: /information reload"));
                    }
                }

                @Override
                public Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
                    if (args.length <= 1) {
                        String prefix = (args.length == 1) ? args[0].toLowerCase(Locale.ROOT) : "";
                        if ("reload".startsWith(prefix)) {
                            return List.of("reload");
                        }
                    }
                    return List.of();
                }
            });
        });
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllOnlineSync();
        }
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
                sendMsg(sender, getConfig().getString("messages.usage", "&eUsage: /playerinfo <player>"));
                return;
            }
            targetName = sender.getName();
        } else {
            targetName = args[0];
        }

        boolean isSelfQuery = (sender instanceof Player && sender.getName().equalsIgnoreCase(targetName));
        boolean hasOthersPerm = !(sender instanceof Player) || sender.isOp() || sender.hasPermission(permOthers);
        boolean hasSelfPerm = hasOthersPerm || sender.hasPermission(permSelf);

        if (isSelfQuery) {
            if (!hasSelfPerm) {
                sendMsg(sender, getConfig().getString("messages.no-permission", "&cYou do not have permission!"));
                return;
            }
        } else if (!hasOthersPerm) {
            sendMsg(sender, getConfig().getString("messages.no-permission-other", "&cYou cannot view other players!"));
            return;
        }

        boolean restrictSelfView = isSelfQuery && !hasOthersPerm;
        Set<String> hiddenFields = new HashSet<>();
        if (restrictSelfView) {
            for (String field : getConfig().getStringList("settings.self-hidden-fields")) {
                hiddenFields.add(field.trim().toLowerCase(Locale.ROOT));
            }
        }

        Player targetOnline = Bukkit.getPlayerExact(targetName);
        if (targetOnline != null && targetOnline.isOnline()) {
            PlayerSnapshot snapshot = playerDataManager.captureOnlineSnapshot(targetOnline);
            sendFormattedInfo(sender, snapshot, true, targetOnline.isBanned(), hiddenFields);
            return;
        }

        PlayerSnapshot offlineSnapshot = playerDataManager.findSnapshotByName(targetName);
        if (offlineSnapshot == null) {
            String offlineMsg = getConfig().getString("messages.player-not-found", "&cThe player {player} has no recorded data.");
            sendMsg(sender, offlineMsg.replace("{player}", targetName));
            return;
        }

        boolean isBanned = Bukkit.getOfflinePlayer(offlineSnapshot.uuid()).isBanned();
        sendFormattedInfo(sender, offlineSnapshot, false, isBanned, hiddenFields);
    }

    private void sendFormattedInfo(CommandSender sender, PlayerSnapshot snapshot, boolean isOnline, boolean isBanned, Set<String> hiddenFields) {
        String statusStr = isOnline
                ? getConfig().getString("messages.info.status-online", "&aONLINE")
                : getConfig().getString("messages.info.status-offline", "&cOFFLINE");

        sendMsg(sender, getConfig().getString("messages.info.header", "&a--- Information about {player} ---").replace("{player}", snapshot.name()));

        sendInfoLine(sender, hiddenFields, "status", "&fStatus: {status}", "{status}", statusStr);
        if (!isOnline) {
            sendInfoLine(sender, hiddenFields, "last-seen", "&fLast Seen: &e{date}", "{date}", formatTimestamp(snapshot.lastSeen()));
        }
        sendInfoLine(sender, hiddenFields, "uuid", "&fUUID: &7{uuid}", "{uuid}", snapshot.uuid().toString());
        sendInfoLine(sender, hiddenFields, "ip", "&fIP Address: &4{ip}", "{ip}", snapshot.ip());
        sendInfoLine(sender, hiddenFields, "op-status", "&fOP Status: &e{op}", "{op}", snapshot.op() ? "Yes" : "No");
        sendInfoLine(sender, hiddenFields, "banned-status", "&fBanned: &c{banned}", "{banned}", isBanned ? "Yes" : "No");
        sendInfoLine(sender, hiddenFields, "first-join", "&fFirst Join: &e{date}", "{date}", formatTimestamp(snapshot.firstPlayed()));
        sendInfoLine(sender, hiddenFields, "playtime", "&fTotal Playtime: &a{playtime}", "{playtime}", snapshot.formattedPlaytime());
        sendInfoLine(sender, hiddenFields, "world", "&fWorld: &e{world}", "{world}", snapshot.world());
        sendInfoLine(sender, hiddenFields, "dimension", "&fDimension: &b{dimension}", "{dimension}", snapshot.dimension());
        sendInfoLine(sender, hiddenFields, "position", "&fPosition: X={x} Y={y} Z={z}",
                "{x}", String.valueOf(snapshot.x()),
                "{y}", String.valueOf(snapshot.y()),
                "{z}", String.valueOf(snapshot.z()));
        sendInfoLine(sender, hiddenFields, "last-death", "&fLast Death: &c{death}", "{death}", snapshot.lastDeath());
        sendInfoLine(sender, hiddenFields, "spawn-location", "&fSpawn Location: &e{spawn}", "{spawn}", snapshot.spawnLocation());
        sendInfoLine(sender, hiddenFields, "health", "&fHealth: &c{health}/20", "{health}", String.valueOf(snapshot.health()));
        sendInfoLine(sender, hiddenFields, "food", "&fFood: &6{food}/20", "{food}", String.valueOf(snapshot.food()));
        sendInfoLine(sender, hiddenFields, "xp-level", "&fXP Level: &b{level}", "{level}", String.valueOf(snapshot.xpLevel()));
        sendInfoLine(sender, hiddenFields, "ping", "&fPing: &d{ping} ms", "{ping}", isOnline ? String.valueOf(snapshot.ping()) : "N/A");
        sendInfoLine(sender, hiddenFields, "gamemode", "&fGameMode: &9{gamemode}", "{gamemode}", snapshot.gamemode());
        sendInfoLine(sender, hiddenFields, "mob-kills", "&fMob Kills: &a{mob_kills}", "{mob_kills}", String.valueOf(snapshot.mobKills()));
        sendInfoLine(sender, hiddenFields, "deaths", "&fTotal Deaths: &c{deaths}", "{deaths}", String.valueOf(snapshot.deaths()));
        sendInfoLine(sender, hiddenFields, "pvp-wins", "&fPvP Wins: &e{pvp_wins}", "{pvp_wins}", String.valueOf(snapshot.pvpWins()));
        sendInfoLine(sender, hiddenFields, "pvp-losses", "&fPvP Losses: &4{pvp_losses}", "{pvp_losses}", String.valueOf(snapshot.pvpLosses()));
        sendInfoLine(sender, hiddenFields, "effects", "&fActive Effects: &d{effects}", "{effects}", snapshot.effects());
    }

    private void sendInfoLine(CommandSender sender, Set<String> hiddenFields, String key, String defaultMsg, String... replacements) {
        if (hiddenFields.contains(key)) {
            return;
        }
        String msg = getConfig().getString("messages.info." + key, defaultMsg);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        sendMsg(sender, msg);
    }

    private void sendMsg(CommandSender sender, String rawMessage) {
        Component component = LEGACY_SERIALIZER.deserialize(rawMessage);
        sender.sendMessage(component);
    }

    private String formatTimestamp(long millis) {
        if (millis <= 0L) {
            return "Unknown";
        }
        return DATE_FORMATTER.format(Instant.ofEpochMilli(millis));
    }

    @Override
    public Collection<String> suggest(@NotNull CommandSourceStack commandSourceStack, @NotNull String[] args) {
        if (args.length > 1) {
            return List.of();
        }

        CommandSender sender = commandSourceStack.getSender();
        String permSelf = getConfig().getString("settings.permission-self", "information.self");
        String permOthers = getConfig().getString("settings.permission-others", "information.playerinfo");
        String prefix = (args.length == 1) ? args[0].toLowerCase(Locale.ROOT) : "";

        boolean canViewOthers = !(sender instanceof Player) || sender.isOp() || sender.hasPermission(permOthers);
        if (!canViewOthers) {
            if (sender.hasPermission(permSelf) && sender.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                return List.of(sender.getName());
            }
            return List.of();
        }

        Set<String> suggestions = new LinkedHashSet<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                suggestions.add(online.getName());
            }
        }
        for (String known : playerDataManager.getKnownPlayerNames()) {
            if (known.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                suggestions.add(known);
            }
        }
        return suggestions;
    }
}