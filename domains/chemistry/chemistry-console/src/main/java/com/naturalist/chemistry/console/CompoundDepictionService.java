package com.naturalist.chemistry.console;

import com.naturalist.chemistry.compound.CompoundName;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders compound 2D structural depictions as SVG using the CDK.
 * <p>
 * Each entry pairs a SMILES string with a depiction note: discrete organic
 * molecules render their canonical structure; ionic salts render the
 * formula unit as discrete ions; and class-label catalog entries
 * (organic-acid-chelate, potassium-fatty-acids, calcium-pectate) render a
 * representative stand-in molecule and the note flags this.
 */
@Service
class CompoundDepictionService {

    record Depiction(String smiles, String note) {}

    private static final Map<String, Depiction> DEPICTIONS_BY_SLUG = Map.ofEntries(
            // Organic — exact canonical structure
            Map.entry("aristolochic-acid-i", new Depiction(
                    "COc1cc2ccc3c(C(=O)O)c4cc5OCOc5cc4c([N+]([O-])=O)c3c2cc1",
                    "Phenanthrene-derived nitro acid; the methoxy at C-8 distinguishes it from aristolochic acid II.")),
            Map.entry("aristolochic-acid-ii", new Depiction(
                    "O=C(O)c1c2cc3OCOc3cc2c([N+]([O-])=O)c2ccc3ccccc3c12",
                    "Demethoxylated congener of aristolochic acid I; the nitro group at C-6 is shared with AA-I.")),
            Map.entry("formic-acid", new Depiction(
                    "OC=O",
                    "Simplest carboxylic acid (HCOOH) — the active fumigant used against Varroa destructor.")),
            Map.entry("oxalic-acid", new Depiction(
                    "OC(=O)C(=O)O",
                    "Smallest dicarboxylic acid; an effective Varroa miticide applied by dribble or vapor.")),
            Map.entry("thymol", new Depiction(
                    "Cc1ccc(C(C)C)c(O)c1",
                    "Monoterpenoid phenol from Thymus vulgaris essential oil.")),
            Map.entry("azadirachtin", new Depiction(
                    "CC(=O)OC1C(O)C2(COC(=O)C)C(C=C)OC3(O)C24OC4C(C(=O)OC)C13C",
                    "Simplified depiction of the tetranortriterpenoid limonoid core from Azadirachta indica seeds; the full natural product is significantly more elaborate.")),
            // Ionic salts — formula unit shown as discrete ions
            Map.entry("calcium-sulfate-dihydrate", new Depiction(
                    "[Ca+2].[O-]S(=O)(=O)[O-].O.O",
                    "Ionic depiction — Ca²⁺, the sulfate dianion, and two waters of crystallization (gypsum).")),
            Map.entry("calcium-chloride", new Depiction(
                    "[Ca+2].[Cl-].[Cl-]",
                    "Ionic compound; Ca²⁺ paired with two chloride anions per formula unit.")),
            Map.entry("potassium-sulfate", new Depiction(
                    "[K+].[K+].[O-]S(=O)(=O)[O-]",
                    "Ionic; two K⁺ balancing the sulfate dianion.")),
            Map.entry("elemental-sulfur", new Depiction(
                    "S1SSSSSSS1",
                    "S₈ — the crown-shaped ring allotrope, the stable form of elemental sulfur at room temperature.")),
            Map.entry("magnesium-sulfate", new Depiction(
                    "[Mg+2].[O-]S(=O)(=O)[O-]",
                    "Ionic; Mg²⁺ paired with the sulfate dianion (anhydrous form shown).")),
            Map.entry("calcium-carbonate", new Depiction(
                    "[Ca+2].[O-]C(=O)[O-]",
                    "Ionic; Ca²⁺ paired with the carbonate dianion.")),
            // Class labels — representative stand-in molecule
            Map.entry("calcium-pectate", new Depiction(
                    "OC1OC(C(=O)O)C(O)C(O)C1O",
                    "Stand-in: D-galacturonic acid, the monomer unit. Pectate in soil is a Ca²⁺-bridged polymer of α-1,4-linked galacturonate residues.")),
            Map.entry("organic-acid-chelate", new Depiction(
                    "OC(=O)CC(O)(C(=O)O)CC(=O)O",
                    "Stand-in: citric acid — representative of the polycarboxylic acids used in TPS CalMag OAC to chelate Ca²⁺ and Mg²⁺ for foliar uptake.")),
            Map.entry("potassium-fatty-acids", new Depiction(
                    "CCCCCCCC/C=C\\CCCCCCCC(=O)[O-].[K+]",
                    "Stand-in: potassium oleate — a typical long-chain fatty-acid potassium soap component."))
    );

    private final SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
    private final DepictionGenerator depictionGenerator = new DepictionGenerator()
            .withSize(900, 700)
            .withAtomColors()
            .withZoom(2.5)
            .withFillToFit();
    private final Map<String, String> svgCache = new ConcurrentHashMap<>();

    Optional<String> depictAsSvg(CompoundName name) {
        Depiction depiction = DEPICTIONS_BY_SLUG.get(name.value());
        if (depiction == null) {
            return Optional.empty();
        }
        return Optional.of(svgCache.computeIfAbsent(name.value(), key -> renderSvg(depiction.smiles())));
    }

    Optional<String> depictionNote(CompoundName name) {
        Depiction depiction = DEPICTIONS_BY_SLUG.get(name.value());
        return Optional.ofNullable(depiction).map(Depiction::note);
    }

    boolean canDepict(CompoundName name) {
        return DEPICTIONS_BY_SLUG.containsKey(name.value());
    }

    Set<String> depictableSlugs() {
        return DEPICTIONS_BY_SLUG.keySet();
    }

    private String renderSvg(String smiles) {
        try {
            IAtomContainer molecule = smilesParser.parseSmiles(smiles);
            return depictionGenerator.depict(molecule).toSvgStr();
        } catch (CDKException e) {
            throw new IllegalStateException("Failed to render SMILES: " + smiles, e);
        }
    }
}
