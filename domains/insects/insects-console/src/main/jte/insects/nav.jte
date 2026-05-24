@import com.naturalist.clades.Clade
@import com.naturalist.clades.CladeTraversal
@import com.naturalist.clades.Eukaryota
@import com.naturalist.clades.Insecta
@import java.util.List

@param String active = ""

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
<nav class="insects-subnav" aria-label="Insects catalog sub-navigation">
    <a href="/insects/families" aria-current="${"family".equals(active) ? "page" : "false"}">Family</a>
    <a href="/insects/genera" aria-current="${"genus".equals(active) ? "page" : "false"}">Genus</a>
    <a href="/insects" aria-current="${"species".equals(active) ? "page" : "false"}">Species</a>
</nav>
