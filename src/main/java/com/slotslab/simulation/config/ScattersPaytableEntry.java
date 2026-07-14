package com.slotslab.simulation.config;

/**
 * One interval in the SCATTERS contacts paytable.
 * A contact with total tile count in [{@code from}, {@code to}] falls into this interval.
 * The payout multiplier is defined per-symbol in {@code SymbolConfig#paytable},
 * indexed by the position of this entry in the paytable list.
 */
public record ScattersPaytableEntry(int from, int to) {}
