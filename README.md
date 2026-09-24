# Generic CRUD Framework for Spring Boot

The Generic CRUD Framework simplifies the development of Spring Boot applications by providing a structured approach to mapping Data Transfer Objects (DTOs) to entities and implementing CRUD operations. Mapping is explicit: every conversion is code you write, found by a registry through the exact pair of classes — nothing is copied by matching field names.

## Features
* Explicit DTO to entity mappings and vice versa: one config per entity, no implicit field copying.
* A mapper registry that fails loudly on a missing or duplicate pair of classes.
* Extended support for CRUD operations on entities with direct and related entities.
* Predefined hooks for custom business logic before and after CRUD operations.
* Ready-Made CRUD Services and Controllers: Enables quick generation of fully functional CRUD services and controllers with minimal coding required. This feature allows developers to focus on business logic and application-specific requirements by leveraging generic patterns and practices for common CRUD operations.

## Getting Started

### Prerequisites

* JDK 25+
* Spring Boot 3.5+

### Installation via Maven

The library is published to GitHub Packages. **GitHub Packages requires authentication even
for reading public packages**, so a token is needed once per machine and per CI runner.

1. Create a personal access token with the `read:packages` scope.
2. Add it to `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github-crud-generic</id>
      <username>YOUR_GITHUB_LOGIN</username>
      <password>YOUR_TOKEN_WITH_read:packages</password>
    </server>
  </servers>
</settings>
```

3. Add the repository and the dependency to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github-crud-generic</id>
        <url>https://maven.pkg.github.com/NikolayNN/crud-generic</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>by.nhorushko</groupId>
        <artifactId>crud-abstract-generic</artifactId>
        <version>15.0</version>
    </dependency>
</dependencies>
```

The `<id>` in `settings.xml` and in `<repositories>` must match.

Released versions are listed at https://github.com/NikolayNN/crud-generic/packages

**Migrating from JitPack (versions up to 13.3.15-jakarta):** the coordinates changed from
`com.github.NikolayNN:crud-generic` to `by.nhorushko:crud-abstract-generic`, and the
`jitpack.io` repository is no longer needed. Java imports do not change — including
`by.nhorushko.filterspecification.*`, whose sources now live in this library.

## Usage Guide

### Step 1: Enable the Generic CRUD Framework
Before diving into the specifics of entity and DTO creation, enable the framework in your Spring Boot application by using the @EnableAbsGenericCrud annotation. It registers the mapper registry and the `AbsMapper` facade the services map through.

Add @EnableAbsGenericCrud to your Spring Boot application's main class or any configuration class:

```java
@SpringBootApplication
@EnableAbsGenericCrud
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}

```

### Step 2: Define Your Entities

Entities implement AbstractEntity<?> (from `by.nhorushko.crudgeneric.flex.model`), where ? is the type of your identifier (e.g., Long).

```java
@Entity
public class MyEntity implements AbstractEntity<Long> {
    // Entity definition
}
```

### Step 3: Create DTOs

Define your DTOs for create, read, and update operations. Create DTOs implement AbsCreateDto, update DTOs implement AbsUpdateDto<?>, read DTOs implement AbstractDto<?> (all from `by.nhorushko.crudgeneric.flex.model`).

```java
public class MyCreateDto implements AbsCreateDto {
    // Fields specific to creation
}

public class MyReadDto implements AbstractDto<Long> {
    // Fields for reading
}

public class MyUpdateDto implements AbsUpdateDto<Long> {
    // Fields for updating
}
```

### Step 4: Implement Mapping Configurations
Extend `AbsFlexMapConfig` — one config per entity. Every field is written explicitly; nothing is copied by matching names. Declare one `Updater` per PATCH body class in `patches()`.

```java
@Component
public class MyMappingConfig extends AbsFlexMapConfig<MyCreateDto, MyUpdateDto, MyReadDto, MyEntity> {

    public MyMappingConfig(AbsMapper mapper) {
        super(mapper, MyCreateDto.class, MyUpdateDto.class, MyReadDto.class, MyEntity.class);
    }

