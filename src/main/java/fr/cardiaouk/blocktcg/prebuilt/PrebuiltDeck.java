package fr.cardiaouk.blocktcg.prebuilt;

import java.util.List;

public record PrebuiltDeck(String id, String name, String theme, int price, String container, List<String> cards) {}
