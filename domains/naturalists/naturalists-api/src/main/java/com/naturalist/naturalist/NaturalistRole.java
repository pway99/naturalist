package com.naturalist.naturalist;

/**
 * The primary relationship a {@link Naturalist} has with Oak Vista and its ecosystem.
 * <p>
 * Role governs which application capabilities a naturalist can access and what
 * responsibilities are attributed to them in the domain model. A single person
 * may hold multiple roles over time — a student who becomes a caretaker is
 * represented by a role change on the entity, not a new entity.
 */
public enum NaturalistRole {

    /**
     * Occasional observer — present at Oak Vista for a visit, participating in
     * garden walks, pollinator surveys, or ecological observation sessions.
     * Read-only access to all catalog knowledge; no amendment or management
     * responsibilities attributed.
     */
    VISITOR,

    /**
     * Active garden tender — responsible for planting, watering, soil amendment,
     * pest management, and harvest in one or more zones. All amendment events,
     * irrigation events, and chemical applications are attributable to a caretaker.
     */
    CARETAKER,

    /**
     * Formal learner — enrolled in a structured educational programme using Oak Vista
     * as a living laboratory. Associated with lesson plans and ecological stage
     * progression tracking. Corresponds to Durrell's belief that nature is the
     * best classroom.
     */
    STUDENT,

    /**
     * Apiary manager — responsible for hive health monitoring, Varroa treatment
     * scheduling, Small Hive Beetle management, and honey harvest. A keeper must
     * hold at minimum {@link EcologicalStage#PRACTITIONER} stage for apiary
     * responsibilities to be attributed.
     */
    KEEPER,

    /**
     * Leads others in ecological observation and interpretation — guides field
     * walks, facilitates Durrell-level descriptions for visitors and students.
     * A teacher is also implicitly a CARETAKER or KEEPER in the Oak Vista context.
     */
    TEACHER
}
