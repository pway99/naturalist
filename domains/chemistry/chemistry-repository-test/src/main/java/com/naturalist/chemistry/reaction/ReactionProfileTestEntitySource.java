package com.naturalist.chemistry.reaction;

import com.naturalist.data.TestEntitySource;
public class ReactionProfileTestEntitySource extends TestEntitySource<ReactionId, ReactionName, ReactionProfile> {
    public ReactionProfileTestEntitySource() {
        super(ReactionId::of);
        loadFile("chemistry/reaction/reactionProfiles.json");
    }
}
