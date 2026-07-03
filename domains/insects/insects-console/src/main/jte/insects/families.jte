@import com.naturalist.insects.InsectEntityCollections
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.InsectOrderName
@import com.naturalist.insects.console.AncestorIntro
@import com.naturalist.insects.console.BreadcrumbSegment
@import com.naturalist.insects.console.CladeTrail
@import com.naturalist.data.Page
@import java.util.List
@import java.util.Map

@param Page<InsectFamily> familyPage
@param Map<InsectOrderName, InsectOrder> orderByName
@param InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.empty()
@param List<BreadcrumbSegment> breadcrumb = List.of()
@param CladeTrail cladeTrail = null
@param List<AncestorIntro> ancestorIntros = List.of()

@template.layout.page(title = "Insect Families", content = @`
    @template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
    @template.insects.ancestorIntros(intros = ancestorIntros)

    <h1>Insect Families</h1>
    <p>Family-rank records in the Oak Vista catalog.</p>

    <div class="entity-grid">
        @for(var f : familyPage.content())
            !{var order = orderByName.get(f.orderName());}
            !{var commonName = f.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(f.name().value());}
            <article>
                <header>
                    <a href="/insects/families/${f.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>order</dt>
                    <dd>${order.order().value()}</dd>
                    <dt>family</dt>
                    <dd>${f.family().value()}</dd>
                </dl>
                !{var images = gallery.forEntity(f.name()).stream().toList();}
                @if(!images.isEmpty())
                    @template.insects.cardImages(images = images, linkUrl = "/insects/families/" + f.name().value(), alt = commonName)
                @endif
            </article>
        @endfor
    </div>

    @template.components.pager(page = familyPage, baseUrl = "/insects/families")
`)
