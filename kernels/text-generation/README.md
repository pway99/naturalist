# text-generation

A vendor-neutral port for LLM-backed text generation — the text-only sibling of
the `vision` port. Domain code calls the interface; the concrete model lives
behind an adapter.

---

## What it provides

**`TextGenerationService`** — the port. A single method, `generate(tool,
systemPrompt, userPrompt)`, returns a `ToolResult`. It is symmetric with
`VisionService` but takes no image, and it reuses the `vision` kernel's
`ToolSchema` and `ToolResult` value objects rather than redefining them. Used to
generate structured content — four-level Durrell descriptions, diagnostic
features — from textual source material.

The concrete model lives in
[`adapters/anthropic-text-generation`](../../adapters/anthropic-text-generation/);
a `NoOpTextGenerationService` ships in the kernel for tests and unwired
composition roots. As with `vision`, the port is structural — it describes
generation as tool use and holds no domain vocabulary of its own.

---

## Learn more

- [`docs/briefings/vision-identification.md`](../../docs/briefings/vision-identification.md) — the LLM-adapter seam this mirrors.
- [ADR-026 — Resilience as a first-order concern](../../docs/adr/ADR-026-resilience-first-order-concern.md) — the port-behind-a-facade precedent.
