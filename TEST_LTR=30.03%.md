## General:
```json
{
  "primaryReelsBaseReelCounts": [60, 50, 40, 30, 15, 12, 10, 6],
  "secondaryReelsBaseReelCounts": [40, 35, 30, 25, 15, 12, 10, 6],
  "hitRateSymbolReelCounts": [130, 110, 80, 50, 30],
  "spiralGapComplementSymbol": [8],
  "stackSizes": [1,2,3,4,5],
  "stackChances": [43.5, 37.0, 16.4, 1.9, 1.2],
  "minDistance": 1,
  "reelSetChances": [18.0, 18.0, 18.0, 11.3, 9.4, 8.2, 7.2, 4.0, 3.0, 2.0, 0.9]
}
```

## Reel Sets:
```json
[
  {
    "reelSetName": "ReelSet#0:NW:0,1,1",
    "tilesCounts": [
      [0, 50, 40, 0, 15, 12, 0, 6],
      [60, 0, 40, 30, 0, 12, 10, 0],
      [60, 50, 0, 30, 15, 0, 10, 6],
      [40, 35, 30, 25, 15, 12, 10, 6],
      [40, 35, 30, 25, 15, 12, 10, 6]
    ],
    "restrictions": [
      {"stackSizes": [1, 2, 3, 4, 5], "stackChances": [43.5, 37, 16.4, 1.9, 1.2], "minDistance": 1}
    ]
  },
  {
    "reelSetName": "ReelSet#1:NW:1,0,1",
    "tilesCounts": [
      [60, 50, 0, 30, 15, 0, 10, 6],
      [0, 50, 40, 0, 15, 12, 0, 6],
      [60, 0, 40, 30, 0, 12, 10, 0],
      [40, 35, 30, 25, 15, 12, 10, 6],
      [40, 35, 30, 25, 15, 12, 10, 6]
    ],
    "restrictions": [
      {"stackSizes": [1, 2, 3, 4, 5], "stackChances": [43.5, 37, 16.4, 1.9, 1.2], "minDistance": 1}
    ]
  },
  {
    "reelSetName": "ReelSet#2:NW:1,1,0",
    "tilesCounts": [
      [60, 0, 40, 30, 0, 12, 10, 0],
      [60, 50, 0, 30, 15, 0, 10, 6],
      [0, 50, 40, 0, 15, 12, 0, 6],
      [40, 35, 30, 25, 15, 12, 10, 6],
      [40, 35, 30, 25, 15, 12, 10, 6]
    ],
    "restrictions": [
      {"stackSizes": [1, 2, 3, 4, 5], "stackChances": [43.5, 37, 16.4, 1.9, 1.2], "minDistance": 1}
    ]
  },
  {
    "reelSetName": "ReelSet#3:HR:1:0,1,1",
    "tilesCounts": [
      [130, 50, 40, 8, 15, 12, 8, 6],
      [110, 8, 40, 30, 8, 12, 10, 8],
      [80, 50, 8, 30, 15, 8, 10, 6],
      [50, 35, 30, 25, 15, 12, 10, 6],
      [30, 35, 30, 25, 15, 12, 10, 6]
    ],
    "restrictions": [
      {"stackSizes": [1, 2, 3, 4, 5], "stackChances": [43.5, 37, 16.4, 1.9, 1.2], "minDistance": 1}
    ]
  },
  {
    "reelSetName": "ReelSet#4:HR:2:1,0,1",
    "tilesCounts": [
      [60, 130, 8, 30, 15, 8, 10, 6],
      [8, 110, 40, 8, 15, 12, 8, 6],
      [60, 80, 40, 30, 8, 12, 10, 8],
      [40, 50, 30, 25, 15, 12, 10, 6],
      [40, 30, 30, 25, 15, 12, 10, 6]
    ],
    "restrictions": [
      {"stackSizes": [1, 2, 3, 4, 5], "stackChances": [43.5, 37, 16.4, 1.9, 1.2], "minDistance": 1}
    ]
  },
  {
    "reelSetName": "ReelSet#5:HR:3:1,1,0",
    "tilesCounts": [
      [60, 8, 130, 30, 8, 12, 10, 8],
      [60, 50, 110, 30, 15, 8, 10, 6],
      [8, 50, 80, 8, 15, 12, 8, 6],
      [40, 35, 50, 25, 15, 12, 10, 6],
      [40, 35, 30, 25, 15, 12, 10, 6]
    ],
    "restrictions": [
      {"stackSizes": [1, 2, 3, 4, 5], "stackChances": [43.5, 37, 16.4, 1.9, 1.2], "minDistance": 1}
    ]
  },
  {
    "reelSetName": "ReelSet#6:HR:4:0,1,1",
    "tilesCounts": [
      [8, 50, 40, 130, 15, 12, 8, 6],
      [60, 8, 40, 110, 8, 12, 10, 8],
      [60, 50, 8, 80, 15, 8, 10, 6],
      [40, 35, 30, 50, 15, 12, 10, 6],
      [40, 35, 30, 30, 15, 12, 10, 6]
    ],
    "restrictions": [
      {"stackSizes": [1, 2, 3, 4, 5], "stackChances": [43.5, 37, 16.4, 1.9, 1.2], "minDistance": 1}
    ]
  },
  {
    "reelSetName": "ReelSet#7:HR:5:1,0,1",
    "tilesCounts": [
      [60, 50, 8, 30, 130, 8, 10, 6],
      [8, 50, 40, 8, 110, 12, 8, 6],
      [60, 8, 40, 30, 80, 12, 10, 8],
      [40, 35, 30, 25, 50, 12, 10, 6],
      [40, 35, 30, 25, 30, 12, 10, 6]
    ],
    "restrictions": [
      {"stackSizes": [1, 2, 3, 4, 5], "stackChances": [43.5, 37, 16.4, 1.9, 1.2], "minDistance": 1}
    ]
  },
  {
    "reelSetName": "ReelSet#8:HR:6:1,1,0",
    "tilesCounts": [
      [60, 8, 40, 30, 8, 130, 10, 8],
      [60, 50, 8, 30, 15, 110, 10, 6],
      [8, 50, 40, 8, 15, 80, 8, 6],
      [40, 35, 30, 25, 15, 50, 10, 6],
      [40, 35, 30, 25, 15, 30, 10, 6]
    ],
    "restrictions": [
      {"stackSizes": [1, 2, 3, 4, 5], "stackChances": [43.5, 37, 16.4, 1.9, 1.2], "minDistance": 1}
    ]
  },
  {
    "reelSetName": "ReelSet#9:HR:7:0,1,1",
    "tilesCounts": [
      [8, 50, 40, 8, 15, 12, 130, 6],
      [60, 8, 40, 30, 8, 12, 110, 8],
      [60, 50, 8, 30, 15, 8, 80, 6],
      [40, 35, 30, 25, 15, 12, 50, 6],
      [40, 35, 30, 25, 15, 12, 30, 6]
    ],
    "restrictions": [
      {"stackSizes": [1, 2, 3, 4, 5], "stackChances": [43.5, 37, 16.4, 1.9, 1.2], "minDistance": 1}
    ]
  },
  {
    "reelSetName": "ReelSet#10:HR:8:1,0,1",
    "tilesCounts": [
      [60, 50, 8, 30, 15, 8, 10, 130],
      [8, 50, 40, 8, 15, 12, 8, 110],
      [60, 8, 40, 30, 8, 12, 10, 80],
      [45, 35, 30, 25, 15, 12, 10, 50],
      [45, 35, 30, 25, 15, 12, 10, 30]
    ],
    "restrictions": [
      {"stackSizes": [1, 2, 3, 4, 5], "stackChances": [43.5, 37, 16.4, 1.9, 1.2], "minDistance": 1}
    ]
  }
]
```

