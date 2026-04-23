package com.naturalist.chemistry.reaction;

import com.naturalist.data.TestEntitySource;

public class ReactionProfileTestEntitySource extends TestEntitySource<ReactionName, ReactionProfile> {
    public ReactionProfileTestEntitySource() {
        loadFile("chemistry/reaction/reactionProfiles.json");
    }
}
