@param String preschool
@param String elementary
@param String secondary
@param String university

<section class="description-tabs">
    <nav>
        <button class="tab active" onclick="showTab(this, 'preschool')">Preschool</button>
        <button class="tab" onclick="showTab(this, 'elementary')">Elementary</button>
        <button class="tab" onclick="showTab(this, 'secondary')">Secondary</button>
        <button class="tab" onclick="showTab(this, 'university')">University</button>
    </nav>
    <div class="tab-content active" id="tab-preschool">
        $unsafe{preschool}
    </div>
    <div class="tab-content" id="tab-elementary">
        $unsafe{elementary}
    </div>
    <div class="tab-content" id="tab-secondary">
        $unsafe{secondary}
    </div>
    <div class="tab-content" id="tab-university">
        $unsafe{university}
    </div>
</section>

<script>
    function showTab(button, level) {
        var section = button.closest('.description-tabs');
        section.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
        section.querySelectorAll('.tab').forEach(el => el.classList.remove('active'));
        section.querySelector('#tab-' + level).classList.add('active');
        button.classList.add('active');
        localStorage.setItem('description-tab', level);
    }
    (function () {
        var saved = localStorage.getItem('description-tab');
        if (!saved) return;
        document.querySelectorAll('.description-tabs').forEach(function (section) {
            var btn = Array.from(section.querySelectorAll('.tab')).find(
                function (b) { return b.textContent.trim().toLowerCase() === saved; });
            if (btn) showTab(btn, saved);
        });
    }());
</script>
