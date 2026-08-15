@import com.naturalist.soil.observation.NutrientLine
@import com.naturalist.soil.observation.Nutrients
@param NutrientLine line
!{var label = Nutrients.printedNameOf(line.nutrientName());}
@if(line.wasMeasured())
    <tr>
        <td>${label}</td>
        <td>${line.reading().get().value().toString()}</td>
        <td>${line.reading().get().unit().symbol()}</td>
        @if(line.optimum().isPresent())
            <td>${line.optimum().get().range().printedForm()}</td>
        @else
            <td class="not-reported">no printed optimum</td>
        @endif
        @if(line.comparison().isConclusive())
            <td class="verdict" title="${line.comparison().source().label()}">${line.comparison().verdict().name().toLowerCase()} range</td>
        @else
            <td></td>
        @endif
    </tr>
@else
    <tr class="not-reported">
        <td>${label}</td>
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