## Screen and Match Combination 
```json
{
  "screen": {
    "cols": 5,
    "rows": 4
  },
  "minMatch": 3,
  "lineDefinitions": [
    [0,0,0,0,0], [0,1,0,1,0],
    [0,0,1,0,0], [0,0,1,2,3],
    [0,1,2,1,0], [1,1,1,1,1],
    [1,2,1,2,1], [1,0,1,0,1],
    [1,1,2,1,1], [1,1,0,1,1],
    [2,2,2,2,2], [2,3,2,3,2],
    [2,1,2,1,2], [2,2,3,2,2],
    [2,2,1,2,2], [3,3,3,3,3],
    [3,2,3,2,3], [3,3,2,3,3],
    [3,3,2,1,0], [3,2,1,2,3]
  ]
}
```

## Symbol Configuration and Paytable
```json
{
  "symbols": {
    "normal": [1, 2, 3, 4, 5, 6, 7, 8],
    "wild": [],
    "scatter": [],
    "blank": [],
    "locked": []
  },
  "paytableType": "STRICT",
  "payTable": {
    "1": [0.1, 0.2, 0.4],
    "2": [0.1, 0.3, 0.6],
    "3": [0.2, 0.5, 1.0],
    "4": [0.3, 0.6, 1.5],
    "5": [0.5, 1.0, 2.0],
    "6": [1.0, 1.5, 2.5],
    "7": [1.5, 2.5, 5.0],
    "8": [2.5, 5.0, 15.0]
  }
}
```

