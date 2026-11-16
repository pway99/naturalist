# Observability standards

**Status:** draft, 2026-11-15. Not yet linked from `CLAUDE.md`.

This document says how naturalist code observes, and why. It is the rubric the code reviewer
applies. [ADR-017](adr/ADR-017-observability-monitoring-and-validation.md) keeps the mechanics:
the `naturalist.invariant.check` meter and its tags, the constraint graph walk, and
`MonitoringMode`. Where this document and older guidance disagree, this document wins.

## Why we observe

The app is resilient:

- A user never sees a 500.
- Invalid input is rejected with a clear refusal.
- The maintainer never reads or monitors logs.

Observation keeps the third promise without breaking the first two. Everything the maintainer
needs to know reaches them as a count on `naturalist.invariant.check`, under a name a reviewer
can open, and alerts fire on those counts. Something that is not counted, the maintainer never
learns about. **The critical rule is that every exception is noticed.**

A count is only useful if it says what happened and where. So the point that observes is the
point that knows the context: the operation that was running, what it needed, and whether it
could carry on. That is why the consumer observes rather than the producer, and why a type that
carries values never throws on construction. An exception from a DTO constructor or
`Enum.valueOf` knows only its own value. It cannot say which operation failed or whether the value
mattered, and without logs nobody can work that out.

## Principles

- **P1. Resilient by default.** No 500s, and the maintainer never needs a log. Every rule below
  serves this.
- **P2. Every exception is noticed at the point that knows what failed and why, once per call
  site.** A double count is one failure noticed twice at the same call site: two `noticeError`
  calls in one handler, or a constraint observation and a `noticeError` for the same exception in
  one handler. A producer that notices a failure it hands on, and a consumer that notices it
  again, is not a double count: each observation names its own layer, and the metrics are read
  together.
- **P3. Bad input from outside is rejected at the edge.** An edge is an inbound adapter: a
  `@RestController` or a message handler. Jackson deserializes the request there, before anything
  is submitted to a core module, so an inbound deserialization error never reaches domain or
  kernel code. The edge turns it into a refusal and notices it.
- **P4. A business rule saying no is a decision.** It is observed under a predicate name at
  `WARN`, before the exception the operation throws.
- **P5. A caught failure is noticed with how it was handled,** using
  `noticeError(name, e, Handling.X)`.
- **P6. Producers hand values on and consumers decide.** Types that carry values (DTOs, records
  read from storage or JSON, enums mapped from strings) hold what they were given and declare
  `invariants()`. A lookup from a string returns null for a value it does not recognise. The
  consumer observes and
  either carries on, when the invalid part is not involved, or throws.
- **P7. Each edge has a backstop.** A last-resort handler at every web and bus edge notices any
  exception nobody noticed and returns a defined response. Each time the backstop fires, an
  observation is missing nearer the cause, and fixing that is a defect to fix at the source.

## How these standards are enforced

If every rule were mechanical, one OpenRewrite recipe would do. Observability needs judgment.
Whether something is a decision, whether a consumer can carry on, and which name explains a
failure are questions a pattern match cannot answer. Enforcement is therefore split into three
kinds, and each rule below is tagged with its kind.

| Kind | Who checks | When |
|---|---|---|
| **Tool** | OpenRewrite recipe or ArchUnit rule | Build. A reviewer does not spend time on these once the tool exists. Until it exists, the reviewer checks it. |
| **Judgment** | The code reviewer (a skill or a configured review agent) | Every change. The reviewer applies the rule's *test* and cites the rule ID with a file and line. |
| **Backstop** | Production (P7) | Runtime. A backstop count is an alert and a defect. |

**The reviewer.** The aim is for the maintainer to review findings, not code. The reviewer:

- reads this document as its rubric and nothing else as authority;
- applies the judgment rules, and the tool rules whose tool does not exist yet, to every changed
  method in full (old lines included) and to callers whose flow the change alters;
- reports each finding as rule ID, file:line, what it saw, and the fix the rule implies;
- marks a finding *uncertain* when the rule's test does not settle it, so the maintainer looks at
  that one;
- reports concerns it meets in existing code outside the change separately, so the maintainer
  is aware, without the author having to fix them;
- never edits code, this document, or the tools.

The author (human or agent) applies the same tests while writing. Each test is worded so that
the author and the reviewer reach the same answer.

The reviewer is the read-only `observability-reviewer` project agent; see *Decided*.

