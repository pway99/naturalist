package com.naturalist.chemistry.reaction;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class ReactionProfileTestEntitySource extends TestEntitySource<ReactionName, ReactionProfile> {
    public ReactionProfileTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("chemistry/reaction/reactionProfiles.json");
    }
}
