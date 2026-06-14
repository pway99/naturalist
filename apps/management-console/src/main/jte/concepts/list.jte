@import com.naturalist.library.Concept
@import java.util.List
@param List<Concept> concepts

@template.layout.page(title = "Concepts", content = @`
    <h1>Concepts</h1>
    <p>Teaching notes on how the catalog of life is organised.</p>
    <ul class="concept-list">
        @for(var concept : concepts)
            <li><a href="/concepts/${concept.name().value()}">${concept.title()}</a></li>
        @endfor
    </ul>
    <p><a href="/clades">Browse the tree of life →</a></p>
`)