    @Override
    protected MyEntity toEntity(MyCreateDto dto) {
        MyEntity entity = new MyEntity();
        entity.setName(dto.getName());
        if (dto.getItems() != null) { // mapAll(null) is null: decide what a null list means
            entity.setItems(mapper.mapAll(dto.getItems(), MyItemEntity.class)); // nested children: explicit
        }
        return entity;
    }

    @Override
    protected void updateEntity(MyUpdateDto dto, MyEntity entity) {
        entity.setName(dto.getName());
    }

    @Override
    protected MyReadDto toReadDto(MyEntity entity) {
        return new MyReadDto(entity.getId(), entity.getName());
    }

    @Override
    protected void patches(Patches<MyEntity> p) {
        p.add(MyNamePatch.class, (patch, entity) -> entity.setName(patch.name()));
    }
}
```

For a single direction use `AbsMapEntityToDto` (view DTOs, read-only services), `AbsMapDtoToEntity` (nested children) or `AbsMapUpdateDtoToEntity`; for a one-off pair declare a bean `Mapper.of(From.class, To.class, fn)`. A missing pair fails with `MappingNotFoundException` on the first call; a pair registered twice fails at startup.

### Step 5: Create Services
Extend AbsFlexServiceCRUD or AbsFlexServiceExtCRUD for CRUD services. Implement abstract methods and use the provided functionalities.

```java
@Service
public class MyEntityService extends AbsFlexServiceCRUD<Long, MyEntity, MyReadDto, MyUpdateDto, MyCreateDto, MyRepository> {
    // Constructor and methods
}
```

`update(dto)`, `patch(id, body)` and the protected `changeEntity(id, change)` all load the entity through `loadForUpdate(id)` and store it through `saveUpdated(entity)` — override those two seams instead of `update`.

### Step 6: Develop Controllers
Extend AbsFlexControllerCRUD or AbsFlexControllerExtCRUD for CRUD operations in your controller.

```java
@RestController
@RequestMapping("/my-entity")
public class MyEntityController extends AbsFlexControllerCRUD<Long, MyReadDto, MyReadDtoView, MyUpdateDto, MyCreateDto, MySettings, MyEntityService> {
    // Constructor and endpoint methods
}

