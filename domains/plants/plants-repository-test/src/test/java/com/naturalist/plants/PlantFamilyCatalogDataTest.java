package com.naturalist.plants;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data assertions over the order and family catalogs — the top two rungs.
 * <p>
 * The FK constraint on {@link PlantFamilyTestEntitySource} already refuses to load a
 * family whose order is missing. These assertions stay because they name the rule and
 * fail with a message listing every gap, where the constraint failure names only the
 * first one it hits.
 */
class PlantFamilyCatalogDataTest {

    @RegisterExtension
    private final NaturalistTestExtension db = NaturalistTestExtension.create();

    private List<PlantOrder> orders() {
        return db.getNamed(PlantOrderTestEntitySource.class).entityStream().toList();
    }

    private List<PlantFamily> families() {
        return db.getNamed(PlantFamilyTestEntitySource.class).entityStream().toList();
    }

    @Test
    void everyFamilyResolvesToACataloguedOrder() {
        Set<String> orderSlugs = orders().stream()
                .map(o -> o.name().value())
                .collect(Collectors.toSet());

        assertThat(families())
                .allSatisfy(f -> assertThat(orderSlugs)
                        .as("family '%s' references order '%s'",
                                f.name().value(), f.orderName().value())
                        .contains(f.orderName().value()));
    }

    @Test
    void everyOrderNameIsAValidSlug() {
        assertThat(orders())
                .allSatisfy(o -> assertThat(o.name().isValid())
                        .as("order slug '%s' must be lower-kebab-case", o.name().value())
                        .isTrue());
    }

    @Test
    void orderSlugIsTheLowercasedOrderEpithet() {
        assertThat(orders())
                .allSatisfy(o -> assertThat(o.name().value())
                        .as("order record '%s' should be slugged from its epithet '%s'",
                                o.name().value(), o.order().value())
                        .isEqualTo(o.order().value().toLowerCase(Locale.ROOT)));
    }

    @Test
    void everyCataloguedOrderHasAtLeastOneFamily() {
        // An order with no families is either a stale record or a family the catalog
        // has not authored yet. Both are data gaps worth naming.
        Set<String> referenced = families().stream()
                .map(f -> f.orderName().value())
                .collect(Collectors.toSet());

        assertThat(orders())
                .allSatisfy(o -> assertThat(referenced)
                        .as("order '%s' has no families in the catalog", o.name().value())
                        .contains(o.name().value()));
    }
}
