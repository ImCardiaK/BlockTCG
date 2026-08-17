package fr.cardiaouk.blocktcg.booster;

import org.bukkit.Material;
import java.util.Map;

public record BoosterDefinition(String id, String name, Material material, int price, int cards, Map<String,Integer> rarityWeights, String theme) {}
