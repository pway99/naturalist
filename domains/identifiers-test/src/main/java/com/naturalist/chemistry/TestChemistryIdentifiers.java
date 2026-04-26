package com.naturalist.chemistry;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.chemistry.compound.DepictionId;
import com.naturalist.chemistry.element.ElementName;
import com.naturalist.chemistry.product.ProductName;

import java.util.UUID;

/**
 * Hardcoded EntityName constants for deterministic chemistry repository test authoring.
 * <p>
 * Class structure mirrors the domain object graph: each compound owns an {@code Elements}
 * inner class containing only the elements that constitute it. The top-level {@code Elements}
 * class is the single source of truth for all element name constants — compound inner classes
 * reference these rather than re-declaring them.
 * <p>
 * Every entity type defines at least two known names (to enable single-entity and
 * set-based lookup tests) and a {@code NotFound} inner class containing a fictitious name
 * that is guaranteed never to appear in any JSON catalog (for empty-result assertions).
 * <p>
 * Usage in contract tests:
 * <pre>
 *     repository().getByName(TestChemistryIdentifiers.Compounds.PotassiumSulfate.name)
 *     repository().getByName(TestChemistryIdentifiers.Compounds.NotFound.name)
 *     repository().getByName(TestChemistryIdentifiers.Elements.K)
 *     repository().getByName(TestChemistryIdentifiers.Elements.NotFound.name)
 *     repository().getByName(TestChemistryIdentifiers.Compounds.CalciumSulfateDihydrate.depictionName)
 *     repository().getByName(TestChemistryIdentifiers.Compounds.NotFound.depictionName)
 * </pre>
 */
public class TestChemistryIdentifiers {

    private TestChemistryIdentifiers() {}

    // -------------------------------------------------------------------------
    // Elements — single source of truth for all element name constants
    // -------------------------------------------------------------------------

    public static class Elements {

        private Elements() {}

        public static final ElementName C  = ElementName.of("carbon");
        public static final ElementName Ca = ElementName.of("calcium");
        public static final ElementName Cl = ElementName.of("chlorine");
        public static final ElementName H  = ElementName.of("hydrogen");
        public static final ElementName K  = ElementName.of("potassium");
        public static final ElementName Mg = ElementName.of("magnesium");
        public static final ElementName O  = ElementName.of("oxygen");
        public static final ElementName S  = ElementName.of("sulfur");

        /** Fictitious element name — guaranteed absent from the catalog. */
        public static class NotFound {
            public static final ElementName name = ElementName.of("unobtainium");
        }
    }

    // -------------------------------------------------------------------------
    // Compounds — each compound owns its constituent Elements
    // -------------------------------------------------------------------------

    public static class Compounds {

        private Compounds() {}

        /** Fictitious identifiers for all entity types within this scope — guaranteed absent from any catalog. */
        public static class NotFound {
            public static final CompoundName name = CompoundName.of("unobtainium-oxide");
            public static final String commonName = "Unobtainium Oxide";
            public static final DepictionId depictionName = DepictionId.of(
                    UUID.fromString("01970000-0001-7001-8001-0000000000ff"));
        }

        public static class CalciumSulfateDihydrate {
            public static final CompoundName name = CompoundName.of("calcium-sulfate-dihydrate");
            public static final String commonName = "Calcium Sulfate Dihydrate";
            public static final DepictionId depictionName = DepictionId.of(
                    UUID.fromString("01970000-0001-7001-8001-000000000001"));

            public static class Elements {
                public static final ElementName Ca = TestChemistryIdentifiers.Elements.Ca;
                public static final ElementName S  = TestChemistryIdentifiers.Elements.S;
                public static final ElementName O  = TestChemistryIdentifiers.Elements.O;
                public static final ElementName H  = TestChemistryIdentifiers.Elements.H;
            }
        }

        public static class CalciumChloride {
            public static final CompoundName name = CompoundName.of("calcium-chloride");
            public static final String commonName = "Calcium Chloride";

            public static class Elements {
                public static final ElementName Ca = TestChemistryIdentifiers.Elements.Ca;
                public static final ElementName Cl = TestChemistryIdentifiers.Elements.Cl;
            }
        }

        public static class PotassiumSulfate {
            public static final CompoundName name = CompoundName.of("potassium-sulfate");
            public static final String commonName = "Potassium Sulfate";

            public static class Elements {
                public static final ElementName K = TestChemistryIdentifiers.Elements.K;
                public static final ElementName S = TestChemistryIdentifiers.Elements.S;
                public static final ElementName O = TestChemistryIdentifiers.Elements.O;
            }
        }

        public static class ElementalSulfur {
            public static final CompoundName name = CompoundName.of("elemental-sulfur");
            public static final String commonName = "Elemental Sulfur";

            public static class Elements {
                public static final ElementName S = TestChemistryIdentifiers.Elements.S;
            }
        }

        public static class MagnesiumSulfate {
            public static final CompoundName name = CompoundName.of("magnesium-sulfate");
            public static final String commonName = "Magnesium Sulfate Heptahydrate";

            public static class Elements {
                public static final ElementName Mg = TestChemistryIdentifiers.Elements.Mg;
                public static final ElementName S  = TestChemistryIdentifiers.Elements.S;
                public static final ElementName O  = TestChemistryIdentifiers.Elements.O;
                public static final ElementName H  = TestChemistryIdentifiers.Elements.H;
            }
        }

