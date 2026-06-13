package com.naturalist.authority.eol;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class EolAuthorityTestEntitySource extends TestEntitySource<String, EolAuthorityEntry> {

    public EolAuthorityTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("eol/eol-authority-fixtures.json");
    }
}