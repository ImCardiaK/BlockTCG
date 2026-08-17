package fr.cardiaouk.blocktcg.command;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import fr.cardiaouk.blocktcg.card.CardDefinition;
import fr.cardiaouk.blocktcg.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class TCGCommand implements CommandExecutor, TabCompleter {
    private final BlockTCGPlugin plugin;
    public TCGCommand(BlockTCGPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) { help(sender); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!(sender instanceof Player) && !Set.of("give", "addcoins", "reload", "language", "lang", "cardadmin").contains(sub)) {
            sender.sendMessage("Commande joueur uniquement."); return true;
        }
        try {
            switch (sub) {
                case "collection" -> plugin.menus().openCollection((Player) sender, 0);
                case "catalog", "cards" -> plugin.menus().openCatalogThemes((Player) sender);
                case "coins" -> sender.sendMessage(plugin.msg(plugin.lang().tr("menu.coins", "coins", plugin.data().get(((Player) sender).getUniqueId()).coins())));
                case "deck", "decks" -> handleDeck((Player) sender, args);
                case "challenge" -> { Player t = requirePlayer(sender, args, 1); if (t != null) plugin.duels().challenge((Player) sender, t); }
                case "accept" -> plugin.duels().accept((Player) sender);
                case "trade" -> { Player self=(Player)sender; if(args.length<2 && plugin.trades().active(self.getUniqueId())) plugin.menus().openTrade(self); else { Player t = requirePlayer(sender, args, 1); if (t != null) plugin.trades().request(self, t); } }
                case "tradeaccept" -> plugin.trades().accept((Player) sender);
                case "give" -> handleGive(sender, args);
                case "addcoins" -> handleAddCoins(sender, args);
                case "npc" -> handleNpc((Player) sender, args);
                case "cardid" -> handleCardId((Player) sender);
                case "cardadmin" -> handleCardAdmin(sender, args);
                case "language", "lang" -> handleLanguage(sender, args);
                case "reload" -> {
                    if (!sender.hasPermission("blocktcg.admin")) { sender.sendMessage(ChatColor.RED + "Permission refusée."); break; }
                    plugin.reloadEverything(); sender.sendMessage(plugin.msg(ChatColor.GREEN + "BlockTCG rechargé: cartes, decks, boosters, langues et configurations."));
                }
                default -> help(sender);
            }
        } catch (NumberFormatException ex) { sender.sendMessage(plugin.msg(ChatColor.RED + "Nombre invalide.")); }
        return true;
    }

    private void handleLanguage(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocktcg.admin")) { sender.sendMessage(ChatColor.RED + "Permission refusée."); return; }
        if (args.length < 2) { sender.sendMessage("/tcg lang <fr|en|es|de>"); return; }
        if (!plugin.lang().setLanguage(args[1])) { sender.sendMessage(ChatColor.RED + "Langue disponible: fr, en, es, de"); return; }
        sender.sendMessage(plugin.msg(ChatColor.GREEN + plugin.lang().tr("language.changed", "lang", plugin.lang().current())));
    }

    private void handleCardAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocktcg.admin")) { sender.sendMessage(ChatColor.RED + "Permission refusée."); return; }
        if (args.length < 3) {
            sender.sendMessage("/tcg cardadmin create <id>");
            sender.sendMessage("/tcg cardadmin set <id> <enabled|name|theme|rarity|attack|defense|mana|material|ability> <valeur>");
            sender.sendMessage(ChatColor.GRAY + "Toutes les cartes restent aussi éditables directement dans plugins/BlockTCG/cards.yml.");
            return;
        }
        if (args[1].equalsIgnoreCase("create")) {
            boolean ok=plugin.cards().createTemplate(args[2]);
            sender.sendMessage(plugin.msg(ok?ChatColor.GREEN+"Carte modèle créée dans cards.yml.":ChatColor.RED+"Impossible: ID invalide ou déjà existant.")); return;
        }
        if (args[1].equalsIgnoreCase("set") && args.length >= 5) {
            String value=String.join(" ",Arrays.copyOfRange(args,4,args.length));
            boolean ok=plugin.cards().setField(args[2],args[3],value);
            sender.sendMessage(plugin.msg(ok?ChatColor.GREEN+"Carte modifiée et rechargée.":ChatColor.RED+"Champ/valeur/ID invalide.")); return;
        }
        sender.sendMessage(ChatColor.RED + "Syntaxe invalide. /tcg cardadmin");
    }

    private void handleDeck(Player p, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("menu")) { plugin.menus().openDeckList(p); return; }
        String action = args[1].toLowerCase(Locale.ROOT); PlayerData d = plugin.data().get(p.getUniqueId());
        if (action.equals("create")) {
            if (args.length < 4) { p.sendMessage(plugin.msg("Utilise /tcg deck create <chest|shulker> <nom>")); return; }
            String material = switch (args[2].toLowerCase(Locale.ROOT)) { case "chest", "coffre" -> "CHEST"; case "shulker" -> plugin.configs().menus().getString("decks.default-container", "PURPLE_SHULKER_BOX"); default -> null; };
            if (material == null) { p.sendMessage(plugin.msg(ChatColor.RED + "Type requis: chest ou shulker.")); return; }
            String name = safeDeckName(String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
            if (name.isBlank() || d.decks().containsKey(name)) { p.sendMessage(plugin.msg(ChatColor.RED + "Nom invalide ou deck déjà existant.")); return; }
            d.decks().put(name, new ArrayList<>()); d.setDeckContainer(name, material); plugin.data().save(p.getUniqueId()); plugin.deckItems().give(p, name); plugin.menus().openDeckEditor(p, name, 0); return;
        }
        if (args.length < 3) { p.sendMessage(plugin.msg("Nom de deck requis.")); return; }
        String name = safeDeckName(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
        switch (action) {
            case "edit" -> plugin.menus().openDeckEditor(p, name, 0);
            case "view", "open" -> plugin.menus().openDeckContents(p, name);
            case "delete" -> { if (!d.decks().containsKey(name)) return; d.decks().remove(name); d.setDeckContainer(name, null); if (name.equalsIgnoreCase(d.activeDeck())) d.setActiveDeck(null); plugin.deckItems().remove(p, name); plugin.data().save(p.getUniqueId()); }
            case "select" -> { List<String> deck=d.decks().get(name);int need=plugin.getConfig().getInt("game.deck-size",30);if(deck==null||deck.size()!=need){p.sendMessage(plugin.msg(ChatColor.RED+"Le deck doit avoir "+need+" cartes."));return;}d.setActiveDeck(name);plugin.data().save(p.getUniqueId());plugin.deckItems().refresh(p,name); }
            case "item" -> { if(d.decks().containsKey(name))plugin.deckItems().refresh(p,name); }
            default -> p.sendMessage(plugin.msg("Actions: create, edit, view, delete, select, item"));
        }
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
        if (args.length < 2) { npcHelp(p); return; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if(args.length<4){npcHelp(p);return;}
                String role=args[2].toLowerCase(Locale.ROOT); EntityType entityType;
                try{entityType=EntityType.valueOf(args[3].toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){p.sendMessage(ChatColor.RED+"EntityType inconnu.");return;}
                String name=args.length>4?String.join(" ",Arrays.copyOfRange(args,4,args.length)):"&6BlockTCG NPC";
                try { Entity e=plugin.npcs().create(p.getLocation(),role,entityType,name); p.sendMessage(plugin.msg(ChatColor.GREEN+"NPC créé: "+e.getUniqueId())); }
                catch(IllegalArgumentException ex){p.sendMessage(plugin.msg(ChatColor.RED+ex.getMessage()));}
            }
            case "remove", "delete", "supprimer" -> p.sendMessage(plugin.msg(plugin.npcs().removeNearest(p)?ChatColor.GREEN+"NPC le plus proche supprimé.":ChatColor.RED+"Aucun NPC BlockTCG dans un rayon de 10 blocs."));
            case "move", "deplacer", "déplacer" -> p.sendMessage(plugin.msg(plugin.npcs().moveNearest(p)?ChatColor.GREEN+"NPC déplacé à ta position.":ChatColor.RED+"Aucun NPC BlockTCG dans un rayon de 10 blocs."));
            case "near", "list" -> {
                List<Entity> list=plugin.npcs().nearby(p,20); p.sendMessage(ChatColor.GOLD+"NPC BlockTCG proches: "+list.size());
                for(Entity e:list)p.sendMessage(ChatColor.GRAY+"- "+e.getType()+" / "+plugin.npcs().role(e)+" / "+e.getUniqueId()+" / "+Math.round(e.getLocation().distance(p.getLocation()))+"m");
            }
            default -> npcHelp(p);
        }
    }

    private void npcHelp(Player p){
        p.sendMessage("/tcg npc create <merchant|duelist|deckmerchant> <ENTITY_TYPE> [nom]");
        p.sendMessage("/tcg npc remove  - supprime le NPC BlockTCG le plus proche (10 blocs)");
        p.sendMessage("/tcg npc move    - déplace le NPC BlockTCG le plus proche sur toi");
        p.sendMessage("/tcg npc list    - liste les NPC proches et leurs UUID");
    }

    private void handleCardId(Player p) {
        if (!p.hasPermission("blocktcg.admin")) { p.sendMessage(ChatColor.RED + "Permission refusée."); return; }
        ItemStack item = p.getInventory().getItemInMainHand(); String id = plugin.cardItems().getCardId(item);
        if (id == null) { p.sendMessage(plugin.msg("L'objet en main n'est pas une carte BlockTCG.")); return; }
        CardDefinition card = plugin.cards().get(id); p.sendMessage(plugin.msg(ChatColor.DARK_GRAY + "ID technique: " + ChatColor.WHITE + id + (card == null ? "" : ChatColor.GRAY + " (" + card.name() + ")")));
    }

    private String safeDeckName(String name) { return name.replace("|", "-").replace("§", "").trim(); }
    private Player requirePlayer(CommandSender s, String[] args, int idx) { if (args.length <= idx) { s.sendMessage(plugin.msg("Joueur requis.")); return null; } Player t = Bukkit.getPlayer(args[idx]); if (t == null) s.sendMessage(plugin.msg("Joueur introuvable.")); return t; }

    private void help(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "===== BlockTCG =====");
        s.sendMessage(ChatColor.YELLOW + "/tcg collection, /tcg cards, /tcg deck, /tcg coins");
        s.sendMessage(ChatColor.YELLOW + "/tcg challenge <joueur> / /tcg accept");
        s.sendMessage(ChatColor.YELLOW + "/tcg trade <joueur> / /tcg tradeaccept" + ChatColor.GRAY + " - ouvre l'échange GUI à double confirmation");
        if (s.hasPermission("blocktcg.admin")) {
            s.sendMessage(ChatColor.RED + "Admin: /tcg npc, cardadmin, cardid, give, addcoins, lang, reload");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> base = new ArrayList<>(List.of("help", "collection", "coins", "deck", "decks", "cards", "catalog", "challenge", "accept", "trade", "tradeaccept"));
        if (sender.hasPermission("blocktcg.admin")) base.addAll(List.of("give", "addcoins", "npc", "cardid", "cardadmin", "lang", "reload"));
        if (args.length == 1) return filter(base, args[0]);
        if (args.length == 2 && Set.of("challenge", "trade", "give", "addcoins").contains(args[0].toLowerCase(Locale.ROOT))) return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("npc")) return filter(List.of("create","remove","move","list"),args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("cardadmin")) return filter(List.of("create","set"),args[1]);
        if (args.length == 2 && Set.of("lang","language").contains(args[0].toLowerCase(Locale.ROOT))) return filter(List.of("fr","en","es","de"),args[1]);
        if (args.length == 2 && Set.of("deck", "decks").contains(args[0].toLowerCase(Locale.ROOT))) return filter(List.of("menu", "create", "edit", "view", "delete", "select", "item"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("deck") && args[1].equalsIgnoreCase("create")) return filter(List.of("chest", "shulker"), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) return filter(plugin.cards().list().stream().map(CardDefinition::id).toList(), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("npc") && args[1].equalsIgnoreCase("create")) return filter(List.of("duelist", "merchant", "deckmerchant"), args[2]);
        if (args.length == 4 && args[0].equalsIgnoreCase("npc") && args[1].equalsIgnoreCase("create")) return filter(Arrays.stream(EntityType.values()).filter(EntityType::isSpawnable).map(EntityType::name).toList(), args[3]);
        if (args.length == 4 && args[0].equalsIgnoreCase("cardadmin") && args[1].equalsIgnoreCase("set")) return filter(List.of("enabled","name","theme","rarity","attack","defense","mana","material","ability"),args[3]);
        return List.of();
    }

    private List<String> filter(List<String> in, String prefix) { String p = prefix.toLowerCase(Locale.ROOT); return in.stream().filter(x -> x.toLowerCase(Locale.ROOT).startsWith(p)).limit(80).toList(); }
}
