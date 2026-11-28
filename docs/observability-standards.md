# Observability standards

**Status:** in use since 2026-11-15; linked from `CLAUDE.md`. Decision history:
[`notes/observability-decisions.md`](notes/observability-decisions.md) (not part of the rubric).

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

Observation is also how the code keeps learning. Every object's state is observed as it moves
through the system, so a change that breaks an invariant nobody expected shows up as a count
during a rolling deployment, not in a manual QA cycle or a user's report. That is why guards stay
(J5) and why a value is observed again by each consumer (P2): together they are the confidence a
refactor needs.

## Levels

The `level` tag says who has to act. `Level`'s Javadoc carries the same rules.

| Level | Means | Who sets it |
|---|---|---|
| `ERROR` | An exception was thrown: `throwWhenInvalid()`, or `noticeError`. | The framework only. A call site never passes `ERROR` to `observe(...)`; a count with no throw is a contradiction. |
| `WARN` | Something is wrong: invalid data, broken logic, a business rule saying no (J2), a value our own code produced that fails its invariants. The maintainer triages. | The call site, with `observe(Level.WARN)`. |
| `INFO` | Signal, not a problem: what callers ask for that we lack (B1), a restricted path in use (J5). No action needed. | The call site, with `observe(Level.INFO)`. |

## Principles

- **P1. Resilient by default.** No 500s, and the maintainer never needs a log. Every rule below
  serves this.
- **P2. Every exception is noticed at the point that knows what failed and why.** Observe
  wherever a value or failure is consumed, however often it was observed upstream: each layer's
  observation names its own layer, and the metrics are read together. The one limit is inside a
  single handler: a `catch` notices its exception once (M15). Judge each handler on its own;
  never trace the call path to decide whether a layer may observe. Example: a failed insert is
  noticed in `InsectLifeStageTransaction.insert` and again in `AbstractEntityRepository.insert`;
  metrics are tagged by class and method, so the two counts stay apart.
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
- **P8. Production teaches.** Observe state as it moves, so an unexpected invariant break is
  counted during a rolling deployment instead of waiting for manual QA.

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
- sorts its findings into two sections: *this change* (lines the change added or edited, and
  rules the change broke elsewhere), which the author fixes, and *existing code* (old lines in
  changed methods, class-level rules on untouched lines, anything met while following a call),
  which goes to the maintainer and is never fixed in the same change, so a small change stays a
  small diff;
- never edits code, this document, or the tools.

The author (human or agent) applies the same tests while writing. Each test is worded so that
the author and the reviewer reach the same answer.

The reviewer is the read-only `observability-reviewer` project agent
(`.claude/agents/observability-reviewer.md`). The session that wrote a change dispatches it
before declaring the task done; Pat runs it with `/observability-review [PR# | branch | path]`.

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
| J7 | Which non-private methods in the diff's units take parameters, and does each open with an `arguments(...)` check? | `java_file_outline` for the signatures, then `java_class_source` with `member` |
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
goes up, it is a finding.

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
catches the refusal and translates it, so every future caller is covered. A caller that
translates the refusal may observe it too (P2), under its own name, never the decider's: an
alert keyed on the decider's name then fires once per refusal.

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

The name is the call that failed: the kind of call, then what it does (`sqlInsert`, `sqlUpdate`,
`fileWrite`, `httpPost`, `jsonSerialize`). Never a past-tense result such as `inserted`: on a
dashboard it reads as a count of successes. When one handler covers two calls of the same kind,
add what each one touches (`sqlInsertParent`). Every path through one catch uses the same name.

A failure the catch turns into a decision (a `WARN` refusal, an `INFO` signal) stays a constraint
instead (J2). Never write `state(s -> s.isTrue(false, "<name>"))` for a caught failure.

**Test.** *Read the catch's last statement. Does the `Handling` describe it?*

Right:

```java
// InsectIdentificationCommandImpl.reserveStoreAndCatalogue
} catch (RuntimeException e) {
    observer.noticeError("reserveStoreAndCatalogue", "catalogued", e, Handling.RETHROWN);
    try {
        imageStore.delete(SPACE, storedFileName);
    } catch (RuntimeException deleteFailure) {
        observer.noticeError("reserveStoreAndCatalogue", "storedDiscarded", deleteFailure,
                Handling.SWALLOWED);
        e.addSuppressed(deleteFailure);
    }
    throw e;
}
```

**A failed cleanup is counted.** Attaching it with `addSuppressed` is not noticing it. A delete
that fails leaves its file behind; failed deletes accumulate until the disk is full, and then
the whole system stops. The cleanup's catch notices the failure (`SWALLOWED`: the original
failure is what propagates).

