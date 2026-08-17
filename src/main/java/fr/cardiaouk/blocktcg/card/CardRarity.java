package fr.cardiaouk.blocktcg.card;

import org.bukkit.ChatColor;

public enum CardRarity {
    COMMON(ChatColor.WHITE),
    UNCOMMON(ChatColor.GREEN),
    RARE(ChatColor.AQUA),
    EPIC(ChatColor.LIGHT_PURPLE),
    LEGENDARY(ChatColor.GOLD);

    private final ChatColor color;
    CardRarity(ChatColor color) { this.color = color; }
    public ChatColor color() { return color; }
}
