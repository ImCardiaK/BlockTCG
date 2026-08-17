package fr.cardiaouk.blocktcg;

import fr.cardiaouk.blocktcg.card.*;
import fr.cardiaouk.blocktcg.command.TCGCommand;
import fr.cardiaouk.blocktcg.data.PlayerDataManager;
import fr.cardiaouk.blocktcg.duel.DuelManager;
import fr.cardiaouk.blocktcg.gui.*;
import fr.cardiaouk.blocktcg.npc.NpcManager;
import fr.cardiaouk.blocktcg.trade.TradeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockTCGPlugin extends JavaPlugin implements Listener {
    private CardRegistry cards;
    private CardItemFactory cardItems;
    private PlayerDataManager data;
    private DuelManager duels;
    private MenuManager menus;
    private NpcManager npcs;
    private TradeManager trades;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cards=new CardRegistry(this);cards.load();
        cardItems=new CardItemFactory(this);
        data=new PlayerDataManager(this);
        menus=new MenuManager(this);
        duels=new DuelManager(this);
        npcs=new NpcManager(this);
        trades=new TradeManager(this);
        TCGCommand tcg=new TCGCommand(this); PluginCommand cmd=getCommand("tcg"); if(cmd==null)throw new IllegalStateException("Commande tcg absente de plugin.yml");cmd.setExecutor(tcg);cmd.setTabCompleter(tcg);
        getServer().getPluginManager().registerEvents(new MenuListener(this),this);
        getServer().getPluginManager().registerEvents(npcs,this);
        getServer().getPluginManager().registerEvents(this,this);
        getLogger().info("BlockTCG activé avec "+cards.size()+" cartes.");
    }

    @Override public void onDisable(){if(data!=null)data.saveAll();}
    @EventHandler public void onQuit(PlayerQuitEvent e){trades.cancel(e.getPlayer().getUniqueId());data.unload(e.getPlayer().getUniqueId());}

    public void reloadEverything(){reloadConfig();cards.load();}
    public String msg(String text){String prefix=getConfig().getString("messages.prefix","&8[&6BlockTCG&8] &r");return ChatColor.translateAlternateColorCodes('&',prefix)+text;}
    public CardRegistry cards(){return cards;} public CardItemFactory cardItems(){return cardItems;} public PlayerDataManager data(){return data;} public DuelManager duels(){return duels;} public MenuManager menus(){return menus;} public NpcManager npcs(){return npcs;} public TradeManager trades(){return trades;}
}
