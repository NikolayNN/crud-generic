# Declarative filter fields for flex pageable — Design

**Date:** 2026-07-14
**Status:** approved

## Context

Configuring a paged endpoint today spreads one filterable field over five places
(LocatorServer's `RtRoute` stack is the reference example):

1. controller `@RequestParam` + `@Parameter` description listing allowed operations as prose;
2. controller `FilterSpecificationUtils.isAvailableOperation(...)` call with an ops `Set`;
3. controller `new PageFilterRequest.Filter("name", nameFilter)`;
4. `AbsFlexPagingAndSortingService.buildSpecification` switch — dispatch by **type**
   (`getStringSpecification` etc.);
5. `AbsFilterSpecification.setupFieldPaths` — entity path + **type** (`Context("user.id", Long.class)`).

The type is declared twice, the field name four times, allowed ops twice. The pattern repeats
across ~9 `FilterSpecification*` classes and ~7 pageable services in LocatorServer.

The controller-side validation is also silently broken: `isAvailableOperation` *returns* a
boolean (the throwing variant is `checkFilterOperation`), and the result is discarded, so
disallowed operations are not rejected today.

Decisions made with the owner:
1. The fix lands in **crud-generic 5.0** (`flex.pageable`), not in LocatorServer.
2. The **HTTP API does not change** for filters: explicit per-field `@RequestParam`s and the
   wire format `like#foo` / `btn#a,b` stay as they are.
3. The legacy `+field`/`-field` sort variant is **dropped** — it caused real problems (`+` is
   URL-decoded to a space). Accepted sort syntax: `asc#field`, `desc#field`, or a bare `field`
   (ascending).
4. The old mechanism is **fully replaced** (no parallel API): `AbsFilterSpecification` and the
   `buildSpecification` switch are deleted.
5. Approach: **fluent registry declared in the pageable service** (one line per field), not
   entity annotations (a web contract doesn't belong on JPA entities, and one entity may serve
   two endpoints with different filter sets) and not a data-only specs class (would keep a
   pure-boilerplate bean per entity).

## New API (`by.nhorushko.crudgeneric.flex.pageable`)

### `FilterFields<ENTITY>`

Immutable registry of filterable fields — the single place a field is declared. Built via
`FilterFields.builder(converters)`:

```java
f.string("name", CONTAINS)                                  // path = name
 .string("description", CONTAINS)
 .ofLong("userId", "user.id", EQUAL)                        // path differs from name
 .instant("archivedAt", GT, GTE, LT, LTE, BETWEEN, IS_NULL, NOT_NULL)
 .instant("createdTime", GT, GTE, LT, LTE, BETWEEN)
 .ofBoolean("oneOff", EQUAL)
 .ofEnum("status", "job.status", JobStatus.class, EQUAL, IN) // converter derived from the enum class
 .field("score", "rating.score", BigDecimal.class, GT, LT)   // any type with a registered converter
 .custom("special", filter -> (root, q, cb) -> ...)          // escape hatch, builds Specification manually
 .build();
```

Each entry records: external filter name, entity path (defaults to the name), value type,
allowed `FilterOperation`s. Registry responsibilities:

- **`Specification<ENTITY> toSpecification(PageFilterRequest.Filter)`** — validates, then builds:
  - unknown field name → `FilterValidationException`;
  - operation not in the declared set → `FilterValidationException`;
  - blank filter value → the filter is skipped (no specification), matching today's
    `alwaysTrue`/empty-filter semantics;
  - `custom` entries delegate straight to the factory — no operation validation (the factory
    owns the behaviour).
- **`Sort sort(String sortExpression)`** — parses `asc#field` / `desc#field` / bare `field`
  (ascending), maps the property through the registry (`userId` → `user.id`) and returns a
  Spring `Sort` directly; an unknown property passes through unchanged (same as today's
  `handleSort`). A legacy `+`/`-` prefix or any other malformed expression →
  `FilterValidationException`. Single-property sort, as today. The service builds
  `PageRequest.of(page, size, sort)` itself — the `PageableUtils` →
  `PageRequestBuilder` string round-trip is gone from this path.

Builder rules (fail fast at startup, all `IllegalStateException`):
- typed entries must declare at least one operation;
- duplicate names are rejected;
- `build()` verifies a converter is registered in `Converters` for every declared type —
  except entries that carry their own converter (`ofEnum`) and `custom` entries.

Convenience methods cover the types with default converters: `string`, `ofLong`, `ofInteger`,
`ofDouble`, `ofFloat`, `ofBoolean`, `instant` (each with `(name, ops...)` and
`(name, path, ops...)` overloads).

**`ofEnum(name, [path,] enumClass, ops...)`** carries its own converter derived from the class
(`Enum.valueOf`), so enums need **no registration in `Converters` at all** — today every enum
filter costs an extra `map.put(X.class, X::valueOf)` line in the application's `Converters`
subclass (LocatorServer's `ConvertersExt` has eight such lines). An unknown constant in the
request is a client error: it is wrapped in `FilterValidationException` (400), not surfaced as
a raw `IllegalArgumentException` (500).

Everything else goes through `field(name, path, type, ops...)` plus a converter registered in
the application's `Converters` subclass, or through `custom`.

Internally the registry instantiates the stateless `FilterSpecifications<ENTITY, T>` helpers
from filter-specifications-lib directly (`new`), replacing the eight `@Lazy @Autowired`
generic-injection fields of `AbsFilterSpecification`. The filter layer no longer depends on
Spring wiring; unit tests need no context. `Converters` stays injected — it is abstract and
applications register custom converters by subclassing it.

### `FilterValidationException`

New in `flex.exception`: `FilterValidationException extends RuntimeException` with fields
`field`, `operation`, `allowedOperations` and a message in the spirit of
`checkFilterOperation` (`"Filter: 'name', expect operations: '[like]', but was 'eq'"`).
Applications map it to HTTP 400 in their exception handlers (README notes this).

### `AbsFlexPagingAndSortingService<ID, DTO, ENTITY>`

Reworked:

- the `SPECS` type parameter disappears;
- constructor: `(JpaSpecificationExecutor<ENTITY> repository, AbsModelMapper mapper,
  Class<DTO> dtoClass, Converters converters)`;
- one abstract method replaces the old two:
  `protected abstract FilterFields<ENTITY> filterFields(FilterFields.Builder<ENTITY> f);`
  invoked lazily on first use and cached (not from the constructor — it is overridable and may
  touch subclass state);
- `toDto(entity)` gets a default implementation via `mapper.map(entity, dtoClass)`
  (still overridable);
- `page(PageFilterRequest)` walks the filter group recursively as today, but builds every
  specification through the registry — validation is always on and happens before the query.

## Deleted

- `AbsFilterSpecification` (with `Context`, `handleSort`, the generic-injection fields);
- abstract `buildSpecification(Filter)` and the `SPECS` parameter on
  `AbsFlexPagingAndSortingService`.

`PageFilterRequest`, `FilterGroupBuilder` and the filter-specifications-lib dependency are
unchanged. `PageableUtils` stays (applications use it directly) but is no longer referenced by
`AbsFlexPagingAndSortingService`. `BasePageRequest` changes its default sort from `-id` to
`desc#id` and its javadoc examples to the `asc#`/`desc#` syntax.

## Migration (README, Migration-to-5.0 section)

Per entity: delete the `FilterSpecification*` class, fold its path/type map and the service's
switch into one `filterFields` declaration, drop the mapper field and `toDto` override if the
default suffices. Reference before/after (RtRoute):

```java
@Service
public class RtRoutePageableService
        extends AbsFlexPagingAndSortingService<Long, RtRoute, RtRouteEntity> {

    public RtRoutePageableService(RtRouteRepository repository, AbsModelMapper mapper,
                                  Converters converters) {
        super(repository, mapper, RtRoute.class, converters);
    }

    @Override
    protected FilterFields<RtRouteEntity> filterFields(FilterFields.Builder<RtRouteEntity> f) {
        return f.string("name", CONTAINS)
                .string("description", CONTAINS)
                .ofLong("userId", "user.id", EQUAL)
                .instant("archivedAt", GT, GTE, LT, LTE, BETWEEN, IS_NULL, NOT_NULL)
                .instant("createdTime", GT, GTE, LT, LTE, BETWEEN)
                .ofBoolean("oneOff", EQUAL)
                .build();
    }
}
```

Controller: the static ops `Set`s and the (broken) `isAvailableOperation` calls are removed;
`@RequestParam`/`@Parameter` declarations stay (external contract + Swagger). Annotation
descriptions remain hand-written — annotation values are compile-time constants — but they are
documentation only; enforcement is automatic. Hardcoded one-off specs like `oneOff`
(`cb.isFalse`) become regular declarations: `.ofBoolean("oneOff", EQUAL)` in the registry plus
`buildFilter(EQUAL, false)` in the controller.

Enum filters: entries migrated to `ofEnum` no longer need their `map.put(X.class, X::valueOf)`
line in the application's `Converters` subclass — once all pageable services are migrated,
LocatorServer's `ConvertersExt` shrinks to the `LocalDate`/`LocalDateTime` converters.

Sort parameters: endpoints (and clients) using the legacy `+field`/`-field` syntax must switch
to `asc#field`/`desc#field` — after 5.0 the legacy prefixes are rejected with a 400. Defaults
like `@RequestParam(defaultValue = "desc#createdTime")` already comply; `BasePageRequest`
subclasses relying on the old `-id` default pick up `desc#id` automatically.

A field is now declared in **two** places: one registry line + one controller parameter
(down from five).

## Tests

- **library** (no Spring context): `FilterFields` builder — every typed method, nested path,
  enum entry without a registered converter, custom entry, duplicate name rejected, missing
  converter rejected, no-ops entry rejected;
  `toSpecification` — disallowed operation, unknown field and unknown enum constant throw,
  blank value skips;
  `sort` — `asc#`/`desc#`/bare-field parsing, mapped property, pass-through, legacy `+`/`-`
  rejected.
- **test-application** (new ITs; pageable has no coverage there today): a paged endpoint over an
  H2-backed flex entity exercising string CONTAINS, long EQUAL on a nested path, instant
  BETWEEN, combined AND filters, sort by a mapped property, and HTTP 400 for a disallowed
  operation and an unknown field.
- README Migration-to-5.0 gains the pageable subsection with the before/after above.

## Out of scope

- LocatorServer changes (they happen during its 5.0 migration, following the README);
- filter-specifications-lib changes (none needed; wire format and operations untouched);
- generating Swagger descriptions from the registry (annotation values must be constants;
  revisit only if docs drift becomes a real problem);
- a generic `?filter=` query parameter (explicitly rejected: breaking API change).
