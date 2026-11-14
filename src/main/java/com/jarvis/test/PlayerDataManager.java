package com.jarvis.test;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final Information plugin;
    private final File dataFolder;

    private final Map<UUID, PlayerSnapshot> cache = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameIndex = new ConcurrentHashMap<>();
    private final Map<UUID, Object> fileLocks = new ConcurrentHashMap<>();

    public PlayerDataManager(Information plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        loadAllFromDisk();
    }

    private void loadAllFromDisk() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                String rawName = file.getName().replace(".yml", "");
                UUID uuid = UUID.fromString(rawName);
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                PlayerSnapshot snapshot = new PlayerSnapshot(
                        uuid,
                        config.getString("name", "Unknown"),
                        config.getString("ip", "Unknown"),
                        config.getString("previous-ip", "None"),
                        config.getBoolean("op", false),
                        config.getBoolean("banned", false),
                        config.getLong("first-played", 0L),
                        config.getLong("last-seen", 0L),
                        config.getLong("playtime-seconds", 0L),
                        config.getString("world", "Unknown"),
                        config.getString("dimension", "Unknown"),
                        config.getInt("x", 0),
                        config.getInt("y", 0),
                        config.getInt("z", 0),
                        config.getString("last-death", "None"),
                        config.getString("spawn-location", "Not set"),
                        config.getInt("health", 0),
                        config.getInt("food", 0),
                        config.getInt("xp-level", 0),
                        config.getInt("ping", 0),
                        config.getString("gamemode", "UNKNOWN"),
                        config.getInt("mob-kills", 0),
                        config.getInt("deaths", 0),
                        config.getInt("pvp-wins", 0),
                        config.getInt("pvp-losses", 0),
                        config.getString("effects", "None")
                );

                putInCache(snapshot);
            } catch (Exception e) {
                plugin.getLogger().warning("Impossibile caricare il file utente: " + file.getName());
            }
        }
    }

    private void putInCache(PlayerSnapshot snapshot) {
        cache.put(snapshot.uuid(), snapshot);
        if (snapshot.name() != null && !snapshot.name().equals("Unknown")) {
            String key = snapshot.name().toLowerCase(Locale.ROOT);
            UUID existingUuid = nameIndex.get(key);
            if (existingUuid == null || existingUuid.equals(snapshot.uuid())) {
                nameIndex.put(key, snapshot.uuid());
            } else {
                PlayerSnapshot existing = cache.get(existingUuid);
                if (existing == null || snapshot.lastSeen() >= existing.lastSeen()) {
                    nameIndex.put(key, snapshot.uuid());
                }
            }
        }
    }

    public PlayerSnapshot captureOnlineSnapshot(Player player) {
        PlayerSnapshot existing = cache.get(player.getUniqueId());
        int pvpLosses = (existing != null) ? existing.pvpLosses() : 0;
        PlayerSnapshot snapshot = PlayerSnapshot.fromPlayer(player, existing, pvpLosses);
        putInCache(snapshot);
        return snapshot;
    }

    public PlayerSnapshot getSnapshotForPlaceholder(OfflinePlayer player) {
        if (player.isOnline() && player.getPlayer() != null && Bukkit.isPrimaryThread()) {
            return captureOnlineSnapshot(player.getPlayer());
        }
        return cache.get(player.getUniqueId());
    }

    public PlayerSnapshot getSnapshot(UUID uuid) {
        return cache.get(uuid);
    }

    public PlayerSnapshot findSnapshotByName(String name) {
        if (name == null) {
            return null;
        }
        UUID uuid = nameIndex.get(name.toLowerCase(Locale.ROOT));
        return (uuid != null) ? cache.get(uuid) : null;
    }

    public List<String> getKnownPlayerNames() {
        List<String> names = new ArrayList<>();
        for (PlayerSnapshot snapshot : cache.values()) {
            if (snapshot.name() != null && !snapshot.name().equals("Unknown")) {
                names.add(snapshot.name());
            }
        }
        return names;
    }

    public void savePlayerAsync(Player player) {
        PlayerSnapshot snapshot = captureOnlineSnapshot(player);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeSnapshotToFile(snapshot));
    }

    public void recordPvpLossAndSaveAsync(Player victim) {
        PlayerSnapshot existing = cache.get(victim.getUniqueId());
        int newLosses = ((existing != null) ? existing.pvpLosses() : 0) + 1;
        PlayerSnapshot snapshot = PlayerSnapshot.fromPlayer(victim, existing, newLosses);
        putInCache(snapshot);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeSnapshotToFile(snapshot));
    }

    public void saveAllOnlineSync() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerSnapshot snapshot = captureOnlineSnapshot(player);
            writeSnapshotToFile(snapshot);
        }
    }

    private void writeSnapshotToFile(PlayerSnapshot snapshot) {
        Object lock = fileLocks.computeIfAbsent(snapshot.uuid(), ignored -> new Object());
        synchronized (lock) {
            File playerFile = new File(dataFolder, snapshot.uuid() + ".yml");
            YamlConfiguration config = new YamlConfiguration();

            config.set("name", snapshot.name());
            config.set("uuid", snapshot.uuid().toString());
            config.set("ip", snapshot.ip());
            config.set("previous-ip", snapshot.previousIp());
            config.set("op", snapshot.op());
            config.set("banned", snapshot.banned());
            config.set("first-played", snapshot.firstPlayed());
            config.set("last-seen", snapshot.lastSeen());
            config.set("playtime-seconds", snapshot.playtimeSeconds());
            config.set("mob-kills", snapshot.mobKills());
            config.set("deaths", snapshot.deaths());
            config.set("pvp-wins", snapshot.pvpWins());
            config.set("pvp-losses", snapshot.pvpLosses());
            config.set("world", snapshot.world());
            config.set("dimension", snapshot.dimension());
            config.set("x", snapshot.x());
            config.set("y", snapshot.y());
            config.set("z", snapshot.z());
            config.set("last-death", snapshot.lastDeath());
            config.set("spawn-location", snapshot.spawnLocation());
            config.set("health", snapshot.health());
            config.set("food", snapshot.food());
            config.set("xp-level", snapshot.xpLevel());
            config.set("ping", snapshot.ping());
            config.set("gamemode", snapshot.gamemode());
            config.set("effects", snapshot.effects());

            try {
                config.save(playerFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not save user data for " + snapshot.name());
            }
        }
    }
}