@import com.naturalist.insects.InsectCitationView
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectFunctionalRole
@import com.naturalist.insects.InsectGenus
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.InsectSpecies
@import com.naturalist.insects.InsectImage
@import com.naturalist.insects.InsectImageId
@import com.naturalist.insects.FieldObservation
@import com.naturalist.insects.console.BreadcrumbSegment
@import com.naturalist.insects.console.CladeTrail
@import com.naturalist.insects.lifestage.LifeStage
@import java.util.List
@import java.util.Map
@import org.springframework.security.web.csrf.CsrfToken

@param InsectSpecies species
@param InsectGenus genus
@param InsectFamily family
@param InsectOrder order
@param List<InsectImage> images
@param List<LifeStage> stages = List.of()
@param InsectFunctionalRole role = null
@param InsectCitationView citations = null
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""
@param CsrfToken _csrf = null
@param List<BreadcrumbSegment> breadcrumb = List.of()
@param CladeTrail cladeTrail = null
@param java.util.List<com.naturalist.insects.console.AncestorIntro> ancestorIntros = List.of()
@param boolean collected = false
@param boolean lens = false
@param Map<InsectImageId, FieldObservation> observations = java.util.Map.of()

!{var commonName = species.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(species.name().value());}
!{var binomial = genus.genus().value() + " " + species.epithet().value();}

