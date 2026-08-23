package com.naturalist.insects.catalog;

import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectSpeciesName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsLinkerTest {

    // The linker only switches on ref.name(); the domain is irrelevant here.
    private record TestDomain() implements DomainId {
        @Override
        public String value() {
            return "insects";
        }
    }

    private final InsectsLinker linker = new InsectsLinker();

    private String link(EntityName name) {
        return linker.linkFor(new EntityRef(new TestDomain(), name));
    }

    @Test
    void linksOrderToOrderDetail() {
        assertThat(link(InsectOrderName.of("lepidoptera")))
                .isEqualTo("/insects/orders/lepidoptera");
    }

    @Test
    void linksFamilyToFamilyDetail() {
        assertThat(link(InsectFamilyName.of("papilionidae")))
                .isEqualTo("/insects/families/papilionidae");
    }

    @Test
    void linksGenusToGenusDetail() {
        assertThat(link(InsectGenusName.of("battus")))
                .isEqualTo("/insects/genera/battus");
    }

    @Test
    void linksSpeciesToSpeciesDetail() {
        assertThat(link(InsectSpeciesName.of("battus-philenor")))
                .isEqualTo("/insects/battus-philenor");
    }
}
