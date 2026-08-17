package fr.cardiaouk.blocktcg.npc;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import fr.cardiaouk.blocktcg.card.CardDefinition;
import fr.cardiaouk.blocktcg.card.CardRarity;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class NpcManager implements Listener {
    private final BlockTCGPlugin plugin;
    private final NamespacedKey typeKey;
    private final Random random = new Random();

    public NpcManager(BlockTCGPlugin plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "npc_type");
    }

    public Entity create(Location loc, String role, EntityType entityType, String name) {
        if (loc.getWorld() == null) throw new IllegalArgumentException("Monde introuvable.");
        if (!entityType.isSpawnable()) throw new IllegalArgumentException("Ce type d'entité n'est pas spawnable par Bukkit/Paper.");
        Entity entity = loc.getWorld().spawnEntity(loc, entityType);
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&', name));
        entity.setCustomNameVisible(plugin.configs().npcs().getBoolean("display-name", true));
        entity.setInvulnerable(true);
        entity.setPersistent(true);
        if (entity instanceof Mob mob && plugin.configs().npcs().getBoolean("disable-ai", true)) mob.setAI(false);
        entity.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, role.toLowerCase(Locale.ROOT));
        return entity;
    }

    public boolean isTcgNpc(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (isTcgNpc(e.getEntity())) e.setCancelled(true);
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
            plugin.menus().openMerchantBuy(p, 0);
        }
    }

    public int buyPrice(CardDefinition card) {
        int base = plugin.configs().economy().getInt("merchant.buy." + card.rarity().name(), defaultBuy(card.rarity()));
        int attack = plugin.configs().economy().getInt("merchant.stat-value.attack", 2);
        int defense = plugin.configs().economy().getInt("merchant.stat-value.defense", 2);
        int mana = plugin.configs().economy().getInt("merchant.stat-value.mana", 3);
        return Math.max(1, base + card.attack() * attack + card.defense() * defense + card.mana() * mana);
    }

    public int sellPrice(CardDefinition card) {
        int base = plugin.configs().economy().getInt("merchant.sell." + card.rarity().name(), defaultSell(card.rarity()));
        int attack = plugin.configs().economy().getInt("merchant.sell-stat-value.attack", 1);
        int defense = plugin.configs().economy().getInt("merchant.sell-stat-value.defense", 1);
        int mana = plugin.configs().economy().getInt("merchant.sell-stat-value.mana", 1);
        return Math.max(1, base + card.attack() * attack + card.defense() * defense + card.mana() * mana);
    }

    public boolean buy(Player p, CardDefinition card) {
        var data = plugin.data().get(p.getUniqueId());
        int price = buyPrice(card);
        if (data.coins() < price) {
            p.sendMessage(plugin.msg(ChatColor.RED + "Il te manque " + (price - data.coins()) + " coins."));
            return false;
        }
        data.addCoins(-price);
        data.addCard(card.id(), 1);
        plugin.data().save(p.getUniqueId());
        p.sendMessage(plugin.msg(ChatColor.GREEN + "Achat de " + card.name() + " pour " + price + " coins."));
        return true;
    }

    public boolean sell(Player p, CardDefinition card, int amount) {
        var data = plugin.data().get(p.getUniqueId());
        amount = Math.max(1, amount);
        if (data.cardCount(card.id()) < amount) {
            p.sendMessage(plugin.msg(ChatColor.RED + "Tu ne possèdes pas assez d'exemplaires."));
            return false;
        }
        int reward = sellPrice(card) * amount;
        data.addCard(card.id(), -amount);
        data.addCoins(reward);
        plugin.data().save(p.getUniqueId());
        p.sendMessage(plugin.msg(ChatColor.GOLD + "Vente de " + amount + " × " + card.name() + " : +" + reward + " coins."));
        return true;
    }

    private int defaultBuy(CardRarity rarity) {
        return switch (rarity) {
            case COMMON -> 20; case UNCOMMON -> 35; case RARE -> 60; case EPIC -> 110; case LEGENDARY -> 200;
        };
    }

    private int defaultSell(CardRarity rarity) {
        return switch (rarity) {
            case COMMON -> 6; case UNCOMMON -> 12; case RARE -> 22; case EPIC -> 40; case LEGENDARY -> 75;
        };
    }

    private List<String> randomDeck() {
        List<CardDefinition> cards = new ArrayList<>(plugin.cards().all());
        List<String> deck = new ArrayList<>();
        int size = plugin.getConfig().getInt("game.deck-size", 30);
        int maxCopies = plugin.getConfig().getInt("game.max-copies-per-card", 2);
        while (deck.size() < size && !cards.isEmpty()) {
            CardDefinition c = cards.get(random.nextInt(cards.size()));
            long copies = deck.stream().filter(c.id()::equals).count();
            if (copies < maxCopies) deck.add(c.id());
        }
        return deck;
    }
}
