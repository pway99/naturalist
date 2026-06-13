package com.naturalist.authority;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.naturalist.ddd.NamedEntity;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OnlineSource.class, name = "ONLINE_SOURCE")
})
public sealed interface Citation extends NamedEntity<CitationName>
        permits OnlineSource {

    AuthorityReference authorityReference();

    String title();
}
