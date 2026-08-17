# BlockTCG 1.1.0 — Paper 1.21.11

BlockTCG transforme un serveur Paper en véritable Trading Card Game Minecraft : 120 cartes uniques, collections, decks physiques, duels joueurs/NPC, marchands, échanges et économie.

## Prérequis

- Minecraft / Paper 1.21.11
- Java 21
- Aucune dépendance plugin obligatoire

## Installation

1. Compile le projet avec GitHub Actions ou Gradle.
2. Récupère `BlockTCG-1.1.0.jar`.
3. Place le JAR dans `plugins/`.
4. Démarre Paper 1.21.11.
5. Les fichiers de configuration sont générés dans `plugins/BlockTCG/`.

## Fichiers de configuration

- `config.yml` : règles générales de jeu et taille des decks.
- `cards.yml` : définition des 120 cartes.
- `menus.yml` : présentation des cartes et type de boîte de deck par défaut.
- `economy.yml` : récompenses de duels et prix des marchands.
- `npcs.yml` : comportement des NPC TCG.
- `players/<uuid>.yml` : collection, coins, decks et boîtes de deck de chaque joueur.

## Cartes

Chaque carte possède :

- un nom public ;
- un thème ;
- une rareté ;
- une attaque ;
- une défense ;
- un coût en mana ;
- un item vanilla Minecraft ;
- éventuellement un effet descriptif.

Les IDs techniques sont stockés dans le `PersistentDataContainer`. Ils ne sont pas montrés aux joueurs normaux dans le lore.

Les opérateurs / joueurs ayant `blocktcg.admin` peuvent :

- voir l'ID dans les menus si `cards.show-id-to-admins: true` dans `menus.yml` ;
- utiliser `/tcg cardid` avec une carte en main.

## Decks physiques

Un deck est représenté par un coffre ou une shulker box unique liée au joueur et au deck avec le PDC.

Création :

```text
/tcg deck create chest Mon Deck
/tcg deck create shulker Mon Deck
```

Le clic droit sur la boîte ouvre directement le contenu du deck sans placer le bloc au sol. La pose est annulée par BlockTCG.

Le contenu réel du deck reste sauvegardé côté plugin afin d'éviter les duplications et les corruptions. La boîte est l'accès physique au deck.

Commandes :

```text
/tcg deck
/tcg deck edit <nom>
/tcg deck view <nom>
/tcg deck select <nom>
/tcg deck item <nom>
/tcg deck delete <nom>
```

`/tcg deck item <nom>` régénère proprement la boîte si le joueur l'a perdue.

## Menus

### Collection

```text
/tcg collection
```

Affiche les cartes possédées, le nombre d'exemplaires, les coins, un accès aux decks et au catalogue.

### Catalogue

```text
/tcg cards
/tcg catalog
```

Navigation en trois niveaux :

1. thème ;
2. rareté / catégorie ;
3. liste des cartes correspondantes.

### Decks

```text
/tcg deck
```

Le menu présente chaque deck avec son contenant, son nombre de cartes et son statut actif. Clic gauche pour voir son contenu, clic droit pour le modifier.

## Marchands

Un clic droit sur un NPC marchand ouvre un GUI dédié.

Le menu possède :

- mode Achat ;
- mode Vente ;
- prix visible par carte ;
- solde visible ;
- pagination ;
- vente par 1 carte ;
- Shift + clic pour vendre jusqu'à 5 exemplaires.

Les prix sont entièrement configurables dans `economy.yml`.

## Coins

Les joueurs peuvent notamment gagner des coins :

- en gagnant un duel contre un joueur ;
- en gagnant un duel contre un NPC ;
- en vendant des cartes aux marchands ;
- via une attribution administrateur.

Réglages dans `economy.yml`.

## NPC marchands et duellistes

Syntaxe :

```text
/tcg npc create <merchant|duelist> <ENTITY_TYPE> [nom]
```

Exemples :

```text
/tcg npc create merchant VILLAGER Marchand Royal
/tcg npc create merchant ZOMBIE Marchand des Marais
/tcg npc create duelist SKELETON Maître des Os
/tcg npc create duelist ALLAY Gardien Céleste
```

Tous les `EntityType` que Paper déclare comme réellement spawnables sont acceptés. Les NPC BlockTCG sont persistants, invulnérables et, lorsqu'ils sont des mobs, leur IA peut être désactivée via `npcs.yml`.

## Duels

```text
/tcg challenge <joueur>
/tcg accept
```

Les duellistes NPC démarrent un duel au clic droit.

Un deck actif doit :

- contenir exactement la taille configurée ;
- respecter le nombre maximal d'exemplaires lors de sa construction ;
- correspondre aux cartes réellement possédées au moment du duel.

## Échanges joueurs

```text
/tcg trade <joueur>
/tcg tradeaccept
/tcg offercard <nom de carte> [quantité]
/tcg offercoins <montant>
/tcg offerdeck <nom>
/tcg tradestatus
/tcg confirmtrade
```

Lorsqu'un deck est transféré, ses cartes sont transférées avec lui et une nouvelle boîte liée au nouveau propriétaire est créée.

## Administration

Permission : `blocktcg.admin` (OP par défaut).

```text
/tcg give <joueur> <cardId> [quantité]
/tcg addcoins <joueur> <montant>
/tcg npc create <merchant|duelist> <ENTITY_TYPE> [nom]
/tcg cardid
/tcg reload
```

## Compilation GitHub Actions

Le workflow est fourni dans :

```text
.github/workflows/build.yml
```

Il utilise Java 21 et Gradle 8.10.2. Après un push sur `main` ou `master`, ouvre l'onglet **Actions** de GitHub puis télécharge l'artifact `BlockTCG-jar`.

Le fichier final est généré dans :

```text
build/libs/BlockTCG-1.1.0.jar
```
