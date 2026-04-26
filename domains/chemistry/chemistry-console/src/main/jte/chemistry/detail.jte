@import com.naturalist.chemistry.compound.Compound
@import com.naturalist.chemistry.product.Product
@import java.util.List

@param Compound compound
@param boolean hasDepiction = false
@param String depictionNote = null
@param List<Product> products = java.util.List.of()

@template.layout.page(title = compound.name().value(), content = @`
    <a href="/chemistry" class="naturalist-backlink">&larr; Back to catalog</a>

    <h1>${compound.commonName()}</h1>
    <p>${compound.name().value()} &middot; <em>${compound.formula()}</em></p>

    @if(hasDepiction)
        <figure class="compound-depiction">
            <img src="/chemistry/${compound.name().value()}/depiction.svg"
                 alt="2D structural depiction of ${compound.commonName()}" />
        </figure>
        @if(depictionNote != null)
            <section>
                <h2>Depiction note</h2>
                <p>${depictionNote}</p>
            </section>
        @endif
    @endif

    <section>
        <h2>Identity</h2>
        <ul>
            <li><strong>Slug:</strong> <code>${compound.name().value()}</code></li>
            <li><strong>Common name:</strong> ${compound.commonName()}</li>
            <li><strong>Formula:</strong> <code>${compound.formula()}</code></li>
            @if(compound.molecularWeight() != null)
                <li><strong>Molecular weight:</strong> ${compound.molecularWeight().value().toPlainString()} g/mol</li>
            @endif
            @if(compound.omriListed())
                <li><strong>OMRI listed</strong></li>
            @endif
            @if(compound.cdfaRegistered())
                <li><strong>CDFA registered</strong></li>
            @endif
        </ul>
    </section>

    <section>
        <h2>Compound info</h2>
        <ul>
            <li><strong>Chemical nature:</strong> ${compound.compoundInfo().chemicalNature().name()}</li>
            <li><strong>Physical form:</strong> ${compound.compoundInfo().physicalForm().name()}</li>
            <li><strong>pH character:</strong> ${compound.phCharacter().name()}</li>
        </ul>
        <details>
            <summary><strong>Functional roles</strong> (${compound.compoundInfo().functionalRoles().size()})</summary>
            <ul>
                @for(var role : compound.compoundInfo().functionalRoles())
                    <li>${role.getClass().getSimpleName()}</li>
                @endfor
            </ul>
        </details>
        <details>
            <summary><strong>Constituent elements</strong> (${compound.compoundInfo().constituentElements().size()})</summary>
            <ul>
                @for(var element : compound.compoundInfo().constituentElements())
                    <li>${element.name()} &mdash; ${element.elementName().value()}</li>
                @endfor
            </ul>
        </details>
    </section>

    <section>
        <h2>Solubility</h2>
        <ul>
            <li><strong>Category:</strong> ${compound.solubility().category().name()}</li>
            <li><strong>g/L at 20&deg;C:</strong> ${compound.solubility().gramsPerLiterAt20C().value().toPlainString()}</li>
            <li><strong>EC contribution factor:</strong> ${compound.solubility().ecContributionFactor().toPlainString()}</li>
            @if(compound.solubility().notes() != null)
                <li><strong>Notes:</strong> ${compound.solubility().notes()}</li>
            @endif
        </ul>
    </section>

    <section>
        <h2>Bioavailability</h2>
        <ul>
            <li><strong>Primary pathway:</strong> ${compound.bioavailability().primaryPathway().name()}</li>
            <li><strong>Relative absorption rate:</strong> ${compound.bioavailability().relativeAbsorptionRate().toPlainString()}</li>
            <li><strong>Cuticular:</strong> ${String.valueOf(compound.bioavailability().cuticular())}</li>
            <li><strong>Stomatal:</strong> ${String.valueOf(compound.bioavailability().stomatal())}</li>
            <li><strong>Chelate-enhanced:</strong> ${String.valueOf(compound.bioavailability().isChelateEnhanced())}</li>
            @if(compound.bioavailability().mechanism() != null)
                <li><strong>Mechanism:</strong> ${compound.bioavailability().mechanism()}</li>
            @endif
        </ul>
    </section>

    @if(compound.volatilization() != null)
        <section>
            <h2>Volatilization</h2>
            <ul>
                <li><strong>Min effective temp:</strong> ${compound.volatilization().minEffectiveTempF().value().toPlainString()}&deg;F</li>
                <li><strong>Optimal temp:</strong> ${compound.volatilization().optimalTempF().value().toPlainString()}&deg;F</li>
                <li><strong>Max safe temp:</strong> ${compound.volatilization().maxSafeTempF().value().toPlainString()}&deg;F</li>
                <li><strong>Vapor pressure at 20&deg;C:</strong> ${compound.volatilization().vaporPressureAt20C().toPlainString()}</li>
                <li><strong>Efficacy notes:</strong> ${compound.volatilization().efficacyNotes()}</li>
                <li><strong>Safety notes:</strong> ${compound.volatilization().safetyNotes()}</li>
            </ul>
        </section>
    @endif

    @if(compound.safety() != null)
        <section>
            <h2>Safety</h2>
            <ul>
                <li><strong>Hazard level:</strong> ${compound.safety().hazardLevel().name()}</li>
                <li><strong>Max safe concentration (ppm):</strong> ${compound.safety().maxSafeConcentrationPpm().toPlainString()}</li>
                @if(compound.safety().minApplicationTempF() != null)
                    <li><strong>Min application temp:</strong> ${compound.safety().minApplicationTempF().value().toPlainString()}&deg;F</li>
                @endif
                @if(compound.safety().maxApplicationTempF() != null)
                    <li><strong>Max application temp:</strong> ${compound.safety().maxApplicationTempF().value().toPlainString()}&deg;F</li>
                @endif
                <li><strong>Requires PPE:</strong> ${String.valueOf(compound.safety().requiresProtectiveEquipment())}</li>
                <li><strong>Hazardous to bees when wet:</strong> ${String.valueOf(compound.safety().hazardousToBeesWhenWet())}</li>
                <li><strong>Requires evening application:</strong> ${String.valueOf(compound.safety().requiresEveningApplication())}</li>
                <li><strong>Application constraints:</strong> ${compound.safety().applicationConstraints()}</li>
            </ul>
        </section>
    @endif

    @if(!compound.properties().isEmpty())
        <section>
            <h2>Properties</h2>
            <ul>
                @for(var entry : compound.properties().entrySet())
                    <li><strong>${entry.getKey()}:</strong> ${entry.getValue()}</li>
                @endfor
            </ul>
        </section>
    @endif

    @if(!products.isEmpty())
        <section>
            <h2>Products containing this compound</h2>
            <ul>
                @for(var product : products)
                    <li>
                        <a href="/chemistry/products/${product.name().value()}">${product.displayName()}</a>
                        &middot; <code>${product.name().value()}</code>
                    </li>
                @endfor
            </ul>
        </section>
    @endif
`)
