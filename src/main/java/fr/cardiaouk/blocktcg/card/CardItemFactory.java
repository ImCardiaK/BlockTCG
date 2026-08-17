package fr.cardiaouk.blocktcg.card;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class CardItemFactory {
    private final BlockTCGPlugin plugin;
    private final NamespacedKey cardKey;

    public CardItemFactory(BlockTCGPlugin plugin) {
        this.plugin = plugin;
        this.cardKey = new NamespacedKey(plugin, "card_id");
    }

    public ItemStack create(CardDefinition card, int amount) { return create(card, amount, false); }

    public ItemStack createForViewer(CardDefinition card, int amount, Player viewer) {
        boolean showId = viewer != null && viewer.hasPermission("blocktcg.admin") && plugin.configs().menus().getBoolean("cards.show-id-to-admins", true);
        return create(card, amount, showId);
    }

    private ItemStack create(CardDefinition card, int amount, boolean revealId) {
        ItemStack item = new ItemStack(card.material(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(card.rarity().color() + "✦ " + ChatColor.BOLD + card.name());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━");
        lore.add(ChatColor.GRAY + "Thème  " + ChatColor.WHITE + "◆ " + card.theme());
        lore.add(ChatColor.GRAY + "Rareté " + card.rarity().color() + "◆ " + prettyRarity(card.rarity()));
        lore.add(ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━");
        lore.add(ChatColor.RED + "⚔ Attaque  " + ChatColor.WHITE + card.attack());
        lore.add(ChatColor.BLUE + "🛡 Défense  " + ChatColor.WHITE + card.defense());
        lore.add(ChatColor.AQUA + "✧ Mana     " + ChatColor.WHITE + card.mana());
        if (!card.ability().isBlank()) {
            lore.add("");
            lore.add(ChatColor.GOLD + "✦ Effet");
            lore.add(ChatColor.YELLOW + "  " + card.ability());
        }
        if (revealId) {
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "ID admin: " + card.id());
        }
        lore.add(ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(cardKey, PersistentDataType.STRING, card.id());
        item.setItemMeta(meta);
        return item;
    }

    private String prettyRarity(CardRarity rarity) {
        String s = rarity.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public String getCardId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(cardKey, PersistentDataType.STRING);
    }
}
