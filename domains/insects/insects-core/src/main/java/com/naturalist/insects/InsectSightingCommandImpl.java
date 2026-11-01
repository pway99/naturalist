package com.naturalist.insects;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import com.naturalist.observation.OrganismObservation;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@DomainService
class InsectSightingCommandImpl implements InsectCommand.SightingCommand {

    private final Observer observer = Observer.forClass(InsectSightingCommandImpl.class);
    private final InsectCommand.ObservationCommand observations;
    private final InsectQuery.ObservationQuery observationQuery;

    InsectSightingCommandImpl(InsectCommand.ObservationCommand observations,
                              InsectQuery.ObservationQuery observationQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(observations, "observations")
                        .notNull(observationQuery, "observationQuery"))
                .throwWhenInvalid();
        this.observations = observations;
        this.observationQuery = observationQuery;
    }

    @Override
    public InsectObservationId record(InsectRankName subject, NaturalistName naturalist, @Nullable String notes) {
        observer.arguments("record", i -> i
                        .identifier(subject, "subject")
                        .entityName(naturalist, "naturalist"))
                .throwWhenInvalid();
        var id = InsectObservationId.create();
        observations.insert(new OrganismObservation<InsectObservationId, InsectRankName>(
                id, naturalist, subject, Instant.now(), blankToNull(notes), null, null));
        return id;
    }

    @Override
    public void updateNotes(InsectObservationId observationId, NaturalistName viewer, @Nullable String notes) {
        observer.arguments("updateNotes", i -> i
                        .entityId(observationId, "observationId")
                        .entityName(viewer, "viewer"))
                .throwWhenInvalid();
        observationQuery.getByName(observationId)
                .filter(o -> o.observedBy().equals(viewer))
                .ifPresent(o -> observations.update(o.withNotes(blankToNull(notes))));
    }

    private static @Nullable String blankToNull(@Nullable String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
