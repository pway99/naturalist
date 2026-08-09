@import com.naturalist.soil.SoilProfile

@param SoilProfile profile

!{var info = profile.info();}

@template.layout.page(title = info.name().value(), content = @`
    <h1>Soil Profile — ${info.name().value()}</h1>
    <dl class="taxonomy">
        <dt>zone</dt>
        <dd>${info.zoneName().value()}@if(info.isSubZoneScoped()) · ${info.subZoneName().value()}@endif</dd>
    </dl>

    @if(profile.labAnalyses().isEmpty())
        <p><em>No lab analyses on record for this profile.</em></p>
    @else
        @for(var analysis : profile.labAnalyses())
            !{var a = analysis.info();}
            !{var panel = analysis.nutrients();}
            !{var chars = analysis.physicalCharacteristics();}
            !{var bs = chars.cationBaseSaturation();}
            <section class="analysis">
                <h2>${a.sampleDate().toString()} — ${a.crop().value()}</h2>
                <p class="analysis-provenance">${a.labId()} · sample ${a.labSampleId()}</p>

                <h3>Primary nutrients</h3>
                <table>
                    <thead><tr><th>Nutrient</th><th>Value</th><th>Unit</th></tr></thead>
                    <tbody>
                        @template.soil.readingRow(label = "Nitrate-N", reading = panel.primary().nitrateN())
                        @template.soil.readingRow(label = "Phosphorus (P₂O₅)", reading = panel.primary().phosphorusP2O5())
                        @template.soil.readingRow(label = "Potassium — exchangeable", reading = panel.primary().potassiumExch())
                        @template.soil.readingRow(label = "Potassium — soluble", reading = panel.primary().potassiumSoluble())
                    </tbody>
                </table>

                <h3>Secondary nutrients</h3>
                <table>
                    <thead><tr><th>Nutrient</th><th>Value</th><th>Unit</th></tr></thead>
                    <tbody>
                        @template.soil.readingRow(label = "Calcium — exchangeable", reading = panel.secondary().calciumExch())
                        @template.soil.readingRow(label = "Calcium — soluble", reading = panel.secondary().calciumSoluble())
                        @template.soil.readingRow(label = "Magnesium — exchangeable", reading = panel.secondary().magnesiumExch())
                        @template.soil.readingRow(label = "Magnesium — soluble", reading = panel.secondary().magnesiumSoluble())
                        @template.soil.readingRow(label = "Sodium — exchangeable", reading = panel.secondary().sodiumExch())
                        @template.soil.readingRow(label = "Sodium — soluble", reading = panel.secondary().sodiumSoluble())
                        @template.soil.readingRow(label = "Sulfate", reading = panel.secondary().sulfate())
                    </tbody>
                </table>

                <h3>Micronutrients</h3>
                <table>
                    <thead><tr><th>Nutrient</th><th>Value</th><th>Unit</th></tr></thead>
                    <tbody>
                        @template.soil.readingRow(label = "Zinc", reading = panel.micro().zinc())
                        @template.soil.readingRow(label = "Manganese", reading = panel.micro().manganese())
                        @template.soil.readingRow(label = "Iron", reading = panel.micro().iron())
                        @template.soil.readingRow(label = "Copper", reading = panel.micro().copper())
                        @template.soil.readingRow(label = "Boron", reading = panel.micro().boron())
                        @template.soil.readingRow(label = "Chloride", reading = panel.micro().chloride())
                    </tbody>
                </table>

                <h3>Physical &amp; derived</h3>
                <table>
                    <tbody>
                        <tr><td>pH</td><td>${chars.pH().value().toString()}</td></tr>
                        <tr><td>Soil salinity (EC)</td><td>${chars.ecDsPerMeter().value().toString()} dS/m</td></tr>
                        <tr><td>CEC</td><td>${chars.cecMeqPer100g().value().toString()} meq/100g</td></tr>
                        <tr><td>Limestone</td><td>${chars.limestonePct().value().toString()} %</td></tr>
                        <tr><td>Saturation</td><td>${chars.saturationPct().value().toString()} %</td></tr>
                    </tbody>
                </table>

                <h3>Cation base saturation</h3>
                <table>
                    <tbody>
                        <tr><td>Calcium</td><td>${bs.calciumPct().toString()} %</td></tr>
                        <tr><td>Magnesium</td><td>${bs.magnesiumPct().toString()} %</td></tr>
                        <tr><td>Potassium</td><td>${bs.potassiumPct().toString()} %</td></tr>
                        <tr><td>Sodium</td><td>${bs.sodiumPct().toString()} %</td></tr>
                        <tr><td>Hydrogen</td><td>${bs.hydrogenPct().toString()} %</td></tr>
                    </tbody>
                </table>
            </section>
        @endfor
    @endif
`)
