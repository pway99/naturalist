@import com.naturalist.library.GlossaryTerm
@import java.util.List
@param List<GlossaryTerm> terms

@template.layout.page(title = "Glossary", content = @`
    <h1>Glossary</h1>
    <p>The naturalist's vernacular — the words used across the field guide, defined in plain terms.</p>

    <input type="search" id="glossary-filter" class="glossary-filter"
           placeholder="Filter terms…" aria-label="Filter glossary terms" autocomplete="off">

    <dl class="glossary-list">
        @for(var term : terms)
            <div class="glossary-entry"
                 data-haystack="${term.term().toLowerCase()} ${term.definition().toLowerCase()}">
                <dt><a href="/glossary/${term.name().value()}">${term.term()}</a></dt>
                <dd>${term.definition()}</dd>
            </div>
        @endfor
    </dl>
    <p class="glossary-empty" id="glossary-empty" hidden>No terms match your filter.</p>

    <script>
      (function () {
        var input = document.getElementById('glossary-filter');
        var entries = Array.prototype.slice.call(document.querySelectorAll('.glossary-entry'));
        var empty = document.getElementById('glossary-empty');
        if (!input) return;
        input.addEventListener('input', function () {
          var q = input.value.trim().toLowerCase();
          var anyVisible = false;
          entries.forEach(function (el) {
            var match = q === '' || el.getAttribute('data-haystack').indexOf(q) !== -1;
            el.hidden = !match;
            if (match) anyVisible = true;
          });
          empty.hidden = anyVisible;
        });
      })();
    </script>
`)
