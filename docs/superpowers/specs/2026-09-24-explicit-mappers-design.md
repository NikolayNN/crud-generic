# Explicit mappers instead of ModelMapper — Design

**Date:** 2026-09-24
**Status:** approved

## Context

Every DTO ↔ entity conversion in the flex stack goes through ModelMapper. Only one direction is
actually explicit today: entity → read DTO, where `AbsMapEntityToDto` installs a converter that
calls the consumer's `create(entity)`. Everything else is implicit field matching that the owner
keeps fighting:

- **create DTO → entity** and **update DTO → entity** copy fields by name (STANDARD matching,
  field matching on, `skipNull`), then run the `mapSpecificFields` hook. What gets copied depends
  on accessor names, Lombok annotations and ModelMapper's matching rules, not on code the consumer
  wrote.
- **read DTO → entity** is registered by every `AbsFlexMapConfigDefault` only so that
  `AbsTypeMapChecker` and `updatePartial` are satisfied. `updatePartial` is called by nobody:
  not by the library controllers, not by LocatorServer, not by bi-dvr. LocatorServer's own mapper
  comments describe this map as dangerous on 14.x: implicit mapping would copy `owner` deep over
  the managed `UserEntity` and overwrite `areaM2`.
- **entity → entity self-map** is registered for "cloning" and used by nobody.
- **nested children** (e.g. `OrderCreateDto.lines` → `OrderEntity.lines`) cascade implicitly
  through whatever type maps happen to be registered; `nullifyZeroId` for children lives in a
  post-converter because that is the only hook ModelMapper fires for nested objects.
- Registration is a constructor side effect (`AbsMapBasic` calls `createTypeMap` in its
  constructor), which is why `AbsMapperEagerInitPostProcessor` exists: to force every mapper bean
  to be instantiated so its type map gets registered before the first `map()` call.
- `AbsMapperExtRelation` finds the relation field by reflection (first field whose type is `EXT`).

Consumers: LocatorServer (17 `AbsFlexMapConfigDefault`, 13 `AbsMapperExtRelation`, 11 v2 mappers,
138 direct `mapper.map(x, Y.class)` calls) and bi-dvr (4 mappers). Both are on `13.3.15-jakarta`
and have not migrated to 14.x, so they will move to this version in one pass and never have to
fight implicit mapping on 14.x at all.

## Decisions made with the owner

1. **Hard cut.** ModelMapper is removed from the library. No fallback: a pair that is not in the
   registry is an error at startup (checker) or at call time.
2. **`updatePartial` and the read DTO → entity map are removed**, together with `FieldCopyUtil`
   (reflection-based field copy used only there). PATCH is served by `patch(dto)` (registry-based)
   and `changeEntity(id, change)` (closure-based), both explicit.
3. **`AbsMapperExtRelation.setRelation` becomes abstract**; the reflective field search is deleted.
4. **API shape:** two interfaces `Mapper` / `Updater`, a `MapperRegistry`, and one
   `AbsFlexMapConfig` per entity with three abstract methods. Consumers never implement the
   interfaces directly for the CRUD triple; base classes with one abstract method cover view DTOs
   and children.
5. **Facade renamed** `AbsModelMapper` → `AbsMapper`. Method signatures stay, so the 138 direct
   call sites in LocatorServer do not change; only the constructor parameter type does.
6. **Version 15.0.** The 14.1 cycle has nothing in it (`## Не выпущено` is empty), so the first
   commit renames the cycle: `chore: start 15.0`.

## Target API

All mapper types live in `by.nhorushko.crudgeneric.flex.mapper`. The sub-packages `mapper.core`,
`mapper.composite` and `mapper.mapper` disappear.

### Core interfaces

```java
public interface Mapper<FROM, TO> {
    Class<FROM> fromClass();
    Class<TO> toClass();
    TO map(FROM from);

    static <F, T> Mapper<F, T> of(Class<F> from, Class<T> to, Function<F, T> fn);
}

public interface Updater<FROM, TO> {
    Class<FROM> fromClass();
    Class<TO> toClass();
    void update(FROM from, TO into);

    static <F, T> Updater<F, T> of(Class<F> from, Class<T> to, BiConsumer<F, T> fn);
}

/** A bean that contributes several mappers at once (AbsFlexMapConfig implements it). */
public interface MapperSource {
    Collection<Mapper<?, ?>> mappers();
    Collection<Updater<?, ?>> updaters();
}
```

Pair classes are declared explicitly (`fromClass()` / `toClass()`), exactly as today's base classes
take them in the constructor. Generic-parameter resolution via `ResolvableType` is deliberately
not used: it is another kind of magic and does not work for lambdas.

