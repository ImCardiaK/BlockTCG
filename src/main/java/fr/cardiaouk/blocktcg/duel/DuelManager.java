package fr.cardiaouk.blocktcg.duel;

import fr.cardiaouk.blocktcg.BlockTCGPlugin;
import fr.cardiaouk.blocktcg.card.CardDefinition;
import fr.cardiaouk.blocktcg.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public final class DuelManager {
    public static final class Unit {
        public final CardDefinition card;
        public int hp;
        public Unit(CardDefinition card) { this.card = card; this.hp = card.defense(); }
    }

    public static final class Fighter {
        public final UUID uuid;
        public final String name;
        public final boolean npc;
        public int health;
        public int mana;
        public int maxMana;
        public final List<String> deck;
        public final List<String> hand = new ArrayList<>();
        public final Unit[] board = new Unit[3];

        Fighter(UUID uuid, String name, boolean npc, int health, List<String> deck) {
            this.uuid = uuid; this.name = name; this.npc = npc; this.health = health;
            this.deck = new ArrayList<>(deck);
            Collections.shuffle(this.deck);
        }
    }

    public static final class Duel {
        public final Fighter a;
        public final Fighter b;
        public UUID turn;
        public boolean finished;
        Duel(Fighter a, Fighter b) { this.a=a; this.b=b; this.turn=a.uuid; }
        public Fighter fighter(UUID id) { return a.uuid.equals(id) ? a : b.uuid.equals(id) ? b : null; }
        public Fighter enemy(UUID id) { return a.uuid.equals(id) ? b : b.uuid.equals(id) ? a : null; }
    }

    private final BlockTCGPlugin plugin;
    private final Map<UUID, Duel> byPlayer = new HashMap<>();
    private final Map<UUID, UUID> requests = new HashMap<>();

    public DuelManager(BlockTCGPlugin plugin) { this.plugin = plugin; }

    public void challenge(Player from, Player to) {
        if (from.equals(to)) { from.sendMessage(plugin.msg("Impossible de te défier toi-même.")); return; }
        if (byPlayer.containsKey(from.getUniqueId()) || byPlayer.containsKey(to.getUniqueId())) {
            from.sendMessage(plugin.msg("Un des joueurs est déjà en duel.")); return;
        }
        requests.put(to.getUniqueId(), from.getUniqueId());
        from.sendMessage(plugin.msg("Défi envoyé à " + to.getName() + "."));
        to.sendMessage(plugin.msg(from.getName() + " te défie. Utilise /tcg accept."));
    }

    public void accept(Player target) {
        UUID challengerId = requests.remove(target.getUniqueId());
        if (challengerId == null) { target.sendMessage(plugin.msg("Aucun défi en attente.")); return; }
        Player challenger = Bukkit.getPlayer(challengerId);
        if (challenger == null) { target.sendMessage(plugin.msg("Le joueur n'est plus connecté.")); return; }
        startPlayers(challenger, target);
    }

    private List<String> activeDeck(Player p) {
        PlayerData data = plugin.data().get(p.getUniqueId());
        String active = data.activeDeck();
        if (active == null) return null;
        List<String> deck = data.decks().get(active);
        int size = plugin.getConfig().getInt("game.deck-size", 30);
        if (deck == null || deck.size() != size) return null;
        Map<String,Integer> required = new HashMap<>();
        for (String id : deck) required.merge(id, 1, Integer::sum);
        for (var e : required.entrySet()) if (data.cardCount(e.getKey()) < e.getValue()) return null;
        return deck;
    }

    public void startPlayers(Player p1, Player p2) {
        List<String> d1 = activeDeck(p1), d2 = activeDeck(p2);
        if (d1 == null || d2 == null) {
            p1.sendMessage(plugin.msg("Chaque joueur doit avoir un deck actif complet."));
            p2.sendMessage(plugin.msg("Chaque joueur doit avoir un deck actif complet."));
            return;
        }
        int hp = plugin.getConfig().getInt("game.starting-health", 30);
        Duel duel = new Duel(new Fighter(p1.getUniqueId(), p1.getName(), false, hp, d1), new Fighter(p2.getUniqueId(), p2.getName(), false, hp, d2));
        initialize(duel);
        byPlayer.put(p1.getUniqueId(), duel); byPlayer.put(p2.getUniqueId(), duel);
        beginTurn(duel, duel.a);
        plugin.menus().openBattle(p1, duel);
        plugin.menus().openBattle(p2, duel);
    }

    public void startNpc(Player player, String npcName, List<String> npcDeck) {
        List<String> d1 = activeDeck(player);
        if (d1 == null) { player.sendMessage(plugin.msg("Tu dois avoir un deck actif complet.")); return; }
        int hp = plugin.getConfig().getInt("game.starting-health", 30);
        UUID npcId = UUID.randomUUID();
        Duel duel = new Duel(new Fighter(player.getUniqueId(), player.getName(), false, hp, d1), new Fighter(npcId, npcName, true, hp, npcDeck));
        initialize(duel); byPlayer.put(player.getUniqueId(), duel); beginTurn(duel, duel.a); plugin.menus().openBattle(player, duel);
    }

    private void initialize(Duel duel) {
        int hand = plugin.getConfig().getInt("game.starting-hand", 5);
        for (int i=0;i<hand;i++) { draw(duel.a); draw(duel.b); }
    }

    private void draw(Fighter f) { if (!f.deck.isEmpty() && f.hand.size() < 9) f.hand.add(f.deck.remove(0)); }

    private void beginTurn(Duel duel, Fighter f) {
        f.maxMana = Math.min(plugin.getConfig().getInt("game.max-mana", 10), f.maxMana + 1);
        f.mana = f.maxMana;
        draw(f);
        duel.turn = f.uuid;
        if (!f.npc) {
            Player p = Bukkit.getPlayer(f.uuid);
            if (p != null) p.sendMessage(plugin.msg(ChatColor.AQUA + "À ton tour — " + f.mana + " mana."));
        } else Bukkit.getScheduler().runTaskLater(plugin, () -> npcTurn(duel), 20L);
    }

    public Duel get(UUID player) { return byPlayer.get(player); }

    public boolean playCard(Player player, int handIndex, int boardIndex) {
        Duel duel = byPlayer.get(player.getUniqueId());
        if (duel == null || duel.finished) return false;
        Fighter f = duel.fighter(player.getUniqueId());
        if (!duel.turn.equals(f.uuid)) { player.sendMessage(plugin.msg("Ce n'est pas ton tour.")); return false; }
        if (handIndex < 0 || handIndex >= f.hand.size() || boardIndex < 0 || boardIndex > 2 || f.board[boardIndex] != null) return false;
        String id = f.hand.get(handIndex); CardDefinition card = plugin.cards().get(id);
        if (card == null) return false;
        if (f.mana < card.mana()) { player.sendMessage(plugin.msg("Mana insuffisant.")); return false; }
        f.mana -= card.mana(); f.hand.remove(handIndex); f.board[boardIndex] = new Unit(card);
        refresh(duel); return true;
    }

    public void endTurn(Player player) {
        Duel duel = byPlayer.get(player.getUniqueId());
        if (duel == null || duel.finished) return;
        Fighter f = duel.fighter(player.getUniqueId());
        if (!duel.turn.equals(f.uuid)) { player.sendMessage(plugin.msg("Ce n'est pas ton tour.")); return; }
        resolveAttacks(duel, f, duel.enemy(f.uuid));
        if (checkEnd(duel)) return;
        beginTurn(duel, duel.enemy(f.uuid)); refresh(duel);
    }

    private void npcTurn(Duel duel) {
        if (duel.finished || !duel.turn.equals(duel.b.uuid)) return;
        Fighter n = duel.b;
        for (int slot=0; slot<3; slot++) {
            if (n.board[slot] != null) continue;
            int chosen = -1;
            for (int i=0;i<n.hand.size();i++) {
                CardDefinition c = plugin.cards().get(n.hand.get(i));
                if (c != null && c.mana() <= n.mana && (chosen < 0 || c.attack() > plugin.cards().get(n.hand.get(chosen)).attack())) chosen = i;
            }
            if (chosen >= 0) {
                CardDefinition c = plugin.cards().get(n.hand.remove(chosen)); n.mana -= c.mana(); n.board[slot] = new Unit(c);
            }
        }
        resolveAttacks(duel, n, duel.a);
        if (checkEnd(duel)) return;
        beginTurn(duel, duel.a); refresh(duel);
    }

    private void resolveAttacks(Duel duel, Fighter attacker, Fighter defender) {
        for (int i=0;i<3;i++) {
            Unit atk = attacker.board[i]; if (atk == null) continue;
            Unit def = defender.board[i];
            if (def == null) defender.health -= atk.card.attack();
            else {
                def.hp -= atk.card.attack();
                atk.hp -= def.card.attack();
                if (def.hp <= 0) defender.board[i] = null;
                if (atk.hp <= 0) attacker.board[i] = null;
            }
        }
    }

    private boolean checkEnd(Duel duel) {
        if (duel.a.health > 0 && duel.b.health > 0) return false;
        duel.finished = true;
        Fighter winner = duel.a.health > 0 ? duel.a : duel.b;
        Fighter loser = winner == duel.a ? duel.b : duel.a;
        if (!winner.npc) {
            Player wp = Bukkit.getPlayer(winner.uuid);
            if (wp != null) {
                int reward = loser.npc ? plugin.configs().economy().getInt("rewards.duel-vs-npc", 75) : plugin.configs().economy().getInt("rewards.duel-vs-player", 25);
                plugin.data().get(winner.uuid).addCoins(reward); plugin.data().save(winner.uuid);
                wp.sendMessage(plugin.msg(ChatColor.GOLD + "Victoire ! +" + reward + " pièces."));
            }
        }
        if (!loser.npc) { Player lp=Bukkit.getPlayer(loser.uuid); if(lp!=null) lp.sendMessage(plugin.msg(ChatColor.RED + "Défaite.")); }
        byPlayer.remove(duel.a.uuid); byPlayer.remove(duel.b.uuid);
        return true;
    }

    private void refresh(Duel duel) {
        if (!duel.a.npc) { Player p=Bukkit.getPlayer(duel.a.uuid); if(p!=null) plugin.menus().openBattle(p,duel); }
        if (!duel.b.npc) { Player p=Bukkit.getPlayer(duel.b.uuid); if(p!=null) plugin.menus().openBattle(p,duel); }
    }
}
