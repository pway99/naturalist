@import com.naturalist.library.console.GlossaryLinker
@import com.naturalist.soil.SoilProfile
@import com.naturalist.soil.observation.AssessmentSource
@import com.naturalist.soil.observation.NutrientCategory
@import com.naturalist.soil.observation.RecommendedAmount

@param SoilProfile profile
@param GlossaryLinker glossaryLinker = GlossaryLinker.none()

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
            <section class="analysis">
                <h2>${a.sampleDate().toString()} — ${a.cropType().value()}</h2>
                <p class="analysis-provenance">${a.labId()} · sample ${a.labSampleId()}
                    @if(a.sampleDepth() != null)
                        · sampled at ${a.sampleDepth().value().toString()} in
                    @else
                        · <span class="not-reported">depth not recorded</span>
                    @endif
                    @if(a.samplingProtocol() != null)
                        !{var p = a.samplingProtocol();}
                        · ${p.subsampleCount()} subsample(s), ${p.tool().name().toLowerCase().replace('_', ' ')}, ${p.compositingMethod().name().toLowerCase().replace('_', ' ')}
                    @endif
                </p>

                <p class="fraction-note">$unsafe{glossaryLinker.linkHtml("Each cation is reported as two fractions — exchangeable, held on soil surfaces, and soluble, dissolved and available to roots now.", a.sampleDate().toString() + "-fractions")}</p>

                <h3>Primary nutrients</h3>
                @template.soil.nutrientTable(lines = analysis.nutrientLines(NutrientCategory.PRIMARY))

                <h3>Secondary nutrients</h3>
                @template.soil.nutrientTable(lines = analysis.nutrientLines(NutrientCategory.SECONDARY))

                <h3>Micronutrients</h3>
                @template.soil.nutrientTable(lines = analysis.nutrientLines(NutrientCategory.MICRO))

                @if(analysis.physicalCharacteristics().isPresent())
                    !{var chars = analysis.physicalCharacteristics().get();}
                    !{var bs = chars.cationBaseSaturation();}
                    <h3>Physical &amp; derived</h3>
                    <table>
                        <tbody>
                            <tr><td>pH</td><td>${chars.pH().value().toString()}</td></tr>
                            <tr><td>$unsafe{glossaryLinker.linkHtml("Soil salinity (EC)", a.sampleDate().toString())}</td><td>${chars.ecDsPerMeter().value().toString()} dS/m</td></tr>
                            <tr><td>SAR (sodium adsorption ratio)</td><td>${chars.sar().value().toString()}</td></tr>
                            <tr><td>$unsafe{glossaryLinker.linkHtml("CEC", a.sampleDate().toString())}</td><td>${chars.cecMeqPer100g().value().toString()} meq/100g</td></tr>
                            <tr><td>$unsafe{glossaryLinker.linkHtml("Limestone", a.sampleDate().toString())}</td><td>${chars.limestonePct().value().toString()} %</td></tr>
                            <tr><td>$unsafe{glossaryLinker.linkHtml("Saturation", a.sampleDate().toString())}</td><td>${chars.saturationPct().value().toString()} %</td></tr>
                        </tbody>
                    </table>

                    <h3>Cation base saturation</h3>
                    <table>
                        <tbody>
                            <tr><td>Calcium</td><td>${bs.calciumPct().toString()} %</td></tr>
                            <tr><td>Magnesium</td><td>${bs.magnesiumPct().toString()} %</td></tr>
                            <tr><td>Potassium</td><td>${bs.potassiumPct().toString()} %</td></tr>
                            <tr><td>Sodium</td><td>${bs.sodiumPct().toString()} %</td></tr>
                            @if(bs.hydrogenBelowDetectionLimit())
                                <tr><td>Hydrogen</td><td title="The lab reported this as below its detection limit. The true value is somewhere between zero and the figure shown.">&lt; ${bs.hydrogenPct().toString()} %</td></tr>
                            @else
                                <tr><td>Hydrogen</td><td>${bs.hydrogenPct().toString()} %</td></tr>
                            @endif
                        </tbody>
                    </table>
                    @if(bs.hydrogenBelowDetectionLimit())
                        <p class="not-reported">Base saturation sums to between ${bs.saturationSumLowerBound().toString()}% and ${bs.saturationSumUpperBound().toString()}% — hydrogen was below the detection limit, so the total is a range.</p>
                    @endif
                @else
                    <p class="not-reported"><em>No physical or derived measurements on record for this analysis.</em></p>
                @endif

                @if(!analysis.reportedRecommendations().isEmpty())
                    <h3>What the lab recommended</h3>
                    <table>
                        <thead><tr><th>Input</th><th>Amount</th><th>Unit</th><th>Via</th></tr></thead>
                        <tbody>
                        @for(var r : analysis.reportedRecommendations().applicationsOnly().stream().toList())
                                <tr>
                                    <td>${r.inputName().value().replace('-', ' ')}</td>
                                    @if(r.amount() instanceof RecommendedAmount.Quantity q)
                                        <td>${q.value().toString()}</td>
                                    @elseif(r.amount() instanceof RecommendedAmount.BelowDetectionLimit b)
                                        <td title="Below what the lab's method resolves. The true amount is between zero and the figure shown.">&lt; ${b.limit().toString()}</td>
                                    @else
                                        <td class="advised-none" title="The lab recommended applying none. This is advice, not a missing value.">none</td>
                                    @endif
                                    <td>${r.unit().symbol()}</td>
                                    <td>@if(r.route() != null)${r.route().name().toLowerCase()}@endif</td>
                                </tr>
                            @endfor
                        </tbody>
                    </table>
                @endif

                <p class="assessment-source">Values and optimum ranges are ${AssessmentSource.LAB_PRINTED.label()}.
                    The <em>Position</em> column is ${AssessmentSource.DERIVED_FROM_PRINTED_RANGE.label()} —
                    it is not the lab's own five-band verdict, which uses information the report does not print as numbers,
                    and the two can disagree.</p>
            </section>
        @endfor
    @endif
`)
