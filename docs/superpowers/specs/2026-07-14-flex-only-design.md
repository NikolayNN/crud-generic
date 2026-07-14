# Flex-only library (пункт 4) — Design

**Date:** 2026-07-14
**Status:** approved

## Context

The library owner keeps only the `by.nhorushko.crudgeneric.flex` stack; v1 (root packages) and v2
are legacy tiers superseded by flex. Flex currently leaks 8 dependencies into v1/v2 packages, and
all pagination support lives in v1/v2 — while the consuming application (LocatorServer) actively
uses `AbsFlexPagingAndSortingService`, `PageFilterRequest`, `AbsFilterSpecification` and
filter-specifications-lib directly. LocatorServer is pinned to a pre-4.0 library version (it still
imports `AbsMapUpdateDtoToPresetEntity`, removed in `aee52e5`), so breaking master does not affect
it until it deliberately upgrades; it will migrate in one pass using the migration table this work
adds to the README.

Decisions made with the owner:
1. **Kernel moves into flex** (not left in place).
2. **Pagination is ported into flex** (not dropped): the flex variant only; the `@Deprecated`
   v2 `AbsPagingAndSortingService` is deleted.
3. **Big-bang execution** on master (no deprecation release in between).

## Target library structure

One root package `by.nhorushko.crudgeneric.flex` remains:

```
flex/
├── AbsModelMapper                    (unchanged)
├── config/                           (unchanged, minus two isMapperType branches)
├── controller/                       (unchanged, imports updated)
├── mapper/ mapper.core/ mapper.composite/ mapper.mapper/   (unchanged)
├── service/                          (unchanged)
├── model/     ← ADD: AbstractDto, AbstractEntity, IdEntity      (from v2.domain)
│               ADD: SettingsVoid, SettingsTranslateable          (from root domain)
│               (existing: AbsBaseDto, AbsCreateDto, AbsUpdateDto)
├── exception/ ← NEW: AppNotFoundException, AuthenticationException (from root exception)
├── util/      ← NEW: FieldCopyUtil, PageableUtils               (from root util)
└── pageable/  ← NEW: PageFilterRequest, AbsFilterSpecification, FilterGroupBuilder,
                AbsFlexPagingAndSortingService                    (from v2.pageable)
                BasePageRequest                                   (from v2.controller)
```

Behaviour changes nowhere: only package moves, import updates, and deletions.

### Deleted

- All of v1: root `controller`, `domain`, `mapper`, `service`, remaining `util`
  (`SpecificationUtils` is used only by deleted code).
- All of v2: `v2.controller`, `v2.domain`, `v2.mapper`, `v2.pageable`, `v2.service`.
- `AbsPagingAndSortingService` (v2, already `@Deprecated` pointing at the flex variant; drags in
  the v2 mapper stack). LocatorServer's 3 usages migrate to `AbsFlexPagingAndSortingService`
  (constructor change + implement `toDto`).
- `AbsMapperBase` (v2) is NOT ported: it existed only for the eager-init type check of v2 mappers.
  `AbsMapperEagerInitPostProcessor.isMapperType()` drops its v1 (`AbstractMapper`) and v2
  (`AbsMapperBase`) branches — only flex types remain.

### pom.xml (library)

- `filter-specifications-lib` **stays** (needed by `PageableUtils`, `AbsFilterSpecification`;
  the app also uses it directly).
- `json-patch` **removed** (zero usages — dead dependency).
- Version: `4.0-SNAPSHOT` → `5.0-SNAPSHOT` (marker of the flex-only line).

## Library tests

- v1/v2 test packages deleted.
- `IdEntityIsNewTest` and `AppNotFoundExceptionTest` move with their classes (package/import update).
- Flex tests: import updates only.
- `AbsMapperEagerInitPostProcessorTest`: assertions for the v1/v2 branches removed, flex-type
  assertions kept.
- Shared test fixtures (`Car`, `User`, `Message` + entities) kept with import updates.

## test-application migration

