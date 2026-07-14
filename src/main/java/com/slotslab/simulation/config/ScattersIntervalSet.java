package com.slotslab.simulation.config;

import java.util.List;

/**
 * A named group of count-range intervals used by the SCATTERS strategy.
 * Each NORMAL symbol references a set by name; the symbol's paytable provides
 * one multiplier per interval in the set (indexed by position).
 */
public record ScattersIntervalSet(String name, List<ScattersPaytableEntry> intervals) {}
