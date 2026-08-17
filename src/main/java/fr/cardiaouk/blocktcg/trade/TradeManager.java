package fr.cardiaouk.blocktcg.trade;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import fr.cardiaouk.blocktcg.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public final class TradeManager {
    public static final class Offer {
        public final UUID owner;
        public final UUID partner;
        public final Map<String,Integer> cards = new HashMap<>();
        public int coins;
        public String deckName;
        public boolean confirmed;
        Offer(UUID owner, UUID partner){this.owner=owner;this.partner=partner;}
    }

    private final BlockTCGPlugin plugin;
    private final Map<UUID, UUID> requests = new HashMap<>();
    private final Map<UUID, Offer> offers = new HashMap<>();

    public TradeManager(BlockTCGPlugin plugin){this.plugin=plugin;}

    public void request(Player from, Player to){
        if(from.equals(to)){from.sendMessage(plugin.msg("Impossible d'échanger avec toi-même."));return;}
        requests.put(to.getUniqueId(), from.getUniqueId());
        from.sendMessage(plugin.msg("Demande d'échange envoyée à "+to.getName()+"."));
        to.sendMessage(plugin.msg(from.getName()+" propose un échange. /tcg tradeaccept"));
    }

    public void accept(Player target){
        UUID other=requests.remove(target.getUniqueId()); if(other==null){target.sendMessage(plugin.msg("Aucune demande d'échange."));return;}
        Player p=Bukkit.getPlayer(other); if(p==null){target.sendMessage(plugin.msg("Le joueur n'est plus connecté."));return;}
        offers.put(p.getUniqueId(),new Offer(p.getUniqueId(),target.getUniqueId()));
        offers.put(target.getUniqueId(),new Offer(target.getUniqueId(),p.getUniqueId()));
        p.sendMessage(plugin.msg("Échange ouvert. Utilise /tcg offercard, /tcg offercoins, /tcg offerdeck puis /tcg confirmtrade."));
        target.sendMessage(plugin.msg("Échange ouvert. Utilise /tcg offercard, /tcg offercoins, /tcg offerdeck puis /tcg confirmtrade."));
    }

    public void offerCard(Player p,String id,int qty){
        Offer o=offers.get(p.getUniqueId()); if(o==null){p.sendMessage(plugin.msg("Aucun échange actif."));return;}
        if(plugin.cards().get(id)==null||qty<0){p.sendMessage(plugin.msg("Carte ou quantité invalide."));return;}
        PlayerData d=plugin.data().get(p.getUniqueId()); if(d.cardCount(id)<qty){p.sendMessage(plugin.msg("Tu ne possèdes pas assez de cette carte."));return;}
        if(qty==0)o.cards.remove(id); else o.cards.put(id,qty); reset(o); p.sendMessage(plugin.msg("Offre carte: "+id+" x"+qty));
    }

    public void offerCoins(Player p,int amount){
        Offer o=offers.get(p.getUniqueId()); if(o==null)return;
        if(amount<0||plugin.data().get(p.getUniqueId()).coins()<amount){p.sendMessage(plugin.msg("Montant invalide."));return;}
        o.coins=amount; reset(o); p.sendMessage(plugin.msg("Offre pièces: "+amount));
    }

    public void offerDeck(Player p,String name){
        Offer o=offers.get(p.getUniqueId()); if(o==null)return;
        if(name.equalsIgnoreCase("none")){o.deckName=null; reset(o); return;}
        if(!plugin.data().get(p.getUniqueId()).decks().containsKey(name)){p.sendMessage(plugin.msg("Deck introuvable."));return;}
        o.deckName=name; reset(o); p.sendMessage(plugin.msg("Deck offert: "+name));
    }

    private void reset(Offer changed){ changed.confirmed=false; Offer partner=offers.get(changed.partner); if(partner!=null)partner.confirmed=false; }

    public void status(Player p){
        Offer a=offers.get(p.getUniqueId()); if(a==null){p.sendMessage(plugin.msg("Aucun échange actif."));return;}
        Offer b=offers.get(a.partner); Player other=Bukkit.getPlayer(a.partner);
        p.sendMessage(ChatColor.GOLD+"--- Échange avec "+(other==null?"?":other.getName())+" ---");
        p.sendMessage(ChatColor.YELLOW+"Ton offre: "+a.coins+" pièces, "+a.cards+", deck="+a.deckName+", confirmé="+a.confirmed);
        if(b!=null)p.sendMessage(ChatColor.AQUA+"Son offre: "+b.coins+" pièces, "+b.cards+", deck="+b.deckName+", confirmé="+b.confirmed);
    }

    public void confirm(Player p){
        Offer a=offers.get(p.getUniqueId()); if(a==null){p.sendMessage(plugin.msg("Aucun échange actif."));return;}
        Offer b=offers.get(a.partner); if(b==null)return;
        a.confirmed=true; p.sendMessage(plugin.msg("Offre confirmée."));
        Player other=Bukkit.getPlayer(a.partner); if(other!=null)other.sendMessage(plugin.msg(p.getName()+" a confirmé l'échange."));
        if(b.confirmed) execute(a,b);
    }

    private boolean validate(Offer o){
        PlayerData d=plugin.data().get(o.owner); if(d.coins()<o.coins)return false;
        Map<String,Integer> required = new HashMap<>(o.cards);
        if(o.deckName!=null){
            List<String> deck=d.decks().get(o.deckName); if(deck==null)return false;
            for(String id:deck) required.merge(id,1,Integer::sum);
        }
        for(var e:required.entrySet())if(d.cardCount(e.getKey())<e.getValue())return false;
        return true;
    }

    private void execute(Offer a,Offer b){
        if(!validate(a)||!validate(b)){ notifyPair(a,"Échange annulé: une offre n'est plus valide."); cancel(a.owner); return; }
        PlayerData da=plugin.data().get(a.owner), db=plugin.data().get(b.owner);
        da.addCoins(-a.coins+b.coins); db.addCoins(-b.coins+a.coins);
        transferCards(da,db,a.cards); transferCards(db,da,b.cards);
        transferDeck(da,db,a.deckName,a.owner,b.owner); transferDeck(db,da,b.deckName,b.owner,a.owner);
        plugin.data().save(a.owner); plugin.data().save(b.owner);
        notifyPair(a,ChatColor.GREEN+"Échange terminé avec succès.");
        offers.remove(a.owner); offers.remove(b.owner);
    }

    private void transferCards(PlayerData from,PlayerData to,Map<String,Integer> map){ for(var e:map.entrySet()){from.addCard(e.getKey(),-e.getValue());to.addCard(e.getKey(),e.getValue());} }
    private void transferDeck(PlayerData from,PlayerData to,String name,UUID fromId,UUID toId){
        if(name==null)return; List<String> deck=from.decks().remove(name); if(deck==null)return;
        String container=from.deckContainer(name); from.setDeckContainer(name,null);
        for(String id:deck){ from.addCard(id,-1); to.addCard(id,1); }
        String target=name; int n=2; while(to.decks().containsKey(target))target=name+"-"+(n++); to.decks().put(target,new ArrayList<>(deck));
        to.setDeckContainer(target, container==null ? plugin.configs().menus().getString("decks.default-container","PURPLE_SHULKER_BOX") : container);
        if(name.equalsIgnoreCase(from.activeDeck()))from.setActiveDeck(null);
        Player fromPlayer=Bukkit.getPlayer(fromId); if(fromPlayer!=null) plugin.deckItems().remove(fromPlayer,name);
        Player toPlayer=Bukkit.getPlayer(toId); if(toPlayer!=null) plugin.deckItems().give(toPlayer,target);
    }
    private void notifyPair(Offer a,String msg){ for(UUID id:List.of(a.owner,a.partner)){Player p=Bukkit.getPlayer(id);if(p!=null)p.sendMessage(plugin.msg(msg));} }
    public void cancel(UUID id){ Offer o=offers.remove(id); if(o!=null)offers.remove(o.partner); }
}
