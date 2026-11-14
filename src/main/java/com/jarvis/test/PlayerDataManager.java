package com.jarvis.test;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PlayerDataManager {

    private final Information plugin;
    private final File dataFolder;

    public PlayerDataManager(Information plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void savePlayerDataAsync(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerData(player));
    }

    public void savePlayerData(Player player) {
        File playerFile = new File(dataFolder, player.getUniqueId() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        config.set("name", player.getName());
        config.set("uuid", player.getUniqueId().toString());

        InetSocketAddress socketAddress = player.getAddress();
        String ipAddress = (socketAddress != null) ? socketAddress.getAddress().getHostAddress() : "Unknown";
        config.set("ip", ipAddress);

        config.set("op", player.isOp());
        config.set("banned", player.isBanned());
        config.set("first-played", player.getFirstPlayed());
        config.set("last-seen", System.currentTimeMillis());

        config.set("playtime-seconds", player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20);
        config.set("mob-kills", player.getStatistic(Statistic.MOB_KILLS));
        config.set("deaths", player.getStatistic(Statistic.DEATHS));
        config.set("pvp-wins", player.getStatistic(Statistic.PLAYER_KILLS));

        if (!config.contains("pvp-losses")) {
            config.set("pvp-losses", 0);
        }

        Location loc = player.getLocation();
        if (loc.getWorld() != null) {
            config.set("world", loc.getWorld().getName());
            config.set("dimension", loc.getWorld().getEnvironment().name());
            config.set("x", (int) loc.getX());
            config.set("y", (int) loc.getY());
            config.set("z", (int) loc.getZ());
        }

        Location deathLoc = player.getLastDeathLocation();
        if (deathLoc != null && deathLoc.getWorld() != null) {
            config.set("last-death", deathLoc.getWorld().getName() + " (X=" + (int)deathLoc.getX() + " Y=" + (int)deathLoc.getY() + " Z=" + (int)deathLoc.getZ() + ")");
        } else {
            config.set("last-death", "None");
        }

        Location spawnLoc = player.getBedSpawnLocation();
        if (spawnLoc != null && spawnLoc.getWorld() != null) {
            config.set("spawn-location", spawnLoc.getWorld().getName() + " (X=" + (int)spawnLoc.getX() + " Y=" + (int)spawnLoc.getY() + " Z=" + (int)spawnLoc.getZ() + ")");
        } else {
            config.set("spawn-location", "Not set");
        }

        config.set("health", (int) player.getHealth());
        config.set("food", player.getFoodLevel());
        config.set("xp-level", player.getLevel());
        config.set("ping", player.getPing());
        config.set("gamemode", player.getGameMode().name());

        Collection<PotionEffect> activeEffects = player.getActivePotionEffects();
        if (!activeEffects.isEmpty()) {
            List<String> effectList = new ArrayList<>();
            for (PotionEffect effect : activeEffects) {
                String effectName = effect.getType().getKey().getKey().replace('_', ' ');
                int amplifier = effect.getAmplifier() + 1;
                effectList.add(effectName + " " + amplifier);
            }
            config.set("effects", String.join(", ", effectList));
        } else {
            config.set("effects", "None");
        }

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save user data for " + player.getName());
        }
    }

    public FileConfiguration getPlayerData(UUID uuid) {
        File playerFile = new File(dataFolder, uuid + ".yml");
        if (!playerFile.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(playerFile);
    }

    public void incrementPvpLosses(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File playerFile = new File(dataFolder, uuid + ".yml");
            if (playerFile.exists()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
                int currentLosses = config.getInt("pvp-losses", 0);
                config.set("pvp-losses", currentLosses + 1);
                try {
                    config.save(playerFile);
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not update pvp losses for UUID " + uuid);
                }
            }
        });
    }
}