package fr.cardiaouk.blocktcg.card;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public final class CardRegistry {
    private final JavaPlugin plugin;
    private final Map<String, CardDefinition> cards = new LinkedHashMap<>();

    public CardRegistry(JavaPlugin plugin) { this.plugin = plugin; }

    public void load() {
        cards.clear();
        File file = new File(plugin.getDataFolder(), "cards.yml");
        if (!file.exists()) plugin.saveResource("cards.yml", false);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("cards");
        if (root == null) throw new IllegalStateException("cards.yml ne contient pas de section cards");
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                CardDefinition def = new CardDefinition(
                        id,
                        s.getString("name", id),
                        s.getString("theme", "NEUTRE"),
                        CardRarity.valueOf(s.getString("rarity", "COMMON").toUpperCase(Locale.ROOT)),
                        s.getInt("attack", 1),
                        s.getInt("defense", 1),
                        s.getInt("mana", 1),
                        Material.valueOf(s.getString("material", "PAPER").toUpperCase(Locale.ROOT)),
                        s.getString("ability", "")
                );
                cards.put(id.toLowerCase(Locale.ROOT), def);
            } catch (Exception ex) {
                plugin.getLogger().warning("Carte invalide '" + id + "': " + ex.getMessage());
            }
        }
        plugin.getLogger().info(cards.size() + " cartes chargées.");
    }

    public CardDefinition get(String id) { return id == null ? null : cards.get(id.toLowerCase(Locale.ROOT)); }
    public Collection<CardDefinition> all() { return Collections.unmodifiableCollection(cards.values()); }
    public List<CardDefinition> list() { return List.copyOf(cards.values()); }
    public int size() { return cards.size(); }
}
