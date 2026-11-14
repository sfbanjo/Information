package com.jarvis.test;

import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record PlayerSnapshot(
        UUID uuid,
        String name,
        String ip,
        String previousIp,
        boolean op,
        boolean banned,
        long firstPlayed,
        long lastSeen,
        long playtimeSeconds,
        String world,
        String dimension,
        int x,
        int y,
        int z,
        String lastDeath,
        String spawnLocation,
        int health,
        int food,
        int xpLevel,
        int ping,
        String gamemode,
        int mobKills,
        int deaths,
        int pvpWins,
        int pvpLosses,
        String effects
) {

    public static PlayerSnapshot fromPlayer(Player player, PlayerSnapshot existing, int pvpLosses) {
        InetSocketAddress socketAddress = player.getAddress();
        String ipAddress = (socketAddress != null && socketAddress.getAddress() != null)
                ? socketAddress.getAddress().getHostAddress()
                : "Unknown";

        String resolvedPreviousIp = "None";
        if (existing != null) {
            String lastSavedIp = existing.ip();
            if (lastSavedIp != null
                    && !lastSavedIp.equals("Unknown")
                    && !ipAddress.equals("Unknown")
                    && !lastSavedIp.equals(ipAddress)) {
                resolvedPreviousIp = lastSavedIp;
            } else if (existing.previousIp() != null) {
                resolvedPreviousIp = existing.previousIp();
            }
        }

        Location loc = player.getLocation();
        String worldName = (loc.getWorld() != null) ? loc.getWorld().getName() : "Unknown";
        String dimensionName = (loc.getWorld() != null) ? loc.getWorld().getEnvironment().name() : "Unknown";

        String lastDeathStr = formatLocation(player.getLastDeathLocation(), "None");
        String spawnStr = formatLocation(player.getRespawnLocation(), "Not set");

        Collection<PotionEffect> activeEffects = player.getActivePotionEffects();
        String effectsStr;
        if (activeEffects.isEmpty()) {
            effectsStr = "None";
        } else {
            List<String> effectList = new ArrayList<>();
            for (PotionEffect effect : activeEffects) {
                String effectName = effect.getType().getKey().getKey().replace('_', ' ');
                int amplifier = effect.getAmplifier() + 1;
                effectList.add(effectName + " " + amplifier);
            }
            effectsStr = String.join(", ", effectList);
        }

        return new PlayerSnapshot(
                player.getUniqueId(),
                player.getName(),
                ipAddress,
                resolvedPreviousIp,
                player.isOp(),
                player.isBanned(),
                player.getFirstPlayed(),
                System.currentTimeMillis(),
                player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L,
                worldName,
                dimensionName,
                (int) loc.getX(),
                (int) loc.getY(),
                (int) loc.getZ(),
                lastDeathStr,
                spawnStr,
                (int) player.getHealth(),
                player.getFoodLevel(),
                player.getLevel(),
                player.getPing(),
                player.getGameMode().name(),
                player.getStatistic(Statistic.MOB_KILLS),
                player.getStatistic(Statistic.DEATHS),
                player.getStatistic(Statistic.PLAYER_KILLS),
                pvpLosses,
                effectsStr
        );
    }

    public boolean hasDifferentPreviousIp() {
        return previousIp != null
                && !previousIp.isBlank()
                && !previousIp.equals("None")
                && !previousIp.equals("Unknown")
                && !previousIp.equals(ip);
    }

    public String formattedPlaytime() {
        long days = playtimeSeconds / 86400;
        long hours = (playtimeSeconds % 86400) / 3600;
        long minutes = (playtimeSeconds % 3600) / 60;
        return days + "d " + hours + "h " + minutes + "m";
    }

    private static String formatLocation(Location loc, String fallback) {
        if (loc == null || loc.getWorld() == null) {
            return fallback;
        }
        return loc.getWorld().getName()
                + " (X=" + (int) loc.getX()
                + " Y=" + (int) loc.getY()
                + " Z=" + (int) loc.getZ() + ")";
    }
}