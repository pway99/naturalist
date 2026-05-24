@param String active = ""

<nav class="insects-subnav" aria-label="Insects catalog sub-navigation">
    <a href="/insects/families" aria-current="${"family".equals(active) ? "page" : "false"}">Family</a>
    <a href="/insects/genera" aria-current="${"genus".equals(active) ? "page" : "false"}">Genus</a>
    <a href="/insects" aria-current="${"species".equals(active) ? "page" : "false"}">Species</a>
</nav>
