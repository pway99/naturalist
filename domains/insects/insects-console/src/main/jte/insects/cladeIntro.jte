@import com.naturalist.clades.CladeTraversal
@import com.naturalist.clades.Eukaryota
@import com.naturalist.clades.Insecta

@param String cladePreschool = ""
@param String cladeElementary = ""
@param String cladeSecondary = ""
@param String cladeUniversity = ""

!{var ancestry = CladeTraversal.ancestry(new Insecta()).reversed();}
!{var display = ancestry.stream().filter(c -> !(c instanceof Eukaryota)).toList();}

<div class="clade-lineage" aria-label="Taxonomic placement">
    @for(int i = 0; i < display.size(); i++)
        @if(i > 0)
            <span aria-hidden="true"> › </span>
        @endif
        <span>${display.get(i).displayName()}</span>
    @endfor
</div>
<details class="clade-introduction" id="clade-intro">
    <summary><h2>Class Insecta</h2></summary>
    @template.components.description(
        preschool = cladePreschool,
        elementary = cladeElementary,
        secondary = cladeSecondary,
        university = cladeUniversity)
</details>
<script>
    (function () {
        var el = document.getElementById('clade-intro');
        if (!el) return;
        var stored = localStorage.getItem('clade-intro-open');
        if (stored !== 'false') el.open = true;
        el.addEventListener('toggle', function () {
            localStorage.setItem('clade-intro-open', el.open);
        });
    }());
</script>
