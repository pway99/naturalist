package com.naturalist.insects.lifestage;

import com.naturalist.data.TestEntitySource;

public class LifeStageTestEntitySource extends TestEntitySource<LifeStageName, LifeStage> {

    public LifeStageTestEntitySource() {
        loadFile("insects/life-stages.json");
    }
}
