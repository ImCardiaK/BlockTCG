package fr.cardiaouk.blocktcg.card;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class CardItemFactory {
    private final NamespacedKey cardKey;

    public CardItemFactory(BlockTCGPlugin plugin) {
        this.cardKey = new NamespacedKey(plugin, "card_id");
    }

    public ItemStack create(CardDefinition card, int amount) {
        ItemStack item = new ItemStack(card.material(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(card.rarity().color() + "✦ " + card.name());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "ID: " + card.id());
        lore.add(ChatColor.GRAY + "Thème: " + ChatColor.WHITE + card.theme());
        lore.add(card.rarity().color() + "Rareté: " + card.rarity().name());
        lore.add(ChatColor.RED + "⚔ Attaque: " + ChatColor.WHITE + card.attack());
        lore.add(ChatColor.BLUE + "🛡 Défense: " + ChatColor.WHITE + card.defense());
        lore.add(ChatColor.AQUA + "✧ Mana: " + ChatColor.WHITE + card.mana());
        if (!card.ability().isBlank()) {
            lore.add("");
            lore.add(ChatColor.YELLOW + card.ability());
        }
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Clic droit pour ajouter à la collection");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(cardKey, PersistentDataType.STRING, card.id());
        item.setItemMeta(meta);
        return item;
    }

    public String getCardId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(cardKey, PersistentDataType.STRING);
    }
}