### `MapperRegistry`

- Built once from every `Mapper`, `Updater` and `MapperSource` bean; `MapperSource` beans are
  unrolled. Immutable afterwards except for the lookup cache.
- Two maps keyed by `(fromClass, toClass)`. Registering a pair twice fails construction with
  `IllegalStateException` naming the pair and both implementations.
- **Lookup** `mapper(Class<?> from, Class<T> to)`: exact key first, then `from.getSuperclass()`
  up to `Object`. Interfaces are not consulted (a class implementing two registered interfaces
  would be ambiguous). This is what makes Hibernate proxies and DTO subclasses resolve.
- **Lookup** `updater(Class<?> from, Class<?> to)`: walks the source chain as above and
  additionally the destination chain, because the destination is an existing instance that may be
  a Hibernate proxy (`getReference`).
- Resolved lookups are cached per `(runtime from, runtime to)` in a `ConcurrentHashMap`.
- A miss throws `MappingNotFoundException` (`flex.exception`, extends `RuntimeException`) with
  the direction and both fully qualified class names, e.g.
  `No Updater registered for com.x.OrderUpdateDto -> com.x.OrderEntity`.
- `find*` variants return `Optional` for the checker.

### Facade `AbsMapper` (replaces `AbsModelMapper`)

```java
public class AbsMapper {
    public AbsMapper(MapperRegistry registry, EntityManager entityManager);           // tests
    public AbsMapper(Supplier<MapperRegistry> registry, EntityManager entityManager); // Spring, lazy

    public <T> T map(Object source, Class<T> destinationType);   // null source → null
    public <T> T map(Object source, T destination);              // null source → destination unchanged; uses Updater
    public <T> List<T> mapAll(Collection<?> source, Class<T> destinationType); // null → null
    public <T extends AbstractEntity<?>> T reference(AbstractDto<?> dto, Class<T> entityClass);
    public <T extends AbstractEntity<?>> T referenceById(Object id, Class<T> entityClass);
    public EntityManager getEntityManager();
}
```

`getModelMapper()` is gone. The registry is resolved from the supplier on first use and memoised.
That is what breaks the bean cycle *mapper bean → AbsMapper → MapperRegistry → every mapper bean*:
`AbsMapper` never touches the registry during construction, so mapper beans keep injecting
`AbsMapper` in their constructors exactly as they inject `AbsModelMapper` today.

### Spring wiring (`flex.config`)

`AbsGenericCrudConfiguration` (imported by `@EnableAbsGenericCrud`, unchanged annotation):

| Bean | Depends on | Notes |
|---|---|---|
| `MapperRegistry` | `List<Mapper<?,?>>`, `List<Updater<?,?>>`, `List<MapperSource>` | Spring instantiates every mapper bean to satisfy the injection, lazy-init or not. |
| `AbsMapper` | `ObjectProvider<MapperRegistry>`, `EntityManager` | `new AbsMapper(provider::getObject, em)`. |
| `AbsMappingChecker` | `List<? extends AbsFlexServiceR>`, `MapperRegistry`, `AbsCrudCustomizer` | `SmartLifecycle`, phase `Integer.MAX_VALUE`, replaces `AbsTypeMapChecker`. |

`AbsMappingChecker.start()` checks, per service: `Mapper<ENTITY, READ_DTO>`; for
`AbsFlexServiceRUD` also `Updater<UPDATE_DTO, ENTITY>`; for `AbsFlexServiceCRUD` and
`AbsFlexServiceExtCRUD` also `Mapper<CREATE_DTO, ENTITY>`. It collects every missing pair and fails
once with `IllegalStateException` listing all of them (today it stops at the first). `isRunning`
is set only after the check passes, as today.

`AbsMapperEagerInitPostProcessor` is deleted. Why it is no longer needed: registration is no longer
a constructor side effect. The registry pulls every mapper bean through constructor injection.
`SmartLifecycle` beans are instantiated at refresh even under `spring.main.lazy-initialization=true`,
so the checker builds the registry, which builds every mapper, before `start()`. With the checker
disabled the registry is built on the first `map()` call and still sees every bean definition.

`AbsCrudCustomizer` keeps one flag, `mappingCheckerEnabled` (default `true`).
`typeMapCheckerEnabled` and `eagerTypeMapRegistration` are removed.

### Base classes for consumers

