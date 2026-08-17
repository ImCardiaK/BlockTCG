package fr.cardiaouk.blocktcg.npc;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import fr.cardiaouk.blocktcg.booster.BoosterDefinition;
import fr.cardiaouk.blocktcg.card.CardDefinition;
import fr.cardiaouk.blocktcg.card.CardRarity;
import fr.cardiaouk.blocktcg.prebuilt.PrebuiltDeck;
import fr.cardiaouk.blocktcg.gui.MenuManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class NpcManager implements Listener {
    private final BlockTCGPlugin plugin;
    private final NamespacedKey typeKey;
    private final Map<UUID, UUID> activeNpc = new HashMap<>();
    private BukkitTask rotationTask;

    public NpcManager(BlockTCGPlugin plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "npc_type");
    }

    public void startRotationTask() {
        if (rotationTask != null) rotationTask.cancel();
        long ticks = Math.max(20L, plugin.configs().npcs().getLong("merchant.rotation-check-seconds", 30L) * 20L);
        rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshOpenMerchantMenus, ticks, ticks);
    }

    public void reloadSettings() { startRotationTask(); }
    public void saveState() { /* Les stocks sont déterministes par UUID + cycle, aucune sauvegarde nécessaire. */ }
    public void clearSession(UUID player) { activeNpc.remove(player); }

    public Entity create(Location loc, String role, EntityType entityType, String name) {
        role = normalizeRole(role);
        if (loc.getWorld() == null) throw new IllegalArgumentException("Monde introuvable.");
        if (!Set.of("merchant", "duelist", "deckmerchant").contains(role)) throw new IllegalArgumentException("Rôle invalide: merchant, duelist ou deckmerchant.");
        if (!entityType.isSpawnable()) throw new IllegalArgumentException("Ce type d'entité n'est pas spawnable par Bukkit/Paper.");
        Entity entity = loc.getWorld().spawnEntity(loc, entityType);
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&', name));
        entity.setCustomNameVisible(plugin.configs().npcs().getBoolean("display-name", true));
        entity.setInvulnerable(true);
        entity.setPersistent(true);
        if (entity instanceof Mob mob && plugin.configs().npcs().getBoolean("disable-ai", true)) mob.setAI(false);
        entity.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, role);
        return entity;
    }

    private String normalizeRole(String role) {
        if (role == null) return "";
        return switch (role.toLowerCase(Locale.ROOT)) {
            case "deck", "deckmerchant", "deck_merchant", "deck-seller" -> "deckmerchant";
            default -> role.toLowerCase(Locale.ROOT);
        };
    }

    public boolean isTcgNpc(Entity entity) { return entity != null && entity.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING); }
    public String role(Entity entity) { return entity == null ? null : entity.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING); }

    public Entity nearest(Player player, double radius) {
        return player.getNearbyEntities(radius, radius, radius).stream()
                .filter(this::isTcgNpc)
                .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);
    }

    public boolean removeNearest(Player player) {
        Entity entity = nearest(player, 10);
        if (entity == null) return false;
        entity.remove(); return true;
    }

    public boolean moveNearest(Player player) {
        Entity entity = nearest(player, 10);
        if (entity == null) return false;
        entity.teleport(player.getLocation()); return true;
    }

    public List<Entity> nearby(Player player, double radius) {
        return player.getNearbyEntities(radius, radius, radius).stream().filter(this::isTcgNpc).toList();
    }

    @EventHandler public void onDamage(EntityDamageEvent e) { if (isTcgNpc(e.getEntity())) e.setCancelled(true); }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        String type = role(entity); if (type == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        activeNpc.put(p.getUniqueId(), entity.getUniqueId());
        switch (type) {
            case "duelist" -> plugin.duels().startNpc(p, displayName(entity), randomDeck(entity.getUniqueId()));
            case "merchant" -> plugin.menus().openMerchantBuy(p, 0);
            case "deckmerchant" -> plugin.menus().openPrebuiltDeckShop(p);
        }
    }

    private String displayName(Entity entity) {
        String name = ChatColor.stripColor(entity.getCustomName());
        return name == null || name.isBlank() ? entity.getType().name() : name;
    }

    public Entity activeNpc(Player p) {
        UUID id = activeNpc.get(p.getUniqueId()); if (id == null) return null;
        for (World w : Bukkit.getWorlds()) { Entity e = w.getEntity(id); if (e != null) return e; }
        return null;
    }

    public List<CardDefinition> currentStock(Player p) {
        Entity npc = activeNpc(p);
        if (npc == null || !"merchant".equals(role(npc))) return List.of();
        return stockFor(npc.getUniqueId());
    }

    public List<CardDefinition> stockFor(UUID npcId) {
        int size = Math.max(1, plugin.configs().npcs().getInt("merchant.card-stock-size", 18));
        long cycle = currentCycle();
        Random random = new Random(npcId.getMostSignificantBits() ^ npcId.getLeastSignificantBits() ^ (cycle * 341873128712L));
        List<CardDefinition> pool = new ArrayList<>(plugin.cards().list());
        Collections.shuffle(pool, random);
        if (pool.size() > size) pool = new ArrayList<>(pool.subList(0, size));
        return pool;
    }

    public long currentCycle() {
        long minutes = Math.max(1L, plugin.configs().npcs().getLong("merchant.rotation-minutes", 30L));
        return System.currentTimeMillis() / (minutes * 60_000L);
    }

    public long secondsUntilRotation() {
        long minutes = Math.max(1L, plugin.configs().npcs().getLong("merchant.rotation-minutes", 30L));
        long period = minutes * 60_000L;
        return Math.max(0L, (period - (System.currentTimeMillis() % period)) / 1000L);
    }

    private void refreshOpenMerchantMenus() {
        for (UUID playerId : new ArrayList<>(activeNpc.keySet())) {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null || !p.isOnline()) continue;
            String title = p.getOpenInventory().getTitle();
            if (title.startsWith(MenuManager.MERCHANT_BUY_TITLE)) plugin.menus().openMerchantBuy(p, 0);
        }
    }

    public int buyPrice(CardDefinition card) {
        int base = plugin.configs().economy().getInt("merchant.buy." + card.rarity().name(), defaultBuy(card.rarity()));
        return Math.max(1, base + card.attack() * plugin.configs().economy().getInt("merchant.stat-value.attack", 2)
                + card.defense() * plugin.configs().economy().getInt("merchant.stat-value.defense", 2)
                + card.mana() * plugin.configs().economy().getInt("merchant.stat-value.mana", 3));
    }

    public int sellPrice(CardDefinition card) {
        int base = plugin.configs().economy().getInt("merchant.sell." + card.rarity().name(), defaultSell(card.rarity()));
        return Math.max(1, base + card.attack() * plugin.configs().economy().getInt("merchant.sell-stat-value.attack", 1)
                + card.defense() * plugin.configs().economy().getInt("merchant.sell-stat-value.defense", 1)
                + card.mana() * plugin.configs().economy().getInt("merchant.sell-stat-value.mana", 1));
    }

    public boolean buy(Player p, CardDefinition card) {
        if (!currentStock(p).stream().anyMatch(c -> c.id().equalsIgnoreCase(card.id()))) {
            p.sendMessage(plugin.msg(ChatColor.RED + plugin.lang().tr("merchant.not-in-stock"))); return false;
        }
        var data = plugin.data().get(p.getUniqueId()); int price = buyPrice(card);
        if (data.coins() < price) { p.sendMessage(plugin.msg(ChatColor.RED + plugin.lang().tr("merchant.not-enough-coins"))); return false; }
        data.addCoins(-price); data.addCard(card.id(), 1); plugin.data().save(p.getUniqueId());
        p.sendMessage(plugin.msg(ChatColor.GREEN + plugin.lang().tr("merchant.bought", "card", card.name(), "price", price))); return true;
    }

    public boolean sell(Player p, CardDefinition card, int amount) {
        var data = plugin.data().get(p.getUniqueId()); amount = Math.max(1, amount);
        if (data.cardCount(card.id()) < amount) { p.sendMessage(plugin.msg(ChatColor.RED + plugin.lang().tr("merchant.not-enough-cards"))); return false; }
        int reward = sellPrice(card) * amount; data.addCard(card.id(), -amount); data.addCoins(reward); plugin.data().save(p.getUniqueId());
        p.sendMessage(plugin.msg(ChatColor.GOLD + plugin.lang().tr("merchant.sold", "count", amount, "card", card.name(), "price", reward))); return true;
    }

    public boolean buyBooster(Player p, BoosterDefinition booster) {
        var data = plugin.data().get(p.getUniqueId());
        if (data.coins() < booster.price()) { p.sendMessage(plugin.msg(ChatColor.RED + plugin.lang().tr("merchant.not-enough-coins"))); return false; }
        data.addCoins(-booster.price()); plugin.data().save(p.getUniqueId());
        var left = p.getInventory().addItem(plugin.boosterManager().create(booster, 1));
        left.values().forEach(stack -> p.getWorld().dropItemNaturally(p.getLocation(), stack));
        return true;
    }

    public boolean buyPrebuiltDeck(Player p, PrebuiltDeck deck) {
        var data = plugin.data().get(p.getUniqueId());
        if (data.coins() < deck.price()) { p.sendMessage(plugin.msg(ChatColor.RED + plugin.lang().tr("merchant.not-enough-coins"))); return false; }
        if (deck.cards().isEmpty()) { p.sendMessage(plugin.msg(ChatColor.RED + "Deck invalide dans decks.yml.")); return false; }
        data.addCoins(-deck.price());
        String name = deck.name(); int suffix = 2; while (data.decks().containsKey(name)) name = deck.name() + " " + suffix++;
        List<String> cards = new ArrayList<>(deck.cards());
        data.decks().put(name, cards); data.setDeckContainer(name, deck.container());
        for (String id : cards) data.addCard(id, 1);
        plugin.data().save(p.getUniqueId()); plugin.deckItems().give(p, name);
        p.sendMessage(plugin.msg(ChatColor.GREEN + plugin.lang().tr("merchant.deck-bought", "deck", name, "price", deck.price())));
        return true;
    }

    private int defaultBuy(CardRarity rarity) { return switch (rarity) { case COMMON -> 20; case UNCOMMON -> 35; case RARE -> 60; case EPIC -> 110; case LEGENDARY -> 200; }; }
    private int defaultSell(CardRarity rarity) { return switch (rarity) { case COMMON -> 6; case UNCOMMON -> 12; case RARE -> 22; case EPIC -> 40; case LEGENDARY -> 75; }; }

    private List<String> randomDeck(UUID npcId) {
        List<CardDefinition> cards = new ArrayList<>(plugin.cards().all());
        Collections.shuffle(cards, new Random(npcId.getMostSignificantBits() ^ currentCycle()));
        List<String> deck = new ArrayList<>(); int size = plugin.getConfig().getInt("game.deck-size", 30); int maxCopies = plugin.getConfig().getInt("game.max-copies-per-card", 2);
        int cursor = 0;
        while (deck.size() < size && !cards.isEmpty()) {
            CardDefinition c = cards.get(cursor++ % cards.size()); long copies = deck.stream().filter(c.id()::equals).count(); if (copies < maxCopies) deck.add(c.id());
        }
        return deck;
    }
}
