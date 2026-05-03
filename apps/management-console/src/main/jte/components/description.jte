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
        document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
        document.querySelectorAll('.tab').forEach(el => el.classList.remove('active'));
        document.getElementById('tab-' + level).classList.add('active');
        button.classList.add('active');
    }
</script>