```

## Миграция на 15.0 (явные мапперы)

15.0 удаляет ModelMapper. Каждое преобразование DTO ↔ entity — код потребителя, который
`MapperRegistry` находит по точной паре классов. Ничего не копируется по совпадению имён
полей, и `null` сам по себе ничего не значит.

**Порядок для потребителя на `13.3.15-jakarta`:** сначала хвост v1/v2 переводится на flex на
той же 13.3.15 (таблица «Migration to 5.0 (flex-only)» ниже), затем 13.3.15 → 15.0 одним шагом
по таблице этого раздела. **Через 14.0 не ходить:** там `updatePartial` неявно кладёт read DTO
на managed-сущность, и через этот путь пошли бы все PATCH. Окно без компиляции сокращается
подготовкой на 13.3.15: переопределить `setRelation` во всех наследниках `AbsMapperExtRelation`
(метод там уже protected) и сделать явными поля, которые сегодня копируются по имени.

Таблица составлена по API 13.3.15, на котором сидят потребители, а не по 14.0.

### Было (13.3.15) → стало (15.0)

| 13.3.15 | 15.0 |
|---|---|
| `AbsModelMapper` | `AbsMapper`, тот же пакет `by.nhorushko.crudgeneric.flex`. `map(x, Y.class)`, `mapAll`, `reference`, `referenceById`, `getEntityManager()` не меняются — меняется тип параметра конструкторов |
| `AbsModelMapper.getModelMapper()` | удалён; каждое прямое обращение к ModelMapper становится `Mapper` / `Updater` |
| in-place `map(source, destination)` — только в 14.x; в 13.3.15 — `getModelMapper().map(source, destination)` | `update(source, destination)` через зарегистрированный `Updater`; `null`-приёмник — `NullPointerException` (14.x возвращал `null`) |
| `AbsFlexMapConfigDefault`, `AbsFlexMapConfigAbstract` | `AbsFlexMapConfig`: `toEntity`, `updateEntity`, `toReadDto` и необязательный `patches(p)` |
| фабрика `mapperCreateDtoToEntity(...)` + `mapSpecificFieldsCreateDtoToEntity(mapper, dto, entity)` поверх неявного копирования | `toEntity(dto)`: `new Entity()` + те же строки + поля, которые раньше копировались по имени, включая `id`, если он нужен (assigned id, sentinel `0`) |
| фабрика `mapperUpdateDtoToEntity(...)` + `mapSpecificFieldsUpdateDtoToEntity` | `updateEntity(dto, entity)` на managed-сущности |
| `AbsMapUpdateDtoToPresetEntity` из `mapperUpdateDtoToEntity(...)` | `updateEntity(dto, entity)`; фабрика исчезает |
| `handleAfterMapSpecificFields(source, received)`: `entityManager.find` + перенос полей | загрузка — `loadForUpdate(id)` сервиса (по умолчанию `findById`; переопределяется ради fetch join или фильтра), перенос — тело `updateEntity` |
| `mergeActualAndReceived(actual, received)` | то же тело в `updateEntity`, только источник — DTO, а не `received`; отсоединённой копии больше нет |
| `mapperReadDtoToEntity(...)`, `AbsMapDtoToPresetEntity` для read DTO, `mapSpecificFieldsReadDtoToEntity` | удалены; тела PATCH — `patches()`, связь из read DTO — `mapper.reference(dto, Entity.class)` |
| `mapperEntityToReadDto(...)` + `createReadDtoFromEntity(mapper, entity)` | `toReadDto(entity)`; маппер доступен полем `mapper` |
| `mapperEntityToEntity(...)` (self-map, движок `mergeActualAndReceived` по умолчанию) | удалён без замены |
| `AbsMapBasic`, `AbsMapBaseDtoToEntity`, `AbsMapCreateDtoToEntity`, `mapper.core.AbsMapDtoToEntity` + `mapSpecificFields` / `handleAfterMapSpecificFields` | `AbsMapDtoToEntity` (`flex.mapper`): `create(dto)`; `map()` = `create()` + `nullifyZeroId()` |
| `AbsMapEntityToDto` | то же имя и тот же `create(entity)`; внутренности ModelMapper удалены |
| `AbsMapUpdateDtoToEntity` + `mapSpecificFields` | то же имя, абстрактный `update(dto, entity)` |
| `customizeTypeMap(TypeMap)`, `RegisterableMapper` | бин `Mapper.of(From.class, To.class, fn)` или своя реализация `Mapper` / `Updater` |
| `mapper.mapper.AbsMapperExtRelation`, `setRelation` по умолчанию ищет поле рефлексией | `mapper.AbsMapperExtRelation`, `setRelation(target, relation)` абстрактный |
| `updatePartial(id, partial)`, `IGNORE_PARTIAL_UPDATE_PROPERTIES`, `FieldCopyUtil` | `patch(id, body)` + `Updater` на каждый partial-класс в `patches()` конфига; изменения в коде — `changeEntity(id, change)` |
| переопределённый `update(dto)` с `mapEntity(dto)` ради `saveAndFlush` или своей загрузки | швы `saveUpdated(entity)` / `loadForUpdate(id)` |
| `mapEntity(Object)`, `mapAllEntities(Collection<?>)` в `AbsFlexServiceRUD` | `mapEntity(CREATE_DTO)`, `mapAllEntities(Collection<CREATE_DTO>)` в `AbsFlexServiceCRUD` |
| `beforeUpdateHook(AbstractDto<ID>)`, звался и из `updatePartial` | `beforeUpdateHook(UPDATE_DTO)` — только из `update`; у `patch` свой `beforePatchHook(id, body)` |
| `AbsUpdateChangesHookable.beforeUpdateHook(previous, AbstractDto<ID> current)` | `current` — `Object`: update DTO или тело патча |
| `AbsTypeMapChecker`, `AbsCrudCustomizer` (`typeMapCheckerEnabled`, `eagerTypeMapRegistration`), `AbsMapperEagerInitPostProcessor` | удалены, см. «Стартового чекера нет» |
| бин `absGenericCrudModelMapper`, свой `@Primary ModelMapper` | не нужны; зависимость `org.modelmapper:modelmapper` из библиотеки убрана — если приложение само пользуется ModelMapper, объявите её у себя |

### Preset-маппер → `updateEntity`

```java
// 13.3.15: маппер сам находит строку и переносит поля с отсоединённой копии
@Override
protected AbsMapBasic<MyUpdateDto, MyEntity> mapperUpdateDtoToEntity(
        AbsModelMapper mapper, Class<MyUpdateDto> updateDtoClass, Class<MyEntity> entityClass) {
    return new AbsMapUpdateDtoToPresetEntity<>(mapper, updateDtoClass, entityClass) {
        @Override
        protected MyEntity mergeActualAndReceived(MyEntity actual, MyEntity received) {
            actual.setName(received.getName());
            actual.setDescription(received.getDescription());
            return actual;
        }
    };
}

