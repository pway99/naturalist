@import com.naturalist.chemistry.element.Element
@import com.naturalist.data.Page

@param Page<Element> elementsPage

@template.layout.page(title = "Elements", content = @`
    <a href="/chemistry" class="naturalist-backlink">&larr; Back to compounds</a>

    <h1>Elements</h1>
    <p>The elements the catalog knows, with the ionic form each takes in soil solution.</p>

    <table>
        <thead>
            <tr><th>Element</th><th>Symbol</th><th>Atomic weight</th><th>Ionic form</th><th>Charge</th></tr>
        </thead>
        <tbody>
            @for(var e : elementsPage.content())
                <tr>
                    <td><a href="/chemistry/elements/${e.name().value()}">${e.name().value()}</a></td>
                    <td><code>${e.symbol()}</code></td>
                    <td>${e.atomicWeight().value().toString()} g/mol</td>
                    <td><code>${e.ionicForm()}</code></td>
                    @if(e.isCation())
                        <td>+${e.ionicCharge()} &middot; cation</td>
                    @elseif(e.isAnion())
                        <td>${e.ionicCharge()} &middot; anion</td>
                    @else
                        <td class="not-reported">uncharged</td>
                    @endif
                </tr>
            @endfor
        </tbody>
    </table>

    @template.components.pager(page = elementsPage, baseUrl = "/chemistry/elements")
`)
