package org.evo.reels.rtp;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable lookup table for symbol configurations. Built once per simulation run.
 */
public class SymbolTable {

    private final Map<Integer, SymbolConfig> byId;
    private final int wildId;   // -1 if no wild defined

    public SymbolTable(List<SymbolConfig> configs) {
        byId = new HashMap<>(configs.size() * 2);
        int foundWild = -1;
        for (SymbolConfig cfg : configs) {
            byId.put(cfg.symbolId(), cfg);
            if (cfg.type() == SymbolType.WILD) foundWild = cfg.symbolId();
        }
        wildId = foundWild;
    }

    public SymbolConfig get(int id)  { return byId.get(id); }
    public boolean isWild(int id)    { return id == wildId; }
    public boolean isScatter(int id) {
        SymbolConfig c = byId.get(id);
        return c != null && c.type() == SymbolType.SCATTER;
    }
    public int wildId() { return wildId; }
    public Collection<SymbolConfig> all() { return byId.values(); }
}
