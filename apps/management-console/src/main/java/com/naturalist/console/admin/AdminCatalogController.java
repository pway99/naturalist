package com.naturalist.console.admin;

import com.naturalist.catalog.CatalogContribution;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityReferences;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

/**
 * Renders {@code /admin/catalog} — the catalog-assembly diagnostic surface
 * described in {@code docs/plans/admin-console.md} (view 3, M1).
 *
 * <p>The contributions, providers, and domain IDs the assembled
 * {@link com.naturalist.catalog.Catalog} consumes are already Spring beans
 * fed into the {@code Catalog} {@code @Bean} by
 * {@code CatalogConfiguration}. This controller injects the same lists
 * directly — the same authoritative source the assembly itself reads —
 * rather than introducing introspection on the {@code Catalog} interface
 * that every future adapter (Solr-backed, persistent) would have to
 * implement for one caller's benefit.
 *
 * <p>No live calls into {@link CatalogContribution#searchableEntities()}
 * or {@link EntityReferences#referencesTo} are made here: a diagnostic
 * page that triggers a fan-out on every render is the wrong shape.
 * "Did my contribution get picked up?" is answered by presence in the
 * list; counts and live invocations are future enhancements.
 */
@Controller
public class AdminCatalogController {

    private final List<DomainId> domains;
    private final List<CatalogContribution> contributions;
    private final List<EntityReferences<?>> providers;

    AdminCatalogController(List<DomainId> domains,
                           List<CatalogContribution> contributions,
                           List<EntityReferences<?>> providers) {
        this.domains = domains;
        this.contributions = contributions;
        this.providers = providers;
    }

    @GetMapping("/admin/catalog")
    String catalog(Model model) {
        Map<String, DomainGroup> bySlug = new TreeMap<>();
        for (DomainId domain : domains) {
            groupFor(bySlug, domain);
        }
        for (CatalogContribution contribution : contributions) {
            groupFor(bySlug, contribution.domain())
                    .contributions.add(Bean.from(contribution.getClass()));
        }
        for (EntityReferences<?> provider : providers) {
            groupFor(bySlug, provider.domain())
                    .providers.add(Provider.from(provider.getClass(), provider.referenceType()));
        }

        List<DomainGroup> groups = new ArrayList<>(bySlug.values());
        for (DomainGroup group : groups) {
            group.contributions.sort(Comparator.comparing(Bean::fullyQualifiedName));
            group.providers.sort(Comparator
                    .comparing(Provider::referenceTypeSimpleName)
                    .thenComparing(Provider::fullyQualifiedName));
        }
        model.addAttribute("groups", groups);
        return "admin/catalog";
    }

    private static DomainGroup groupFor(Map<String, DomainGroup> bySlug, DomainId domain) {
        return bySlug.computeIfAbsent(domain.value(), DomainGroup::empty);
    }

    public static final class DomainGroup {

        private final String slug;
        private final List<Bean> contributions = new ArrayList<>();
        private final List<Provider> providers = new ArrayList<>();

        private DomainGroup(String slug) {
            this.slug = slug;
        }

        static DomainGroup empty(String slug) {
            return new DomainGroup(slug);
        }

        public String slug() {
            return slug;
        }

        public List<Bean> contributions() {
            return contributions;
        }

        public List<Provider> providers() {
            return providers;
        }
    }

    public record Bean(String simpleName, String fullyQualifiedName) {

        static Bean from(Class<?> type) {
            return new Bean(type.getSimpleName(), type.getName());
        }
    }

    public record Provider(String simpleName,
                           String fullyQualifiedName,
                           String referenceTypeSimpleName,
                           String referenceTypeFullyQualifiedName) {

        static Provider from(Class<?> type, Class<?> referenceType) {
            return new Provider(
                    type.getSimpleName(),
                    type.getName(),
                    referenceType.getSimpleName(),
                    referenceType.getName());
        }
    }
}