**A reply to our own call.** When code parses the reply to a call *we* made (a model's tool
output, a vendor's response), a parse failure means a dependency misbehaved, not that the user
sent bad input. The code that parses the reply notices it here, with `WRAPPED` or `SWALLOWED` by
its exit, and never as a refusal. This is not B2: B2 covers only inbound requests at an edge.

#### J4. The consumer chooses throw or observe

**Kind:** Judgment.

A method that receives or acquires a value chooses the terminal operation by asking *would the
invalid value make this method throw?*

- **Yes:** `throwWhenInvalid()`, so the exception is counted under a name. Example:
  `InsectLifeStageDbo.toEntity` checks `isStageKind(stageKind)` before switching on it, because an
  unknown kind reaches the `default -> throw`.
- **No, or it is someone else's call:** hand it on. A method whose own decision depends on the
  value observes it (`observe(level)`); one that only passes it on observes nothing of it, because
  the consumer that uses it observes it. Example: a repository read checks its arguments and
  returns what it read without observing it; the query or command that uses the result observes
  it.

A producer never throws on a value only because it is invalid; it does not know whether its
consumers care. **Observe what you were given, not what you built.** A method's `arguments(...)`
check covers its input; it does not then re-check the value it built from that input with a
throwing `state(...)`, because that takes the decision away from the consumer that receives it.
Example: `*Dbo.from(entity)` observes `entity` as its argument and returns the Dbo, with no
`observable(dbo)` check after building it.

**A Dbo's mapping to the entity is a read, and is permissive.** `toEntity` and the child Dbos'
`to*` methods throw on nothing they read: an unrecognised column maps to null (C1), a name is
built as stored, and the consumer observes the result. Their arguments are checked only for
presence (`notNull`, `throwWhenInvalid()`), since a missing collection would throw here; what the
collections hold is handed on unchecked. The one exception is a value with no entity to build at all, such as an unknown
`stageKind` with no permit to switch to (the example above). Example:
`InsectLifeStageDbo.toEntity`.

**Test.** *Which parts of the value does this method use? Would an invalid one throw here?*

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

A plain result record handed back to startup or tooling code (a tally such as
`FilesystemImageStore.ConformReport`, returned to the seeder) is not a framework kind and needs no
`invariants()`. Pat, 2026-11-17.

The same goes for any constraint, inside `invariants()` or not: **a constraint that can never fail
by construction is a finding.** It checks something the same code already guarantees (a record's
`toString()`, a value built on the previous line, a value already guaranteed by an earlier check in
the same method), so it looks like an observation but counts nothing. Example:
`isTrue(!stored.toString().isBlank(), "storedNamed")` is never false, because a record's
`toString()` is `FileName[value=…]`; observe `valueObject(stored, "stored")` or `stored.isValid()`.

**A guard is not a finding.** A constraint that cannot fail only because of how *other* code
behaves today is a guard: a producer's unwritten contract, or a cross-field rule the factory
happens to satisfy. It is what catches the refactor that breaks the contract, and removing it
lowers the confidence a refactor needs. Keep it, and name the contract it guards. Example: a rank
page's `isTrue(plant, Plant::hasSpecies, "hasSpecies")` cannot fail while `PlantFactory.buildByName`
always sets the asked-for rung, but a query that degrades to the genus would trip it. It would not
render a species page with no species. The test: *does it fail only if code elsewhere changes?* Then
it is a guard. Pat, 2026-11-26.

**Exception: a restricted path announces itself.** Under `MonitoringMode.ALWAYS`, a constraint
placed inside the branch it names counts every entry to that branch. That is a deliberate
non-error signal, and it is not a finding when the branch is a restricted code path the maintainer
must know is being used. Example: in `NaturalistUserDetailsService.loadUserByUsername`,
`state(i -> i.isTrue(isConfigAdmin, "isConfigAdmin")).observe(Level.WARN)` inside
`if (isConfigAdmin)` counts every config-admin login. The constraint is named after the branch
condition. The exception never covers an `ON_FAILURE` observer, where the constraint counts
nothing.

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

#### J7. A callable method observes its parameters

**Kind:** Judgment, partly tool-checkable.

A method another class can call (public, protected or package-private, static or not) checks
every parameter it uses before using it: `arguments(...)` with the kind's constraint (M14:
`identifier`, `entityName`, `readModel`, `notNull` for a plain reference, ...), ending in
`throwWhenInvalid()` (M1) or, at an edge that answers instead of throwing, in `observe(...)` (B1).
Without the check, a `null` throws an NPE nobody counts (J1), and an invalid value produces a
silently wrong result. A private helper may rely on its caller's check, when every caller checks the
value it passes. A constructor is covered by M5.

