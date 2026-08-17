package fr.cardiaouk.blocktcg.config;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class PluginConfigs {
    private final BlockTCGPlugin plugin;
    private YamlConfiguration menus;
    private YamlConfiguration economy;
    private YamlConfiguration npcs;

    public PluginConfigs(BlockTCGPlugin plugin) { this.plugin = plugin; reload(); }

    public void reload() {
        this.menus = load("menus.yml");
        this.economy = load("economy.yml");
        this.npcs = load("npcs.yml");
    }

    private YamlConfiguration load(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) plugin.saveResource(name, false);
        return YamlConfiguration.loadConfiguration(file);
    }

    public YamlConfiguration menus() { return menus; }
    public YamlConfiguration economy() { return economy; }
    public YamlConfiguration npcs() { return npcs; }
}
