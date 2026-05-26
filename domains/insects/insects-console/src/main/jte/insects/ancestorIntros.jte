@import com.naturalist.insects.console.AncestorIntro
@import java.util.List

@param List<AncestorIntro> intros

@if(!intros.isEmpty())
    <div class="ancestor-intros">
        @for(var intro : intros)
            @template.insects.ancestorIntro(intro = intro)
        @endfor
    </div>
    <script>
        (function () {
            // Per-rank-type localStorage keys (data-storage-key) — opening
            // "Order" persists across all pages, independent of "Family",
            // so the user's signal "I want rank-context for X" travels.
            document.querySelectorAll('.ancestor-intro').forEach(function (el) {
                var key = el.getAttribute('data-storage-key');
                if (!key) return;
                if (localStorage.getItem(key) === 'true') el.open = true;
                el.addEventListener('toggle', function () {
                    localStorage.setItem(key, el.open);
                });
            });
        }());
    </script>
@endif
