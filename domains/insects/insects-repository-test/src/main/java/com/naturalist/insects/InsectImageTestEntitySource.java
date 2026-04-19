package com.naturalist.insects;

import com.naturalist.data.NamedTestEntitySource;

public class InsectImageTestEntitySource extends NamedTestEntitySource<InsectImageName, InsectImage> {

    public InsectImageTestEntitySource() {
        loadFile("insects/insect-images.json");
    }
}
