package fr.cardiaouk.blocktcg.gui;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import fr.cardiaouk.blocktcg.card.CardDefinition;
import fr.cardiaouk.blocktcg.card.CardRarity;
import fr.cardiaouk.blocktcg.data.PlayerData;
import fr.cardiaouk.blocktcg.duel.DuelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class MenuManager {
    private final BlockTCGPlugin plugin;
    private final NamespacedKey actionKey;

    public static final String COLLECTION_TITLE = ChatColor.DARK_GREEN + "BlockTCG • Collection";
    public static final String DECKS_TITLE = ChatColor.DARK_BLUE + "BlockTCG • Mes decks";
    public static final String DECK_PREFIX = ChatColor.DARK_BLUE + "BlockTCG • Édition • ";
    public static final String DECK_CONTENT_PREFIX = ChatColor.DARK_AQUA + "BlockTCG • Deck • ";
    public static final String CATALOG_THEMES_TITLE = ChatColor.DARK_PURPLE + "BlockTCG • Catalogue • Thèmes";
    public static final String CATALOG_RARITY_PREFIX = ChatColor.DARK_PURPLE + "BlockTCG • Catégories • ";
    public static final String CATALOG_LIST_TITLE = ChatColor.DARK_PURPLE + "BlockTCG • Catalogue";
    public static final String MERCHANT_BUY_TITLE = ChatColor.GOLD + "BlockTCG • Marchand • Acheter";
    public static final String MERCHANT_SELL_TITLE = ChatColor.GOLD + "BlockTCG • Marchand • Vendre";
    public static final String BATTLE_TITLE = ChatColor.DARK_RED + "BlockTCG • Duel";

    public MenuManager(BlockTCGPlugin plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "gui_action");
    }

    public void openCollection(Player p, int page) {
        PlayerData data = plugin.data().get(p.getUniqueId());
        List<CardDefinition> owned = plugin.cards().list().stream().filter(c -> data.cardCount(c.id()) > 0).toList();
        int maxPage = Math.max(0, (owned.size() - 1) / 45);
        page = Math.max(0, Math.min(page, maxPage));
        Inventory inv = Bukkit.createInventory(null, 54, COLLECTION_TITLE + " • " + (page + 1));
        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i; if (idx >= owned.size()) break;
            CardDefinition c = owned.get(idx);
            ItemStack item = plugin.cardItems().createForViewer(c, 1, p);
            addLore(item, "", ChatColor.GOLD + "Possédées: " + ChatColor.WHITE + data.cardCount(c.id()));
            inv.setItem(i, item);
        }
        if (page > 0) inv.setItem(45, simple(Material.ARROW, ChatColor.YELLOW + "Page précédente", "collection-page|" + (page - 1)));
        inv.setItem(48, simple(Material.BOOK, ChatColor.LIGHT_PURPLE + "Catalogue complet", "catalog"));
        inv.setItem(49, simple(Material.GOLD_NUGGET, ChatColor.GOLD + "Coins: " + data.coins(), null));
        inv.setItem(50, simple(Material.CHEST, ChatColor.AQUA + "Mes decks", "decks"));
        if (page < maxPage) inv.setItem(53, simple(Material.ARROW, ChatColor.YELLOW + "Page suivante", "collection-page|" + (page + 1)));
        p.openInventory(inv);
    }

    public void openDeckList(Player p) {
        PlayerData data = plugin.data().get(p.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, DECKS_TITLE);
        int slot = 0;
        for (var entry : data.decks().entrySet()) {
            if (slot >= 45) break;
            String name = entry.getKey();
            Material mat = Material.matchMaterial(Objects.requireNonNullElse(data.deckContainer(name), plugin.configs().menus().getString("decks.default-container", "PURPLE_SHULKER_BOX")));
            if (mat == null) mat = Material.CHEST;
            ItemStack icon = simple(mat, (name.equalsIgnoreCase(data.activeDeck()) ? ChatColor.GREEN + "✓ " : ChatColor.AQUA) + name, "deck|" + name);
            addLore(icon,
                    ChatColor.GRAY + "Cartes: " + ChatColor.WHITE + entry.getValue().size() + "/" + plugin.getConfig().getInt("game.deck-size", 30),
                    name.equalsIgnoreCase(data.activeDeck()) ? ChatColor.GREEN + "Deck actif" : ChatColor.GRAY + "Deck inactif",
                    "",
                    ChatColor.YELLOW + "Clic gauche: voir le contenu",
                    ChatColor.GOLD + "Clic droit: modifier");
            inv.setItem(slot++, icon);
        }
        inv.setItem(48, simple(Material.CRAFTING_TABLE, ChatColor.YELLOW + "Créer un deck", null));
        addLore(inv.getItem(48), ChatColor.GRAY + "Commande: /tcg deck create <chest|shulker> <nom>");
        inv.setItem(49, simple(Material.GOLD_NUGGET, ChatColor.GOLD + "Coins: " + data.coins(), null));
        inv.setItem(50, simple(Material.BOOK, ChatColor.LIGHT_PURPLE + "Catalogue", "catalog"));
        inv.setItem(53, simple(Material.BARRIER, ChatColor.RED + "Fermer", "close"));
        p.openInventory(inv);
    }

    public void openDeckContents(Player p, String deckName) {
        PlayerData data = plugin.data().get(p.getUniqueId());
        List<String> deck = data.decks().get(deckName);
        if (deck == null) { p.sendMessage(plugin.msg(ChatColor.RED + "Deck introuvable.")); return; }
        Inventory inv = Bukkit.createInventory(null, 54, DECK_CONTENT_PREFIX + deckName);
        for (int i = 0; i < Math.min(45, deck.size()); i++) {
            CardDefinition card = plugin.cards().get(deck.get(i));
            if (card != null) inv.setItem(i, plugin.cardItems().createForViewer(card, 1, p));
        }
        inv.setItem(45, simple(Material.ARROW, ChatColor.YELLOW + "Retour aux decks", "decks"));
        inv.setItem(48, simple(Material.WRITABLE_BOOK, ChatColor.AQUA + "Modifier ce deck", "deck-edit|" + deckName));
        boolean active = deckName.equalsIgnoreCase(data.activeDeck());
        inv.setItem(49, simple(active ? Material.LIME_DYE : Material.GRAY_DYE,
                active ? ChatColor.GREEN + "Deck actif" : ChatColor.YELLOW + "Définir comme deck actif", "deck-activate|" + deckName));
        inv.setItem(50, simple(Material.CHEST, ChatColor.GOLD + "Récupérer la boîte de deck", "deck-item|" + deckName));
        inv.setItem(53, simple(Material.BARRIER, ChatColor.RED + "Fermer", "close"));
        p.openInventory(inv);
    }

    public void openDeckEditor(Player p, String deckName, int page) {
        PlayerData data = plugin.data().get(p.getUniqueId());
        List<String> deck = data.decks().get(deckName);
        if (deck == null) { p.sendMessage(plugin.msg("Deck introuvable.")); return; }
        List<CardDefinition> owned = plugin.cards().list().stream().filter(c -> data.cardCount(c.id()) > 0).toList();
        int maxPage = Math.max(0, (owned.size() - 1) / 45);
        page = Math.max(0, Math.min(page, maxPage));
        Inventory inv = Bukkit.createInventory(null, 54, DECK_PREFIX + deckName + " • " + (page + 1));
        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i; if (idx >= owned.size()) break;
            CardDefinition c = owned.get(idx);
            ItemStack item = plugin.cardItems().createForViewer(c, 1, p);
            long inDeck = deck.stream().filter(c.id()::equalsIgnoreCase).count();
            addLore(item, "",
                    ChatColor.GRAY + "Collection: " + ChatColor.WHITE + data.cardCount(c.id()) + ChatColor.DARK_GRAY + "  •  " + ChatColor.GRAY + "Deck: " + ChatColor.AQUA + inDeck,
                    ChatColor.GREEN + "Clic gauche: +1",
                    ChatColor.RED + "Clic droit: -1");
            inv.setItem(i, item);
        }
        if (page > 0) inv.setItem(45, simple(Material.ARROW, ChatColor.YELLOW + "Page précédente", "deck-page|" + deckName + "|" + (page - 1)));
        inv.setItem(46, simple(Material.BOOK, ChatColor.AQUA + "Voir le contenu", "deck-view|" + deckName));
        inv.setItem(48, simple(Material.CHEST, ChatColor.GOLD + "Cartes: " + deck.size() + "/" + plugin.getConfig().getInt("game.deck-size", 30), null));
        boolean active = deckName.equalsIgnoreCase(data.activeDeck());
        inv.setItem(49, simple(active ? Material.LIME_DYE : Material.GRAY_DYE, active ? ChatColor.GREEN + "Deck actif" : ChatColor.YELLOW + "Activer le deck", "deck-activate|" + deckName));
        inv.setItem(50, simple(Material.PURPLE_SHULKER_BOX, ChatColor.LIGHT_PURPLE + "Actualiser la boîte", "deck-item|" + deckName));
        inv.setItem(51, simple(Material.ARROW, ChatColor.YELLOW + "Mes decks", "decks"));
        if (page < maxPage) inv.setItem(53, simple(Material.ARROW, ChatColor.YELLOW + "Page suivante", "deck-page|" + deckName + "|" + (page + 1)));
        p.openInventory(inv);
    }

    public void openCatalogThemes(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, CATALOG_THEMES_TITLE);
        List<String> themes = plugin.cards().list().stream().map(CardDefinition::theme).distinct().sorted().toList();
        Material[] icons = {Material.OAK_SAPLING, Material.LILY_PAD, Material.MAGMA_CREAM, Material.HEART_OF_THE_SEA, Material.SAND, Material.SNOWBALL, Material.NETHER_STAR, Material.ENDER_EYE};
        int i = 0;
        for (String theme : themes) {
            ItemStack item = simple(icons[i % icons.length], ChatColor.AQUA + pretty(theme), "catalog-theme|" + theme);
            long count = plugin.cards().list().stream().filter(c -> c.theme().equalsIgnoreCase(theme)).count();
            addLore(item, ChatColor.GRAY + "Cartes: " + ChatColor.WHITE + count, "", ChatColor.YELLOW + "Cliquer pour choisir la rareté");
            inv.setItem(i++, item);
        }
        inv.setItem(22, simple(Material.BOOK, ChatColor.LIGHT_PURPLE + "Toutes les cartes", "catalog-theme|ALL"));
        inv.setItem(26, simple(Material.ARROW, ChatColor.YELLOW + "Retour collection", "collection-page|0"));
        p.openInventory(inv);
    }

    public void openCatalogRarities(Player p, String theme) {
        Inventory inv = Bukkit.createInventory(null, 27, CATALOG_RARITY_PREFIX + pretty(theme));
        int[] slots = {10, 11, 12, 13, 14};
        Material[] mats = {Material.PAPER, Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.NETHER_STAR};
        CardRarity[] rarities = CardRarity.values();
        for (int i = 0; i < rarities.length; i++) {
            CardRarity rarity = rarities[i];
            ItemStack item = simple(mats[Math.min(i, mats.length - 1)], rarity.color() + pretty(rarity.name()), "catalog-list|" + theme + "|" + rarity.name() + "|0");
            addLore(item, ChatColor.GRAY + "Afficher cette catégorie");
            inv.setItem(slots[i], item);
        }
        inv.setItem(16, simple(Material.BOOK, ChatColor.WHITE + "Toutes les raretés", "catalog-list|" + theme + "|ALL|0"));
        inv.setItem(22, simple(Material.ARROW, ChatColor.YELLOW + "Retour aux thèmes", "catalog"));
        p.openInventory(inv);
    }

    public void openCatalogList(Player p, String theme, String rarity, int page) {
        List<CardDefinition> cards = plugin.cards().list().stream()
                .filter(c -> theme.equalsIgnoreCase("ALL") || c.theme().equalsIgnoreCase(theme))
                .filter(c -> rarity.equalsIgnoreCase("ALL") || c.rarity().name().equalsIgnoreCase(rarity))
                .toList();
        int maxPage = Math.max(0, (cards.size() - 1) / 45);
        page = Math.max(0, Math.min(page, maxPage));
        Inventory inv = Bukkit.createInventory(null, 54, CATALOG_LIST_TITLE + " • " + (page + 1));
        PlayerData data = plugin.data().get(p.getUniqueId());
        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i; if (idx >= cards.size()) break;
            CardDefinition card = cards.get(idx);
            ItemStack item = plugin.cardItems().createForViewer(card, 1, p);
            addLore(item, "", ChatColor.GRAY + "Possédées: " + ChatColor.WHITE + data.cardCount(card.id()));
            inv.setItem(i, item);
        }
        if (page > 0) inv.setItem(45, simple(Material.ARROW, ChatColor.YELLOW + "Page précédente", "catalog-list|" + theme + "|" + rarity + "|" + (page - 1)));
        inv.setItem(48, simple(Material.OAK_SIGN, ChatColor.AQUA + "Thème: " + pretty(theme), null));
        inv.setItem(49, simple(Material.NAME_TAG, ChatColor.LIGHT_PURPLE + "Rareté: " + pretty(rarity), null));
        inv.setItem(50, simple(Material.BOOK, ChatColor.YELLOW + "Filtres", "catalog"));
        if (page < maxPage) inv.setItem(53, simple(Material.ARROW, ChatColor.YELLOW + "Page suivante", "catalog-list|" + theme + "|" + rarity + "|" + (page + 1)));
        p.openInventory(inv);
    }

    public void openMerchantBuy(Player p, int page) {
        List<CardDefinition> cards = plugin.cards().list();
        int maxPage = Math.max(0, (cards.size() - 1) / 45);
        page = Math.max(0, Math.min(page, maxPage));
        Inventory inv = Bukkit.createInventory(null, 54, MERCHANT_BUY_TITLE + " • " + (page + 1));
        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i; if (idx >= cards.size()) break;
            CardDefinition card = cards.get(idx);
            ItemStack item = plugin.cardItems().createForViewer(card, 1, p);
            addLore(item, "", ChatColor.GOLD + "Prix d'achat: " + ChatColor.WHITE + plugin.npcs().buyPrice(card) + " coins", ChatColor.GREEN + "Cliquer pour acheter");
            inv.setItem(i, item);
        }
        if (page > 0) inv.setItem(45, simple(Material.ARROW, ChatColor.YELLOW + "Page précédente", "merchant-buy|" + (page - 1)));
        inv.setItem(48, simple(Material.EMERALD, ChatColor.GREEN + "Mode VENTE", "merchant-sell|0"));
        inv.setItem(49, simple(Material.GOLD_NUGGET, ChatColor.GOLD + "Tes coins: " + plugin.data().get(p.getUniqueId()).coins(), null));
        inv.setItem(50, simple(Material.BOOK, ChatColor.LIGHT_PURPLE + "Catalogue", "catalog"));
        if (page < maxPage) inv.setItem(53, simple(Material.ARROW, ChatColor.YELLOW + "Page suivante", "merchant-buy|" + (page + 1)));
        p.openInventory(inv);
    }

    public void openMerchantSell(Player p, int page) {
        PlayerData data = plugin.data().get(p.getUniqueId());
        List<CardDefinition> cards = plugin.cards().list().stream().filter(c -> data.cardCount(c.id()) > 0).toList();
        int maxPage = Math.max(0, (cards.size() - 1) / 45);
        page = Math.max(0, Math.min(page, maxPage));
        Inventory inv = Bukkit.createInventory(null, 54, MERCHANT_SELL_TITLE + " • " + (page + 1));
        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i; if (idx >= cards.size()) break;
            CardDefinition card = cards.get(idx);
            ItemStack item = plugin.cardItems().createForViewer(card, 1, p);
            addLore(item, "",
                    ChatColor.GRAY + "Possédées: " + ChatColor.WHITE + data.cardCount(card.id()),
                    ChatColor.GOLD + "Valeur: " + ChatColor.WHITE + plugin.npcs().sellPrice(card) + " coins / carte",
                    ChatColor.GREEN + "Clic gauche: vendre 1",
                    ChatColor.YELLOW + "Shift + clic: vendre jusqu'à 5");
            inv.setItem(i, item);
        }
        if (page > 0) inv.setItem(45, simple(Material.ARROW, ChatColor.YELLOW + "Page précédente", "merchant-sell|" + (page - 1)));
        inv.setItem(48, simple(Material.CHEST, ChatColor.YELLOW + "Mode ACHAT", "merchant-buy|0"));
        inv.setItem(49, simple(Material.GOLD_NUGGET, ChatColor.GOLD + "Tes coins: " + data.coins(), null));
        if (page < maxPage) inv.setItem(53, simple(Material.ARROW, ChatColor.YELLOW + "Page suivante", "merchant-sell|" + (page + 1)));
        p.openInventory(inv);
    }

    public void openBattle(Player viewer, DuelManager.Duel duel) {
        DuelManager.Fighter self = duel.fighter(viewer.getUniqueId()), enemy = duel.enemy(viewer.getUniqueId()); if (self == null) return;
        Inventory inv = Bukkit.createInventory(null, 54, BATTLE_TITLE);
        inv.setItem(4, simple(Material.NETHER_STAR, ChatColor.GOLD + enemy.name + " ❤ " + Math.max(0, enemy.health), null));
        inv.setItem(13, simple(Material.HEART_OF_THE_SEA, ChatColor.AQUA + "Tour: " + (duel.turn.equals(self.uuid) ? "TOI" : enemy.name), null));
        for (int i = 0; i < 3; i++) {
            if (enemy.board[i] != null) inv.setItem(19 + i, unitItem(viewer, enemy.board[i]));
            if (self.board[i] != null) inv.setItem(28 + i, unitItem(viewer, self.board[i]));
            else inv.setItem(28 + i, simple(Material.LIGHT_GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Emplacement " + (i + 1), "board|" + i));
        }
        inv.setItem(36, simple(Material.REDSTONE, ChatColor.RED + "PV: " + Math.max(0, self.health), null));
        inv.setItem(40, simple(Material.CLOCK, ChatColor.YELLOW + "Terminer le tour", "endturn"));
        inv.setItem(44, simple(Material.LAPIS_LAZULI, ChatColor.AQUA + "Mana: " + self.mana + "/" + self.maxMana, null));
        for (int i = 0; i < Math.min(9, self.hand.size()); i++) {
            CardDefinition c = plugin.cards().get(self.hand.get(i)); if (c == null) continue;
            ItemStack item = plugin.cardItems().createForViewer(c, 1, viewer);
            addLore(item, ChatColor.GREEN + "Clique pour jouer sur le premier emplacement libre");
            inv.setItem(45 + i, item);
        }
        viewer.openInventory(inv);
    }

    private ItemStack unitItem(Player viewer, DuelManager.Unit u) {
        ItemStack item = plugin.cardItems().createForViewer(u.card, 1, viewer);
        addLore(item, ChatColor.RED + "PV unité: " + u.hp + "/" + u.card.defense());
        return item;
    }

    public ItemStack simple(Material mat, String name, String marker) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(name);
        if (marker != null) m.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, marker);
        i.setItemMeta(m);
        return i;
    }

    public String marker(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    private void addLore(ItemStack item, String... lines) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>(Objects.requireNonNullElse(meta.getLore(), List.of()));
        lore.addAll(Arrays.asList(lines));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private String pretty(String raw) {
        if (raw == null || raw.isBlank()) return "Tous";
        if (raw.equalsIgnoreCase("ALL")) return "Tous";
        String s = raw.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
