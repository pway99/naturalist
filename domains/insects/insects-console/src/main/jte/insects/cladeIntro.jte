@import com.naturalist.insects.console.BreadcrumbSegment
@import java.util.List

@param List<BreadcrumbSegment> breadcrumb
@param String cladePreschool = ""
@param String cladeElementary = ""
@param String cladeSecondary = ""
@param String cladeUniversity = ""

@template.insects.breadcrumb(segments = breadcrumb)
<details class="clade-introduction" id="clade-intro">
    <summary><h2>Class Insecta</h2></summary>
    @template.components.description(
        preschool = cladePreschool,
        elementary = cladeElementary,
        secondary = cladeSecondary,
        university = cladeUniversity)
</details>
<script>
    (function () {
        var el = document.getElementById('clade-intro');
        if (!el) return;
        var stored = localStorage.getItem('clade-intro-open');
        if (stored !== 'false') el.open = true;
        el.addEventListener('toggle', function () {
            localStorage.setItem('clade-intro-open', el.open);
        });
    }());
</script>
