@param String id
@param String label
@param String text
@param String moreHref
@param String moreLabel

<%-- A small circled "?" that toggles a native top-layer popover holding a
     short explanation plus a "learn more" link. No JavaScript: the HTML
     `popover` attribute handles open/dismiss (click-outside and Esc) and the
     keyboard/focus semantics; the invoking button is the popover's implicit
     anchor (see the anchor-positioning rules in naturalist.css).

     `text` is emitted with $unsafe so callers may include light inline markup
     (<strong>/<em>). It is trusted, developer-authored copy — never user
     input — matching how components/description.jte renders its levels. --%>
<span class="info-popover">
    <button type="button" class="info-popover-trigger"
            popovertarget="${id}" aria-label="${label}">?</button>
    <span id="${id}" popover class="info-popover-box">
        $unsafe{text}
        <a href="${moreHref}">${moreLabel}</a>
    </span>
</span>