## RTP Result
```json
{
  "rtpPercent": 30.03218699996797,
  "totalSpins": 10000000,
  "elapsedMs": 1300,
  "betSize": 1,
  "avgWin": 1.1996195290055893,
  "medianWin": 0.6,
  "maxWin": 140,
  "stdDev": 1.2204557973964303,
  "volatilityIndex": 4.063825912504248,
  "volatilityLabel": "Medium",
  "hitRatePct": 25.03476,
  "comboBreakdown": [
    {
      "tileId": 1,
      "length": 3,
      "hits": 1598556,
      "hitRate": "15.9856%",
      "multiplier": 0.1,
      "rtpContribution": "1.5986%"
    },
    {
      "tileId": 1,
      "length": 4,
      "hits": 478918,
      "hitRate": "4.7892%",
      "multiplier": 0.2,
      "rtpContribution": "0.9578%"
    },
    {
      "tileId": 1,
      "length": 5,
      "hits": 109366,
      "hitRate": "1.0937%",
      "multiplier": 0.4,
      "rtpContribution": "0.4375%"
    },
    {
      "tileId": 2,
      "length": 3,
      "hits": 1233402,
      "hitRate": "12.3340%",
      "multiplier": 0.1,
      "rtpContribution": "1.2334%"
    },
    {
      "tileId": 2,
      "length": 4,
      "hits": 356984,
      "hitRate": "3.5698%",
      "multiplier": 0.3,
      "rtpContribution": "1.0710%"
    },
    {
      "tileId": 2,
      "length": 5,
      "hits": 78364,
      "hitRate": "0.7836%",
      "multiplier": 0.6,
      "rtpContribution": "0.4702%"
    },
    {
      "tileId": 3,
      "length": 3,
      "hits": 1021919,
      "hitRate": "10.2192%",
      "multiplier": 0.2,
      "rtpContribution": "2.0438%"
    },
    {
      "tileId": 3,
      "length": 4,
      "hits": 286688,
      "hitRate": "2.8669%",
      "multiplier": 0.5,
      "rtpContribution": "1.4334%"
    },
    {
      "tileId": 3,
      "length": 5,
      "hits": 60382,
      "hitRate": "0.6038%",
      "multiplier": 1,
      "rtpContribution": "0.6038%"
    },
    {
      "tileId": 4,
      "length": 3,
      "hits": 791366,
      "hitRate": "7.9137%",
      "multiplier": 0.3,
      "rtpContribution": "2.3741%"
    },
    {
      "tileId": 4,
      "length": 4,
      "hits": 217167,
      "hitRate": "2.1717%",
      "multiplier": 0.6,
      "rtpContribution": "1.3030%"
    },
    {
      "tileId": 4,
      "length": 5,
      "hits": 42812,
      "hitRate": "0.4281%",
      "multiplier": 1.5,
      "rtpContribution": "0.6422%"
    },
    {
      "tileId": 5,
      "length": 3,
      "hits": 391615,
      "hitRate": "3.9162%",
      "multiplier": 0.5,
      "rtpContribution": "1.9581%"
    },
    {
      "tileId": 5,
      "length": 4,
      "hits": 102687,
      "hitRate": "1.0269%",
      "multiplier": 1,
      "rtpContribution": "1.0269%"
    },
    {
      "tileId": 5,
      "length": 5,
      "hits": 19488,
      "hitRate": "0.1949%",
      "multiplier": 2,
      "rtpContribution": "0.3898%"
    },
    {
      "tileId": 6,
      "length": 3,
      "hits": 292509,
      "hitRate": "2.9251%",
      "multiplier": 1,
      "rtpContribution": "2.9251%"
    },
    {
      "tileId": 6,
      "length": 4,
      "hits": 75262,
      "hitRate": "0.7526%",
      "multiplier": 1.5,
      "rtpContribution": "1.1289%"
    },
    {
      "tileId": 6,
      "length": 5,
      "hits": 13862,
      "hitRate": "0.1386%",
      "multiplier": 2.5,
      "rtpContribution": "0.3465%"
    },
    {
      "tileId": 7,
      "length": 3,
      "hits": 188386,
      "hitRate": "1.8839%",
      "multiplier": 1.5,
      "rtpContribution": "2.8258%"
    },
    {
      "tileId": 7,
      "length": 4,
      "hits": 47322,
      "hitRate": "0.4732%",
      "multiplier": 2.5,
      "rtpContribution": "1.1831%"
    },
    {
      "tileId": 7,
      "length": 5,
      "hits": 8777,
      "hitRate": "0.0878%",
      "multiplier": 5,
      "rtpContribution": "0.4389%"
    },
    {
      "tileId": 8,
      "length": 3,
      "hits": 83936,
      "hitRate": "0.8394%",
      "multiplier": 2.5,
      "rtpContribution": "2.0984%"
    },
    {
      "tileId": 8,
      "length": 4,
      "hits": 20317,
      "hitRate": "0.2032%",
      "multiplier": 5,
      "rtpContribution": "1.0159%"
    },
    {
      "tileId": 8,
      "length": 5,
      "hits": 3508,
      "hitRate": "0.0351%",
      "multiplier": 15,
      "rtpContribution": "0.5262%"
    }
  ]
}
```
