Notes captured from rest:

- Repository test abstraction, the pattern is simple and consistent abstract the test logic
    - perhaps parameteried tests?
    - abstract methods for lists of entities
    - does not exist name
- TestEntitySourceTest asserts at least 4 entities exist
- fieldnotes.Description, that should be formatted text
    - look at orange sulfer insect the university description
    - can EntityNames or NamedValues be formatted with tags so that the ui can link to their association?
- query abstraction yes to implementing the by-id functions they will be necessary for factories
- factories will switch their query for entities favoring id when its defined
- query test abstraction same logic here avoid unnecessary duplication
- <domain>-ui submodule for console and app purely organization
- <domain>-ext submodule for external resources
    - <domain>-ext-ecowitt sensor data
    - <domain>-ext-ucd uc davis ipm website
- the user interface app and console present a navigable interface from insect -> zone -> weather etc
- dont forget to add the naturalist-rewrite module for static analysis
- ask about records and equals, referential equality similar to how the openrewrite framwork works is so nice

Licence!!!!

- I need a license on these artiacts. This is the product of a principal engineer having 25 years experience
  who is capable of modeling complex multi dimensional systems in an effort to understand their unique nature.
  and then present the model in such a manner that exposes the level of understanding necessary to develop hypethoses

Using the Strongly typed system for navigating the query and command graph

- EntityId and EntityName inherit from Identity
- Queries at the namespace root are Aggregate Queries they are interfaces having nested entity queries

InsectQuery insectQuery;
insectQuery
.species().get(Identitiy identity)

insectQuery().get(Identity) -> returns the InsectAggregate

Queries are simple each implementation should be less that 20 lines max and even thats alot
they observer the arguments then delegate.
AggregateQueries delegate to factories

The class graph hydrated from the dependency injection framework should provide a navigable tree
allowing the naturalist, developer and ai agent to quickly and efficiently obtain their desired object

Query tests should be constrained to their functional behavior, they delegate to repositories they should not test their
behavior
just ensure its accurately invoked

PersistenceId -> this should be nullable on insert and then required on retrieve. The TestEntitySource already
performs the necessary behavior of assigning an id on insert. Lets figure out how to bake that understanding into the
system.

Ideally there would be a special constraint that is able to achive this, if that is too difficult then the application
boundary
should be testing for a valid PK. perhaps queries assert ID on retrive, if the ID is null then there a program error
that should be thrown.