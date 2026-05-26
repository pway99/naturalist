@import com.naturalist.insects.console.AncestorIntro

@param AncestorIntro intro

<details class="ancestor-intro" data-storage-key="${intro.storageKey()}">
    <summary><h2>${intro.label()}</h2></summary>
    @template.components.description(
        preschool = intro.preschool(),
        elementary = intro.elementary(),
        secondary = intro.secondary(),
        university = intro.university())
</details>
