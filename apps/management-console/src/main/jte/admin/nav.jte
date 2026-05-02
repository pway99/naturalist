@param String current

<nav class="admin-nav" aria-label="Admin views">
    @if(current.equals("resilience"))
        <a href="/admin/resilience" aria-current="page">Resilience</a>
    @else
        <a href="/admin/resilience">Resilience</a>
    @endif
    @if(current.equals("domain-services"))
        <a href="/admin/domain-services" aria-current="page">Domain Services</a>
    @else
        <a href="/admin/domain-services">Domain Services</a>
    @endif
</nav>