### The reviewer uses the Kaname Engine

Most judgment rules depend on code outside the diff: who calls a changed method, who catches what
it throws, and what a consumer does with what it receives. The reviewer answers those questions
with the Kaname Engine, not grep or whole-file reads. The engine works from resolved symbols, so it
does not confuse same-named methods and does not match comments.

| Rule | Engine question | Tool |
|---|---|---|
| J1, J3, P2 | Who calls this method, and does each call site catch or propagate its failure? How does each handler exit? | `java_failure_handling_census` with `ownerFqn` + `methodName` |
| J1 | Which handlers anywhere would catch this exception type, and does each notice or hand it on? | `java_failure_handling_census` with `exceptionFqn` |
| J3 | Does the `Handling` match the exit? | the census's handler exit: `RETHROWS_CAUGHT` → `RETHROWN`, `THROWS_OTHER` → `WRAPPED`, `RETURNS` / `FALLS_THROUGH` / `LEAVES_LOOP` → `SWALLOWED` |
| J2 | Where is this domain exception thrown? | `jvm_find_type` on the exception, `siteKind: body` |
| J4 | Who consumes this producer's value, and which parts do they use? | `jvm_find_method` for callers, then `java_class_source` with `member` |
| J5 | What does this type declare as valid? | `java_class_source` with `member: invariants` |
| C1 | Where are project enums mapped with `valueOf`? | `jvm_find_method` with `methodName: valueOf` |
| M5, M6, M7 | Any `Objects.requireNonNull`, logger, or `io.micrometer` use in the diff's units? | `jvm_find_method` / `jvm_find_type` on those owners |

The census does not see Observer calls. It shows where a failure is handled and how the handler
exits, and the reviewer then reads that handler with `java_class_source` to check its
`noticeError` or constraint. The census also reports the log level a handler uses, which flags M6.
Each tool's blind spots are in its `cannotSee` field. A finding that rests on one of them is marked
*uncertain*.

## Rules

Each rule has a statement, a **test**, real examples from the repo, and its kind. Examples are
shown with the standard variable names (M13), even where the repo code doesn't use them yet.

### Judgment rules

These rules are where review time goes.

#### J1. Every throw is noticed

**Kind:** Judgment. A recipe can flag candidates (a `throw`, `orElseThrow()` or empty `catch`
with no observation in the same method) but cannot decide them.

**Startup assembly is exempt.** Code that runs only while the application is being wired
(composition roots, `*Assembly.from` factories, registry validation at startup) fails the
deploy if it throws. That failure cannot reach a user as a 500 and cannot go unseen, so J1 does
not ask it to notice exceptions it lets through.

Every exception that leaves or is swallowed by our code is noticed (P2). That happens in
one of three ways:

| The exception is… | Noticed by |
|---|---|
| a refusal of caller input | an `arguments(...).throwWhenInvalid()` constraint (J-rules below, M1) |
| a refusal by a business rule | a decision constraint observed before the throw (J2) |
| a failure our code caught | `noticeError(name, e, Handling.X)` (J3) |
| a failure nobody caught | the edge backstop (P7), which counts it as a defect |

**Test.** For each `throw`, `orElseThrow()`, `catch` and call that can fail: *if this happens a
thousand times tonight, which count goes up, and does its name say what happened?* If no count
goes up, it is a finding. If a count goes up but a layer further out also counts the same
failure, that is also a finding (counted twice).

Right: a caught failure is noticed, then wrapped with its cause.

```java
// FileSecretProvider.lookup
} catch (IOException e) {
    methodObserver.noticeError("readable", e, Handling.WRAPPED);
    throw new IllegalStateException("secret file could not be read: " + path, e);
}
```

Wrong: a refusal of caller input that nothing counts.

```java
// PlantsEntityRefResolver.resolve
if (!PLANTS.value().equals(domain)) {
    throw new IllegalArgumentException("Unknown citation subject domain: " + domain);
}
```

The fix is an `arguments(...)` constraint on `domain` (`isTrue(PLANTS.value().equals(domain),
"isPlantsDomain")`) followed by `throwWhenInvalid()`.

#### J2. Decisions are observed before the refusal

**Kind:** Judgment.

When an operation refuses because a business rule says no, it observes that decision with a
predicate-named `state(...)` at `WARN` before throwing. The thrown exception and its message do
not change. A branch that neither refuses nor fails is not a decision under this rule.

