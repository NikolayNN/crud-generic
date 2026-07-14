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
| `by.nhorushko.crudgeneric.v2.pageable.AbsFilterSpecification` | `by.nhorushko.crudgeneric.flex.pageable.AbsFilterSpecification` |
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
| `v2.pageable.AbsPagingAndSortingService` (deprecated) | `AbsFlexPagingAndSortingService` (constructor takes the repository + filter specs; implement `toDto` and `buildSpecification`) |
| `PagingAndSortingImmutableGenericService`, `PageableGenericRestController` | `AbsFlexPagingAndSortingService` + your own controller endpoint building a `PageFilterRequest` |
| `SpecificationUtils` | Removed with no direct replacement — compose `org.springframework.data.jpa.domain.Specification` instances directly, or use `AbsFilterSpecification` + `PageFilterRequest` for filter-driven paging |

Notes:
- Flex `save()` is an upsert (`persistOrMerge`): id `null`/`0` or an absent assigned id inserts; an existing id updates.
- `delete(missingId)` is a silent no-op (idempotent).
- The sentinel id `0` is treated as "new" and normalised to `null` on every save path.
