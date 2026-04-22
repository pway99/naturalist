package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.TemperatureFahrenheit;
import com.naturalist.ddd.AggregateRoot;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.ddd.UniqueValue;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A chemical compound — the aggregate root for compound data.
 * <p>
 * {@code Compound} carries stable slug identity via {@link CompoundName} and is the
 * consistency boundary. It owns {@link CompoundInfo} (chemical classification), all
 * profile value objects, and {@code properties}.
 * <p>
 * {@link CompoundInfo} groups the physical-chemical classification fields.
 * Profiles ({@link SolubilityProfile}, {@link BioavailabilityProfile}, etc.) and
 * {@code properties} have no independent lifecycle — they exist only as attributes of
 * their parent compound.
 * <p>
 * {@code Compound} instances are assembled from {@code compounds.json} at startup.
 * The Java model is agnostic to specific compounds — the catalog is data, not code.
 * Domain modules reference compounds by {@link CompoundName} slug.
 */
@AggregateRoot
public record Compound(
        CompoundName name,
        @UniqueValue String commonName,
        CompoundInfo compoundInfo,
        SolubilityProfile solubility,
        BioavailabilityProfile bioavailability,
        @Nullable VolatilizationProfile volatilization,
        @Nullable SafetyProfile safety,
        Map<String, String> properties
) implements NamedEntity<CompoundName> {

    public Compound withSafety(@Nullable SafetyProfile safety) {
        return new Compound(name, commonName, compoundInfo, solubility, bioavailability,
                volatilization, safety, properties);
    }

    // ── Chemical classification delegations ──────────────────────────────────

    public String formula() {
        return compoundInfo.formula();
    }

    public @Nullable MolecularWeight molecularWeight() {
        return compoundInfo.molecularWeight();
    }

    public CompoundType type() {
        return compoundInfo.type();
    }

    public PhCharacter phCharacter() {
        return compoundInfo.phCharacter();
    }

    public boolean isPHNeutral() {
        return compoundInfo.isPHNeutral();
    }

    public boolean isAcidic() {
        return compoundInfo.isAcidic();
    }

    // ── Profile accessors ────────────────────────────────────────────────────

    public Optional<VolatilizationProfile> volatilizationOptional() {
        return Optional.ofNullable(volatilization);
    }

    public Optional<SafetyProfile> safetyOptional() {
        return Optional.ofNullable(safety);
    }

    // ── Properties ───────────────────────────────────────────────────────────

    public Optional<String> property(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    // ── Behavioral queries — agnostic to specific compound ───────────────────

    public boolean isHighlySoluble() {
        return solubility.isHighlySoluble();
    }

    /**
     * EC contribution per gram dissolved in one liter.
     * All soluble salts increase EC. Computed from molecular weight
     * and ion count — agnostic to specific compound.
     */
    public BigDecimal ecContributionPerGramPerLiter() {
        return solubility.ecContributionFactor();
    }

    public boolean isSafe() {
        return safety == null || safety.hazardLevel() == SafetyProfile.HazardLevel.NONE;
    }

    public boolean isVolatile() {
        return volatilization != null;
    }

    public boolean isFumigant() {
        return compoundInfo.type() == CompoundType.VOLATILE_ORGANIC && volatilization != null;
    }

    public boolean isChelate() {
        return compoundInfo.type() == CompoundType.CHELATE;
    }

    /**
     * Is this compound safe to apply at the given temperature?
     * Delegates to SafetyProfile if present.
     * If no safety profile defined, compound is considered safe.
     */
    public boolean isSafeAtTemperature(TemperatureFahrenheit temperature) {
        return safety == null || safety.isWithinTemperatureWindow(temperature);
    }

    /**
     * Is this compound effective at the given temperature?
     * Relevant for fumigants (thymol, formic acid).
     * Non-fumigants are always effective (temperature-independent).
     */
    public boolean isEffectiveAtTemperature(TemperatureFahrenheit temperature) {
        if (volatilization == null) return true;
        return volatilization.isEffectiveAt(temperature);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notBlank(commonName, "commonName")
                .valueObject(this, Compound::compoundInfo, "compoundInfo")
                .valueObject(this, Compound::solubility, "solubility")
                .valueObject(this, Compound::bioavailability, "bioavailability")
                .valueObjectOrNull(this, Compound::volatilization, "volatilization")
                .valueObjectOrNull(this, Compound::safety, "safety")
                .notNull(this, Compound::properties, "properties");
    }
}
