@import com.naturalist.console.admin.AdminCatalogController.DomainGroup
@import java.util.List

@param List<DomainGroup> groups

@template.layout.page(title = "Catalog", content = @`
    @template.admin.nav(current = "catalog")
    <section class="admin-catalog">
        <h1>Catalog</h1>
        <p>Domains, <code>CatalogContribution</code>s, and
           <code>EntityReferences</code> providers the assembled
           <code>Catalog</code> bean was built from. A heading present here
           is one the composition root registered; absence on a domain you
           expected is a wiring defect.</p>

        @if(groups.isEmpty())
            <p><em>No domains registered.</em></p>
        @else
            @for(var group : groups)
                <section class="admin-catalog-group">
                    <h2>${group.slug()}</h2>

                    <h3>Contributions</h3>
                    @if(group.contributions().isEmpty())
                        <p><em>No contributions registered.</em></p>
                    @else
                        <ul>
                            @for(var bean : group.contributions())
                                <li>
                                    <code>${bean.simpleName()}</code>
                                    <small>${bean.fullyQualifiedName()}</small>
                                </li>
                            @endfor
                        </ul>
                    @endif

                    <h3>Reference Providers</h3>
                    @if(group.providers().isEmpty())
                        <p><em>No reference providers registered.</em></p>
                    @else
                        <ul>
                            @for(var provider : group.providers())
                                <li>
                                    <code>${provider.simpleName()}</code>
                                    &rarr;
                                    <code>${provider.referenceTypeSimpleName()}</code>
                                    <small>${provider.fullyQualifiedName()}</small>
                                </li>
                            @endfor
                        </ul>
                    @endif
                </section>
            @endfor
        @endif
    </section>
`)
