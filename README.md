# Configurable Reel Set Generator
Tile proportion preserving rolls generator for slot machine. Very useful for creating no win reels for slot machine with cluster payout strategy, low/high payout reel sets for slot machine with ways and megaways payout strategy and many more.
Full control of reel shuffling, using stack size and minimal distance for two tiles of same type in different stacks.

## Structure
```
src
└─ main
    └── java
          ├── convert 
          |    ├── CayConverter.java
          |    ├── CountConverter.java
          │    └── EvoConverter.java
          ├── dto 
          |    ├── TemplateJsonReelDefinitions.java
          │    └── TemplateJsonReelSetsEvo.java
          ├── exception 
          │    ├── InvalidJsonFormatException.java
          │    ├── NoSuchCompanyFormatException.java
          │    └── NotSupportedOutputMediaType.java
          ├── io
          │    ├── ReaderManager.java
          │    └── WriterManager.java
          ├── reel 
          │    ├── ReelSet.java
          │    ├── ReelSetEvo.java
          │    ├── ReelSetsCollectionData.java
          │    └── Restriction.java
          ├── rng 
          |    ├── IRNG.java
          |    ├── IWeightedRandomBag.java
          │    ├── RNG.java
          │    └── WeightedRandomBag.java
          ├── shuffler 
          |    ├── FlatGenerator.java
          |    ├── RestrictionsApplier.java
          |    └── ShuffleGenerator.java
          ├── wrapper 
          |    └── ConvertWrapper.java
          └── ShuffleWithRestrictions.java
```

## reelDefinitions.json

```json
{
  "mapName": "GAME_NAME",
  "gameId": 123,

  "strategy": "shuffle",
  "output": "file",
  "resultFilePath": "./result.txt",

  "convert": {
    "confirm": false,
    "toCom" : "cay",
    "src": "./result.txt",
    "dest": "./result.txt"
  },

  "reelSets": [
    {
      "tilesCounts": [
        [ 8,34,44,18,68,72,28],
        [22,13,44,54,22,72,76],
        [22,34,16,54,68,27,76],
        [ 8,34,44,18,68,72,28],
        [22,13,44,54,22,72,76],
        [22,34,16,54,68,27,76],
        [ 8,34,44,18,68,72,28]
      ],
      "restrictions": [
        {"stackSizes": [1,2,3,4,5], "stackChances": [12,36,36,10,6], "minDistance": 1}
        {"stackSizes": [2,3,4,5], "stackChances": [33,33,20,14], "minDistance": 2}
        {"stackSizes": [1,3,5], "stackChances": [36,36,28], "minDistance": 3}
      ]
    },
    {
      "tilesCounts": [
        [23,34,16,54,68,27,76],
        [ 8,34,44,18,68,72,28],
        [22,13,44,54,22,72,76],
        [22,34,16,54,68,27,76],
        [ 8,34,44,18,68,72,28],
        [22,13,44,54,22,72,76],
        [22,34,16,54,68,27,76]
      ],
      "restrictions": [
        {"stackSizes": [1,2,3,4,5], "stackChances": [20,20,20,20,20], "minDistance": 1}
      ]
    }
  ]
}

```
As we can observe, we can use alias for property names: restrictions -> res, tileCounts -> cnts and etc. (We can find all alias in reel/Restriction.java).

Let n be the number of reels and k be the number of restrictions.
If n > k, then the i-th restriction applies for all reels, which row number j (0-based enumerating) divided modulo k gives i.
Else the j-th restriction applies for the j-th reel and the last k - n restrictions are ignored.
