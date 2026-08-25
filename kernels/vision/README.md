# vision

A vendor-neutral port for LLM-backed image identification. Domain code calls the
interface; the concrete model lives behind an adapter, so the core carries no
knowledge of any provider.

---

## What it provides

**`VisionService`** — the port. A single method, `identify(image, tool,
systemPrompt)`, runs one identification turn and returns a `VisionExchange`. The
kernel defines the seam only; the Anthropic implementation lives in
[`adapters/anthropic-vision`](../../adapters/anthropic-vision/), following the
same thin-kernel-port precedent as the `Resilience` facade. A
`NoOpVisionService` ships in the kernel for unit tests and unwired composition
roots.

**`VisionExchange`** — a tool-use conversation with the model. `result()` is the
tool call from the latest turn; `respond(toolResultJson, nextTool)` sends a tool
result back and returns the next turn. This lets a caller run a second turn — for
example, hand the model a set of similar existing features and ask it to resolve
reuse-or-new — without the kernel knowing the provider's conversation mechanics.

**Supporting value objects.** `Image` (bytes, media type, and `ImageMetadata`),
`ToolSchema` (a tool's name, description, and JSON parameter schema), and
`ToolResult` (the tool name and its JSON arguments) — the vendor-neutral shapes
that cross the port. Every one declares its own invariants.

Carrying no domain vocabulary, the port describes identification structurally as
tool use; what an identification *means* — a species, a rank — belongs to the
calling domain.

---

## Learn more

- [`docs/briefings/vision-identification.md`](../../docs/briefings/vision-identification.md) — the full identification pipeline, from photo to catalog entry.
- [ADR-026 — Resilience as a first-order concern](../../docs/adr/ADR-026-resilience-first-order-concern.md) — the port-behind-a-facade precedent this follows.
