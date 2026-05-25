@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectOrder
@import java.util.List

@param InsectOrder order
@param List<InsectFamily> families
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = order.order().value(), content = @`
    @template.insects.nav(active = "order")
    <h1>${order.order().value()}</h1>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    <section>
        <h2>Families in this order</h2>
        @if(families.isEmpty())
            <p><em>No families catalogued under this order yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var f : families)
                    !{var commonName = f.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(f.name().value());}
                    <article>
                        <header>
                            <a href="/insects/families/${f.name().value()}">
                                <strong>${commonName}</strong>
                            </a>
                        </header>
                        <dl class="taxonomy">
                            <dt>family</dt>
                            <dd><em>${f.family().value()}</em></dd>
                        </dl>
                    </article>
                @endfor
            </div>
        @endif
    </section>
`)
