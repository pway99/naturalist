@import com.naturalist.clades.Clade
@import com.naturalist.insects.console.CladeTaxonCard
@import com.naturalist.insects.console.CladeNav
@import com.naturalist.insects.console.BreadcrumbSegment
@import com.naturalist.insects.console.CladeTrail
@import java.util.List

@param Clade clade
@param List<CladeTaxonCard> orders = java.util.List.of()
@param List<CladeTaxonCard> families = java.util.List.of()
@param List<CladeTaxonCard> genera = java.util.List.of()
@param List<CladeTaxonCard> species = java.util.List.of()
@param List<Clade> childClades = java.util.List.of()
@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param CladeTrail cladeTrail = null
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = clade.displayName(), content = @`
    @template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
    <h1>${clade.displayName()}</h1>
    <p><em>A clade — a branch of the tree of life, not a Linnaean rank.</em></p>

    @template.components.description(
    preschool = descriptionPreschool,
    elementary = descriptionElementary,
    secondary = descriptionSecondary,
    university = descriptionUniversity)

    <%-- Insects place clades at every rank, so the taxa here span Order → Species. --%>
    @template.insects.cladeTaxonGroup(heading = "Orders in this clade", taxa = orders)
    @template.insects.cladeTaxonGroup(heading = "Families in this clade", taxa = families)
    @template.insects.cladeTaxonGroup(heading = "Genera in this clade", taxa = genera)
    @template.insects.cladeTaxonGroup(heading = "Species in this clade", taxa = species)

    @if(!childClades.isEmpty())
        <section>
            <h2>Clades within ${clade.displayName()}</h2>
            <div class="entity-grid">
                @for(var child : childClades)
                    <article>
                        <header>
                            <a href="${CladeNav.pageUrl(child)}"><strong>${child.displayName()}</strong></a>
                        </header>
                        <p class="description-preview">${child.description().preschool()}</p>
                    </article>
                @endfor
            </div>
        </section>
    @endif

    @if(orders.isEmpty() && families.isEmpty() && genera.isEmpty() && species.isEmpty() && childClades.isEmpty())
        <p><em>No insects or narrower clades sit under this clade yet.</em></p>
    @endif
`)