// 15.0: строку загружает сервис (loadForUpdate), конфиг пишет только поля запроса
@Override
protected void updateEntity(MyUpdateDto dto, MyEntity entity) {
    entity.setName(dto.getName());
    entity.setDescription(dto.getDescription());
}
```

### PATCH

```java
// 13.3.15
service.updatePartial(id, new PartialName(name));

// 15.0: тот же вызов под новым именем, partial-класс остаётся…
service.patch(id, new PartialName(name));

// …плюс Updater на каждый partial-класс в конфиге сущности
@Override
protected void patches(Patches<MyEntity> p) {
    p.add(PartialName.class, (body, entity) -> entity.setName(body.getName()));
}

// Изменение, которое пишет код, а не тело запроса
public MyReadDto archive(Long id) {
    return changeEntity(id, entity -> entity.setArchivedAt(Instant.now()));
}
```

Тело без зарегистрированного `Updater` — `MappingNotFoundException`, ничего не сохраняется.
Собственные read, update и create DTO сервиса телом патча быть не могут: `patch` отвергает их
`IllegalArgumentException` до хуков. Для update DTO — `update(dto)`.

### Швы записи

```java
// 13.3.15: update переопределён целиком ради saveAndFlush
@Override
public MyReadDto update(MyUpdateDto dto) {
    return mapReadDto(repository.saveAndFlush(mapEntity(dto)));
}

