package fr.cardiaouk.blocktcg.gui;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import fr.cardiaouk.blocktcg.card.CardDefinition;
import fr.cardiaouk.blocktcg.data.PlayerData;
import fr.cardiaouk.blocktcg.duel.DuelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class MenuManager {
    private final BlockTCGPlugin plugin;
    public static final String COLLECTION_TITLE = ChatColor.DARK_GREEN + "BlockTCG • Collection";
    public static final String DECK_PREFIX = ChatColor.DARK_BLUE + "BlockTCG • Deck: ";
    public static final String BATTLE_TITLE = ChatColor.DARK_RED + "BlockTCG • Duel";

    public MenuManager(BlockTCGPlugin plugin) { this.plugin = plugin; }

    public void openCollection(Player p, int page) {
        List<CardDefinition> owned = plugin.cards().list().stream().filter(c -> plugin.data().get(p.getUniqueId()).cardCount(c.id()) > 0).toList();
        int maxPage = Math.max(0, (owned.size()-1)/45); page = Math.max(0, Math.min(page,maxPage));
        Inventory inv = Bukkit.createInventory(null,54,COLLECTION_TITLE + " " + (page+1));
        PlayerData data = plugin.data().get(p.getUniqueId());
        for(int i=0;i<45;i++) {
            int idx=page*45+i; if(idx>=owned.size()) break;
            CardDefinition c=owned.get(idx); ItemStack item=plugin.cardItems().create(c,1); ItemMeta m=item.getItemMeta();
            List<String> lore=new ArrayList<>(Objects.requireNonNullElse(m.getLore(),List.of())); lore.add(ChatColor.GOLD+"Possédées: "+data.cardCount(c.id())); m.setLore(lore); item.setItemMeta(m); inv.setItem(i,item);
        }
        if(page>0) inv.setItem(45, simple(Material.ARROW, ChatColor.YELLOW+"Page précédente", "page:"+(page-1)));
        inv.setItem(49, simple(Material.GOLD_NUGGET, ChatColor.GOLD+"Pièces: "+data.coins(), null));
        if(page<maxPage) inv.setItem(53, simple(Material.ARROW, ChatColor.YELLOW+"Page suivante", "page:"+(page+1)));
        p.openInventory(inv);
    }

    public void openDeckEditor(Player p, String deckName, int page) {
        PlayerData data=plugin.data().get(p.getUniqueId());
        List<String> deck=data.decks().get(deckName); if(deck==null){p.sendMessage(plugin.msg("Deck introuvable."));return;}
        List<CardDefinition> owned=plugin.cards().list().stream().filter(c->data.cardCount(c.id())>0).toList();
        int maxPage=Math.max(0,(owned.size()-1)/45); page=Math.max(0,Math.min(page,maxPage));
        Inventory inv=Bukkit.createInventory(null,54,DECK_PREFIX+deckName+" • "+(page+1));
        for(int i=0;i<45;i++) {
            int idx=page*45+i; if(idx>=owned.size()) break;
            CardDefinition c=owned.get(idx); ItemStack item=plugin.cardItems().create(c,1); ItemMeta m=item.getItemMeta();
            int inDeck=(int)deck.stream().filter(c.id()::equalsIgnoreCase).count();
            List<String> lore=new ArrayList<>(Objects.requireNonNullElse(m.getLore(),List.of()));
            lore.add(""); lore.add(ChatColor.GOLD+"Collection: "+data.cardCount(c.id())+" | Deck: "+inDeck);
            lore.add(ChatColor.GREEN+"Clic gauche: ajouter"); lore.add(ChatColor.RED+"Clic droit: retirer"); m.setLore(lore); item.setItemMeta(m); inv.setItem(i,item);
        }
        if(page>0) inv.setItem(45,simple(Material.ARROW,ChatColor.YELLOW+"Page précédente","page:"+(page-1)));
        inv.setItem(48,simple(Material.CHEST,ChatColor.AQUA+"Deck: "+deck.size()+"/"+plugin.getConfig().getInt("game.deck-size",30),null));
        boolean active=deckName.equalsIgnoreCase(data.activeDeck());
        inv.setItem(49,simple(active?Material.LIME_DYE:Material.GRAY_DYE,(active?ChatColor.GREEN:ChatColor.GRAY)+(active?"Deck actif":"Cliquer pour activer"),"activate"));
        if(page<maxPage) inv.setItem(53,simple(Material.ARROW,ChatColor.YELLOW+"Page suivante","page:"+(page+1)));
        p.openInventory(inv);
    }

    public void openBattle(Player viewer, DuelManager.Duel duel) {
        DuelManager.Fighter self=duel.fighter(viewer.getUniqueId()), enemy=duel.enemy(viewer.getUniqueId()); if(self==null)return;
        Inventory inv=Bukkit.createInventory(null,54,BATTLE_TITLE);
        inv.setItem(4,simple(Material.NETHER_STAR,ChatColor.GOLD+enemy.name+" ❤ "+Math.max(0,enemy.health),null));
        inv.setItem(13,simple(Material.HEART_OF_THE_SEA,ChatColor.AQUA+"Tour: "+(duel.turn.equals(self.uuid)?"TOI":enemy.name),null));
        for(int i=0;i<3;i++) {
            if(enemy.board[i]!=null) inv.setItem(19+i,unitItem(enemy.board[i]));
            if(self.board[i]!=null) inv.setItem(28+i,unitItem(self.board[i]));
            else inv.setItem(28+i,simple(Material.LIGHT_GRAY_STAINED_GLASS_PANE,ChatColor.GRAY+"Emplacement "+(i+1),"board:"+i));
        }
        inv.setItem(36,simple(Material.REDSTONE,ChatColor.RED+"PV: "+Math.max(0,self.health),null));
        inv.setItem(40,simple(Material.CLOCK,ChatColor.YELLOW+"Terminer le tour","endturn"));
        inv.setItem(44,simple(Material.LAPIS_LAZULI,ChatColor.AQUA+"Mana: "+self.mana+"/"+self.maxMana,null));
        for(int i=0;i<Math.min(9,self.hand.size());i++) {
            CardDefinition c=plugin.cards().get(self.hand.get(i)); if(c==null)continue;
            ItemStack item=plugin.cardItems().create(c,1); ItemMeta m=item.getItemMeta(); List<String> lore=new ArrayList<>(Objects.requireNonNullElse(m.getLore(),List.of()));
            lore.add(ChatColor.GREEN+"Clique pour jouer sur le premier emplacement libre"); m.setLore(lore); item.setItemMeta(m); inv.setItem(45+i,item);
        }
        viewer.openInventory(inv);
    }

    private ItemStack unitItem(DuelManager.Unit u) {
        ItemStack item=plugin.cardItems().create(u.card,1); ItemMeta m=item.getItemMeta(); List<String> lore=new ArrayList<>(Objects.requireNonNullElse(m.getLore(),List.of())); lore.add(ChatColor.RED+"PV unité: "+u.hp+"/"+u.card.defense()); m.setLore(lore); item.setItemMeta(m); return item;
    }

    public ItemStack simple(Material mat,String name,String marker) {
        ItemStack i=new ItemStack(mat); ItemMeta m=i.getItemMeta(); m.setDisplayName(name); if(marker!=null)m.setLore(List.of(ChatColor.BLACK+marker)); i.setItemMeta(m); return i;
    }

    public String marker(ItemStack item) {
        if(item==null||!item.hasItemMeta()||item.getItemMeta().getLore()==null)return null;
        for(String s:item.getItemMeta().getLore()) { String clean=ChatColor.stripColor(s); if(clean!=null&&(clean.startsWith("page:")||clean.equals("activate")||clean.equals("endturn")||clean.startsWith("board:")))return clean; }
        return null;
    }
}
