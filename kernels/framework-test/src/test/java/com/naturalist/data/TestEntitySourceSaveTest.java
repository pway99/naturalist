package com.naturalist.data;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Constraints;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kernel-level coverage of the one edge in {@link TestEntitySource#save} that no
 * real consumer exercises intentionally: a source that declares a unique
 * constraint (so the constraint-match branch of {@code save} can fire) but has
 * not overridden {@link TestEntitySource#withKey}. Every production
 * {@code TestEntitySource} that reaches this branch today (e.g.
 * {@code CitationAssociationTestEntitySource}, {@code InsectFeatureTestEntitySource})
 * overrides {@code withKey}, so nothing in the consumer test suites would ever
 * catch a regression to the un-overridden default silently misbehaving instead
 * of failing loudly.
 */
class TestEntitySourceSaveTest {

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    record Item(String name, String value) implements Named<String> {
        @Override
        public String key() {
            return name;
        }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.notBlank(name, "name").notBlank(value, "value");
        }
    }

    /**
     * Declares a "value" unique constraint (so a constraint match is reachable)
     * but deliberately does not override {@code withKey} — the case under test.
     */
    static class ItemSourceWithoutWithKey extends TestEntitySource<String, Item> {
        ItemSourceWithoutWithKey(NaturalistDatabase database) {
            super(database);
        }

        @Override
        protected List<UniqueConstraint<Item>> uniqueConstraints() {
            return List.of(new UniqueConstraint<>() {
                @Override
                public String name() {
                    return "value";
                }

                @Override
                public Function<Item, ?> valueFunction() {
                    return Item::value;
                }
            });
        }
    }

    @Test
    void save_uniqueConstraintMatchWithoutWithKeyOverride_throwsUnsupportedOperationException() {
        ItemSourceWithoutWithKey source =
                nte.getNamed(ItemSourceWithoutWithKey.class);
        source.insert(new Item("item-a", "shared-value"));

        assertThatThrownBy(() -> source.save(new Item("item-b", "shared-value")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("withKey");
    }
}
