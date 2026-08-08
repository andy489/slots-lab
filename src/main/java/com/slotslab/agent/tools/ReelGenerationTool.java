package com.slotslab.agent.tools;

import com.slotslab.agent.model.GeneratedReels;
import com.slotslab.reel.ReelSetsCollectionData;

public interface ReelGenerationTool {

    GeneratedReels generate(ReelSetsCollectionData config);
}
