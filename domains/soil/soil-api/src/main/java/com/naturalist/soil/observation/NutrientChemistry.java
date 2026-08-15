package com.naturalist.soil.observation;

import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * What substance a soil nutrient row measures, and in what form the lab prints it —
 * soil's outbound reference into the chemistry catalog.
 * <p>
 * <b>Why soil owns this.</b> The referencing domain owns its outbound reference, as
 * {@code PhytochemicalConstituent.compoundName} does in plants. Chemistry knows what
 * nitrogen is; only soil knows that its {@code nitrate-n} row measures nitrogen.
 * <p>
 * {@code substance} is typed {@link EntityName} rather than a concrete subclass because
 * a nutrient may reference either an element ({@code ElementName}) or a compound
 * ({@code CompoundName}). The console resolves whichever it is through the catalog seam
 * and never switches on the type; the chemistry console's linker already dispatches on
 * the concrete class. Every current nutrient references an element.
 * <p>
 * Carries its own {@code nutrient} key so the value travels without losing its subject —
 * a consumer asking "which nutrients reference this substance?" can filter a stream of
 * these, which a bare map value could not answer.
 *
 * @param nutrient     the soil nutrient row, e.g. {@code nitrate-n}
 * @param substance    the chemistry entity it measures, e.g. {@code nitrogen}
 * @param reportedForm how the lab prints the value relative to that substance
 */
public record NutrientChemistry(
        NutrientName nutrient,
        EntityName substance,
        ReportedForm reportedForm
) implements ValueObject {

    /** The chemistry a nutrient row references. */
    public static NutrientChemistry of(NutrientName nutrient,
                                       EntityName substance,
                                       ReportedForm reportedForm) {
        return new NutrientChemistry(nutrient, substance, reportedForm);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(nutrient, "nutrient")
                .entityName(substance, "substance")
                .notNull(reportedForm, "reportedForm");
    }
}
