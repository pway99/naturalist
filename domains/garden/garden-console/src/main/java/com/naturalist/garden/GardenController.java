package com.naturalist.garden;

import com.naturalist.data.PageRequest;
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
 * Read-only viewer for what is growing at Oak Vista: the planted zones, and what went into each.
 * <p>
 * Routes follow the model rather than a friendlier synonym. A URL is an addressable contract, not
 * page copy — {@code /garden/planted-zones/backyard/backyard-south} stays true when the heading
 * above it says whatever reads best, and "beds" would have been wrong at the second level anyway,
 * since a row is not a bed.
 */
@Controller
@RequestMapping("/garden")
public class GardenController {

    private final PlantingQuery plantingQuery;
    private final PlantedZoneQuery plantedZoneQuery;

    GardenController(PlantingQuery plantingQuery, PlantedZoneQuery plantedZoneQuery) {
        this.plantingQuery = plantingQuery;
        this.plantedZoneQuery = plantedZoneQuery;
    }

    @GetMapping
    String index() {
        return "redirect:/garden/planted-zones";
    }

    /**
     * Garden keeps no catalog of places — it only knows where something was planted, so the list is
     * derived from the plantings rather than enumerated. A zone with nothing ever planted in it is
     * invisible here, which is honest: garden has nothing to say about it.
     */
    @GetMapping("/planted-zones")
    String plantedZones(Model model) {
        List<PlantedZone> plantedZones = plantingQuery.findPage(PageRequest.console(0)).content().stream()
                .map(Planting::zoneName)
                .distinct()
                .map(zoneName -> plantedZoneQuery.getByZoneName(zoneName).orElseThrow())
                .toList();
        model.addAttribute("plantedZones", plantedZones);
        model.addAttribute("today", LocalDate.now());
        return "garden/list";
    }

    @GetMapping("/planted-zones/{zone}")
    String plantedZone(@PathVariable String zone, Model model) {
        var planted = plantedZoneQuery.getByZoneName(ZoneName.of(zone));
        if (planted.isEmpty()) {
            return "redirect:/garden/planted-zones";
        }
        model.addAttribute("planted", planted.get());
        model.addAttribute("today", LocalDate.now());
        return "garden/plantedZone";
    }

    /** One row of a zone — the useful grain when a zone holds five boxes. */
    @GetMapping("/planted-zones/{zone}/{row}")
    String row(@PathVariable String zone, @PathVariable String row, Model model) {
        var planted = plantedZoneQuery.getBySubZoneName(ZoneName.of(zone), SubZoneName.of(row));
        if (planted.isEmpty()) {
            return "redirect:/garden/planted-zones/" + zone;
        }
        model.addAttribute("planted", planted.get());
        model.addAttribute("today", LocalDate.now());
        return "garden/plantedZone";
    }
}
