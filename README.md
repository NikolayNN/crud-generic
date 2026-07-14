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

* JDK 17+
* Spring Boot 3.x
* ModelMapper

### Installation via Maven
Add the JitPack repository and the framework dependency to your pom.xml:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.NikolayNN</groupId>
    <artifactId>crud-generic</artifactId>
    <version>latest.version.here</version>
</dependency>

```
Find the latest versions at: https://jitpack.io/#NikolayNN/crud-generic

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
