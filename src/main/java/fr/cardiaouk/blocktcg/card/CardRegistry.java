package fr.cardiaouk.blocktcg.card;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class CardRegistry {
    private final JavaPlugin plugin;
    private final Map<String, CardDefinition> cards = new LinkedHashMap<>();

    public CardRegistry(JavaPlugin plugin) { this.plugin = plugin; }

    public void load() {
        cards.clear();
        File file = file();
        if (!file.exists()) plugin.saveResource("cards.yml", false);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("cards");
        if (root == null) throw new IllegalStateException("cards.yml ne contient pas de section cards");
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id); if (s == null || !s.getBoolean("enabled", true)) continue;
            try {
                Material material = Material.matchMaterial(s.getString("material", "PAPER"));
                if (material == null) throw new IllegalArgumentException("material inconnu");
                CardDefinition def = new CardDefinition(
                        id,
                        s.getString("name", id),
                        s.getString("theme", "NEUTRE"),
                        CardRarity.valueOf(s.getString("rarity", "COMMON").toUpperCase(Locale.ROOT)),
                        Math.max(0, s.getInt("attack", 1)),
                        Math.max(1, s.getInt("defense", 1)),
                        Math.max(0, s.getInt("mana", 1)),
                        material,
                        s.getString("ability", "")
                );
                cards.put(id.toLowerCase(Locale.ROOT), def);
            } catch (Exception ex) { plugin.getLogger().warning("Carte invalide '" + id + "': " + ex.getMessage()); }
        }
        plugin.getLogger().info(cards.size() + " cartes chargées depuis cards.yml.");
    }

    public boolean createTemplate(String rawId) {
        String id = sanitizeId(rawId); if (id.isBlank()) return false;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file());
        if (y.contains("cards." + id)) return false;
        String p="cards."+id+".";
        y.set(p+"enabled", true); y.set(p+"name", "Nouvelle carte"); y.set(p+"theme", "NEUTRE"); y.set(p+"rarity", "COMMON");
        y.set(p+"attack", 1); y.set(p+"defense", 1); y.set(p+"mana", 1); y.set(p+"material", "PAPER"); y.set(p+"ability", "");
        try { y.save(file()); load(); return true; } catch (IOException ex) { plugin.getLogger().severe(ex.getMessage()); return false; }
    }

    public boolean setField(String rawId, String field, Object value) {
        String id=sanitizeId(rawId); String f=field.toLowerCase(Locale.ROOT);
        if(!Set.of("enabled","name","theme","rarity","attack","defense","mana","material","ability").contains(f)) return false;
        YamlConfiguration y=YamlConfiguration.loadConfiguration(file()); if(!y.contains("cards."+id))return false;
        Object parsed=value;
        if(Set.of("attack","defense","mana").contains(f)) { try { parsed=Integer.parseInt(String.valueOf(value)); } catch(NumberFormatException ex){ return false; } }
        if(f.equals("enabled")) parsed=Boolean.parseBoolean(String.valueOf(value));
        y.set("cards."+id+"."+f,parsed);
        try { y.save(file()); load(); return true; } catch(IOException ex){ plugin.getLogger().severe(ex.getMessage()); return false; }
    }

    private String sanitizeId(String raw){ return raw==null?"":raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]","_"); }
    private File file(){ return new File(plugin.getDataFolder(),"cards.yml"); }
    public CardDefinition get(String id) { return id == null ? null : cards.get(id.toLowerCase(Locale.ROOT)); }
    public Collection<CardDefinition> all() { return Collections.unmodifiableCollection(cards.values()); }
    public List<CardDefinition> list() { return List.copyOf(cards.values()); }
    public int size() { return cards.size(); }
}
