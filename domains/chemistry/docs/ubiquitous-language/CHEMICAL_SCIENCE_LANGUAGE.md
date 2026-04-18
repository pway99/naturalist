# ChemicalScience Ubiquitous Language

## Oak Vista Garden Management System

## Addendum to UBIQUITOUS_LANGUAGE.md

---

## ChemicalScience Module Terms

### Element

An atomic building block with defined symbol, atomic weight, and ionic form.
Elements are the constituent parts of compounds.
Examples in Oak Vista context: Ca, Mg, K, N, S, P, Cl, O, H, C.

### Compound

A chemical entity composed of elements in defined proportions.
Has formula, molecular weight, solubility, and physical properties.
The Java Compound class is agnostic — specific compounds are JSON catalog entries.
Examples: CaSO4·2H2O (gypsum), HCOOH (formic acid), C10H14O (thymol).

### CompoundId

A strongly typed identifier for a compound in the catalog.
Used by domain modules (Soil, Insects) to reference compounds
without depending on specific compound classes.
Example: CompoundId.of("calcium-sulfate-dihydrate")

### Compound Catalog

The complete registry of chemical compounds known to the system.
Defined in JSON — not in Java code.
Loaded at startup by CompoundCatalogLoader into CompoundRegistry.
New compounds added by editing JSON — zero code change required.

### Compound Type

Classification of a compound by its primary chemical character.
One of: INORGANIC_SALT, ORGANIC_ACID, MINERAL, ELEMENT,
CHELATE, BIOLOGICAL_COMPOUND, VOLATILE_ORGANIC.

### Solubility Profile

The water solubility characteristics of a compound at standard conditions.
Expressed as grams per liter at 20°C.
Relevant to dissolution rate in soil — highly soluble compounds
(K2SO4, KNO3) act faster than sparingly soluble (CaSO4, CaCO3).

### pH Character

Whether a compound is acidic, basic, or neutral in solution.
Formic acid: strongly acidic. Calcium carbonate: alkaline.
Gypsum: neutral. Critical for predicting soil pH effects.

### Reaction

A documented chemical transformation between compounds.
Has reactants, products, conditions (temperature, pH, biological catalyst),
and significance to the Oak Vista management context.
Reactions are catalog entries — defined in reactions.json.

### Cation Exchange

The displacement of one cation from a soil exchange site by another.
Governed by selectivity series and mass action law.
In Oak Vista context: Ca2+ displacing K+ and Na+ from coco coir sites.
Modeled as a Reaction with specific conditions and equilibrium constants.

### Bioavailability Profile

How a compound is absorbed by a biological system.
Different profiles for: root uptake, foliar stomatal absorption,
foliar cuticular absorption, fumigant volatilization.
A compound may have multiple bioavailability profiles.

### Chelation

The binding of a metal ion (Ca2+, Mg2+) by an organic acid molecule
forming a stable complex that resists precipitation and improves absorption.
Chelated compounds penetrate waxy leaf cuticle more effectively than ionic salts.
TPS CalMag OAC uses organic acid chelation — higher foliar bioavailability
than Rot-Stop calcium chloride.

### Volatilization Profile

For compounds that act as vapor-phase treatments (formic acid, thymol).
Describes the relationship between temperature and vapor production rate.
Critical for Varroa treatment efficacy and safety.
Formic acid: dangerously high vapor pressure above 85°F in Chico summers.
Thymol: insufficient volatilization below 59°F; effective 59-105°F.

### Safety Profile

The concentration limits, temperature windows, and exposure constraints
for safe application of a compound.
Includes: hazard level, maximum safe concentration, application temperature
window, protective equipment requirements, bee safety status.

### Temperature Window

The range of ambient temperatures within which a compound is both
safe and effective for its intended application.
Compounds outside their temperature window are either ineffective
(too cold — thymol) or dangerous (too hot — formic acid).

### EC Contribution

The increase in electrical conductivity (dS/m) that a dissolved compound
adds to soil solution. All soluble salts contribute to EC.
Relevant for: K2SO4, CaCl2, MgSO4, NH4+ from blood meal.
Summed across all amendments to predict total EC impact.

### Hazard Level

Classification of a compound's risk to applicators and non-target organisms.
One of: NONE, LOW, MODERATE, HIGH, EXTREME.
Formic acid: HIGH (corrosive vapor, skin burns).
Oxalic acid: MODERATE (irritant, harmful if ingested).
Gypsum: NONE (inert, food-safe).
Neem oil: LOW (mild irritant).

---

## Compound Catalog — Oak Vista Instances

These are the specific compounds discovered during this project.
Defined in JSON files — not in Java code.

| CompoundId                | Common Name       | Formula    | Primary Use            |
|---------------------------|-------------------|------------|------------------------|
| calcium-sulfate-dihydrate | Garden Gypsum     | CaSO4·2H2O | Soil Ca rehabilitation |
| calcium-chloride          | Rot-Stop active   | CaCl2      | Foliar Ca delivery     |
| potassium-sulfate         | K2SO4 amendment   | K2SO4      | Potassium correction   |
| elemental-sulfur          | Soil Sulfur       | S          | Sulfate source, pH mod |
| magnesium-sulfate         | Epsom Salt        | MgSO4·7H2O | Mg foliar/drench       |
| calcium-carbonate         | Limestone         | CaCO3      | Native soil buffer     |
| calcium-pectate           | Cell wall Ca      | Ca-pectate | BER mechanism          |
| formic-acid               | Varroa treatment  | HCOOH      | Mite fumigant          |
| oxalic-acid               | Varroa treatment  | H2C2O4     | Mite contact/vapor     |
| thymol                    | Apiguard active   | C10H14O    | Mite fumigant          |
| azadirachtin              | Neem active       | C35H44O16  | Thrips/mite disruption |
| potassium-fatty-acids     | Insecticidal soap | K-RCOOH    | Contact insecticide    |
| organic-acid-chelate      | TPS OAC chelate   | generic    | Enhanced Ca/Mg uptake  |