**A missing value must be able to arrive as null.** A parameter or record component typed as a
primitive (`int`, `long`, `boolean`) cannot say "missing": an absent JSON field or unset column
silently becomes `0` or `false`, and a null `Integer` passed to an `int` throws an unboxing NPE at
the call site, before any check runs. Use the `java.lang` wrapper (`Integer`, `Long`, `Boolean`),
so `notNull(count, "count")` counts the missing value under a name (M16).

Example: `PlantMatch.genusOf(PlantSpeciesName species)` read `species.value()` unchecked. It now
opens with `observer.arguments("genusOf", i -> i.entityName(species, "species")).throwWhenInvalid()`.

**Test.** *What does this method do with `null`, or with an invalid value of the parameter's type?*
If the answer is an NPE or a silently wrong result, it is a finding. Pat, 2026-11-26.

### Boundary rules

#### B1. Edges reject bad input and notice it

**Kind:** Judgment until the edge handlers exist; then Tool (every controller and listener goes
through one).

A REST controller adapter or bus listener turns bad input, including Jackson deserialization
errors, into a refusal (a 4xx for HTTP; a rejected, not retried, event on a bus), and notices it
with `kind=argument` under a name that identifies the endpoint or event type. A redirect or a
fallback page in answer to bad input (an unknown slug, a missing record) is a refusal too, and is
noticed before the redirect, with `kind=state` when the check is on a lookup of the input (M2): a slug that names nothing is a broken link of ours or
someone probing URLs, and both are worth a count.

By the time a request reaches the controller it has passed Spring and Jackson, so the edge is in
control and knows the context. It answers resiliently, by what was asked:

- **A request for one thing** (a page for a slug): the caller deserves to know the request was
  invalid. The response says so (a not-found page, or a redirect that carries the message), never
  a silent redirect.
- **A request for a list:** return what is possible. Invalid entries are left out and observed
  where they were found (C1, J4); the request itself is not refused.

A refusal the edge answers instead of throwing ends its check in `observe(...)`, and the answer
follows. The check is `arguments(...)` on the caller's input as given, and `state(...)` on
anything the edge computed from it (M2): a lookup result, or a value object built from two
parameters such as `GridCell.of(lat, lon)`.

The level says who has something to fix:

- **`INFO` for what the caller sent.** An invalid or unknown name in a URL, or a lookup of it that
  finds nothing, is expected traffic: a stale link, a record not added yet, someone probing URLs.
  Nothing in our code is wrong, but the count still shows what visitors ask for that we lack.
- **`WARN` for what our own code handed the edge.** A stored record or read model that fails its
  invariants is a state for the maintainer to fix, even though a request surfaced it.

```java
observer.state("cladeDetail", i -> i
                .isTrue(Clade.find(slug).isPresent(), "knownSlug"))
        .observe(Level.INFO);
```

The plants console is the reference (`PlantsController`): the handler observes its argument and
whether the lookup found anything at `INFO`, then hands the found value to its page method, which
observes it at `WARN` (`readModel(plant, "plant")`).

This is the one exception to M1 (and the matching exception for a `state(...)` check).

