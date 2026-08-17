# BlockTCG 1.2.0 — Paper 1.21.11

BlockTCG transforme un serveur Paper en Trading Card Game entièrement configurable : cartes, collections, decks physiques, duels, NPC, marchands dynamiques, boosters, decks préconstruits, échanges GUI et économie.

## Installation / GitHub

Le projet est prêt à être importé à la racine d'un dépôt GitHub.

1. Crée un dépôt GitHub vide.
2. Décompresse le ZIP BlockTCG.
3. Envoie **le contenu du dossier BlockTCG** à la racine du dépôt.
4. Ouvre l'onglet **Actions**.
5. Lance **Build BlockTCG**, ou pousse simplement sur `main`/`master`.
6. Récupère l'artifact `BlockTCG-jar`.
7. Place `BlockTCG-1.2.0.jar` dans le dossier `plugins/` de ton serveur Paper 1.21.11.

Le workflow utilise Java 21 et Gradle 8.10.2.

## Fichiers de configuration

Au premier lancement, le plugin crée :

```text
plugins/BlockTCG/
├── config.yml
├── cards.yml
├── decks.yml
├── boosters.yml
├── economy.yml
├── menus.yml
├── npcs.yml
├── lang/
│   ├── fr.yml
│   ├── en.yml
│   ├── es.yml
│   └── de.yml
└── players/
```

Toutes les données importantes sont modifiables sans recompilation. Utilise ensuite :

```text
/tcg reload
```

## Cartes

Le projet contient **160 cartes uniques**, soit 20 cartes pour chacun des 8 thèmes :

- FORET
- MARAIS
- VOLCAN
- OCEAN
- DESERT
- GIVRE
- CELESTE
- NEANT

Chaque carte est enregistrée dans `cards.yml`.

Exemple :

```yaml
cards:
  dragon_cendre:
    enabled: true
    name: "Dragon de Cendre"
    theme: VOLCAN
    rarity: EPIC
    attack: 15
    defense: 12
    mana: 7
    material: BLAZE_POWDER
    ability: "Une créature née au cœur du volcan."
```

Champs disponibles :

```text
enabled
name
theme
rarity
attack
defense
mana
material
ability
```

Raretés :

```text
COMMON
UNCOMMON
RARE
EPIC
LEGENDARY
```

### Création rapide d'une carte en jeu

```text
/tcg cardadmin create <id>
```

Le plugin ajoute automatiquement un modèle dans `cards.yml`.

Modification rapide :

```text
/tcg cardadmin set dragon_cendre attack 18
/tcg cardadmin set dragon_cendre rarity LEGENDARY
/tcg cardadmin set dragon_cendre material DRAGON_BREATH
/tcg cardadmin set dragon_cendre name Dragon de Cendre Ancien
```

Tu peux aussi modifier directement `cards.yml`, puis lancer `/tcg reload`.

Les IDs techniques sont cachés aux joueurs. Les administrateurs peuvent inspecter une carte avec :

```text
/tcg cardid
```

## Decks physiques

Les decks sont représentés par un coffre ou une shulker.

```text
/tcg deck create chest MonDeck
/tcg deck create shulker MonDeck
```

La boîte de deck :

- est liée au propriétaire ;
- ne peut pas être posée au sol ;
- s'ouvre avec un clic droit ;
- ouvre le contenu virtuel du deck ;
- reste synchronisée avec le deck sauvegardé.

Menu :

```text
/tcg deck
```

## Decks préconstruits

Les decks préconstruits sont définis dans `decks.yml`.

Le projet fournit 8 decks de 30 cartes :

- Forêt Ancestrale
- Marées Profondes
- Flammes Éternelles
- Brumes du Marais
- Empire des Dunes
- Hiver Sans Fin
- Ordre du Firmament
- Appel du Néant

Ils sont vendus par les NPC de rôle `deckmerchant`.

Exemple de création :

```text
/tcg npc create deckmerchant VILLAGER Maître des Decks
```

Chaque deck acheté donne au joueur la boîte de deck et les cartes nécessaires.

## NPC

Rôles disponibles :

```text
merchant

duelist

deckmerchant
```

Création :

```text
/tcg npc create merchant VILLAGER Marchand Royal
/tcg npc create duelist SKELETON Maître des Os
/tcg npc create deckmerchant ALLAY Archiviste des Decks
```

Le type doit être un `EntityType` réellement spawnable par Paper.

### Supprimer un NPC