```java
public abstract class AbsFlexMapConfig<CREATE_DTO extends AbsBaseDto,
                                       UPDATE_DTO extends AbstractDto<?>,
                                       READ_DTO extends AbstractDto<?>,
                                       ENTITY extends AbstractEntity<?>> implements MapperSource {
    protected final AbsMapper mapper;
    public AbsFlexMapConfig(AbsMapper mapper, Class<CREATE_DTO> createDtoClass, Class<UPDATE_DTO> updateDtoClass,
                            Class<READ_DTO> readDtoClass, Class<ENTITY> entityClass);

    protected abstract ENTITY toEntity(CREATE_DTO dto);
    protected abstract void updateEntity(UPDATE_DTO dto, ENTITY entity);
    protected abstract READ_DTO toReadDto(ENTITY entity);

    /** Extra in-place updaters for PATCH bodies; default: none. */
    protected void patches(Patches<ENTITY> p) { }
}

public final class Patches<ENTITY> {
    public <P extends AbstractDto<?>> Patches<ENTITY> add(Class<P> patchClass, BiConsumer<P, ENTITY> apply);
}
```

`mappers()` returns `Mapper<CREATE_DTO, ENTITY>` (wraps `toEntity`, then calls
`entity.nullifyZeroId()` on the result) and `Mapper<ENTITY, READ_DTO>` (wraps `toReadDto`).
`updaters()` returns `Updater<UPDATE_DTO, ENTITY>` (wraps `updateEntity`) plus one `Updater` per
`patches()` entry. The "id 0 means new" normalisation stays: it is a documented library rule, not
implicit matching.

Standalone bases, each with one abstract method:

| Class | Implements | Abstract method | Notes |
|---|---|---|---|
| `AbsMapEntityToDto<ENTITY extends AbstractEntity<?>, DTO extends AbstractDto<?>>` | `Mapper<ENTITY, DTO>` | `DTO create(ENTITY from)` | Same contract as today, ModelMapper internals removed. |
| `AbsMapDtoToEntity<DTO extends AbsBaseDto, ENTITY extends AbstractEntity<?>>` | `Mapper<DTO, ENTITY>` | `ENTITY create(DTO from)` | `map()` = `create()` + `nullifyZeroId()`. Replaces `AbsMapBasic`, `AbsMapBaseDtoToEntity`, `AbsMapCreateDtoToEntity`. |
| `AbsMapUpdateDtoToEntity<DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>>` | `Updater<DTO, ENTITY>` | `void update(DTO from, ENTITY into)` | |
| `AbsMapperExtRelation<DTO extends AbsCreateDto, ENTITY, EXT_ID, EXT extends AbstractEntity<?>>` | — | `void setRelation(ENTITY target, EXT relation)` | `map(extId, dto)` / `mapAll` unchanged: `mapper.map(dto, entityClass)` + `referenceById`. `FieldUtils` search deleted. |

All constructors take `AbsMapper` first, then the pair classes, as today.

Example (test-application `OrderMapConfig`):

```java
@Component
public class OrderMapConfig extends AbsFlexMapConfig<OrderCreateDto, OrderUpdateDto, OrderDto, OrderEntity> {
    public OrderMapConfig(AbsMapper mapper) {
        super(mapper, OrderCreateDto.class, OrderUpdateDto.class, OrderDto.class, OrderEntity.class);
    }
    @Override protected OrderEntity toEntity(OrderCreateDto dto) {
        OrderEntity e = new OrderEntity();
        e.setName(dto.getName());
        e.setLines(mapper.mapAll(dto.getLines(), OrderLineEntity.class)); // nested children: explicit
        return e;
    }
    @Override protected void updateEntity(OrderUpdateDto dto, OrderEntity e) { e.setName(dto.getName()); }
    @Override protected OrderDto toReadDto(OrderEntity e) { return new OrderDto(e.getId(), e.getName()); }
    @Override protected void patches(Patches<OrderEntity> p) {
        p.add(OrderNamePatch.class, (patch, e) -> e.setName(patch.name()));
    }
}
```

### Services (`flex.service`)

