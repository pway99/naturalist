package com.naturalist.test.query.nofanout;

import java.util.List;
import java.util.Set;

class FooRepositoryMock implements FooRepository {
    @Override public String getByName(String name) { return name.toUpperCase(); }
    @Override public List<String> getByEntityNameSet(Set<String> names) { return names.stream().sorted().toList(); }
}
