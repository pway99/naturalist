# ChemicalScience Module — Claude Instructions

## Oak Vista Garden Management System

---

## Why ChemicalScience Exists

Chemistry is a cross-cutting scientific domain used by multiple modules:

- Soil: gypsum dissolution, cation exchange, nitrogen mineralization, pH buffering
- Insects: Varroa treatment compounds (formic acid, oxalic acid, thymol)
- Plants: calcium pectate cell wall formation, chelated foliar absorption
- Future modules TBD

Chemistry facts do not belong inside Soil or Insects.
They are scientific truths about compounds and reactions that those
modules reference but do not own.

---

## Critical Design Decision — Model vs Data Separation

### Java classes define BEHAVIOR and SCHEMA — agnostic to specific compounds

### JSON files define INSTANCES — the actual chemical catalog

This means:

- Adding a new compound = add a JSON entry, zero code change
- The compound catalog is configuration, not domain logic
- Java model is reusable across any chemical domain

```
chemical-science/
  compound/        ← Java model classes (schema + behavior)
  reaction/        ← Java model classes
  bioavailability/ ← Java model classes
  safety/          ← Java model classes
  catalog/         ← JSON data files (the actual compounds)
    elements.json
    compounds.json
    reactions.json
    safety-profiles.json
```

---

## Module Position in DAG

```
Identifiers (shared kernel)
     |
     +-------- ChemicalScience (scientific foundation)
     |               |
     +-------- Zones  |
     |                |
     +-------- Soil --+  (depends on ChemicalScience)
     |                |
     +-------- Insects (future) --+  (depends on ChemicalScience)
     |                            |
     +-------- Plants (future) ---+  (depends on ChemicalScience)
```

ChemicalScience depends only on Identifiers.
All domain modules may depend on ChemicalScience.
ChemicalScience never depends on Soil, Insects, or Plants.

---

## What ChemicalScience Owns

**Compounds** — chemical entities with formula, molecular weight,
solubility, pH, and physical properties.

**Elements** — atomic building blocks. Ca, Mg, K, N, S, P etc.
Elements are referenced by compounds.

**Reactions** — how compounds interact.
CaSO4 dissolution. Acid-base neutralization.
Sulfur oxidation. Cation exchange equilibria.

**Bioavailability profiles** — how compounds are absorbed by
biological systems. Stomatal vs cuticular foliar absorption.
Ionic vs chelated root uptake. Volatilization for fumigant treatments.

**Safety profiles** — concentration limits, temperature windows,
exposure thresholds. Formic acid vapor pressure curve.
Thymol effective temperature range. EC toxicity thresholds.

---

## What ChemicalScience Does NOT Own

- Agronomic decisions (apply gypsum to Box 1) → Soil module
- Beekeeping decisions (treat with formic acid in August) → Insects module
- Optimum nutrient ranges for tomatoes → Soil module
- Which treatment to use for Varroa → Insects module

ChemicalScience provides facts. Domain modules make decisions using those facts.

---

## Compound Catalog — Compounds Discovered in This Project

### Soil Chemistry

- Calcium Sulfate Dihydrate (CaSO4·2H2O) — gypsum, primary rehabilitation agent
- Calcium Chloride (CaCl2) — Rot-Stop RTU, fast foliar calcium
- Potassium Sulfate (K2SO4) — potassium correction amendment
- Elemental Sulfur (S) — sulfate source, pH modifier
- Magnesium Sulfate (MgSO4) — Epsom salt, foliar/drench magnesium
- Calcium Carbonate (CaCO3) — limestone, pH buffer in native Chico soil
- Calcium Pectate — cell wall structural compound (BER mechanism)
- Organic Acid Chelate (generic) — TPS OAC chelation mechanism

### Apiary Chemistry

- Formic Acid (HCOOH) — Varroa miticide, temperature-sensitive
- Oxalic Acid (H2C2O4) — Varroa miticide, broodless application
- Thymol (C10H14O) — Apiguard active ingredient, volatilization-dependent

### Pest Management

- Azadirachtin — neem oil active component, thrips/mite disruption
- Potassium Fatty Acids — insecticidal soap, contact kill mechanism

---

## JSON Catalog Files

### elements.json

Atomic elements referenced by compounds.
Fields: symbol, name, atomicWeight, ionicForm, charge

### compounds.json

All chemical compounds in the Oak Vista system.
Fields: id, name, formula, molecularWeight, solubility,
ph, constituentElements, compoundType, properties

### reactions.json

Chemical reactions relevant to Oak Vista management.
Fields: id, name, reactants, products, conditions, significance

### safety-profiles.json

Safety thresholds and application windows.
Fields: compoundId, maxSafeConcentration, temperatureWindow,
applicationConstraints, hazardLevel

---

## Deserialization

CompoundCatalogLoader reads JSON files at startup and populates
the in-memory compound registry. Domain modules query the registry
by compound ID — a string key matching the JSON catalog entry.

CompoundId is a strongly typed identifier from the Identifiers module.

---

## Pending Implementation

1. Element record
2. Compound abstract class + concrete subtypes
3. ReactionProfile value object
4. BioavailabilityProfile value object
5. SafetyProfile value object
6. CompoundRegistry — in-memory store keyed by CompoundId
7. CompoundCatalogLoader — JSON deserializer
8. elements.json — atomic elements
9. compounds.json — all Oak Vista compounds
10. reactions.json — key reactions
11. safety-profiles.json — thresholds and windows
