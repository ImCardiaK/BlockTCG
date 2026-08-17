package fr.cardiaouk.blocktcg;

import fr.cardiaouk.blocktcg.booster.*;
import fr.cardiaouk.blocktcg.card.CardItemFactory;
import fr.cardiaouk.blocktcg.card.CardRegistry;
import fr.cardiaouk.blocktcg.command.TCGCommand;
import fr.cardiaouk.blocktcg.config.PluginConfigs;
import fr.cardiaouk.blocktcg.data.PlayerDataManager;
import fr.cardiaouk.blocktcg.deck.DeckItemFactory;
import fr.cardiaouk.blocktcg.duel.DuelManager;
import fr.cardiaouk.blocktcg.gui.MenuListener;
import fr.cardiaouk.blocktcg.gui.MenuManager;
import fr.cardiaouk.blocktcg.lang.LanguageManager;
import fr.cardiaouk.blocktcg.npc.NpcManager;
import fr.cardiaouk.blocktcg.prebuilt.PrebuiltDeckRegistry;
import fr.cardiaouk.blocktcg.trade.TradeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockTCGPlugin extends JavaPlugin implements Listener {
    private CardRegistry cards;
    private CardItemFactory cardItems;
    private DeckItemFactory deckItems;
    private PlayerDataManager data;
    private DuelManager duels;
    private MenuManager menus;
    private NpcManager npcs;
    private TradeManager trades;
    private PluginConfigs configs;
    private LanguageManager lang;
    private PrebuiltDeckRegistry prebuiltDecks;
    private BoosterRegistry boosters;
    private BoosterManager boosterManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configs = new PluginConfigs(this);
        lang = new LanguageManager(this);
        cards = new CardRegistry(this); cards.load();
        cardItems = new CardItemFactory(this);
        data = new PlayerDataManager(this);
        deckItems = new DeckItemFactory(this);
        prebuiltDecks = new PrebuiltDeckRegistry(this); prebuiltDecks.load();
        boosters = new BoosterRegistry(this); boosters.load();
        boosterManager = new BoosterManager(this);
        menus = new MenuManager(this);
        duels = new DuelManager(this);
        npcs = new NpcManager(this); npcs.startRotationTask();
        trades = new TradeManager(this);

        TCGCommand tcg = new TCGCommand(this);
        PluginCommand cmd = getCommand("tcg");
        if (cmd == null) throw new IllegalStateException("Commande tcg absente de plugin.yml");
        cmd.setExecutor(tcg); cmd.setTabCompleter(tcg);

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(npcs, this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BlockTCG activé avec " + cards.size() + " cartes.");
    }

    @Override public void onDisable() { if (data != null) data.saveAll(); if (npcs != null) npcs.saveState(); }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var player = e.getPlayer();
        boolean existing = data.hasSavedData(player.getUniqueId());
        var d = data.get(player.getUniqueId());
        if (!existing) {
            for (String deck : d.decks().keySet()) deckItems.give(player, deck);
            data.save(player.getUniqueId());
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) {
        trades.cancel(e.getPlayer().getUniqueId());
        npcs.clearSession(e.getPlayer().getUniqueId());
        data.unload(e.getPlayer().getUniqueId());
    }

    public void reloadEverything() {
        reloadConfig();
        configs.reload();
        lang.reload();
        cards.load();
        prebuiltDecks.load();
        boosters.load();
        npcs.reloadSettings();
    }

    public String msg(String text) {
        String prefix = getConfig().getString("messages.prefix", "&8[&6BlockTCG&8] &r");
        return ChatColor.translateAlternateColorCodes('&', prefix) + text;
    }

    public CardRegistry cards() { return cards; }
    public CardItemFactory cardItems() { return cardItems; }
    public DeckItemFactory deckItems() { return deckItems; }
    public PlayerDataManager data() { return data; }
    public DuelManager duels() { return duels; }
    public MenuManager menus() { return menus; }
    public NpcManager npcs() { return npcs; }
    public TradeManager trades() { return trades; }
    public PluginConfigs configs() { return configs; }
    public LanguageManager lang() { return lang; }
    public PrebuiltDeckRegistry prebuiltDecks() { return prebuiltDecks; }
    public BoosterRegistry boosters() { return boosters; }
    public BoosterManager boosterManager() { return boosterManager; }
}