**Terminate as early as possible.** Commands are persistent; their success depends on a valid
state, so control flow stops at the first point that can tell a request is invalid. An edge that
can detect it (a length limit the entity publishes, an unknown slug) checks it itself, observes
the refusal as above and answers, before calling core. The command still validates its own
input. A refusal only the command could detect is caught at the edge and answered without being
counted again. Order an edge's checks cheapest first: request parameters before reading the
body, reading before decoding, so a bad parameter never costs a 20 MB read or an image decode.

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
| M1 | `arguments(...)` always ends in `throwWhenInvalid()` (a Dbo's mapping to the entity checks its arguments only with `notNull`: J4), except at an edge that answers a refusal instead of throwing, which ends its check (`arguments(...)` or `state(...)`) in `observe(Level.INFO)` for the caller's input and its lookup, or `observe(Level.WARN)` for a value our own code handed the edge (B1). |
| M2 | An `arguments(...)` lambda references only the method's parameters (or elements of them). Anything the method acquired goes through `state(...)`, and so does anything it calculated, even from its parameters alone (a map lookup, a `putIfAbsent` result, a value object built from several parameters such as `GridCell.of(lat, lon)`): observing a calculated value as an argument would be misleading. This holds when the check is a refusal too: where the value came from decides the kind, not what the check does with it (`state(i -> i.isTrue(spaceWithinRoot(space), "spaceWithinRoot")).throwWhenInvalid()`). |
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
| M14 | A value of a framework kind is observed with its kind's constraint, which checks null and the type's `invariants()`: `valueObject`, `identifier`, `entityName`, `entityId`, `namedValue`, `observable`, `aggregate`, `readModel` (and their `OrNull` / collection forms). A `NamedValue` (`FileName`, `EmailAddress`) is one of these kinds: `namedValue(name, "name")` checks null and `isValid()`. Not a bare `notNull`, and never a hand-written `== null` check that skips or returns. Example: `DomainId` is a `ValueObject`, so `recordSlug` observes `valueObject(domain, "domain")` instead of `if (domain == null) return;`. |
| M15 | A `catch` calls `noticeError` at most once, and never pairs it with a constraint observation for the same exception (P2). Judged inside the one handler; layers elsewhere do not matter. |
| M16 | Parameters and record components use `java.lang` wrapper types (`Integer`, `Long`, `Boolean`, ...), not primitives, so a missing value arrives as null and the argument observation counts it (J7). |
| M17 | No `notNull` on a value that another constraint in the same check already fails on null: `kebabFormat`, `notBlank`, `email`, `atLeast` / `atMost` / `inRange` / `greaterThan`, `isPresent`, and every M14 kind constraint. `maxLength` and the `OrNull` forms pass null, so pair them with `notNull` (or `notBlank`) when the value is required. |

## Known gaps today

These are where the code does not meet the standards yet. The reviewer reports them inside a
changed method under *existing code*, and does not repeat them when it meets them elsewhere.

- **`noticeError` names in the past tense (J3).** Names written before 2026-11-27 name a result
  (`readable`, `catalogued`, `stored`, `conformed`, `serialized`) instead of the call that failed. A
  later sweep renames them; the examples in this document that still use them predate the rule.
- **Rdbms insert catches do not notice (P2, M15).** Most `*Transaction` and `*EntityRepositoryRdbms`
  inserts catch the mapper's exception, turn a unique violation into `PrimaryKeyConstraintException`
  and rethrow anything else, with no `noticeError`. Each path notices it once:
  `noticeError("sqlInsert", e, Handling.WRAPPED)` before the translated throw, `Handling.RETHROWN`
  before `throw e`. Reference: `InsectLifeStageTransaction.insert`.
- **Repository reads still observe what they read (J4).** About 68 files, `AbstractEntityRepository`'s
  inherited `getByName` / `getByEntityNameSet` / `getPage` among them, end a read in
  `state(s -> s.observableCollection(result, "result")).observe(Level.WARN)` (or `namedEntityOrNull`,
  `entityNameCollection`). The rule is now that the read returns what it read unobserved and the
  consumer observes it; a separate sweep removes these.
- **No edge handler or backstop (B1, B3).** There is no `@ControllerAdvice` or error controller in
  the code. Unexpected exceptions become Spring's default 500. There is no message bus yet.
- **Enums mapped with `valueOf` (C1).** `AccountDbo`, `EmailVerificationTokenDbo`, `CompoundDbo`,
  `CompoundConstituentElementDbo` and others throw on an unknown value instead of returning
  null. `Clade.of` and `Bioregion.of` refuse an unknown slug instead of returning null.
- **Unobserved throws (J1).** The observe-every-throw sweep is on hold at slice 3 (PR #3) until
  these standards are agreed. `PlantsEntityRefResolver` is one remaining example.
- **Decision observed by the consumer, not the decider (J2).** `UsageCommandImpl.reserve` must
  observe the budget refusal itself. When it does, `InsectIdentificationCommandImpl`'s
  `refusal(naturalist, "withinBudget", …)` may stay but takes its own name (not `withinBudget`),
  so the alert does not page twice for one refusal. The `IdentificationBudgetSpent` alert in
  `deploy/prometheus/alerts.yml` matches `name=~".*\\.withinBudget"`, so the decider names its
  constraint `withinBudget` to keep the alert firing.
- **Caught failures observed as constraints (M12).** `FilesystemImageStore.store` observes its
  caught failures with `state(i -> i.isTrue(false, "stored")).observe(ERROR)` instead of
  `noticeError(..., Handling.RETHROWN)`; the other `isTrue(false, …)` sites predate `noticeError`.
- **Observer variable names (M13).** 9 `Observer` fields are named `OBSERVER`, and most
  `MethodObserver` locals are named `m` or after their method. A tool can rename them all; until
  then, the reviewer reports M13 only inside changed methods.

- **Dbos re-check what they built (J4).** Most `*Dbo.from(...)` factories end in
  `state(i -> i.observable(d, "dbo")).throwWhenInvalid()`. The insects life-stage Dbos are fixed;
  the rest are reported as existing code.
- **Primitive parameters and record components (M16).** Added 2026-11-27; existing code has not
  been swept. The reviewer reports M16 only inside changed methods and on record components a
  change adds or edits.

## Open questions

None.
