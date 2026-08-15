@import com.naturalist.soil.observation.NutrientLine
@import java.util.List
@param List<NutrientLine> lines
<table>
    <thead>
        <tr><th>Nutrient</th><th>Value</th><th>Unit</th><th>Optimum (as printed)</th><th>Position</th></tr>
    </thead>
    <tbody>
        @for(var line : lines)
            @template.soil.readingRow(line = line)
        @endfor
    </tbody>
</table>
