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

* JDK 8+
* Spring Boot 2.x
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
Versions: https://jitpack.io/#NikolayNN/crud-generic

## Usage Guide

### Step 1: Define Your Entities

Entities should extend AbstractEntity<?>, where ? is the type of your identifier (e.g., Long).

```java
@Entity
public class MyEntity extends AbstractEntity<Long> {
    // Entity definition
}
```

### Step 2: Create DTOs

Define your DTOs for create, read, and update operations. Create and Update DTOs should extend AbsCreateDto and AbstractDto<?> respectively, while Read DTO can directly extend AbstractDto<?>.

```java
public class MyCreateDto extends AbsCreateDto {
    // Fields specific to creation
}

public class MyReadDto extends AbstractDto<Long> {
    // Fields for reading
}

public class MyUpdateDto extends AbstractDto<Long> {
    // Fields for updating
}
```

### Step 3: Implement Mapping Configurations
Extend AbsFlexMapConfig in your configuration to set up mappings. Override abstract methods to return concrete mapper instances.

```java
@Configuration
public class MyMappingConfig extends AbsFlexMapConfig<MyCreateDto, MyUpdateDto, MyReadDto, MyEntity> {

    public MyMappingConfig(AbsDtoModelMapper mapper) {
        super(mapper, MyCreateDto.class, MyUpdateDto.class, MyReadDto.class, MyEntity.class);
    }

    @Override
    protected AbsMapBaseDtoToEntity<MyReadDto, MyEntity> mapperReadDtoToEntity(AbsDtoModelMapper mapper, Class<MyReadDto> readDtoClass, Class<MyEntity> entityClass) {
        // Implementation of mapping from MyReadDto to MyEntity
    }

    // Implement other mappings similarly
}
```
### Step 4: Create Services
Extend AbsFlexServiceCRUD or AbsFlexServiceExtCRUD for CRUD services. Implement abstract methods and use the provided functionalities.

```java
@Service
public class MyEntityService extends AbsFlexServiceCRUD<Long, MyEntity, MyReadDto, MyUpdateDto, MyCreateDto, MyRepository> {
    // Constructor and methods
}
```

### Step 5: Develop Controllers
Extend AbsFlexControllerCRUD or AbsFlexControllerExtCRUD for CRUD operations in your controller.

```java
@RestController
@RequestMapping("/my-entity")
public class MyEntityController extends AbsFlexControllerCRUD<Long, MyReadDto, MyReadDtoView, MyUpdateDto, MyCreateDto, MySettings, MyEntityService> {
    // Constructor and endpoint methods
}

```
