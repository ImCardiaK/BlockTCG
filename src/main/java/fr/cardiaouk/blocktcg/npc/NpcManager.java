package fr.cardiaouk.blocktcg.npc;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import fr.cardiaouk.blocktcg.card.CardDefinition;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class NpcManager implements Listener {
    private final BlockTCGPlugin plugin;
    private final NamespacedKey typeKey;
    private final Random random = new Random();

    public NpcManager(BlockTCGPlugin plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "npc_type");
    }

    public Villager create(Location loc, String type, String name) {
        Villager v = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        v.setCustomName(ChatColor.GOLD + name);
        v.setCustomNameVisible(true);
        v.setAI(false);
        v.setInvulnerable(true);
        v.setPersistent(true);
        v.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.toLowerCase());
        return v;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        String type = entity.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (type == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (type.equals("duelist")) {
            plugin.duels().startNpc(p, ChatColor.stripColor(entity.getCustomName()), randomDeck());
        } else if (type.equals("merchant")) {
            if (p.isSneaking()) sellActiveDeck(p); else buyRandomCard(p);
        }
    }

    private void sellActiveDeck(Player p) {
        var data = plugin.data().get(p.getUniqueId());
        String active = data.activeDeck();
        if (active == null || !data.decks().containsKey(active)) {
            p.sendMessage(plugin.msg("Aucun deck actif à vendre."));
            return;
        }
        List<String> deck = data.decks().get(active);
        java.util.Map<String,Integer> needed = new java.util.HashMap<>();
        for (String id : deck) needed.merge(id, 1, Integer::sum);
        for (var e : needed.entrySet()) {
            if (data.cardCount(e.getKey()) < e.getValue()) {
                p.sendMessage(plugin.msg("Ton deck actif contient des cartes que tu ne possèdes plus."));
                return;
            }
        }
        int value = 0;
        for (String id : deck) {
            CardDefinition c = plugin.cards().get(id);
            if (c != null) value += 8 + c.mana() * 3 + c.rarity().ordinal() * 8;
            data.addCard(id, -1);
        }
        data.decks().remove(active);
        data.setActiveDeck(null);
        data.addCoins(value);
        plugin.data().save(p.getUniqueId());
        p.sendMessage(plugin.msg(ChatColor.GOLD + "Deck vendu au marchand pour " + value + " pièces."));
    }

    private List<String> randomDeck() {
        List<CardDefinition> cards = new ArrayList<>(plugin.cards().all());
        List<String> deck = new ArrayList<>();
        int size = plugin.getConfig().getInt("game.deck-size", 30);
        while (deck.size() < size && !cards.isEmpty()) {
            CardDefinition c = cards.get(random.nextInt(cards.size()));
            long copies = deck.stream().filter(c.id()::equals).count();
            if (copies < 2) deck.add(c.id());
        }
        return deck;
    }

    private void buyRandomCard(Player p) {
        var data = plugin.data().get(p.getUniqueId());
        int price = 50;
        if (data.coins() < price) {
            p.sendMessage(plugin.msg("Le marchand demande " + price + " pièces."));
            return;
        }
        List<CardDefinition> cards = new ArrayList<>(plugin.cards().all());
        CardDefinition c = cards.get(random.nextInt(cards.size()));
        data.addCoins(-price);
        data.addCard(c.id(), 1);
        plugin.data().save(p.getUniqueId());
        p.sendMessage(plugin.msg(ChatColor.GREEN + "Achat: " + c.name() + ChatColor.GRAY + " pour " + price + " pièces."));
    }
}