Place-toi à moins de 10 blocs :

```text
/tcg npc remove
```

Le NPC BlockTCG le plus proche est supprimé.

### Déplacer un NPC

```text
/tcg npc move
```

Le NPC BlockTCG le plus proche est téléporté à ta position.

### Lister les NPC proches

```text
/tcg npc list
```

Affiche type, rôle, UUID et distance.

Tous les NPC BlockTCG sont protégés contre les dégâts. Les mobs peuvent avoir leur IA désactivée via `npcs.yml`.

## Marchands dynamiques

Chaque NPC `merchant` possède son propre stock.

Le stock dépend de :

- l'UUID du NPC ;
- du cycle marchand courant ;
- de la configuration dans `npcs.yml`.

Exemple :

```yaml
merchant:
  card-stock-size: 18
  rotation-minutes: 30
  rotation-check-seconds: 30
```

Ainsi deux marchands peuvent présenter des cartes différentes. Le stock change automatiquement à chaque cycle.

Le marchand possède des interfaces séparées :

- achat de cartes ;
- vente de cartes ;
- boosters.

Les prix sont gérés dans `economy.yml`.

## Boosters

Les boosters sont définis dans `boosters.yml`.

Exemple :

```yaml
boosters:
  premium:
    enabled: true
    name: "Booster Premium"
    material: ENDER_CHEST
    price: 500
    cards: 6
    theme: ALL
    rarity-weights:
      COMMON: 35
      UNCOMMON: 30
      RARE: 20
      EPIC: 11
      LEGENDARY: 4
```

Les poids sont relatifs.

Un clic droit sur un booster lance une ouverture animée :

- titre ;
- suspense temporel ;
- sons ;
- particules ;
- révélation progressive des cartes ;
- son spécial pour les cartes légendaires.

## Échanges joueur ↔ joueur

Les anciennes commandes d'ajout de cartes/coins dans une offre ont été remplacées par une interface graphique.

```text
/tcg trade <joueur>
/tcg tradeaccept
```

Le GUI permet de :

- ajouter/retirer des cartes ;
- augmenter/diminuer les coins ;
- sélectionner un deck complet ;
- voir l'offre de l'autre joueur ;
- confirmer son offre ;
- annuler l'échange.

**Double confirmation obligatoire :** toute modification d'une offre annule automatiquement les confirmations des deux joueurs. L'échange n'est exécuté que lorsque les deux offres sont encore valides et que les deux joueurs ont confirmé.

Si tu fermes le menu pendant un échange actif :

```text
/tcg trade
```

le rouvre.

## Catalogue

```text
/tcg cards
```

Navigation :

```text
Thème → Rareté → Cartes
```

Les IDs techniques ne sont pas nécessaires pour l'utilisation normale.

## Économie

Les joueurs peuvent obtenir des coins notamment grâce à :

- victoire contre un joueur ;
- victoire contre un NPC ;
- vente de cartes ;
- commandes administrateur.

Configuration : `economy.yml`.

## Langues

Langues incluses :

```text
fr — Français
en — English
es — Español
de — Deutsch
```

Dans `config.yml` :

```yaml
language: fr
```

Ou en jeu :

```text
/tcg lang fr
/tcg lang en
/tcg lang es
/tcg lang de
```

Permission admin requise pour modifier la langue globale.

Les traductions sont dans `lang/*.yml` et peuvent être modifiées sans recompilation.

## Commandes principales

```text
/tcg help
/tcg collection
/tcg cards
/tcg coins
/tcg deck
/tcg challenge <joueur>
/tcg accept
/tcg trade <joueur>
/tcg tradeaccept
```

Admin :

```text
/tcg give <joueur> <cardId> [quantité]
/tcg addcoins <joueur> <montant>
/tcg cardid
/tcg cardadmin create <id>
/tcg cardadmin set <id> <champ> <valeur>
/tcg npc create <merchant|duelist|deckmerchant> <ENTITY_TYPE> [nom]
/tcg npc remove
/tcg npc move
/tcg npc list
/tcg lang <fr|en|es|de>
/tcg reload
```

## Permissions

```text
blocktcg.play  — joueur normal
blocktcg.admin — administration / OP par défaut
```

## Compilation locale

Si Gradle est installé :

```bash
gradle clean build
```

JAR :

```text
build/libs/BlockTCG-1.2.0.jar
```

Sinon, utilise directement GitHub Actions fourni dans `.github/workflows/build.yml`.