        public static class CalciumCarbonate {
            public static final CompoundName name = CompoundName.of("calcium-carbonate");
            public static final String commonName = "Calcium Carbonate";

            public static class Elements {
                public static final ElementName Ca = TestChemistryIdentifiers.Elements.Ca;
                public static final ElementName C  = TestChemistryIdentifiers.Elements.C;
                public static final ElementName O  = TestChemistryIdentifiers.Elements.O;
            }
        }

        public static class CalciumPectate {
            public static final CompoundName name = CompoundName.of("calcium-pectate");
            public static final String commonName = "Calcium Pectate";

            public static class Elements {
                public static final ElementName Ca = TestChemistryIdentifiers.Elements.Ca;
                public static final ElementName C  = TestChemistryIdentifiers.Elements.C;
                public static final ElementName O  = TestChemistryIdentifiers.Elements.O;
                public static final ElementName H  = TestChemistryIdentifiers.Elements.H;
            }
        }

        public static class OrganicAcidChelate {
            public static final CompoundName name = CompoundName.of("organic-acid-chelate");
            public static final String commonName = "Organic Acid Chelated Calcium-Magnesium";

            public static class Elements {
                public static final ElementName Ca = TestChemistryIdentifiers.Elements.Ca;
                public static final ElementName Mg = TestChemistryIdentifiers.Elements.Mg;
                public static final ElementName C  = TestChemistryIdentifiers.Elements.C;
                public static final ElementName O  = TestChemistryIdentifiers.Elements.O;
                public static final ElementName H  = TestChemistryIdentifiers.Elements.H;
            }
        }

        public static class FormicAcid {
            public static final CompoundName name = CompoundName.of("formic-acid");
            public static final String commonName = "Formic Acid";
            public static final DepictionId depictionName = DepictionId.of(
                    UUID.fromString("01970000-0001-7001-8001-000000000002"));

            public static class Elements {
                public static final ElementName H = TestChemistryIdentifiers.Elements.H;
                public static final ElementName C = TestChemistryIdentifiers.Elements.C;
                public static final ElementName O = TestChemistryIdentifiers.Elements.O;
            }
        }

        public static class OxalicAcid {
            public static final CompoundName name = CompoundName.of("oxalic-acid");
            public static final String commonName = "Oxalic Acid";

            public static class Elements {
                public static final ElementName H = TestChemistryIdentifiers.Elements.H;
                public static final ElementName C = TestChemistryIdentifiers.Elements.C;
                public static final ElementName O = TestChemistryIdentifiers.Elements.O;
            }
        }

        public static class Thymol {
            public static final CompoundName name = CompoundName.of("thymol");
            public static final String commonName = "Thymol";

            public static class Elements {
                public static final ElementName C = TestChemistryIdentifiers.Elements.C;
                public static final ElementName H = TestChemistryIdentifiers.Elements.H;
                public static final ElementName O = TestChemistryIdentifiers.Elements.O;
            }
        }

        public static class Azadirachtin {
            public static final CompoundName name = CompoundName.of("azadirachtin");
            public static final String commonName = "Azadirachtin";

            public static class Elements {
                public static final ElementName C = TestChemistryIdentifiers.Elements.C;
                public static final ElementName H = TestChemistryIdentifiers.Elements.H;
                public static final ElementName O = TestChemistryIdentifiers.Elements.O;
            }
        }

        public static class PotassiumFattyAcids {
            public static final CompoundName name = CompoundName.of("potassium-fatty-acids");
            public static final String commonName = "Potassium Salts of Fatty Acids";

            public static class Elements {
                public static final ElementName K = TestChemistryIdentifiers.Elements.K;
                public static final ElementName C = TestChemistryIdentifiers.Elements.C;
                public static final ElementName O = TestChemistryIdentifiers.Elements.O;
                public static final ElementName H = TestChemistryIdentifiers.Elements.H;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Products — commercial formulations referencing one or more compounds
    // -------------------------------------------------------------------------

    public static class Products {

        private Products() {}

        /** Fictitious identifier — guaranteed absent from any catalog. */
        public static class NotFound {
            public static final ProductName name = ProductName.of("unobtainium-rtu");
            public static final String displayName = "Unobtainium RTU";
        }

        public static class Apiguard {
            public static final ProductName name = ProductName.of("apiguard");
            public static final String displayName = "Apiguard (Véto-pharma)";
        }

        public static class TpsCalmagOac {
            public static final ProductName name = ProductName.of("tps-calmag-oac");
            public static final String displayName = "TPS Nutrients CalMag OAC";
        }

        public static class BonideRotStopRtu {
            public static final ProductName name = ProductName.of("bonide-rot-stop-rtu");
            public static final String displayName = "Bonide Rot-Stop RTU";
        }

        public static class DownToEarth_0_0_50 {
            public static final ProductName name = ProductName.of("down-to-earth-0-0-50");
            public static final String displayName = "Down to Earth 0-0-50";
        }

        public static class EbStoneSoilSulfurMinerals {
            public static final ProductName name = ProductName.of("eb-stone-soil-sulfur-minerals");
            public static final String displayName = "E.B. Stone Soil Sulfur Minerals";
        }
    }
}
