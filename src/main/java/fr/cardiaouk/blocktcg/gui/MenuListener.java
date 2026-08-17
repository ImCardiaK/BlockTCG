package fr.cardiaouk.blocktcg.gui;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import fr.cardiaouk.blocktcg.card.CardDefinition;
import fr.cardiaouk.blocktcg.data.PlayerData;
import fr.cardiaouk.blocktcg.duel.DuelManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class MenuListener implements Listener {
    private final BlockTCGPlugin plugin;
    public MenuListener(BlockTCGPlugin plugin){this.plugin=plugin;}

    @EventHandler
    public void onRedeem(PlayerInteractEvent e){
        if(e.getHand()!=EquipmentSlot.HAND)return;
        switch(e.getAction()) { case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {} default -> {return;} }
        ItemStack item=e.getItem(); String id=plugin.cardItems().getCardId(item); if(id==null)return;
        CardDefinition card=plugin.cards().get(id); if(card==null)return;
        e.setCancelled(true); Player p=e.getPlayer(); plugin.data().get(p.getUniqueId()).addCard(id,1); plugin.data().save(p.getUniqueId());
        item.setAmount(item.getAmount()-1); p.sendMessage(plugin.msg(ChatColor.GREEN+card.name()+" ajoutée à ta collection."));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;
        String title=e.getView().getTitle();
        if(title.startsWith(MenuManager.COLLECTION_TITLE)) { e.setCancelled(true); handleCollection(p,e); }
        else if(title.startsWith(MenuManager.DECK_PREFIX)) { e.setCancelled(true); handleDeck(p,e,title); }
        else if(title.equals(MenuManager.BATTLE_TITLE)) { e.setCancelled(true); handleBattle(p,e); }
    }

    private void handleCollection(Player p,InventoryClickEvent e){
        String marker=plugin.menus().marker(e.getCurrentItem());
        if(marker!=null&&marker.startsWith("page:")) plugin.menus().openCollection(p,Integer.parseInt(marker.substring(5)));
    }

    private void handleDeck(Player p,InventoryClickEvent e,String title){
        String stripped=ChatColor.stripColor(title); if(stripped==null)return;
        String rest=stripped.substring("BlockTCG • Deck: ".length()); int sep=rest.lastIndexOf(" • "); String deckName=sep>0?rest.substring(0,sep):rest;
        int page=0; if(sep>0)try{page=Integer.parseInt(rest.substring(sep+3))-1;}catch(Exception ignored){}
        String marker=plugin.menus().marker(e.getCurrentItem());
        if(marker!=null) {
            if(marker.startsWith("page:")){plugin.menus().openDeckEditor(p,deckName,Integer.parseInt(marker.substring(5)));return;}
            if(marker.equals("activate")){plugin.data().get(p.getUniqueId()).setActiveDeck(deckName);plugin.data().save(p.getUniqueId());plugin.menus().openDeckEditor(p,deckName,page);return;}
        }
        String id=plugin.cardItems().getCardId(e.getCurrentItem()); if(id==null)return;
        PlayerData data=plugin.data().get(p.getUniqueId()); List<String> deck=data.decks().get(deckName); if(deck==null)return;
        int max=plugin.getConfig().getInt("game.deck-size",30), copies=plugin.getConfig().getInt("game.max-copies-per-card",2);
        long current=deck.stream().filter(id::equalsIgnoreCase).count();
        if(e.isRightClick()) { if(current>0){for(int i=deck.size()-1;i>=0;i--)if(deck.get(i).equalsIgnoreCase(id)){deck.remove(i);break;}} }
        else { if(deck.size()<max&&current<Math.min(copies,data.cardCount(id)))deck.add(id); else p.sendMessage(plugin.msg("Limite atteinte pour cette carte ou deck complet.")); }
        plugin.data().save(p.getUniqueId()); plugin.menus().openDeckEditor(p,deckName,page);
    }

    private void handleBattle(Player p,InventoryClickEvent e){
        DuelManager.Duel duel=plugin.duels().get(p.getUniqueId()); if(duel==null){p.closeInventory();return;}
        String marker=plugin.menus().marker(e.getCurrentItem()); if("endturn".equals(marker)){plugin.duels().endTurn(p);return;}
        if(e.getRawSlot()>=45&&e.getRawSlot()<=53){
            int handIndex=e.getRawSlot()-45; DuelManager.Fighter f=duel.fighter(p.getUniqueId()); int board=-1; for(int i=0;i<3;i++)if(f.board[i]==null){board=i;break;} if(board<0){p.sendMessage(plugin.msg("Ton plateau est plein."));return;} plugin.duels().playCard(p,handIndex,board);
        }
    }
}
