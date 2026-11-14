package com.jarvis.test;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class InformationPlaceholderExpansion extends PlaceholderExpansion {

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
        return "1.6";
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

        UUID uuid = player.getUniqueId();

        if (player.isOnline() && player.getPlayer() != null) {
            plugin.getPlayerDataManager().savePlayerData(player.getPlayer());
        }

        FileConfiguration data = plugin.getPlayerDataManager().getPlayerData(uuid);
        if (data == null) {
            return "N/A";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        switch (params.toLowerCase()) {
            case "status":
                return player.isOnline() ? "ONLINE" : "OFFLINE";
            case "uuid":
                return data.getString("uuid", player.getUniqueId().toString());
            case "ip":
                return data.getString("ip", "Unknown");
            case "op":
                return data.getBoolean("op", false) ? "Yes" : "No";
            case "banned":
                return data.getBoolean("banned", false) ? "Yes" : "No";
            case "first_join":
                return sdf.format(new Date(data.getLong("first-played", 0)));
            case "last_seen":
                return player.isOnline() ? "Now" : sdf.format(new Date(data.getLong("last-seen", System.currentTimeMillis())));
            case "playtime":
                long totalSeconds = data.getLong("playtime-seconds", 0);
                return (totalSeconds / 86400) + "d " + ((totalSeconds % 86400) / 3600) + "h " + ((totalSeconds % 3600) / 60) + "m";
            case "world":
                return data.getString("world", "Unknown");
            case "dimension":
                return data.getString("dimension", "Unknown");
            case "x":
                return String.valueOf(data.getInt("x", 0));
            case "y":
                return String.valueOf(data.getInt("y", 0));
            case "z":
                return String.valueOf(data.getInt("z", 0));
            case "last_death":
                return data.getString("last-death", "None");
            case "spawn":
                return data.getString("spawn-location", "Not set");
            case "health":
                return String.valueOf(data.getInt("health", 0));
            case "food":
                return String.valueOf(data.getInt("food", 0));
            case "level":
                return String.valueOf(data.getInt("xp-level", 0));
            case "ping":
                return (player.isOnline() && player.getPlayer() != null) ? String.valueOf(player.getPlayer().getPing()) : "N/A";
            case "gamemode":
                return data.getString("gamemode", "UNKNOWN");
            case "mob_kills":
                return String.valueOf(data.getInt("mob-kills", 0));
            case "deaths":
                return String.valueOf(data.getInt("deaths", 0));
            case "pvp_wins":
                return String.valueOf(data.getInt("pvp-wins", 0));
            case "pvp_losses":
                return String.valueOf(data.getInt("pvp-losses", 0));
            case "effects":
                return data.getString("effects", "None");
            default:
                return null;
        }
    }
}