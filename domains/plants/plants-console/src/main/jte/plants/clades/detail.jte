@import com.naturalist.clades.Clade
@import com.naturalist.plants.PlantOrder
@import com.naturalist.plants.console.BreadcrumbSegment
@import java.util.List

@param Clade clade
@param List<PlantOrder> orders = java.util.List.of()
@param List<Clade> childClades = java.util.List.of()
@param List<BreadcrumbSegment> breadcrumb = java.util.List.of()
@param List<Clade> cladeTrail = java.util.List.of()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = clade.displayName(), content = @`
    @template.plants.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail, current = clade)
    <h1>${clade.displayName()}</h1>
    <p><em>A clade — a branch of the tree of life, not a Linnaean rank.</em></p>

    @template.components.description(
    preschool = descriptionPreschool,
    elementary = descriptionElementary,
    secondary = descriptionSecondary,
    university = descriptionUniversity)

    @if(!orders.isEmpty())
        <section>
            <h2>Orders placed in this clade</h2>
            <div class="entity-grid">
                @for(var o : orders)
                    !{var commonName = o.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(o.order().value());}
                    <article>
                        <header>
                            <a href="/plants/orders/${o.name().value()}">
                                <strong>${commonName}</strong>
                            </a>
                        </header>
                        <dl class="taxonomy">
                            <dt>order</dt>
                            <dd>${o.order().value()}</dd>
                        </dl>
                        <p class="description-preview">${o.description().preschool()}</p>
                    </article>
                @endfor
            </div>
        </section>
    @endif

    @if(!childClades.isEmpty())
        <section>
            <h2>Clades within ${clade.displayName()}</h2>
            <div class="entity-grid">
                @for(var child : childClades)
                    <article>
                        <header>
                            <a href="/plants/clades/${child.slug()}">
                                <strong>${child.displayName()}</strong>
                            </a>
                        </header>
                        <p class="description-preview">${child.description().preschool()}</p>
                    </article>
                @endfor
            </div>
        </section>
    @endif

    @if(orders.isEmpty() && childClades.isEmpty())
        <p><em>No orders or narrower clades sit under this clade yet.</em></p>
    @endif
`)
