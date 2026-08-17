package fr.cardiaouk.blocktcg.command;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import fr.cardiaouk.blocktcg.card.CardDefinition;
import fr.cardiaouk.blocktcg.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.Normalizer;
import java.util.*;

public final class TCGCommand implements CommandExecutor, TabCompleter {
    private final BlockTCGPlugin plugin;
    public TCGCommand(BlockTCGPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) { help(sender); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!(sender instanceof Player) && !Set.of("give", "addcoins", "reload").contains(sub)) {
            sender.sendMessage("Commande joueur uniquement."); return true;
        }
        try {
            switch (sub) {
                case "collection" -> plugin.menus().openCollection((Player) sender, 0);
                case "catalog", "cards" -> plugin.menus().openCatalogThemes((Player) sender);
                case "coins" -> sender.sendMessage(plugin.msg("Coins: " + plugin.data().get(((Player) sender).getUniqueId()).coins()));
                case "deck", "decks" -> handleDeck((Player) sender, args);
                case "challenge" -> { Player t = requirePlayer(sender, args, 1); if (t != null) plugin.duels().challenge((Player) sender, t); }
                case "accept" -> plugin.duels().accept((Player) sender);
                case "trade" -> { Player t = requirePlayer(sender, args, 1); if (t != null) plugin.trades().request((Player) sender, t); }
                case "tradeaccept" -> plugin.trades().accept((Player) sender);
                case "offercard" -> handleOfferCard((Player) sender, args);
                case "offercoins" -> { if (args.length < 2) { sender.sendMessage("/tcg offercoins <montant>"); break; } plugin.trades().offerCoins((Player) sender, Integer.parseInt(args[1])); }
                case "offerdeck" -> { if (args.length < 2) { sender.sendMessage("/tcg offerdeck <nom>"); break; } plugin.trades().offerDeck((Player) sender, String.join(" ", Arrays.copyOfRange(args, 1, args.length))); }
                case "tradestatus" -> plugin.trades().status((Player) sender);
                case "confirmtrade" -> plugin.trades().confirm((Player) sender);
                case "give" -> handleGive(sender, args);
                case "addcoins" -> handleAddCoins(sender, args);
                case "npc" -> handleNpc((Player) sender, args);
                case "cardid" -> handleCardId((Player) sender);
                case "reload" -> {
                    if (!sender.hasPermission("blocktcg.admin")) { sender.sendMessage(ChatColor.RED + "Permission refusée."); break; }
                    plugin.reloadEverything(); sender.sendMessage(plugin.msg("Configurations et cartes rechargées."));
                }
                default -> help(sender);
            }
        } catch (NumberFormatException ex) { sender.sendMessage(plugin.msg(ChatColor.RED + "Nombre invalide.")); }
        return true;
    }

    private void handleDeck(Player p, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("menu")) { plugin.menus().openDeckList(p); return; }
        String action = args[1].toLowerCase(Locale.ROOT);
        PlayerData d = plugin.data().get(p.getUniqueId());

        if (action.equals("create")) {
            if (args.length < 4) { p.sendMessage(plugin.msg("Utilise /tcg deck create <chest|shulker> <nom>")); return; }
            String kind = args[2].toLowerCase(Locale.ROOT);
            String material = switch (kind) {
                case "chest", "coffre" -> "CHEST";
                case "shulker" -> plugin.configs().menus().getString("decks.default-container", "PURPLE_SHULKER_BOX");
                default -> null;
            };
            if (material == null) { p.sendMessage(plugin.msg(ChatColor.RED + "Type requis: chest ou shulker.")); return; }
            String name = safeDeckName(String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
            if (name.isBlank()) { p.sendMessage(plugin.msg(ChatColor.RED + "Nom de deck invalide.")); return; }
            if (d.decks().containsKey(name)) { p.sendMessage(plugin.msg("Ce deck existe déjà.")); return; }
            d.decks().put(name, new ArrayList<>());
            d.setDeckContainer(name, material);
            plugin.data().save(p.getUniqueId());
            plugin.deckItems().give(p, name);
            plugin.menus().openDeckEditor(p, name, 0);
            return;
        }

        if (args.length < 3) { p.sendMessage(plugin.msg("Nom de deck requis.")); return; }
        String name = safeDeckName(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
        switch (action) {
            case "edit" -> plugin.menus().openDeckEditor(p, name, 0);
            case "view", "open" -> plugin.menus().openDeckContents(p, name);
            case "delete" -> {
                if (!d.decks().containsKey(name)) { p.sendMessage(plugin.msg("Deck introuvable.")); return; }
                d.decks().remove(name); d.setDeckContainer(name, null);
                if (name.equalsIgnoreCase(d.activeDeck())) d.setActiveDeck(null);
                plugin.deckItems().remove(p, name); plugin.data().save(p.getUniqueId());
                p.sendMessage(plugin.msg(ChatColor.GREEN + "Deck supprimé."));
            }
            case "select" -> {
                List<String> deck = d.decks().get(name);
                int need = plugin.getConfig().getInt("game.deck-size", 30);
                if (deck == null || deck.size() != need) { p.sendMessage(plugin.msg("Le deck doit contenir exactement " + need + " cartes.")); return; }
                String previous = d.activeDeck(); d.setActiveDeck(name); plugin.data().save(p.getUniqueId());
                if (previous != null && plugin.deckItems().has(p, previous)) plugin.deckItems().refresh(p, previous);
                if (plugin.deckItems().has(p, name)) plugin.deckItems().refresh(p, name);
                p.sendMessage(plugin.msg(ChatColor.GREEN + "Deck actif: " + name));
            }
            case "item" -> {
                if (!d.decks().containsKey(name)) { p.sendMessage(plugin.msg("Deck introuvable.")); return; }
                plugin.deckItems().refresh(p, name);
                p.sendMessage(plugin.msg(ChatColor.GREEN + "Boîte de deck récupérée."));
            }
            default -> p.sendMessage(plugin.msg("Actions: create, edit, view, delete, select, item"));
        }
    }

    private void handleOfferCard(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(plugin.msg("Utilise /tcg offercard <nom de carte> [quantité]")); return; }
        int qty = 1;
        int end = args.length;
        if (args.length > 2) {
            try { qty = Integer.parseInt(args[args.length - 1]); end--; } catch (NumberFormatException ignored) {}
        }
        String query = String.join(" ", Arrays.copyOfRange(args, 1, end));
        CardDefinition card = resolveCard(query);
        if (card == null) { p.sendMessage(plugin.msg(ChatColor.RED + "Carte introuvable. Utilise son nom affiché.")); return; }
        plugin.trades().offerCard(p, card.id(), qty);
    }

    private CardDefinition resolveCard(String query) {
        CardDefinition byId = plugin.cards().get(query);
        if (byId != null) return byId;
        String n = normalize(query);
        return plugin.cards().list().stream().filter(c -> normalize(c.name()).equals(n)).findFirst().orElse(null);
    }

    private String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('_', ' ').trim().toLowerCase(Locale.ROOT);
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocktcg.admin")) { sender.sendMessage(ChatColor.RED + "Permission refusée."); return; }
        if (args.length < 3) { sender.sendMessage("/tcg give <joueur> <cardId> [quantité]"); return; }
        Player t = Bukkit.getPlayer(args[1]); CardDefinition c = plugin.cards().get(args[2]); int q = args.length > 3 ? Integer.parseInt(args[3]) : 1;
        if (t == null || c == null) { sender.sendMessage("Joueur/carte introuvable."); return; }
        t.getInventory().addItem(plugin.cardItems().createForViewer(c, q, t)); sender.sendMessage(plugin.msg("Carte donnée."));
    }

    private void handleAddCoins(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocktcg.admin")) { sender.sendMessage(ChatColor.RED + "Permission refusée."); return; }
        if (args.length < 3) { sender.sendMessage("/tcg addcoins <joueur> <montant>"); return; }
        Player t = Bukkit.getPlayer(args[1]); if (t == null) return;
        int amount = Integer.parseInt(args[2]); plugin.data().get(t.getUniqueId()).addCoins(amount); plugin.data().save(t.getUniqueId()); sender.sendMessage(plugin.msg("Solde modifié."));
    }

    private void handleNpc(Player p, String[] args) {
        if (!p.hasPermission("blocktcg.admin")) { p.sendMessage(ChatColor.RED + "Permission refusée."); return; }
        if (args.length < 4 || !args[1].equalsIgnoreCase("create")) {
            p.sendMessage("/tcg npc create <merchant|duelist> <ENTITY_TYPE> [nom]");
            p.sendMessage(ChatColor.GRAY + "Exemple: /tcg npc create merchant ZOMBIE Marchand des Marais");
            return;
        }
        String role = args[2].toLowerCase(Locale.ROOT);
        if (!role.equals("duelist") && !role.equals("merchant")) { p.sendMessage("Rôle: duelist ou merchant"); return; }
        EntityType entityType;
        try { entityType = EntityType.valueOf(args[3].toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { p.sendMessage(plugin.msg(ChatColor.RED + "EntityType inconnu.")); return; }
        if (!entityType.isSpawnable()) { p.sendMessage(plugin.msg(ChatColor.RED + "Cette entité n'est pas spawnable par Paper.")); return; }
        String name = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : (role.equals("duelist") ? "&6Maître des Cartes" : "&6Marchand TCG");
        try {
            plugin.npcs().create(p.getLocation(), role, entityType, name);
            p.sendMessage(plugin.msg(ChatColor.GREEN + "NPC " + role + " créé avec l'entité " + entityType.name() + "."));
        } catch (IllegalArgumentException ex) { p.sendMessage(plugin.msg(ChatColor.RED + ex.getMessage())); }
    }

    private void handleCardId(Player p) {
        if (!p.hasPermission("blocktcg.admin")) { p.sendMessage(ChatColor.RED + "Permission refusée."); return; }
        ItemStack item = p.getInventory().getItemInMainHand();
        String id = plugin.cardItems().getCardId(item);
        if (id == null) { p.sendMessage(plugin.msg("L'objet en main n'est pas une carte BlockTCG.")); return; }
        CardDefinition card = plugin.cards().get(id);
        p.sendMessage(plugin.msg(ChatColor.DARK_GRAY + "ID technique: " + ChatColor.WHITE + id + (card == null ? "" : ChatColor.GRAY + " (" + card.name() + ")")));
    }

    private String safeDeckName(String name) { return name.replace("|", "-").replace("§", "").trim(); }

    private Player requirePlayer(CommandSender s, String[] args, int idx) {
        if (args.length <= idx) { s.sendMessage(plugin.msg("Joueur requis.")); return null; }
        Player t = Bukkit.getPlayer(args[idx]); if (t == null) s.sendMessage(plugin.msg("Joueur introuvable.")); return t;
    }

    private void help(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "===== BlockTCG =====");
        s.sendMessage(ChatColor.YELLOW + "/tcg collection" + ChatColor.GRAY + " - Ta collection");
        s.sendMessage(ChatColor.YELLOW + "/tcg cards" + ChatColor.GRAY + " - Catalogue par thème et rareté");
        s.sendMessage(ChatColor.YELLOW + "/tcg deck" + ChatColor.GRAY + " - Menu clair de tes decks");
        s.sendMessage(ChatColor.YELLOW + "/tcg deck create <chest|shulker> <nom>" + ChatColor.GRAY + " - Nouveau deck physique");
        s.sendMessage(ChatColor.YELLOW + "/tcg challenge <joueur> / /tcg accept" + ChatColor.GRAY + " - Duel");
        s.sendMessage(ChatColor.YELLOW + "/tcg trade <joueur> / /tcg tradeaccept" + ChatColor.GRAY + " - Échange joueur");
        s.sendMessage(ChatColor.YELLOW + "/tcg offercard <nom> [qte], offercoins <n>, offerdeck <nom>, tradestatus, confirmtrade");
        s.sendMessage(ChatColor.YELLOW + "/tcg coins" + ChatColor.GRAY + " - Solde de coins");
        if (s.hasPermission("blocktcg.admin")) {
            s.sendMessage(ChatColor.RED + "Admin: /tcg give, addcoins, npc create, cardid, reload");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> base = new ArrayList<>(List.of("help", "collection", "coins", "deck", "decks", "cards", "catalog", "challenge", "accept", "trade", "tradeaccept", "offercard", "offercoins", "offerdeck", "tradestatus", "confirmtrade"));
        if (sender.hasPermission("blocktcg.admin")) base.addAll(List.of("give", "addcoins", "npc", "cardid", "reload"));
        if (args.length == 1) return filter(base, args[0]);
        if (args.length == 2 && Set.of("challenge", "trade", "give", "addcoins").contains(args[0].toLowerCase(Locale.ROOT))) return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        if (args.length == 2 && Set.of("deck", "decks").contains(args[0].toLowerCase(Locale.ROOT))) return filter(List.of("menu", "create", "edit", "view", "delete", "select", "item"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("deck") && args[1].equalsIgnoreCase("create")) return filter(List.of("chest", "shulker"), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("blocktcg.admin")) return filter(plugin.cards().list().stream().map(CardDefinition::id).toList(), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("npc") && args[1].equalsIgnoreCase("create")) return filter(List.of("duelist", "merchant"), args[2]);
        if (args.length == 4 && args[0].equalsIgnoreCase("npc") && args[1].equalsIgnoreCase("create")) return filter(Arrays.stream(EntityType.values()).filter(EntityType::isSpawnable).map(EntityType::name).toList(), args[3]);
        return List.of();
    }

    private List<String> filter(List<String> in, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return in.stream().filter(x -> x.toLowerCase(Locale.ROOT).startsWith(p)).limit(80).toList();
    }
}
