@import com.naturalist.console.admin.AdminDomainServicesController.Domain
@import java.util.List

@param List<Domain> domains

@template.layout.page(title = "Domain Services", content = @`
    @template.admin.nav(current = "domain-services")
    <section class="admin-domain-services">
        <h1>Domain Services</h1>
        <p>Classes carrying the kernel <code>&#64;DomainService</code> marker that
            the Spring-runtime adapter registered as beans, grouped by domain
            package. A class listed here is one the composition root has
            wired — absence here on a class you expected is a discovery defect.</p>

        @if(domains.isEmpty())
            <p><em>No domain services registered.</em></p>
        @else
            @for(var domain : domains)
                <section class="admin-domain-services-group">
                    <h2>${domain.name()}</h2>
                    <ul>
                        @for(var bean : domain.beans())
                            <li>
                                <code>${bean.simpleName()}</code>
                                <small>${bean.fullyQualifiedName()}</small>
                            </li>
                        @endfor
                    </ul>
                </section>
            @endfor
        @endif
    </section>
`)