// 15.0: только шов, и он работает для update, patch и changeEntity
@Override
protected MyEntity saveUpdated(MyEntity entity) {
    return repository.saveAndFlush(entity); // значения, посчитанные базой, попадают в ответ
}
```

### Правила

- **Null.** Библиотека null не трактует. `toEntity`, `updateEntity` и `patches()` пишут ровно
  то, что написано: `e.setName(dto.getName())` очищает поле, `if (dto.getName() != null)
  e.setName(dto.getName())` сохраняет. До 15.0 null не очищал поле ни на одном пути записи
  (`skipNull` у `update`, пропуск при маппинге в сущность у `updatePartial`), поэтому при
  переносе каждого update DTO и partial-класса решение «не трогать» или «очистить»
  принимается на каждое поле. Фасад особо обрабатывает только null-источник:
  `map(null, X.class)` → `null`, `update(null, target)` → `target` без изменений.
- **Точный ключ.** Реестр ищет пару только по точным классам: ни суперклассы, ни интерфейсы
  не рассматриваются. Для иерархии DTO нужна пара на каждый конкретный класс: в LocatorServer —
  `StepAddress` / `StepWarehouse` / `StepTransit` (пара на абстрактный `Step` не нужна:
  экземпляра с таким runtime-классом не бывает); в цепочках
  `Carrier extends CarrierUpdate extends CarrierCreate` (так же Skill и Warehouse) каждый класс
  получает свою пару — маппер `CarrierCreate` не подхватит `CarrierUpdate`. Единственное
  исключение — JPA-прокси: класс без `@Entity` с `@Entity`-предком приводится к ближайшему
  такому предку. Поэтому и написанный руками наследник сущности без `@Entity` маппится парой
  сущности, а пару для него самого зарегистрировать нельзя — это ошибка старта. Ленивая ссылка на полиморфную сущность — прокси объявленного базового класса;
  чтобы маппить её по конкретному наследнику, сначала `Hibernate.unproxy(...)`.
- **Иерархии сущностей (`@Inheritance`).** Правило точного ключа действует и на сущности:
  runtime-класс загруженной строки — конкретный `@Entity`-наследник, и у него свой ключ.
  `Mapper<StepEntity, Step>` не подхватит загруженный `StepAddressEntity`: нужна пара на каждый
  конкретный класс сущности — `Mapper.of(StepAddressEntity.class, Step.class, …)`, так же для
  `StepWarehouseEntity` и `StepTransitEntity`, и `Updater` на каждый, если его обновляют (то же
  для наследников `JobHistoryEntity`). `AbsFlexMapConfig` с одним `ENTITY` и `AbsFlexServiceRUD`
  на абстрактной базе такие иерархии не покрывают. Ленивая ссылка `@ManyToOne StepEntity` —
  прокси объявленного базового класса, он приводится к `StepEntity`: либо сначала
  `Hibernate.unproxy(...)`, и тогда сработает пара конкретного класса, либо зарегистрируйте пару
  базового класса, если маппинг обходится его полями.
- **Связь из read DTO — через `reference`.** `map(driverDto, DriverEntity.class)` ради связи
  заменяется на `mapper.reference(driverDto, DriverEntity.class)`: `getReference` по id без
  копирования полей. Пары read DTO → entity в 15.0 нет.
- **Read DTO не делит изменяемое состояние с сущностью.** `previous` в
  `AbsUpdateChangesHookable.afterUpdateHook` маппится из управляемой сущности до изменения, без
  глубокой копии. Если `toReadDto` / `create` отдаёт её коллекцию, `Date` или embeddable, а
  изменение правит их на месте (`getLines().clear(); addAll(...)`), снимок «до» совпадёт с «после».
  Копируйте: `List.copyOf(...)`, `mapper.mapAll(...)` в DTO детей, `Instant` вместо `Date`.
- **Не маппить в конструкторах и `@PostConstruct`.** Первый `map()` собирает реестр, а сборка
  создаёт все бины-мапперы. Если это случится при создании бина, от которого зависит хотя бы
  один маппер, Spring бросит `BeanCurrentlyInCreationException`. Прогревы кэшей — в
  `ApplicationReadyEvent` или `SmartLifecycle`.
- **Проверки на все пути записи — в `loadForUpdate`.** `beforeUpdateHook(UPDATE_DTO)`
  вызывается только из `update`; `patch` зовёт `beforePatchHook(id, body)`, `changeEntity` —
  ни тот, ни другой. В 13.3.15 `updatePartial` проходил через `beforeUpdateHook`. Запрет,
  который должен закрывать все пути (например, правка глобального навыка в
  `LogisticSkillService`), переносится в переопределённый `loadForUpdate`.
- **Стартового чекера нет.** `AbsTypeMapChecker`, `AbsCrudCustomizer` и
  `AbsMapperEagerInitPostProcessor` удалены: пара без маппера всплывает
  `MappingNotFoundException` при первом вызове с направлением и обоими классами, дубль пары
  ломает старт. Тесты, которые выключали чекер бином `AbsCrudCustomizer`, просто теряют эту
  конфигурацию; мокать flex-сервисы можно без оговорок. Нужна проверка состава пар на старте —
  свой `SmartLifecycle` поверх публичных `MapperRegistry.findMapper` / `findUpdater`.
- **AOP на `updatePartial`.** Pointcut'ы на `updatePartial` после переезда молча перестанут
  срабатывать (в LocatorServer — `TrackerCertificateExpireCacheAspect`): перенацельте их на
  `patch`.

## Migration to 5.0 (flex-only)

Version 5.0 removes the legacy v1 (`by.nhorushko.crudgeneric.*` root packages) and v2
(`by.nhorushko.crudgeneric.v2.*`) stacks. Only `by.nhorushko.crudgeneric.flex.*` remains.

### Moved classes (import rename only — behaviour unchanged)

| 4.x import | 5.0 import |
|---|---|
| `by.nhorushko.crudgeneric.v2.domain.AbstractDto` | `by.nhorushko.crudgeneric.flex.model.AbstractDto` |
| `by.nhorushko.crudgeneric.v2.domain.AbstractEntity` | `by.nhorushko.crudgeneric.flex.model.AbstractEntity` |
| `by.nhorushko.crudgeneric.v2.domain.IdEntity` | `by.nhorushko.crudgeneric.flex.model.IdEntity` |
| `by.nhorushko.crudgeneric.domain.SettingsVoid` | `by.nhorushko.crudgeneric.flex.model.SettingsVoid` |
| `by.nhorushko.crudgeneric.domain.SettingsTranslateable` | `by.nhorushko.crudgeneric.flex.model.SettingsTranslateable` |
| `by.nhorushko.crudgeneric.exception.AppNotFoundException` | `by.nhorushko.crudgeneric.flex.exception.AppNotFoundException` |
| `by.nhorushko.crudgeneric.exception.AuthenticationException` | `by.nhorushko.crudgeneric.flex.exception.AuthenticationException` |
| `by.nhorushko.crudgeneric.util.FieldCopyUtil` | `by.nhorushko.crudgeneric.flex.util.FieldCopyUtil` |
| `by.nhorushko.crudgeneric.util.PageableUtils` | `by.nhorushko.crudgeneric.flex.util.PageableUtils` |
| `by.nhorushko.crudgeneric.v2.pageable.PageFilterRequest` | `by.nhorushko.crudgeneric.flex.pageable.PageFilterRequest` |
| `by.nhorushko.crudgeneric.v2.pageable.FilterGroupBuilder` | `by.nhorushko.crudgeneric.flex.pageable.FilterGroupBuilder` |
| `by.nhorushko.crudgeneric.v2.pageable.AbsFlexPagingAndSortingService` | `by.nhorushko.crudgeneric.flex.pageable.AbsFlexPagingAndSortingService` |
| `by.nhorushko.crudgeneric.v2.controller.BasePageRequest` | `by.nhorushko.crudgeneric.flex.pageable.BasePageRequest` |

### Removed classes and their flex replacements

| Removed (v1/v2) | Replace with |
|---|---|
| `ImmutableGenericService`, `CrudGenericService`, `CrudAdditionalGenericService`, `CrudExpandGenericService`, `RudGenericService`, `PartialDtoGenericService`, `v2.AbsServiceR/RUD/CRUD` | `AbsFlexServiceR` / `AbsFlexServiceRUD` / `AbsFlexServiceCRUD` |
| `v2.AbsServiceExtCRUD` | `AbsFlexServiceExtCRUD` |
| `ImmutableDtoAbstractMapper`, `AbstractMapper`, `v2.AbsMapperDto/AbsMapperEntityDto/AbsMapperEntityExtDto/AbsMapperBase` | `AbsFlexMapConfigDefault` (one config per entity registers create/update/read maps) |
| v1 `*RestController`, `v2.AbsControllerR/RU/RUD/CRUD/ExtCRUD` | `AbsFlexControllerR` / `AbsFlexControllerRU` / `AbsFlexControllerRUD` / `AbsFlexControllerCRUD` / `AbsFlexControllerExtCRUD` |
| `v2.pageable.AbsPagingAndSortingService` (deprecated), `v2.pageable.AbsFilterSpecification` + per-entity `FilterSpecification*` subclasses | `AbsFlexPagingAndSortingService` + one `filterFields(builder)` declaration (see “Pageable in 5.0” below) |
| `PagingAndSortingImmutableGenericService`, `PageableGenericRestController` | `AbsFlexPagingAndSortingService` + your own controller endpoint building a `PageFilterRequest` |
| `SpecificationUtils` | Removed with no direct replacement — compose `org.springframework.data.jpa.domain.Specification` instances directly, or use `AbsFlexPagingAndSortingService` + `PageFilterRequest` for filter-driven paging |

Notes:
- Flex `save()` is an upsert (`persistOrMerge`): id `null`/`0` or an absent assigned id inserts; an existing id updates.
- `delete(missingId)` is a silent no-op (idempotent).
- The sentinel id `0` is treated as "new" and normalised to `null` on every save path.

### Pageable in 5.0 — declarative FilterFields

`AbsFilterSpecification` (the path/type map base class) and the per-service `buildSpecification` switch are
replaced by a single declaration. Per entity: delete the `FilterSpecification*` class, fold its
map and the switch into `filterFields(builder)`, drop the `toDto` override if the default
mapper-based one suffices:

```java
@Service
public class RtRoutePageableService
        extends AbsFlexPagingAndSortingService<Long, RtRoute, RtRouteEntity> {

    public RtRoutePageableService(RtRouteRepository repository, AbsMapper mapper) {
        super(repository, mapper, RtRoute.class);
    }

    @Override
    protected FilterFields<RtRouteEntity> filterFields(FilterFields.Builder<RtRouteEntity> f) {
        return f.string("name", CONTAINS)
                .string("description", CONTAINS)
                .ofLong("userId", "user.id", EQUAL)
                .instant("archivedAt", GREATER_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN, LESSTHAN_OR_EQUAL_TO, BETWEEN, IS_NULL, NOT_NULL)
                .instant("createdTime", GREATER_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN, LESSTHAN_OR_EQUAL_TO, BETWEEN)
                .ofBoolean("oneOff", EQUAL)
                .build();
    }
}
```

- Operation validation is always on: unknown field, disallowed operation, unconvertible value
  or malformed sort raise `FilterValidationException` — map it to HTTP 400 in your exception
  handler. Controller-side `isAvailableOperation`/`checkFilterOperation` calls and the ops
  `Set` constants are deleted.
- Typed builder methods (`string`, `ofLong`, `ofInteger`, `ofDouble`, `ofFloat`, `ofBoolean`,
  `instant`, `ofLocalDate`, `ofLocalDateTime`, `ofEnum`) carry built-in converters. Enum and
  date entries no longer need `map.put(X.class, X::valueOf)` in your `Converters` subclass —
  delete the subclass if only `field()`-free declarations remain. `field(name, path, type, ops)`
  is the only method that reads `Converters` (pass it via the 4-arg service constructor).
- Custom one-off specs move to `.custom(name, filter -> Specification)`.
- **Sort syntax narrowed**: `asc#field`, `desc#field` or bare `field` (ascending). The legacy
  `+field`/`-field` prefixes are rejected with `FilterValidationException` (in URLs `+` decodes
  to a space). `BasePageRequest`'s default sort changed from `-id` to `desc#id`.

