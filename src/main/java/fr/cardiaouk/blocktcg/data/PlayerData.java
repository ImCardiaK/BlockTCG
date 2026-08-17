package fr.cardiaouk.blocktcg.data;

import java.util.*;

public final class PlayerData {
    private int coins;
    private final Map<String, Integer> collection = new HashMap<>();
    private final Map<String, List<String>> decks = new LinkedHashMap<>();
    private final Map<String, String> deckContainers = new HashMap<>();
    private String activeDeck;

    public PlayerData(int coins) { this.coins = coins; }

    public int coins() { return coins; }
    public void setCoins(int coins) { this.coins = Math.max(0, coins); }
    public void addCoins(int amount) { setCoins(this.coins + amount); }

    public Map<String, Integer> collection() { return collection; }
    public int cardCount(String id) { return collection.getOrDefault(id, 0); }
    public void addCard(String id, int amount) {
        int next = cardCount(id) + amount;
        if (next <= 0) collection.remove(id); else collection.put(id, next);
    }

    public Map<String, List<String>> decks() { return decks; }
    public String activeDeck() { return activeDeck; }
    public void setActiveDeck(String activeDeck) { this.activeDeck = activeDeck; }

    public String deckContainer(String name) { return deckContainers.get(name); }
    public void setDeckContainer(String name, String material) {
        if (material == null) deckContainers.remove(name); else deckContainers.put(name, material);
    }
    public Map<String, String> deckContainers() { return deckContainers; }
}
