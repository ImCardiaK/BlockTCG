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
import java.util.UUID;

public final class MenuListener implements Listener {
    private final BlockTCGPlugin plugin;
    public MenuListener(BlockTCGPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onUseItem(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        switch (e.getAction()) { case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {} default -> { return; } }
        ItemStack item = e.getItem();
        if (item == null) return;

        if (plugin.deckItems().isDeck(item)) {
            e.setCancelled(true);
            Player p = e.getPlayer();
            UUID owner = plugin.deckItems().getOwner(item);
            String deckName = plugin.deckItems().getDeckName(item);
            if (!p.getUniqueId().equals(owner)) {
                p.sendMessage(plugin.msg(ChatColor.RED + "Cette boîte de deck appartient à un autre joueur."));
                return;
            }
            if (!plugin.data().get(p.getUniqueId()).decks().containsKey(deckName)) {
                p.sendMessage(plugin.msg(ChatColor.RED + "Cette boîte de deck n'est plus valide."));
                return;
            }
            plugin.menus().openDeckContents(p, deckName);
            return;
        }

        String id = plugin.cardItems().getCardId(item);
        if (id == null) return;
        CardDefinition card = plugin.cards().get(id);
        if (card == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        plugin.data().get(p.getUniqueId()).addCard(id, 1);
        plugin.data().save(p.getUniqueId());
        item.setAmount(item.getAmount() - 1);
        p.sendMessage(plugin.msg(ChatColor.GREEN + card.name() + " ajoutée à ta collection."));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (title.startsWith(MenuManager.COLLECTION_TITLE)) { e.setCancelled(true); handleGeneral(p, e); }
        else if (title.equals(MenuManager.DECKS_TITLE)) { e.setCancelled(true); handleDeckList(p, e); }
        else if (title.startsWith(MenuManager.DECK_PREFIX)) { e.setCancelled(true); handleDeckEditor(p, e, title); }
        else if (title.startsWith(MenuManager.DECK_CONTENT_PREFIX)) { e.setCancelled(true); handleGeneral(p, e); }
        else if (title.equals(MenuManager.CATALOG_THEMES_TITLE) || title.startsWith(MenuManager.CATALOG_RARITY_PREFIX) || title.startsWith(MenuManager.CATALOG_LIST_TITLE)) { e.setCancelled(true); handleGeneral(p, e); }
        else if (title.startsWith(MenuManager.MERCHANT_BUY_TITLE)) { e.setCancelled(true); handleMerchantBuy(p, e, title); }
        else if (title.startsWith(MenuManager.MERCHANT_SELL_TITLE)) { e.setCancelled(true); handleMerchantSell(p, e, title); }
        else if (title.equals(MenuManager.BATTLE_TITLE)) { e.setCancelled(true); handleBattle(p, e); }
    }

    private void handleGeneral(Player p, InventoryClickEvent e) {
        String marker = plugin.menus().marker(e.getCurrentItem());
        if (marker == null) return;
        String[] a = marker.split("\\|", -1);
        switch (a[0]) {
            case "collection-page" -> plugin.menus().openCollection(p, Integer.parseInt(a[1]));
            case "catalog" -> plugin.menus().openCatalogThemes(p);
            case "decks" -> plugin.menus().openDeckList(p);
            case "close" -> p.closeInventory();
            case "deck-edit" -> plugin.menus().openDeckEditor(p, a[1], 0);
            case "deck-view" -> plugin.menus().openDeckContents(p, a[1]);
            case "deck-activate" -> activateDeck(p, a[1]);
            case "deck-item" -> { plugin.deckItems().refresh(p, a[1]); p.sendMessage(plugin.msg(ChatColor.GREEN + "Boîte de deck actualisée.")); plugin.menus().openDeckContents(p, a[1]); }
            case "catalog-theme" -> plugin.menus().openCatalogRarities(p, a[1]);
            case "catalog-list" -> plugin.menus().openCatalogList(p, a[1], a[2], Integer.parseInt(a[3]));
            case "merchant-buy" -> plugin.menus().openMerchantBuy(p, Integer.parseInt(a[1]));
            case "merchant-sell" -> plugin.menus().openMerchantSell(p, Integer.parseInt(a[1]));
        }
    }

    private void handleDeckList(Player p, InventoryClickEvent e) {
        String marker = plugin.menus().marker(e.getCurrentItem());
        if (marker == null) return;
        if (marker.equals("catalog")) { plugin.menus().openCatalogThemes(p); return; }
        if (marker.equals("close")) { p.closeInventory(); return; }
        if (!marker.startsWith("deck|")) return;
        String deckName = marker.substring(5);
        if (e.isRightClick()) plugin.menus().openDeckEditor(p, deckName, 0);
        else plugin.menus().openDeckContents(p, deckName);
    }

    private void handleDeckEditor(Player p, InventoryClickEvent e, String title) {
        String deckName = deckNameFromEditorTitle(title);
        if (deckName == null) return;
        String marker = plugin.menus().marker(e.getCurrentItem());
        if (marker != null) {
            String[] a = marker.split("\\|", -1);
            switch (a[0]) {
                case "deck-page" -> plugin.menus().openDeckEditor(p, a[1], Integer.parseInt(a[2]));
                case "deck-view" -> plugin.menus().openDeckContents(p, a[1]);
                case "deck-activate" -> activateDeck(p, a[1]);
                case "deck-item" -> { plugin.deckItems().refresh(p, a[1]); plugin.menus().openDeckEditor(p, a[1], pageFromTitle(title)); }
                case "decks" -> plugin.menus().openDeckList(p);
            }
            return;
        }

        String id = plugin.cardItems().getCardId(e.getCurrentItem());
        if (id == null) return;
        PlayerData data = plugin.data().get(p.getUniqueId());
        List<String> deck = data.decks().get(deckName); if (deck == null) return;
        int max = plugin.getConfig().getInt("game.deck-size", 30);
        int copies = plugin.getConfig().getInt("game.max-copies-per-card", 2);
        long current = deck.stream().filter(id::equalsIgnoreCase).count();
        if (e.isRightClick()) {
            if (current > 0) for (int i = deck.size() - 1; i >= 0; i--) if (deck.get(i).equalsIgnoreCase(id)) { deck.remove(i); break; }
        } else {
            if (deck.size() < max && current < Math.min(copies, data.cardCount(id))) deck.add(id);
            else p.sendMessage(plugin.msg(ChatColor.RED + "Limite atteinte pour cette carte ou deck complet."));
        }
        plugin.data().save(p.getUniqueId());
        if (plugin.deckItems().has(p, deckName)) plugin.deckItems().refresh(p, deckName);
        plugin.menus().openDeckEditor(p, deckName, pageFromTitle(title));
    }

    private void activateDeck(Player p, String deckName) {
        PlayerData data = plugin.data().get(p.getUniqueId());
        List<String> deck = data.decks().get(deckName);
        int required = plugin.getConfig().getInt("game.deck-size", 30);
        if (deck == null || deck.size() != required) {
            p.sendMessage(plugin.msg(ChatColor.RED + "Le deck doit contenir exactement " + required + " cartes pour être actif."));
            return;
        }
        String previous = data.activeDeck();
        data.setActiveDeck(deckName);
        plugin.data().save(p.getUniqueId());
        if (previous != null && plugin.deckItems().has(p, previous)) plugin.deckItems().refresh(p, previous);
        if (plugin.deckItems().has(p, deckName)) plugin.deckItems().refresh(p, deckName);
        plugin.menus().openDeckContents(p, deckName);
    }

    private void handleMerchantBuy(Player p, InventoryClickEvent e, String title) {
        String marker = plugin.menus().marker(e.getCurrentItem());
        if (marker != null) { handleGeneral(p, e); return; }
        String id = plugin.cardItems().getCardId(e.getCurrentItem());
        CardDefinition card = plugin.cards().get(id);
        if (card != null && plugin.npcs().buy(p, card)) plugin.menus().openMerchantBuy(p, pageFromTitle(title));
    }

    private void handleMerchantSell(Player p, InventoryClickEvent e, String title) {
        String marker = plugin.menus().marker(e.getCurrentItem());
        if (marker != null) { handleGeneral(p, e); return; }
        String id = plugin.cardItems().getCardId(e.getCurrentItem());
        CardDefinition card = plugin.cards().get(id);
        if (card != null) {
            int amount = e.isShiftClick() ? Math.min(5, plugin.data().get(p.getUniqueId()).cardCount(id)) : 1;
            if (plugin.npcs().sell(p, card, amount)) plugin.menus().openMerchantSell(p, pageFromTitle(title));
        }
    }

    private void handleBattle(Player p, InventoryClickEvent e) {
        DuelManager.Duel duel = plugin.duels().get(p.getUniqueId());
        if (duel == null) { p.closeInventory(); return; }
        String marker = plugin.menus().marker(e.getCurrentItem());
        if ("endturn".equals(marker)) { plugin.duels().endTurn(p); return; }
        if (e.getRawSlot() >= 45 && e.getRawSlot() <= 53) {
            int handIndex = e.getRawSlot() - 45;
            DuelManager.Fighter f = duel.fighter(p.getUniqueId());
            int board = -1; for (int i = 0; i < 3; i++) if (f.board[i] == null) { board = i; break; }
            if (board < 0) { p.sendMessage(plugin.msg("Ton plateau est plein.")); return; }
            plugin.duels().playCard(p, handIndex, board);
        }
    }

    private int pageFromTitle(String title) {
        String stripped = ChatColor.stripColor(title);
        if (stripped == null) return 0;
        int sep = stripped.lastIndexOf(" • ");
        if (sep < 0) return 0;
        try { return Math.max(0, Integer.parseInt(stripped.substring(sep + 3)) - 1); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private String deckNameFromEditorTitle(String title) {
        String stripped = ChatColor.stripColor(title);
        if (stripped == null) return null;
        String prefix = "BlockTCG • Édition • ";
        if (!stripped.startsWith(prefix)) return null;
        String rest = stripped.substring(prefix.length());
        int sep = rest.lastIndexOf(" • ");
        return sep < 0 ? rest : rest.substring(0, sep);
    }
}