@template.layout.page(title = binomial, content = @`
    @template.insects.nav(breadcrumb = breadcrumb, cladeTrail = cladeTrail)
    @template.insects.ancestorIntros(intros = ancestorIntros)

    @if(!stages.isEmpty())
        <details class="ancestor-intro" data-storage-key="life-stages">
            <summary><h2>Life Stages</h2></summary>
            <section class="plate">
                @for(var stage : stages)
                    <details class="ancestor-intro stage-detail" data-storage-key="life-stage-${stage.kind().name().toLowerCase()}">
                        <summary><h2>${stage.kind().name().charAt(0) + stage.kind().name().substring(1).toLowerCase()}</h2></summary>
                        <p class="lead">${stage.description().secondary()}</p>
                        @template.insects.stageFacts(stage = stage)
                    </details>
                @endfor
            </section>
        </details>
    @endif

    <h1>${commonName}</h1>

    @if(collected)
        <p class="collected-badge">✓ In your collection</p>
    @endif

    @if(role != null && (!role.guilds().isEmpty() || role.beneficial()))
        <div class="guilds">
            @for(var guild : role.guilds())
                <a href="/insects/guild/${guild.name()}"
                   class="badge guild-${guild.name().toLowerCase()}">${guild.name()}</a>
            @endfor
            @if(role.beneficial())
                <span class="badge beneficial">beneficial</span>
            @endif
        </div>
    @endif

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    @if(species.sightingNotes() != null)
        <section>
            <h2>Sighting Notes</h2>
            <p>${species.sightingNotes()}</p>
        </section>
    @endif

    @if(!images.isEmpty())
        <section>
            <h2>Photo Gallery</h2>
            <div class="image-gallery">
                @for(var img : images)
                    <figure class="observation-card">
                        <img src="/insects/uploads/${img.resourceName().value()}"
                             alt="${species.name().value()}"
                             loading="lazy"
                             onerror="this.onerror=null; this.src='/insects/images/${img.resourceName().value()}'">
                        <%-- Fall back to classpath images for pre-upload catalog images --%>
                        @if(observations.containsKey(img.id()))
                            !{var obs = observations.get(img.id());}
                            <figcaption>
                                @if(obs.location() != null)
                                    <p class="obs-location">${obs.location()}</p>
                                @endif
                                @if(obs.identification() != null)
                                    !{var id = obs.identification();}
                                    <div class="obs-identification">
                                        <p class="obs-confidence">
                                            <span class="obs-confidence-value">${String.format("%.0f%%", id.confidence() * 100)}</span> confidence
                                        </p>
                                        <details class="id-evidence">
                                            <summary>Why this ID?</summary>
                                            <p>${id.evidence()}</p>
                                        </details>
                                        @if(!id.alternatives().isEmpty())
                                            <p class="id-alternatives-label">Also considered</p>
                                            <ul class="id-alternatives">
                                                @for(var alt : id.alternatives())
                                                    <li>
                                                        <em>${alt.scientificName()}</em>@if(alt.commonName() != null) (${alt.commonName()})@endif
                                                        <span class="id-alternative-confidence">${String.format("%.0f%%", alt.confidence() * 100)}</span>
                                                    </li>
                                                @endfor
                                            </ul>
                                        @endif
                                    </div>
                                @endif
                                <form method="post" action="/insects/${species.name().value()}/notes" class="obs-notes-form">
                                    @if(_csrf != null)
                                        <input type="hidden" name="${_csrf.getParameterName()}"
                                               value="${_csrf.getToken()}">
                                    @endif
                                    <input type="hidden" name="observationId" value="${obs.id().value().toString()}">
                                    <label class="obs-notes-label">Your notes</label>
                                    <textarea name="notes" rows="2"
                                              placeholder="Your field notes...">${obs.notes() != null ? obs.notes() : ""}</textarea>
                                    <button type="submit">Save notes</button>
                                </form>
                            </figcaption>
                        @endif
                    </figure>
                @endfor
            </div>
        </section>
    @endif

    @if(lens && images.isEmpty())
        <p class="collection-empty">No photos of yours here yet — add one below to start your collection.</p>
    @endif

    @if(_csrf != null)
        <form method="post" action="/insects/${species.name().value()}/observe" class="observe-form">
            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
            <input type="text" name="notes" placeholder="Notes (optional)">
            <button type="submit">Add to my collection</button>
        </form>
    @endif

    <section class="add-image">
        <h2>Add Photo</h2>
        <form id="identify-form" action="/insects/${species.name().value()}/images" method="post"
              enctype="multipart/form-data">
            @if(_csrf != null)
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
            @endif
            <div class="form-group">
                <input type="file" name="image" accept="image/*" capture="environment" required>
            </div>
            <div class="form-group">
                <input type="text" name="location" placeholder="Location (e.g. Oak Vista, Chico, CA)">
            </div>
            <div class="form-group">
                <textarea name="notes" rows="2" placeholder="Field notes (optional)"></textarea>
            </div>
            <button type="submit">Add photo</button>
        </form>
        <script src="/js/image-upload.js"></script>
    </section>

    @if(species.chemicalDefense() != null)
        <section>
            <h2>Chemical Defence</h2>
            <p><strong>Mechanism:</strong> ${species.chemicalDefense().mechanism()}</p>
            @if(species.chemicalDefense().sourceCompounds() != null)
                <p><strong>Source compounds:</strong> ${species.chemicalDefense().sourceCompounds()}</p>
            @endif
            @if(species.chemicalDefense().aposematicSignal() != null)
                <p><strong>Aposematic signal:</strong> ${species.chemicalDefense().aposematicSignal()}</p>
            @endif
            @if(!species.chemicalDefense().protectedStages().isEmpty())
                <p><strong>Protected stages:</strong>
                    @for(var stage : species.chemicalDefense().protectedStages())
                        <span class="badge stage">${stage.name()}</span>
                    @endfor
                </p>
            @endif
        </section>
    @endif

    @if(species.voltinism() != null)
        <section>
            <h2>Voltinism</h2>
            <p><strong>Pattern:</strong> ${species.voltinism().pattern().name()}</p>
            @if(species.voltinism().notes() != null)
                <p>${species.voltinism().notes()}</p>
            @endif
        </section>
    @endif

    @if(species.habitatRequirements() != null)
        <section>
            <h2>Habitat Requirements</h2>
            @if(species.habitatRequirements().nectarSources() != null)
                <p><strong>Nectar sources:</strong> ${species.habitatRequirements().nectarSources()}</p>
            @endif
            @if(species.habitatRequirements().shelter() != null)
                <p><strong>Shelter:</strong> ${species.habitatRequirements().shelter()}</p>
            @endif
            @if(species.habitatRequirements().preyAvailability() != null)
                <p><strong>Prey availability:</strong> ${species.habitatRequirements().preyAvailability()}</p>
            @endif
            @if(species.habitatRequirements().lighting() != null)
                <p><strong>Lighting response:</strong> ${species.habitatRequirements().lighting()}</p>
            @endif
            @if(species.habitatRequirements().elevationRange() != null)
                <p><strong>Elevation:</strong> ${species.habitatRequirements().elevationRange()}</p>
            @endif
        </section>
    @endif

    @if(species.beneficialProfile() != null)
        <section>
            <h2>Beneficial Profile</h2>
            <p>${species.beneficialProfile().significance()}</p>
            @if(species.beneficialProfile().pestManagementValue() != null)
                <p><strong>Pest management:</strong> ${species.beneficialProfile().pestManagementValue()}</p>
            @endif
        </section>
    @endif

    @if(species.ecologicalSignificance() != null)
        <section>
            <h2>Ecological Significance</h2>
            @if(species.ecologicalSignificance().foodWebPosition() != null)
                <p><strong>Food web:</strong> ${species.ecologicalSignificance().foodWebPosition()}</p>
            @endif
            @if(species.ecologicalSignificance().indicatorValue() != null)
                <p><strong>Indicator value:</strong> ${species.ecologicalSignificance().indicatorValue()}</p>
            @endif
        </section>
    @endif

    @template.insects.citations(citations = citations)

`)
