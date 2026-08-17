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
        public final Map<String,Integer> cards = new LinkedHashMap<>();
        public int coins;
        public String deckName;
        public boolean confirmed;
        Offer(UUID owner, UUID partner){ this.owner=owner; this.partner=partner; }
    }

    private final BlockTCGPlugin plugin;
    private final Map<UUID, UUID> requests = new HashMap<>();
    private final Map<UUID, Offer> offers = new HashMap<>();

    public TradeManager(BlockTCGPlugin plugin){ this.plugin=plugin; }

    public void request(Player from, Player to){
        if(from.equals(to)){from.sendMessage(plugin.msg("Impossible d'échanger avec toi-même."));return;}
        if (offers.containsKey(from.getUniqueId()) || offers.containsKey(to.getUniqueId())) { from.sendMessage(plugin.msg(ChatColor.RED + "Un des joueurs a déjà un échange actif.")); return; }
        requests.put(to.getUniqueId(), from.getUniqueId());
        from.sendMessage(plugin.msg(plugin.lang().tr("trade.request-sent", "player", to.getName())));
        to.sendMessage(plugin.msg(plugin.lang().tr("trade.request-received", "player", from.getName())));
    }

    public void accept(Player target){
        UUID other=requests.remove(target.getUniqueId()); if(other==null){target.sendMessage(plugin.msg(plugin.lang().tr("trade.no-request")));return;}
        Player p=Bukkit.getPlayer(other); if(p==null){target.sendMessage(plugin.msg(plugin.lang().tr("trade.player-offline")));return;}
        offers.put(p.getUniqueId(),new Offer(p.getUniqueId(),target.getUniqueId()));
        offers.put(target.getUniqueId(),new Offer(target.getUniqueId(),p.getUniqueId()));
        plugin.menus().openTrade(p); plugin.menus().openTrade(target);
    }

    public Offer offer(UUID player) { return offers.get(player); }
    public Offer partnerOffer(UUID player) { Offer o=offers.get(player); return o==null?null:offers.get(o.partner); }
    public boolean active(UUID player) { return offers.containsKey(player); }

    public void addCard(Player p, String id, int delta) {
        Offer o=offers.get(p.getUniqueId()); if(o==null)return;
        PlayerData d=plugin.data().get(p.getUniqueId());
        int current=o.cards.getOrDefault(id,0); int next=Math.max(0,current+delta);
        if(next>d.cardCount(id)) next=d.cardCount(id);
        if(next==0)o.cards.remove(id); else o.cards.put(id,next);
        changed(o); refreshPair(o);
    }

    public void addCoins(Player p,int delta){
        Offer o=offers.get(p.getUniqueId()); if(o==null)return;
        int max=plugin.data().get(p.getUniqueId()).coins(); o.coins=Math.max(0,Math.min(max,o.coins+delta)); changed(o); refreshPair(o);
    }

    public void cycleDeck(Player p) {
        Offer o=offers.get(p.getUniqueId()); if(o==null)return;
        List<String> names=new ArrayList<>(plugin.data().get(p.getUniqueId()).decks().keySet());
        if(names.isEmpty()){o.deckName=null;return;}
        if(o.deckName==null)o.deckName=names.get(0);
        else { int i=names.indexOf(o.deckName); o.deckName=(i<0||i+1>=names.size())?null:names.get(i+1); }
        changed(o); refreshPair(o);
    }

    private void changed(Offer changed){ changed.confirmed=false; Offer partner=offers.get(changed.partner); if(partner!=null)partner.confirmed=false; }

    public void confirm(Player p){
        Offer a=offers.get(p.getUniqueId()); if(a==null)return;
        Offer b=offers.get(a.partner); if(b==null)return;
        a.confirmed=!a.confirmed;
        refreshPair(a);
        if(a.confirmed && b.confirmed) execute(a,b);
    }

    public void cancel(Player p) { Offer o=offers.get(p.getUniqueId()); if(o!=null) { notifyPair(o, ChatColor.RED + plugin.lang().tr("trade.cancelled")); cancel(p.getUniqueId()); } }

    private boolean validate(Offer o){
        PlayerData d=plugin.data().get(o.owner); if(d.coins()<o.coins)return false;
        Map<String,Integer> required = new HashMap<>(o.cards);
        if(o.deckName!=null){ List<String> deck=d.decks().get(o.deckName); if(deck==null)return false; for(String id:deck) required.merge(id,1,Integer::sum); }
        for(var e:required.entrySet())if(d.cardCount(e.getKey())<e.getValue())return false;
        return true;
    }

    private void execute(Offer a,Offer b){
        if(!validate(a)||!validate(b)){ notifyPair(a,ChatColor.RED+plugin.lang().tr("trade.invalid")); cancel(a.owner); return; }
        PlayerData da=plugin.data().get(a.owner), db=plugin.data().get(b.owner);
        da.addCoins(-a.coins+b.coins); db.addCoins(-b.coins+a.coins);
        transferCards(da,db,a.cards); transferCards(db,da,b.cards);
        transferDeck(da,db,a.deckName,a.owner,b.owner); transferDeck(db,da,b.deckName,b.owner,a.owner);
        plugin.data().save(a.owner); plugin.data().save(b.owner);
        notifyPair(a,ChatColor.GREEN+plugin.lang().tr("trade.success"));
        offers.remove(a.owner); offers.remove(b.owner);
        Player pa=Bukkit.getPlayer(a.owner), pb=Bukkit.getPlayer(a.partner); if(pa!=null)pa.closeInventory(); if(pb!=null)pb.closeInventory();
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

    private void refreshPair(Offer o){
        for(UUID id:List.of(o.owner,o.partner)){ Player p=Bukkit.getPlayer(id); if(p!=null && active(id)) plugin.menus().openTrade(p); }
    }
    private void notifyPair(Offer a,String msg){ for(UUID id:List.of(a.owner,a.partner)){Player p=Bukkit.getPlayer(id);if(p!=null)p.sendMessage(plugin.msg(msg));} }
    public void cancel(UUID id){ Offer o=offers.remove(id); if(o!=null)offers.remove(o.partner); requests.remove(id); }
}
