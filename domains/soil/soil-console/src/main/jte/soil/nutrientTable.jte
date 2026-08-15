@import com.naturalist.soil.console.catalog.NutrientChemistryLinks
@import com.naturalist.soil.observation.NutrientLine
@import java.util.List
@param List<NutrientLine> lines
@param NutrientChemistryLinks chemistryLinks = NutrientChemistryLinks.none()
<table>
    <thead>
        <tr><th>Nutrient</th><th>Value</th><th>Unit</th><th>Optimum (as printed)</th><th>Position</th></tr>
    </thead>
    <tbody>
        @for(var line : lines)
            @template.soil.readingRow(line = line, chemistryLinks = chemistryLinks)
        @endfor
    </tbody>
</table>
