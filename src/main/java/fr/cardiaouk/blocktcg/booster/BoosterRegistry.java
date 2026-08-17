package fr.cardiaouk.blocktcg.booster;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public final class BoosterRegistry {
    private final BlockTCGPlugin plugin;
    private final Map<String, BoosterDefinition> boosters = new LinkedHashMap<>();
    private YamlConfiguration config;
    public BoosterRegistry(BlockTCGPlugin plugin) { this.plugin = plugin; }
    public void load() {
        boosters.clear();
        File file = new File(plugin.getDataFolder(), "boosters.yml");
        if (!file.exists()) plugin.saveResource("boosters.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        this.config = y;
        ConfigurationSection root = y.getConfigurationSection("boosters"); if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id); if (s == null || !s.getBoolean("enabled", true)) continue;
            Material mat = Material.matchMaterial(s.getString("material", "BUNDLE")); if (mat == null) mat = Material.BUNDLE;
            Map<String,Integer> weights = new LinkedHashMap<>();
            ConfigurationSection w = s.getConfigurationSection("rarity-weights");
            if (w != null) for (String key : w.getKeys(false)) weights.put(key.toUpperCase(Locale.ROOT), Math.max(0, w.getInt(key)));
            boosters.put(id.toLowerCase(Locale.ROOT), new BoosterDefinition(id, s.getString("name", id), mat, s.getInt("price", 200), Math.max(1, s.getInt("cards", 5)), weights, s.getString("theme", "ALL")));
        }
        plugin.getLogger().info(boosters.size() + " boosters chargés.");
    }
    public Collection<BoosterDefinition> all(){ return Collections.unmodifiableCollection(boosters.values()); }
    public BoosterDefinition get(String id){ return id == null ? null : boosters.get(id.toLowerCase(Locale.ROOT)); }
    public YamlConfiguration config(){ return config; }
}