- `AbsFlexServiceR`: field type `AbsMapper`; `mapReadDto` / `mapAllReadDto` unchanged.
- `AbsFlexServiceRUD`:
  - `update(UPDATE_DTO)` unchanged in signature; the in-place `mapper.map(dto, entity)` now
    resolves `Updater<UPDATE_DTO, ENTITY>`.
  - **New** `public READ_DTO patch(AbstractDto<ENTITY_ID> body)`: same private `runUpdate`
    pipeline as `update` (id check, `beforeUpdateHook`, `AbsUpdateChangesHookable` hooks, load,
    `mapper.map(body, entity)`, save, `afterUpdateHook`). Needs an `Updater<body class, ENTITY>`;
    otherwise `MappingNotFoundException`.
  - **New** `protected READ_DTO changeEntity(ENTITY_ID id, Consumer<ENTITY> change)`: load or
    `AppNotFoundException`; if the service is `AbsUpdateChangesHookable`, snapshot
    `previous = mapReadDto(entity)` before the change; apply `change`; `repository.save`;
    `current = mapReadDto`; `afterUpdateHook(current)`; hookable `afterUpdateHook(previous, current)`.
    The DTO-taking before-hooks are skipped because there is no DTO; the javadoc says so.
  - **Removed**: `updatePartial`, `copyPartial`, `IGNORE_PARTIAL_UPDATE_PROPERTIES`,
    `mapEntity(Object)`, `mapAllEntities(Collection<?>)`.
- `AbsFlexServiceCRUD`: **new** `protected ENTITY mapEntity(CREATE_DTO)` and
  `protected List<ENTITY> mapAllEntities(Collection<CREATE_DTO>)` (typed, moved here from RUD).
  `save` / `saveAll` / `persistOrMerge` unchanged.
- `AbsFlexServiceExtCRUD`, `AbsFlexPagingAndSortingService`, all `AbsFlexController*`: only the
  `AbsMapper` type.
- `AbsUpdateChangesHookable` unchanged (`beforeUpdateHook(READ_DTO previous, AbstractDto<ENTITY_ID> current)`
  already fits patch bodies).

### Error behaviour

| Situation | Exception | When |
|---|---|---|
| Pair missing from registry | `MappingNotFoundException` (direction + both FQCNs) | call time |
| Same pair registered twice | `IllegalStateException` (pair + both implementations) | registry construction |
| Service without its mappers | `IllegalStateException` listing every missing pair | context start (`AbsMappingChecker`) |

## Deleted

Library main: `AbsModelMapper`, `mapper.core.AbsMapBasic`, `mapper.core.AbsMapBaseDtoToEntity`,
`mapper.core.AbsMapDtoToEntity` (old), `mapper.core.RegisterableMapper`,
`mapper.AbsMapCreateDtoToEntity`, `mapper.composite.AbsFlexMapConfigAbstract`,
`mapper.composite.AbsFlexMapConfigDefault`, `config.AbsMapperEagerInitPostProcessor`,
`config.AbsTypeMapChecker`, `util.FieldCopyUtil`. Dependency `org.modelmapper:modelmapper` removed
from `library/pom.xml`. `commons-lang3` stays (`filterspecification`, `PageFilterRequest`).

Library tests: `AbsModelMapperInPlaceMapTest`, `AbsMapperEagerInitPostProcessorTest`,
`AbsCrudCustomizerEagerFlagTest`, `AbsMapBasicRegistrationTest`, `AbsFlexMapConfigSharedEntityTest`,
`AbsMapBaseDtoToEntityNullifyZeroIdTest`, `AbsTypeMapCheckerTest` (the last two are rewritten
under new names, see below).

test-application: `config/ModelMapperConfig`, `eagerinit/*` (three tests), `util/FieldCopyUtilTest`.

## test-application migration

- `OrderMapConfig`, `RegionMapConfig`, `TaskMapConfig` → `AbsFlexMapConfig` with explicit
  `toEntity` / `updateEntity` / `toReadDto`; Order maps `lines` through `mapper.mapAll` and
  declares `OrderNamePatch` in `patches()`.
- `OrderLineMapConfig` → `OrderLineMapper extends AbsMapDtoToEntity` (create direction only; no IT
  maps `OrderLineEntity` back to `OrderLineDto`, so no read-direction mapper is registered).
- `MeetingMapper`, `OrderViewMapper`: constructor parameter type only.
- `TaskExtMapper`: implements `setRelation(TaskEntity task, ProjectEntity project)`.
- Services: constructor parameter type. `OrderServiceCRUD` gains `rename(id, name)` built on
  `changeEntity`, used by the new IT.
- New IT `FlexPatchIT`: `patch(OrderNamePatch)` goes through the registered `Updater` and the
  update hooks; `rename` goes through `changeEntity`; a patch body with no `Updater` fails with
  `MappingNotFoundException`.
- New IT `MapperRegistryLazyInitIT`: context started with `spring.main.lazy-initialization=true`
  has every mapper of the demo in the registry and the checker passes (replaces `eagerinit/*`).
- `FlexSaveOverridingMapperIT`: the ModelMapper `customizeTypeMap` override becomes a
  `Mapper.of(ZeroIdOrderCreate.class, OrderEntity.class, ...)` bean that copies id `0` verbatim;
  the assertion (persistOrMerge still inserts) is unchanged.