**Test.** *Does this branch end in a refusal the caller will see? If yes, is there a `state(...)`
just before it whose name says the reason?*

Right:

```java
// AccountCommandImpl.register
InvariantObservation decision = methodObserver
        .state(i -> i.isTrue(existing.isEmpty(), "emailUnregistered"))
        .observe(Level.WARN);
if (!decision.violations().isEmpty()) {
    throw new DuplicateEmailException("email already registered");
}
```

**The decider observes.** The method that applies the rule observes it, even when a caller
catches the refusal and translates it. A caller that translates a refusal that was already
observed does not observe it again. That protects every future caller and keeps one count
per refusal, which the budget alerts rely on (this is about decisions, not caught failures; P2's
layering does not apply).

Wrong: `UsageCommandImpl.reserve` throws `BudgetExceededException` when
`cu.used() >= rule.limit()` and observes nothing. Today its caller,
`InsectIdentificationCommandImpl`, catches it and observes `withinBudget` instead, so a new
caller would refuse without anything being counted.

#### J3. A caught failure is noticed, with the matching handling

**Kind:** Judgment. A tool can check that a `catch` contains `noticeError`; whether the
`Handling` and name are right needs a reader.

Every `catch` that handles a failure calls `noticeError(name, e, Handling.X)`, with the `Handling`
matching the catch's next statement:

- `SWALLOWED`: it returns a fallback, skips the step, or does nothing;
- `RETHROWN`: it throws `e` itself, after any cleanup;
- `WRAPPED`: it throws a new exception with `e` as the cause.

A failure the catch turns into a decision (a `WARN` refusal, an `INFO` signal) stays a constraint
instead (J2). Never write `state(s -> s.isTrue(false, "<name>"))` for a caught failure.

**Test.** *Read the catch's last statement. Does the `Handling` describe it? Has an inner layer
already noticed this same exception (then this layer must not)?*

Right:

```java
// InsectIdentificationCommandImpl.reserveStoreAndCatalogue
} catch (RuntimeException e) {
    observer.noticeError("reserveStoreAndCatalogue", "catalogued", e, Handling.RETHROWN);
    try {
        imageStore.delete(SPACE, storedFileName);
    } catch (RuntimeException deleteFailure) {
        e.addSuppressed(deleteFailure);
    }
    throw e;
}
```

**A reply to our own call.** When code parses the reply to a call *we* made (a model's tool
output, a vendor's response), a parse failure means a dependency misbehaved, not that the user
sent bad input. The code that parses the reply notices it here, with `WRAPPED` or `SWALLOWED` by
its exit, and never as a refusal. This is not B2: B2 covers only inbound requests at an edge.

#### J4. The consumer chooses throw or observe

**Kind:** Judgment.

A method that receives or acquires a value chooses the terminal operation by asking *can I, or the
next thing I hand this to, proceed on the invalid value?*

- **No:** `throwWhenInvalid()`. Example: `*Dbo.from(domainObject)` on the write path throws,
  because the INSERT cannot take a bad row.
- **Yes, or it is someone else's call:** `observe(level)` and return it. Example: repository
  reads observe what they read at `WARN` and return it, scoped to the concrete adapter.

A producer never throws on a value only because it is invalid; it does not know whether its
consumers care.

**Test.** *Which parts of the value does this method (or its next consumer) use? Are the invalid
parts among them?*

**A command observes what it builds before handing it on.** An aggregate, entity or value a
command instantiates is observed with its kind's constraint (`aggregate(...)`, `valueObject(...)`,
`observable(...)`, M14) and `throwWhenInvalid()` before it is passed to a transaction, so an
invalid one is refused with a count instead of failing inside the transaction. Place the check
so that anything the command already did (a stored file, a reservation) is undone when it
refuses: inside the `try` that cleans up, or before the side effect. Pass the instance you
observed, not a new one.

#### J5. `invariants()` state the type's real rules

**Kind:** Judgment.

`invariants()` declares what makes an instance valid in the domain, including cross-field rules
(`isTrue(family.belongsToOrder(order), "familyBelongsToOrder")`), not just null checks. A
consumer can only decide (J4) about rules the type declares.

When a change adds a rule about an entity's field (a command now limits `notes` to 2000
characters), the entity's own `invariants()` must declare it too, even though the entity is not
in the diff (`whenNotNull(notes, c -> c.maxLength(notes, MAX_NOTES_LENGTH, "notes"))` on
`OrganismObservation`). The limit lives with the entity, and the command uses the same constant.

