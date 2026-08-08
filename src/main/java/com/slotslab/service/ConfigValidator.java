package com.slotslab.service;

import com.slotslab.reel.ReelSet;
import com.slotslab.reel.ReelSetsCollectionData;
import com.slotslab.reel.Restriction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConfigValidator {

    public void validate(ReelSetsCollectionData config) {
        if (config.strategy() == null)
            throw new IllegalArgumentException("strategy is required (shuffle or flat)");

        if (config.reelSets() == null || config.reelSets().isEmpty())
            throw new IllegalArgumentException("At least one reelSet is required");

        List<ReelSet> sets = config.reelSets();
        for (int si = 0; si < sets.size(); si++) {
            ReelSet set = sets.get(si);
            validateReelSet(si, set);
        }
    }

    private void validateReelSet(int si, ReelSet set) {
        String prefix = "Reel Set #" + si + ": ";

        if (set.tilesCounts() == null || set.tilesCounts().isEmpty())
            throw new IllegalArgumentException(prefix + "tilesCounts cannot be empty");

        for (int ri = 0; ri < set.tilesCounts().size(); ri++) {
            List<Integer> row = set.tilesCounts().get(ri);
            if (row == null || row.isEmpty())
                throw new IllegalArgumentException(prefix + "reel R" + (ri + 1) + " tile counts cannot be empty");
            for (Integer v : row) {
                if (v == null || v < 0)
                    throw new IllegalArgumentException(prefix + "reel R" + (ri + 1) + " counts must be >= 0");
            }
        }

        if (set.restrictions() != null && !set.restrictions().isEmpty()) {
            int reelCount = set.tilesCounts().size();
            if (set.restrictions().size() > reelCount)
                throw new IllegalArgumentException(prefix + "restrictions count (" + set.restrictions().size()
                        + ") exceeds reel count (" + reelCount + ")");

            for (int ri = 0; ri < set.restrictions().size(); ri++) {
                validateRestriction(prefix, ri, set.restrictions().get(ri));
            }
        }
    }

    private void validateRestriction(String prefix, int ri, Restriction r) {
        String rp = prefix + "restriction #" + (ri + 1) + ": ";

        if (r.stacks() == null || r.stacks().isEmpty())
            throw new IllegalArgumentException(rp + "stackSizes cannot be empty");
        for (Integer v : r.stacks()) {
            if (v == null || v <= 0)
                throw new IllegalArgumentException(rp + "stackSizes must be positive integers");
        }

        if (r.chances() == null || r.chances().isEmpty())
            throw new IllegalArgumentException(rp + "stackChances cannot be empty");
        for (Double v : r.chances()) {
            if (v == null || v < 0)
                throw new IllegalArgumentException(rp + "stackChances must be >= 0");
        }

        if (r.stacks().size() != r.chances().size())
            throw new IllegalArgumentException(rp + "stackSizes (" + r.stacks().size()
                    + ") and stackChances (" + r.chances().size() + ") must have equal length");

        if (r.distance() < 0)
            throw new IllegalArgumentException(rp + "minDistance must be >= 0");
    }
}
