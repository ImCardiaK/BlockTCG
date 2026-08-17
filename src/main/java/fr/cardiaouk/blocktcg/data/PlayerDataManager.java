package fr.cardiaouk.blocktcg.data;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PlayerDataManager {
    private final BlockTCGPlugin plugin;
    private final File dir;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public PlayerDataManager(BlockTCGPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "players");
        if (!dir.exists()) dir.mkdirs();
    }

    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    private PlayerData load(UUID uuid) {
        File file = file(uuid);
        int start = plugin.getConfig().getInt("game.starting-coins", 250);
        if (!file.exists()) {
            PlayerData fresh = new PlayerData(start);
            List<String> starter = new ArrayList<>();
            for (String theme : List.of("foret", "ocean", "desert")) {
                for (int i = 1; i <= 10; i++) {
                    String id = theme + "_" + String.format("%02d", i);
                    if (plugin.cards().get(id) != null) {
                        fresh.addCard(id, 1);
                        starter.add(id);
                    }
                }
            }
            fresh.decks().put("Starter", starter);
            fresh.setDeckContainer("Starter", plugin.configs().menus().getString("decks.default-container", "PURPLE_SHULKER_BOX"));
            fresh.setActiveDeck("Starter");
            return fresh;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        PlayerData data = new PlayerData(y.getInt("coins", start));
        ConfigurationSection c = y.getConfigurationSection("collection");
        if (c != null) for (String id : c.getKeys(false)) data.collection().put(id, c.getInt(id));
        ConfigurationSection d = y.getConfigurationSection("decks");
        if (d != null) for (String name : d.getKeys(false)) data.decks().put(name, new ArrayList<>(d.getStringList(name)));
        ConfigurationSection dc = y.getConfigurationSection("deck-containers");
        if (dc != null) for (String name : dc.getKeys(false)) data.setDeckContainer(name, dc.getString(name));
        data.setActiveDeck(y.getString("active-deck"));
        return data;
    }

    public void save(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;
        YamlConfiguration y = new YamlConfiguration();
        y.set("coins", data.coins());
        for (var e : data.collection().entrySet()) y.set("collection." + e.getKey(), e.getValue());
        for (var e : data.decks().entrySet()) y.set("decks." + e.getKey(), e.getValue());
        for (var e : data.deckContainers().entrySet()) y.set("deck-containers." + e.getKey(), e.getValue());
        y.set("active-deck", data.activeDeck());
        try { y.save(file(uuid)); }
        catch (IOException ex) { plugin.getLogger().severe("Impossible de sauvegarder " + uuid + ": " + ex.getMessage()); }
    }

    public boolean hasSavedData(UUID uuid) { return file(uuid).exists(); }
    public void saveAll() { new ArrayList<>(cache.keySet()).forEach(this::save); }
    public void unload(UUID uuid) { save(uuid); cache.remove(uuid); }
    private File file(UUID uuid) { return new File(dir, uuid + ".yml"); }
}
