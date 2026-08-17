package fr.cardiaouk.blocktcg.deck;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import fr.cardiaouk.blocktcg.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DeckItemFactory {
    private final BlockTCGPlugin plugin;
    private final NamespacedKey ownerKey;
    private final NamespacedKey deckKey;

    public DeckItemFactory(BlockTCGPlugin plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "deck_owner");
        this.deckKey = new NamespacedKey(plugin, "deck_name");
    }

    public ItemStack create(Player owner, String deckName) {
        PlayerData data = plugin.data().get(owner.getUniqueId());
        Material mat = material(data.deckContainer(deckName));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_AQUA + "✦ Deck • " + ChatColor.AQUA + deckName);
        List<String> cards = data.decks().getOrDefault(deckName, List.of());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Boîte de deck BlockTCG");
        lore.add("");
        lore.add(ChatColor.WHITE + "Cartes: " + ChatColor.AQUA + cards.size() + "/" + plugin.getConfig().getInt("game.deck-size", 30));
        lore.add(ChatColor.WHITE + "État: " + (deckName.equalsIgnoreCase(data.activeDeck()) ? ChatColor.GREEN + "ACTIF" : ChatColor.GRAY + "Inactif"));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Clic droit: ouvrir le deck");
        lore.add(ChatColor.DARK_GRAY + "Objet lié à son propriétaire");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        meta.getPersistentDataContainer().set(deckKey, PersistentDataType.STRING, deckName);
        item.setItemMeta(meta);
        return item;
    }

    private Material material(String configured) {
        String fallback = plugin.configs().menus().getString("decks.default-container", "PURPLE_SHULKER_BOX");
        Material mat = Material.matchMaterial(configured == null ? fallback : configured);
        if (mat == null || !(mat == Material.CHEST || mat.name().endsWith("SHULKER_BOX"))) mat = Material.PURPLE_SHULKER_BOX;
        return mat;
    }

    public String getDeckName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(deckKey, PersistentDataType.STRING);
    }

    public UUID getOwner(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try { return UUID.fromString(raw); } catch (IllegalArgumentException ex) { return null; }
    }

    public boolean isDeck(ItemStack item) { return getDeckName(item) != null; }

    public void give(Player player, String deckName) {
        ItemStack item = create(player, deckName);
        var left = player.getInventory().addItem(item);
        left.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }

    public void refresh(Player player, String deckName) {
        remove(player, deckName);
        give(player, deckName);
    }

    public void remove(Player player, String deckName) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (!isDeck(item)) continue;
            UUID owner = getOwner(item);
            String name = getDeckName(item);
            if (player.getUniqueId().equals(owner) && deckName.equalsIgnoreCase(name)) player.getInventory().setItem(slot, null);
        }
    }

    public boolean has(Player player, String deckName) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !isDeck(item)) continue;
            if (player.getUniqueId().equals(getOwner(item)) && deckName.equalsIgnoreCase(getDeckName(item))) return true;
        }
        return false;
    }
}
