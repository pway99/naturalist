@import com.naturalist.soil.SoilProfile
@import java.util.List

@param List<SoilProfile> profiles

@template.layout.page(title = "Soil Profiles", content = @`
    <h1>Soil Profiles</h1>
    <p>Managed soil units at Oak Vista, with their most recent lab analysis.</p>

    <div class="entity-grid">
        @for(var profile : profiles)
            !{var info = profile.info();}
            <article>
                <header>
                    <a href="/soil/profiles/${info.name().value()}">
                        <strong>${info.name().value()}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>zone</dt>
                    <dd>${info.zoneName().value()}@if(info.isSubZoneScoped()) · ${info.subZoneName().value()}@endif</dd>
                    @if(profile.latestLabAnalysis().isPresent())
                        !{var latest = profile.latestLabAnalysis().get();}
                        <dt>crop</dt>
                        <dd>${latest.info().crop().value()}</dd>
                        <dt>latest test</dt>
                        <dd>${latest.info().sampleDate().toString()}</dd>
                    @endif
                </dl>
            </article>
        @endfor
    </div>
`)
