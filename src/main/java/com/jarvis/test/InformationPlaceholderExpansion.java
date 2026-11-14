package com.jarvis.test;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class InformationPlaceholderExpansion extends PlaceholderExpansion {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final Information plugin;

    public InformationPlaceholderExpansion(Information plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "information";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Jarvis";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        PlayerSnapshot snapshot = plugin.getPlayerDataManager().getSnapshotForPlaceholder(player);
        if (snapshot == null) {
            return "N/A";
        }

        return switch (params.toLowerCase(Locale.ROOT)) {
            case "status" -> player.isOnline() ? "ONLINE" : "OFFLINE";
            case "uuid" -> snapshot.uuid().toString();
            case "ip" -> snapshot.ip();
            case "previous_ip" -> snapshot.previousIp();
            case "op" -> snapshot.op() ? "Yes" : "No";
            case "banned" -> player.isBanned() ? "Yes" : "No";
            case "first_join" -> formatTimestamp(snapshot.firstPlayed());
            case "last_seen" -> player.isOnline() ? "Now" : formatTimestamp(snapshot.lastSeen());
            case "playtime" -> snapshot.formattedPlaytime();
            case "world" -> snapshot.world();
            case "dimension" -> snapshot.dimension();
            case "x" -> String.valueOf(snapshot.x());
            case "y" -> String.valueOf(snapshot.y());
            case "z" -> String.valueOf(snapshot.z());
            case "last_death" -> snapshot.lastDeath();
            case "spawn" -> snapshot.spawnLocation();
            case "health" -> String.valueOf(snapshot.health());
            case "food" -> String.valueOf(snapshot.food());
            case "level" -> String.valueOf(snapshot.xpLevel());
            case "ping" -> (player.isOnline() && player.getPlayer() != null)
                    ? String.valueOf(player.getPlayer().getPing())
                    : "N/A";
            case "gamemode" -> snapshot.gamemode();
            case "mob_kills" -> String.valueOf(snapshot.mobKills());
            case "deaths" -> String.valueOf(snapshot.deaths());
            case "pvp_wins" -> String.valueOf(snapshot.pvpWins());
            case "pvp_losses" -> String.valueOf(snapshot.pvpLosses());
            case "effects" -> snapshot.effects();
            default -> null;
        };
    }

    private String formatTimestamp(long millis) {
        if (millis <= 0L) {
            return "Unknown";
        }
        return DATE_FORMATTER.format(Instant.ofEpochMilli(millis));
    }
}