**Test.** *Name a value of this type the domain would call wrong. Does `invariants()` reject it?*

The same goes for any constraint, inside `invariants()` or not: **a constraint that can never fail
is a finding.** It checks something that is always true (a record's `toString()`, a value already
guaranteed by an earlier check), so it looks like an observation but counts nothing. Example:
`isTrue(!stored.toString().isBlank(), "storedNamed")` is never false, because a record's
`toString()` is `FileName[value=…]`; observe `valueObject(stored, "stored")` or `stored.isValid()`.

#### J6. Names say the reason

**Kind:** Judgment, partly tool-checkable.

A constraint's name becomes the metric tag and the alert, so it has to explain itself:

- a parameter check is named after the parameter (`"email"`);
- a decision or cross-field rule is named after the predicate method it calls, so a reviewer can
  open it (`isFalse(token.isConsumed(), "isConsumed")`), or after the condition that must hold
  (`"emailUnregistered"`, `"withinBudget"`);
- a caught failure is named after what was being attempted (`"readable"`, `"catalogued"`).

**Test.** *Read only the tag `Class.method.name`. Can you say what happened without opening the
code?*

### Boundary rules

#### B1. Edges reject bad input and notice it

**Kind:** Judgment until the edge handlers exist; then Tool (every controller and listener goes
through one).

A REST controller adapter or bus listener turns bad input, including Jackson deserialization
errors, into a refusal (a 4xx for HTTP; a rejected, not retried, event on a bus), and notices it
with `kind=argument` under a name that identifies the endpoint or event type. A redirect or a
fallback page in answer to bad input (an unknown slug, a missing record) is a refusal too, and is
noticed the same way before the redirect: a slug that names nothing is a broken link of ours or
someone probing URLs, and both are worth a count.

By the time a request reaches the controller it has passed Spring and Jackson, so the edge is in
control and knows the context. It answers resiliently, by what was asked:

- **A request for one thing** (a page for a slug): the caller deserves to know the request was
  invalid. The response says so (a not-found page, or a redirect that carries the message), never
  a silent redirect.
- **A request for a list:** return what is possible. Invalid entries are left out and observed
  where they were found (C1, J4); the request itself is not refused.

A refusal the edge answers instead of throwing is observed with `arguments(...)` at `WARN`, on
the caller's input, and the answer follows:

```java
observer.arguments("cladeDetail", i -> i
                .isTrue(Clade.find(slug).isPresent(), "knownSlug"))
        .observe(Level.WARN);
```

This is the one exception to M1.

#### B2. Inbound Jackson errors stay at the edge

**Kind:** Tool.

An edge (a `@RestController` or message handler) deserializes its request before calling core, so
a Jackson error on inbound input is raised and handled there (B1). Core is handed only typed,
deserialized values; domain and kernel code never deserialize an inbound request themselves, and
never catch, wrap or notice an inbound Jackson error.

**Our own deserializers are the exception.** A custom `JsonDeserializer` we write takes control of
deserialization and knows the target model's requirements, so it treats the `JsonNode` as its
argument: it observes the fields it reads with `arguments(...)` before using them (`notBlank`,
`kebabFormat` for a slug, and so on), and `throwWhenInvalid()` refuses bad input with a count.

```java
JsonNode node = p.readValueAsTree();
String rankText = node.path("rank").asText(null);
String slug = node.path("value").asText(null);
observer.arguments("deserialize", i -> i
                .notBlank(rankText, "rank")
                .kebabFormat(slug, "value"))
        .throwWhenInvalid();
```

#### B3. Each edge has a backstop

**Kind:** Tool (it exists, and every edge is covered).

A last-resort handler at each web and bus edge notices any exception that nothing noticed, and
returns a defined response instead of a 500 (P7). It does not count exceptions that are already
noticed (refusals and noticed failures), so every backstop count means something went
unnoticed.

### Construction rules

#### C1. Value-carrying types never throw on construction

**Kind:** Tool for enums mapped from strings; Judgment for other types.

DTOs, records read from storage or JSON, and value types hold what they were given and declare
`invariants()` (P6). A lookup that maps a string to an enum or a sealed value type (a
`@JsonCreator`, an `of(String)`, a `fromValue`) returns **null** for a value it does not
recognise, null and blank included, instead of throwing. It has several consumers and cannot
know what each needs. No type gets a made-up `UNRECOGNISED` member: the value sets are often
biological, and a fake member would sit among the real ones.

