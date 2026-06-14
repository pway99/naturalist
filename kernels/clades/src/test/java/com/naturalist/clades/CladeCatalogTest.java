package com.naturalist.clades;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CladeCatalogTest {

    @Test
    void allCoversEveryPermit() {
        assertThat(CladeCatalog.all())
                .hasSize(Clade.class.getPermittedSubclasses().length);
    }

    @Test
    void onlyEukaryotaHasNoParent() {
        assertThat(CladeCatalog.all().stream().filter(c -> c.parent().isEmpty()).toList())
                .containsExactly(new Eukaryota());
    }

    @Test
    void childrenOfInsectaIncludesItsDirectDescendants() {
        assertThat(CladeCatalog.childrenOf(new Insecta()))
                .contains(new Holometabola(), new Hemiptera(), new Blattodea());
    }

    @Test
    void childrenOfALeafIsEmpty() {
        assertThat(CladeCatalog.childrenOf(new Troidini())).isEmpty();
    }

    @Test
    void childrenAreSortedByDisplayName() {
        var children = CladeCatalog.childrenOf(new Insecta());
        var sorted = children.stream().map(Clade::displayName).sorted().toList();
        assertThat(children.stream().map(Clade::displayName).toList()).isEqualTo(sorted);
    }
}
