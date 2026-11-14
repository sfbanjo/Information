package com.jarvis.test;

import org.bukkit.Bukkit;

public class PlaceholderManager {

    public static void registerExpansion(Information plugin) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PAPIHook.hook(plugin);
        } else {
            plugin.getLogger().info("PlaceholderAPI non trovato. Funzionalità placeholders disabilitata.");
        }
    }

    private static class PAPIHook {
        private static void hook(Information plugin) {
            new InformationPlaceholderExpansion(plugin).register();
            plugin.getLogger().info("Espansione PlaceholderAPI registrata con successo!");
        }
    }
}