package fr.cardiaouk.blocktcg.prebuilt;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public final class PrebuiltDeckRegistry {
    private final BlockTCGPlugin plugin;
    private final Map<String, PrebuiltDeck> decks = new LinkedHashMap<>();
    public PrebuiltDeckRegistry(BlockTCGPlugin plugin) { this.plugin = plugin; }

    public void load() {
        decks.clear();
        File file = new File(plugin.getDataFolder(), "decks.yml");
        if (!file.exists()) plugin.saveResource("decks.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = y.getConfigurationSection("prebuilt-decks");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id); if (s == null || !s.getBoolean("enabled", true)) continue;
            List<String> cards = s.getStringList("cards").stream().filter(c -> plugin.cards().get(c) != null).toList();
            decks.put(id.toLowerCase(Locale.ROOT), new PrebuiltDeck(id, s.getString("name", id), s.getString("theme", "Neutre"), s.getInt("price", 500), s.getString("container", "BLUE_SHULKER_BOX"), new ArrayList<>(cards)));
        }
        plugin.getLogger().info(decks.size() + " decks préconstruits chargés.");
    }
    public Collection<PrebuiltDeck> all() { return Collections.unmodifiableCollection(decks.values()); }
    public PrebuiltDeck get(String id) { return id == null ? null : decks.get(id.toLowerCase(Locale.ROOT)); }
}