- `FlexTwoConfigsForSameEntityIT`: unchanged in intent; two configs for one entity register
  distinct pairs and do not collide.
- Every other IT (`FlexSaveCascadeIT`, `FlexUpdateIT`, `FlexDeleteIT`, `FlexExtSaveIT`,
  `FlexAssignedIdSaveIT`, `MeetingPage*IT`) stays as is. Their staying green is the proof that
  service behaviour did not change.

## Library unit tests (new or rewritten)

- `MapperRegistryTest`: exact lookup; superclass walk (registered for `Base`, instance of
  `Sub extends Base` resolves); updater walks the destination chain too; interfaces are not
  consulted; duplicate pair fails with both names; miss throws `MappingNotFoundException` with
  the direction; `MapperSource` is unrolled; two sources sharing an entity with different DTOs
  coexist.
- `AbsMapperTest`: null handling of all three `map*` methods; `map(from, into)` delegates to the
  `Updater` and returns `into`; the registry supplier is called once.
- `AbsFlexMapConfigTest`: exposes exactly three adapters with the declared classes plus one per
  `patches()` entry; `toEntity` result with id `0` comes back with `null` id.
- `AbsMapDtoToEntityNullifyZeroIdTest`: rewrite of the existing test on the new base.
- `AbsMappingCheckerTest`: reports every missing pair in one exception; skips when disabled;
  `isRunning` is false after a failed start.
- `AbsGenericCrudConfigurationTest`: the three beans exist, no ModelMapper bean, `AbsMapper`
  is constructible before the registry is.
- `AbsFlexServiceRUDPatchTest`: `patch` runs the same hooks as `update`; missing `Updater`
  surfaces as `MappingNotFoundException`; `changeEntity` applies the change, saves, runs the
  after-hooks and skips the DTO before-hooks.
- `AbsMapperExtRelationTest`: `map(extId, dto)` calls the abstract `setRelation` with the
  reference.

## pom, docs, version

- `library/pom.xml`: drop `org.modelmapper:modelmapper`.
- Version: `mise exec -- mvn -q versions:set -DnewVersion=15.0 -DgenerateBackupPoms=false`,
  commit `chore: start 15.0` (cycle rename, nothing released as 14.1).
- README: features list drops ModelMapper; Step 4 shows the explicit `AbsFlexMapConfig`; new
  section "Migration to 15.0 (explicit mappers)" with a table old → new, the PATCH options
  (`patch(dto)` + `patches()`, `changeEntity`), the `AbsCrudCustomizer` flag rename, and a note
  that consumers on 13.3.15 apply the flex-only table and this one in one pass.
- CHANGELOG `## Не выпущено`: the change, in Russian, following the 14.0 entry style.
- CLAUDE.md: one line — mapping is explicit through the registry; no ModelMapper, no reflection
  in mapping.

## Execution: 4 commits, each `./mvnw -B verify` green

1. `chore: start 15.0` — version only.
2. `feat(mapper): explicit Mapper/Updater registry` — additive: `Mapper`, `Updater`,
   `MapperSource`, `MapperRegistry`, `Patches`, `MappingNotFoundException` and their unit tests.
   ModelMapper still present; nothing uses the registry yet.
3. `feat!: replace ModelMapper with explicit mappers` — `AbsMapper`, `AbsFlexMapConfig`, the
   rewritten bases, `AbsMappingChecker`, configuration, services (`patch`, `changeEntity`, typed
   `mapEntity`), all deletions incl. the dependency, test-application migration and the new ITs.
   One commit because the old `AbsMapEntityToDto` / `AbsMapUpdateDtoToEntity` names are reused
   in the same package and test-application cannot compile against both.
4. `docs: README, CHANGELOG and CLAUDE.md for explicit mappers`.

## Verification

- `./mvnw -B verify` green after every commit (unit + failsafe ITs).
- `grep -ri modelmapper library/src test-application/src library/pom.xml` → empty.
- `grep -rn "FieldUtils\|java.lang.reflect" library/src/main/java/by/nhorushko/crudgeneric/flex` → empty.
- `grep -rn "updatePartial\|FieldCopyUtil\|RegisterableMapper\|AbsModelMapper" library/src test-application/src` → empty.

## Out of scope

- Migrating LocatorServer and bi-dvr (separate effort per consumer, enabled by the README tables).
- Checker coverage for `AbsFlexPagingAndSortingService` (its `toDto` may be overridden, so a
  missing pair there is not necessarily an error).
- Any change in `filterspecification`, `pageable`, controllers, or the `commons-lang3` dependency.
