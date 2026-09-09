# Generic CRUD Framework for Spring Boot

The Generic CRUD Framework simplifies the development of Spring Boot applications by providing a structured approach to mapping Data Transfer Objects (DTOs) to entities and implementing CRUD operations. Leveraging the power of ModelMapper and abstract classes, it streamlines the creation of services and controllers with minimal boilerplate code.

## Features
* Simplified DTO to Entity mappings and vice versa.
* Abstract configurations for easy mapping between DTOs and entities.
* Extended support for CRUD operations on entities with direct and related entities.
* Predefined hooks for custom business logic before and after CRUD operations.
* Ready-Made CRUD Services and Controllers: Enables quick generation of fully functional CRUD services and controllers with minimal coding required. This feature allows developers to focus on business logic and application-specific requirements by leveraging generic patterns and practices for common CRUD operations.

## Getting Started

### Prerequisites

* JDK 25+
* Spring Boot 3.5+
* ModelMapper

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
        <version>14.0</version>
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
Before diving into the specifics of entity and DTO creation, enable the framework in your Spring Boot application by using the @EnableAbsGenericCrud annotation. This step is crucial as it sets up the necessary configurations for ModelMapper and other components required by the framework.

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
Extend AbsFlexMapConfigDefault in your configuration to set up mappings. Override createReadDtoFromEntity to construct the read DTO; override the mapSpecificFields* hooks only when a mapping needs custom logic.

```java
@Component
public class MyMappingConfig extends AbsFlexMapConfigDefault<MyCreateDto, MyUpdateDto, MyReadDto, MyEntity> {

    public MyMappingConfig(AbsModelMapper mapper) {
        super(mapper, MyCreateDto.class, MyUpdateDto.class, MyReadDto.class, MyEntity.class);
    }

    @Override
    protected MyReadDto createReadDtoFromEntity(AbsModelMapper mapper, MyEntity entity) {
        return new MyReadDto(entity.getId(), entity.getName());
    }
}
```
### Step 5: Create Services
Extend AbsFlexServiceCRUD or AbsFlexServiceExtCRUD for CRUD services. Implement abstract methods and use the provided functionalities.

```java
@Service
public class MyEntityService extends AbsFlexServiceCRUD<Long, MyEntity, MyReadDto, MyUpdateDto, MyCreateDto, MyRepository> {
    // Constructor and methods
}
```

### Step 6: Develop Controllers
Extend AbsFlexControllerCRUD or AbsFlexControllerExtCRUD for CRUD operations in your controller.

```java
@RestController
@RequestMapping("/my-entity")
public class MyEntityController extends AbsFlexControllerCRUD<Long, MyReadDto, MyReadDtoView, MyUpdateDto, MyCreateDto, MySettings, MyEntityService> {
    // Constructor and endpoint methods
}

```

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

    public RtRoutePageableService(RtRouteRepository repository, AbsModelMapper mapper) {
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