Deleted with v1 (no unique coverage): the Mock*/Group/Item family — `MockService`, `MockBService`,
`MockADescriptionService`, `GroupService`, `MockController`, their entities/DTOs/mappers/
repositories, tests `CrudGenericServiceIT`, `MockControllerIT`, `MockADescriptionServiceTest`,
`MockBDtoMapperTest`, `SaveV1IT`, `SaveV1CascadeIT`, SQL scripts `add-entities-*.sql`.

v2 fixtures — unique coverage migrates to flex, rest deleted:

| v2 today | Fate |
|---|---|
| `RegionServiceCRUD` + `RegionMapper` (assigned id) | → flex: `RegionMapConfig` (`AbsFlexMapConfigDefault`) + `RegionServiceCRUD extends AbsFlexServiceCRUD`; new `FlexAssignedIdSaveIT` (test-first): assigned id absent → insert; present → update, no duplicate (persistOrMerge upsert semantics, not yet covered by flex ITs) |
| `SaveOverridingMapperIT` | → rewritten as flex overriding-mapper IT (persistOrMerge defends mappers overriding toEntity) |
| eagerinit tests (assert on v2 bean `mockAImmutableMapper`) | → rewritten onto flex beans (`taskMapConfig`/`orderMapConfig`) and `OrderEntity→OrderDto` mapping |
| Tracker (+ 17 `TrackerServiceIT` tests, `TrackerAbsMapperEntityDto`, `data.sql` seeding) | deleted — duplicates coverage already in flex ITs and library unit tests; sentinel-0 covered by `FlexSaveCascadeIT`/`FlexExtSaveIT` |
| Driver/User (v2 ext) + `DriverServiceTest`, `UserMapper`, `DriverMapper` | deleted — flex ext coverage is Project/Task (`FlexExtSaveIT`) |

Kept files (Order*, Project/Task, `AbstractEntityNullifyZeroIdTest`, `MapperNullifyZeroIdTest`,
`FieldCopyUtilTest`, …) get import updates `v2.domain.*` → `flex.model.*` etc.

## README

- Fix name drift: `AbsFlexMapConfig` → `AbsFlexMapConfigDefault`, `AbsDtoModelMapper` → `AbsModelMapper`.
- Prerequisites: JDK 17+, Spring Boot 3.x (currently says JDK 8+ / Boot 2.x).
- New **Migration to 5.0** section:
  - import table: `v2.domain.AbstractDto` → `flex.model.AbstractDto`, `exception.*` →
    `flex.exception.*`, `v2.pageable.*` → `flex.pageable.*`, `domain.SettingsVoid` →
    `flex.model.SettingsVoid`, `v2.controller.BasePageRequest` → `flex.pageable.BasePageRequest`, …
  - class-equivalents table: `ImmutableGenericService`/`CrudGenericService`/`AbsServiceCRUD` →
    `AbsFlexServiceCRUD`; `AbsServiceExtCRUD` → `AbsFlexServiceExtCRUD`; `AbsMapperEntityDto`/
    `ImmutableDtoAbstractMapper` → `AbsFlexMapConfigDefault`; `AbsPagingAndSortingService` →
    `AbsFlexPagingAndSortingService`; v1/v2 controllers → `AbsFlexController*`.

## Execution: 4 commits, each builds green

1. Move kernel + pageable into flex (imports updated everywhere incl. test-application); full suite green.
2. Migrate test-application (Region → flex, eagerinit rewrite, new ITs written test-first; delete the v1 Mock*/Group/Item family and the superseded v2 fixtures/tests).
3. Delete v1/v2 + the two eager-init branches + `json-patch`.
4. README + version `5.0-SNAPSHOT`.

## Verification

- `mvn -pl library install` and `mvn -pl test-application test -Dtest=*,*IT` green after every commit.
- `grep -r "import by.nhorushko.crudgeneric" --include=*.java` over both modules shows only
  `by.nhorushko.crudgeneric.flex.*` imports; no source files remain outside `flex/` under
  `library/src/main/java/by/nhorushko/crudgeneric/`.

## Out of scope

- Migrating LocatorServer itself (separate effort; enabled by the README migration tables).
- Any behaviour changes in flex services/mappers.
