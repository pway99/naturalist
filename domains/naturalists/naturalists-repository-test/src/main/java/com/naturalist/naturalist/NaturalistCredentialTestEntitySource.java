package com.naturalist.naturalist;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class NaturalistCredentialTestEntitySource
        extends TestEntitySource<NaturalistName, NaturalistCredential> {

    public NaturalistCredentialTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("naturalists/naturalist-credentials.json");
    }
}