## Релиз и версии

Версия в корневом `pom.xml` — это версия, **которая сейчас разрабатывается**. Релиз её не
меняет: коммит `chore: release X.Y` закрывает раздел changelog, master fast-forward'ится на
него, и сразу после этого develop переводится на следующую версию (`chore: start X.Y+1`).

Релиз запускается командой `/aurora-release` из ветки `develop` при чистом дереве. Дальше
всё делает CI: тесты, публикация `by.nhorushko:crud-abstract-generic:X.Y` в GitHub Packages,
тег `vX.Y` и GitHub Release из раздела `## X.Y` в `CHANGELOG.md`.

Теги руками не ставятся: тег — это результат релиза, а не его вход. Версию перед релизом
поднимать не нужно.

### Восстановление после неудачного релиза

Простой перезапуск workflow сам по себе не поможет: GitHub Packages отказывает 409 на
повторную публикацию уже опубликованной версии, поэтому нужно сначала понять, в каком из
двух промежуточных состояний остановился релиз.

- **Артефакт опубликован, тега нет** — удалить версию пакета на странице packages
  репозитория, затем перезапустить workflow.
- **Артефакт и тег на месте, GitHub Release не создан** — создать Release вручную из
  соответствующего раздела `CHANGELOG.md`.

## CI (GitHub Actions)

`.github/workflows/ci.yml`:

| Событие | Что происходит |
|---|---|
| PR в `develop`/`master` | `./mvnw -B verify` |
| push в `develop` | `./mvnw -B verify`, ничего не публикуется |
| push в `master` | проверка «тег `vX.Y` ещё не существует», verify, deploy в GitHub Packages, тег `vX.Y`, GitHub Release |
| ручной запуск (`workflow_dispatch`), даже на `master` | только `./mvnw -B verify`, публикации не бывает |

Секретов заводить не нужно — публикация идёт под `GITHUB_TOKEN`.
