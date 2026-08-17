package fr.cardiaouk.blocktcg.command;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import fr.cardiaouk.blocktcg.card.CardDefinition;
import fr.cardiaouk.blocktcg.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public final class TCGCommand implements CommandExecutor, TabCompleter {
    private final BlockTCGPlugin plugin;
    public TCGCommand(BlockTCGPlugin plugin){this.plugin=plugin;}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length==0||args[0].equalsIgnoreCase("help")){help(sender);return true;}
        String sub=args[0].toLowerCase(Locale.ROOT);
        if(!(sender instanceof Player p) && !Set.of("give","addcoins","reload").contains(sub)){sender.sendMessage("Commande joueur uniquement.");return true;}
        try {
            switch(sub){
                case "collection" -> plugin.menus().openCollection((Player)sender,0);
                case "coins" -> sender.sendMessage(plugin.msg("Pièces: "+plugin.data().get(((Player)sender).getUniqueId()).coins()));
                case "deck" -> handleDeck((Player)sender,args);
                case "challenge" -> {Player t=requirePlayer(sender,args,1); if(t!=null)plugin.duels().challenge((Player)sender,t);}
                case "accept" -> plugin.duels().accept((Player)sender);
                case "trade" -> {Player t=requirePlayer(sender,args,1); if(t!=null)plugin.trades().request((Player)sender,t);}
                case "tradeaccept" -> plugin.trades().accept((Player)sender);
                case "offercard" -> {if(args.length<2){sender.sendMessage("/tcg offercard <id> [quantité]");break;}int q=args.length>2?Integer.parseInt(args[2]):1;plugin.trades().offerCard((Player)sender,args[1],q);}
                case "offercoins" -> {if(args.length<2)break;plugin.trades().offerCoins((Player)sender,Integer.parseInt(args[1]));}
                case "offerdeck" -> {if(args.length<2)break;plugin.trades().offerDeck((Player)sender,args[1]);}
                case "tradestatus" -> plugin.trades().status((Player)sender);
                case "confirmtrade" -> plugin.trades().confirm((Player)sender);
                case "give" -> handleGive(sender,args);
                case "addcoins" -> handleAddCoins(sender,args);
                case "npc" -> handleNpc((Player)sender,args);
                case "reload" -> {if(!sender.hasPermission("blocktcg.admin")){sender.sendMessage(ChatColor.RED+"Permission refusée.");break;}plugin.reloadEverything();sender.sendMessage(plugin.msg("Configuration et cartes rechargées."));}
                case "cards" -> listCards(sender,args);
                default -> help(sender);
            }
        } catch (NumberFormatException ex){sender.sendMessage(plugin.msg(ChatColor.RED+"Nombre invalide."));}
        return true;
    }

    private void handleDeck(Player p,String[] args){
        if(args.length<2){
            PlayerData d=plugin.data().get(p.getUniqueId());
            p.sendMessage(plugin.msg("Decks: "+d.decks().keySet()+" | actif="+d.activeDeck()));
            p.sendMessage(plugin.msg("/tcg deck create <nom> | edit <nom> | delete <nom> | select <nom>")); return;
        }
        String action=args[1].toLowerCase(Locale.ROOT); if(args.length<3){p.sendMessage(plugin.msg("Nom de deck requis."));return;}
        String name=String.join("_",Arrays.copyOfRange(args,2,args.length)); PlayerData d=plugin.data().get(p.getUniqueId());
        switch(action){
            case "create" -> {if(d.decks().containsKey(name)){p.sendMessage(plugin.msg("Ce deck existe déjà."));return;}d.decks().put(name,new ArrayList<>());plugin.data().save(p.getUniqueId());plugin.menus().openDeckEditor(p,name,0);}
            case "edit" -> plugin.menus().openDeckEditor(p,name,0);
            case "delete" -> {d.decks().remove(name);if(name.equalsIgnoreCase(d.activeDeck()))d.setActiveDeck(null);plugin.data().save(p.getUniqueId());p.sendMessage(plugin.msg("Deck supprimé."));}
            case "select" -> {List<String> deck=d.decks().get(name);int need=plugin.getConfig().getInt("game.deck-size",30);if(deck==null||deck.size()!=need){p.sendMessage(plugin.msg("Le deck doit contenir exactement "+need+" cartes."));return;}d.setActiveDeck(name);plugin.data().save(p.getUniqueId());p.sendMessage(plugin.msg("Deck actif: "+name));}
            default -> p.sendMessage(plugin.msg("Action inconnue."));
        }
    }

    private void handleGive(CommandSender sender,String[] args){
        if(!sender.hasPermission("blocktcg.admin")){sender.sendMessage(ChatColor.RED+"Permission refusée.");return;}
        if(args.length<3){sender.sendMessage("/tcg give <joueur> <cardId> [quantité]");return;}
        Player t=Bukkit.getPlayer(args[1]); CardDefinition c=plugin.cards().get(args[2]); int q=args.length>3?Integer.parseInt(args[3]):1;
        if(t==null||c==null){sender.sendMessage("Joueur/carte introuvable.");return;}
        t.getInventory().addItem(plugin.cardItems().create(c,q)); sender.sendMessage(plugin.msg("Carte donnée."));
    }

    private void handleAddCoins(CommandSender sender,String[] args){
        if(!sender.hasPermission("blocktcg.admin")){sender.sendMessage(ChatColor.RED+"Permission refusée.");return;}
        if(args.length<3)return; Player t=Bukkit.getPlayer(args[1]); if(t==null)return; int amount=Integer.parseInt(args[2]);plugin.data().get(t.getUniqueId()).addCoins(amount);plugin.data().save(t.getUniqueId());sender.sendMessage(plugin.msg("Solde modifié."));
    }

    private void handleNpc(Player p,String[] args){
        if(!p.hasPermission("blocktcg.admin")){p.sendMessage(ChatColor.RED+"Permission refusée.");return;}
        if(args.length<3||!args[1].equalsIgnoreCase("create")){p.sendMessage("/tcg npc create <duelist|merchant> [nom]");return;}
        String type=args[2].toLowerCase(Locale.ROOT); if(!type.equals("duelist")&&!type.equals("merchant")){p.sendMessage("Type: duelist ou merchant");return;}
        String name=args.length>3?String.join(" ",Arrays.copyOfRange(args,3,args.length)):(type.equals("duelist")?"Maître des Cartes":"Marchand TCG");
        plugin.npcs().create(p.getLocation(),type,name);p.sendMessage(plugin.msg("NPC créé."));
    }

    private void listCards(CommandSender s,String[] args){
        int page=args.length>1?Math.max(1,Integer.parseInt(args[1])):1; List<CardDefinition> list=plugin.cards().list(); int start=(page-1)*10;
        s.sendMessage(ChatColor.GOLD+"--- Cartes BlockTCG page "+page+" ---");
        for(int i=start;i<Math.min(start+10,list.size());i++){CardDefinition c=list.get(i);s.sendMessage(c.rarity().color()+c.id()+ChatColor.GRAY+" — "+c.name()+" ["+c.theme()+"] "+c.attack()+"/"+c.defense()+" mana "+c.mana());}
    }

    private Player requirePlayer(CommandSender s,String[] args,int idx){if(args.length<=idx){s.sendMessage(plugin.msg("Joueur requis."));return null;}Player t=Bukkit.getPlayer(args[idx]);if(t==null)s.sendMessage(plugin.msg("Joueur introuvable."));return t;}

    private void help(CommandSender s){
        s.sendMessage(ChatColor.GOLD+"===== BlockTCG =====");
        s.sendMessage(ChatColor.YELLOW+"/tcg collection"+ChatColor.GRAY+" - Collection GUI");
        s.sendMessage(ChatColor.YELLOW+"/tcg deck create|edit|delete|select <nom>"+ChatColor.GRAY+" - Gestion des decks");
        s.sendMessage(ChatColor.YELLOW+"/tcg challenge <joueur> / /tcg accept"+ChatColor.GRAY+" - Duel");
        s.sendMessage(ChatColor.YELLOW+"/tcg trade <joueur> / /tcg tradeaccept"+ChatColor.GRAY+" - Échange");
        s.sendMessage(ChatColor.YELLOW+"/tcg offercard <id> [qte], offercoins <n>, offerdeck <nom>, tradestatus, confirmtrade");
        s.sendMessage(ChatColor.YELLOW+"/tcg cards [page] / /tcg coins");
        if(s.hasPermission("blocktcg.admin"))s.sendMessage(ChatColor.RED+"Admin: /tcg give, addcoins, npc create, reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(args.length==1)return filter(List.of("help","collection","coins","deck","cards","challenge","accept","trade","tradeaccept","offercard","offercoins","offerdeck","tradestatus","confirmtrade","give","addcoins","npc","reload"),args[0]);
        if(args.length==2&&Set.of("challenge","trade","give","addcoins").contains(args[0].toLowerCase()))return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),args[1]);
        if(args.length==2&&args[0].equalsIgnoreCase("deck"))return filter(List.of("create","edit","delete","select"),args[1]);
        if(args.length==2&&args[0].equalsIgnoreCase("offercard"))return filter(plugin.cards().list().stream().map(CardDefinition::id).toList(),args[1]);
        if(args.length==3&&args[0].equalsIgnoreCase("give"))return filter(plugin.cards().list().stream().map(CardDefinition::id).toList(),args[2]);
        if(args.length==3&&args[0].equalsIgnoreCase("npc"))return filter(List.of("duelist","merchant"),args[2]);
        return List.of();
    }
    private List<String> filter(List<String> in,String prefix){String p=prefix.toLowerCase();return in.stream().filter(x->x.toLowerCase().startsWith(p)).limit(50).toList();}
}
