package fr.cardiaouk.blocktcg.booster;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import fr.cardiaouk.blocktcg.card.CardDefinition;
import fr.cardiaouk.blocktcg.card.CardRarity;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class BoosterManager {
    private final BlockTCGPlugin plugin;
    private final NamespacedKey key;
    private final Random random = new Random();
    private final Set<UUID> opening = new HashSet<>();

    public BoosterManager(BlockTCGPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "booster_id");
    }

    public ItemStack create(BoosterDefinition booster, int amount) {
        ItemStack item = new ItemStack(booster.material(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "✦ " + booster.name());
        meta.setLore(List.of(
                ChatColor.GRAY + plugin.lang().tr("booster.lore-1"),
                ChatColor.GRAY + plugin.lang().tr("booster.lore-2", "count", booster.cards()),
                "",
                ChatColor.YELLOW + plugin.lang().tr("booster.right-click")
        ));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, booster.id());
        item.setItemMeta(meta);
        return item;
    }

    public String getId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public boolean isBooster(ItemStack item) { return getId(item) != null; }

    public void open(Player player, ItemStack item) {
        if (opening.contains(player.getUniqueId())) return;
        BoosterDefinition booster = plugin.boosters().get(getId(item));
        if (booster == null) return;
        opening.add(player.getUniqueId());
        item.setAmount(item.getAmount() - 1);
        player.closeInventory();
        player.sendTitle(ChatColor.GOLD + "✦ " + booster.name(), ChatColor.YELLOW + plugin.lang().tr("booster.opening"), 5, 25, 5);

        long suspenseStep = Math.max(2L, plugin.boosters().config().getLong("animation.suspense-step-ticks", 8L));
        int suspenseSteps = Math.max(1, plugin.boosters().config().getInt("animation.suspense-steps", 5));
        long revealDelay = Math.max(1L, plugin.boosters().config().getLong("animation.reveal-delay-ticks", 8L));
        int particleCount = Math.max(1, plugin.boosters().config().getInt("animation.particle-count", 18));
        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (!player.isOnline()) { opening.remove(player.getUniqueId()); cancel(); return; }
                tick++;
                player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0,1.2,0), particleCount, .45,.55,.45,.15);
                player.playSound(player.getLocation(), tick < 4 ? Sound.BLOCK_NOTE_BLOCK_HAT : Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.8f + tick * 0.12f);
                if (tick < suspenseSteps) return;
                List<CardDefinition> rewards = roll(booster);
                int delay = 0;
                for (CardDefinition card : rewards) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        plugin.data().get(player.getUniqueId()).addCard(card.id(), 1);
                        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0,1.2,0), 24, .5,.6,.5,.1);
                        player.playSound(player.getLocation(), card.rarity() == CardRarity.LEGENDARY ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.1f);
                        player.sendMessage(plugin.msg(card.rarity().color() + "✦ " + card.name()));
                    }, delay);
                    delay += (int) revealDelay;
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.data().save(player.getUniqueId());
                    opening.remove(player.getUniqueId());
                    player.sendMessage(plugin.msg(ChatColor.GREEN + plugin.lang().tr("booster.finished", "count", rewards.size())));
                }, delay + 2L);
                cancel();
            }
        }.runTaskTimer(plugin, 0L, suspenseStep);
    }

    private List<CardDefinition> roll(BoosterDefinition booster) {
        List<CardDefinition> result = new ArrayList<>();
        for (int i = 0; i < booster.cards(); i++) {
            CardRarity rarity = rollRarity(booster.rarityWeights());
            List<CardDefinition> pool = plugin.cards().list().stream()
                    .filter(c -> c.rarity() == rarity)
                    .filter(c -> booster.theme().equalsIgnoreCase("ALL") || c.theme().equalsIgnoreCase(booster.theme()))
                    .toList();
            if (pool.isEmpty()) pool = plugin.cards().list();
            result.add(pool.get(random.nextInt(pool.size())));
        }
        return result;
    }

    private CardRarity rollRarity(Map<String,Integer> weights) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) return CardRarity.COMMON;
        int r = random.nextInt(total);
        for (CardRarity rarity : CardRarity.values()) {
            r -= weights.getOrDefault(rarity.name(), 0);
            if (r < 0) return rarity;
        }
        return CardRarity.COMMON;
    }
}
