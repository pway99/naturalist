@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.InsectOrderName
@import com.naturalist.data.Page
@import java.util.Map

@param Page<InsectFamily> familyPage
@param Map<InsectOrderName, InsectOrder> orderByName

@template.layout.page(title = "Insect Families", content = @`
    @template.insects.nav(active = "family")
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
                    <dd><em>${f.family().value()}</em></dd>
                </dl>
            </article>
        @endfor
    </div>

    @template.components.pager(page = familyPage, baseUrl = "/insects/families")
`)
