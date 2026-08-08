@import com.naturalist.insects.FieldObservation
@import com.naturalist.insects.InsectImage
@import com.naturalist.insects.InsectImageId
@import java.util.List
@import java.util.Map

@param List<InsectImage> images = List.of()
@param Map<InsectImageId, FieldObservation> observations = Map.of()
@param String alt = ""
@param String notesAction = ""
@param String returnPath = ""
@param String csrfParam = null
@param String csrfToken = null

@if(!images.isEmpty())
    <section>
        <h2>Photo Gallery</h2>
        <div class="image-gallery">
            @for(var img : images)
                <figure class="observation-card">
                    <img src="/insects/uploads/${img.resourceName().value()}"
                         alt="${alt}"
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
                                            @for(var alt2 : id.alternatives())
                                                <li>
                                                    <em>${alt2.scientificName()}</em>@if(alt2.commonName() != null) (${alt2.commonName()})@endif
                                                    <span class="id-alternative-confidence">${String.format("%.0f%%", alt2.confidence() * 100)}</span>
                                                </li>
                                            @endfor
                                        </ul>
                                    @endif
                                </div>
                            @endif
                            @if(!notesAction.isEmpty())
                                <form method="post" action="${notesAction}" class="obs-notes-form">
                                    @if(csrfParam != null)
                                        <input type="hidden" name="${csrfParam}"
                                               value="${csrfToken}">
                                    @endif
                                    <input type="hidden" name="observationId" value="${obs.id().value().toString()}">
                                    <input type="hidden" name="returnPath" value="${returnPath}">
                                    <label class="obs-notes-label">Your notes</label>
                                    <textarea name="notes" rows="2"
                                              placeholder="Your field notes...">${obs.notes() != null ? obs.notes() : ""}</textarea>
                                    <button type="submit">Save notes</button>
                                </form>
                            @endif
                        </figcaption>
                    @endif
                </figure>
            @endfor
        </div>
    </section>
@endif
