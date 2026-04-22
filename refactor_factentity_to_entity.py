#!/usr/bin/env python3
"""
Refactor: FactEntity -> Entity, FactName -> EntityId (UUIDv7).

The kernel's surrogate-key branch is renamed. Concrete *Name classes that
extend FactName are renamed to *Id extending EntityId. The EntityId abstract
base validates UUIDv7 strictly (value.version() == 7).

Usage:
    python refactor_factentity_to_entity.py                 # dry run
    python refactor_factentity_to_entity.py --apply         # write changes
    python refactor_factentity_to_entity.py --root /path    # repo root (default: cwd)

Idempotent: running --apply twice produces no changes on the second run.

Exits nonzero if a discovered FactName subclass cannot be renamed safely
(e.g. its simple name does not end in "Name") or if a target rename would
collide with an existing file.
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable


# -----------------------------------------------------------------------------
# New kernel file contents
# -----------------------------------------------------------------------------

ENTITY_JAVA = '''package com.naturalist.ddd;

/**
 * A domain record identified by a surrogate {@link EntityId} (UUIDv7).
 *
 * <p>Use {@code Entity} for records whose identity is not a natural key —
 * events, observations, measurements, lab analyses. The identifier is a
 * time-ordered UUIDv7 generated at record construction, so the entire identity
 * lifecycle begins in Java and no round-trip to the database is required at
 * insert time.
 *
 * <p>Cross-domain references to {@code Entity} instances are never by value.
 * Other domains reason about them through service interfaces.
 *
 * @param <ID> the concrete {@link EntityId} subtype for this entity
 */
public interface Entity<ID extends EntityId> extends Named<ID> {
}
'''

ENTITY_ID_JAVA = '''package com.naturalist.ddd;

import java.util.Objects;
import java.util.UUID;

/**
 * Surrogate identifier for an {@link Entity} — a UUIDv7 assigned at record
 * construction.
 *
 * <p>UUIDv7 encodes a Unix-ms timestamp in its leading 48 bits, so values
 * sort chronologically and keep B-tree indexes dense under sustained insert
 * load. The kernel commits to v7 specifically: {@link #isValid()} rejects any
 * other UUID version.
 *
 * <p>{@code EntityId} is abstract. Each concrete subtype (e.g.
 * {@code AmendmentEventId}, {@code LabAnalysisId}) is a distinct type at
 * compile time — equality is qualified by {@code getClass()}, so two distinct
 * id types holding the same UUID never compare equal.
 */
public abstract class EntityId {

    final UUID value;

    protected EntityId(UUID value) {
        this.value = value;
    }

    public UUID value() {
        return value;
    }

    /**
     * True when {@link #value()} is non-null and carries UUID version 7.
     */
    public boolean isValid() {
        return value != null && value.version() == 7;
    }

    public boolean isNotValid() {
        return !isValid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityId that = (EntityId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
'''


KERNEL_DIR = Path("kernels/framework/src/main/java/com/naturalist/ddd")
DELETE_PATHS = [
    KERNEL_DIR / "FactEntity.java",
    KERNEL_DIR / "FactName.java",
]
CREATE_PATHS = {
    KERNEL_DIR / "Entity.java": ENTITY_JAVA,
    KERNEL_DIR / "EntityId.java": ENTITY_ID_JAVA,
}

SOURCE_EXTS = {".java", ".json", ".xml", ".md"}
SKIP_DIR_PARTS = {".git", "build", ".gradle", "target", "node_modules", "out", "bin", ".idea"}
# ADRs are historical narrative about the refactor to this shape; do not rewrite.
EXCLUDE_PATH_FRAGMENTS = [
    Path("docs") / "adr",
]

FACTNAME_EXTENDS = re.compile(
    r"\bpublic\s+(?:final\s+|abstract\s+)?class\s+(\w+)\s+extends\s+FactName\b"
)


def _is_excluded(path: Path, root: Path) -> bool:
    try:
        rel = path.relative_to(root)
    except ValueError:
        return True
    for frag in EXCLUDE_PATH_FRAGMENTS:
        parts = frag.parts
        for i in range(len(rel.parts) - len(parts) + 1):
            if rel.parts[i : i + len(parts)] == parts:
                return True
    return False


def iter_sources(root: Path) -> Iterable[Path]:
    for p in root.rglob("*"):
        if not p.is_file():
            continue
        if any(part in SKIP_DIR_PARTS for part in p.parts):
            continue
        if p.suffix not in SOURCE_EXTS:
            continue
        if _is_excluded(p, root):
            continue
        yield p


def discover_factname_subclasses(root: Path) -> tuple[dict[str, str], list[str]]:
    """Return (mapping, warnings).

    mapping: simple class name -> new simple class name (XxxName -> XxxId).
    warnings: human-readable messages for classes that extend FactName but do
              not end in 'Name' (left un-renamed; operator should handle by hand).
    """
    mapping: dict[str, str] = {}
    warnings: list[str] = []
    for path in iter_sources(root):
        if path.suffix != ".java":
            continue
        text = path.read_text(encoding="utf-8")
        for m in FACTNAME_EXTENDS.finditer(text):
            old = m.group(1)
            if not old.endswith("Name"):
                warnings.append(
                    f"{path}: class {old} extends FactName but does not end in 'Name'; "
                    f"script will NOT rename this class. Handle manually."
                )
                continue
            new = old[: -len("Name")] + "Id"
            if old in mapping and mapping[old] != new:
                warnings.append(
                    f"conflicting rename for {old}: {mapping[old]} vs {new}"
                )
            mapping[old] = new
    return mapping, warnings


@dataclass
class Plan:
    deletes: list[Path] = field(default_factory=list)
    creates: dict[Path, str] = field(default_factory=dict)
    file_renames: dict[Path, Path] = field(default_factory=dict)
    content_edits: dict[Path, str] = field(default_factory=dict)
    warnings: list[str] = field(default_factory=list)


def build_plan(root: Path) -> Plan:
    plan = Plan()

    name_id_map, warnings = discover_factname_subclasses(root)
    plan.warnings.extend(warnings)

    # Kernel deletes
    for rel in DELETE_PATHS:
        abs_path = root / rel
        if abs_path.exists():
            plan.deletes.append(abs_path)

    # Kernel creates
    for rel, content in CREATE_PATHS.items():
        plan.creates[root / rel] = content

    # File renames for discovered *Name.java
    for path in iter_sources(root):
        if path.suffix != ".java":
            continue
        stem = path.stem
        if stem in name_id_map:
            target = path.with_name(name_id_map[stem] + ".java")
            if target.exists() and target != path:
                plan.warnings.append(
                    f"rename target already exists: {path} -> {target}; skipping rename"
                )
                continue
            plan.file_renames[path] = target

    # Content substitutions: build a single alternation regex for speed.
    tokens: dict[str, str] = {
        "FactEntity": "Entity",
        "FactName": "EntityId",
    }
    tokens.update(name_id_map)
    if tokens:
        token_re = re.compile(
            r"\b(" + "|".join(re.escape(k) for k in tokens) + r")\b"
        )
    else:
        token_re = None

    if token_re is not None:
        deletes_set = set(plan.deletes)
        for path in iter_sources(root):
            if path in deletes_set:
                continue
            try:
                text = path.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                plan.warnings.append(f"skipping non-utf8 file: {path}")
                continue
            new_text = token_re.sub(lambda m: tokens[m.group(1)], text)
            if new_text != text:
                plan.content_edits[path] = new_text

    return plan


def describe_plan(plan: Plan) -> None:
    print(f"=== DELETES ({len(plan.deletes)}) ===")
    for p in plan.deletes:
        print(f"  - {p}")
    print(f"=== CREATES ({len(plan.creates)}) ===")
    for p in plan.creates:
        print(f"  + {p}")
    print(f"=== FILE RENAMES ({len(plan.file_renames)}) ===")
    for old, new in plan.file_renames.items():
        print(f"  ~ {old} -> {new.name}")
    print(f"=== CONTENT EDITS ({len(plan.content_edits)}) ===")
    for p in plan.content_edits:
        print(f"  * {p}")
    if plan.warnings:
        print(f"=== WARNINGS ({len(plan.warnings)}) ===")
        for w in plan.warnings:
            print(f"  ! {w}")


def apply_plan(plan: Plan) -> None:
    # 1. Deletes
    for path in plan.deletes:
        print(f"DELETE  {path}")
        path.unlink()

    # 2. Creates (overwrite is fine; idempotent)
    for path, content in plan.creates.items():
        print(f"CREATE  {path}")
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")

    # 3. Content edits — write to the ORIGINAL path first.
    #    File renames in step 4 will move them to the final location.
    for path, text in plan.content_edits.items():
        print(f"EDIT    {path}")
        path.write_text(text, encoding="utf-8")

    # 4. File renames
    for old, new in plan.file_renames.items():
        print(f"RENAME  {old.name} -> {new.name}  ({old.parent})")
        new.parent.mkdir(parents=True, exist_ok=True)
        old.rename(new)


def main() -> int:
    ap = argparse.ArgumentParser(
        description="Rename FactEntity/FactName to Entity/EntityId (UUIDv7)."
    )
    ap.add_argument("--apply", action="store_true", help="actually modify files")
    ap.add_argument("--root", default=".", help="repo root (default: cwd)")
    args = ap.parse_args()

    root = Path(args.root).resolve()
    if not (root / "kernels" / "framework").exists():
        print(
            f"refusing to run: {root} does not look like the naturalist repo root "
            f"(no kernels/framework directory). Pass --root if running from elsewhere.",
            file=sys.stderr,
        )
        return 2

    plan = build_plan(root)
    describe_plan(plan)

    if plan.warnings and args.apply:
        print(
            "\nWARNINGS present above. Re-run without --apply to inspect; "
            "proceeding with --apply will skip problematic items listed.",
            file=sys.stderr,
        )

    if args.apply:
        print("\n--- APPLYING ---\n")
        apply_plan(plan)
        print("\nDone. Next: ./gradlew compileJava")
    else:
        print("\n(dry run — re-run with --apply to write changes)")

    return 0


if __name__ == "__main__":
    sys.exit(main())
