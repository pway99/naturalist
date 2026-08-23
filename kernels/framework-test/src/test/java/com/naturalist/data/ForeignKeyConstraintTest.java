package com.naturalist.data;

import com.naturalist.ddd.Named;
import com.naturalist.exception.ForeignKeyConstraintException;
import com.naturalist.observability.Constraints;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the FK enforcement path on {@link TestEntitySource#preSaveChecks}.
 * Two minimal fake entities — {@link Parent} and {@link Child} — exercise the
 * pass / miss / null-skip / update paths without pulling in any domain.
 */
class ForeignKeyConstraintTest {

    @RegisterExtension
    final NaturalistTestExtension db = NaturalistTestExtension.create();

    @Test
    void insertWithResolvedForeignKeyPasses() {
        ParentSource parents = db.getNamed(ParentSource.class);
        ChildSource children = db.getNamed(ChildSource.class);

        parents.insert(new Parent("alpha"));
        children.insert(new Child("a1", "alpha"));

        assertThat(children.getByName("a1")).isPresent();
    }

    @Test
    void insertWithUnresolvedForeignKeyThrows() {
        // intentionally do not seed the parent — only get the child source
        ChildSource children = db.getNamed(ChildSource.class);

        Child orphan = new Child("a1", "missing");

        assertThatThrownBy(() -> children.insert(orphan))
                .isInstanceOf(ForeignKeyConstraintException.class)
                .hasMessageContaining("parentName")
                .hasMessageContaining("missing");
    }

    @Test
    void nullForeignKeyValueIsSkipped() {
        ChildSource children = db.getNamed(ChildSource.class);

        // null FK is a pass at the FK layer — record-invariant layer is the
        // place to forbid null when the FK is required.
        children.insert(new Child("a1", null));

        assertThat(children.getByName("a1")).isPresent();
    }

    @Test
    void updateWithUnresolvedForeignKeyThrows() {
        ParentSource parents = db.getNamed(ParentSource.class);
        ChildSource children = db.getNamed(ChildSource.class);

        parents.insert(new Parent("alpha"));
        children.insert(new Child("a1", "alpha"));

        Child rebound = new Child("a1", "missing");

        assertThatThrownBy(() -> children.update(rebound))
                .isInstanceOf(ForeignKeyConstraintException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void lazySourceLoadingResolvesForeignKey() {
        // Reach for the child source first; the FK check lazily forces the
        // parent source to be constructed via NaturalistDatabase.getNamed —
        // the wiring an rdbms-shaped catalog relies on at startup.
        ChildSource children = db.getNamed(ChildSource.class);
        ParentSource parents = db.getNamed(ParentSource.class);
        parents.insert(new Parent("alpha"));

        children.insert(new Child("a1", "alpha"));

        assertThat(children.getByName("a1")).isPresent();
    }

    // ── fakes ────────────────────────────────────────────────────────────

    record Parent(String name) implements Named<String> {
        @Override
        public String key() { return name; }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.notBlank(name, "name");
        }
    }

    record Child(String name, String parentName) implements Named<String> {
        @Override
        public String key() { return name; }

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i.notBlank(name, "name");
        }
    }

    public static class ParentSource extends TestEntitySource<String, Parent> {
        public ParentSource(NaturalistDatabase database) {
            super(database);
        }
    }

    public static class ChildSource extends TestEntitySource<String, Child> {
        public ChildSource(NaturalistDatabase database) {
            super(database);
        }

        @Override
        protected List<ForeignKeyConstraint<Child, ?>> foreignKeyConstraints() {
            return List.of(ForeignKeyConstraint.of(
                    "parentName",
                    Child::parentName,
                    ParentSource.class));
        }
    }
}
