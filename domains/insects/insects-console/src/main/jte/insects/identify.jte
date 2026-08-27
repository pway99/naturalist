@import org.springframework.security.web.csrf.CsrfToken

@param CsrfToken _csrf = null
@param String identified = null
@param String limit = null

!{var csrfName = _csrf != null ? _csrf.getParameterName() : "";}
!{var csrfValue = _csrf != null ? _csrf.getToken() : "";}

@template.layout.page(
    title = "Identify Insect",
    content = @`
        <div class="identify-page">
            <h1>Identify an Insect</h1>
            @if(identified != null)
                <p class="success-message">Identified as <strong>${identified}</strong>.
                   The species has been added to the catalog with your observation and photo.</p>
            @endif
            @if(limit != null)
                @if("PER_USER".equals(limit))
                    <p role="alert">You've reached your daily identification limit.
                       It resets tomorrow -- try again then.</p>
                @elseif("RATE".equals(limit))
                    <p role="alert">Too many identification requests right now.
                       Please wait a moment and try again.</p>
                @else
                    <p role="alert">Identification is temporarily unavailable due to high demand.
                       Please try again later.</p>
                @endif
            @endif
            <p>Upload a photo and Claude will suggest an identification.
               You can verify the result against authoritative references.</p>

            <form id="identify-form" action="/insects/identify" method="post"
                  enctype="multipart/form-data">
                <input type="hidden" name="${csrfName}" value="${csrfValue}">

                <div class="form-group">
                    <label for="image">Photo</label>
                    <input type="file" id="image" name="image"
                           accept="image/*" capture="environment" required>
                </div>

                <img id="image-preview" style="display:none; max-width:400px; margin:1rem 0;"
                     alt="Preview">

                <div class="form-group">
                    <label for="location">Location</label>
                    <input type="text" id="location" name="location"
                           placeholder="e.g. Deer Creek, Butte County, CA">
                </div>

                <div class="form-group">
                    <label for="capturedAt">Date observed</label>
                    <input type="text" id="capturedAt" name="capturedAt"
                           placeholder="auto-detected from photo">
                </div>

                <div class="form-group">
                    <label for="notes">Field notes (optional)</label>
                    <textarea id="notes" name="notes" rows="3"
                              placeholder="Habitat, behavior, weather..."></textarea>
                </div>

                <button type="submit">Identify</button>
            </form>
        </div>

        <script src="/js/image-upload.js"></script>
    `
)
