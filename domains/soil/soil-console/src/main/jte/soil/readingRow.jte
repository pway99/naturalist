@import com.naturalist.soil.console.catalog.NutrientChemistryLinks
@import com.naturalist.soil.observation.NutrientLine
@import com.naturalist.soil.observation.Nutrients
@param NutrientLine line
@param NutrientChemistryLinks chemistryLinks = NutrientChemistryLinks.none()
!{var label = Nutrients.printedNameOf(line.nutrientName());}
!{var substanceUrl = chemistryLinks.linkFor(line.nutrientName());}
!{var substanceTitle = chemistryLinks.titleFor(line.nutrientName());}
@if(line.wasMeasured())
    <tr>
        @if(substanceUrl != null)
            <td><a href="${substanceUrl}" title="${substanceTitle}">${label}</a></td>
        @else
            <td>${label}</td>
        @endif
        <td>${line.reading().get().value().toString()}</td>
        <td>${line.reading().get().unit().symbol()}</td>
        @if(line.optimum().isPresent())
            <td>${line.optimum().get().range().printedForm()}</td>
        @else
            <td class="not-reported">no printed optimum</td>
        @endif
        @if(line.comparison().isConclusive())
            <td class="verdict verdict-${line.comparison().verdict().name().toLowerCase()}" title="${line.comparison().source().label()}">${line.comparison().verdict().name().toLowerCase()} range</td>
        @else
            <td></td>
        @endif
    </tr>
@else
    <tr class="not-reported">
        @if(substanceUrl != null)
            <td><a href="${substanceUrl}" title="${substanceTitle}">${label}</a></td>
        @else
            <td>${label}</td>
        @endif
        <td title="This lab did not report this nutrient. Not the same as a measured zero.">not reported</td>
        <td></td>
        @if(line.optimum().isPresent())
            <td>${line.optimum().get().range().printedForm()}</td>
        @else
            <td></td>
        @endif
        <td></td>
    </tr>
@endif
