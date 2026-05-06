package com.naturalist.insects.lifestage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.insects.LifeStageName;

public class LifeStageTestEntitySource extends TestEntitySource<LifeStageName, LifeStage> {

    public LifeStageTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/life-stages.json");
    }
}