The signal comes from the layers around the lookup. On the read path from the database:

1. MyBatis maps the column into the Dbo as stored.
2. The Dbo is observable; its constraint check fires on the bad column and names it.
3. The Dbo's factory maps to the entity; the lookup returns null, so the entity is invalid.
4. The repository hands the invalid entity on (J4).
5. The consumer observes the invalid entity and decides: skip it so the rest still shows (a
   browse page), or throw when it cannot proceed. Its observation says where the entity came
   from; with the Dbo's signal, that points at the rows holding null or bad values.

A caller whose value must exist observes the null itself (`notNull`, or `isPresent` on an
`Optional` variant) and refuses if it cannot proceed.

Wrong:

```java
// AccountDbo, read path
AccessLevel.valueOf(access),
AccountStatus.valueOf(status)
```

A bad stored value fails every read of that account with an exception that names neither the
operation nor the account.

### Mechanical rules

These are about the shape of the code. Each one is tool-checkable; until its tool exists, the
reviewer checks it.

| ID | Rule |
|---|---|
| M1 | `arguments(...)` always ends in `throwWhenInvalid()`, except at an edge that answers a refusal instead of throwing, which ends in `observe(Level.WARN)` (B1). |
| M2 | An `arguments(...)` lambda references only the method's parameters (or elements of them). Anything the method acquired goes through `state(...)`, and so does anything it calculated, even from its parameters alone (a map lookup, a `putIfAbsent` result): observing a calculated value as an argument would be misleading. |
| M3 | Checks made at the same point are one chained call, one constraint per line. Split only when a later check needs a value the earlier one guarantees. |
| M4 | A method that observes once calls `observer.arguments("method", ...)` / `observer.state("method", ...)` / `observer.noticeError("method", ...)` directly. A method that observes two or more times gets one `MethodObserver methodObserver = observer.forMethod("method")` and routes everything through it. A helper that observes on its own behalf uses its own method name. A helper shared to avoid duplication (DRY) may instead take the caller's `MethodObserver` as a parameter, so each observation is tagged with the caller's name; it never takes a method-name string. |
| M5 | No `Objects.requireNonNull` or hand-written null check that throws. Constructors validate through `observer.arguments("constructor", ...)`. |
| M6 | No logging in domain or kernel code. The maintainer does not read logs, so a log line informs nobody. |
| M7 | Only `com.naturalist.observability.Metric` touches `io.micrometer`. |
| M8 | `Observer` fields are `static final`. Checked on every class the change touches. An interface cannot hold a private field, so an interface's static method may build its Observer inline: `Observer.forClass(Clade.class).forMethod("of")`. |
| M9 | Range and sign checks use `atLeast` / `atMost` / `greaterThan` / `inRange`, never `isTrue(x >= n, ...)`. |
| M10 | Inside `invariants()`, use the direct-value form (`notNull(bar, "bar")`), not the by-function form on `this`. |
| M11 | `accounts-core`, console authentication and registration construct their `Observer` with `MonitoringMode.ALWAYS`; everything else uses the default `ON_FAILURE`. |
| M12 | A `catch` never observes with `isTrue(false, ...)` unless the catch turns the failure into a decision (J2, J3). |
| M13 | The class's `Observer` field is named `observer` (not `OBSERVER`). A method's `MethodObserver` local is named `methodObserver` (not `m`, and not the method's name). A helper's `MethodObserver` parameter is named `methodObserver` too. |
| M14 | A value of a framework kind is observed with its kind's constraint, which checks null and the type's `invariants()`: `valueObject`, `identifier`, `entityName`, `entityId`, `observable`, `aggregate`, `readModel` (and their `OrNull` / collection forms). Not a bare `notNull`, and never a hand-written `== null` check that skips or returns. Example: `DomainId` is a `ValueObject`, so `recordSlug` observes `valueObject(domain, "domain")` instead of `if (domain == null) return;`. |

## Known gaps today

These are where the code does not meet the standards yet. The reviewer reports them inside a
changed method like any other finding, and does not repeat them as existing-code concerns.

- **No edge handler or backstop (B1, B3).** There is no `@ControllerAdvice` or error controller in
  the code. Unexpected exceptions become Spring's default 500. There is no message bus yet.
- **Enums mapped with `valueOf` (C1).** `AccountDbo`, `EmailVerificationTokenDbo`, `CompoundDbo`,
  `CompoundConstituentElementDbo` and others throw on an unknown value instead of returning
  null. `Clade.of` and `Bioregion.of` refuse an unknown slug instead of returning null.
- **Unobserved throws (J1).** The observe-every-throw sweep is on hold at slice 3 (PR #3) until
  these standards are agreed. `PlantsEntityRefResolver` is one remaining example.
- **Decision observed by the consumer, not the decider (J2).** `UsageCommandImpl.reserve` must
  observe the budget refusal itself. When it does, `InsectIdentificationCommandImpl`'s
  `refusal(naturalist, "withinBudget", …)` would count the same refusal a second time, so its
  observation for that case goes. The `IdentificationBudgetSpent` alert in
  `deploy/prometheus/alerts.yml` matches `name=~".*\\.withinBudget"`, so the decider names its
  constraint `withinBudget` to keep the alert firing.
- **Caught failures observed as constraints (M12).** `FilesystemImageStore.store` observes its
  caught failures with `state(i -> i.isTrue(false, "stored")).observe(ERROR)` instead of
  `noticeError(..., Handling.RETHROWN)`; the other `isTrue(false, …)` sites predate `noticeError`.
- **Observer variable names (M13).** 9 `Observer` fields are named `OBSERVER`, and most
  `MethodObserver` locals are named `m` or after their method. A tool can rename them all; until
  then, the reviewer reports M13 only inside changed methods.

## Open questions

None.

## Decided

- A decision is observed by the decider, not by a caller that translates it (J2). Pat,
  2026-11-15.
- A lookup from a string (enum, sealed value type, `@JsonCreator`) returns null for an
  unrecognised value; no type gets an `UNRECOGNISED` member (C1). Supersedes the earlier
  `UNRECOGNISED` decision. Pat, 2026-11-15.
- Variable names: `observer` for the class's `Observer`, `methodObserver` for a method's
  `MethodObserver` (M13). Pat, 2026-11-15.
- The reviewer is a read-only project agent, `.claude/agents/observability-reviewer.md`. The
  session that wrote a change dispatches it before declaring the task done (CLAUDE.md,
  *Completeness verification*); Pat runs it on demand with `/observability-review [PR# | branch]`.
  A Stop hook that forces the review is deferred. Design:
  [`docs/plans/2026-11-15-observability-reviewer-design.md`](plans/2026-11-15-observability-reviewer-design.md).
  Pat, 2026-11-15.
- An edge is an inbound adapter (`@RestController`, message handler); Jackson errors on inbound
  input are raised and handled there, before core (P3, B2). A reply to our own call is noticed
  by the code that parses it (J3). Pat, 2026-11-15.
- A double count is one failure noticed twice at the same call site. A producer and its consumer
  each noticing the same failure is fine; the metrics are read together (P2). Pat, 2026-11-15.
- Reviewer scope: everything inside a changed method, old lines included; callers whose data or
  control flow the change alters; and, reported separately for awareness, concerns met in
  existing code. Pat, 2026-11-15.
- M8 is checked on every class a change touches. A helper uses its own method name unless it is
  shared to avoid duplication, when it may take the caller's `MethodObserver` (M4). Pat,
  2026-11-15.
- A custom Jackson deserializer we write observes the `JsonNode`'s fields as arguments
  (`notBlank`, `kebabFormat`) before using them (B2). Pat, 2026-11-15.
- Startup assembly code (composition roots, `*Assembly.from`, startup registry validation) is
  exempt from J1. Pat, 2026-11-15.
- A redirect or fallback page in answer to bad input is a refusal, noticed at the edge (B1).
  Pat, 2026-11-15.
- A value a method calculates, even from its parameters alone, is state, not an argument (M2).
  Pat, 2026-11-15.
- A value of a framework kind (ValueObject, EntityName, …) is observed with its kind's
  constraint, not just null-checked (M14). Pat, 2026-11-15.
- An interface's static method may build its Observer inline (M8). Pat, 2026-11-15.
- An edge observes a refusal it answers (rather than throws) with `arguments(...).observe(WARN)`;
  a request for one thing tells the caller it was invalid, a request for a list returns what is
  possible (B1, M1). Pat, 2026-11-15.
- A command observes the aggregate it builds before the transaction, with cleanup covering the
  refusal (J4); a rule a change adds about an entity's field is declared in that entity's
  `invariants()` (J5). Pat, 2026-11-15.
