package fr.cardiaouk.blocktcg.lang;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public final class LanguageManager {
    private final BlockTCGPlugin plugin;
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private String current;

    public LanguageManager(BlockTCGPlugin plugin) { this.plugin = plugin; reload(); }

    public void reload() {
        languages.clear();
        for (String code : List.of("fr", "en", "es", "de")) {
            File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
            if (!file.exists()) plugin.saveResource("lang/" + code + ".yml", false);
            languages.put(code, YamlConfiguration.loadConfiguration(file));
        }
        current = normalize(plugin.getConfig().getString("language", "fr"));
        if (!languages.containsKey(current)) current = "fr";
    }

    public boolean setLanguage(String code) {
        String normalized = normalize(code);
        if (!languages.containsKey(normalized)) return false;
        current = normalized;
        plugin.getConfig().set("language", normalized);
        plugin.saveConfig();
        return true;
    }

    private String normalize(String code) {
        if (code == null) return "fr";
        String c = code.trim().toLowerCase(Locale.ROOT);
        return switch (c) {
            case "fr", "francais", "français", "french" -> "fr";
            case "en", "anglais", "english" -> "en";
            case "es", "espagnol", "espanol", "spanish" -> "es";
            case "de", "allemand", "deutsch", "german" -> "de";
            default -> c;
        };
    }

    public String current() { return current; }
    public Set<String> available() { return Set.copyOf(languages.keySet()); }

    public String tr(String key, Object... replacements) {
        YamlConfiguration cfg = languages.getOrDefault(current, languages.get("fr"));
        String value = cfg.getString(key);
        if (value == null && languages.get("fr") != null) value = languages.get("fr").getString(key, key);
        if (value == null) value = key;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
