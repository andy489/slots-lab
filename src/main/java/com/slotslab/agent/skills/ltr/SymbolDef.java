package com.slotslab.agent.skills.ltr;

public record SymbolDef(int symbolId, String tier, String hint) {

    public boolean isJunior()    { return "junior".equals(tier); }
    public boolean isSenior()    { return "senior".equals(tier); }
    public boolean isWild()      { return "wild".equals(tier); }
    public boolean isScatter()   { return "scatter".equals(tier); }
    public boolean isMultiWild() { return "multiwild".equals(tier); }
    public boolean isSpecial()   { return isWild() || isScatter() || isMultiWild(); }
}
