# BlockTCG — Paper 1.21.11

BlockTCG transforme un serveur Minecraft Paper en Trading Card Game jouable directement en jeu.

## Pré-requis

- Minecraft / Paper **1.21.11**
- Java **21**
- Aucune dépendance plugin externe obligatoire

## Fonctionnalités incluses

- **120 cartes uniques** dans `cards.yml`.
- 8 thèmes : `FORET`, `MARAIS`, `VOLCAN`, `OCEAN`, `DESERT`, `GIVRE`, `CELESTE`, `NEANT`.
- Chaque carte possède : item vanilla, nom, thème, rareté, attaque, défense, mana et texte de carte.
- Raretés : COMMON, UNCOMMON, RARE, EPIC, LEGENDARY.
- Cartes physiques Minecraft sécurisées par `PersistentDataContainer`.
- Clic droit sur une carte physique : ajout à la collection virtuelle du joueur.
- Collection persistante YAML par UUID.
- Chaque nouveau joueur reçoit automatiquement un deck `Starter` de 30 cartes et les cartes correspondantes.
- Monnaie interne BlockTCG.
- Création / suppression / sélection de decks.
- Deck-builder en GUI : clic gauche pour ajouter, clic droit pour retirer.
- Deck standard de 30 cartes, 2 exemplaires maximum par carte (configurable).
- Duel joueur contre joueur avec demande/acceptation.
- Duel joueur contre NPC.
- Plateau de 3 lignes, mana progressif, main, points de vie et résolution de combat.
- NPC `duelist` : lance un combat avec deck généré.
- NPC `merchant` : vend une carte aléatoire contre la monnaie interne.
- Échanges joueurs : cartes, pièces et deck complet.
- Sauvegarde des collections, decks et monnaie dans `plugins/BlockTCG/players/`.
- Auto-complétion des commandes.
- GitHub Actions prêt à produire le JAR.

## Installation rapide via GitHub

1. Crée un repository GitHub vide.
2. Décompresse le ZIP BlockTCG et envoie **tout son contenu** à la racine du repository.
3. Commit/push sur `main`.
4. Ouvre l'onglet **Actions** de GitHub.
5. Lance `Build BlockTCG` si le workflow ne s'est pas lancé automatiquement.
6. Dans le run terminé, télécharge l'artifact `BlockTCG-jar`.
7. Mets `BlockTCG-1.0.0.jar` dans le dossier `plugins/` de ton serveur Paper 1.21.11.
8. Redémarre complètement le serveur.

Le workflow utilise Java 21 et Gradle 8.10.2 et compile avec l'API Paper `1.21.11-R0.1-SNAPSHOT`.

## Démarrage en jeu

### Donner des cartes

```text
/tcg give <joueur> <cardId> [quantité]
```

Exemple :

```text
/tcg give Steve foret_01 2
```

Le joueur reçoit l'item Minecraft représentant la carte. Il fait clic droit avec la carte pour l'ajouter à sa collection.

### Voir sa collection

```text
/tcg collection
```

### Créer un deck

```text
/tcg deck create MonDeck
```

Le GUI s'ouvre. Clic gauche sur une carte pour l'ajouter, clic droit pour en retirer un exemplaire.

Pour rouvrir le deck :

```text
/tcg deck edit MonDeck
```

Pour le sélectionner :

```text
/tcg deck select MonDeck
```

Un deck actif doit contenir exactement 30 cartes par défaut.

## Duels joueurs

```text
/tcg challenge <joueur>
/tcg accept
```

Pendant le duel :

- les cartes en main sont en bas du GUI ;
- cliquer une carte la joue dans la première ligne libre si le mana suffit ;
- `Terminer le tour` déclenche les attaques de tes trois lignes ;
- une unité en face absorbe l'attaque et les deux unités s'infligent leurs dégâts ;
- une ligne vide inflige les dégâts directement au joueur ;
- premier joueur à 0 PV perd.

## NPC

Permission requise : `blocktcg.admin`.

Créer un NPC duelliste :

```text
/tcg npc create duelist Maître des Cartes
```

Créer un marchand :

```text
/tcg npc create merchant Marchand TCG
```

Clic droit sur le NPC :

- `duelist` -> démarre un duel ;
- `merchant` -> clic normal : achète une carte aléatoire pour 50 pièces ; **Shift + clic droit** : vend le deck actif complet au marchand contre des pièces.

## Échanges joueurs

Démarrer :

```text
/tcg trade <joueur>
/tcg tradeaccept
```

Construire son offre :

```text
/tcg offercard <cardId> [quantité]
/tcg offercoins <montant>
/tcg offerdeck <nomDuDeck>
/tcg tradestatus
/tcg confirmtrade
```

Utiliser `offerdeck none` pour retirer un deck de l'offre.

Les deux joueurs doivent confirmer. Une modification d'offre réinitialise automatiquement les confirmations.

## Administration

```text
/tcg give <joueur> <cardId> [quantité]
/tcg addcoins <joueur> <montant>
/tcg npc create <duelist|merchant> [nom]
/tcg reload
/tcg cards [page]
```

Permission :

```text
blocktcg.admin
```

## Configuration

`config.yml` permet notamment de modifier :

- monnaie de départ ;
- taille des decks ;
- copies maximales d'une carte ;
- PV de départ ;
- taille de main ;
- mana maximal ;
- récompenses de victoire.

`cards.yml` contient les 120 cartes. Tu peux ajouter de nouvelles cartes avec la même structure :

```yaml
cards:
  mon_id_unique:
    name: "Nom de la carte"
    theme: "FORET"
    rarity: "RARE"
    attack: 8
    defense: 10
    mana: 5
    material: "OAK_SAPLING"
    ability: "Texte de carte."
```

Le `material` doit être un nom valide de `org.bukkit.Material` pour Minecraft 1.21.11.

## Données joueurs

Les fichiers sont générés dans :

```text
plugins/BlockTCG/players/<UUID>.yml
```

Ils contiennent la monnaie, la collection, les decks et le deck actif.

## Notes de version 1.0.0

Cette version est une base TCG complète et autonome. Elle est pensée pour pouvoir être étendue ensuite avec boosters, crafting de boosters, capacités de cartes réellement actives, classement ELO, saisons, marketplace, raretés holographiques, quêtes NPC, boss TCG et interface d'échange 100% GUI.
