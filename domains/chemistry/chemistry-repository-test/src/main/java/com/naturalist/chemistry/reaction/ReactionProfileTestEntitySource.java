package com.naturalist.chemistry.reaction;

import com.naturalist.data.NamedTestEntitySource;

public class ReactionProfileTestEntitySource extends NamedTestEntitySource<ReactionName, ReactionProfile> {
    public ReactionProfileTestEntitySource() {
        loadFile("chemistry/reaction/reactionProfiles.json");
    }
}
