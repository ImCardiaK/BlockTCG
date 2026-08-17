package fr.cardiaouk.blocktcg.card;

import org.bukkit.Material;

public record CardDefinition(
        String id,
        String name,
        String theme,
        CardRarity rarity,
        int attack,
        int defense,
        int mana,
        Material material,
        String ability
) {}
