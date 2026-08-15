package com.naturalist.garden.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import com.naturalist.garden.GardenTestContext;
import com.naturalist.garden.Planting;
import com.naturalist.garden.PlantedZone;
import com.naturalist.garden.PlantedZoneQuery;
import com.naturalist.garden.PlantingQuery;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only viewer for what is growing at Oak Vista: the beds, and what went into each. "Bed" is
 * the page's word for a {@link PlantedZone} — the model is named for what it records, the page for
 * what a gardener calls it.
 */
@Controller
@RequestMapping("/garden")
public class GardenController {

    private final PlantingQuery plantingQuery;
    private final PlantedZoneQuery plantedZoneQuery;

    GardenController() {
        // TODO: becomes a Spring-managed bean when the rdbms adapter replaces the in-memory context.
        GardenTestContext context = GardenTestContext.create(NaturalistDatabase.create());
        this.plantingQuery = context.plantingQuery();
        this.plantedZoneQuery = context.plantedZoneQuery();
    }

    @GetMapping
    String index() {
        return "redirect:/garden/beds";
    }

    /**
     * Garden keeps no catalog of beds — it only knows the places something was planted, so the
     * list is derived from the plantings rather than enumerated. A bed with nothing ever planted
     * in it is invisible here, which is honest: garden has nothing to say about it.
     */
    @GetMapping("/beds")
    String beds(Model model) {
        List<PlantedZone> beds = plantingQuery.findPage(PageRequest.console(0)).content().stream()
                .map(Planting::zoneName)
                .distinct()
                .map(zoneName -> plantedZoneQuery.getByZoneName(zoneName).orElseThrow())
                .toList();
        model.addAttribute("beds", beds);
        model.addAttribute("today", LocalDate.now());
        return "garden/list";
    }

    @GetMapping("/beds/{zone}")
    String bed(@PathVariable String zone, Model model) {
        var planted = plantedZoneQuery.getByZoneName(ZoneName.of(zone));
        if (planted.isEmpty()) {
            return "redirect:/garden/beds";
        }
        model.addAttribute("planted", planted.get());
        model.addAttribute("today", LocalDate.now());
        return "garden/bed";
    }

    /** One row of a bed — the useful grain when a zone holds five boxes. */
    @GetMapping("/beds/{zone}/{row}")
    String row(@PathVariable String zone, @PathVariable String row, Model model) {
        var planted = plantedZoneQuery.getBySubZoneName(ZoneName.of(zone), SubZoneName.of(row));
        if (planted.isEmpty()) {
            return "redirect:/garden/beds/" + zone;
        }
        model.addAttribute("planted", planted.get());
        model.addAttribute("today", LocalDate.now());
        return "garden/bed";
    }
}
